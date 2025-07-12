package com.wang.aiagent.chat.service;


import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;

import com.wang.aiagent.chat.config.SiliconFlowAiConfig;
import com.wang.aiagent.chat.domain.SiliconFlowAiResponse;
import com.wang.aiagent.chat.domain.SiliconFlowParam;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * 发送接口请求
 *
 * @Author wuchenxi
 * @Date 2023-07-23 17:46:49
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SiliconFlowAiService {

    private final SiliconFlowAiConfig Config;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .build();

    /**
     * 发起同步会话
     *
     * @param SiliconFlowParam 请求参数
     * @return
     */
    public SiliconFlowAiResponse chat(SiliconFlowParam SiliconFlowParam) {
        if (SiliconFlowParam == null) {
            throw new RuntimeException("参数异常：param不能为空");
        }
        SiliconFlowParam.setModel(Config.getModel());


        SiliconFlowParam.setStream(false);
        try {

            Gson gson = new Gson();
            String requestBody = gson.toJson(SiliconFlowParam);

            Request request = new Request.Builder()
                    .url(Config.getChatCompletionUrl())
                    .post(RequestBody.create(MediaType.get("application/json"), requestBody))
                    .addHeader("Authorization", "Bearer " + Config.getAccessKeyID())
                    .addHeader("Content-Type", "application/json")
                    .build();

            // 发送请求并处理响应
            Response response = httpClient.newCall(request).execute();

            assert response.body() != null;
//            log.info(requestBody);
            SiliconFlowAiResponse bean = JSONUtil.toBean(response.body().string(), SiliconFlowAiResponse.class);
//            log.info("-----------------"+String.valueOf(bean));
//            log.info(response.body().string());
            return bean;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public  String uploadFile(@NonNull MultipartFile file) throws IOException {

        byte[] bytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(bytes);


        // 构建消息列表
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        content.put("type", "image_url");
        content.put("image_url", "data:image/jpeg;base64," + base64Image);
        userMessage.put("role", "user");
        userMessage.put("content", content);
        messages.add(userMessage);

        // 创建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", Config.getModel());
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", 2500);
        requestBody.put("top_p", 0.9);
        requestBody.put("temperature", 0.3);

        Gson gson = new Gson();
        String jsonBody = gson.toJson(requestBody);

        // 构建HTTP请求
        Request request = new Request.Builder()
                .url(Config.getChatCompletionUrl())
                .post(RequestBody.create(MediaType.get("application/json"), jsonBody))
                .addHeader("Authorization", "Bearer " + Config.getAccessKeyID())
                .addHeader("Content-Type", "application/json")
                .build();

        // 发送请求并处理响应
        Response response = httpClient.newCall(request).execute();
        if (response.isSuccessful()) {
            String fileContent = response.body().string();
            log.info("API Response: " + fileContent);
            return fileContent;
        } else {
            String errorResponse = response.body().string();
            log.error("API Error Response: " + errorResponse);
            throw new RuntimeException("请检查文件内容");
        }
    }

    public File transferToFile(MultipartFile multipartFile) {
        //选择用缓冲区来实现这个转换即使用java 创建的临时文件 使用 MultipartFile.transferto()方法 。
        File file = null;
        try {
            String originalFilename = multipartFile.getOriginalFilename();
            //获取文件后缀
            String prefix = originalFilename.substring(originalFilename.lastIndexOf("."));
            file = File.createTempFile(originalFilename, prefix);    //创建零食文件
            multipartFile.transferTo(file);
            //删除
            file.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private HttpRequest getCommonRequest(@NonNull String url) {
        return HttpRequest.of(url).header(Header.AUTHORIZATION, "Bearer " + Config.getAccessKeyID());
    }



}
