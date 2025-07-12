package com.wang.aiagent.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * @author wang
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SiliconFlowChatMessage implements Serializable {
    private String role;
    private String content;
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_FUNCTION = "function";
    public static SiliconFlowChatMessage createUserMsg(String content){
        return new SiliconFlowChatMessage(ROLE_USER,content);
    }
    public static SiliconFlowChatMessage createAssistantMsg(String content){
        return new SiliconFlowChatMessage(ROLE_ASSISTANT,content);
    }
}
