package com.wang.aiagent.service.utils.impl;

import com.wang.aiagent.domain.CryptoKline;
import com.wang.aiagent.domain.vo.CryptoOverviewVo;
import com.wang.aiagent.mapper.CoinGeckoCoinMapper;
import com.wang.aiagent.service.utils.CryptoPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.wang.aiagent.domain.CoinGeckoCoin;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.wang.aiagent.domain.MarketCoin;
import com.wang.aiagent.domain.vo.CryptoEmotionVo;

import static java.net.Proxy.Type.HTTP;

@RequiredArgsConstructor
@Service
public class CryptoPriceServiceImpl implements CryptoPriceService {
    private final RestTemplate restTemplate;
    private static final String BINANCE_API_URL = "https://api.binance.com/api/v3/ticker/price?symbol=%s";
    private static final String BINANCE_KLINE_API_URL = "https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&limit=%d";
    private final CoinGeckoCoinMapper coinGeckoCoinMapper;
    public Map<String, Object> getCryptoPrice(String symbol) {
        String url = String.format(BINANCE_API_URL, symbol.toUpperCase());
        Map<String, Object> result = new HashMap<>();

        Map response = restTemplate.getForObject(url, Map.class);
        return response;


    }

    public List<CryptoKline> getCryptoKline(String symbol, String interval, int limit) {
        String url = String.format(BINANCE_KLINE_API_URL, symbol.toUpperCase(), interval, limit);
        Map<String, Object> result = new HashMap<>();

        List<List<Object>> raw = restTemplate.getForObject(url, List.class);
        List<CryptoKline> klineList = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        for (List<Object> arr : raw) {
            CryptoKline cryptoKline = new CryptoKline();
            long openTime = ((Number) arr.get(0)).longValue();
            long closeTime = ((Number) arr.get(6)).longValue();
            cryptoKline.setOpenTime(openTime);
            cryptoKline.setOpen((String) arr.get(1));
            cryptoKline.setHigh((String) arr.get(2));
            cryptoKline.setLow((String) arr.get(3));
            cryptoKline.setClose((String) arr.get(4));
            cryptoKline.setVolume((String) arr.get(5));
            cryptoKline.setCloseTime(closeTime);
            cryptoKline.setQuoteAssetVolume((String) arr.get(7));
            cryptoKline.setNumberOfTrades(((Number) arr.get(8)).intValue());
            cryptoKline.setTakerBuyBaseAssetVolume((String) arr.get(9));
            cryptoKline.setTakerBuyQuoteAssetVolume((String) arr.get(10));
            cryptoKline.setIgnore(arr.get(11).toString());
            cryptoKline.setOpenTimeStr(fmt.format(Instant.ofEpochMilli(openTime)));
            cryptoKline.setCloseTimeStr(fmt.format(Instant.ofEpochMilli(closeTime)));
            klineList.add(cryptoKline);
        }
        return klineList;


    }

