package com.wang.aiagent.service.agent.impl;

import com.wang.aiagent.domain.MarketCoin;
import com.wang.aiagent.service.agent.ValuationAnalystService;
import com.wang.aiagent.service.utils.CryptoPriceService;
import com.wang.aiagent.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValuationAnalystServiceImpl implements ValuationAnalystService {
    private final CryptoPriceService cryptoPriceService;
    @Override
    public Map<String, Object> analyzeValuation(String symbol) {
        symbol = StringUtils.extractSymbol(symbol);
        List<MarketCoin> marketCoins = cryptoPriceService.getCoinMarkets(symbol);
        if (marketCoins == null || marketCoins.isEmpty()) {
            return Collections.singletonMap("error", "No market data found for symbol: " + symbol);
        }
        MarketCoin coin = marketCoins.get(0);
        // 估值相关指标
        double marketCap = coin.getMarket_cap() != null ? coin.getMarket_cap() : 0.0;
        double fdv = coin.getFully_diluted_valuation() != null ? coin.getFully_diluted_valuation() : 0.0;
        double pe = 0.0; // 预留PE等估值因子
        // 归一化处理
        double normMarketCap = Math.min(marketCap / 1e9, 1.0); // 10亿为高
        double normFdv = fdv > 0 ? Math.max(1.0 - fdv / 1e10, 0) : 0.5;
        double normPe = 0.5; // 预留
        // 信号判定
        String marketCapSignal = normMarketCap > 0.6 ? "bullish" : (normMarketCap < 0.4 ? "bearish" : "neutral");
        String fdvSignal = normFdv > 0.6 ? "bullish" : (normFdv < 0.4 ? "bearish" : "neutral");
        String peSignal = normPe > 0.6 ? "bullish" : (normPe < 0.4 ? "bearish" : "neutral");
        // 组合信号加权
        double weightedSum = normMarketCap * 0.5 + normFdv * 0.4 + normPe * 0.1;
        String overallSignal = weightedSum > 0.6 ? "bullish" : (weightedSum < 0.4 ? "bearish" : "neutral");
        // 置信度直接用double
        double confidence = weightedSum;
        // 详细分项
        Map<String, Object> strategySignals = new LinkedHashMap<>();
        Map<String, Object> marketCapMap = new HashMap<>();
        marketCapMap.put("signal", marketCapSignal);
        marketCapMap.put("confidence", normMarketCap);
        marketCapMap.put("metrics", Collections.singletonMap("market_cap", marketCap));
        strategySignals.put("market_cap", marketCapMap);
        Map<String, Object> fdvMap = new HashMap<>();
        fdvMap.put("signal", fdvSignal);
        fdvMap.put("confidence", normFdv);
        fdvMap.put("metrics", Collections.singletonMap("fully_diluted_valuation", fdv));
        strategySignals.put("fdv", fdvMap);
        Map<String, Object> peMap = new HashMap<>();
        peMap.put("signal", peSignal);
        peMap.put("confidence", normPe);
        peMap.put("metrics", Collections.singletonMap("pe", pe));
        strategySignals.put("pe", peMap);
        // 汇总
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("signal", overallSignal);
        result.put("confidence", confidence);
        result.put("strategy_signals", strategySignals);
        result.put("基础信息", coin);
        return result;
    }
}
