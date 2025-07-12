package com.wang.aiagent.service.agent;

import com.wang.aiagent.domain.agent.TechnicalAnalyst;
import com.wang.aiagent.domain.vo.CryptoCoinVo;

import java.util.Map;

/**
 * 技术分析agent
 */
public interface TechnicalAnalystService {
    Map<String, Object> InsertTechnicalAnalyst(CryptoCoinVo cryptoCoinVo);

}
