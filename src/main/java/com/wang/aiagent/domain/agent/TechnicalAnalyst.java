package com.wang.aiagent.domain.agent;

import lombok.Data;

@Data
public class TechnicalAnalyst {
    private Long analystId;//分析id
    private String technicalContent;//技术分析师内容
    private String trendFollowing;//趋势跟踪
    private String meanReversion;//均值回归
    private String momentum;//动量
    private String volatilityAnalysis;//波动率分析
    private String statisticalArbitrageSignals;//统计套利信号
    private Double confidence; // 信心度
    private String signals; // 信号列表，逗号分隔

}
