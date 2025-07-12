package com.wang.aiagent.service.agent;

import java.util.Map;

public interface EmotionAnalystService {
    /**
     * 分析情绪
     * @param symbol
     * @return
     **/
    Map<String, Object> analyzeEmotion(String symbol);
}