    @Override
    public List<CoinGeckoCoin> getCoinGeckoCoinList(boolean includePlatform) {
        String url = "https://api.coingecko.com/api/v3/coins/list?include_platform=" + includePlatform;
        Proxy proxy = new Proxy(HTTP, new InetSocketAddress("127.0.0.1", 10809));
        OkHttpClient client = new OkHttpClient.Builder()
            .proxy(proxy)
            .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("x-cg-demo-api-key", "CG-a7QiLuMdSd1PYv4xKw95RKKR")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                Gson gson = new Gson();
                List<CoinGeckoCoin> coinList = gson.fromJson(json, new TypeToken<List<CoinGeckoCoin>>(){}.getType());
                // 先全表删除
                coinGeckoCoinMapper.delete(null);
                // 批量插入
                if (coinList != null && !coinList.isEmpty()) {
                    coinGeckoCoinMapper.insertBatch(coinList);
                }
                return coinList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public List<MarketCoin> getCoinMarkets(String symbol) {
        String url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&symbols=" +symbol;

        Proxy proxy = new Proxy(HTTP, new InetSocketAddress("127.0.0.1", 10809));
        OkHttpClient client = new OkHttpClient.Builder().proxy(proxy).build();
        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("accept", "application/json")
            .addHeader("x-cg-demo-api-key", "CG-a7QiLuMdSd1PYv4xKw95RKKR")
            .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                Gson gson = new Gson();
                return gson.fromJson(json, new TypeToken<List<MarketCoin>>(){}.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public List<CryptoOverviewVo> getTradingViewOverview() {
        String url = "https://scanner.tradingview.com/coin/scan?label-product=screener-coin";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set("Origin", "https://cn.tradingview.com");
        headers.set("Referer", "https://cn.tradingview.com/");
        headers.set("Accept", "application/json");
        headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.set("Accept-Language", "zh-CN,zh-TW;q=0.9,zh;q=0.8,en;q=0.7,zh-HK;q=0.6");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
        String body = "{\"columns\":[\"base_currency\",\"base_currency_desc\",\"base_currency_logoid\",\"update_mode\",\"type\",\"typespecs\",\"exchange\",\"crypto_total_rank\",\"close\",\"pricescale\",\"minmov\",\"fractional\",\"minmove2\",\"currency\",\"24h_close_change|5\",\"market_cap_calc\",\"fundamental_currency_code\",\"24h_vol_cmc\",\"circulating_supply\",\"24h_vol_to_market_cap\",\"socialdominance\",\"crypto_common_categories.tr\",\"Recommend.All\"],\"ignore_unknown_fields\":false,\"options\":{\"lang\":\"zh\"},\"range\":[0,2000],\"sort\":{\"sortBy\":\"crypto_total_rank\",\"sortOrder\":\"asc\"},\"symbols\":{},\"markets\":[\"coin\"]}";

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
        List<String> encoding = response.getHeaders().get("Content-Encoding");
        byte[] responseBody = response.getBody();
        String result;
        if (encoding != null && encoding.stream().anyMatch(e -> e.contains("gzip"))) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(responseBody));
                 InputStreamReader reader = new InputStreamReader(gis, StandardCharsets.UTF_8);
                 BufferedReader in = new BufferedReader(reader)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                result = sb.toString();
            } catch (IOException e) {
                throw new RuntimeException("解压GZIP失败", e);
            }
        } else {
            result = new String(responseBody, StandardCharsets.UTF_8);
        }
        // 解析JSON并转换为VO列表
        List<CryptoOverviewVo> voList = new ArrayList<>();
        try {
            Gson gson = new Gson();
            Map<String, Object> map = gson.fromJson(result, Map.class);
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) map.get("data");
            for (Map<String, Object> item : dataList) {
                List<Object> d = (List<Object>) item.get("d");
                CryptoOverviewVo vo = new CryptoOverviewVo();
                vo.setBaseCurrency((String) d.get(0));
                vo.setBaseCurrencyDesc((String) d.get(1));
                vo.setBaseCurrencyLogoid((String) d.get(2));
                vo.setUpdateMode((String) d.get(3));
                vo.setType((String) d.get(4));
                vo.setTypespecs(d.get(5) != null ? d.get(5).toString() : null);
                vo.setExchange((String) d.get(6));
                vo.setCryptoTotalRank(d.get(7) != null ? ((Number) d.get(7)).intValue() : null);
                vo.setClose(d.get(8) != null ? ((Number) d.get(8)).doubleValue() : null);
                vo.setPricescale(d.get(9) != null ? ((Number) d.get(9)).intValue() : null);
                vo.setMinmov(d.get(10) != null ? ((Number) d.get(10)).intValue() : null);
                vo.setFractional((String) d.get(11));
                vo.setMinmove2(d.get(12) != null ? ((Number) d.get(12)).intValue() : null);
                vo.setCurrency((String) d.get(13));
                vo.setCloseChange24h5(d.get(14) != null ? ((Number) d.get(14)).doubleValue() : null);
                vo.setMarketCapCalc(d.get(15) != null ? ((Number) d.get(15)).doubleValue() : null);
                vo.setFundamentalCurrencyCode((String) d.get(16));
                vo.setVol24hCmc(d.get(17) != null ? ((Number) d.get(17)).doubleValue() : null);
                vo.setCirculatingSupply(d.get(18) != null ? ((Number) d.get(18)).doubleValue() : null);
                vo.setVolToMarketCap24h(d.get(19) != null ? ((Number) d.get(19)).doubleValue() : null);
                vo.setSocialDominance(d.get(20) != null ? ((Number) d.get(20)).doubleValue() : null);
                vo.setCryptoCommonCategoriesTr(d.get(21) != null ? d.get(21).toString() : null);
                vo.setRecommendAll(d.get(22) != null ? d.get(22).toString() : null);
                voList.add(vo);
            }
        } catch (Exception e) {
            throw new RuntimeException("解析TradingView返回数据失败", e);
        }
        return voList;
    }

    @Override
    public Object getTradingViewPerformance() {
        String url = "https://scanner.tradingview.com/coin/scan?label-product=screener-coin";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set("Origin", "https://cn.tradingview.com");
        headers.set("Referer", "https://cn.tradingview.com/");
        headers.set("Accept", "application/json");
        headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.set("Accept-Language", "zh-CN,zh-TW;q=0.9,zh;q=0.8,en;q=0.7,zh-HK;q=0.6");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
        String body = "{\"columns\":[\"base_currency\",\"base_currency_desc\",\"base_currency_logoid\",\"update_mode\",\"type\",\"typespecs\",\"exchange\",\"crypto_total_rank\",\"market_cap_calc\",\"fundamental_currency_code\",\"24h_close_change|5\",\"Perf.W\",\"Perf.1M\",\"Perf.3M\",\"Perf.6M\",\"Perf.YTD\",\"Perf.Y\",\"Perf.5Y\",\"Perf.10Y\",\"Perf.All\",\"Volatility.D\"],\"ignore_unknown_fields\":false,\"options\":{\"lang\":\"zh\"},\"range\":[0,2000],\"sort\":{\"sortBy\":\"crypto_total_rank\",\"sortOrder\":\"asc\"},\"symbols\":{},\"markets\":[\"coin\"]}";

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
        List<String> encoding = response.getHeaders().get("Content-Encoding");
        byte[] responseBody = response.getBody();
        String result;
        if (encoding.stream().anyMatch(e -> e.contains("gzip"))) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(responseBody));
                 InputStreamReader reader = new InputStreamReader(gis, StandardCharsets.UTF_8);
                 BufferedReader in = new BufferedReader(reader)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                result = sb.toString();
            } catch (IOException e) {
                throw new RuntimeException("解压GZIP失败", e);
            }
        } else {
            result = new String(responseBody, StandardCharsets.UTF_8);
        }
        return ResponseEntity.status(response.getStatusCode()).body(result);
    }

    @Override
    public Object getTradingViewValuation() {
        String url = "https://scanner.tradingview.com/coin/scan?label-product=screener-coin";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set("Origin", "https://cn.tradingview.com");
        headers.set("Referer", "https://cn.tradingview.com/");
        headers.set("Accept", "application/json");
        headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.set("Accept-Language", "zh-CN,zh-TW;q=0.9,zh;q=0.8,en;q=0.7,zh-HK;q=0.6");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
        String body = "{\"columns\":[\"base_currency\",\"base_currency_desc\",\"base_currency_logoid\",\"update_mode\",\"type\",\"typespecs\",\"exchange\",\"crypto_total_rank\",\"close\",\"pricescale\",\"minmov\",\"fractional\",\"minmove2\",\"currency\",\"market_cap_calc\",\"fundamental_currency_code\",\"market_cap_diluted_calc\",\"circulating_supply\",\"total_supply\",\"24h_vol_to_market_cap\",\"nvt\",\"velocity\"],\"ignore_unknown_fields\":false,\"options\":{\"lang\":\"zh\"},\"range\":[0,2000],\"sort\":{\"sortBy\":\"crypto_total_rank\",\"sortOrder\":\"asc\"},\"symbols\":{},\"markets\":[\"coin\"]}\n";

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
        List<String> encoding = response.getHeaders().get("Content-Encoding");
        byte[] responseBody = response.getBody();
        String result;
        if (encoding.stream().anyMatch(e -> e.contains("gzip"))) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(responseBody));
                 InputStreamReader reader = new InputStreamReader(gis, StandardCharsets.UTF_8);
                 BufferedReader in = new BufferedReader(reader)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                result = sb.toString();
            } catch (IOException e) {
                throw new RuntimeException("解压GZIP失败", e);
            }
        } else {
            result = new String(responseBody, StandardCharsets.UTF_8);
        }
        return ResponseEntity.status(response.getStatusCode()).body(result);
    }

    @Override
    public Object getTradingViewTransaction() {
        String url = "https://scanner.tradingview.com/coin/scan?label-product=screener-coin";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set("Origin", "https://cn.tradingview.com");
        headers.set("Referer", "https://cn.tradingview.com/");
        headers.set("Accept", "application/json");
        headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.set("Accept-Language", "zh-CN,zh-TW;q=0.9,zh;q=0.8,en;q=0.7,zh-HK;q=0.6");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
        String body = "{\"columns\":[\"base_currency\",\"base_currency_desc\",\"base_currency_logoid\",\"update_mode\",\"type\",\"typespecs\",\"exchange\",\"crypto_total_rank\",\"market_cap_calc\",\"fundamental_currency_code\",\"txs_count\",\"txs_volume\",\"txs_volume_usd\",\"average_transaction_usd\",\"large_tx_count\",\"large_tx_volume_usd\"],\"ignore_unknown_fields\":false,\"options\":{\"lang\":\"zh\"},\"range\":[0,2000],\"sort\":{\"sortBy\":\"crypto_total_rank\",\"sortOrder\":\"asc\"},\"symbols\":{},\"markets\":[\"coin\"]}";

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
        List<String> encoding = response.getHeaders().get("Content-Encoding");
        byte[] responseBody = response.getBody();
        String result;
        if (encoding.stream().anyMatch(e -> e.contains("gzip"))) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(responseBody));
                 InputStreamReader reader = new InputStreamReader(gis, StandardCharsets.UTF_8);
                 BufferedReader in = new BufferedReader(reader)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                result = sb.toString();
            } catch (IOException e) {
                throw new RuntimeException("解压GZIP失败", e);
            }
        } else {
            result = new String(responseBody, StandardCharsets.UTF_8);
        }
        return ResponseEntity.status(response.getStatusCode()).body(result);
    }

    @Override
    public List<CryptoEmotionVo> getTradingViewEmotion() {
        String url = "https://scanner.tradingview.com/coin/scan?label-product=screener-coin";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set("Origin", "https://cn.tradingview.com");
        headers.set("Referer", "https://cn.tradingview.com/");
        headers.set("Accept", "application/json");
        headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.set("Accept-Language", "zh-CN,zh-TW;q=0.9,zh;q=0.8,en;q=0.7,zh-HK;q=0.6");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
        String body = "{\"columns\":[\"base_currency\",\"base_currency_desc\",\"base_currency_logoid\",\"update_mode\",\"type\",\"typespecs\",\"exchange\",\"crypto_total_rank\",\"market_cap_calc\",\"fundamental_currency_code\",\"altrank\",\"galaxyscore\",\"socialdominance\",\"sentiment\",\"social_volume_24h\",\"tweets\",\"github_commits\"],\"ignore_unknown_fields\":false,\"options\":{\"lang\":\"zh\"},\"range\":[0,2000],\"sort\":{\"sortBy\":\"crypto_total_rank\",\"sortOrder\":\"asc\"},\"symbols\":{},\"markets\":[\"coin\"]}\n";

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
        List<String> encoding = response.getHeaders().get("Content-Encoding");
        byte[] responseBody = response.getBody();
        String result;
        if (encoding != null && encoding.stream().anyMatch(e -> e.contains("gzip"))) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(responseBody));
                 InputStreamReader reader = new InputStreamReader(gis, StandardCharsets.UTF_8);
                 BufferedReader in = new BufferedReader(reader)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                result = sb.toString();
            } catch (IOException e) {
                throw new RuntimeException("解压GZIP失败", e);
            }
        } else {
            result = new String(responseBody, StandardCharsets.UTF_8);
        }
        // 解析JSON并转换为VO列表
        List<CryptoEmotionVo> voList = new ArrayList<>();
        try {
            Gson gson = new Gson();
            Map<String, Object> map = gson.fromJson(result, Map.class);
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) map.get("data");
            for (Map<String, Object> item : dataList) {
                List<Object> d = (List<Object>) item.get("d");
                CryptoEmotionVo vo = new CryptoEmotionVo();
                vo.setBaseCurrency((String) d.get(0));
                vo.setBaseCurrencyDesc((String) d.get(1));
                vo.setBaseCurrencyLogoid((String) d.get(2));
                vo.setUpdateMode((String) d.get(3));
                vo.setType((String) d.get(4));
                vo.setTypespecs(d.get(5) != null ? d.get(5).toString() : null);
                vo.setExchange((String) d.get(6));
                vo.setCryptoTotalRank(d.get(7) != null ? d.get(7).toString() : null);
                vo.setMarketCapCalc(d.get(8) != null ? d.get(8).toString() : null);
                vo.setFundamentalCurrencyCode((String) d.get(9));
                vo.setAltrank(d.get(10) != null ? d.get(10).toString() : null);
                vo.setGalaxyscore(d.get(11) != null ? d.get(11).toString() : null);
                vo.setSocialdominance(d.get(12) != null ? d.get(12).toString() : null);
                vo.setSentiment(d.get(13) != null ? d.get(13).toString() : null);
                vo.setSocialVolume24h(d.get(14) != null ? d.get(14).toString() : null);
                vo.setTweets(d.get(15) != null ? d.get(15).toString() : null);
                vo.setGithubCommits(d.get(16) != null ? d.get(16).toString() : null);
                voList.add(vo);
            }
        } catch (Exception e) {
            throw new RuntimeException("解析TradingView返回数据失败", e);
        }
        return voList;
    }

    @Override
    public Object getTradingViewTechnicalIndex() {
        String url = "https://scanner.tradingview.com/coin/scan?label-product=screener-coin";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set("Origin", "https://cn.tradingview.com");
        headers.set("Referer", "https://cn.tradingview.com/");
        headers.set("Accept", "application/json");
        headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.set("Accept-Language", "zh-CN,zh-TW;q=0.9,zh;q=0.8,en;q=0.7,zh-HK;q=0.6");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
        String body = "{\"columns\":[\"base_currency\",\"base_currency_desc\",\"base_currency_logoid\",\"update_mode\",\"type\",\"typespecs\",\"exchange\",\"crypto_total_rank\",\"Recommend.All\",\"Recommend.MA\",\"Recommend.Other\",\"RSI\",\"Mom\",\"pricescale\",\"minmov\",\"fractional\",\"minmove2\",\"AO\",\"CCI20\",\"Stoch.K\",\"Stoch.D\",\"Candle.3BlackCrows\",\"Candle.3WhiteSoldiers\",\"Candle.AbandonedBaby.Bearish\",\"Candle.AbandonedBaby.Bullish\",\"Candle.Doji\",\"Candle.Doji.Dragonfly\",\"Candle.Doji.Gravestone\",\"Candle.Engulfing.Bearish\",\"Candle.Engulfing.Bullish\",\"Candle.EveningStar\",\"Candle.Hammer\",\"Candle.HangingMan\",\"Candle.Harami.Bearish\",\"Candle.Harami.Bullish\",\"Candle.InvertedHammer\",\"Candle.Kicking.Bearish\",\"Candle.Kicking.Bullish\",\"Candle.LongShadow.Lower\",\"Candle.LongShadow.Upper\",\"Candle.Marubozu.Black\",\"Candle.Marubozu.White\",\"Candle.MorningStar\",\"Candle.ShootingStar\",\"Candle.SpinningTop.Black\",\"Candle.SpinningTop.White\",\"Candle.TriStar.Bearish\",\"Candle.TriStar.Bullish\"],\"ignore_unknown_fields\":false,\"options\":{\"lang\":\"zh\"},\"range\":[0,2000],\"sort\":{\"sortBy\":\"crypto_total_rank\",\"sortOrder\":\"asc\"},\"symbols\":{},\"markets\":[\"coin\"]}\n";

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
        List<String> encoding = response.getHeaders().get("Content-Encoding");
        byte[] responseBody = response.getBody();
        String result;
        if (encoding.stream().anyMatch(e -> e.contains("gzip"))) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(responseBody));
                 InputStreamReader reader = new InputStreamReader(gis, StandardCharsets.UTF_8);
                 BufferedReader in = new BufferedReader(reader)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                }
                result = sb.toString();
            } catch (IOException e) {
                throw new RuntimeException("解压GZIP失败", e);
            }
        } else {
            result = new String(responseBody, StandardCharsets.UTF_8);
        }
        return ResponseEntity.status(response.getStatusCode()).body(result);
    }
}
