package com.wang.aiagent.domain.vo;

import lombok.Data;

@Data
public class CryptoEmotionVo {
    /** 基础币种代码 */
    private String baseCurrency;
    /** 基础币种描述 */
    private String baseCurrencyDesc;
    /** 基础币种logo id */
    private String baseCurrencyLogoid;
    /** 更新模式 */
    private String updateMode;
    /** 类型 */
    private String type;
    /** 类型规格 */
    private String typespecs;
    /** 交易所 */
    private String exchange;
    /** 加密货币总排名 */
    private String cryptoTotalRank;
    /** 市值计算方式 */
    private String marketCapCalc;
    /** 基础面货币代码 */
    private String fundamentalCurrencyCode;
    /** AltRank */
    private String altrank;
    /** Galaxy 分数 */
    private String galaxyscore;
    /** 社交占比 */
    private String socialdominance;
    /** 情绪% */
    private String sentiment;
    /** 24小时社交流量 */
    private String socialVolume24h;
    /** 推文数 24小时*/
    private String tweets;
    /** Github提交数 */
    private String githubCommits;
} 