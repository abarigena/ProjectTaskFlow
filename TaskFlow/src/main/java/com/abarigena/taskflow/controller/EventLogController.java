package com.abarigena.taskflow.controller;

import com.abarigena.taskflow.serviceNoSQL.EventLogService;
import com.abarigena.taskflow.storeNoSQL.entity.EventLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventLogController {
    private final EventLogService eventLogService;

    /**
     * Получает все журналы событий с использованием пагинации и сортировки.
     * @param page номер страницы
     * @param size количество элементов на странице
     * @param sort параметры сортировки (например, "createdAt,desc")
     * @return поток журналов событий
     */
    @GetMapping
    public Flux<EventLog> getAllEventLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort) {

        log.info("Received request to get all event logs: page={}, size={}, sort={}", page, size, sort);
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, sortParams[0]);

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        return eventLogService.getAllEventLogs(pageable);
    }

    /**
     * Получает журнал событий по его идентификатору.
     * @param id идентификатор журнала событий
     * @return моно журнала событий
     */
    @GetMapping("/{id}")
    public Mono<EventLog> getEventLogById(@PathVariable String id) {
        log.info("Received request to get event log by id: {}", id);
        return eventLogService.getEventLogById(id);
    }
}
