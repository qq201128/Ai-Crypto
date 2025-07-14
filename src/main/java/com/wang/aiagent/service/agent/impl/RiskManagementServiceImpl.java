package com.wang.aiagent.service.agent.impl;

import com.wang.aiagent.service.agent.RiskManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.wang.aiagent.service.agent.DebateRoomAnalystService;
import com.wang.aiagent.service.utils.CryptoPriceService;
import com.wang.aiagent.domain.CryptoKline;
import com.wang.aiagent.utils.CalculateUtils;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class RiskManagementServiceImpl implements RiskManagementService {
    private final DebateRoomAnalystService debateRoomAnalystService;
    private final CryptoPriceService cryptoPriceService;

    @Override
    public Map<String, Object> analyzeRiskManagement(String symbol, String interval, int limit, Map<String, Object> portfolio) {
        // 1. 获取K线数据
        List<CryptoKline> klineList = cryptoPriceService.getCryptoKline(symbol, interval, limit);
        if (klineList == null || klineList.size() < 60) {
            return Map.of("error", "K线数据不足，无法进行风险分析");
        }
        // 2. 计算收益率序列
        List<Double> closeList = new ArrayList<>();
        for (CryptoKline k : klineList) {
            closeList.add(Double.parseDouble(k.getClose()));
        }
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < closeList.size(); i++) {
            returns.add((closeList.get(i) - closeList.get(i - 1)) / closeList.get(i - 1));
        }
        // 3. 波动率
        double dailyVol = std(returns);
        double volatility = dailyVol * Math.sqrt(252);
        // 4. 滚动波动率分布
        List<Double> rollingStd = new ArrayList<>();
        for (int i = 119; i < returns.size(); i++) {
            List<Double> window = returns.subList(i - 119, i + 1);
            rollingStd.add(std(window) * Math.sqrt(252));
        }
        double volatilityMean = mean(rollingStd);
        double volatilityStd = std(rollingStd);
        double volatilityPercentile = volatilityStd != 0 ? (volatility - volatilityMean) / volatilityStd : 0.0;
        // 5. VaR
        List<Double> sortedReturns = new ArrayList<>(returns);
        Collections.sort(sortedReturns);
        int varIdx = (int) (sortedReturns.size() * 0.05);
        double var95 = sortedReturns.get(varIdx);
        // 6. 最大回撤
        double maxDrawdown = calcMaxDrawdown(closeList);
        // 7. 市场风险评分
        int marketRiskScore = 0;
        if (volatilityPercentile > 1.5) marketRiskScore += 2;
        else if (volatilityPercentile > 1.0) marketRiskScore += 1;
        if (var95 < -0.03) marketRiskScore += 2;
        else if (var95 < -0.02) marketRiskScore += 1;
        if (maxDrawdown < -0.20) marketRiskScore += 2;
        else if (maxDrawdown < -0.10) marketRiskScore += 1;
        // 8. 持仓限制
        double stock = portfolio.get("stock") instanceof Number ? ((Number)portfolio.get("stock")).doubleValue() : 0.0;
        double cash = portfolio.get("cash") instanceof Number ? ((Number)portfolio.get("cash")).doubleValue() : 0.0;
        double lastPrice = closeList.get(closeList.size() - 1);
        double currentStockValue = stock * lastPrice;
        double totalPortfolioValue = cash + currentStockValue;
        double basePositionSize = totalPortfolioValue * 0.25;
        double maxPositionAmount;
        if (marketRiskScore >= 4) maxPositionAmount = basePositionSize * 0.5;
        else if (marketRiskScore >= 2) maxPositionAmount = basePositionSize * 0.75;
        else maxPositionAmount = basePositionSize;
        // 修正：最大持仓币数量
        double maxPositionSize = lastPrice > 0 ? maxPositionAmount / lastPrice : 0.0;
        // 9. 压力测试
        Map<String, Double> stressScenarios = Map.of(
                "market_crash", -0.20,
                "moderate_decline", -0.10,
                "slight_decline", -0.05
        );
        Map<String, Map<String, Object>> stressTestResults = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : stressScenarios.entrySet()) {
            double potentialLoss = currentStockValue * entry.getValue();
            double impact = (cash + currentStockValue) != 0 ? potentialLoss / (cash + currentStockValue) : Double.NaN;
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("potential_loss", potentialLoss);
            res.put("portfolio_impact", impact);
            stressTestResults.put(entry.getKey(), res);
        }
        // 10. 获取辩论室信号
        Map<String, Object> debateResult = debateRoomAnalystService.analyzeDebateRoomRisk(symbol, interval, limit);
        double bullConfidence = getDouble(debateResult.getOrDefault("buy_result", Map.of()).toString(), "confidence");
        double bearConfidence = getDouble(debateResult.getOrDefault("sell_result", Map.of()).toString(), "confidence");
        double debateConfidence = getDouble(debateResult, "hybrid_diff");
        String debateSignal = debateResult.getOrDefault("suggestion", "hold").toString();
        // 11. 信心差异调整风险分数
        double confidenceDiff = Math.abs(bullConfidence - bearConfidence) / 100.0;
        if (confidenceDiff < 0.1) marketRiskScore += 1;
        if (debateConfidence < 0.3) marketRiskScore += 1;
        int riskScore = Math.min(Math.round(marketRiskScore), 10);
        // 12. 交易建议
        String tradingAction;
        if (riskScore >= 9) tradingAction = "hold";
        else if (riskScore >= 7) tradingAction = "reduce";
        else {
            if (debateSignal.contains("买入")) tradingAction = "buy";
            else if (debateSignal.contains("风险")) tradingAction = "sell";
            else tradingAction = "hold";
        }
        // 13. 输出
        Map<String, Object> riskMetrics = new LinkedHashMap<>();
        riskMetrics.put("volatility", volatility);
        riskMetrics.put("value_at_risk_95", var95);
        riskMetrics.put("max_drawdown", maxDrawdown);
        riskMetrics.put("market_risk_score", marketRiskScore);
        riskMetrics.put("stress_test_results", stressTestResults);
        Map<String, Object> debateAnalysis = new LinkedHashMap<>();
        debateAnalysis.put("bull_confidence", bullConfidence);
        debateAnalysis.put("bear_confidence", bearConfidence);
        debateAnalysis.put("debate_confidence", debateConfidence);
        debateAnalysis.put("debate_signal", debateSignal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("max_position_size", maxPositionSize); // 币数量
        result.put("max_position_amount", maxPositionAmount); // 金额
        result.put("risk_score", riskScore);
        result.put("trading_action", tradingAction);
        result.put("risk_metrics", riskMetrics);
        result.put("debate_analysis", debateAnalysis);
        result.put("reasoning", String.format("Risk Score %d/10: Market Risk=%d, Volatility=%.2f%%, VaR=%.2f%%, Max Drawdown=%.2f%%, Debate Signal=%s", riskScore, marketRiskScore, volatility * 100, var95 * 100, maxDrawdown * 100, debateSignal));
        // 智能置信度算法
        double confidence = 0.3;
        if (result.containsKey("risk_score")) {
            try {
                double riskScore1 = Double.parseDouble(result.get("risk_score").toString());
                confidence = Math.max(0.0, Math.min(1.0, 1.0 - riskScore1 / 10.0));
            } catch (Exception e) { /* ignore */ }
        }
        result.put("confidence", confidence);
        if (!result.containsKey("signal")) {
            String action = result.getOrDefault("trading_action", "hold").toString();
            String signal = "neutral";
            if ("buy".equalsIgnoreCase(action)) signal = "bullish";
            else if ("sell".equalsIgnoreCase(action)) signal = "bearish";
            result.put("signal", signal);
        }
        return result;
    }

    private double mean(List<Double> list) {
        if (list == null || list.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double d : list) sum += d;
        return sum / list.size();
    }
    private double std(List<Double> list) {
        double m = mean(list);
        double sum = 0.0;
        for (double d : list) sum += (d - m) * (d - m);
        return list.size() > 1 ? Math.sqrt(sum / (list.size() - 1)) : 0.0;
    }
    private double calcMaxDrawdown(List<Double> prices) {
        double maxDrawdown = 0.0;
        double peak = prices.get(0);
        for (double price : prices) {
            if (price > peak) peak = price;
            double drawdown = (price - peak) / peak;
            if (drawdown < maxDrawdown) maxDrawdown = drawdown;
        }
        return maxDrawdown;
    }
    private double getDouble(Object obj, String key) {
        if (obj instanceof Map) {
            Object val = ((Map<?, ?>) obj).get(key);
            if (val == null) return 0.0;
            String str = val.toString().replace("%", "");
            try { return Double.parseDouble(str); } catch (Exception e) { return 0.0; }
        }
        return 0.0;
    }
    private double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0.0;
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0.0; }
    }
}
