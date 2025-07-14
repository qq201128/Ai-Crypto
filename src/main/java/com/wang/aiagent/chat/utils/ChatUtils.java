package com.wang.aiagent.chat.utils;



public class ChatUtils {

    public static String debateRoomChat(String content) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("#你是一位专业的金融分析师，请分析以下投资研究员的观点，并给出你的第三方分析:\n");
        stringBuilder.append("用户输入").append(content);
        stringBuilder.append("请提供以下格式的 JSON 回复:\n" +
                "{\n" +
                "    \"analysis\": \"你的详细分析，评估各方观点的优劣，并指出你认为最有说服力的论点\",\n" +
                "    \"score\": 0.5,  // 你的评分，从 -1.0(极度看空) 到 1.0(极度看多)，0 表示中性\n" +
                "    \"reasoning\": \"你给出这个评分的简要理由\"\n" +
                "}\n" +
                "\n" +
                "务必确保你的回复是有效的 JSON 格式，且包含上述所有字段。回复必须使用中文，不要使用英文或其他语言。");

        return stringBuilder.toString();
    }

    public static String macroNewsChat(String content) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("你是一名资深的虚拟币市场宏观分析师。请根据以下提供的加密货币当日的**全部新闻数据**，进行深入分析并生成一份专业的宏观总结报告。\n" +
                "\n" +
                "报告应包含以下几个方面：\n" +
                "1.  **市场情绪解读**：整体评估当前市场情绪（如：乐观、谨慎、悲观），并简述判断依据。\n" +
                "2.  **热点板块识别**：找出新闻中反映出的1-3个主要热点板块或主题，并说明其驱动因素。\n" +
                "3.  **潜在风险提示**：揭示新闻中可能隐藏的1-2个宏观层面或市场层面的潜在风险点。\n" +
                "4.  **政策影响分析**：如果新闻提及重要政策变动，请分析其可能对市场产生的短期和长期影响。\n" +
                "5.  **综合展望**：基于以上分析，对短期市场走势给出一个简明扼要的展望。\n" +
                "\n" +
                "请确保分析客观、逻辑清晰，语言专业。直接返回分析报告内容，不要包含任何额外说明或客套话。");
        stringBuilder.append("当日新闻数据如下：").append(content);

        return stringBuilder.toString();

    }

    public static String macroSymbolNewsChat(String content) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("你是一位专业的宏观经济分析师，专注于分析宏观经济环境对特定加密货币的影响。\n");
        stringBuilder.append("请分析以下新闻，从宏观角度评估当前经济环境，并分析这些宏观因素对目标加密货币的潜在影响。\n\n");
        stringBuilder.append("请关注以下宏观因素：\n");
        stringBuilder.append("1. 货币政策：利率、流动性、公开市场操作等\n");
        stringBuilder.append("2. 财政政策：政府支出、税收政策、补贴等\n");
        stringBuilder.append("3. 产业政策：行业监管、合规、创新支持等\n");
        stringBuilder.append("4. 国际环境：全球经济形势、贸易关系、地缘政治等\n");
        stringBuilder.append("5. 市场情绪：投资者信心、市场流动性、风险偏好等\n\n");
        stringBuilder.append("你的分析应该包括：\n");
        stringBuilder.append("1. 宏观环境评估：积极(positive)、中性(neutral)或消极(negative)\n");
        stringBuilder.append("2. 对目标加密货币的影响：利好(positive)、中性(neutral)或利空(negative)\n");
        stringBuilder.append("3. 关键影响因素：列出3-5个最重要的宏观因素\n");
        stringBuilder.append("4. 详细推理：解释为什么这些因素会影响目标加密货币\n\n");
        stringBuilder.append("请确保你的分析：\n");
        stringBuilder.append("- 基于事实和数据，而非猜测\n");
        stringBuilder.append("- 关注中长期影响，而非短期波动\n");
        stringBuilder.append("- 提供具体、可操作的见解\n\n");
        stringBuilder.append("- 详细推理需使用中文回复\n\n");
        stringBuilder.append("请以JSON格式返回结果，包含以下字段：macro_environment（宏观环境：positive/neutral/negative）、impact_on_symbol（对币种影响：positive/neutral/negative）、key_factors（关键因素数组）、reasoning（详细推理）。\n\n");
        stringBuilder.append("新闻数据如下：").append(content);
        return stringBuilder.toString();
    }
}
