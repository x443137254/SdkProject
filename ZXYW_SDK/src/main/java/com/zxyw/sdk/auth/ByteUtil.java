package com.zxyw.sdk.auth;

public class ByteUtil {

	public static int byteToInt(byte b) {
		return (int)b & 0xff;
	}

	public static long byteToLong(byte b) {
		return (long)b & 0xff;
	}

	public static byte intToByte(int x) {
		return (byte)x;
	}

	public static byte longToByte(long x) {
		return (byte)x;
	}

	/**
	 * 16进制字符串转换为数组
	 */
	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.isEmpty())
			return null;

		byte tv;
		byte[] rtn = new byte[hexString.length() / 2];
		hexString = hexString.toUpperCase();

		for (int i = 0; i < rtn.length; i++)
		{
			char l = hexString.charAt(i * 2);
			char r = hexString.charAt(i * 2 + 1);

			if (l >= '0' && l <= '9')
				tv = (byte)(l - '0');
			else if (l >= 'A' && l <= 'F')
				tv = (byte)(l - 'A' + 10);
			else
				tv = 0;

			tv = (byte)(tv << 4);

			if (r >= '0' && r <= '9')
				tv |= (byte)(r - '0');
			else if (r >= 'A' && r <= 'F')
				tv |= (byte)(r - 'A' + 10);
			else
				tv = 0;

			rtn[i] = tv;
		}

		return rtn;
	}

	/**
	 * 数组转换为16进制字符串
	 */
	public static String bytesToHexString(byte[] array) {
		if (array == null || array.length <= 0)
			return null;

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++)
		{
			sb.append(String.format("%02X", array[i]));
		}

		return sb.toString();
	}

	public static byte[] intToBytes(int value, int len) {
		byte[] b = new byte[len];
		for (int i = 0; i < len; i++) {
			b[len - i - 1] = (byte)((value >> 8 * i) & 0xff);
		}
		return b;
	}

	/**
	 * 异或
	 */
	public static String getXor(String segNum, String xorNum) {
		byte[] segBytes = hexStringToBytes(segNum);
		byte[] xorBytes = hexStringToBytes(xorNum);
		byte[] resultBytes = new byte[8];
		for (int i = 0; i < 8; i++) {
			resultBytes[i] = intToByte(byteToInt(segBytes[i]) ^ byteToInt(xorBytes[i]));
		}
		return bytesToHexString(resultBytes);
	}

	public static String byteToString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		if (bytes != null) {
			for(int i = 0; i < bytes.length; ++i) {
				sb.append(String.format("%02X", bytes[i]));
			}
		}

		return sb.toString();
	}
}

