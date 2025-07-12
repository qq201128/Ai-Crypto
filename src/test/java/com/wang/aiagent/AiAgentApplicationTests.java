package com.wang.aiagent;

import com.wang.aiagent.service.agent.*;
import java.util.*;

import com.wang.aiagent.service.agent.impl.PortfolioManagementAnalystServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.mockito.Mockito.*;

@SpringBootTest
class AiAgentApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testPortfolioManagementWithMockAnalysts() {
        // Mock各分析师服务
        BuyAnalystService buyAnalystService = mock(BuyAnalystService.class);
        SellAnalystService sellAnalystService = mock(SellAnalystService.class);
        DebateRoomAnalystService debateRoomAnalystService = mock(DebateRoomAnalystService.class);
        RiskManagementService riskManagementService = mock(RiskManagementService.class);
        FundamentalsAnalystService fundamentalsAnalystService = mock(FundamentalsAnalystService.class);
        EmotionAnalystService emotionAnalystService = mock(EmotionAnalystService.class);
        ValuationAnalystService valuationAnalystService = mock(ValuationAnalystService.class);
        MacroNewsAnalystService macroNewsAnalystService = mock(MacroNewsAnalystService.class);
        MacroSymbolAnalystService macroSymbolAnalystService = mock(MacroSymbolAnalystService.class);

        // Mock返回值
        Map<String, Object> technicalSignal = Map.of(
            "signal", "bullish",
            "confidence", "80%",
            "strategy_signals", Map.of(
                "trend_following", Map.of("signal", "bullish", "confidence", "80%", "metrics", Map.of("adx", 30)),
                "mean_reversion", Map.of("signal", "neutral", "confidence", "50%", "metrics", Map.of("rsi_14", 55)),
                "momentum", Map.of("signal", "bullish", "confidence", "90%", "metrics", Map.of("momentum_1m", 0.12)),
                "volatility", Map.of("signal", "neutral", "confidence", "60%", "metrics", Map.of("historical_volatility", 0.18))
            )
        );
        Map<String, Object> fundamentalSignal = Map.of(
            "signal", "bullish",
            "confidence", "75%",
            "strategy_signals", Map.of(
                "valuation", Map.of("signal", "bullish", "confidence", "80%"),
                "liquidity", Map.of("signal", "bullish", "confidence", "70%"),
                "scarcity", Map.of("signal", "neutral", "confidence", "50%"),
                "history", Map.of("signal", "bullish", "confidence", "85%"),
                "volatility", Map.of("signal", "neutral", "confidence", "60%")
            )
        );
        Map<String, Object> sentimentSignal = Map.of(
            "signal", "neutral",
            "confidence", "60%",
            "strategy_signals", Map.of(
                "sentiment", Map.of("signal", "neutral", "confidence", "60%"),
                "galaxy", Map.of("signal", "neutral", "confidence", "60%"),
                "social_dominance", Map.of("signal", "neutral", "confidence", "60%"),
                "tweets", Map.of("signal", "neutral", "confidence", "60%"),
                "github_commits", Map.of("signal", "neutral", "confidence", "60%")
            )
        );
        Map<String, Object> valuationSignal = Map.of(
            "signal", "bullish",
            "confidence", "85%",
            "strategy_signals", Map.of(
                "market_cap", Map.of("signal", "bullish", "confidence", "90%"),
                "fdv", Map.of("signal", "neutral", "confidence", "60%"),
                "pe", Map.of("signal", "neutral", "confidence", "60%")
            )
        );
        Map<String, Object> macroSymbolSignal = Map.of(
            "analysis", "宏观环境积极，政策利好。"
        );
        Map<String, Object> macroNewsSignal = Map.of(
            "analysis", "市场情绪乐观，资金流入明显。"
        );
        Map<String, Object> riskSignal = Map.of(
            "risk_score", 2,
            "trading_action", "buy",
            "max_position_size", 10000,
            "risk_metrics", Map.of(
                "volatility", 0.12,
                "max_drawdown", -0.08,
                "value_at_risk_95", -0.02,
                "market_risk_score", 2
            )
        );
        when(buyAnalystService.analyzeBuyOpportunity(anyString(), anyString(), anyInt())).thenReturn(technicalSignal);
        when(fundamentalsAnalystService.analyzeFundamentals(anyString())).thenReturn(fundamentalSignal);
        when(emotionAnalystService.analyzeEmotion(anyString())).thenReturn(sentimentSignal);
        when(valuationAnalystService.analyzeValuation(anyString())).thenReturn(valuationSignal);
        when(macroSymbolAnalystService.getMacroSymbolNews(anyString())).thenReturn(macroSymbolSignal);
        when(macroNewsAnalystService.getMacroNews()).thenReturn(macroNewsSignal);
        when(riskManagementService.analyzeRiskManagement(anyString(), anyString(), anyInt(), anyMap())).thenReturn(riskSignal);
        when(debateRoomAnalystService.analyzeDebateRoomRisk(anyString(), anyString(), anyInt())).thenReturn(Map.of("signal", "bullish", "confidence", "80%"));

        // 构造PortfolioManagementAnalystServiceImpl
        PortfolioManagementAnalystServiceImpl service = new PortfolioManagementAnalystServiceImpl(
            buyAnalystService, sellAnalystService, debateRoomAnalystService, riskManagementService,
            fundamentalsAnalystService, emotionAnalystService, valuationAnalystService,
            macroNewsAnalystService, macroSymbolAnalystService
        );
        Map<String, Object> portfolio = new HashMap<>();
        portfolio.put("cash", 10000.0);
        portfolio.put("stock", 0.0);
        Map<String, Object> result = service.getPortfolioManagement("BTC", "1d", 60, portfolio);
        System.out.println(result.get("分析报告"));
    }
}
