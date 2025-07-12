package com.wang.aiagent.domain.vo;

import lombok.Data;


@Data
public class CryptoOverviewVo {
    private String baseCurrency; // 基础货币
    private String baseCurrencyDesc; // 基础货币描述
    private String baseCurrencyLogoid; // 基础货币Logo ID
    private String updateMode; // 更新模式
    private String type; // 类型
    private String typespecs; // 类型规格
    private String exchange; // 交易所
    private Integer cryptoTotalRank; // 加密货币总排名
    private Double close; // 当前价格
    private Integer pricescale; // 价格精度
    private Integer minmov; // 最小变动单位
    private String fractional; // 小数位数
    private Integer minmove2; // 最小变动单位2
    private String currency; // 计价货币
    private Double closeChange24h5; // 24小时涨跌幅
    private Double marketCapCalc; // 市值
    private String fundamentalCurrencyCode; // 基础计价货币代码
    private Double vol24hCmc; // 24小时成交量（CMC）
    private Double circulatingSupply; // 流通供应量
    private Double volToMarketCap24h; // 24小时成交量与市值比
    private Double socialDominance; // 社交占比
    private String cryptoCommonCategoriesTr; // 加密货币常见分类（多语言）
    private String recommendAll; // 技术评级 -为卖出
} 