package com.zyyqq.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    private Long sessionId;

    @NotBlank(message = "消息内容不能为空")
    private String content;
}