package com.wang.aiagent.service.agent.impl;

import com.wang.aiagent.chat.service.impl.SiliconFlowAIServiceImpl;
import com.wang.aiagent.service.agent.MacroNewsAnalystService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class MacroNewsAnalystServiceImpl implements MacroNewsAnalystService {
    private final SiliconFlowAIServiceImpl siliconFlowAIService;
    private final RestTemplate restTemplate;

    @Override
    public Map<String, Object> getMacroNews() {
        String url = "https://api.panewslab.com/webapi/flashnews?rn=20&lid=1&apppush=0";
        List<Map<String, String>> newsList = new ArrayList<>();
        String errorMsg = null;
        try {
            // RestTemplate请求
            Map map = restTemplate.getForObject(url, Map.class);
            if (map != null && map.get("data") != null) {
                Map data = (Map) map.get("data");
                List flashNews = (List) data.get("flashNews");
                if (flashNews != null && !flashNews.isEmpty()) {
                    Map first = (Map) flashNews.get(0);
                    List list = (List) first.get("list");
                    for (Object item : list) {
                        Map news = (Map) item;
                        String title = news.getOrDefault("title", "").toString();
                        String desc = news.getOrDefault("desc", "").toString();
                        String publishTime = news.getOrDefault("publishTime", "").toString();
                        Map<String, String> newsMap = new HashMap<>();
                        newsMap.put("title", title);
                        newsMap.put("desc", desc);
                        newsMap.put("publishTime", publishTime);
                        newsList.add(newsMap);
                    }
                }
            } else {
                errorMsg = "PANews接口请求失败: 无数据";
            }
        } catch (Exception e) {
            errorMsg = "PANews接口异常: " + e.getMessage();
        }
        // 2. 组装新闻数据为JSON字符串
        Gson gson = new Gson();
        String newsJson = gson.toJson(newsList);
        // 3. 调用大模型分析
        String macroNewsAnalysis = siliconFlowAIService.macroNewsChat(newsJson);
        // 4. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("newsList", newsList);
        result.put("analysis", macroNewsAnalysis);
        if (errorMsg != null) {
            result.put("error", errorMsg);
        }
        // 智能置信度算法
        double confidence = 0.2;
        if (result.containsKey("newsList") && result.get("newsList") instanceof List) {
            List<?> newsList1 = (List<?>) result.get("newsList");
            int total = newsList1.size();
            int positive = 0;
            for (Object newsObj : newsList1) {
                if (newsObj instanceof Map) {
                    Map<?, ?> newsMap = (Map<?, ?>) newsObj;
                    Object titleObj = newsMap.get("title");
                    String title = titleObj != null ? titleObj.toString().toLowerCase() : "";
                    if (title.contains("涨") || title.contains("rise") || title.contains("突破") || title.contains("record high") || title.contains("bull")) positive++;
                }
            }
            if (total > 0) confidence = Math.min(1.0, 0.2 + 0.6 * ((double) positive / total) + 0.2 * Math.log10(total + 1));
        }
        result.put("confidence", confidence);
        if (!result.containsKey("signal")) result.put("signal", "neutral");
        return result;
    }
}
