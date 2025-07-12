package com.wang.aiagent.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * @author wanng
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SiliconFlowContent {
    private String type;//type类型
    private String text;//问题
    private Map<String,String> image_url;//图片地址
}
