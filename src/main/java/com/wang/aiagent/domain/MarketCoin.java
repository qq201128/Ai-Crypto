package com.wang.aiagent.domain;

import lombok.Data;

/**
 * CoinGecko /coins/markets API 响应实体
 */
@Data
public class MarketCoin {
    /** 币种ID */
    private String id;
    /** 币种符号 */
    private String symbol;
    /** 币种名称 */
    private String name;
    /** 币种图片URL */
    private String image;
    /** 当前价格（以指定货币计价） */
    private Double current_price;
    /** 市值（以指定货币计价） */
    private Double market_cap;
    /** 市值排名 */
    private Integer market_cap_rank;
    /** 完全稀释估值（以指定货币计价） */
    private Double fully_diluted_valuation;
    /** 24小时成交量（以指定货币计价） */
    private Double total_volume;
    /** 24小时最高价（以指定货币计价） */
    private Double high_24h;
    /** 24小时最低价（以指定货币计价） */
    private Double low_24h;
    /** 24小时价格变动（以指定货币计价） */
    private Double price_change_24h;
    /** 24小时价格变动百分比 */
    private Double price_change_percentage_24h;
    /** 24小时市值变动（以指定货币计价） */
    private Double market_cap_change_24h;
    /** 24小时市值变动百分比 */
    private Double market_cap_change_percentage_24h;
    /** 流通供应量 */
    private Double circulating_supply;
    /** 总供应量 */
    private Double total_supply;
    /** 最大供应量 */
    private Double max_supply;
    /** 历史最高价（ATH，All Time High） */
    private Double ath;
    /** 距历史最高价的变动百分比 */
    private Double ath_change_percentage;
    /** 历史最高价日期 */
    private String ath_date;
    /** 历史最低价（ATL，All Time Low） */
    private Double atl;
    /** 距历史最低价的变动百分比 */
    private Double atl_change_percentage;
    /** 历史最低价日期 */
    private String atl_date;
    /** 投资回报率（可为null或对象，通常忽略） */
    private Object roi;
    /** 最后更新时间戳 */
    private String last_updated;
} 