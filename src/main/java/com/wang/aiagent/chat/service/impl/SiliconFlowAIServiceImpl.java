package com.wang.aiagent.chat.service.impl;



import com.wang.aiagent.chat.abstractd.AbstractChatService;
import com.wang.aiagent.chat.domain.Choice;
import com.wang.aiagent.chat.domain.SiliconFlowAiResponse;
import com.wang.aiagent.chat.domain.SiliconFlowChatMessage;
import com.wang.aiagent.chat.domain.SiliconFlowParam;
import com.wang.aiagent.chat.service.SiliconFlowAiService;
import com.wang.aiagent.chat.utils.ChatUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Lazy
@Service
@RequiredArgsConstructor
@Slf4j
public class SiliconFlowAIServiceImpl extends AbstractChatService implements InitializingBean {

    private final SiliconFlowAiService siliconFlowAiService;
    public String debateRoomChat(String content){
        SiliconFlowParam siliconFlowParam = new SiliconFlowParam();
        siliconFlowParam.setModel("deepseek-ai/DeepSeek-V3");
        List<SiliconFlowChatMessage> siliconFlowChatMessageList = new ArrayList<>();
        SiliconFlowChatMessage siliconFlowChatMessage  = new SiliconFlowChatMessage();
        siliconFlowChatMessage.setRole("user");
        System.out.println(content);
        siliconFlowChatMessage.setContent(ChatUtils.debateRoomChat(content));
        siliconFlowChatMessageList.add(siliconFlowChatMessage);
        siliconFlowParam.setMessages(siliconFlowChatMessageList);


        SiliconFlowAiResponse siliconFlowAiResponse = siliconFlowAiService.chat(siliconFlowParam);
        List<Choice> choicesFunction = siliconFlowAiResponse.getChoices();
        String userIntentAnalyzerResult = choicesFunction.get(0).getMessage().getContent();

        //对大模型返回的文本进行处理，保留markdown格式和必要的空白字符
        String substringJsonFinal = userIntentAnalyzerResult
                .replaceAll("```json\\s*", "") // 移除json代码块标记
                .replaceAll("```\\s*", "")     // 移除其他代码块标记
                .replaceAll("(?m)^\\s+$", "")  // 移除仅包含空白字符的行
                .replaceAll("\\r", "")         // 统一换行符
                .replaceAll("\\n{3,}", "\n\n") // 将3个及以上连续换行减少为2个
                .trim();                       // 清理首尾空白

//        log.info(substringJsonFinal);

        return substringJsonFinal;
    }
    public String macroNewsChat(String content){
        SiliconFlowParam siliconFlowParam = new SiliconFlowParam();
        siliconFlowParam.setModel("deepseek-ai/DeepSeek-V3");
        List<SiliconFlowChatMessage> siliconFlowChatMessageList = new ArrayList<>();
        SiliconFlowChatMessage siliconFlowChatMessage  = new SiliconFlowChatMessage();
        siliconFlowChatMessage.setRole("user");
        System.out.println(content);
        siliconFlowChatMessage.setContent(ChatUtils.macroSymbolNewsChat(content));
        siliconFlowChatMessageList.add(siliconFlowChatMessage);
        siliconFlowParam.setMessages(siliconFlowChatMessageList);


        SiliconFlowAiResponse siliconFlowAiResponse = siliconFlowAiService.chat(siliconFlowParam);
        List<Choice> choicesFunction = siliconFlowAiResponse.getChoices();
        String userIntentAnalyzerResult = choicesFunction.get(0).getMessage().getContent();

        //对大模型返回的文本进行处理，保留markdown格式和必要的空白字符
        String substringJsonFinal = userIntentAnalyzerResult
                .replaceAll("```json\\s*", "") // 移除json代码块标记
                .replaceAll("```\\s*", "")     // 移除其他代码块标记
                .replaceAll("(?m)^\\s+$", "")  // 移除仅包含空白字符的行
                .replaceAll("\\r", "")         // 统一换行符
                .replaceAll("\\n{3,}", "\n\n") // 将3个及以上连续换行减少为2个
                .trim();                       // 清理首尾空白

//        log.info(substringJsonFinal);

        return substringJsonFinal;
    }
    public String macroSymbolNewsChat(String content){
        SiliconFlowParam siliconFlowParam = new SiliconFlowParam();
        siliconFlowParam.setModel("deepseek-ai/DeepSeek-V3");
        List<SiliconFlowChatMessage> siliconFlowChatMessageList = new ArrayList<>();
        SiliconFlowChatMessage siliconFlowChatMessage  = new SiliconFlowChatMessage();
        siliconFlowChatMessage.setRole("user");
        System.out.println(content);
        siliconFlowChatMessage.setContent(ChatUtils.macroSymbolNewsChat(content));
        siliconFlowChatMessageList.add(siliconFlowChatMessage);
        siliconFlowParam.setMessages(siliconFlowChatMessageList);


        SiliconFlowAiResponse siliconFlowAiResponse = siliconFlowAiService.chat(siliconFlowParam);
        List<Choice> choicesFunction = siliconFlowAiResponse.getChoices();
        String userIntentAnalyzerResult = choicesFunction.get(0).getMessage().getContent();

        //对大模型返回的文本进行处理，保留markdown格式和必要的空白字符
        String substringJsonFinal = userIntentAnalyzerResult
                .replaceAll("```json\\s*", "") // 移除json代码块标记
                .replaceAll("```\\s*", "")     // 移除其他代码块标记
                .replaceAll("(?m)^\\s+$", "")  // 移除仅包含空白字符的行
                .replaceAll("\\r", "")         // 统一换行符
                .replaceAll("\\n{3,}", "\n\n") // 将3个及以上连续换行减少为2个
                .trim();                       // 清理首尾空白

//        log.info(substringJsonFinal);

        return substringJsonFinal;
    }
    @Override
    public void afterPropertiesSet() throws Exception {

    }

}
