package com.wang.aiagent.utils;

public class StringUtils {
    /**
     * 提取币种符号（如 dogeusdt -> doge）
     * @param input 输入字符串
     * @return 提取后的币种符号
     */
    public static String extractSymbol(String input) {
        if (input == null || input.length() <= 3) {
            return input;
        }
        String[] suffixes = {"usdt", "btc", "eth", "bnb", "busd", "usd", "usdc"};
        String lower = input.toLowerCase();
        for (String suffix : suffixes) {
            if (lower.endsWith(suffix)) {
                return input.substring(0, input.length() - suffix.length());
            }
        }
        return input;
    }
} 