package com.abarigena.taskflow.aspect;

import com.abarigena.taskflow.dto.DomainEvent;
import com.abarigena.taskflow.producer.DomainEventProducerService;
import com.abarigena.taskflow.producer.KafkaLogProducerService;
import com.abarigena.taskflow.serviceNoSQL.LogEntryService;
import com.abarigena.taskflow.storeNoSQL.entity.EventLog;
import com.abarigena.taskflow.storeNoSQL.entity.LogEntry;
import com.abarigena.taskflow.storeNoSQL.repository.EventLogRepository;
import com.abarigena.taskflow.storeSQL.entity.Comment;
import com.abarigena.taskflow.storeSQL.entity.Project;
import com.abarigena.taskflow.storeSQL.entity.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceLoggingAspect {

    private final LogEntryService logEntryService;
    private final KafkaLogProducerService kafkaLogProducerService;
    private final DomainEventProducerService domainEventProducerService;
    private final EventLogRepository eventLogRepository;
    private final ObjectMapper objectMapper;

    enum OperationType {CREATE, UPDATE, DELETE, UNKNOWN}

    @Pointcut("execution(public * com.abarigena.taskflow.serviceSQL.*ServiceImpl.*(..)) || " +
            "execution(public * com.abarigena.taskflow.serviceNoSQL.*ServiceImpl.*(..)) " +
            "&& !within(com.abarigena.taskflow.serviceNoSQL.LogEntryServiceImpl)" +
            "&& !within(com.abarigena.taskflow.serviceNoSQL.TaskHistoryServiceImpl)")
    public void serviceMethods() {
    }

    @Around("serviceMethods()")
    public Object logServiceMethodAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getName();
        Object[] args = joinPoint.getArgs();

        Map<String, Object> context = new HashMap<>();
        context.put("class", className);
        context.put("method", methodName);

        try {
            context.put("args", Arrays.stream(args)
                    .map(arg -> arg != null ? arg.toString() : "null")
                    .collect(Collectors.joining(", ")));
        } catch (Exception e) {
            log.error("AOP Логирование: Не удалось преобразовать аргументы метода {}.{} в строку", className, methodName, e);
            context.put("args", "Не удалось преобразовать");
        }

        log.info(">>> Начинаем выполнение метода: {}.{}", className, methodName);
        logEntryService.logInfo(String.format("Начало выполнения метода %s.%s", className, methodName), context)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        Object result = joinPoint.proceed();

        if (result instanceof Mono<?> monoResult) {
            return monoResult
                    .doOnSuccess(successValue -> {
                        Map<String, Object> successContext = new HashMap<>(context);
                        log.info(">>> Успешное завершение метода: {}.{} \n {}", className, methodName, successContext);
                        logEntryService.logInfo(String.format("Успешное завершение метода %s.%s", className, methodName), successContext)
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                        handleKafkaLogging(successValue, className, methodName, args);

                        OperationType opType = getOperationType(methodName);
                        String entType = getEntityType(className);
                        if (opType != OperationType.UNKNOWN && !entType.equals("Unknown")) {
                            publishDomainEventAndUpdateLog(successValue, entType, opType, args);
                        }
                    })
                    .doOnError(error -> {
                        Map<String, Object> errorContext = new HashMap<>(context);
                        errorContext.put("error", error.getMessage());
                        errorContext.put("errorType", error.getClass().getName());
                        log.error(">>> Ошибка выполнения метода {}.{} : {}", className, methodName, error.getMessage());
                        logEntryService.logError(String.format("Ошибка выполнения метода %s.%s: %s", className, methodName, error.getMessage()),
                                        errorContext)
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                    });
        } else if (result instanceof Flux<?> fluxResult) {
            return fluxResult.doFinally(signalType -> {
                Map<String, Object> finalContext = new HashMap<>(context);
                finalContext.put("signal", signalType.toString());
                LogEntry.LogLevel level = LogEntry.LogLevel.INFO;
                String message = String.format("Завершение метода %s.%s с сигналом %s", className, methodName, signalType);

                if (signalType == SignalType.ON_ERROR) {
                    level = LogEntry.LogLevel.ERROR;
                    message = String.format("Метод %s.%s завершился с ошибкой", className, methodName);
                }

                if (level == LogEntry.LogLevel.ERROR) {
                    if (signalType != SignalType.ON_ERROR) {
                        log.info(message, finalContext);
                        logEntryService.logInfo(message, finalContext)
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                    } else {
                         log.error(message, finalContext);
                         logEntryService.logError(message, finalContext)
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                    }
                } else {
                    log.info(message, finalContext);
                    logEntryService.logInfo(message, finalContext)
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                }
            });
        } else {
            log.warn("AOP Логирование: Метод {}.{} вернул не реактивный тип. Логирование завершения будет базовым.", className, methodName);
            handleKafkaLogging(result, className, methodName, args);

            OperationType opType = getOperationType(methodName);
            String entType = getEntityType(className);
            if (opType != OperationType.UNKNOWN && !entType.equals("Unknown")) {
                publishDomainEventAndUpdateLog(result, entType, opType, args);
            }
            return result;
        }
    }

    private OperationType getOperationType(String methodName) {
        String lowerMethodName = methodName.toLowerCase();
        if (lowerMethodName.contains("create") || lowerMethodName.contains("save") || lowerMethodName.contains("add")) {
            return OperationType.CREATE;
        } else if (lowerMethodName.contains("update") || lowerMethodName.contains("edit")) {
            return OperationType.UPDATE;
        } else if (lowerMethodName.contains("delete") || lowerMethodName.contains("remove")) {
            return OperationType.DELETE;
        }
        return OperationType.UNKNOWN;
    }

    private String getEntityType(String className) {
        if (className.endsWith("TaskServiceImpl")) return "Task";
        if (className.endsWith("ProjectServiceImpl")) return "Project";
        if (className.endsWith("CommentServiceImpl")) return "Comment";
        return "Unknown";
    }

    private String populateTaskContext(Map<String, Object> kafkaContext, Object dataObject, Object[] args, OperationType operationType) {
        String kafkaMessage = "";
        switch (operationType) {
            case CREATE:
            case UPDATE:
                if (dataObject instanceof Task) {
                    Task task = (Task) dataObject;
                    kafkaContext.put("taskId", task.getId());
                    if (task.getTitle() != null) kafkaContext.put("title", task.getTitle());
                    if (task.getStatus() != null) kafkaContext.put("status", task.getStatus().toString());
                    kafkaMessage = operationType == OperationType.CREATE ? "Task created" : "Task updated";
                }
                break;
            case DELETE:
                if (args.length > 0) {
                    if (args[0] instanceof Long) {
                        kafkaContext.put("taskId", args[0]);
                        kafkaMessage = "Task deleted";
                    } else if (args[0] instanceof Task) {
                        Task task = (Task) args[0];
                        kafkaContext.put("taskId", task.getId());
                        kafkaMessage = "Task deleted";
                    }
                }
                break;
            default:
                return "";
        }
        return kafkaMessage;
    }

    private String populateProjectContext(Map<String, Object> kafkaContext, Object dataObject, Object[] args, OperationType operationType) {
        String kafkaMessage = "";
        switch (operationType) {
            case CREATE:
            case UPDATE:
                if (dataObject instanceof Project) {
                    Project project = (Project) dataObject;
                    kafkaContext.put("projectId", project.getId());
                    if (project.getName() != null) kafkaContext.put("name", project.getName());
                    if (project.getStatus() != null) kafkaContext.put("status", project.getStatus().toString());
                    kafkaMessage = operationType == OperationType.CREATE ? "Project created" : "Project updated";
                }
                break;
            case DELETE:
                if (args.length > 0) {
                    if (args[0] instanceof Long) {
                        kafkaContext.put("projectId", args[0]);
                        kafkaMessage = "Project deleted";
                    } else if (args[0] instanceof Project) {
                        Project project = (Project) args[0];
                        kafkaContext.put("projectId", project.getId());
                        kafkaMessage = "Project deleted";
                    }
                }
                break;
            default:
                return "";
        }
        return kafkaMessage;
    }

    private String populateCommentContext(Map<String, Object> kafkaContext, Object dataObject, Object[] args, OperationType operationType) {
        String kafkaMessage = "";
        switch (operationType) {
            case CREATE:
            case UPDATE:
                if (dataObject instanceof Comment) {
                    Comment comment = (Comment) dataObject;
                    kafkaContext.put("commentId", comment.getId());
                    kafkaContext.put("taskId", comment.getTaskId());
                    kafkaContext.put("userId", comment.getUserId());
                    kafkaMessage = operationType == OperationType.CREATE ? "Comment created" : "Comment updated";
                }
                break;
            case DELETE:
                if (args.length > 0) {
                    if (args[0] instanceof Long) {
                        kafkaContext.put("commentId", args[0]);
                        kafkaMessage = "Comment deleted";
                    } else if (args[0] instanceof Comment) {
                        Comment comment = (Comment) args[0];
                        kafkaContext.put("commentId", comment.getId());
                        kafkaMessage = "Comment deleted";
                    }
                }
                break;
            default:
                return "";
        }
        return kafkaMessage;
    }

    private void handleKafkaLogging(Object dataObject, String className, String methodName, Object[] args) {
        Map<String, Object> kafkaContext = new HashMap<>();
        String kafkaMessage = "";
        OperationType operationType = getOperationType(methodName);

        if (operationType == OperationType.UNKNOWN) {
            return;
        }

        String entityType = getEntityType(className);

        switch (entityType) {
            case "Task":
                kafkaMessage = populateTaskContext(kafkaContext, dataObject, args, operationType);
                break;
            case "Project":
                kafkaMessage = populateProjectContext(kafkaContext, dataObject, args, operationType);
                break;
            case "Comment":
                kafkaMessage = populateCommentContext(kafkaContext, dataObject, args, operationType);
                break;
            default:
                log.debug("Kafka logging not configured for entity type derived from class: {}", className);
                return; 
        }

        if (kafkaMessage != null && !kafkaMessage.isEmpty()) {
            kafkaLogProducerService.sendLog("INFO", kafkaMessage, kafkaContext);
        } else {
            log.debug("No Kafka message generated for method {} in class {}. Operation type: {}, Entity type: {}", methodName, className, operationType, entityType);
        }
    }

    /**
     * Публикует доменное событие в Kafka и сохраняет соответствующий EventLog в MongoDB.
     * Этот метод вызывается после успешного выполнения CRUD операций в сервисах.
     *
     * @param methodResult  Результат выполнения сервисного метода.
     * @param entityType    Тип сущности (например, "TASK", "PROJECT", "COMMENT").
     * @param operationType Тип операции (CREATE, UPDATE, DELETE).
     * @param methodArgs    Аргументы, переданные в сервисный метод (используются для получения ID при удалении).
     */
    private void publishDomainEventAndUpdateLog(Object methodResult, String entityType, OperationType operationType, Object[] methodArgs) {
        String eventTypeString = entityType + "_" + operationType.name();
        String entityIdString = null;
        Object eventPayload = methodResult;

        switch (operationType) {
            case CREATE:
            case UPDATE:
                if (methodResult == null) {
                    log.warn("Результат метода для {} {} равен null. Невозможно извлечь ID или payload.", entityType, operationType);
                    return;
                }
                try {
                    Object idValue = methodResult.getClass().getMethod("getId").invoke(methodResult);
                    entityIdString = String.valueOf(idValue);
                } catch (Exception e) {
                    log.error("Не удалось извлечь ID из methodResult для события {}. Тип результата: {}. Ошибка: {}",
                            eventTypeString, methodResult.getClass().getName(), e.getMessage());
                    return;
                }
                break;
            case DELETE:
                if (methodArgs != null && methodArgs.length > 0 && methodArgs[0] instanceof Long) {
                    entityIdString = String.valueOf(methodArgs[0]);
                    if (eventPayload == null || eventPayload instanceof reactor.core.publisher.Mono) {
                        eventPayload = Map.of("id", entityIdString, "status", "DELETED");
                    }
                } else {
                    log.warn("Не удалось извлечь ID для операции DELETE события {}. Аргументы метода: {}",
                            eventTypeString, Arrays.toString(methodArgs));
                    return;
                }
                break;
            case UNKNOWN:
            default:
                log.debug("Операция {} не требует публикации доменного события для сущности {}.", operationType, entityType);
                return;
        }

        if (entityIdString == null) {
            log.warn("entityIdString is null для события {}. Публикация отменена.", eventTypeString);
            return;
        }

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(eventPayload);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации payload в JSON для EventLog события {}: {}. Payload: {}",
                    eventTypeString, e.getMessage(), eventPayload, e);
            return;
        }

        EventLog eventLog = EventLog.builder()
                .eventType(eventTypeString)
                .entityId(entityIdString)
                .entityType(entityType)
                .payload(payloadJson)
                .build();

        DomainEvent domainEvent = DomainEvent.builder()
                .eventType(eventTypeString)
                .entityId(entityIdString)
                .entityType(entityType)
                .payload(eventPayload)
                .createdAt(eventLog.getCreatedAt())
                .build();

        final String finalEntityIdString = entityIdString;
        eventLogRepository.save(eventLog)
                .doOnSuccess(savedLog -> {
                    log.info("Лог события {} для сущности {} (ID: {}) успешно сохранен с ID: {}",
                            savedLog.getEventType(), savedLog.getEntityType(), savedLog.getEntityId(), savedLog.getId());
                    domainEventProducerService.sendDomainEvent(domainEvent);
                    log.info("Доменное событие {} для сущности {} (ID: {}) отправлено в Kafka.",
                            domainEvent.getEventType(), domainEvent.getEntityType(), domainEvent.getEntityId());
                })
                .doOnError(error -> log.error("Ошибка при сохранении EventLog для события {}_{} ID {}: {}",
                        entityType, operationType, finalEntityIdString, error.getMessage(), error))
                .subscribe();

        log.debug("Запланирована асинхронная отправка доменного события {} и сохранение лога для сущности {} (ID: {}).",
                eventTypeString, entityType, entityIdString);
    }
}
