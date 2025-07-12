package com.wang.aiagent.chat.domain;

import lombok.Data;

import java.io.Serializable;
@Data
public class SiliconFlowAiResponseCode implements Serializable {
    protected String error_code;
    protected String error_msg;
}
