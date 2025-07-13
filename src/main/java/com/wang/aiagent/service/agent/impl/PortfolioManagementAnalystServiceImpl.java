package com.wang.aiagent.service.agent.impl;

import com.wang.aiagent.service.agent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class PortfolioManagementAnalystServiceImpl implements PortfolioManagementAnalystService {
    private final BuyAnalystService buyAnalystService;
    private final SellAnalystService sellAnalystService;
    private final DebateRoomAnalystService debateRoomAnalystService;
    private final RiskManagementService riskManagementService;
    private final FundamentalsAnalystService fundamentalsAnalystService;
    private final EmotionAnalystService emotionAnalystService;
    private final ValuationAnalystService valuationAnalystService;
    private final MacroNewsAnalystService macroNewsAnalystService;
    private final MacroSymbolAnalystService macroSymbolAnalystService;

    /**
     * 综合所有分析师、辩论室、风险管理等Agent的信号，做出最终买卖/持有决策，严格遵守风险约束
     * @param symbol 币种符号
     * @param interval K线周期
     * @param limit K线数量
     * @param portfolio 当前持仓信息（Map，包含cash/stock等）
     * @return 综合决策结果
     */
    @Override
    public Map<String, Object> getPortfolioManagement(String symbol, String interval, int limit, Map<String, Object> portfolio) {
        // 防御性编程：portfolio 为空时初始化为默认空持仓
        if (portfolio == null) {
            portfolio = new HashMap<>();
            portfolio.put("cash", 0.0);
            portfolio.put("stock", 0.0);
        }
        // 1. 获取各分析师信号
        Map<String, Object> technicalSignal = null;
        Map<String, Object> fundamentalSignal = null;
        Map<String, Object> sentimentSignal = null;
        Map<String, Object> valuationSignal = null;
        Map<String, Object> macroSymbolSignal = null;
        Map<String, Object> macroNewsSignal = null;

        technicalSignal = buyAnalystService.analyzeBuyOpportunity(symbol, interval, limit); // buy分析师包含技术

        fundamentalSignal = fundamentalsAnalystService.analyzeFundamentals(symbol);

        sentimentSignal = emotionAnalystService.analyzeEmotion(symbol);

        valuationSignal = valuationAnalystService.analyzeValuation(symbol);

        macroSymbolSignal = macroSymbolAnalystService.getMacroSymbolNews(symbol);

        macroNewsSignal = macroNewsAnalystService.getMacroNews();


        // 2. 获取辩论室信号
        Map<String, Object> debateSignal = null;
        try {
            debateSignal = debateRoomAnalystService.analyzeDebateRoomRisk(symbol, interval, limit);
        } catch (Exception e) { log.warn("辩论室信号获取失败: {}", e.getMessage()); }

        // 3. 获取风险管理信号
        Map<String, Object> riskSignal = null;
        try {
            riskSignal = riskManagementService.analyzeRiskManagement(symbol, interval, limit, portfolio);
        } catch (Exception e) { log.warn("风险管理信号获取失败: {}", e.getMessage()); }

        // 4. 组装agent_signals
        List<Map<String, Object>> agentSignals = new ArrayList<>();
        agentSignals.add(buildAgentSignal("technical_analysis", technicalSignal));
        agentSignals.add(buildAgentSignal("fundamental_analysis", fundamentalSignal));
        agentSignals.add(buildAgentSignal("sentiment_analysis", sentimentSignal));
        agentSignals.add(buildAgentSignal("valuation_analysis", valuationSignal));
        agentSignals.add(buildAgentSignal("risk_management", riskSignal));
        agentSignals.add(buildAgentSignal("selected_stock_macro_analysis", macroSymbolSignal));
        agentSignals.add(buildAgentSignal("market_wide_news_summary", macroNewsSignal));
        if (debateSignal != null) {
            agentSignals.add(buildAgentSignal("debate_room", debateSignal));
        }

        // 5. 权重分配（可根据Python实现调整）
        double valuationWeight = 0.3;
        double fundamentalWeight = 0.25;
        double technicalWeight = 0.2;
        double macroWeight = 0.15;
        double sentimentWeight = 0.1;

        // 6. 信号加权汇总（简化版，实际可更复杂）
        double score = 0;
        double totalWeight = 0;
        score += getSignalScore(valuationSignal) * valuationWeight; totalWeight += valuationWeight;
        score += getSignalScore(fundamentalSignal) * fundamentalWeight; totalWeight += fundamentalWeight;
        score += getSignalScore(technicalSignal) * technicalWeight; totalWeight += technicalWeight;
        score += getSignalScore(macroSymbolSignal) * macroWeight * 0.5; totalWeight += macroWeight * 0.5;
        score += getSignalScore(macroNewsSignal) * macroWeight * 0.5; totalWeight += macroWeight * 0.5;
        score += getSignalScore(sentimentSignal) * sentimentWeight; totalWeight += sentimentWeight;
        double finalScore = totalWeight > 0 ? score / totalWeight : 0;

        // 7. 风险约束
        String riskAction = riskSignal != null ? (String) riskSignal.getOrDefault("trading_action", "hold") : "hold";
        double maxPositionSize = riskSignal != null && riskSignal.get("max_position_size") != null ?
                Double.parseDouble(riskSignal.get("max_position_size").toString()) : 0;

        double cash = portfolio.get("cash") instanceof Number ? ((Number)portfolio.get("cash")).doubleValue() : 0.0;
        double stock = portfolio.get("stock") instanceof Number ? ((Number)portfolio.get("stock")).doubleValue() : 0.0;
        String action;
        int quantity = 0;
        double confidence = Math.abs(finalScore);
        String advice = null;
        // 新增：无持仓建仓建议逻辑
        if (cash == 0 && stock == 0) {
            if (finalScore > 0.5 && "buy".equalsIgnoreCase(riskAction)) {
                action = "initiate_position";
                if (maxPositionSize > 0) {
                    advice = "当前无持仓，市场信号偏多，建议建仓。";
                    quantity = (int) maxPositionSize;
                } else {
                    advice = "当前无持仓且无可用资金，市场信号偏多，建议充值后建仓。";
                    quantity = 0;
                }
            } else if (finalScore < -0.5 && "sell".equalsIgnoreCase(riskAction)) {
                action = "wait_for_opportunity";
                advice = "当前无持仓，市场信号偏空，建议继续观望。";
                quantity = 0;
            } else {
                action = "hold";
                advice = "当前无持仓，市场信号中性，建议观望。";
                quantity = 0;
            }
        } else {
            if ("buy".equalsIgnoreCase(riskAction)) {
                action = cash > 0 ? "buy" : "hold";
                quantity = (int)Math.min(maxPositionSize, cash); // 简化：用现金买入
            } else if ("sell".equalsIgnoreCase(riskAction)) {
                action = stock > 0 ? "sell" : "hold";
                quantity = (int)Math.min(stock, maxPositionSize); // 简化：最多卖出持仓
            } else {
                action = "hold";
                quantity = 0;
            }
            advice = null;
        }
        log.info("------------------------------------------------------");
        log.info("technicalSignal: {}", technicalSignal);
        log.info("fundamentalSignal: {}", fundamentalSignal);
        log.info("sentimentSignal: {}", sentimentSignal);
        log.info("valuationSignal: {}", valuationSignal);
        log.info("macroSymbolSignal: {}", macroSymbolSignal);
        log.info("macroNewsSignal: {}", macroNewsSignal);
        log.info("riskSignal: {}", riskSignal);
        log.info("------------------------------------------------------");

        // 8. 决策说明
        String reasoning = String.format("综合各分析师信号，权重加权后得分%.2f，风险管理建议操作：%s，最大持仓：%.2f。", finalScore, riskAction, maxPositionSize);

        // 8.1 详细分析报告拼接
        StringBuilder detailedAnalysis = new StringBuilder();
        detailedAnalysis.append("====================================\n");
        detailedAnalysis.append("          投资分析报告\n");
        detailedAnalysis.append("====================================\n\n");
        // 基本面分析
        detailedAnalysis.append("一、策略分析\n\n");
        detailedAnalysis.append("1. 基本面分析 (权重25%):\n");
        detailedAnalysis.append("   信号: ").append(signalToChinese(fundamentalSignal != null ? fundamentalSignal.getOrDefault("signal", "无数据") : "无数据")).append("\n");
        detailedAnalysis.append("   置信度: ").append(fundamentalSignal != null && fundamentalSignal.get("confidence") != null ? String.format("%.0f%%", ((Number)fundamentalSignal.get("confidence")).doubleValue() * 100) : "0%").append("\n");
        Map<String, Object> fStrategy = fundamentalSignal != null ? (Map<String, Object>) fundamentalSignal.get("strategy_signals") : null;
        detailedAnalysis.append("   要点:\n");
        if (fStrategy != null) {
            detailedAnalysis.append("   - 估值: ").append(fStrategy.getOrDefault("valuation", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - 流动性: ").append(fStrategy.getOrDefault("liquidity", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - 稀缺性: ").append(fStrategy.getOrDefault("scarcity", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - 历史表现: ").append(fStrategy.getOrDefault("history", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - 波动性: ").append(fStrategy.getOrDefault("volatility", Collections.emptyMap())).append("\n");
        } else {
            detailedAnalysis.append("   - 无数据\n");
        }
        // 估值分析
        detailedAnalysis.append("\n2. 估值分析 (权重30%):\n");
        detailedAnalysis.append("   信号: ").append(signalToChinese(valuationSignal != null ? valuationSignal.getOrDefault("signal", "无数据") : "无数据")).append("\n");
        detailedAnalysis.append("   置信度: ").append(valuationSignal != null && valuationSignal.get("confidence") != null ? String.format("%.0f%%", ((Number)valuationSignal.get("confidence")).doubleValue() * 100) : "0%").append("\n");
        Map<String, Object> vStrategy = valuationSignal != null ? (Map<String, Object>) valuationSignal.get("strategy_signals") : null;
        detailedAnalysis.append("   要点:\n");
        if (vStrategy != null) {
            detailedAnalysis.append("   - 市值: ").append(vStrategy.getOrDefault("market_cap", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - FDV: ").append(vStrategy.getOrDefault("fdv", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - PE: ").append(vStrategy.getOrDefault("pe", Collections.emptyMap())).append("\n");
        } else {
            detailedAnalysis.append("   - 无数据\n");
        }
        // 技术分析
        detailedAnalysis.append("\n3. 技术分析 (权重20%):\n");
        detailedAnalysis.append("   信号: ").append(signalToChinese(technicalSignal != null ? technicalSignal.getOrDefault("signal", "无数据") : "无数据")).append("\n");
        detailedAnalysis.append("   置信度: ").append(technicalSignal != null && technicalSignal.get("confidence") != null ? String.format("%.0f%%", ((Number)technicalSignal.get("confidence")).doubleValue() * 100) : "0%").append("\n");
        Map<String, Object> tStrategy = technicalSignal != null ? (Map<String, Object>) technicalSignal.get("strategy_signals") : null;
        detailedAnalysis.append("   要点:\n");
        if (tStrategy != null) {
            detailedAnalysis.append("   - 趋势跟踪: ").append(tStrategy.getOrDefault("trend_following", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - 均值回归: ").append(tStrategy.getOrDefault("mean_reversion", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - 动量: ").append(tStrategy.getOrDefault("momentum", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - 波动率: ").append(tStrategy.getOrDefault("volatility", Collections.emptyMap())).append("\n");
        } else {
            detailedAnalysis.append("   - 无数据\n");
        }
        // 宏观分析
        detailedAnalysis.append("\n4. 宏观分析 (权重15%):\n");
        detailedAnalysis.append("   个股宏观: ").append(macroSymbolSignal != null ? macroSymbolSignal.getOrDefault("analysis", "无数据") : "无数据").append("\n");
        detailedAnalysis.append("   大盘宏观: ").append(macroNewsSignal != null ? macroNewsSignal.getOrDefault("analysis", "无数据") : "无数据").append("\n");
        // 情绪分析
        detailedAnalysis.append("\n5. 情绪分析 (权重10%):\n");
        detailedAnalysis.append("   信号: ").append(signalToChinese(sentimentSignal != null ? sentimentSignal.getOrDefault("signal", "无数据") : "无数据")).append("\n");
        detailedAnalysis.append("   置信度: ").append(sentimentSignal != null && sentimentSignal.get("confidence") != null ? String.format("%.0f%%", ((Number)sentimentSignal.get("confidence")).doubleValue() * 100) : "0%").append("\n");
        Map<String, Object> sStrategy = sentimentSignal != null ? (Map<String, Object>) sentimentSignal.get("strategy_signals") : null;
        detailedAnalysis.append("   要点:\n");
        if (sStrategy != null) {
            detailedAnalysis.append("   - 情绪: ").append(sStrategy.getOrDefault("sentiment", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - Galaxy: ").append(sStrategy.getOrDefault("galaxy", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - 社交主导: ").append(sStrategy.getOrDefault("social_dominance", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - 推文: ").append(sStrategy.getOrDefault("tweets", Collections.emptyMap())).append("\n");
            detailedAnalysis.append("   - Github: ").append(sStrategy.getOrDefault("github_commits", Collections.emptyMap())).append("\n");
        } else {
            detailedAnalysis.append("   - 无数据\n");
        }
        // 风险评估
        detailedAnalysis.append("\n二、风险评估\n");
        if (riskSignal != null) {
            detailedAnalysis.append("风险评分: ").append(riskSignal.getOrDefault("risk_score", "无数据")).append("/10\n");
            Map<String, Object> riskMetrics = (Map<String, Object>) riskSignal.getOrDefault("risk_metrics", Collections.emptyMap());
            detailedAnalysis.append("主要指标:\n");
            detailedAnalysis.append("- 波动率: ").append(String.format("%.2f%%", riskMetrics.getOrDefault("volatility", 0.0))).append("\n");
            detailedAnalysis.append("- 最大回撤: ").append(String.format("%.2f%%", riskMetrics.getOrDefault("max_drawdown", 0.0))).append("\n");
            detailedAnalysis.append("- VaR(95%): ").append(String.format("%.2f%%", riskMetrics.getOrDefault("value_at_risk_95", 0.0))).append("\n");
            detailedAnalysis.append("- 市场风险: ").append(riskMetrics.getOrDefault("market_risk_score", "无数据")).append("/10\n");
        } else {
            detailedAnalysis.append("无风险数据\n");
        }
        // 投资建议
        detailedAnalysis.append("\n三、投资建议\n");
        detailedAnalysis.append("操作建议: ").append(signalToChinese(action)).append("\n");
        detailedAnalysis.append("交易数量: ").append(quantity).append("\n");
        detailedAnalysis.append("决策置信度: ").append(String.format("%.0f%%", confidence * 100)).append("\n");
        // 新增：拼接advice内容
        if (advice != null && !advice.isEmpty()) {
            detailedAnalysis.append("特别提示: ").append(advice).append("\n");
            if ("initiate_position".equals(action) && quantity == 0) {
                detailedAnalysis.append("当前无资金，建议充值后建仓。\n");
            }
        }
        // 决策依据
        detailedAnalysis.append("\n四、决策依据\n");
        detailedAnalysis.append(reasoning).append("\n");
        detailedAnalysis.append("====================================\n");

        // 9. 返回结构
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", action);
        result.put("quantity", quantity);
        result.put("confidence", confidence);
        result.put("agent_signals", agentSignals);
        result.put("reasoning", reasoning);
        result.put("advice", advice);
        result.put("分析报告", detailedAnalysis.toString());
        return result;
    }

    // 工具方法：构建agent信号
    private Map<String, Object> buildAgentSignal(String agentName, Map<String, Object> signal) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("agent_name", agentName);
        if (signal == null) {
            map.put("signal", "unavailable");
            map.put("confidence", 0.0);
        } else {
            map.put("signal", signal.getOrDefault("signal", "unavailable"));
            Object conf = signal.getOrDefault("confidence", 0.0);
            if (conf instanceof String && ((String)conf).endsWith("%")) {
                try { conf = Double.parseDouble(((String)conf).replace("%", "")) / 100.0; } catch (Exception e) { conf = 0.0; }
            }
            map.put("confidence", conf);
        }
        return map;
    }

    // 工具方法：信号转分数
    private double getSignalScore(Map<String, Object> signal) {
        if (signal == null) return 0.0;
        String s = String.valueOf(signal.getOrDefault("signal", "neutral"));
        if ("bullish".equalsIgnoreCase(s)) return 1.0;
        if ("bearish".equalsIgnoreCase(s)) return -1.0;
        return 0.0;
    }

    // 工具方法：英文信号转中文
    private String signalToChinese(Object signalObj) {
        if (signalObj == null) return "无数据";
        String signal = signalObj.toString().toLowerCase();
        switch (signal) {
            case "buy": return "买入";
            case "sell": return "卖出";
            case "hold": return "持有";
            case "neutral": return "观望";
            case "bullish": return "看涨";
            case "bearish": return "看跌";
            default: return signal;
        }
    }
}
