package com.wang.aiagent.service.agent.impl;

import com.wang.aiagent.domain.vo.CryptoEmotionVo;
import com.wang.aiagent.service.agent.EmotionAnalystService;
import com.wang.aiagent.service.utils.CryptoPriceService;
import com.wang.aiagent.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmotionAnalystServiceImpl implements EmotionAnalystService {
    private final CryptoPriceService cryptoPriceService;
    /**
     * 分析情绪
     * @param symbol
     * @return
     **/
    @Override
    public Map<String, Object> analyzeEmotion(String symbol) {
        symbol = StringUtils.extractSymbol(symbol);
        // 获取所有币种的情绪数据
        var emotionList = cryptoPriceService.getTradingViewEmotion();
        if (emotionList == null) return null;
        // 查找目标币种
        CryptoEmotionVo target = null;
        for (Object obj : (List<?>)emotionList) {
            if (obj instanceof CryptoEmotionVo) {
                CryptoEmotionVo vo = (CryptoEmotionVo) obj;
                if (symbol.equalsIgnoreCase(vo.getBaseCurrency())) {
                    target = vo;
                    break;
                }
            }
        }
        if (target == null) return null;
        // 解析各项情绪指标
        double sentiment = 0.0;
        try { sentiment = Double.parseDouble(target.getSentiment()); } catch (Exception ignored) {}
        double galaxyscore = 0.0;
        try { galaxyscore = Double.parseDouble(target.getGalaxyscore()); } catch (Exception ignored) {}
        double socialDominance = 0.0;
        try { socialDominance = Double.parseDouble(target.getSocialdominance()); } catch (Exception ignored) {}
        double tweets = 0.0;
        try { tweets = Double.parseDouble(target.getTweets()); } catch (Exception ignored) {}
        double githubCommits = 0.0;
        try { githubCommits = Double.parseDouble(target.getGithubCommits()); } catch (Exception ignored) {}
        // 归一化处理
        double normSentiment = Math.min(Math.max(sentiment / 100.0, 0), 1);
        double normGalaxy = Math.min(Math.max(galaxyscore / 100.0, 0), 1);
        double normSocial = Math.min(Math.max(socialDominance / 100.0, 0), 1);
        double normTweets = Math.min(Math.max(tweets / 10000.0, 0), 1); // 假设1w为高
        double normGithub = Math.min(Math.max(githubCommits / 100.0, 0), 1); // 假设100为高
        // 信号判定
        String sentimentSignal = normSentiment > 0.6 ? "bullish" : (normSentiment < 0.4 ? "bearish" : "neutral");
        String galaxySignal = normGalaxy > 0.6 ? "bullish" : (normGalaxy < 0.4 ? "bearish" : "neutral");
        String socialSignal = normSocial > 0.6 ? "bullish" : (normSocial < 0.4 ? "bearish" : "neutral");
        String tweetsSignal = normTweets > 0.6 ? "bullish" : (normTweets < 0.4 ? "bearish" : "neutral");
        String githubSignal = normGithub > 0.6 ? "bullish" : (normGithub < 0.4 ? "bearish" : "neutral");
        // 组合信号加权
        double weightedSum = normSentiment * 0.4 + normGalaxy * 0.2 + normSocial * 0.15 + normTweets * 0.15 + normGithub * 0.1;
        String overallSignal = weightedSum > 0.6 ? "bullish" : (weightedSum < 0.4 ? "bearish" : "neutral");
        double confidence = weightedSum;
        // 详细分项
        Map<String, Object> strategySignals = new LinkedHashMap<>();
        Map<String, Object> sentimentMap = new HashMap<>();
        sentimentMap.put("signal", sentimentSignal);
        sentimentMap.put("confidence", normSentiment);
        sentimentMap.put("metrics", Map.of("sentiment", sentiment));
        strategySignals.put("sentiment", sentimentMap);
        Map<String, Object> galaxyMap = new HashMap<>();
        galaxyMap.put("signal", galaxySignal);
        galaxyMap.put("confidence", normGalaxy);
        galaxyMap.put("metrics", Map.of("galaxyscore", galaxyscore));
        strategySignals.put("galaxy", galaxyMap);
        Map<String, Object> socialMap = new HashMap<>();
        socialMap.put("signal", socialSignal);
        socialMap.put("confidence", normSocial);
        socialMap.put("metrics", Map.of("socialdominance", socialDominance));
        strategySignals.put("social_dominance", socialMap);
        Map<String, Object> tweetsMap = new HashMap<>();
        tweetsMap.put("signal", tweetsSignal);
        tweetsMap.put("confidence", normTweets);
        tweetsMap.put("metrics", Map.of("tweets", tweets));
        strategySignals.put("tweets", tweetsMap);
        Map<String, Object> githubMap = new HashMap<>();
        githubMap.put("signal", githubSignal);
        githubMap.put("confidence", normGithub);
        githubMap.put("metrics", Map.of("github_commits", githubCommits));
        strategySignals.put("github_commits", githubMap);
        // 汇总
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("signal", overallSignal);
        result.put("confidence", confidence);
        result.put("strategy_signals", strategySignals);
        result.put("基础信息", target);
        return result;
    }
}
