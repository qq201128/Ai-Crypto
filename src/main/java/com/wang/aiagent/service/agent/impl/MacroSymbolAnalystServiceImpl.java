package com.wang.aiagent.service.agent.impl;

import com.wang.aiagent.chat.service.impl.SiliconFlowAIServiceImpl;
import com.wang.aiagent.service.agent.MacroSymbolAnalystService;
import com.wang.aiagent.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Slf4j
@RequiredArgsConstructor
public class MacroSymbolAnalystServiceImpl implements MacroSymbolAnalystService {
    private final SiliconFlowAIServiceImpl siliconFlowAIService;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Map<String, Object> getMacroSymbolNews(String symbol) {
        symbol = StringUtils.extractSymbol(symbol);
        String url = "https://api-v2.panewslab.com/search";

        List<Map<String, String>> newsList = new ArrayList<>();
        String errorMsg = null;
        try {
            // 构造请求参数
            Map<String, Object> param = new HashMap<>();
            List<String> queryList = new ArrayList<>();
            queryList.add(symbol);
            param.put("query", queryList);
            param.put("orderBy", "datetime");
            List<String> types = new ArrayList<>();
            types.add("articles");
            param.put("types", types);
            param.put("skip", 0);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(param, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String bodyStr = response.getBody();

            if (bodyStr == null){
                throw new RuntimeException("API响应为空");
            }
            JsonElement rootElement = JsonParser.parseString(bodyStr);
            if (!rootElement.isJsonArray()){
                throw new RuntimeException("API响应不是数组");
            }
            JsonArray arr = rootElement.getAsJsonArray();
            for (JsonElement elem : arr) {
                if (!elem.isJsonObject()) {
                    continue;
                }
                JsonObject article = elem.getAsJsonObject();
                String title = article.has("title") ? article.get("title").getAsString() : "";
                String description = article.has("description") ? article.get("description").getAsString() : "";
                Map<String, String> newsMap = new HashMap<>();
                newsMap.put("title", title);
                newsMap.put("description", description);
                newsList.add(newsMap);

            }
        } catch (Exception e) {
            errorMsg = "PANews接口异常: " + e.getMessage();
        }

        Gson gson = new Gson();
        String newsJson = gson.toJson(newsList);
        String macroSymbolNewsAnalysis = siliconFlowAIService.macroSymbolNewsChat(newsJson);
        log.info("大模型返回的analysis: {}", macroSymbolNewsAnalysis);
        Map<String, Object> result = new HashMap<>();
        result.put("newsList", newsList);
        result.put("analysis", macroSymbolNewsAnalysis);
        if (errorMsg != null) {
            result.put("error", errorMsg);
        }
        return result;
    }
}
