package com.wang.aiagent.chat.domain;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @author wang
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SiliconFlowParam implements Serializable {
    protected  String prompt;
    /**
     * 聊天上下文信息。说明：
     * （1）messages成员不能为空，1个成员表示单轮对话，多个成员表示多轮对话
     * （2）最后一个message为当前请求的信息，前面的message为历史对话信息
     * （3）必须为奇数个成员，成员中message的role必须依次为user、assistant
     * （4）最后一个message的content长度（即此轮对话的问题）不能超过2000个字符；如果messages中content总长度大于2000字符，系统会依次遗忘最早的历史会话，直到content的总长度不超过2000个字符
     */
    protected List<SiliconFlowChatMessage> messages;

    protected String model;
    /**
     * 这个值建议按需给个合理的值，如果不给的话，我们会给一个不错的整数比如 1024。
     * 特别要注意的是，这个 max_tokens 是指您期待我们返回的 token 长度，而不是输入 + 输出的总长度。
     * 比如对一个 moonshot-v1-8k 模型，它的最大输入 + 输出总长度是 8192，当输入 messages 总长度为 4096 的时候，
     * 您最多只能设置为 4096，否则我们服务会返回不合法的输入参数（ invalid_request_error ），
     * 并拒绝回答。如果您希望获得“输入的精确 token 数”，
     * 可以使用下面的“计算 Token” API 使用我们的计算器获得计数
     *
     */
    protected int max_tokens = 2048;
    /**
     * 如果设置，值域须为 [0, 1] 我们推荐 0.3，以达到较合适的效果
     */
    protected float temperature=0.3F;
    protected float top_p =0.9F;

    /**
     * 是否以流式接口的形式返回数据，默认false
     */
    protected Boolean stream;
    /**
     * 控制模型是否会调用某个或某些工具。none 表示模型不会调用任何工具，而是以文字形式进行回复。auto 表示模型可选择以文本进行回复或者调用一个或多个工具。在调用时也可以通过将此字段设置为 required 或  {"type": "function", "function": {"name": "some_function"} } 来更强的引导模型使用工具。
     */
    protected Object tool_choice;
    protected List<Objects> tools;

    public boolean isStream() {
        return Objects.equals(this.stream, true);
    }
}
