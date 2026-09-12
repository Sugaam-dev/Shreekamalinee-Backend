package com.pmrgsolution.core.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.pmrgsolution.core.service.RealtimeEventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Controller exposing real-time SSE stream endpoints.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Realtime Streaming", description = "Endpoints for Server-Sent Events (SSE) live updates")
public class RealtimeController {

    private final RealtimeEventService realtimeEventService;

    @Operation(summary = "Subscribe to live real-time server events (Root path)")
    @GetMapping(value = "/realtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEventsRoot(HttpServletRequest request) {
        if (request != null && request.getDispatcherType() == DispatcherType.ERROR) {
            return null;
        }
        return realtimeEventService.subscribe();
    }

    @Operation(summary = "Subscribe to live real-time server events (API v1 path)")
    @GetMapping(value = "/api/v1/realtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEventsV1(HttpServletRequest request) {
        if (request != null && request.getDispatcherType() == DispatcherType.ERROR) {
            return null;
        }
        return realtimeEventService.subscribe();
    }
}
