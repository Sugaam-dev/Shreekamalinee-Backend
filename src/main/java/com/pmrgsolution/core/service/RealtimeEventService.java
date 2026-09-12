package com.pmrgsolution.core.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

/**
 * Enterprise Service for managing Server-Sent Events (SSE) subscriptions
 * and broadcasting real-time live events to all connected clients.
 */
@Slf4j
@Service
public class RealtimeEventService {

    // 30 minutes timeout for individual SSE connections
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final List<SseEmitter> activeEmitters = new CopyOnWriteArrayList<>();

    /**
     * Registers a new SSE client connection and sends an initial connection event.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> activeEmitters.remove(emitter));
        emitter.onTimeout(() -> {
            activeEmitters.remove(emitter);
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        });
        emitter.onError(e -> {
            activeEmitters.remove(emitter);
        });

        activeEmitters.add(emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data("{\"status\":\"connected\",\"timestamp\":" + System.currentTimeMillis() + "}", MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.debug("Failed to send initial SSE connect event: {}", e.getMessage());
            emitter.complete();
            activeEmitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * Broadcasts a named event with JSON payload to all active client streams.
     *
     * @param eventName Name of the event (e.g. PRODUCT_UPDATED, ORDER_UPDATED)
     * @param payload String or Object data payload
     */
    public void broadcast(String eventName, Object payload) {
        if (activeEmitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        String jsonPayload = (payload instanceof String) 
                ? (String) payload 
                : "{\"type\":\"" + eventName + "\",\"timestamp\":" + System.currentTimeMillis() + "}";

        for (SseEmitter emitter : activeEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(jsonPayload, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                deadEmitters.add(emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        }

        if (!deadEmitters.isEmpty()) {
            activeEmitters.removeAll(deadEmitters);
        }
    }

    /**
     * Scheduled heartbeat ping every 25 seconds to keep HTTP connections alive
     * through Nginx, Cloudflare, AWS ALB, and proxy load balancers.
     */
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        if (activeEmitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : activeEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("PING")
                        .data("{\"type\":\"PING\"}", MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                deadEmitters.add(emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        }

        if (!deadEmitters.isEmpty()) {
            activeEmitters.removeAll(deadEmitters);
        }
    }

    /**
     * Returns total currently connected SSE clients count.
     */
    public int getActiveConnectionCount() {
        return activeEmitters.size();
    }
}
