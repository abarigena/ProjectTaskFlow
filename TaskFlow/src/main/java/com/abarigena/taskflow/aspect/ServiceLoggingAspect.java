package com.abarigena.taskflow.aspect;

import com.abarigena.taskflow.producer.KafkaLogProducerService;
import com.abarigena.taskflow.serviceNoSQL.LogEntryService;
import com.abarigena.taskflow.storeNoSQL.entity.LogEntry;
import com.abarigena.taskflow.storeSQL.entity.Comment;
import com.abarigena.taskflow.storeSQL.entity.Project;
import com.abarigena.taskflow.storeSQL.entity.Task;
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
                        // Refactored Kafka Logging
                        handleKafkaLogging(successValue, className, methodName, args);
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
                    if (signalType != SignalType.ON_ERROR) { //This condition seems wrong, should log error if signalType is ON_ERROR
                        log.info(message, finalContext);
                        logEntryService.logInfo(message, finalContext)
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                    } else { // Actual error case
                         log.error(message, finalContext); // Assuming error logging for ON_ERROR
                         logEntryService.logError(message, finalContext, new RuntimeException("Flux completed with error: " + signalType))
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
            // Refactored Kafka Logging for non-reactive results
            handleKafkaLogging(result, className, methodName, args);
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
            default: // UNKNOWN or other types
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
            return; // Not a C/U/D operation we care about for Kafka
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
}
