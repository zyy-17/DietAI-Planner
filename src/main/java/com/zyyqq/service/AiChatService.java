package com.zyyqq.service;

import com.zyyqq.dto.request.ChatRequest;
import com.zyyqq.entity.*;
import com.zyyqq.exception.BusinessException;
import com.zyyqq.repository.AiChatMessageRepository;
import com.zyyqq.repository.AiChatSessionRepository;
import com.zyyqq.repository.AiGenerationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private static final int MAX_HISTORY_ROUNDS = 6;

    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;
    private final AiGenerationLogRepository generationLogRepository;
    private final UserService userService;
    private final DietRecordService dietRecordService;
    private final RecommendationService recommendationService;
    private final TransactionTemplate transactionTemplate;
    private final RestTemplate aiRestTemplate;

    @Value("${ai-service.url}")
    private String aiServiceUrl;

    public List<AiChatSession> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<AiChatMessage> getSessionMessages(Long sessionId, Long userId) {
        AiChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权访问此会话");
        }
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    public AiChatMessage chat(Long userId, ChatRequest request) {
        long start = System.currentTimeMillis();
        String contextSnapshot = buildContextSnapshot(userId);
        log.debug("上下文快照生成耗时: {}ms", System.currentTimeMillis() - start);

        Long sessionId = transactionTemplate.execute(status -> {
            AiChatSession session;
            if (request.getSessionId() == null) {
                session = AiChatSession.builder()
                        .userId(userId)
                        .title("新对话")
                        .build();
                session = sessionRepository.save(session);
            } else {
                session = sessionRepository.findById(request.getSessionId())
                        .orElseThrow(() -> new BusinessException("会话不存在"));
            }

            if (messageRepository.countBySessionId(session.getId()) == 0) {
                session.setTitle(generateSessionTitle(request.getContent()));
                sessionRepository.save(session);
            }

            AiChatMessage userMessage = AiChatMessage.builder()
                    .sessionId(session.getId())
                    .userId(userId)
                    .role("user")
                    .content(request.getContent())
                    .contextSnapshot(contextSnapshot)
                    .build();
            messageRepository.save(userMessage);

            return session.getId();
        });

        long aiStart = System.currentTimeMillis();
        String aiResponse = callAiService(userId, request.getContent(), contextSnapshot, sessionId);
        log.info("AI调用耗时: {}ms, sessionId={}", System.currentTimeMillis() - aiStart, sessionId);

        AiChatMessage assistantMessage = transactionTemplate.execute(status -> {
            AiChatMessage msg = AiChatMessage.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .role("assistant")
                    .content(aiResponse)
                    .build();
            msg = messageRepository.save(msg);

            AiGenerationLog genLog = AiGenerationLog.builder()
                    .userId(userId)
                    .type("chat")
                    .inputSummary(request.getContent())
                    .outputContent(aiResponse)
                    .modelName("ai-service")
                    .isAbnormal(0)
                    .build();
            generationLogRepository.save(genLog);

            return msg;
        });

        return assistantMessage;
    }

    @Transactional
    public AiChatSession createSession(Long userId, String title) {
        AiChatSession session = AiChatSession.builder()
                .userId(userId)
                .title(title != null ? title : "新对话")
                .build();
        return sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        AiChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此会话");
        }
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    private String buildContextSnapshot(Long userId) {
        User user = userService.getUserById(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("用户画像：");
        if (user.getRealName() != null) sb.append("姓名=").append(user.getRealName());
        if (user.getGender() != null) sb.append(", 性别=").append(user.getGender() == 1 ? "男" : "女");
        if (user.getBirthDate() != null) {
            int age = java.time.Period.between(user.getBirthDate(), java.time.LocalDate.now()).getYears();
            sb.append(", 年龄=").append(age).append("岁");
        }
        if (user.getHeight() != null) sb.append(", 身高=").append(user.getHeight()).append("cm");
        if (user.getWeight() != null) sb.append(", 体重=").append(user.getWeight()).append("kg");

        String activityDesc = switch (user.getActivityLevel() != null ? user.getActivityLevel() : 0) {
            case 1 -> "久坐(几乎不运动)";
            case 2 -> "轻度活动(每周1-3次)";
            case 3 -> "中度活动(每周3-5次)";
            case 4 -> "高度活动(每周6-7次)";
            case 5 -> "极高活动(体力劳动)";
            default -> "未知";
        };
        sb.append(", 活动水平=").append(activityDesc);

        if (user.getDietGoal() != null) {
            String goalDesc = switch (user.getDietGoal()) {
                case "lose" -> "减脂";
                case "maintain" -> "维持体重";
                case "gain" -> "增肌";
                default -> user.getDietGoal();
            };
            sb.append(", 饮食目标=").append(goalDesc);
        }

        if (user.getDietPreference() != null && !user.getDietPreference().isEmpty()) {
            sb.append(", 饮食偏好=").append(user.getDietPreference());
        }

        BigDecimal targetCal = userService.calculateTargetCalories(user);
        sb.append(", 每日目标热量=").append(targetCal).append("kcal");

        BigDecimal proteinTarget = resolveTarget(user.getTargetProtein(), targetCal, new BigDecimal("0.20"), new BigDecimal("4"));
        BigDecimal carbTarget = resolveTarget(user.getTargetCarbohydrate(), targetCal, new BigDecimal("0.50"), new BigDecimal("4"));
        BigDecimal fatTarget = resolveTarget(user.getTargetFat(), targetCal, new BigDecimal("0.30"), new BigDecimal("9"));
        sb.append("\n推荐营养素：蛋白质").append(proteinTarget).append("g, 碳水").append(carbTarget).append("g, 脂肪").append(fatTarget).append("g");

        List<DietRecord> todayRecords = dietRecordService.getTodayRecords(userId);
        BigDecimal todayCal = todayRecords.stream().map(DietRecord::getCalories).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayProtein = todayRecords.stream().map(r -> r.getProtein() != null ? r.getProtein() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayCarb = todayRecords.stream().map(r -> r.getCarbohydrate() != null ? r.getCarbohydrate() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayFat = todayRecords.stream().map(r -> r.getFat() != null ? r.getFat() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        sb.append("\n今日已摄入：热量").append(todayCal).append("kcal(剩余").append(targetCal.subtract(todayCal)).append("kcal)");
        sb.append(", 蛋白质").append(todayProtein).append("g, 碳水").append(todayCarb).append("g, 脂肪").append(todayFat).append("g");

        try {
            Map<String, Object> recContext = recommendationService.buildAiRecommendContext(userId, 5);
            @SuppressWarnings("unchecked")
            List<String> candidates = (List<String>) recContext.get("candidateFoods");
            if (candidates != null && !candidates.isEmpty()) {
                sb.append("\n推荐候选食物(算法评分Top5)：").append(String.join("、", candidates));
                sb.append("（请优先从这些食物中推荐，并结合用户偏好生成膳食建议）");
            }
        } catch (Exception e) {
            log.warn("推荐算法构建上下文失败: {}", e.getMessage());
        }

        return sb.toString();
    }

    private BigDecimal resolveTarget(BigDecimal userTarget, BigDecimal targetCal, BigDecimal ratio, BigDecimal calPerGram) {
        if (userTarget != null && userTarget.compareTo(BigDecimal.ZERO) > 0) {
            return userTarget;
        }
        return targetCal.multiply(ratio).divide(calPerGram, 1, RoundingMode.HALF_UP);
    }

    private String callAiService(Long userId, String message, String context, Long sessionId) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", message);
            requestBody.put("context", context);
            requestBody.put("session_id", sessionId);
            requestBody.put("user_id", userId);

            List<AiChatMessage> historyMessages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            int maxMessages = MAX_HISTORY_ROUNDS * 2;
            if (historyMessages.size() > maxMessages) {
                historyMessages = historyMessages.subList(historyMessages.size() - maxMessages, historyMessages.size());
                log.debug("历史消息截断: 保留最近{}条", maxMessages);
            }
            List<Map<String, String>> history = new java.util.ArrayList<>();
            for (AiChatMessage msg : historyMessages) {
                Map<String, String> item = new HashMap<>();
                item.put("role", msg.getRole());
                item.put("content", msg.getContent());
                history.add(item);
            }
            requestBody.put("history", history);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = aiRestTemplate.postForObject(
                    aiServiceUrl + "/api/chat/history",
                    requestBody,
                    Map.class
            );

            if (response != null && response.get("response") != null) {
                return response.get("response").toString();
            }
        } catch (Exception e) {
            log.warn("AI服务调用失败，降级为本地回复: {}", e.getMessage());
            return generateLocalResponse(message, context);
        }
        return generateLocalResponse(message, context);
    }

    private String generateLocalResponse(String message, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据您的健康数据，");

        if (message.contains("吃什么") || message.contains("推荐")) {
            sb.append("建议您选择营养均衡的餐食：\n");
            sb.append("• 主食：糙米饭或全麦面包，控制份量\n");
            sb.append("• 蛋白质：鸡胸肉、清蒸鱼或豆腐\n");
            sb.append("• 蔬菜：西兰花、菠菜等深色蔬菜\n");
            sb.append("• 水果：苹果或橙子作为加餐\n");
            sb.append("注意控制总热量摄入，保持三大营养素均衡配比。");
        } else if (message.contains("热量") || message.contains("卡路里")) {
            sb.append("您的每日目标热量请参考今日饮食页面的推荐值。");
            sb.append("建议碳水化合物占50%，蛋白质占20%，脂肪占30%。");
            sb.append("如有特殊需求，可咨询专业营养师。");
        } else {
            sb.append("我是您的智能膳食助手，可以为您提供：\n");
            sb.append("• 个性化膳食推荐\n");
            sb.append("• 营养摄入分析\n");
            sb.append("• 食物热量查询\n");
            sb.append("• 减脂/增肌饮食方案\n");
            sb.append("请随时向我提问！");
        }

        return sb.toString();
    }

    private String generateSessionTitle(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "新对话";
        }
        String title = content.trim().replaceAll("\\s+", " ");
        if (title.length() > 20) {
            title = title.substring(0, 20) + "...";
        }
        return title;
    }
}