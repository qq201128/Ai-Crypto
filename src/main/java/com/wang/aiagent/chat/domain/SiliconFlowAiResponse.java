package com.wang.aiagent.chat.domain;


import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * @author wang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SiliconFlowAiResponse extends SiliconFlowBaseResponse implements Serializable {
    private List<Choice> choices;
}
