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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private static final int MAX_HISTORY_ROUNDS = 6;

    /** 注入上下文时回溯的天数（含今天），用于让 AI 了解用户"之前吃了什么" */
    private static final int CONTEXT_HISTORY_DAYS = 7;

    /** 每餐最多列举的食物条数，避免上下文过长拖慢推理 */
    private static final int MAX_ITEMS_PER_MEAL = 8;

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
        sb.append("\n每日营养目标：热量").append(targetCal).append("kcal");
        sb.append(", 蛋白质").append(proteinTarget).append("g, 碳水").append(carbTarget).append("g, 脂肪").append(fatTarget).append("g");

        List<DietRecord> todayRecords = dietRecordService.getTodayRecords(userId);
        BigDecimal todayCal = todayRecords.stream().map(DietRecord::getCalories).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayProtein = todayRecords.stream().map(r -> r.getProtein() != null ? r.getProtein() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayCarb = todayRecords.stream().map(r -> r.getCarbohydrate() != null ? r.getCarbohydrate() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayFat = todayRecords.stream().map(r -> r.getFat() != null ? r.getFat() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        sb.append("\n今日已摄入：热量").append(todayCal).append("kcal(还差").append(targetCal.subtract(todayCal)).append("kcal)");
        sb.append(", 蛋白质").append(todayProtein).append("g(还差").append(proteinTarget.subtract(todayProtein)).append("g)");
        sb.append(", 碳水").append(todayCarb).append("g(还差").append(carbTarget.subtract(todayCarb)).append("g)");
        sb.append(", 脂肪").append(todayFat).append("g(还差").append(fatTarget.subtract(todayFat)).append("g)");

        // 今日饮食明细：让 AI 明确知道用户"今天具体吃了什么"
        sb.append("\n今日饮食明细：");
        if (todayRecords.isEmpty()) {
            sb.append("暂无记录（用户今天还没有添加任何饮食）");
        } else {
            appendMealLines(sb, todayRecords);
        }

        // 近 7 天逐日明细 + 趋势：让 AI 知道用户"之前吃了什么"，而不只是今天
        appendRecentHistory(sb, userId);

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

    /** 餐次中文名 */
    private String mealLabel(String mealType) {
        if (mealType == null) {
            return "其他";
        }
        return switch (mealType) {
            case "breakfast" -> "早餐";
            case "lunch" -> "午餐";
            case "dinner" -> "晚餐";
            case "snack" -> "加餐";
            default -> mealType;
        };
    }

    /**
     * 近 7 天逐日明细 + 趋势概览。
     * 目的：让 AI 掌握用户"之前几天吃了什么"，而不只是今天——
     * 这样才能指出重复出现的问题（例如连续几天蛋白质不足、蔬菜偏少），
     * 并让推荐结果贴合用户真实的饮食习惯。
     */
    private void appendRecentHistory(StringBuilder sb, Long userId) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate start = today.minusDays(CONTEXT_HISTORY_DAYS - 1L);
            List<DietRecord> records = dietRecordService.getRecordsByDateRange(userId, start, today);

            sb.append("\n\n【近").append(CONTEXT_HISTORY_DAYS).append("天饮食明细（不含今天）】");
            if (records.isEmpty()) {
                sb.append("暂无历史记录（用户还没有添加过任何饮食）");
                return;
            }

            // 按日期倒序聚合，最近的一天排在最前
            Map<LocalDate, List<DietRecord>> byDay = new TreeMap<>(Comparator.reverseOrder());
            for (DietRecord r : records) {
                if (r.getRecordDate() == null) {
                    continue;
                }
                byDay.computeIfAbsent(r.getRecordDate(), k -> new ArrayList<>()).add(r);
            }

            List<LocalDate> pastDays = new ArrayList<>();
            for (LocalDate d : byDay.keySet()) {
                if (!d.equals(today)) {
                    pastDays.add(d);
                }
            }

            if (pastDays.isEmpty()) {
                sb.append("除今天外暂无更早的记录");
            } else {
                sb.append("共").append(pastDays.size()).append("天有记录：");
                for (LocalDate d : pastDays) {
                    List<DietRecord> dayRecords = byDay.get(d);
                    sb.append("\n").append(d).append("（").append(relativeDayLabel(d, today)).append("）");
                    sb.append("合计").append(round(sumOf(dayRecords, DietRecord::getCalories), 0)).append("kcal");
                    sb.append("/蛋白").append(round(sumOf(dayRecords, DietRecord::getProtein), 1)).append("g");
                    sb.append("/碳水").append(round(sumOf(dayRecords, DietRecord::getCarbohydrate), 1)).append("g");
                    sb.append("/脂肪").append(round(sumOf(dayRecords, DietRecord::getFat), 1)).append("g");
                    appendMealLines(sb, dayRecords);
                }
            }

            // 趋势概览：含今天，用于判断长期饮食习惯与执行力
            int dayCount = Math.max(byDay.size(), 1);
            BigDecimal divisor = BigDecimal.valueOf(dayCount);
            sb.append("\n【近").append(CONTEXT_HISTORY_DAYS).append("天趋势(含今天)】共记录").append(dayCount).append("天");
            sb.append(", 日均热量").append(sumOf(records, DietRecord::getCalories).divide(divisor, 0, RoundingMode.HALF_UP)).append("kcal");
            sb.append(", 日均蛋白质").append(sumOf(records, DietRecord::getProtein).divide(divisor, 1, RoundingMode.HALF_UP)).append("g");
            sb.append(", 日均碳水").append(sumOf(records, DietRecord::getCarbohydrate).divide(divisor, 1, RoundingMode.HALF_UP)).append("g");
            sb.append(", 日均脂肪").append(sumOf(records, DietRecord::getFat).divide(divisor, 1, RoundingMode.HALF_UP)).append("g");
        } catch (Exception e) {
            log.warn("构建近期饮食明细失败: {}", e.getMessage());
        }
    }

    /** 按餐次列举食物明细，超出上限时折叠为「等共N项」，避免上下文过长 */
    private void appendMealLines(StringBuilder sb, List<DietRecord> dayRecords) {
        Map<String, List<DietRecord>> byMeal = new LinkedHashMap<>();
        for (DietRecord r : dayRecords) {
            byMeal.computeIfAbsent(r.getMealType(), k -> new ArrayList<>()).add(r);
        }
        List<String> ordered = new ArrayList<>(List.of("breakfast", "lunch", "dinner", "snack"));
        for (String key : byMeal.keySet()) {
            if (!ordered.contains(key)) {
                ordered.add(key);
            }
        }
        for (String meal : ordered) {
            List<DietRecord> items = byMeal.get(meal);
            if (items == null || items.isEmpty()) {
                continue;
            }
            sb.append("\n  ").append(mealLabel(meal)).append("：");
            int limit = Math.min(items.size(), MAX_ITEMS_PER_MEAL);
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < limit; i++) {
                DietRecord r = items.get(i);
                String name = r.getFoodName() != null ? r.getFoodName() : "未知食物";
                parts.add(name + " " + r.getAmount() + "g(" + r.getCalories() + "kcal)");
            }
            if (items.size() > limit) {
                parts.add("等共" + items.size() + "项");
            }
            sb.append(String.join("、", parts));
        }
    }

    /** 日期相对描述：昨天/前天/周X */
    private String relativeDayLabel(LocalDate date, LocalDate today) {
        long diff = ChronoUnit.DAYS.between(date, today);
        if (diff == 1) {
            return "昨天";
        }
        if (diff == 2) {
            return "前天";
        }
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    private BigDecimal sumOf(List<DietRecord> records, Function<DietRecord, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (DietRecord r : records) {
            BigDecimal value = getter.apply(r);
            if (value != null) {
                total = total.add(value);
            }
        }
        return total;
    }

    private BigDecimal round(BigDecimal value, int scale) {
        return value == null ? BigDecimal.ZERO : value.setScale(scale, RoundingMode.HALF_UP);
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
            // 本轮用户消息在调用 AI 之前已落库，稍后会作为最新的 user 消息单独发送，这里先剔除避免重复提问
            if (!historyMessages.isEmpty()) {
                AiChatMessage last = historyMessages.get(historyMessages.size() - 1);
                if ("user".equals(last.getRole()) && message != null && message.equals(last.getContent())) {
                    historyMessages = new java.util.ArrayList<>(historyMessages.subList(0, historyMessages.size() - 1));
                }
            }
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

    /**
     * AI 服务不可用（超时 / Ollama 未就绪）时的兜底回复。
     * 注意：这里不能伪造成一条正常的营养建议——否则用户无法分辨
     * "模型真的回答了" 还是 "调用失败降级了"，排查问题时会误以为是模型幻觉。
     */
    private String generateLocalResponse(String message, String context) {
        log.warn("降级兜底：AI 未返回结果，用户提问={}，上下文长度={}字",
                message, context != null ? context.length() : 0);
        return "⚠️ AI 服务这次没有响应（本机模型推理超时，或 Ollama 未就绪），没能生成分析。\n\n"
                + "你的健康档案与饮食记录已经读取好了，请稍等片刻后重新发送一次；"
                + "如果经常超时，可以在 `ai-service` 里换用更小的模型（例如 qwen2.5:3b）加快回复速度。";
    }

    /**
     * 会话标题关键词规则表。
     * 每个元素为 {标题, 关键词1, 关键词2, ...}，按数组顺序判定优先级，先命中先返回。
     * 目的：让左侧会话列表显示「晚餐推荐」这类概括性标题，而不是把用户原话整句贴上去。
     */
    private static final List<String[]> TITLE_RULES = List.of(
            new String[]{"晚餐推荐", "晚餐", "晚饭", "晚上", "夜宵"},
            new String[]{"午餐推荐", "午餐", "午饭", "中饭", "中午"},
            new String[]{"早餐推荐", "早餐", "早饭", "早上"},
            new String[]{"加餐建议", "加餐", "零食", "下午茶"},
            new String[]{"食物替换", "替换", "换掉", "换成", "替代"},
            new String[]{"减脂建议", "减脂", "减肥", "瘦身", "掉秤"},
            new String[]{"增肌建议", "增肌", "长肌肉", "增重"},
            new String[]{"饮食分析", "分析", "评估", "达标", "摄入"},
            new String[]{"蛋白质摄入", "蛋白质"},
            new String[]{"膳食纤维", "膳食纤维", "蔬菜", "纤维"},
            new String[]{"热量咨询", "热量", "卡路里", "kcal", "大卡"},
            new String[]{"膳食计划", "食谱", "菜单", "计划", "安排", "搭配"},
            new String[]{"食物推荐", "推荐", "吃什么", "吃啥", "建议"}
    );

    /** 兜底截断时先去掉的口语化填充词 */
    private static final String[] TITLE_FILLERS = {
            "请问", "帮我", "我想", "麻烦", "一下", "能不能", "可以", "然后", "今天", "我"
    };

    private String generateSessionTitle(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "新对话";
        }
        String text = content.trim();

        // 1. 命中关键词：直接给出概括性标题
        //    例：「我今天晚上不知道吃什么，你帮我推荐一下」→「晚餐推荐」
        for (String[] rule : TITLE_RULES) {
            for (int i = 1; i < rule.length; i++) {
                if (text.contains(rule[i])) {
                    return rule[0];
                }
            }
        }

        // 2. 未命中时：去掉填充词与标点后截断，避免整句话占满侧边栏
        String fallback = text;
        for (String filler : TITLE_FILLERS) {
            fallback = fallback.replace(filler, "");
        }
        fallback = fallback.replaceAll("[\\s？?！!。，,、~～]+", "").trim();
        if (fallback.isEmpty()) {
            fallback = text;
        }
        boolean truncated = fallback.length() > 12;
        if (truncated) {
            fallback = fallback.substring(0, 12) + "…";
        }
        return fallback;
    }
}