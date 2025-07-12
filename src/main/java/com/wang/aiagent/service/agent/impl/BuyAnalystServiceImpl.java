package com.wang.aiagent.service.agent.impl;

import com.wang.aiagent.service.agent.BuyAnalystService;
import com.wang.aiagent.service.agent.EmotionAnalystService;
import com.wang.aiagent.service.agent.FundamentalsAnalystService;
import com.wang.aiagent.service.agent.TechnicalAnalystService;
import com.wang.aiagent.service.agent.ValuationAnalystService;
import com.wang.aiagent.domain.vo.CryptoCoinVo;
import com.wang.aiagent.domain.agent.TechnicalAnalyst;
import com.wang.aiagent.utils.StringUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Data
@RequiredArgsConstructor
@Slf4j
@Service
public class BuyAnalystServiceImpl implements BuyAnalystService {
    private final EmotionAnalystService emotionAnalystService;
    private final FundamentalsAnalystService fundamentalsAnalystService;
    private final TechnicalAnalystService technicalAnalystService;
    private final ValuationAnalystService valuationAnalystService;

    @Override
    public Map<String, Object> analyzeBuyOpportunity(String symbol, String interval, int limit) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 技术分析
        CryptoCoinVo coinVo = new CryptoCoinVo();
        coinVo.setSymbol(symbol);
        coinVo.setTimeInterval(interval);
        coinVo.setLimitSize(limit);
        Map<String, Object> technical = technicalAnalystService.InsertTechnicalAnalyst(coinVo);

        symbol = StringUtils.extractSymbol(symbol);
        // 2. 情绪分析
        Map<String, Object> emotion = emotionAnalystService.analyzeEmotion(symbol);
        // 3. 基本面分析
        Map<String, Object> fundamentals = fundamentalsAnalystService.analyzeFundamentals(symbol);
        // 4. 估值分析
        Map<String, Object> valuation = valuationAnalystService.analyzeValuation(symbol);

        // 聚合信号
        List<String> bullishArguments = new ArrayList<>();
        double totalConfidence = 0;
        int count = 0;
        // 情绪
        if (emotion != null && "bullish".equalsIgnoreCase((String) emotion.get("signal"))) {
            bullishArguments.add("情绪面积极，市场参与者乐观");
            Object confObj = emotion.get("confidence");
            double conf = confObj instanceof Number ? ((Number)confObj).doubleValue() : 0.0;
            totalConfidence += conf;
            count++;
        }
        // 基本面
        if (fundamentals != null && "bullish".equalsIgnoreCase((String) fundamentals.get("signal"))) {
            bullishArguments.add("基本面强劲，估值、流动性、稀缺性等多维度表现良好");
            Object confObj = fundamentals.get("confidence");
            double conf = confObj instanceof Number ? ((Number)confObj).doubleValue() : 0.0;
            totalConfidence += conf;
            count++;
        }
        // 技术面
        if (technical != null && "bullish".equalsIgnoreCase((String) technical.get("technicalContent"))) {
            bullishArguments.add("技术面信号积极，趋势、动量等指标支持上涨");
            Object confObj = technical.get("confidence");
            double conf = confObj instanceof Number ? ((Number)confObj).doubleValue() : 0.0;
            totalConfidence += conf;
            count++;
        }
        // 估值
        if (valuation != null && "bullish".equalsIgnoreCase((String) valuation.get("signal"))) {
            bullishArguments.add("估值合理或被低估，具备上涨空间");
            Object confObj = valuation.get("confidence");
            double conf = confObj instanceof Number ? ((Number)confObj).doubleValue() : 0.0;
            totalConfidence += conf;
            count++;
        }
        // 置信度均值
        double avgConfidence = count > 0 ? totalConfidence / count : 0;
        String thesis = bullishArguments.isEmpty() ? "暂无明显看多信号，建议观望。" : String.join("；", bullishArguments);
        result.put("thesis", thesis);
        result.put("confidence", avgConfidence);
        result.put("details", Map.of(
                "emotion", emotion,
                "fundamentals", fundamentals,
                "technical", technical,
                "valuation", valuation
        ));
        result.put("signal", bullishArguments.isEmpty() ? "neutral" : "bullish");
        return result;
    }
}
