package com.platform.app.ai.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.platform.app.ai.application.dto.request.ChatRequest;
import com.platform.app.ai.application.dto.response.ChatResponse;
import com.platform.app.ai.application.port.in.ChatUseCase;
import com.platform.app.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai/chat")
@Tag(name = "AI Chat", description = "AI Assistant endpoints (Slice 1)")
public class ChatController {

    private final ChatUseCase chatUseCase;

    @PostMapping
    @Operation(summary = "Send a prompt and receive structured AI answer")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatUseCase.sendMessage(request);
        return ResponseEntity.ok(ApiResponse.ok("Message processed successfully", response));
    }
}
