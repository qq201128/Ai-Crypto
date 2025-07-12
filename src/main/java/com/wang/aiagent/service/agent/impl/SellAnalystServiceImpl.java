package com.wang.aiagent.service.agent.impl;

import com.wang.aiagent.service.agent.SellAnalystService;
import com.wang.aiagent.utils.StringUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.wang.aiagent.service.agent.EmotionAnalystService;
import com.wang.aiagent.service.agent.FundamentalsAnalystService;
import com.wang.aiagent.service.agent.TechnicalAnalystService;
import com.wang.aiagent.service.agent.ValuationAnalystService;
import com.wang.aiagent.domain.vo.CryptoCoinVo;
import com.wang.aiagent.domain.agent.TechnicalAnalyst;
import org.springframework.stereotype.Service;
import java.util.*;

@Data
@RequiredArgsConstructor
@Slf4j
@Service
public class SellAnalystServiceImpl implements SellAnalystService {
    private final EmotionAnalystService emotionAnalystService;
    private final FundamentalsAnalystService fundamentalsAnalystService;
    private final TechnicalAnalystService technicalAnalystService;
    private final ValuationAnalystService valuationAnalystService;

    @Override
    public Map<String, Object> analyzeSellRisk(String symbol, String interval, int limit) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 技术分析
        CryptoCoinVo coinVo = new CryptoCoinVo();
        coinVo.setSymbol(symbol);
        coinVo.setTimeInterval(interval);
        coinVo.setLimitSize(limit);
        Map<String, Object> technical = technicalAnalystService.InsertTechnicalAnalyst(coinVo);

        symbol = StringUtils.extractSymbol(symbol);
        // 3. 情绪分析
        Map<String, Object> emotion = emotionAnalystService.analyzeEmotion(symbol);
        // 4. 基本面分析
        Map<String, Object> fundamentals = fundamentalsAnalystService.analyzeFundamentals(symbol);
        // 4. 估值分析
        Map<String, Object> valuation = valuationAnalystService.analyzeValuation(symbol);

        // 聚合信号
        List<String> bearishArguments = new ArrayList<>();
        double totalConfidence = 0;
        int count = 0;
        // 情绪
        if (emotion != null && "bearish".equalsIgnoreCase((String) emotion.get("signal"))) {
            bearishArguments.add("情绪面悲观，市场参与者信心不足");
            Object confObj = emotion.get("confidence");
            double conf = confObj instanceof Number ? ((Number)confObj).doubleValue() : 0.0;
            totalConfidence += conf;
            count++;
        }
        // 基本面
        if (fundamentals != null && "bearish".equalsIgnoreCase((String) fundamentals.get("signal"))) {
            bearishArguments.add("基本面疲软，估值、流动性、稀缺性等多维度表现不佳");
            Object confObj = fundamentals.get("confidence");
            double conf = confObj instanceof Number ? ((Number)confObj).doubleValue() : 0.0;
            totalConfidence += conf;
            count++;
        }
        // 技术面
        if (technical != null && "bearish".equalsIgnoreCase((String) technical.get("technicalContent"))) {
            bearishArguments.add("技术面信号消极，趋势、动量等指标支持下跌");
            Object confObj = technical.get("confidence");
            double conf = confObj instanceof Number ? ((Number)confObj).doubleValue() : 0.0;
            totalConfidence += conf;
            count++;
        }
        // 估值
        if (valuation != null && "bearish".equalsIgnoreCase((String) valuation.get("signal"))) {
            bearishArguments.add("估值偏高或高估，存在下行风险");
            Object confObj = valuation.get("confidence");
            double conf = confObj instanceof Number ? ((Number)confObj).doubleValue() : 0.0;
            totalConfidence += conf;
            count++;
        }
        // 置信度均值
        double avgConfidence = count > 0 ? totalConfidence / count : 0;
        String risk = bearishArguments.isEmpty() ? "暂无明显看空信号，建议观望。" : String.join("；", bearishArguments);
        result.put("risk_warning", risk);
        result.put("confidence", avgConfidence);
        result.put("details", Map.of(
                "emotion", emotion,
                "fundamentals", fundamentals,
                "technical", technical,
                "valuation", valuation
        ));
        result.put("signal", bearishArguments.isEmpty() ? "neutral" : "bearish");
        return result;
    }
}
