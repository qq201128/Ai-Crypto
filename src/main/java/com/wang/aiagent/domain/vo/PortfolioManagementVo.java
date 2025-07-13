package com.wang.aiagent.domain.vo;

import lombok.Data;

import java.util.Map;
@Data
public class PortfolioManagementVo {
    private String symbol;
    private String interval;
    private int limit;
    private Map<String, Object> portfolio;
}
