package com.wang.aiagent.service.agent;

import java.util.Map;

/**
 * 基本面分析agent
 */
public interface FundamentalsAnalystService {
    /**
     * 基本面分析入口
     * @param symbol 币种符号（如 btc, eth）
     * @return 基本面分析报告
     */
    Map<String, Object> analyzeFundamentals(String symbol);
}
