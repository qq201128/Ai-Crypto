package com.wang.aiagent.service.agent.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wang.aiagent.service.agent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
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

        // 5. 权重分配（全分析师）
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("technical_analysis", 0.20);
        weights.put("fundamental_analysis", 0.20);
        weights.put("sentiment_analysis", 0.10);
        weights.put("valuation_analysis", 0.15);
        weights.put("risk_management", 0.10);
        weights.put("selected_stock_macro_analysis", 0.10);
        weights.put("market_wide_news_summary", 0.10);
        weights.put("debate_room", 0.05);

        // 6. 信号加权汇总（全分析师）
        double weightedScore = 0;
        double totalWeight = 0;
        for (Map<String, Object> sig : agentSignals) {
            String agent = (String) sig.get("agent_name");
            double weight = weights.getOrDefault(agent, 0.0);
            double confidence = 0.0;
            Object confObj = sig.getOrDefault("confidence", 0.0);
            if (confObj instanceof Number) confidence = ((Number) confObj).doubleValue();
            else {
                try { confidence = Double.parseDouble(confObj.toString()); } catch (Exception e) { confidence = 0.0; }
            }
            double score = getSignalScore(sig);
            weightedScore += score * confidence * weight;
            totalWeight += confidence * weight;
        }
        double finalScore = totalWeight > 0 ? weightedScore / totalWeight : 0;

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
        String reasoning = String.format("综合所有分析师信号，置信度加权后得分%.2f，风险管理建议操作：%s，最大持仓：%.2f。", finalScore, riskAction, maxPositionSize);

        // 8.1 详细分析报告拼接（Markdown美化版）
        StringBuilder detailedAnalysis = new StringBuilder();
        // 核心摘要
        detailedAnalysis.append("# 📊 投资分析报告\n\n");
        detailedAnalysis.append("**操作建议：** `" + signalToChinese(action) + "`  ");
        detailedAnalysis.append("**交易数量：** `" + quantity + "`  ");
        detailedAnalysis.append("**决策置信度：** `" + String.format("%.0f%%", confidence * 100) + "`  ");
        if (advice != null && !advice.isEmpty()) {
            detailedAnalysis.append("**特别提示：** " + advice + "  \n");
        }
        detailedAnalysis.append("---\n\n");

        // 分析师分节配置
        List<Map<String, Object>> analystSections = new ArrayList<>();
        Map<String, Object> section1 = new HashMap<>();
        section1.put("title", "技术分析师");
        section1.put("signal", technicalSignal);
        section1.put("type", "default");
        analystSections.add(section1);
        Map<String, Object> section2 = new HashMap<>();
        section2.put("title", "基本面分析师");
        section2.put("signal", fundamentalSignal);
        section2.put("type", "default");
        analystSections.add(section2);
        Map<String, Object> section3 = new HashMap<>();
        section3.put("title", "情绪分析师");
        section3.put("signal", sentimentSignal);
        section3.put("type", "default");
        analystSections.add(section3);
        Map<String, Object> section4 = new HashMap<>();
        section4.put("title", "估值分析师");
        section4.put("signal", valuationSignal);
        section4.put("type", "default");
        analystSections.add(section4);
        Map<String, Object> section5 = new HashMap<>();
        section5.put("title", "宏观分析师（个股）");
        section5.put("signal", macroSymbolSignal);
        section5.put("type", "macro");
        analystSections.add(section5);
        Map<String, Object> section6 = new HashMap<>();
        section6.put("title", "宏观分析师（大盘）");
        section6.put("signal", macroNewsSignal);
        section6.put("type", "macro");
        analystSections.add(section6);
        Map<String, Object> section7 = new HashMap<>();
        section7.put("title", "风险管理分析师");
        section7.put("signal", riskSignal);
        section7.put("type", "risk");
        analystSections.add(section7);

        detailedAnalysis.append("## 👥 分析师核心结论\n\n");
        for (Map<String, Object> section : analystSections) {
            detailedAnalysis.append(generateAnalystSection(
                (String) section.get("title"),
                section.get("signal"),
                (String) section.get("type")
            ));
        }
        // 辩论室分析师单独处理
        detailedAnalysis.append("### 辩论室分析师\n");
        if (debateSignal != null) {
            detailedAnalysis.append("- **建议：** " + debateSignal.getOrDefault("suggestion", "无数据") + "\n");
            String reason = null;
            Object analysisObj = debateSignal.get("debate_summary");
            if (analysisObj instanceof Map) {
                Object reasoningObj = ((Map<?, ?>) analysisObj).get("reasoning");
                if (reasoningObj != null) reason = reasoningObj.toString();
            } else if (analysisObj instanceof String) {
                try {
                    Gson gson = new Gson();
                    Type type = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> analysisMap = gson.fromJson((String) analysisObj, type);
                    Object reasoningObj = analysisMap.get("reasoning");
                    if (reasoningObj != null) reason = reasoningObj.toString();
                } catch (Exception e) {
                    // ignore
                }
            }
            if (reason != null && !reason.isEmpty()) {
                detailedAnalysis.append("- **理由：** " + reason + "\n");
            }
        } else {
            detailedAnalysis.append("- 无数据\n");
        }
        detailedAnalysis.append("\n---\n\n");

        // 决策依据
        detailedAnalysis.append("## 📝 决策依据\n");
        detailedAnalysis.append(reasoning + "\n");
        detailedAnalysis.append("\n---\n");


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

    // 新增：分析师分节生成工具方法
    /**
     * 生成分析师分节Markdown
     * @param title 分析师标题
     * @param signalObj 信号对象
     * @param type 分析师类型（default/macro/risk）
     * @return Markdown字符串
     */
    private String generateAnalystSection(String title, Object signalObj, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(title).append("\n");
        if (signalObj == null) {
            sb.append("- 无数据\n\n");
            return sb.toString();
        }
        Map<String, Object> signal = null;
        if (signalObj instanceof Map) {
            signal = (Map<String, Object>) signalObj;
        } else {
            // 支持信号为JSON字符串
            try {
                Gson gson = new Gson();
                Type t = new TypeToken<Map<String, Object>>(){}.getType();
                signal = gson.fromJson(signalObj.toString(), t);
            } catch (Exception e) {
                sb.append("- 数据格式错误\n\n");
                return sb.toString();
            }
        }
        // 信号
        sb.append("- **信号：** ").append(signalToChinese(signal.getOrDefault("signal", "无数据"))).append("\n");
        // 置信度
        Object conf = signal.get("confidence");
        String confStr = "0%";
        if (conf instanceof Number) {
            confStr = String.format("%.0f%%", ((Number)conf).doubleValue() * 100);
        } else if (conf != null) {
            try {
                confStr = String.format("%.0f%%", Double.parseDouble(conf.toString()) * 100);
            } catch (Exception e) { confStr = "0%"; }
        }
        sb.append("- **置信度：** ").append(confStr).append("\n");
        // 理由
        String reason = null;
        if ("macro".equals(type)) {
            // 宏观分析师理由字段特殊
            Object analysisObj = signal.get("analysis");
            if (analysisObj instanceof Map) {
                Object reasoningObj = ((Map<?, ?>) analysisObj).get("reasoning");
                if (reasoningObj != null) reason = reasoningObj.toString();
            } else if (analysisObj instanceof String) {
                try {
                    Gson gson = new Gson();
                    Type t = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> analysisMap = gson.fromJson((String) analysisObj, t);
                    Object reasoningObj = analysisMap.get("reasoning");
                    if (reasoningObj != null) reason = reasoningObj.toString();
                } catch (Exception e) { /* ignore */ }
            }
        } else if ("risk".equals(type)) {
            reason = signal.get("analysis") != null ? signal.get("analysis").toString() : null;
            if (reason == null && signal.get("reasoning") != null) reason = signal.get("reasoning").toString();
        } else {
            reason = signal.get("analysis") != null ? signal.get("analysis").toString() : null;
            if (reason == null && signal.get("thesis") != null) reason = signal.get("thesis").toString();
        }
        if (reason != null && !reason.isEmpty()) {
            sb.append("- **理由：** ").append(reason).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }
}
