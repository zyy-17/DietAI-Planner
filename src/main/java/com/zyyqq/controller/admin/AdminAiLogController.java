package com.zyyqq.controller.admin;

import com.zyyqq.dto.response.ApiResponse;
import com.zyyqq.entity.AiGenerationLog;
import com.zyyqq.repository.AiGenerationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ai-logs")
@RequiredArgsConstructor
public class AdminAiLogController {

    private final AiGenerationLogRepository aiGenerationLogRepository;

    @GetMapping
    public ApiResponse<Page<AiGenerationLog>> getLogs(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (type != null && !type.isEmpty()) {
            return ApiResponse.success(aiGenerationLogRepository.findByTypeOrderByCreatedAtDesc(type, pageable));
        }
        return ApiResponse.success(aiGenerationLogRepository.findAllByOrderByCreatedAtDesc(pageable));
    }

    @PutMapping("/{id}/abnormal")
    public ApiResponse<Void> markAbnormal(@PathVariable Long id) {
        AiGenerationLog log = aiGenerationLogRepository.findById(id).orElseThrow(() -> new RuntimeException("记录不存在"));
        log.setIsAbnormal(1);
        aiGenerationLogRepository.save(log);
        return ApiResponse.success("标记成功", null);
    }
}