package com.zxyw.sdk.simpleFingerLib;

final class Command {
    final static byte[] ENROLL = {0x03, 0x01};//录入指纹：根据编号录入一枚指纹，存Template到模块里
    final static byte[] CLEAR = {0x06, 0x01};//删除已注册的所有Template
    final static byte[] DETECT = {0x13, 0x01};//检测指纹输入状态
    final static byte[] GET_DATA = {0x1A, 0x01};//获取按一次手指生成的Template数据
    final static byte[] CANCEL = {0x30, 0x01};//终止与指纹采集有关的指令的运行
    final static byte[] READ = {0x0A, 0x01};//读出指定编号中的指纹模板数据
}
