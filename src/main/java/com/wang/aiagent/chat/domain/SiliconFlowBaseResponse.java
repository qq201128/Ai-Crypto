package com.wang.aiagent.chat.domain;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SiliconFlowBaseResponse extends SiliconFlowAiResponseCode implements Serializable {
    private String id;
    private String object;
    private String created;
    private String model;
    private Usage usage;
}
