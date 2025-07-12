package com.wang.aiagent.service.agent;

import java.util.Map;

public interface ValuationAnalystService {
    /**
     * 分析估值
     * @param symbol
     * @return
     **/
    Map<String, Object> analyzeValuation(String symbol);
}
