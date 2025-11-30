package com.limtide.ugclite.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {

    /**
     * 对字符串进行 MD5 加密
     * @param text 明文
     * @return 32位小写 MD5 密文
     */
    public static String encrypt(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算 MD5 函数
            md.update(text.getBytes());
            byte[] digest = md.digest();

            // 将 byte 数组转换为 16 进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                // 1. 将 byte 转为 int，并保留低 8 位
                int i = b & 0xff;
                // 2. 将 int 转为 16 进制字符串
                String hexString = Integer.toHexString(i);
                // 3. 如果只有 1 位，前面补 0
                if (hexString.length() < 2) {
                    hexString = "0" + hexString;
                }
                sb.append(hexString);
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }
}