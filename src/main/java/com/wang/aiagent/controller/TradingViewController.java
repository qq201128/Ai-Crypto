package com.wang.aiagent.controller;

import com.wang.aiagent.service.utils.CryptoPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tradingview")
public class TradingViewController {
    private final RestTemplate restTemplate;
    private final CryptoPriceService cryptoPriceService;

    /**
     * 获取tradingview的概况信息
     * @return
     */
    @GetMapping("/coin/getTradingViewOverview")
    public Object getTradingViewOverview() {
        return cryptoPriceService.getTradingViewOverview();
    }

    /**
     * 获取tradingview的表现信息
     * @return
     */
    @GetMapping("/coin/getTradingViewPerformance")
    public Object getTradingViewPerformance() {
        return cryptoPriceService.getTradingViewPerformance();
    }
    /**
     * 获取tradingview的估值信息
     * @return
     */
    @GetMapping("/coin/getTradingViewValuation")
    public Object getTradingViewValuation() {
        return cryptoPriceService.getTradingViewValuation();
    }
    /**
     * 获取tradingview的交易信息
     * @return
     */
    @GetMapping("/coin/getTradingViewTransaction")
    public Object getTradingViewTransaction() {
        return cryptoPriceService.getTradingViewTransaction();
    }
    /**
     * 获取tradingview的情绪信息
     * @return
     */
    @GetMapping("/coin/getTradingViewEmotion")
    public Object getTradingViewEmotion() {
        return cryptoPriceService.getTradingViewEmotion();
    }
    /**
     * 获取tradingview的技术指标信息
     * @return
     */
    @GetMapping("/coin/getTradingViewTechnicalIndex")
    public Object getTradingViewTechnicalIndex() {
        return cryptoPriceService.getTradingViewTechnicalIndex();
    }


} 