package com.wang.aiagent.service.agent.impl;

import com.wang.aiagent.domain.CryptoKline;
import com.wang.aiagent.domain.agent.TechnicalAnalyst;
import com.wang.aiagent.domain.vo.CryptoCoinVo;
import com.wang.aiagent.service.agent.TechnicalAnalystService;
import com.wang.aiagent.service.utils.CryptoPriceService;
import com.wang.aiagent.utils.CalculateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicalAnalystServiceImpl implements TechnicalAnalystService {
    private final CryptoPriceService cryptoPriceService;
    @Override
    public  Map<String, Object> InsertTechnicalAnalyst(CryptoCoinVo cryptoCoinVo) {

        //获取到币种等信息
        List<CryptoKline> cryptoKline = cryptoPriceService.getCryptoKline(cryptoCoinVo.getSymbol(), cryptoCoinVo.getTimeInterval(), cryptoCoinVo.getLimitSize());
        // 计算MACD、RSI、布林带、OBV、价格下降
        Map<String, String> macdMap = CalculateUtils.calculateMACD(cryptoKline);
        Map<String, String> rsiMap = CalculateUtils.calculateRSI(cryptoKline, 14);
        Map<String, String> bollingerMap = CalculateUtils.calculateBollingerBands(cryptoKline, 20);
        Map<String, Object> obvtMap = CalculateUtils.calculateOBV(cryptoKline);
        Map<String, Double> priceDropMap = CalculateUtils.calculatePriceDrop(cryptoKline, 5);
        int bullishSignalCount = 0;
        int bearishSignalCount = 0;
        int totalSignalCount = 0;
        if (macdMap.get("macdSignal").equals("bullish")) {
            bullishSignalCount++;
        }else if (macdMap.get("macdSignal").equals("bearish")) {
            bearishSignalCount++;
        }else {
            totalSignalCount++;
        }
        if (rsiMap.get("rsiSignal").equals("bullish")) {
            bullishSignalCount++;
        }else if (rsiMap.get("rsiSignal").equals("bearish")) {
            bearishSignalCount++;
        }else {
            totalSignalCount++;
        }
        if (bollingerMap.get("bollingerSignal").equals("bullish")) {
            bullishSignalCount++;
        }else if (bollingerMap.get("bollingerSignal").equals("bearish")) {
            bearishSignalCount++;
        }else {
            totalSignalCount++;
        }
        if (obvtMap.get("obvSignal").equals("bullish")) {
            bullishSignalCount++;
        }else if (obvtMap.get("obvSignal").equals("bearish")) {
            bearishSignalCount++;
        }else {
            totalSignalCount++;
        }

        //计算置信度
        double confidence = 0.0;
        String priceDropSignal = "";
        double rsi = Double.parseDouble(rsiMap.get("rsi"));
        if (priceDropMap.get("priceDrop") < 0.05 && rsi < 40) {
            confidence += 0.2;
            priceDropSignal = "bullish";
        }else if (priceDropMap.get("priceDrop") < -0.03 && rsi < 45) {
            confidence += 0.1;
            priceDropSignal = "bullish";
        }
        if (priceDropSignal.equals("bullish")) {
            bullishSignalCount++;
        }

        String overallSignal = "";
        if (bullishSignalCount > bearishSignalCount) {
            overallSignal = "bullish";
        }else if (bullishSignalCount < bearishSignalCount) {
            overallSignal = "bearish";
        }else {
            overallSignal = "neutral";
        }
        double bullishConfidence = bullishSignalCount / (double) totalSignalCount;
        double bearishConfidence = bearishSignalCount / (double)totalSignalCount;
        if (bullishConfidence > bearishConfidence) {
            confidence  = bullishConfidence;
        }else if (bullishConfidence < bearishConfidence) {
            confidence  = bearishConfidence;
        }
        Map<String,Object> messageContent = new HashMap<>();
        messageContent.put("macd",macdMap);
        messageContent.put("rsi",rsiMap);
        messageContent.put("bollinger",bollingerMap);
        messageContent.put("obv",obvtMap);
        messageContent.put("overallSignal",overallSignal);
        messageContent.put("confidence",confidence);


        // 计算趋势信号、均值回复信号、动量信号、波动率信号、统计套利信号
        Map<String, Object> trendSignals = CalculateUtils.calculateTrendSignals(cryptoKline);
        Map<String, Object> meanReversionSignals = CalculateUtils.calculateMeanReversionSignals(cryptoKline);
        Map<String, Object> momentumSignals = CalculateUtils.calculateMomentumSignals(cryptoKline);
        Map<String, Object> volatilitySignals = CalculateUtils.calculateVolatilitySignals(cryptoKline);
        Map<String, Object> statArbSignals = CalculateUtils.calculateStatArbSignals(cryptoKline);

        Map<String, Object> combinedSignalUsage = exampleCombinedSignalUsage(cryptoKline, trendSignals, meanReversionSignals, momentumSignals, volatilitySignals, statArbSignals);

        Map<String, Object> reportMap = buildAnalysisReport(combinedSignalUsage, trendSignals, meanReversionSignals, momentumSignals, volatilitySignals, statArbSignals);

        System.out.println(reportMap);
        return reportMap;
    }
    /**
     * 多策略信号加权组合
     * @param signals 各策略信号，格式：strategy -> {"signal": "bullish|neutral|bearish", "confidence": Double}
     * @param weights 各策略权重，格式：strategy -> 权重
     * @return Map，包含signal（bullish/bearish/neutral）、confidence（绝对值分数）
     */
    public static Map<String, Object> weightedSignalCombination(Map<String, Map<String, Object>> signals, Map<String, Double> weights) {
        // 信号数值映射
        Map<String, Integer> signalValues = new HashMap<>();
        signalValues.put("bullish", 1);
        signalValues.put("neutral", 0);
        signalValues.put("bearish", -1);

        double weightedSum = 0.0;
        double totalConfidence = 0.0;

        for (Map.Entry<String, Map<String, Object>> entry : signals.entrySet()) {
            String strategy = entry.getKey();
            Map<String, Object> signalMap = entry.getValue();
            String signalStr = String.valueOf(signalMap.get("signal"));
            int numericSignal = signalValues.getOrDefault(signalStr, 0);
            double weight = weights.getOrDefault(strategy, 1.0);
            double confidence = 0.0;
            Object confObj = signalMap.get("confidence");
            if (confObj instanceof Number) {
                confidence = ((Number) confObj).doubleValue();
            } else {
                try {
                    confidence = Double.parseDouble(String.valueOf(confObj));
                } catch (Exception e) {
                    confidence = 1.0;
                }
            }
            weightedSum += numericSignal * weight * confidence;
            totalConfidence += weight * confidence;
        }
        double finalScore = totalConfidence > 0 ? weightedSum / totalConfidence : 0.0;
        String finalSignal;
        if (finalScore > 0.2) {
            finalSignal = "bullish";
        } else if (finalScore < -0.2) {
            finalSignal = "bearish";
        } else {
            finalSignal = "neutral";
        }
        Map<String, Object> result = new HashMap<>();
        result.put("signal", finalSignal);
        result.put("confidence", Math.abs(finalScore));
        return result;
    }

    /**
     * 对metrics中的数值进行0-1归一化（简单min-max归一化）
     */
    public static Map<String, Object> normalizeMetrics(Map<String, Object> metrics) {
        if (metrics == null || metrics.isEmpty()) return metrics;
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (Object v : metrics.values()) {
            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();
                if (d < min) min = d;
                if (d > max) max = d;
            }
        }
        Map<String, Object> norm = new HashMap<>();
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            Object v = entry.getValue();
            if (v instanceof Number && max > min) {
                double d = ((Number) v).doubleValue();
                norm.put(entry.getKey(), (d - min) / (max - min));
            } else {
                norm.put(entry.getKey(), v);
            }
        }
        return norm;
    }

    /**
     * 构建analysis_report结构，类似Python实现
     */
    public static Map<String, Object> buildAnalysisReport(Map<String, Object> combinedSignal,
                                                         Map<String, Object> trendSignals,
                                                         Map<String, Object> meanReversionSignals,
                                                         Map<String, Object> momentumSignals,
                                                         Map<String, Object> volatilitySignals,
                                                         Map<String, Object> statArbSignals) {
        Map<String, Object> report = new HashMap<>();
        report.put("signal", combinedSignal.get("signal"));
        double conf = combinedSignal.get("confidence") instanceof Number ? ((Number)combinedSignal.get("confidence")).doubleValue() : 0.0;
        report.put("confidence", String.format("%d%%", Math.round(conf * 100)));
        Map<String, Object> strategySignals = new HashMap<>();
        // trend_following
        Map<String, Object> trendMetrics = (Map<String, Object>) trendSignals.get("metrics");
        double trendConf = trendSignals.get("confidence") instanceof Number ? ((Number)trendSignals.get("confidence")).doubleValue() : 0.0;
        Map<String, Object> trendMap = new HashMap<>();
        trendMap.put("signal", trendSignals.get("signal"));
        trendMap.put("confidence", String.format("%d%%", Math.round(trendConf * 100)));
        trendMap.put("metrics", normalizeMetrics(trendMetrics));
        strategySignals.put("trend_following", trendMap);
        // mean_reversion
        Map<String, Object> meanRevMetrics = (Map<String, Object>) meanReversionSignals.get("metrics");
        double meanRevConf = meanReversionSignals.get("confidence") instanceof Number ? ((Number)meanReversionSignals.get("confidence")).doubleValue() : 0.0;
        Map<String, Object> meanRevMap = new HashMap<>();
        meanRevMap.put("signal", meanReversionSignals.get("signal"));
        meanRevMap.put("confidence", String.format("%d%%", Math.round(meanRevConf * 100)));
        meanRevMap.put("metrics", normalizeMetrics(meanRevMetrics));
        strategySignals.put("mean_reversion", meanRevMap);
        // momentum
        Map<String, Object> momentumMetrics = (Map<String, Object>) momentumSignals.get("metrics");
        double momentumConf = momentumSignals.get("confidence") instanceof Number ? ((Number)momentumSignals.get("confidence")).doubleValue() : 0.0;
        Map<String, Object> momentumMap = new HashMap<>();
        momentumMap.put("signal", momentumSignals.get("signal"));
        momentumMap.put("confidence", String.format("%d%%", Math.round(momentumConf * 100)));
        momentumMap.put("metrics", normalizeMetrics(momentumMetrics));
        strategySignals.put("momentum", momentumMap);
        // volatility
        Map<String, Object> volatilityMetrics = (Map<String, Object>) volatilitySignals.get("metrics");
        double volatilityConf = volatilitySignals.get("confidence") instanceof Number ? ((Number)volatilitySignals.get("confidence")).doubleValue() : 0.0;
        Map<String, Object> volatilityMap = new HashMap<>();
        volatilityMap.put("signal", volatilitySignals.get("signal"));
        volatilityMap.put("confidence", String.format("%d%%", Math.round(volatilityConf * 100)));
        volatilityMap.put("metrics", normalizeMetrics(volatilityMetrics));
        strategySignals.put("volatility", volatilityMap);
        // statistical_arbitrage
        Map<String, Object> statArbMetrics = (Map<String, Object>) statArbSignals.get("metrics");
        double statArbConf = statArbSignals.get("confidence") instanceof Number ? ((Number)statArbSignals.get("confidence")).doubleValue() : 0.0;
        Map<String, Object> statArbMap = new HashMap<>();
        statArbMap.put("signal", statArbSignals.get("signal"));
        statArbMap.put("confidence", String.format("%d%%", Math.round(statArbConf * 100)));
        statArbMap.put("metrics", normalizeMetrics(statArbMetrics));
        strategySignals.put("statistical_arbitrage", statArbMap);
        report.put("strategy_signals", strategySignals);
        return report;
    }

    /**
     * 示例：如何在Java中组合多策略信号
     */
    public Map<String, Object> exampleCombinedSignalUsage(List<CryptoKline> cryptoKline,
                                                          Map<String, Object> trendSignals,
                                                          Map<String, Object> meanReversionSignals,
                                                          Map<String, Object> momentumSignals,
                                                          Map<String, Object> volatilitySignals,
                                                          Map<String, Object> statArbSignals) {
        // 策略权重
        Map<String, Double> strategyWeights = new HashMap<>();
        strategyWeights.put("trend", 0.30);
        strategyWeights.put("mean_reversion", 0.25); // mean reversion权重提升
        strategyWeights.put("momentum", 0.25);
        strategyWeights.put("volatility", 0.15);
        strategyWeights.put("stat_arb", 0.05);



        // 组装信号Map
        Map<String, Map<String, Object>> signals = new HashMap<>();
        signals.put("trend", trendSignals);
        signals.put("mean_reversion", meanReversionSignals);
        signals.put("momentum", momentumSignals);
        signals.put("volatility", volatilitySignals);
        signals.put("stat_arb", statArbSignals);

        // 组合信号
        Map<String, Object> combinedSignal = weightedSignalCombination(signals, strategyWeights);


        return combinedSignal;
    }



}
