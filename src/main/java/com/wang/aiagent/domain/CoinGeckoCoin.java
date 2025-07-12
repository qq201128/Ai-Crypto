package com.wang.aiagent.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Map;

@Data
@TableName("coin_gecko_coin")
public class CoinGeckoCoin {
    @TableId
    private String id;
    private String symbol;
    private String name;
    private Map<String, String> platforms;
} 