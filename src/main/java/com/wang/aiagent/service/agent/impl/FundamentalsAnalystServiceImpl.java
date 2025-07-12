package com.wang.aiagent.service.agent.impl;

import com.wang.aiagent.domain.MarketCoin;
import com.wang.aiagent.service.agent.FundamentalsAnalystService;
import com.wang.aiagent.service.utils.CryptoPriceService;
import com.wang.aiagent.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FundamentalsAnalystServiceImpl implements FundamentalsAnalystService {
    private final CryptoPriceService cryptoPriceService;

    @Override
    public Map<String, Object> analyzeFundamentals(String symbol) {
        symbol = StringUtils.extractSymbol(symbol);
        List<MarketCoin> marketCoins = cryptoPriceService.getCoinMarkets(symbol);
        if (marketCoins == null || marketCoins.isEmpty()) {
            return Collections.singletonMap("error", "No market data found for symbol: " + symbol);
        }
        MarketCoin coin = marketCoins.get(0);

        // 1. 估值与流动性
        Map<String, Object> valuation = new HashMap<>();
        valuation.put("当前价格", coin.getCurrent_price());
        valuation.put("市值", coin.getMarket_cap());
        valuation.put("市值排名", coin.getMarket_cap_rank());
        valuation.put("完全稀释估值", coin.getFully_diluted_valuation());
        valuation.put("24小时成交量", coin.getTotal_volume());

        // 2. 供应结构
        Map<String, Object> supply = new HashMap<>();
        supply.put("流通供应量", coin.getCirculating_supply());
        supply.put("总供应量", coin.getTotal_supply());
        supply.put("最大供应量", coin.getMax_supply());

        // 3. 历史表现
        Map<String, Object> history = new HashMap<>();
        history.put("历史最高价", coin.getAth());
        history.put("距历史最高价变动%", coin.getAth_change_percentage());
        history.put("历史最高价日期", coin.getAth_date());
        history.put("历史最低价", coin.getAtl());
        history.put("距历史最低价变动%", coin.getAtl_change_percentage());
        history.put("历史最低价日期", coin.getAtl_date());

        // 4. 短期波动
        Map<String, Object> volatility = new HashMap<>();
        volatility.put("24小时最高价", coin.getHigh_24h());
        volatility.put("24小时最低价", coin.getLow_24h());
        volatility.put("24小时价格变动", coin.getPrice_change_24h());
        volatility.put("24小时价格变动%", coin.getPrice_change_percentage_24h());
        volatility.put("24小时市值变动", coin.getMarket_cap_change_24h());
        volatility.put("24小时市值变动%", coin.getMarket_cap_change_percentage_24h());

        // 5. 其他基础信息
        Map<String, Object> others = new HashMap<>();
        others.put("币种ID", coin.getId());
        others.put("币种符号", coin.getSymbol());
        others.put("币种名称", coin.getName());
        others.put("币种图片", coin.getImage());
        others.put("投资回报率", coin.getRoi());
        others.put("最后更新时间", coin.getLast_updated());

        // 6. 量化评分（示例：市值、流动性、稀缺性、历史表现等多维打分）
        Map<String, Double> score = new HashMap<>();
        score.put("估值得分", scoreValuation(coin));
        score.put("流动性得分", scoreLiquidity(coin));
        score.put("稀缺性得分", scoreScarcity(coin));
        score.put("历史表现得分", scoreHistory(coin));
        score.put("波动性得分", scoreVolatility(coin));
        score.put("综合基本面得分", (scoreValuation(coin) + scoreLiquidity(coin) + scoreScarcity(coin) + scoreHistory(coin)) / 4);

        // 7. 信号判定与置信度
        Map<String, Object> strategySignals = new LinkedHashMap<>();
        // 估值信号
        double valuationScore = score.get("估值得分");
        String valuationSignal = valuationScore > 0.7 ? "bullish" : (valuationScore < 0.3 ? "bearish" : "neutral");
        Map<String, Object> valuationMap = new HashMap<>();
        valuationMap.put("signal", valuationSignal);
        valuationMap.put("confidence", valuationScore);
        valuationMap.put("metrics", normalizeMetrics(valuation));
        strategySignals.put("valuation", valuationMap);
        // 流动性信号
        double liquidityScore = score.get("流动性得分");
        String liquiditySignal = liquidityScore > 0.7 ? "bullish" : (liquidityScore < 0.3 ? "bearish" : "neutral");
        Map<String, Object> liquidityMap = new HashMap<>();
        liquidityMap.put("signal", liquiditySignal);
        liquidityMap.put("confidence", liquidityScore);
        liquidityMap.put("metrics", normalizeMetrics(valuation));
        strategySignals.put("liquidity", liquidityMap);
        // 稀缺性信号
        double scarcityScore = score.get("稀缺性得分");
        String scarcitySignal = scarcityScore > 0.7 ? "bullish" : (scarcityScore < 0.3 ? "bearish" : "neutral");
        Map<String, Object> scarcityMap = new HashMap<>();
        scarcityMap.put("signal", scarcitySignal);
        scarcityMap.put("confidence", scarcityScore);
        scarcityMap.put("metrics", normalizeMetrics(supply));
        strategySignals.put("scarcity", scarcityMap);
        // 历史表现信号
        double historyScore = score.get("历史表现得分");
        String historySignal = historyScore > 0.7 ? "bullish" : (historyScore < 0.3 ? "bearish" : "neutral");
        Map<String, Object> historyMap = new HashMap<>();
        historyMap.put("signal", historySignal);
        historyMap.put("confidence", historyScore);
        historyMap.put("metrics", normalizeMetrics(history));
        strategySignals.put("history", historyMap);
        // 波动性信号
        double volatilityScore = score.get("波动性得分");
        String volatilitySignal = volatilityScore < 0.3 ? "bullish" : (volatilityScore > 0.7 ? "bearish" : "neutral");
        Map<String, Object> volatilityMap = new HashMap<>();
        volatilityMap.put("signal", volatilitySignal);
        volatilityMap.put("confidence", 1.0 - volatilityScore);
        volatilityMap.put("metrics", normalizeMetrics(volatility));
        strategySignals.put("volatility", volatilityMap);
        // 综合信号加权
        double weightedSum = valuationScore * 0.3 + liquidityScore * 0.2 + scarcityScore * 0.2 + historyScore * 0.2 + (1.0 - volatilityScore) * 0.1;
        String overallSignal = weightedSum > 0.6 ? "bullish" : (weightedSum < 0.4 ? "bearish" : "neutral");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("signal", overallSignal);
        result.put("confidence", weightedSum);
        result.put("strategy_signals", strategySignals);
        result.put("基础信息", others);
        return result;
    }

    // 以下为简单的量化打分示例，可根据实际需求调整
    private double scoreValuation(MarketCoin coin) {
        // 市值越高得分越高，完全稀释估值越低得分越高
        double marketCapScore = coin.getMarket_cap() != null && coin.getMarket_cap() > 0 ? Math.min(coin.getMarket_cap() / 1e9, 1.0) : 0;
        double fdvScore = coin.getFully_diluted_valuation() != null && coin.getFully_diluted_valuation() > 0
                ? Math.max(1.0 - coin.getFully_diluted_valuation() / 1e10, 0) : 0.5;
        return 0.7 * marketCapScore + 0.3 * fdvScore;
    }

    private double scoreLiquidity(MarketCoin coin) {
        // 24小时成交量越高得分越高
        return coin.getTotal_volume() != null && coin.getTotal_volume() > 0 ? Math.min(coin.getTotal_volume() / 1e8, 1.0) : 0;
    }

    private double scoreScarcity(MarketCoin coin) {
        // 流通/最大供应量比值越低越稀缺
        if (coin.getCirculating_supply() != null && coin.getMax_supply() != null && coin.getMax_supply() > 0) {
            double ratio = coin.getCirculating_supply() / coin.getMax_supply();
            return 1.0 - Math.min(ratio, 1.0);
        }
        return 0.5;
    }

    private double scoreHistory(MarketCoin coin) {
        // 距历史高点回撤越小，历史低点越远，得分越高
        double athScore = coin.getAth_change_percentage() != null ? Math.max(0.0, Math.min(1.0, 1.0 - Math.abs(coin.getAth_change_percentage()) / 100.0)) : 0.5;
        double atlScore = coin.getAtl_change_percentage() != null ? Math.max(0.0, Math.min(1.0, Math.abs(coin.getAtl_change_percentage()) / 100.0)) : 0.5;
        return 0.5 * athScore + 0.5 * atlScore;
    }

    private double scoreVolatility(MarketCoin coin) {
        // 24小时价格波动率
        if (coin.getHigh_24h() != null && coin.getLow_24h() != null && coin.getCurrent_price() != null && coin.getCurrent_price() > 0) {
            double vol = (coin.getHigh_24h() - coin.getLow_24h()) / coin.getCurrent_price();
            return Math.min(vol, 1.0);
        }
        return 0.5;
    }

    /**
     * 对metrics中的数值进行0-1归一化（简单min-max归一化）
     * @param metrics 原始指标Map
     * @return 归一化后的Map
     */
    private Map<String, Object> normalizeMetrics(Map<String, Object> metrics) {
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
}
