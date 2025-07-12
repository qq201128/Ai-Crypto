package com.wang.aiagent.domain;

import lombok.Data;

@Data
public class CryptoKline {
    /** 开盘时间（毫秒时间戳） */
    private long openTime;
    /** 开盘价 */
    private String open;
    /** 最高价 */
    private String high;
    /** 最低价 */
    private String low;
    /** 收盘价 */
    private String close;
    /** 成交量 */
    private String volume;
    /** 收盘时间（毫秒时间戳） */
    private long closeTime;
    /** 成交额（计价币种数量） */
    private String quoteAssetVolume;
    /** 成交笔数 */
    private int numberOfTrades;
    /** 主动买入成交量 */
    private String takerBuyBaseAssetVolume;
    /** 主动买入成交额 */
    private String takerBuyQuoteAssetVolume;
    /** 忽略字段 */
    private String ignore;
    /** 开盘时间（格式化：yyyy-MM-dd HH:mm:ss） */
    private String openTimeStr;
    /** 收盘时间（格式化：yyyy-MM-dd HH:mm:ss） */
    private String closeTimeStr;
} 