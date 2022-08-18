package com.zxyw.sdk.auth;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {

    private static final String key = "6pvesA+fhm0vl_V!";
    private static final String iv = "MWjpqmbtsHA&1QE5";

    public static String encode(String data) {
        if (data == null || data.equals("")) return null;
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            final int blockSize = cipher.getBlockSize();
            final byte[] dataBytes = data.getBytes();
            int plaintextLength = dataBytes.length;
            if (plaintextLength % blockSize != 0) {
                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
            }
            final byte[] plaintext = new byte[plaintextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key.getBytes(), "AES"),
                    new IvParameterSpec(iv.getBytes()));
            return bytesToHexString(cipher.doFinal(plaintext));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decode(String data) {
        if (data == null || data.equals("")) return null;
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key.getBytes(), "AES"),
                    new IvParameterSpec(iv.getBytes()));
            return new String(cipher.doFinal(hexStringToByte(data))).trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String bytesToHexString(byte[] src) {
        if (src == null || src.length <= 0) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (byte b : src) {
            String hv = Integer.toHexString(b & 0xFF);
            if (hv.length() < 2) {
                sb.append(0);
            }
            sb.append(hv);
        }
        return sb.toString();
    }

    private static byte[] hexStringToByte(String src) {
        if (src == null || src.length() == 0) return null;
        if (src.length() % 2 == 1) {
            src = "0" + src;
        }
        final byte[] bytes = new byte[src.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            final int a = Integer.parseInt(src.substring(i * 2, i * 2 + 2), 16);
            bytes[i] = (byte) (a & 0xF0 | a & 0x0F);
        }
        return bytes;
    }
}
