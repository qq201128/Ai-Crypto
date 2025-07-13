package com.wang.aiagent.controller;

import com.wang.aiagent.domain.CoinGeckoCoin;
import com.wang.aiagent.domain.CryptoKline;
import com.wang.aiagent.domain.agent.TechnicalAnalyst;
import com.wang.aiagent.domain.vo.CryptoCoinVo;
import com.wang.aiagent.domain.vo.PortfolioManagementVo;
import com.wang.aiagent.service.agent.*;
import com.wang.aiagent.service.utils.CryptoPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import com.wang.aiagent.domain.MarketCoin;

@RestController
@RequiredArgsConstructor
public class CryptoPriceController {
    private final CryptoPriceService cryptoPriceService;
    private final TechnicalAnalystService technicalAnalystService;
    private final FundamentalsAnalystService fundamentalsAnalystService;
    private final EmotionAnalystService emotionAnalystService;
    private final ValuationAnalystService valuationAnalystService;
    private final MacroNewsAnalystService macroNewsAnalystService;
    private final MacroSymbolAnalystService macroSymbolAnalystService;
    private final PortfolioManagementAnalystService portfolioManagementAnalystService;

    @GetMapping("/api/crypto/price")
    public Map<String, Object> getCryptoPrice(@RequestParam String symbol) {
        return cryptoPriceService.getCryptoPrice(symbol);
    }

    @GetMapping("/api/crypto/kline")
    public List<CryptoKline> getCryptoKline(@RequestParam String symbol,
                                            @RequestParam String interval,
                                            @RequestParam(required = false, defaultValue = "1") int limit) {
        return cryptoPriceService.getCryptoKline(symbol, interval, limit);
    }


    @GetMapping("/api/InsertTechnicalAnalyst")
    public  Map<String, Object> InsertTechnicalAnalyst(@RequestParam(value = "symbol") String symbol,
                                                   @RequestParam(value = "interval") String interval,
                                                   @RequestParam(value = "limit") int limit) {
        CryptoCoinVo cryptoCoinVo = new CryptoCoinVo();
        cryptoCoinVo.setSymbol(symbol);
        cryptoCoinVo.setTimeInterval(interval);
        cryptoCoinVo.setLimitSize(limit);
        return technicalAnalystService.InsertTechnicalAnalyst(cryptoCoinVo);
    }

    @GetMapping("/api/getCoinGeckoCoinList")
    public List<CoinGeckoCoin> getCoinGeckoCoinList(@RequestParam(value = "includePlatform") boolean includePlatform) {

        return cryptoPriceService.getCoinGeckoCoinList(includePlatform);
    }

    @GetMapping("/api/getMarkets")
    public List<MarketCoin> getMarkets(@RequestParam String symbol
    ) {
        return cryptoPriceService.getCoinMarkets(symbol);
    }
    @GetMapping("/api/analyzeFundamentals")
    public Object analyzeFundamentals(@RequestParam String symbol){
        return fundamentalsAnalystService.analyzeFundamentals(symbol);
    }
    @GetMapping("/api/analyzeEmotion")
    public Object analyzeEmotion(@RequestParam String symbol){
        return emotionAnalystService.analyzeEmotion(symbol);
    }


    @GetMapping("/api/analyzeValuation")
    public Object analyzeValuation(@RequestParam String symbol){
        return valuationAnalystService.analyzeValuation(symbol);
    }

    @GetMapping("/api/getMacroNews")
    public Object getMacroNews(){
        return macroNewsAnalystService.getMacroNews();
    }

    @GetMapping("/api/getMacroSymbolNews")
    public Object getMacroSymbolNews(@RequestParam String symbol){
        return macroSymbolAnalystService.getMacroSymbolNews(symbol);
    }

    @PostMapping("/api/getPortfolioManagement")
    public Object getPortfolioManagement(@RequestBody PortfolioManagementVo req) {
        return portfolioManagementAnalystService.getPortfolioManagement(
            req.getSymbol(), req.getInterval(), req.getLimit(), req.getPortfolio());
    }
} 