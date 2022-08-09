package com.zxyw.sdk.auth;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class DesUtil {

	/**
	 * 加密数据
	 */
	public static String encrypt(String message, String key) throws Exception {
		Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
		DESKeySpec desKeySpec = new DESKeySpec(ByteUtil.hexStringToBytes(key));
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		return ByteUtil.bytesToHexString(cipher.doFinal(ByteUtil.hexStringToBytes(message)));
	}

	/**
	 * 解密数据
	 */
	public static String decrypt(String message, String key) throws Exception {
		Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
		DESKeySpec desKeySpec = new DESKeySpec(ByteUtil.hexStringToBytes(key));
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
		SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		return ByteUtil.bytesToHexString(cipher.doFinal(ByteUtil.hexStringToBytes(message)));
	}

}
