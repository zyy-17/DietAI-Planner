package com.zyyqq.controller;

import com.zyyqq.dto.request.ChatRequest;
import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.AiChatMessage;
import com.zyyqq.entity.AiChatSession;
import com.zyyqq.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @GetMapping("/sessions")
    public ApiResponse<List<AiChatSession>> getSessions(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(aiChatService.getUserSessions(userId));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<AiChatMessage>> getSessionMessages(
            Authentication authentication,
            @PathVariable Long sessionId) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(aiChatService.getSessionMessages(sessionId, userId));
    }

    @PostMapping("/send")
    public ApiResponse<AiChatMessage> sendMessage(Authentication authentication,
                                                  @RequestBody ChatRequest request) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(aiChatService.chat(userId, request));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(Authentication authentication,
                                    @RequestBody ChatRequest request) {
        Long userId = getUserId(authentication);
        SseEmitter emitter = new SseEmitter(300_000L);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                aiChatService.chatStream(userId, request, emitter);
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            } finally {
                executor.shutdown();
            }
        });

        return emitter;
    }

    /** 创建新的AI对话会话 */
    @PostMapping("/sessions")
    public ApiResponse<AiChatSession> createSession(Authentication authentication,
                                                    @RequestBody(required = false) java.util.Map<String, String> body) {
        Long userId = getUserId(authentication);
        String title = body != null ? body.get("title") : null;
        return ApiResponse.success(aiChatService.createSession(userId, title));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(Authentication authentication,
                                           @PathVariable Long sessionId) {
        Long userId = getUserId(authentication);
        aiChatService.deleteSession(sessionId, userId);
        return ApiResponse.success("删除成功", null);
    }

    /** 从认证信息中提取用户ID */
    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}