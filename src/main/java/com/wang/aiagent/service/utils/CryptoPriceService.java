package com.wang.aiagent.service.utils;

import com.wang.aiagent.domain.CoinGeckoCoin;
import com.wang.aiagent.domain.CryptoKline;
import com.wang.aiagent.domain.MarketCoin;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.net.InetSocketAddress;
import java.net.Proxy;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public interface CryptoPriceService {
    //获取到币种的k线数据
    List<CryptoKline> getCryptoKline(String symbol, String interval, int limit);
    //获取到币种的价格数据
    Map<String, Object> getCryptoPrice(String symbol);
    // 获取 CoinGecko 币种列表
    List<CoinGeckoCoin> getCoinGeckoCoinList(boolean includePlatform);
    // 获取 CoinGecko 币种市场行情
    List<MarketCoin> getCoinMarkets(String symbol);
    // 获取tradingview的概况信息
    Object getTradingViewOverview();
    // 获取tradingview的表现信息
    Object getTradingViewPerformance();
    // 获取tradingview的估值信息
    Object getTradingViewValuation();
    // 获取tradingview的交易信息
    Object getTradingViewTransaction();
    // 获取tradingview的情绪信息
    Object getTradingViewEmotion();
    // 获取tradingview的技术指标信息
    Object getTradingViewTechnicalIndex();
}