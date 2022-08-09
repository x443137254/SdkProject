package com.zxyw.sdk.simpleFingerLib;


import android.ndk.AdTest.AdLoad;

public class FingerTools {

    private static AdLoad adLoad;

    static byte[] swap(byte[] data) {
        byte[] b = new byte[2];
        try {
            b[0] = data[1];
        } catch (Exception ignore) {
        }
        try {
            b[1] = data[0];
        } catch (Exception ignore) {
        }
        return b;
    }

    static byte[] sum(byte[] data) {
        if (data != null && data.length > 2) {
            byte[] temp = sum(data, 0, data.length - 2);
            data[data.length - 2] = temp[0];
            data[data.length - 1] = temp[1];
            return data;
        }
        return null;
    }

    static byte[] sum(byte[] data, int start, int end) {
        byte[] result = new byte[2];
        int n = 0;
        for (int i = start; i < end; i++) {
            n += (data[i] & 0xFF);
        }
        byte[] temp = intToBytes(n);
        result[0] = temp[0];
        result[1] = temp[1];
        return result;
    }

    private static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) (value & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[3] = (byte) ((value >> 24) & 0xFF);
        return src;
    }

    public static boolean compare(byte[] data1, byte[] data2,int level) {
        if (data1 == null || data1.length != 570 || data2 == null || data2.length != 570) {
            return false;
        }
        int[] result = new int[1];
        if (adLoad == null) adLoad = new AdLoad();
        int match = adLoad.FPMatch(data1, data2, level, result, null);
        return match == 0 && result[0] > 0;
    }

    static String bytes2string(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        StringBuilder builder = new StringBuilder();
        char[] temp = new char[2];
        for (byte aByte : bytes) {
            int v = aByte & 0xFF;
            temp[0] = hexArray[v >>> 4];
            temp[1] = hexArray[v & 0x0F];
            builder.append(temp);
            builder.append(" ");
        }
        return builder.toString();
    }
}
