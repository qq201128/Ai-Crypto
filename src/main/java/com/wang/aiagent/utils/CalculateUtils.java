package com.wang.aiagent.utils;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.wang.aiagent.domain.CryptoKline;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CalculateUtils {
    public static Map<String,String> calculateMACD(List<CryptoKline> cryptoKlineList){
        //收盘价数组，去掉最后一个未收盘的K线
        double[] closePrices = new double[cryptoKlineList.size() - 1];
        for (int i = 0; i < cryptoKlineList.size() - 1; i++) {
            closePrices[i] = Double.parseDouble(cryptoKlineList.get(i).getClose());
        }

        int len = closePrices.length;

        double[] outMACD = new double[len];
        double[] outSignal = new double[len];
        double[] outHist = new double[len];
        MInteger outBegIdx = new MInteger();
        MInteger outNbElement = new MInteger();

        Core core = new Core();
        // 快速EMA周期=12, 慢速EMA周期=26, 信号线EMA周期=9
        core.macd(0, len - 1, closePrices, 12, 26, 9, outBegIdx, outNbElement, outMACD, outSignal, outHist);

        // 输出最后一个有效值
        int lastIdx = outBegIdx.value + outNbElement.value - 1;
        // 健壮查找最后一个非零有效点
        for (int i = lastIdx; i >= outBegIdx.value; i--) {
            if (outMACD[i] != 0.0 || outSignal[i] != 0.0 || outHist[i] != 0.0) {
                lastIdx = i;
                break;
            }
        }

//        System.out.println("MACD: " + outMACD[lastIdx]);
//        System.out.println("Signal: " + outSignal[lastIdx]);
//        System.out.println("Hist: " + outHist[lastIdx]);

        // 金叉/死叉/中性信号判定
        int prevIdx = lastIdx - 1;
        String signal;
        if (outMACD[prevIdx] < outSignal[prevIdx] && outMACD[lastIdx] > outSignal[lastIdx]) {
            signal = "bullish"; // 金叉
        } else if (outMACD[prevIdx] > outSignal[prevIdx] && outMACD[lastIdx] < outSignal[lastIdx]) {
            signal = "bearish"; // 死叉
        } else {
            signal = "neutral";
        }
//        System.out.println("MACD Signal: " + signal);

        Map<String,String> map = new HashMap<>();
        map.put("macd",String.valueOf(outMACD[lastIdx]));
        map.put("signal",String.valueOf(outSignal[lastIdx]));
        map.put("hist",String.valueOf(outHist[lastIdx]));
        map.put("macdSignal",signal);
        return map;
    }

    /**
     * 计算RSI指标
     * @param cryptoKlineList K线数据列表
     * @param period RSI周期，默认14
     * @return Map，包含最新rsi值（key: "rsi"），全部rsi序列（key: "rsiSeries"，逗号分隔字符串）
     */
    public static Map<String, String> calculateRSI(List<CryptoKline> cryptoKlineList, int period) {
        // 收盘价数组，去掉最后一个未收盘的K线
        double[] closePrices = new double[cryptoKlineList.size() - 1];
        for (int i = 0; i < cryptoKlineList.size() - 1; i++) {
            closePrices[i] = Double.parseDouble(cryptoKlineList.get(i).getClose());
        }
        int len = closePrices.length;
        double[] outRSI = new double[len];
        MInteger outBegIdx = new MInteger();
        MInteger outNbElement = new MInteger();
        Core core = new Core();
        // 调用ta-lib的RSI计算
        core.rsi(0, len - 1, closePrices, period, outBegIdx, outNbElement, outRSI);
        // 输出最后一个有效值
        int lastIdx = outBegIdx.value + outNbElement.value - 1;
        // 健壮查找最后一个非零有效点
        for (int i = lastIdx; i >= outBegIdx.value; i--) {
            if (outRSI[i] != 0.0) {
                lastIdx = i;
                break;
            }
        }

        // 构造全部rsi序列（逗号分隔字符串）
        StringBuilder rsiSeries = new StringBuilder();
        for (int i = outBegIdx.value; i <= lastIdx; i++) {  
            rsiSeries.append(outRSI[i]);
            if (i < lastIdx) rsiSeries.append(",");
        }

        // 判定信号
        String rsiSignal;

        double lastRsi = outRSI[lastIdx];
        if (lastRsi < 30) {
            rsiSignal = "bullish";
        } else if (lastRsi > 70) {
            rsiSignal = "bearish";
        } else {
            rsiSignal = "neutral";
        }

        Map<String, String> map = new HashMap<>();
        map.put("rsi", String.valueOf(outRSI[lastIdx]));
        map.put("rsiSignal", rsiSignal);

        return map;
    }

    /**
     * 计算Bollinger Bands（布林带）
     * @param cryptoKlineList K线数据列表
     * @param window 窗口期，默认20
     * @return Map，包含最新upper、lower、middle band值
     */
    public static Map<String, String> calculateBollingerBands(List<CryptoKline> cryptoKlineList, int window) {
        // 收盘价数组，去掉最后一个未收盘的K线
        double[] closePrices = new double[cryptoKlineList.size() - 1];
        for (int i = 0; i < cryptoKlineList.size() - 1; i++) {
            closePrices[i] = Double.parseDouble(cryptoKlineList.get(i).getClose());
        }
        int len = closePrices.length;
        double[] outUpper = new double[len];
        double[] outMiddle = new double[len];
        double[] outLower = new double[len];
        MInteger outBegIdx = new MInteger();
        MInteger outNbElement = new MInteger();
        Core core = new Core();
        // 调用ta-lib的Bollinger Bands计算，2倍标准差，SMA
        core.bbands(0, len - 1, closePrices, window, 2.0, 2.0, com.tictactec.ta.lib.MAType.Sma, outBegIdx, outNbElement, outUpper, outMiddle, outLower);
        // 输出最后一个有效值
        int lastIdx = outBegIdx.value + outNbElement.value - 1;
        // 健壮查找最后一个非零有效点
        for (int i = lastIdx; i >= outBegIdx.value; i--) {
            if (outUpper[i] != 0.0 || outMiddle[i] != 0.0 || outLower[i] != 0.0) {
                lastIdx = i;
                break;
            }
        }

        // 判定布林带信号
        double currentPrice = closePrices[len - 1];
        String bollingerSignal;
        if (currentPrice < outLower[lastIdx]) {
            bollingerSignal = "bullish";
        } else if (currentPrice > outUpper[lastIdx]) {
            bollingerSignal = "bearish";
        } else {
            bollingerSignal = "neutral";
        }
        
        Map<String, String> map = new HashMap<>();
        map.put("upper", String.valueOf(outUpper[lastIdx]));
        map.put("middle", String.valueOf(outMiddle[lastIdx]));
        map.put("lower", String.valueOf(outLower[lastIdx]));
        map.put("bollingerSignal", bollingerSignal);
        return map;
    }

    /**
     * 计算OBV（On-Balance Volume）指标及其斜率信号
     * @param cryptoKlineList K线数据列表
     * @return Map，包含obvList（List<Long>），obvSlope（Double），obvSignal（String）
     */
    public static Map<String, Object> calculateOBV(List<CryptoKline> cryptoKlineList) {
        List<Long> obvList = new java.util.ArrayList<>();
        obvList.add(0L); // 第一个OBV为0
        for (int i = 1; i < cryptoKlineList.size(); i++) {
            double prevClose = Double.parseDouble(cryptoKlineList.get(i - 1).getClose());
            double currClose = Double.parseDouble(cryptoKlineList.get(i).getClose());
            long currVolume = 0L;
            try {
                currVolume = (long) Double.parseDouble(cryptoKlineList.get(i).getVolume());
            } catch (Exception e) {
                // 解析失败，视为0
            }
            long prevObv = obvList.get(i - 1);
            if (currClose > prevClose) {
                obvList.add(prevObv + currVolume);
            } else if (currClose < prevClose) {
                obvList.add(prevObv - currVolume);
            } else {
                obvList.add(prevObv);
            }
        }
        // 计算obv斜率（最近5个点的均值diff）
        double obvSlope = 0.0;
        int n = obvList.size();
        if (n > 5) {
            long sumDiff = 0L;
            for (int i = n - 5; i < n - 1; i++) {
                sumDiff += obvList.get(i + 1) - obvList.get(i);
            }
            obvSlope = sumDiff / 5.0;
        }
        String obvSignal;
        if (obvSlope > 0) {
            obvSignal = "bullish";
        } else if (obvSlope < 0) {
            obvSignal = "bearish";
        } else {
            obvSignal = "neutral";
        }
        Map<String, Object> result = new java.util.HashMap<>();
//        result.put("obvList", obvList);
        result.put("obvSlope", obvSlope);
        result.put("obvSignal", obvSignal);
        return result;
    }

    /**
     * 计算EMA（Exponential Moving Average，指数移动平均线）
     * @param cryptoKlineList K线数据列表
     * @param period EMA周期
     * @return Map，包含最新ema值（key: "ema"），全部ema序列（key: "emaSeries"，逗号分隔字符串）
     */
    public static Map<String, String> calculateEMA(List<CryptoKline> cryptoKlineList, int period) {
        // 收盘价数组，去掉最后一个未收盘的K线
        double[] closePrices = new double[cryptoKlineList.size() - 1];
        for (int i = 0; i < cryptoKlineList.size() - 1; i++) {
            closePrices[i] = Double.parseDouble(cryptoKlineList.get(i).getClose());
        }
        int len = closePrices.length;
        double[] outEMA = new double[len];
        MInteger outBegIdx = new MInteger();
        MInteger outNbElement = new MInteger();
        Core core = new Core();
        // 调用ta-lib的EMA计算
        core.ema(0, len - 1, closePrices, period, outBegIdx, outNbElement, outEMA);
        // 输出最后一个有效值
        int lastIdx = outBegIdx.value + outNbElement.value - 1;
        // 健壮查找最后一个非零有效点
        for (int i = lastIdx; i >= outBegIdx.value; i--) {
            if (outEMA[i] != 0.0) {
                lastIdx = i;
                break;
            }
        }
        // 构造全部ema序列（逗号分隔字符串）
        StringBuilder emaSeries = new StringBuilder();
        for (int i = outBegIdx.value; i <= lastIdx; i++) {
            emaSeries.append(outEMA[i]);
            if (i < lastIdx) emaSeries.append(",");
        }
        Map<String, String> map = new HashMap<>();
        map.put("ema", String.valueOf(outEMA[lastIdx]));
        map.put("emaSeries", emaSeries.toString());
        return map;
    }

    /**
     * 计算指定周期内的价格跌幅百分比
     * @param cryptoKlineList K线数据列表
     * @param period 计算周期（如5表示当前与5根K线前的收盘价对比）
     * @return Map，包含priceDrop（跌幅百分比，double），confidence（置信度，恒为0.0，预留）
     */
    public static Map<String, Double> calculatePriceDrop(List<CryptoKline> cryptoKlineList, int period) {
        Map<String, Double> result = new HashMap<>();

        int cryptoKlineSize = cryptoKlineList.size();
        if (cryptoKlineSize < period + 1) {
            result.put("priceDrop", 0.0);

            return result;
        }
        // 取最后一根和period根前的收盘价
        double lastClose = Double.parseDouble(cryptoKlineList.get(cryptoKlineSize - 1).getClose());
        double prevClose = Double.parseDouble(cryptoKlineList.get(cryptoKlineSize - period - 1).getClose());
        double priceDrop = (lastClose - prevClose) / prevClose;
        result.put("priceDrop", priceDrop);

        return result;
    }

    /**
     * 计算ADX（Average Directional Index，平均趋向指数）
     * @param cryptoKlineList K线数据列表
     * @param period ADX周期，默认14
     * @return Map，包含最新adx、plusDI、minusDI值
     */
    public static Map<String, String> calculateADX(List<CryptoKline> cryptoKlineList, int period) {
        int len = cryptoKlineList.size() - 1;
        double[] high = new double[len];
        double[] low = new double[len];
        double[] close = new double[len];
        for (int i = 0; i < len; i++) {
            high[i] = Double.parseDouble(cryptoKlineList.get(i).getHigh());
            low[i] = Double.parseDouble(cryptoKlineList.get(i).getLow());
            close[i] = Double.parseDouble(cryptoKlineList.get(i).getClose());
        }

        double[] tr = new double[len];
        double[] plusDM = new double[len];
        double[] minusDM = new double[len];

        tr[0] = 0;
        plusDM[0] = 0;
        minusDM[0] = 0;
        for (int i = 1; i < len; i++) {
            double highLow = high[i] - low[i];
            double highClose = Math.abs(high[i] - close[i - 1]);
            double lowClose = Math.abs(low[i] - close[i - 1]);
            tr[i] = Math.max(highLow, Math.max(highClose, lowClose));

            double upMove = high[i] - high[i - 1];
            double downMove = low[i - 1] - low[i];
            plusDM[i] = (upMove > downMove && upMove > 0) ? upMove : 0;
            minusDM[i] = (downMove > upMove && downMove > 0) ? downMove : 0;
        }

        // 平滑TR, plusDM, minusDM（使用EMA）
        double[] trEma = new double[len];
        double[] plusDmEma = new double[len];
        double[] minusDmEma = new double[len];
        double alpha = 2.0 / (period + 1);
        trEma[0] = tr[0];
        plusDmEma[0] = plusDM[0];
        minusDmEma[0] = minusDM[0];
        for (int i = 1; i < len; i++) {
            trEma[i] = alpha * tr[i] + (1 - alpha) * trEma[i - 1];
            plusDmEma[i] = alpha * plusDM[i] + (1 - alpha) * plusDmEma[i - 1];
            minusDmEma[i] = alpha * minusDM[i] + (1 - alpha) * minusDmEma[i - 1];
        }

        double[] plusDI = new double[len];
        double[] minusDI = new double[len];
        for (int i = 0; i < len; i++) {
            plusDI[i] = trEma[i] == 0 ? 0 : 100.0 * plusDmEma[i] / trEma[i];
            minusDI[i] = trEma[i] == 0 ? 0 : 100.0 * minusDmEma[i] / trEma[i];
        }

        double[] dx = new double[len];
        for (int i = 0; i < len; i++) {
            double sum = plusDI[i] + minusDI[i];
            dx[i] = sum == 0 ? 0 : 100.0 * Math.abs(plusDI[i] - minusDI[i]) / sum;
        }

        // ADX（对DX做EMA）
        double[] adx = new double[len];
        adx[0] = dx[0];
        for (int i = 1; i < len; i++) {
            adx[i] = alpha * dx[i] + (1 - alpha) * adx[i - 1];
        }

        int lastIdx = len - 1;
        Map<String, String> map = new HashMap<>();
        map.put("adx", String.valueOf(adx[lastIdx]));
        map.put("plusDI", String.valueOf(plusDI[lastIdx]));//+di
        map.put("minusDI", String.valueOf(minusDI[lastIdx]));//-di
        return map;
    }

    /**
     * 计算Ichimoku Cloud（云图）指标
     * @param cryptoKlineList K线数据列表
     * @return Map，包含tenkan_sen、kijun_sen、senkou_span_a、senkou_span_b、chikou_span的全部序列（逗号分隔字符串）
     * 实现细节：
     * - tenkan_sen（转换线）: (9-period high + 9-period low)/2
     * - kijun_sen（基准线）: (26-period high + 26-period low)/2
     * - senkou_span_a（先行A）: (tenkan_sen + kijun_sen)/2，向前移26期
     * - senkou_span_b（先行B）: (52-period high + 52-period low)/2，向前移26期
     * - chikou_span（迟行线）: 收盘价向后移26期
     * 所有序列均以逗号分隔字符串返回，长度与输入K线数一致，前置/后置不足部分补null
     * 时间戳: 2025-07-08T23:12:22+08:00
     */
    public static Map<String, String> calculateIchimoku(List<CryptoKline> cryptoKlineList) {
        int n = cryptoKlineList.size();
        double[] high = new double[n];
        double[] low = new double[n];
        double[] close = new double[n];
        for (int i = 0; i < n; i++) {
            high[i] = Double.parseDouble(cryptoKlineList.get(i).getHigh());
            low[i] = Double.parseDouble(cryptoKlineList.get(i).getLow());
            close[i] = Double.parseDouble(cryptoKlineList.get(i).getClose());
        }
        Double[] tenkanSen = new Double[n];
        Double[] kijunSen = new Double[n];
        Double[] senkouSpanA = new Double[n];
        Double[] senkouSpanB = new Double[n];
        Double[] chikouSpan = new Double[n];
        // Tenkan-sen
        for (int i = 0; i < n; i++) {
            if (i >= 8) {
                double maxHigh = high[i];
                double minLow = low[i];
                for (int j = i - 8; j <= i; j++) {
                    if (high[j] > maxHigh) maxHigh = high[j];
                    if (low[j] < minLow) minLow = low[j];
                }
                tenkanSen[i] = (maxHigh + minLow) / 2.0;
            } else {
                tenkanSen[i] = null;
            }
        }
        // Kijun-sen
        for (int i = 0; i < n; i++) {
            if (i >= 25) {
                double maxHigh = high[i];
                double minLow = low[i];
                for (int j = i - 25; j <= i; j++) {
                    if (high[j] > maxHigh) maxHigh = high[j];
                    if (low[j] < minLow) minLow = low[j];
                }
                kijunSen[i] = (maxHigh + minLow) / 2.0;
            } else {
                kijunSen[i] = null;
            }
        }
        // Senkou Span A (shifted forward 26)
        for (int i = 0; i < n; i++) {
            if (i >= 25 && i + 26 < n && tenkanSen[i] != null && kijunSen[i] != null) {
                senkouSpanA[i + 26] = (tenkanSen[i] + kijunSen[i]) / 2.0;
            }
        }
        // Senkou Span B (shifted forward 26)
        for (int i = 0; i < n; i++) {
            if (i >= 51 && i + 26 < n) {
                double maxHigh = high[i];
                double minLow = low[i];
                for (int j = i - 51; j <= i; j++) {
                    if (high[j] > maxHigh) maxHigh = high[j];
                    if (low[j] < minLow) minLow = low[j];
                }
                senkouSpanB[i + 26] = (maxHigh + minLow) / 2.0;
            }
        }
        // Chikou Span (shifted backward 26)
        for (int i = 0; i < n; i++) {
            if (i - 26 >= 0) {
                chikouSpan[i - 26] = close[i];
            }
        }
        // 转为逗号分隔字符串
        StringBuilder tenkanStr = new StringBuilder();
        StringBuilder kijunStr = new StringBuilder();
        StringBuilder spanAStr = new StringBuilder();
        StringBuilder spanBStr = new StringBuilder();
        StringBuilder chikouStr = new StringBuilder();
        for (int i = 0; i < n; i++) {
            tenkanStr.append(tenkanSen[i] == null ? "null" : tenkanSen[i]);
            kijunStr.append(kijunSen[i] == null ? "null" : kijunSen[i]);
            spanAStr.append(senkouSpanA[i] == null ? "null" : senkouSpanA[i]);
            spanBStr.append(senkouSpanB[i] == null ? "null" : senkouSpanB[i]);
            chikouStr.append(chikouSpan[i] == null ? "null" : chikouSpan[i]);
            if (i < n - 1) {
                tenkanStr.append(",");
                kijunStr.append(",");
                spanAStr.append(",");
                spanBStr.append(",");
                chikouStr.append(",");
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("tenkan_sen", tenkanStr.toString());
        map.put("kijun_sen", kijunStr.toString());
        map.put("senkou_span_a", spanAStr.toString());
        map.put("senkou_span_b", spanBStr.toString());
        map.put("chikou_span", chikouStr.toString());
        return map;
    }

    /**
     * 综合多周期EMA、ADX、Ichimoku等指标，输出趋势信号和置信度
     * @param cryptoKlineList K线数据列表
     * @return Map，包含signal（bullish/bearish/neutral）、confidence（置信度）、metrics（adx/trend_strength等）
     */
    public static Map<String, Object> calculateTrendSignals(List<CryptoKline> cryptoKlineList) {
        // 计算多周期EMA
        Map<String, String> ema8 = calculateEMA(cryptoKlineList, 8);
        Map<String, String> ema21 = calculateEMA(cryptoKlineList, 21);
        Map<String, String> ema55 = calculateEMA(cryptoKlineList, 55);
        // 计算ADX
        Map<String, String> adxMap = calculateADX(cryptoKlineList, 14);
        // 计算Ichimoku
        Map<String, String> ichimokuMap = calculateIchimoku(cryptoKlineList);

        // 解析EMA序列，取最后一个有效值
        double ema8Last = Double.parseDouble(ema8.get("ema"));
        double ema21Last = Double.parseDouble(ema21.get("ema"));
        double ema55Last = Double.parseDouble(ema55.get("ema"));

        // 趋势方向
        boolean shortTrend = ema8Last > ema21Last;
        boolean mediumTrend = ema21Last > ema55Last;

        // ADX强度
        double adx = Double.parseDouble(adxMap.get("adx"));
        double trendStrength = adx / 100.0;

        String signal;
        double confidence;
        if (shortTrend && mediumTrend) {
            signal = "bullish";
            confidence = trendStrength;
        } else if (!shortTrend && !mediumTrend) {
            signal = "bearish";
            confidence = trendStrength;
        } else {
            signal = "neutral";
            confidence = 0.5;
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("adx", adx);
        metrics.put("trend_strength", trendStrength);
        // metrics.put("ichimoku", ichimokuMap); // 如需详细云图可放开

        Map<String, Object> result = new HashMap<>();
        result.put("signal", signal);
        result.put("confidence", confidence);
        result.put("metrics", metrics);
        return result;
    }

    /**
     * 均值回归策略信号（z-score、Bollinger Bands、RSI多周期）
     * @param cryptoKlineList K线数据列表
     * @return Map，包含signal（bullish/bearish/neutral）、confidence、metrics（z_score、price_vs_bb、rsi_14、rsi_28）
     */
    public static Map<String, Object> calculateMeanReversionSignals(List<CryptoKline> cryptoKlineList) {
        int n = cryptoKlineList.size();
        if (n < 50 + 1) {
            // 至少需要50根K线
            Map<String, Object> result = new HashMap<>();
            result.put("signal", "neutral");
            result.put("confidence", 0.0);
            result.put("metrics", new HashMap<>());
            return result;
        }
        // 收盘价数组，去掉最后一个未收盘的K线
        double[] closePrices = new double[n - 1];
        for (int i = 0; i < n - 1; i++) {
            closePrices[i] = Double.parseDouble(cryptoKlineList.get(i).getClose());
        }
        int len = closePrices.length;
        // 计算50日均线和标准差
        double ma50 = 0.0, std50 = 0.0;
        for (int i = len - 50; i < len; i++) ma50 += closePrices[i];
        ma50 /= 50.0;
        for (int i = len - 50; i < len; i++) std50 += Math.pow(closePrices[i] - ma50, 2);
        std50 = Math.sqrt(std50 / 50.0);
        double zScore = std50 == 0 ? 0 : (closePrices[len - 1] - ma50) / std50;
        // 计算布林带
        Map<String, String> bb = calculateBollingerBands(cryptoKlineList, 50);
        double bbUpper = Double.parseDouble(bb.get("upper"));
        double bbLower = Double.parseDouble(bb.get("lower"));
        double priceVsBb = (closePrices[len - 1] - bbLower) / (bbUpper - bbLower);
        // 计算RSI多周期
        Map<String, String> rsi14 = calculateRSI(cryptoKlineList, 14);
        Map<String, String> rsi28 = calculateRSI(cryptoKlineList, 28);
        double rsi14Val = Double.parseDouble(rsi14.get("rsi"));
        double rsi28Val = Double.parseDouble(rsi28.get("rsi"));
        // 信号合成
        String signal;
        double confidence;
        if (zScore < -2 && priceVsBb < 0.2) {
            signal = "bullish";
            confidence = Math.min(Math.abs(zScore) / 4.0, 1.0);
        } else if (zScore > 2 && priceVsBb > 0.8) {
            signal = "bearish";
            confidence = Math.min(Math.abs(zScore) / 4.0, 1.0);
        } else {
            signal = "neutral";
            confidence = 0.5;
        }
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("z_score", zScore);
        metrics.put("price_vs_bb", priceVsBb);
        metrics.put("rsi_14", rsi14Val);
        metrics.put("rsi_28", rsi28Val);
        Map<String, Object> result = new HashMap<>();
        result.put("signal", signal);
        result.put("confidence", confidence);
        result.put("metrics", metrics);
        return result;
    }

    /**
     * 多因子动量策略信号（短中长期收益率+成交量动量）
     * @param cryptoKlineList K线数据列表
     * @return Map，包含signal（bullish/bearish/neutral）、confidence、metrics（momentum_1m, momentum_3m, momentum_6m, volume_momentum）
     */
    public static Map<String, Object> calculateMomentumSignals(List<CryptoKline> cryptoKlineList) {
        int n = cryptoKlineList.size();
        if (n < 127 + 1) { // 需要至少127根K线
            Map<String, Object> result = new HashMap<>();
            result.put("signal", "neutral");
            result.put("confidence", 0.0);
            result.put("metrics", new HashMap<>());
            return result;
        }
        // 收盘价、成交量数组，去掉最后一个未收盘的K线
        double[] closePrices = new double[n - 1];
        double[] volumes = new double[n - 1];
        for (int i = 0; i < n - 1; i++) {
            closePrices[i] = Double.parseDouble(cryptoKlineList.get(i).getClose());
            volumes[i] = Double.parseDouble(cryptoKlineList.get(i).getVolume());
        }
        int len = closePrices.length;
        // 计算收益率序列
        double[] returnsArr = new double[len];
        returnsArr[0] = 0.0;
        for (int i = 1; i < len; i++) {
            returnsArr[i] = closePrices[i] / closePrices[i - 1] - 1.0;
        }
        // 滚动动量（短21，中63，长126）
        double mom1m = 0.0, mom3m = 0.0, mom6m = 0.0;
        // 1m: 21期，min_periods=5
        int count1m = 0;
        for (int i = len - 21; i < len; i++) {
            if (i >= 0) { mom1m += returnsArr[i]; count1m++; }
        }
        mom1m = count1m >= 5 ? mom1m : 0.0;
        // 3m: 63期，min_periods=42
        int count3m = 0;
        for (int i = len - 63; i < len; i++) {
            if (i >= 0) { mom3m += returnsArr[i]; count3m++; }
        }
        mom3m = count3m >= 42 ? mom3m : mom1m;
        // 6m: 126期，min_periods=63
        int count6m = 0;
        for (int i = len - 126; i < len; i++) {
            if (i >= 0) { mom6m += returnsArr[i]; count6m++; }
        }
        mom6m = count6m >= 63 ? mom6m : mom3m;
        // 成交量动量
        double volumeSum = 0.0;
        int volumeCount = 0;
        for (int i = len - 21; i < len; i++) {
            if (i >= 0) { volumeSum += volumes[i]; volumeCount++; }
        }
        double volumeMA = volumeCount > 0 ? volumeSum / volumeCount : 0.0;
        double volumeMomentum = volumeMA > 0 ? volumes[len - 1] / volumeMA : 1.0;
        // 动量分数
        double momentumScore = 0.2 * mom1m + 0.3 * mom3m + 0.5 * mom6m;
        // 成交量确认
        boolean volumeConfirmation = volumeMomentum > 1.0;
        String signal;
        double confidence;
        if (momentumScore > 0.05 && volumeConfirmation) {
            signal = "bullish";
            confidence = Math.min(Math.abs(momentumScore) * 5, 1.0);
        } else if (momentumScore < -0.05 && volumeConfirmation) {
            signal = "bearish";
            confidence = Math.min(Math.abs(momentumScore) * 5, 1.0);
        } else {
            signal = "neutral";
            confidence = 0.5;
        }
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("momentum_1m", mom1m);
        metrics.put("momentum_3m", mom3m);
        metrics.put("momentum_6m", mom6m);
        metrics.put("volume_momentum", volumeMomentum);
        Map<String, Object> result = new HashMap<>();
        result.put("signal", signal);
        result.put("confidence", confidence);
        result.put("metrics", metrics);
        return result;
    }

    /**
     * 波动率信号计算（历史波动率、z-score、ATR等）
     * @param cryptoKlineList K线数据列表
     * @return Map，包含signal（bullish/bearish/neutral）、confidence、metrics（historical_volatility、volatility_regime、volatility_z_score、atr_ratio）
     */
    public static Map<String, Object> calculateVolatilitySignals(List<CryptoKline> cryptoKlineList) {
        int n = cryptoKlineList.size();
        if (n < 43) { // 至少43根K线
            Map<String, Object> result = new HashMap<>();
            result.put("signal", "neutral");
            result.put("confidence", 0.0);
            result.put("metrics", new HashMap<>());
            return result;
        }
        // 收盘价数组，去掉最后一个未收盘的K线
        double[] closePrices = new double[n - 1];
        for (int i = 0; i < n - 1; i++) {
            closePrices[i] = Double.parseDouble(cryptoKlineList.get(i).getClose());
        }
        int len = closePrices.length;
        // 计算收益率
        double[] returnsArr = new double[len];
        returnsArr[0] = 0.0;
        for (int i = 1; i < len; i++) {
            returnsArr[i] = closePrices[i] / closePrices[i - 1] - 1.0;
        }
        // 历史波动率（21期，min_periods=10）
        double histVol = 0.0;
        int count = 0;
        for (int i = len - 21; i < len; i++) {
            if (i >= 0) {
                histVol += Math.pow(returnsArr[i] - mean(returnsArr, len - 21, len), 2);
                count++;
            }
        }
        histVol = count >= 10 ? Math.sqrt(histVol / count) * Math.sqrt(252) : 0.0;
        // 波动率均值（42期，min_periods=21）
        double volMa = 0.0;
        int countMa = 0;
        for (int i = len - 42; i < len; i++) {
            if (i >= 0) {
                // 计算每个点的histVol
                double subVol = 0.0;
                int subCount = 0;
                for (int j = i - 20; j <= i; j++) {
                    if (j >= 0) {
                        subVol += Math.pow(returnsArr[j] - mean(returnsArr, i - 20, i + 1), 2);
                        subCount++;
                    }
                }
                double subHistVol = subCount >= 10 ? Math.sqrt(subVol / subCount) * Math.sqrt(252) : 0.0;
                volMa += subHistVol;
                countMa++;
            }
        }
        volMa = countMa >= 21 ? volMa / countMa : 0.0;
        // 波动率regime
        double volRegime = volMa != 0.0 ? histVol / volMa : 1.0;
        // 波动率std（42期，min_periods=21）
        double[] histVolArr = new double[42];
        int idx = 0;
        for (int i = len - 42; i < len; i++) {
            if (i >= 0) {
                double subVol = 0.0;
                int subCount = 0;
                for (int j = i - 20; j <= i; j++) {
                    if (j >= 0) {
                        subVol += Math.pow(returnsArr[j] - mean(returnsArr, i - 20, i + 1), 2);
                        subCount++;
                    }
                }
                histVolArr[idx++] = subCount >= 10 ? Math.sqrt(subVol / subCount) * Math.sqrt(252) : 0.0;
            }
        }
        double volStd = std(histVolArr, idx >= 21 ? idx - 21 : 0, idx);
        // z-score
        double volZScore = volStd != 0.0 ? (histVol - volMa) / volStd : 0.0;
        // ATR（14期，min_periods=7）
        double atr = calculateATR(cryptoKlineList, 14, 7);
        double atrRatio = closePrices[len - 1] != 0.0 ? atr / closePrices[len - 1] : 0.0;
        // NaN处理
        if (Double.isNaN(volRegime)) volRegime = 1.0;
        if (Double.isNaN(volZScore)) volZScore = 0.0;
        // 信号生成
        String signal;
        double confidence;
        if (volRegime < 0.8 && volZScore < -1) {
            signal = "bullish";
            confidence = Math.min(Math.abs(volZScore) / 3, 1.0);
        } else if (volRegime > 1.2 && volZScore > 1) {
            signal = "bearish";
            confidence = Math.min(Math.abs(volZScore) / 3, 1.0);
        } else {
            signal = "neutral";
            confidence = 0.5;
        }
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("historical_volatility", histVol);
        metrics.put("volatility_regime", volRegime);
        metrics.put("volatility_z_score", volZScore);
        metrics.put("atr_ratio", atrRatio);
        Map<String, Object> result = new HashMap<>();
        result.put("signal", signal);
        result.put("confidence", confidence);
        result.put("metrics", metrics);
        return result;
    }

    /**
     * 统计套利信号（短周期偏度、峰度、Hurst指数）
     * @param cryptoKlineList K线数据列表
     * @return Map，包含signal、confidence、metrics（hurst_exponent、skewness、kurtosis）
     */
    public static Map<String, Object> calculateStatArbSignals(List<CryptoKline> cryptoKlineList) {
        int n = cryptoKlineList.size();
        if (n < 43) { // 至少需要43根K线
            Map<String, Object> result = new HashMap<>();
            result.put("signal", "neutral");
            result.put("confidence", 0.0);
            result.put("metrics", new HashMap<>());
            return result;
        }
        // 收盘价数组
        double[] closePrices = new double[n];
        for (int i = 0; i < n; i++) {
            closePrices[i] = Double.parseDouble(cryptoKlineList.get(i).getClose());
        }
        // 计算收益率
        double[] returnsArr = new double[n];
        returnsArr[0] = 0.0;
        for (int i = 1; i < n; i++) {
            returnsArr[i] = closePrices[i] / closePrices[i - 1] - 1.0;
        }
        // 计算rolling skewness/kurtosis（42期，min21）
        double skew = 0.0, kurt = 3.0;
        int window = 42, minPeriods = 21;
        if (n >= window) {
            int start = n - window;
            int end = n;
            int count = 0;
            double mean = 0.0, m2 = 0.0, m3 = 0.0, m4 = 0.0;
            for (int i = start; i < end; i++) {
                mean += returnsArr[i];
                count++;
            }
            mean = count > 0 ? mean / count : 0.0;
            for (int i = start; i < end; i++) {
                double d = returnsArr[i] - mean;
                m2 += d * d;
                m3 += d * d * d;
                m4 += d * d * d * d;
            }
            if (count >= minPeriods && m2 != 0.0) {
                double s2 = m2 / count;
                double s = Math.sqrt(s2);
                skew = (m3 / count) / (s2 * s);
                kurt = (m4 / count) / (s2 * s2);
            }
        }
        // Hurst指数
        double hurst = calculateHurstExponent(cryptoKlineList, 10);
        // NaN处理
        if (Double.isNaN(skew)) skew = 0.0;
        if (Double.isNaN(kurt)) kurt = 3.0;
        // 生成信号
        String signal;
        double confidence;
        if (hurst < 0.4 && skew > 1) {
            signal = "bullish";
            confidence = (0.5 - hurst) * 2;
        } else if (hurst < 0.4 && skew < -1) {
            signal = "bearish";
            confidence = (0.5 - hurst) * 2;
        } else {
            signal = "neutral";
            confidence = 0.5;
        }
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("hurst_exponent", hurst);
        metrics.put("skewness", skew);
        metrics.put("kurtosis", kurt);
        Map<String, Object> result = new HashMap<>();
        result.put("signal", signal);
        result.put("confidence", confidence);
        result.put("metrics", metrics);
        return result;
    }

    /**
     * 计算Hurst指数（Hurst exponent）
     * @param cryptoKlineList K线数据列表
     * @param maxLag 最大滞后期，建议10
     * @return Hurst指数（0.0-1.0）
     */
    public static double calculateHurstExponent(List<CryptoKline> cryptoKlineList, int maxLag) {
        try {
            int n = cryptoKlineList.size();
            if (n < maxLag * 2 + 1) {
                return 0.5;
            }
            // 收盘价转double数组
            double[] prices = new double[n];
            for (int i = 0; i < n; i++) {
                prices[i] = Double.parseDouble(cryptoKlineList.get(i).getClose());
            }
            // 计算对数收益率
            double[] returns = new double[n - 1];
            for (int i = 1; i < n; i++) {
                returns[i - 1] = Math.log(prices[i] / prices[i - 1]);
            }
            if (returns.length < maxLag * 2) {
                return 0.5;
            }
            // 计算tau
            double[] tau = new double[maxLag - 2];
            int tauIdx = 0;
            for (int lag = 2; lag < maxLag; lag++) {
                int len = returns.length - lag;
                if (len <= 0) {
                    tau[tauIdx++] = 1e-8;
                    continue;
                }
                double sum = 0.0;
                for (int i = 0; i < len; i++) {
                    double diff = returns[i + lag] - returns[i];
                    sum += diff * diff;
                }
                double std = Math.sqrt(sum / len);
                tau[tauIdx++] = Math.max(1e-8, std);
            }
            // 计算log(lags)和log(tau)
            double[] logLags = new double[maxLag - 2];
            double[] logTau = new double[maxLag - 2];
            for (int i = 0; i < maxLag - 2; i++) {
                logLags[i] = Math.log(i + 2);
                logTau[i] = Math.log(tau[i]);
            }
            // 最小二乘拟合log-log直线，斜率即为Hurst指数
            double sumX = 0.0, sumY = 0.0, sumXY = 0.0, sumXX = 0.0;
            int m = logLags.length;
            for (int i = 0; i < m; i++) {
                sumX += logLags[i];
                sumY += logTau[i];
                sumXY += logLags[i] * logTau[i];
                sumXX += logLags[i] * logLags[i];
            }
            double denominator = m * sumXX - sumX * sumX;
            if (denominator == 0) return 0.5;
            double slope = (m * sumXY - sumX * sumY) / denominator;
            // Hurst指数限制在0-1
            return Math.max(0.0, Math.min(1.0, slope));
        } catch (Exception e) {
            return 0.5;
        }
    }

    // 辅助方法：均值
    private static double mean(double[] arr, int start, int end) {
        double sum = 0.0;
        int count = 0;
        for (int i = start; i < end; i++) {
            if (i >= 0 && i < arr.length) {
                sum += arr[i];
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }
    // 辅助方法：标准差
    private static double std(double[] arr, int start, int end) {
        double m = mean(arr, start, end);
        double sum = 0.0;
        int count = 0;
        for (int i = start; i < end; i++) {
            if (i >= 0 && i < arr.length) {
                sum += Math.pow(arr[i] - m, 2);
                count++;
            }
        }
        return count > 0 ? Math.sqrt(sum / count) : 0.0;
    }
    /**
     * 计算ATR（Average True Range）
     * @param cryptoKlineList K线数据列表
     * @param period ATR周期
     * @param minPeriods 最小周期要求
     * @return ATR值
     */
    private static double calculateATR(List<CryptoKline> cryptoKlineList, int period, int minPeriods) {
        int n = cryptoKlineList.size();
        if (n < period + 1) return 0.0;
        double[] trArr = new double[n - 1];
        for (int i = 1; i < n; i++) {
            double high = Double.parseDouble(cryptoKlineList.get(i).getHigh());
            double low = Double.parseDouble(cryptoKlineList.get(i).getLow());
            double prevClose = Double.parseDouble(cryptoKlineList.get(i - 1).getClose());
            double tr = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
            trArr[i - 1] = tr;
        }
        // 取最后period个TR的均值
        double sum = 0.0;
        int count = 0;
        for (int i = trArr.length - period; i < trArr.length; i++) {
            if (i >= 0) {
                sum += trArr[i];
                count++;
            }
        }
        return count >= minPeriods ? sum / count : 0.0;
    }


}
