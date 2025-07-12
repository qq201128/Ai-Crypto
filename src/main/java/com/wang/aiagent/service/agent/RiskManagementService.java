package com.wang.aiagent.service.agent;

import java.util.Map;

public interface RiskManagementService {
    /**
     * 风险管理Agent：评估市场和投资风险，给出风险调整后的建议和持仓限制
     * @param symbol 币种符号
     * @param interval K线周期
     * @param limit K线数量
     * @param portfolio 当前持仓信息（Map，包含cash/stock等）
     * @return 风险分析结果
     */
    Map<String, Object> analyzeRiskManagement(String symbol, String interval, int limit, Map<String, Object> portfolio);
}
