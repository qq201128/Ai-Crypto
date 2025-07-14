package com.wang.aiagent.service.agent.impl;

import com.wang.aiagent.chat.service.impl.SiliconFlowAIServiceImpl;
import com.wang.aiagent.service.agent.DebateRoomAnalystService;
import com.wang.aiagent.service.agent.BuyAnalystService;
import com.wang.aiagent.service.agent.SellAnalystService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@RequiredArgsConstructor
@Slf4j
@Service
public class DebateRoomAnalystServiceImpl implements DebateRoomAnalystService {
    private final SiliconFlowAIServiceImpl siliconFlowAIService;
    private final BuyAnalystService buyAnalystService;
    private final SellAnalystService sellAnalystService;

    @Override
    public Map<String, Object> analyzeDebateRoomRisk(String symbol, String interval, int limit) {
        // 1. 买入分析
        Map<String, Object> buyResult = buyAnalystService.analyzeBuyOpportunity(symbol, interval, limit);
        // 2. 卖出分析
        Map<String, Object> sellResult = sellAnalystService.analyzeSellRisk(symbol, interval, limit);

        // 3. 组装观点和置信度，准备大模型输入
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("【买入观点】\n");
        stringBuilder.append(buyResult.getOrDefault("thesis", "无买入观点")).append("\n");
        stringBuilder.append("置信度: ").append(buyResult.getOrDefault("confidence", "0%"));
        stringBuilder.append("\n\n【卖出观点】\n");
        stringBuilder.append(sellResult.getOrDefault("risk_warning", "无卖出观点")).append("\n");
        stringBuilder.append("置信度: ").append(sellResult.getOrDefault("confidence", "0%"));

        // 4. 调用大模型分析
        String debateRoomChat = siliconFlowAIService.debateRoomChat(stringBuilder.toString());

        // 5. 解析置信度（去掉%）
        double buyConfidence = parseConfidence(buyResult.getOrDefault("confidence", "0%"));
        double sellConfidence = parseConfidence(sellResult.getOrDefault("confidence", "0%"));
        // 多空置信度差
        double confidenceDiff = (buyConfidence - sellConfidence) / 100.0; // -1~1

        // 6. 解析LLM评分（假设debateRoomChat返回json或结构化分数，示例用正则或简单提取）
        double llmScore = extractLLMScore(debateRoomChat); // -1~1
        // 7. 混合置信度差异，LLM权重30%
        double hybridDiff = confidenceDiff * 0.7 + llmScore * 0.3;

        // 8. 最终建议
        String suggestion;
        if (Math.abs(hybridDiff) < 0.1) {
            suggestion = "多空观点接近，建议观望。";
        } else if (hybridDiff > 0) {
            suggestion = "看多胜出，建议关注买入机会。";
        } else {
            suggestion = "看空胜出，建议关注风险。";
        }

        // 9. 构建返回消息
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("buy_result", buyResult);
        result.put("sell_result", sellResult);
        result.put("debate_summary", debateRoomChat);
        result.put("confidence_diff", confidenceDiff);
        result.put("llm_score", llmScore);
        result.put("hybrid_diff", hybridDiff);
        result.put("suggestion", suggestion);
        // 补充标准信号字段
        // 智能置信度算法
        double confidence = 0.3;
        if (result.containsKey("debate_confidence")) {
            try {
                confidence = Double.parseDouble(result.get("debate_confidence").toString());
                confidence = Math.max(0.0, Math.min(1.0, confidence));
            } catch (Exception e) { /* ignore */ }
        }
        result.put("confidence", confidence);
        if (!result.containsKey("signal")) result.put("signal", "neutral");
        return result;
    }

    // 工具方法：解析置信度百分比
    private double parseConfidence(Object confidenceObj) {
        if (confidenceObj == null) return 0.0;
        String str = confidenceObj.toString().replace("%", "");
        try { return Double.parseDouble(str); } catch (Exception e) { return 0.0; }
    }
    // 工具方法：从LLM输出中提取-1~1分数（示例实现，实际需根据debateRoomChat格式调整）
    private double extractLLMScore(String llmOutput) {
        // 假设格式如："LLM评分: 0.25"，可用正则或简单查找
        if (llmOutput == null) return 0.0;
        Matcher m = Pattern.compile("LLM评分[:：]\s*([-+]?[0-9]*\\.?[0-9]+)").matcher(llmOutput);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); } catch (Exception e) { return 0.0; }
        }
        // 若无结构化分数，默认0
        return 0.0;
    }
}
