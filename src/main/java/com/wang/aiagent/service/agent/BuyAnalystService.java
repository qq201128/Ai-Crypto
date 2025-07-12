package com.wang.aiagent.service.agent;

import java.util.Map;

public interface BuyAnalystService {

    /**
     * 聚合多分析师信号，生成投资论点和置信度
     * @param symbol 币种符号
     * @param interval K线周期
     * @param limit K线数量
     * @return 聚合分析结果
     */
    Map<String, Object> analyzeBuyOpportunity(String symbol, String interval, int limit);
}
