package com.wang.aiagent.domain.vo;

import lombok.Data;

@Data
public class CryptoCoinVo {
    private String symbol;//币种名称
    private String startTime;//开始时间
    private String endTime;//结束时间
    private String timeInterval;//时间间隔 15min 30min 1h 4h 1day 1week 1month 1year
    private Integer limitSize;//条数 1- 1000
    private String isDetail;//是否返回详情 0否 1是
    private Double initialCapital;//初始资金
    private Integer newsNumber;//新闻数量
}
