package com.abarigena.taskflow.aspect;

import com.abarigena.taskflow.serviceNoSQL.LogEntryService;
import com.abarigena.taskflow.storeNoSQL.entity.LogEntry;
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

    @Pointcut("execution(public * com.abarigena.taskflow.serviceSQL.*ServiceImpl.*(..)) || " +
            "execution(public * com.abarigena.taskflow.serviceNoSQL.*ServiceImpl.*(..)) " +
            "&& !within(com.abarigena.taskflow.serviceNoSQL.LogEntryServiceImpl)")
    public void serviceMethods(){}

    @Around("serviceMethods()")
    public Object logServiceMethodAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getName();

        Object[] args = joinPoint.getArgs();

        Map<String, Object> context = new HashMap<>();
        context.put("class", className);
        context.put("method", methodName);

        try{
            context.put("args", Arrays.stream(args)
                    .map(arg -> arg != null ? arg.toString() : "null")
                    .collect(Collectors.joining(", ")));
        }catch (Exception e){
            log.error("AOP Логирование: Не удалось преобразовать аргументы метода {}.{} в строку", className, methodName, e);
            context.put("args", "Не удалось преобразовать");
        }
        // TODO: Добавить ID пользователя из контекста безопасности в будущем.

        log.info(">>> Начинаем выполнение метода: {}.{}", className, methodName);
        logEntryService.logInfo(String.format("Начало выполнения метода %s.%s", className, methodName), context)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        Object result = joinPoint.proceed();

        if(result instanceof Mono<?> monoResult){
            return monoResult
                    .doOnSuccess(successValue ->{
                        Map<String, Object> successContext = new HashMap<>(context);

                        log.info(">>> Успешное завершение метода: {}.{} \n {}", className, methodName,successContext);
                        logEntryService.logInfo(String.format("Успешное завершение метода %s.%s", className, methodName), successContext)
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
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
        }else if(result instanceof Flux<?> fluxResult){
            return fluxResult.doFinally(signalType ->{
                Map<String, Object> finalContext = new HashMap<>(context);

                finalContext.put("signal", signalType.toString());

                LogEntry.LogLevel level = LogEntry.LogLevel.INFO;
                String message = String.format("Завершение метода %s.%s с сигналом %s", className, methodName, signalType);

                if(signalType == SignalType.ON_ERROR){
                    level = LogEntry.LogLevel.ERROR;

                    message = String.format("Метод %s.%s завершился с ошибкой", className, methodName);
                }

                if(level == LogEntry.LogLevel.ERROR){
                    if (signalType != SignalType.ON_ERROR) {
                        log.info(message, finalContext);
                        logEntryService.logInfo(message, finalContext)
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                    }
                }else{
                    log.info(message, finalContext);
                    logEntryService.logInfo(message, finalContext)
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                }
            });
        }else {
            log.warn("AOP Логирование: Метод {}.{} вернул не реактивный тип. Логирование завершения будет базовым.", className, methodName);
            return result;
        }
    }

}
