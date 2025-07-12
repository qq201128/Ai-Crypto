package com.wang.aiagent.service.agent;

import java.util.Map;

public interface PortfolioManagementAnalystService {
    Map<String,Object> getPortfolioManagement(String symbol, String interval, int limit, Map<String, Object> portfolio);
}
