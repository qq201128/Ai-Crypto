package com.wang.aiagent.chat.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class Choice {
    private Integer index;
    private SiliconFlowChatMessage message;
    private String finish_reason;
    private Integer flag;
    private Delta delta;  // 用于流式响应的增量更新

    @Data
    @NoArgsConstructor
    public static class Delta {
        private String content;
        private String role;
        private String reasoning_content;
    }
}
