package com.zxyw.sdk.simpleFingerLib;

/**
 * 通讯包Parket识别码
 */
final class PacketType {
    final static byte[] COMMAND = {0x55, (byte) 0xAA};//命令包
    final static byte[] RESPONSE = {(byte) 0xAA, 0x55};//响应包
    final static byte[] DATA = {0x5A, (byte) 0xA5};//指令数据包
    final static byte[] RESPONSEDATA = {(byte) 0xA5, 0x5A};//响应数据包
}
