package com.xgzx.veinmanager;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.io.UnsupportedEncodingException;

@SuppressWarnings("all")
public class VeinApi {
    static {
        System.loadLibrary("VeinApi");
    }

    /* 设备相关指令 */
    public static final int XG_CMD_CONNECTION = 0x01;// 连接设备
    public static final int XG_CMD_CLOSE_CONNECTION = 0x02; // 关闭连接
    public static final int XG_CMD_GET_SYSTEM_INFO = 0x03;// 获取版本号和设置信息
    public static final int XG_CMD_GET_DUID = 0x0F; //获取设备序列号
    public static final int XG_CMD_SET_TIMEOUT = 0x08; //设置等待手指超时时间
    /* 识别相关指令 */
    public static final int XG_CMD_FINGER_STATUS = 0x10; // 检测手指放置状态
    public static final int XG_CMD_CLEAR_ENROLL = 0x11;// 清除指定ID模版
    public static final int XG_CMD_CLEAR_ALL_ENROLL = 0x12;// 清除全部模板
    public static final int XG_CMD_GET_EMPTY_ID = 0x13;// 获取空（无登录数据）ID
    public static final int XG_CMD_GET_ENROLL_INFO = 0x14;// 获取总登录用户数和模板数
    public static final int XG_CMD_GET_ID_INFO = 0x15;// 获取指定ID登录信息
    public static final int XG_CMD_ENROLL = 0x16;// 登记
    public static final int XG_CMD_VERIFY = 0x17;// 1:1认证或1:N识别
    public static final int XG_CMD_CANCEL = 0x19;// 取消

    public static final int XG_CMD_GET_CHARA = 0x28; // 采集并读取特征到主机
    public static final int XG_CMD_READ_DATA = 0x20; //从设备读取数据
    public static final int XG_CMD_WRITE_DATA = 0x21; //写入数据到设备
    public static final int XG_CMD_READ_ENROLL = 0x22; //读取指定ID登录数据
    public static final int XG_CMD_WRITE_ENROLL = 0x23; //写入（覆盖）指定ID登录数据
    public static final int XG_CMD_PLAY_VOICE = 0x3b; //播放语音

    /* 错误代码 */
    public static final int XG_ERR_SUCCESS = 0x00;// 操作成功
    public static final int XG_ERR_FAIL = 0x01;// 操作失败
    public static final int XG_ERR_COM = 0x02;// 通讯错误
    public static final int XG_ERR_DATA = 0x03;// 数据校验错误
    public static final int XG_ERR_INVALID_PWD = 0x04;// 密码错误
    public static final int XG_ERR_INVALID_PARAM = 0x05;// 参数错误
    public static final int XG_ERR_INVALID_ID = 0x06;// ID错误
    public static final int XG_ERR_EMPTY_ID = 0x07;// 指定ID为空（无登录数据）
    public static final int XG_ERR_NOT_ENOUGH = 0x08;// 无足够登录空间
    public static final int XG_ERR_NO_SAME_FINGER = 0x09;// 不是同一根手指
    public static final int XG_ERR_DUPLICATION_ID = 0x0A;// 有相同登录ID
    public static final int XG_ERR_TIME_OUT = 0x0B; // 等待手指输入超时
    public static final int XG_ERR_VERIFY = 0x0C;// 认证失败
    public static final int XG_ERR_NO_NULL_ID = 0x0D; // 已无空ID
    public static final int XG_ERR_BREAK_OFF = 0x0E;// 操作中断
    public static final int XG_ERR_NO_CONNECT = 0x0F;// 未连接
    public static final int XG_ERR_NO_SUPPORT = 0x10;// 不支持此操作
    public static final int XG_ERR_NO_VEIN = 0x11;// 无静脉数据
    public static final int XG_ERR_PACKET_PREFIX = 0x30;//包头错误
    public static final int XG_ERR_PACKET_CHECK = 0x31;//校验和错误
    public static final int XG_ERR_PACKET_LACK = 0x32;// 数据不足

    public static final int XG_INPUT_FINGER = 0x20;// 请求放入手指
    public static final int XG_RELEASE_FINGER = 0x21; // 请求拿开手指

    public static final int XG_VOICE_ENRORLL_SUCCESS = 00;// 登记成功
    public static final int XG_VOICE_ENRORLL_FAIL = 02;// 登记失败
    public static final int XG_VOICE_VERIFY_SUCCESS = 33;// 验证成功
    public static final int XG_VOICE_VERIFY_FAIL = 32;// 验证失败
    public static final int XG_VOICE_PUTFINGER = 27;// 请放手指
    public static final int XG_VOICE_PUTFINGER_AGAIN = 23;// 请再放手指
    public static final int XG_VOICE_PUTFINGER_RIGHT = 26;// 请正确放置手指
    public static final int XG_VOICE_BEEP1 = 35;// 滴1
    public static final int XG_VOICE_BEEP2 = 36;// 滴2
    public static final int XG_VOICE_BEEP11 = 37;// 滴滴1
    public static final int XG_VOICE_BEEP22 = 38;// 滴滴2

    /**
     * 输出调试信息到调试文件，同时logcat也有输出，调试信息文件在/sdcard根目录，文件名是AndroidDebug.txt
     *
     * @param str 调试信息
     * @return
     */
    public native static int PrintfDebug(String str);

    /**
     * 是否调试信息输出，on=1输出调试信息，调试信息文件在C盘根目录下的XGComApi.txt,默认关闭
     */
    public native static long FVSetDebug(long on);

    /**
     * 连接USB、网络或串口设备，前提是系统有ROOT权限或相关设备文件可读写
     *
     * @param sDev      设备连接信息
     *                  标准USB设备："","USB"都可以连接USB设备  非标USB设备：可以直接指定PID和VID连接，如 "PID=30264,VID=8457"
     *                  串口设备: "COM:dev/ttyS0,BAUD:115200"
     *                  网络设备: 指定IP和端口，如 "IP:192.168.1.200,PORT:8080" 或通过socket,如 "SOCKET:68"
     * @param sPassword 连接密码,出厂密码"00000000"
     * @return > 0为设备句柄，连接成功 <=0 为连接错误  如-19为未找到设备
     */
    public native static long FVConnectDev(String sDev, String sPassword);

    /**
     * 关闭设备连接
     *
     * @param lDevHandle FVConnectDev函数返回的设备句柄
     * @return 0为设备关闭成功
     */
    public native static long FVCloseDev(long lDevHandle);

    /**
     * 给设备发指令包
     *
     * @param lDevHandle FVConnectDev函数返回的设备句柄
     * @param lCmd       指令码,如连接指令XG_CMD_CONNECTION = 0x01;
     * @param sData      16字节指令数据转成的HEX字符串,如0x123456ABCD则为"123456ABCD"
     *                   byte[]类型的可通过 String.format("%02X")的方式转为HEX字符串，也可以通过FVHexToAscii函数转换
     * @return 0 发送成功 < 0 发送失败
     */
    public native static long FVSendCmdPacket(long lDevHandle, long lCmd, String sData);

    /**
     * 接收设备回包
     *
     * @param lDevHandle FVConnectDev函数返回的设备句柄
     * @param lTimeout   接收超时,单位毫秒,0为默认超时
     * @return 如为null则收包失败，非null收包成功，且为HEX字符串,可通过FVAsciiToHex函数转为byte[]
     */
    public native static String FVRecvCmdPacket(long lDevHandle, long lTimeout);

    /**
     * 功能：接收串口数据
     * 参数：
     * lDevHandle:[IN]  FV_ConnectDev设备返回的设备ID
     * bData:[OUT] 接收数据缓存
     * lSize:[IN] 缓存长度或需要接收数据的大小
     * lTimeout:[IN] 接收超时，0为如果没有数据立即返回，有数据则接收完再返回
     * 返回值: > 0 接收数据的大小，< 0 没有数据
     */
    public native static long FVRecvUartData(long lDevHandle, byte[] bData, long lSize, long lTimeout);

    /**
     * 获取设备参数
     *
     * @param lDevHandle FVConnectDev函数返回的设备句柄
     * @return 返回设备信息和配置参数
     * 格式如下：
     * "DEVNAME:设备名称,SN:序列号,MAXUSER:设备登记最大用户数,USER:设备当前登记的用户数,VER:固件版本号,
     * DEVID:设备编号,BAUD:波特率,SECURITY:安全等级(1,2,3),FINGERTIMEOUT:手指检测超时(1-255秒）,REENROLLCHECK:重复登记检查(0不检查,1检查),
     * ONEFINGERCHECK:同一个手指检测(0不检测,1检测),VOLUME:音量(0关闭1-15音量大小)"
     * 如:"DEVNAME:V6SP,SN:180401123456BP,MAXUSER:100,USER:1,VER:2.01,DEVID:0,BAUD:115200,SECURITY:2,FINGERTIMEOUT:5,REENROLLCHECK:0,ONEFINGERCHECK:0,VOLUME:1"
     */
    public native static String FVGetDevParam(long lDevHandle);

    /**
     * 功能：设置设备参数
     * 参数：
     *
     * @param lDevHandle:[IN] FVConnectDev设备返回的设备ID
     * @param sParam:[IN]     修改设备参数配置，格式同FV_GetDevParam，多个设置项通过逗号,分隔
     *                        如:
     *                        修改波特率为9600:sParam="BAUD:9600",波特率有：9600,19200,38400,57600和115200
     *                        修改手指检测超时为8秒:sParam="FINGERTIMEOUT:8"
     *                        同时修改设备名称和音量:sParam="DEVNAME:FINGER,VOLUME:10"
     *                        恢复出厂设置：sParam="FACTORY"
     *                        复位重启设备：sParam="RESET"
     * @return 返回值: 0 设置成功 < 0设置失败
     */
    public native static long FVSetDevParam(long lDevHandle, String sParam);

    /**
     * 功能：播放指静脉设备语音
     * 参数：
     *
     * @param lDevHandle:[IN] FVConnectDev设备返回的设备ID
     * @param lSound:[IN]     语音ID
     * @return 返回值: 0
     */
    public native static long FVPlayDevSound(long lDevHandle, long lSound);

    /**
     * 功能：检测是否有手指
     * 参数：
     * lDevHandle:[IN]  FVConnectDev设备返回的设备ID
     * 返回值: > 0 有手指 = 0 无手指 < 0错误
     */
    public native static long FVCheckFinger(long lDevHandle);

    /**
     * 功能：从设备采集指静脉特征
     * 参数：
     * lDevHandle:[IN]  FVConnectDev设备返回的设备ID,如果为0此函数会自动连接设备，采集完后会自动关闭连接
     * lTimeout:[IN]  等待手指放入的超时,单位毫秒,0不检测手指
     * 返回值: 返回BASE64的特征字符串，如字符串长度小于10则为出错代码
     */
    public native static String FVGetVeinChara(long lDevHandle, long lTimeout);

    /**
     * 功能：从设备读取已登记的用户模板
     * 参数：
     * lDevHandle:[IN]  FVConnectDev设备返回的设备ID
     * lUserId:[IN]  用户ID
     * 返回值: 返回BASE64的模板字符串，如字符串长度小于10则为出错代码
     */
    public native static String FVReadDevTemp(long lDevHandle, long lUserId);

    /**
     * 功能：用户模板写入到设备的指定用户ID
     * 参数：
     * lDevHandle:[IN]  FVConnectDev设备返回的设备ID
     * lUserId:[IN]  用户ID
     * sTemp:[IN]  BASE64的模板字符串
     * 返回值: = 0 写入成功, < 0 写入失败
     */
    public native static long FVWriteDevTemp(long lDevHandle, long lUserId, String sTemp);

    /**
     * 功能：删除设备上的指定用户ID模板
     * 参数：
     * lDevHandle:[IN]  FVConnectDev设备返回的设备ID
     * lUserId:[IN]  用户ID
     * 返回值: = 0 删除成功，< 0删除失败
     */
    public native static long FVDeleteDevTemp(long lDevHandle, long lUserId);

    /**
     * 功能：获取设备运行调试信息
     * 参数：
     * lDevHandle:[IN]  FVConnectDev设备返回的设备ID
     * sFileName:[IN]  输出文件名，如Debug.txt
     * 返回值: > 0获取成功 <= 0获取失败
     */
    public native static long FVGetDevDebugInfo(long lDevHandle, String sFileNume);

    /**
     * 功能：更新设备固件
     * 参数：
     * lDevHandle:[IN]  FVConnectDev设备返回的设备ID
     * sFileName:[IN]  固件文件名，如D700.dat或C300.xgp
     * 返回值: = 0更新成功 < 0更新失败
     */
    public native static long FVUpgrade(long lDevHandle, String sFileName);

    /**
     * 功能：解析图像数据
     * 参数：
     * sData:[IN]  图像数据
     * lDataLen:[] 数据大小
     * sFileName:[IN]  ".bmp"输出BMP文件的文件名  或 ".jpg"输出JPG文件的文件名,如为null或""则返回BASE64字符串图像数据
     * 如 sFileName = "vein.bmp"则图像会保存在vein.bmp的文件,sFileName = "vein.jpg"则图像会保存在vein.jpg文件
     * 返回值: 返回BASE64字符串图像数据,如保存的是文件则返回数据大小字符串，可通过Integer.parseInt([String])转成整数>0则保存成功 <=0 保存失败
     */
    public native static String FVGetImgFormData(String sData, long lDataLen, String sFileName);

    /**
     * 功能：从设备获取指静脉图像
     * 参数：
     * lDevHandle:[IN]  设备句柄
     * sFileName:[IN]  同FVGetImgFormData函数
     * lTimeout:[IN]  等待手指放入的超时,单位毫秒,0不检测手指
     * 返回值: 同FVGetImgFormData函数
     */
    public native static String FVGetImgFormDev(long lDevHandle, long lTimeout, String sFileName);

    /**
     * 功能：从图像提取特征
     * 参数：
     * sImg:[IN]  指静脉图像文件名或BASE64字符串图像数据
     * 如 sImg = "vein.bmp"则从vein.bmp读取图像数据,sImg = "vein.jpg"则从vein.jpg文件读取图像数据
     * sChara:[OUT]  特征输出缓存,至少要2K的缓存空间
     * 返回值: 返回BASE64的模板字符串，如字符串长度小于10则为出错代码
     */
    public native static String FVGetCharaFromImg(String sImg);

    /**
     * 功能：读取设备的出入记录
     * 参数：
     * lDevHandle:[IN]  设备句柄
     * sStartTime:[IN]  日志搜索开始时间,格式： 2001-01-01,12:30:30
     * sEndTime:[IN]  日志搜索开始时间,格式： 2001-01-01,12:30:30
     * sLog:[OUT] 日志输出缓存，为NULL只搜索日志条数
     * 日志格式：
     * "ID:用户ID,DATE:操作日期,TIME:操作时间,CARDNO:卡号,NAME:姓名,EVENT:事件;\r\n"
     * 如:
     * "ID:1,DATE:2018-04-12,TIME:12:30:21,CARDNO:123456,NAME:流浪,EVENT:USER SUCCESS;\r\n"
     * 事件定义：
     * "USER SUCCESS", //用户验证成功
     * "ADMIN SUCCESS", //管理员验证成功
     * "ADD USER", //增加指静脉用户
     * "ADD ADMIN", //增加管理员
     * "DEL USER", //删除指静脉用户
     * "DEL ADMIN", //删除管理员
     * "DEL ALL", //清除全部用户
     * "DEL LOG", //删除日志
     * "FACTORY", //出厂设置
     * "ADD PASSWORD", //增加密码用户
     * "DEL PASSWORD", //删除密码用户
     * "DEL ALL PASSWORD", //删除所有密码用户
     * "PASSWORD SUCCESS", //密码验证成功
     * "KEY OPEN", //按键开门
     * "REMOTE OPEN", //遥控开门
     * 返回值: 如sLog为NULL的情况下返回搜索到的日志条数= 0无日志 < 0 搜索失败，如sLog为非空的情况下返回总日志字符串的长度
     */
    public native static long FVReadDevLog(long lDevHandle, String sStartTime, String sEndTime, byte[] bLog); //注意中文编码转换

    /**
     * 功能：写入用户信息
     * 参数：
     * lDevHandle:[IN]  设备句柄
     * lStartID:[IN]  开始写入的ID 从1开始
     * lNum:[IN]  写入的用户信息个数
     * bUserInfo:[IN] 用户信息字符串,注意要转成GBK编码，否则中文可能显示不正确，每个用户信息用分号;分割
     * 用户信息必须按此格式赋值,多个用户信息通过;分号隔断,用户信息的不同字段通过,逗号隔断,没有对应信息可以没有相应字段
     * 用户信息格式："NAME:姓名,ADMIN:权限,DEPART:部门编号,CARDNO:卡号,WORKNO:工号,PASSWORD:密码,UID:身份证号;";
     * 如只要用户名为：“NAME:姓名;” 只有用户名和卡号为:“NAME:姓名,CARDNO:卡号;”
     * lBytes:bUserInfo的字节数
     * 返回值: > 0 写入成功的用户数, < 0写入失败
     */
    public native static long FVWriteDevUserInfo(long lDevHandle, long lStartID, long lNum, byte[] bUserInfo, long lBytes); //注意中文编码转换

    /**
     * 功能：读取用户信息
     * 参数：
     * lDevHandle:[IN]  设备句柄
     * lStartID:[IN]  开始读取的ID 从1开始到最大用户数
     * lNum:[IN]  读取的用户信息个数
     * bUserInfo:[OUT] 用户信息输出缓存,一个用户需要255个字节的缓存空间,总的缓存空间是lNum*255，中文是GBK编码，可能需要转换才能正常显示
     * 用户信息格式同FVWriteDevUserInfo
     * 返回值: > 0 bUserInfo的有效数据长度, <= 0读取失败
     */
    public native static long FVReadDevUserInfo(long lDevHandle, long lStartID, long lNum, byte[] bUserInfo); //注意中文编码转换

    /**
     * 功能：创建算法库实例
     * 参数：
     * UserNum:[IN]  算法库最大用户数
     * 返回值: > 0算法库HANDLE <= 0 创建失败
     */
    public native static long FVCreateVeinLib(long lUserNum);

    /**
     * 功能：销毁算法库，回收内存
     * 参数：
     * lLibHandle:[IN]  FVCreateVeinLib返回的算法库HANDLE
     * 返回值: 0
     */
    public native static long FVDestroyVeinLib(long lLibHandle);

    public native static long FVGetNullID(long lLibHandle);

    /**
     * 功能：导入用户模板
     * 参数：
     * lLibHandle:[IN]  FVCreateVeinLib返回的算法库HANDLE
     * UserId:[IN] 用户ID，> 1 并且 < UserNum
     * sTemp:[IN] 用户模板BASE64字符串
     * 返回值: = 0 导入成功 < 0 导入错误的错误码
     */
    public native static long FVImportVeinTemp(long lLibHandle, long lUserId, String sTemp);

    /**
     * 功能：导出用户模板
     * 参数：
     * lLibHandle:[IN]  FVCreateVeinLib返回的算法库HANDLE
     * UserId:[IN] 用户ID，> 1 并且 < UserNum
     * 返回值: 返回用户模板BASE64字符串
     */
    public native static String FVExportVeinTemp(long lLibHandle, long lUserId);

    /**
     * 功能：清除算法库指定用户的模板
     * 参数：
     * lLibHandle:[IN]  FVCreateVeinLib返回的算法库HANDLE
     * UserId:[IN] 用户ID，> 1 并且 < UserNum, 0为全部删除
     * 返回值: 0
     */
    public native static long FVCleanVeinTemp(long lLibHandle, long lUserId);

    /**
     * 功能：设置指定用户ID的用户信息
     * 参数：
     * lLibHandle:[IN]  FVCreateVeinLib返回的算法库HANDLE
     * UserId:[IN] 用户ID，> 1 并且 < UserNum
     * bUserInfo:[IN] 用户信息,注意要转成GBK编码，否则中文可能显示不正确
     * 用户信息格式同FVWriteDevUserInfo
     * lBytes:bUserInfo的字节数
     * 返回值: = 0 设置成功 < 0 设置错误
     */
    public native static long FVSetUserInfo(long lLibHandle, long lUserId, byte[] bUserInfo, long lBytes);

    /**
     * 功能：获取指定用户ID的用户信息
     * 参数：
     * lLibHandle:[IN]  FV_CreateVeinLib返回的算法库HANDLE
     * UserId:[IN] 用户ID，> 1 并且 < UserNum
     * bUserInfo:[OUT] 用户信息输出缓存,中文是GBK编码，可能需要转换才能正常显示
     * 用户信息格式同FVWriteDevUserInfo
     * 返回值: > 0 bUserInfo的有效数据长度, <= 0读取失败
     */
    public native static long FVGetUserInfo(long lLibHandle, long lUserId, byte[] bUserInfo);

    /**
     * 功能：获取模板保存的用户信息
     * 参数：
     * sTemp:[IN] 模板BASE64字符串
     * bUserInfo:[OUT] 用户信息输出缓存,中文是GBK编码，可能需要转换才能正常显示
     * 用户信息格式同FVWriteDevUserInfo
     * 返回值: > 0 bUserInfo的有效数据长度, <= 0读取失败
     */
    public native static long FVGetTempUserInfo(String sTemp, byte[] bUserInfo);

    /**
     * 功能：1:N搜索用户
     * 参数：
     * lLibHandle:[IN]  FVCreateVeinLib返回的算法库HANDLE
     * sChara:[IN] 指静脉特征
     * lTh:[IN] 安全等级 1,2,3 安全等级大，认假越低，拒真越高
     * 返回值: > 0 识别成功的用户ID <= 0 识别失败
     */
    public native static long FVSearchUser(long lLibHandle, String sChara, long lTh);

    /**
     * 功能：1:1验证
     * 参数：
     * sTemp:[IN]  用户模板
     * sChara:[IN] 指静脉特征
     * lScore:[IN] 比对分数75-80，越小通过率越高，认假也越高
     * 返回值: > 0 验证成功后自学习模板BASE64字符串 如为null或长度小于10则验证失败
     */
    public native static String FVVerifyUser(String sTemp, String sChara, long lScore);

    /**
     * 功能：把几个特征融合成一个用户模板
     * 参数：
     * sChara1:[IN]  特征1
     * sChara2:[IN]  特征2
     * sChara3:[IN]  特征3
     * bUserInfo:[IN]  用户信息,NULL或0则忽略
     * lBytes: bUserInfo的有效字节数
     * 用户信息格式同FVWriteDevTemp
     * 返回值: 融合成功后的模板BASE64字符串,如字符串长度小于10则融合失败，返回的是出错代码
     */
    public native static String FVCreateVeinTemp(String sChara1, String sChara2, String sChara3, byte[] bUserInfo, long lBytes);

    /**
     * 功能：模板增加特征
     * 参数：
     * sTemp:[IN] 需要增加特征的模板，BASE64字符串
     * sChara:[IN]  特征
     * bUserInfo:[IN]  用户信息,NULL或0则忽略
     * lBytes: bUserInfo的有效字节数
     * 用户信息格式同FV_WriteDevTemp
     * 返回值: 增加特征成功后的模板BASE64字符串,如字符串长度小于10则增加失败，返回的是出错代码
     */
    public native static String FVAddCharaToTemp(String sTemp, String sChara, byte[] bUserInfo, long lBytes);

    /**
     * 功能：两个特征比对（用于检测是否为同一根手指）
     * 参数：
     * sChara1:[IN]  特征1
     * sChara2:[IN]  特征2
     * Score:[IN] 大于Score则比对成功,一般设置为60比较合适
     * 返回值: = 0 比对成功
     */
    public native static long FVCharaMatch(String sChara1, String sChara2, long lScore);

    /**
     * 功能：解析设备上报的包
     * 参数：
     * bNetBuf:[IN]  网络接收缓存
     * lBufSize:[IN] 网络接收到的字节数
     * bCmd:[OUT]  包指令，HEX字符串格式，前2个字节是指令码
     * bSN:[OUT]  如果包里有设备的SN则返回设备的SN,有效长度是14字节
     * bData:[OUT]  包数据
     * 返回值: >= 0 包解析成功，返回包数据字节数,<0 包错误
     * -0x30:XG_ERR_PACKET_PREFIX 包头错误
     * -0x31:XG_ERR_PACKET_CHECK包校验错误
     * -0x32:XG_ERR_PACKET_LACK包数据不足，也就是包还没有收全
     */
    public native static long FVNetPackParse(byte[] bNetBuf, long lBufSize, byte[] bCmd, byte[] bSN, byte[] bData);

    /**
     * 功能：初始化SOCKET 服务
     * 参数：
     * lPort:[IN]  监听端口
     * sParam:[IN]  预留参数接口
     * 返回值: > 0 成功 监听 socket
     */
    public native static long FVSocketServerInit(long lPort, String sParam);

    /**
     * 功能：等待设备连接请求
     * 参数：
     * lSocket:[IN]  FVSocketServerInit 返回的参数
     * 返回值: > 0 连接成功的设备socket
     */
    public native static long FVSocketAccept(long lSocket);

    /**
     * 功能：等待接收设备发送的数据包
     * 参数：
     * lSocket:[IN]  FVSocketAccept 返回的参数
     * bCmd:[OUT]  包指令，HEX字符串格式，前2个字节是指令码
     * bSN:[OUT]  如果包里有设备的SN则返回设备的SN,有效长度是14字节
     * bData:[OUT]  包数据
     * 返回值: >= 0 包解析成功，返回包数据sData的长度,< 0 包错误或接收错误
     * -0x30:XG_ERR_PACKET_PREFIX 包头错误
     * -0x31:XG_ERR_PACKET_CHECK 包校验错误
     */
    public native static long FVSocketRecvPack(long lSocket, byte[] bCmd, byte[] bSN, byte[] bData);

    /**
     * 功能：通过SOCKET发包
     * 参数：
     * lSocket:[IN] socket
     * lCmd:[IN]  指令码
     * 指令解析参考客户端模式网络协议文档
     * sData:[IN]  包数据，如0x12 0x34 0x56 0xAB 的数据则为 "123456AB"
     * 返回值: > 0 发送成功 socket send 返回值
     */
    public native static long FVSocketSendPack(long lSocket, long lCmd, String sData);

    /**
     * 功能：关闭SOCKET
     * 参数：
     * lSocket:[IN] socket
     * 返回值: 0
     */
    public native static long FVSocketClose(long lSocket);

    /**
     * 功能：模板与模板比对查重
     * 参数：
     * sTemp1:[IN] BASE64编码模板1
     * sTemp2:[IN] BASE64编码模板2
     * lTh:[IN] 1-5安全等级
     * 返回值: >0比对成功，也就是模板相似
     */
    public native static long FVTempMatch(String sTemp1, String sTemp2, long lTh);

    /**
     * 功能：连接服务器
     * 参数：
     * sServerIP:[IN] 服务器IP
     * lPort:[IN] 服务器端口
     * 返回值: 0
     */
    public native static long FVSocketConnect(String sServerIP, long lPort);

    /**
     * 功能：HEX字符串字符串转十六进制字节数据,如:"1234567890ABCDEF" -> 0x1234567890ABCDEF
     * 参数：
     * sAscii:[IN] HEX字符串,也就是字符串是有数字和ABCDEF这几个字母组成的，2个字符代表一个字节的数据,如12则表示一个字节0x12
     * bHex:[OUT] 字节数据输出缓存
     * 返回值: > 0转换成功后的bHex的有效字节数,如"1234"转换成功后的bHex是0x12 0x34 返回值是2
     */
    public native static long FVAsciiToHex(String sAscii, byte[] bHex);

    /**
     * 功能：HEX转字符串,如:0x1234567890ABCDEF ->"1234567890ABCDEF"
     * 参数：
     * bHex:[IN] 字节数据输出缓存
     * lByte:[IN] bHex需要转换的字节数
     * sAscii:[OUT] HEX字符串,也就是字符串是有数字和ABCDEF这几个字母组成的，2个字符代表一个字节的数据,如12则表示一个字节0x12
     * 返回值: 转换后sAscii的字符串长度
     */
    public native static String FVHexToAscii(byte[] bHex, long lByte);

    /**
     * 功能：BASE64字符串还原为byte[]
     * 参数：
     * sBase64:[IN] BASE64字符串
     * bData:[OUT] byte[]输出缓存
     * 返回值: 转换后bData的有效字节数
     */
    public native static long FVDecodeBase64(String sBase64, byte[] bData);

    /**
     * 功能：byte[]进行BASE64编码
     * 参数：
     * bData:[IN] 需要BASE64编码的byte[]缓存
     * lByte:[IN] 需要编码的bData有效字节数
     * 返回值: BASE64字符串
     */
    public native static String FVEncodeBase64(byte[] bData, long lByte);

    //USB设备相关变量，使用USB通讯时需要先在应用层获取到设备使用权限，并且指静脉设备必须配置成CDROM驱动模式
    private static UsbDeviceConnection mUsbDeviceConnection = null;
    private static UsbEndpoint mUsbEndpointIn;
    private static UsbEndpoint mUsbEndpointOut;

    //蓝牙通讯
//    public static BTSocket mBtSocket = null;

    public static String sDevInfo = null; //用于保存当前连接的设备信息
    public static String sDevSN = null; //用于保存当前连接成功的设备序列号

    //非so连接的设备暂时不提供用户设备信息的读写功能，可以通过读取模板的方式来实现，如果确实需要请联系技术支持
    public static long WriteDevUserInfo(long lDevHandle, long lUserId, long lNum, String sUserInfo) {
        byte[] bUserInfo = VeinApi.StrToGBKBytes(sUserInfo);
        if (mUsbDeviceConnection != null) {
            return -1;
        } else {
            return VeinApi.FVWriteDevUserInfo(lDevHandle, lUserId, lNum, bUserInfo, bUserInfo.length);
        }
    }

    public static String ReadDevUserInfo(long lDevHandle, long lUserId, long lNum) {
        long ret;
        byte[] bUserInfo = new byte[(int) lNum * 1000];
        if (mUsbDeviceConnection != null) {
            return null;
        } else {
            ret = VeinApi.FVReadDevUserInfo(lDevHandle, lUserId, lNum, bUserInfo);
            if (ret <= 0) return null;
        }
        return VeinApi.BytestoGBKStr(bUserInfo, (int) ret);
    }

    public static String GetImgFormDev(long lDevHandle, long lTimeout, String sFileName) {
        if (mUsbDeviceConnection != null) {
            return _GetImgFormDev(lTimeout, sFileName);
        } else {
            return FVGetImgFormDev(lDevHandle, lTimeout, sFileName);
        }
    }

    public static String ReadDevTemp(long lDevHandle, long lUserId) {
        if (mUsbDeviceConnection != null) {
            return _ReadDevTemp((int) lUserId);
        } else {
            return FVReadDevTemp(lDevHandle, lUserId);
        }
    }

    public static long WriteDevTemp(long lDevHandle, long lUserId, String sTemp, String sUserInfo) {
        if (sTemp == null) return XG_ERR_INVALID_PARAM * -1;
        if (sTemp.length() < 1024) return XG_ERR_INVALID_PARAM * -1;

        if (sUserInfo != null) {
            //修改模板的用户信息
            long LibHandle = FVCreateVeinLib(1); //创建算法库
            FVImportVeinTemp(LibHandle, 1, sTemp); //导入模板
            byte[] bUserInfo = StrToGBKBytes(sUserInfo); //转换用户信息为GBK编码
            FVSetUserInfo(LibHandle, 1, bUserInfo, bUserInfo.length); //导入用户信息
            sTemp = FVExportVeinTemp(LibHandle, 1); //导出模板
            FVDestroyVeinLib(LibHandle); //销毁算法库
        }
        if (mUsbDeviceConnection != null) {
            return _WriteDevTemp(lUserId, sTemp);
        } else {
            return FVWriteDevTemp(lDevHandle, lUserId, sTemp);
        }
    }

    public static long DeleteDevTemp(long lDevHandle, long lUserId) {
        if (mUsbDeviceConnection != null) {
            if (lUserId == 0) return _DelDevAllTemp(); //删除全部
            return _DelDevTemp(lUserId);
        } else {
            return FVDeleteDevTemp(lDevHandle, lUserId);
        }
    }

    /**
     * 功能：检测手指,如果检测到跟status一样的状态就立即返回，反正直至超时
     * 参数：
     * lDevHandle:[IN] 设备句柄
     * timeout:[IN] 检测超时，单位毫秒
     * status:[IN] 手指状态,1为有手指,0为无手指
     * 返回值: XG_ERR_SUCCESS 成功
     * XG_ERR_TIME_OUT 超时
     * < 0 错误
     */
    public static long CheckFinger(long lDevHandle, int timeout, int status) {
        int interval = 100;
        int timecnt = 0;
        timeout = timeout / interval;
        while (true) {
            long finger = 0;
            if (mUsbDeviceConnection != null) {
                finger = _CheckFinger();
            } else {
                finger = FVCheckFinger(lDevHandle);
            }
            if (finger == status) return XG_ERR_SUCCESS;
            if (finger < 0) return finger; //出错
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
            }
            timecnt++;
            if (timecnt > timeout) return XG_ERR_TIME_OUT * -1;
        }
    }

    public static String GetDevParam(long lDevHandle) {
        if (mUsbDeviceConnection != null) {
            String sSN = _GetDevSN();
            String sMaxUser = _GetDevMaxUser();
            String sSetting = _GetDevSet();
            sDevInfo = sDevInfo + ",SN:" + sSN + "," + sMaxUser + "," + sSetting;
            PrintfDebug(sDevInfo);
            return sDevInfo;
        } else {
            return FVGetDevParam(lDevHandle);
        }
    }

    public static long CloseDev(long lDevHandle) {
        if (mUsbDeviceConnection != null) {
            long ret = _CloseDev();
            return ret;
        } else {
            return FVCloseDev(lDevHandle);
        }
    }

    public static String GetVeinChara(long lDevHandle, long lTimeout) {
        if (mUsbDeviceConnection != null) {
            return _GetVeinChara(lTimeout);
        } else {
            return FVGetVeinChara(lDevHandle, lTimeout);
        }
    }

    public static long PlayDevSound(long lDevHandle, long lSound) {
        if (mUsbDeviceConnection != null) {
            byte[] bData = new byte[16];
            bData[0] = (byte) (lSound & 0xff);
            bData[1] = (byte) 0; //1为同步 0为异步
            int ret = _SendPacket(XG_CMD_PLAY_VOICE, bData, 2);
            if (ret == 0) {
                _RecvPacket(null, 1000);
            }
            return XG_ERR_SUCCESS;
        } else {
            return FVPlayDevSound(lDevHandle, lSound);
        }
    }

    public static long SetDevReEnrollCheck(long lDevHandle, long lCheck) {
        if (mUsbDeviceConnection != null) {
            byte[] bData = new byte[16];
            bData[0] = (byte) (lCheck & 0x01);
            int ret = _SendPacket(0x09, bData, 1);
            if (ret == 0) {
                _RecvPacket(null, 1000);
            }
            return XG_ERR_SUCCESS;
        } else {
            if (lCheck != 0) {
                return FVSetDevParam(lDevHandle, "REENROLLCHECK:1");
            } else {
                return FVSetDevParam(lDevHandle, "REENROLLCHECK:0");
            }
        }
    }

    public static long SetOneFingerCheck(long lDevHandle, long lCheck) {
        if (mUsbDeviceConnection != null) {
            byte[] bData = new byte[16];
            bData[0] = (byte) (lCheck & 0x01);
            int ret = _SendPacket(0x0D, bData, 1);
            if (ret == 0) {
                _RecvPacket(null, 1000);
            }
            return XG_ERR_SUCCESS;
        } else {
            if (lCheck != 0) {
                return FVSetDevParam(lDevHandle, "ONEFINGERCHECK:1");
            } else {
                return FVSetDevParam(lDevHandle, "ONEFINGERCHECK:0");
            }
        }
    }

    public static long SetDevSecurityLevel(long lDevHandle, long lLevel) {
        if (mUsbDeviceConnection != null) {
            byte[] bData = new byte[16];
            bData[0] = (byte) (lLevel);
            int ret = _SendPacket(0x07, bData, 1);
            if (ret == 0) {
                _RecvPacket(null, 1000);
            }
            return XG_ERR_SUCCESS;
        } else {
            return FVSetDevParam(lDevHandle, "SECURITY:" + lLevel);
        }
    }

    public static long SetDevFingerTimeout(long lDevHandle, long lSec) {
        if (mUsbDeviceConnection != null) {
            byte[] bData = new byte[16];
            bData[0] = (byte) (lSec);
            int ret = _SendPacket(0x08, bData, 1);
            if (ret == 0) {
                _RecvPacket(null, 1000);
            }
            return XG_ERR_SUCCESS;
        } else {
            return FVSetDevParam(lDevHandle, "FINGERTIMEOUT:" + lSec);
        }
    }

    public static long SetDevFactory(long lDevHandle) {
        if (mUsbDeviceConnection != null) {
            int ret = _SendPacket(0x04, null, 0);
            if (ret == 0) {
                _RecvPacket(null, 1000);
            }
            return XG_ERR_SUCCESS;
        } else {
            return FVSetDevParam(lDevHandle, "FACTORY");
        }
    }

    public static long SendCmdPacket(long lDevHandle, int lCmd, String sData) {
        if (mUsbDeviceConnection != null) {
            if (sData != null) {
                byte[] bData = new byte[20];
                int len = (int) FVAsciiToHex(sData, bData);
                return _SendPacket(lCmd, bData, len);
            } else {
                return _SendPacket(lCmd, null, 0);
            }
        } else {
            return FVSendCmdPacket(lDevHandle, lCmd, sData);
        }
    }

    public static String RecvCmdPacket(long lDevHandle, int lTimeout) {
        if (mUsbDeviceConnection != null) {
            byte[] bData = new byte[20];
            int ret = _RecvPacket(bData, lTimeout);
            if (ret > 0) {
                return FVHexToAscii(bData, ret);
            }
        } else {
            return FVRecvCmdPacket(lDevHandle, lTimeout);
        }
        return null;
    }

//    public static long ConnectBT(BTSocket BtSocket) {
//        mBtSocket = BtSocket;
//        if (mBtSocket != null) {
//            long ret = _ConnectDev("BT", "00000000");
//            PrintfDebug("BT Connect ret:" + ret);
//            if (ret == 0) return mBtSocket.hashCode();
//        }
//        return 0;
//    }

    private static long _ConnectDev(String sDev, String sPassword) {
        int ret = _SendPacket(XG_CMD_CONNECTION, sPassword.getBytes(), sPassword.length());
        if (ret == XG_ERR_SUCCESS) {
            byte[] bData = new byte[20];
            ret = _RecvPacket(bData, 1000);
            if (ret > 0) {
                if (bData[0] == (byte) 0) {
                    //连接成功
                    String sName = new String(bData, 1, 14);
                    PrintfDebug(sName);
                    sDevInfo = "DEVNAME:" + sName;
                    return XG_ERR_SUCCESS;
                } else {
                    ret = bData[1] * -1;
                    PrintfDebug(Integer.toString(ret));
                    return ret;
                }
            }
        }
        return -1;
    }

    private static long _CloseDev() {
        int ret = _SendPacket(XG_CMD_CLOSE_CONNECTION, null, 0);
        if (ret == XG_ERR_SUCCESS) {
            _RecvPacket(null, 1000);
        }
        return -1;
    }

    private static String _GetVeinChara(long lTimeout) {
        int ret = -1;

        if (lTimeout > 0) {
            long finger = CheckFinger(0, (int) lTimeout, 1); //等待手指放入
            if (finger != XG_ERR_SUCCESS) return Integer.toString((int) finger); //如果返回错误则直接返回
        }

        if (_SendPacket(XG_CMD_GET_CHARA, null, 0) == XG_ERR_SUCCESS) {
            for (; ; ) {
                byte[] data = new byte[16];
                ret = _RecvPacket(data, 6000);
                if (ret > 0) { //接收到回包
                    if (data[0] == (byte) 0) { //采集成功
                        int size = (int) ((data[1] & 0xff) + ((data[2] & 0xff) << 8));
                        byte[] CharaData = new byte[size];
                        PrintfDebug("chara size " + size);
                        ret = _ReadData(XG_CMD_GET_CHARA, CharaData, size, 1000);
                        PrintfDebug("_ReadData ret " + ret);
                        if (ret == XG_ERR_SUCCESS) {
                            String sChara = FVEncodeBase64(CharaData, size);
                            //PrintfDebug(sChara);
                            return sChara;
                        }
                        break;
                    } else if (data[0] == (byte) XG_ERR_FAIL) {
                        ret = data[1];
                        break;
                    } else if (data[0] == (byte) XG_INPUT_FINGER) {
                    } else if (data[0] == (byte) XG_RELEASE_FINGER) {
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return Integer.toString(ret * -1);
    }

    private static long _CheckFinger() {
        int ret = _SendPacket(XG_CMD_FINGER_STATUS, null, 0);
        if (ret == XG_ERR_SUCCESS) {
            byte[] bData = new byte[20];
            ret = _RecvPacket(bData, 1000);
            if (ret > 0 && bData[0] == (byte) 0) {
                return (int) bData[1]; //1有手指 0无手指
            }
        }
        return -1;
    }

    private static String _GetDevSN() {
        int ret = _SendPacket(XG_CMD_GET_DUID, null, 0);
        if (ret == XG_ERR_SUCCESS) {
            byte[] bData = new byte[20];
            ret = _RecvPacket(bData, 1000);
            if (ret > 0 && bData[0] == (byte) 0) {
                sDevSN = new String(bData, 1, 15);
                PrintfDebug(sDevSN);
                return sDevSN;
            }
        }
        return null;
    }

    private static String _GetDevMaxUser() {
        int ret = _SendPacket(XG_CMD_GET_ENROLL_INFO, null, 0);
        if (ret == XG_ERR_SUCCESS) {
            byte[] bData = new byte[20];
            ret = _RecvPacket(bData, 1000);
            if (ret > 0 && bData[0] == (byte) 0) {
                int UserNum = (int) bData[1] + (int) bData[2] * 256;
                int MaxUser = (int) bData[9] + (int) bData[10] * 256;
                String str = String.format("MAXUSER:%d,USER:%d", MaxUser, UserNum);
                PrintfDebug(str);
                return str;
            }
        }
        return Integer.toString(ret);
    }

    private static String _GetDevSet() {
        int ret = _SendPacket(XG_CMD_GET_SYSTEM_INFO, null, 0);
        if (ret == XG_ERR_SUCCESS) {
            byte[] bData = new byte[20];
            ret = _RecvPacket(bData, 1000);
            if (ret > 0 && bData[0] == (byte) 0) {
                int[] baud = {9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600};
                if (bData[11] > 0x0F) bData[11] = (byte) 0;
                else bData[11] = (byte) (bData[11] + (byte) 1);
                String str = String.format("VER:%d.%02d,DEVID:%d,BAUD:%d,SECURITY:%d,FINGERTIMEOUT:%d,REENROLLCHECK:%d,ONEFINGERCHECK:%d,VOLUME:%d",
                        bData[1], bData[2], bData[3], baud[bData[4]], bData[5], bData[6], bData[7], bData[8], bData[11]);
                PrintfDebug(str);
                return str;
            }
        }
        return Integer.toString(ret);
    }

    private static String _ReadDevTemp(long user) {
        byte[] data = new byte[16];
        data[0] = (byte) (user & 0xff);
        data[1] = (byte) ((user >> 8) & 0xff);
        data[2] = 0;
        data[3] = 0;
        int ret = _SendPacket(XG_CMD_READ_ENROLL, data, 4);
        if (ret == XG_ERR_SUCCESS) {
            ret = _RecvPacket(data, 1000);
            if (ret > 0 && data[0] == (byte) 0) {
                int size = (data[1] & 0xff) + ((data[2] & 0xff) << 8);
                PrintfDebug("temp size " + size);
                byte[] bTempBuf = new byte[size];
                ret = _ReadData(XG_CMD_READ_ENROLL, bTempBuf, size, 1000);
                //PrintfDebug("_ReadData ret " + ret);
                if (ret == XG_ERR_SUCCESS) {
                    String sTemp = FVEncodeBase64(bTempBuf, size);
                    return sTemp;
                } else {
                    ret = -1;
                }
            } else {
                ret = data[1] * -1;
            }
        }
        return Integer.toString(ret);
    }

    private static int _WriteDevTemp(long user, String sTemp) {
        byte[] data = new byte[16];
        int size = sTemp.length();
        byte[] bTempBuf = new byte[size];
        size = (int) FVDecodeBase64(sTemp, bTempBuf);
        if (size < 1024) return XG_ERR_INVALID_PARAM * -1;
        data[0] = (byte) (user & 0xff);
        data[1] = (byte) ((user >> 8) & 0xff);
        data[2] = 0;
        data[3] = 0;
        data[4] = (byte) (size & 0xff);
        data[5] = (byte) ((size >> 8) & 0xff);
        data[6] = 0;
        data[7] = 0;
        int ret = _SendPacket(XG_CMD_WRITE_ENROLL, data, 8);
        if (ret == XG_ERR_SUCCESS) {
            ret = _RecvPacket(data, 1000);
            //PrintfDebug("RecvPacket ret " + ret);
            if (ret > 0 && data[0] == XG_ERR_SUCCESS) {
                ret = _WriteData(XG_CMD_WRITE_ENROLL, bTempBuf, size);
                PrintfDebug("_WriteData ret " + ret);
                return ret;
            } else {
                ret = data[1] * -1;
            }
        }
        return ret;
    }

    private static long _DelDevAllTemp() {
        byte[] data = new byte[16];
        int ret = _SendPacket(XG_CMD_CLEAR_ALL_ENROLL, null, 0);
        if (ret == XG_ERR_SUCCESS) {
            ret = _RecvPacket(data, 10000);
            if (ret > 0) {
                if (data[0] == (byte) 0)
                    return XG_ERR_SUCCESS;
                else
                    return data[0] * -1;
            }
        }
        return -1;
    }

    private static long _DelDevTemp(long lUserId) {
        byte[] data = new byte[16];
        data[0] = (byte) (lUserId & 0xff);
        data[1] = (byte) ((lUserId >> 8) & 0xff);
        data[2] = 0;
        data[3] = 0;
        int ret = _SendPacket(XG_CMD_CLEAR_ENROLL, data, 4);
        if (ret == XG_ERR_SUCCESS) {
            ret = _RecvPacket(data, 2000);
            if (ret > 0) {
                if (data[0] == (byte) 0)
                    return XG_ERR_SUCCESS;
                else
                    return data[0] * -1;
            }
        }
        return -1;
    }

    private static String _GetImgFormDev(long lTimeout, String sFileName) {
        int ret = 0;
        byte[] bData = new byte[16];
        bData[8] = 'J';
        ret = _SendPacket(0x24, bData, 16);
        if (ret == XG_ERR_SUCCESS) {
            ret = _RecvPacket(bData, (int) lTimeout);
            if (ret > 0) { //接收到回包
                if (bData[0] == (byte) 0) { //采集成功
                    int Width = bData[1] + (bData[2] << 8);
                    int Height = (bData[3] & 0xff);
                    int size = (int) ((bData[8] & 0xff) + ((bData[9] & 0xff) << 8));
                    PrintfDebug("Width:" + Width + ",Height:" + Height + ",size:" + size);
                    if (size < 1024) {
                        size = Width * Height;
                    }
                    byte[] bBmp = new byte[size];
                    ret = _ReadData(0x24, bBmp, size, 1000);
                    if (ret == XG_ERR_SUCCESS) {
                        String sBmp = FVHexToAscii(bBmp, size);
                        if (size == Width * Height) {
                            //BMP数据要传入宽和高
                            sBmp = sBmp + ",WIDTH:" + Width + ",HEIGHT:" + Height + "SN:" + sDevSN;
                        }
                        String str = FVGetImgFormData(sBmp, size, sFileName);
                        return str;
                    }
                } else {
                    ret = bData[1] * -1;
                }
            }
        }
        return Integer.toString(ret);
    }

    //计算校验和
    private static short _GetCheckSum(byte[] pBuf, int Size) {
        short w_Ret = 0;
        int i;
        for (i = 0; i < Size; i++)
            w_Ret += (pBuf[i] & 0xff);
        return w_Ret;
    }

    private static int _SendPacket(int cmd, byte[] data, int len) {
        int ret = 0;
        byte[] buf = new byte[24];
        for (int i = 0; i < 24; i++) {
            buf[i] = 0;
        }
        buf[0] = (byte) 0xbb;
        buf[1] = (byte) 0xaa;
        buf[2] = (byte) 0; //addr
        buf[3] = (byte) (cmd & 0xff); //cmd
        buf[4] = (byte) 0; //encode
        buf[5] = (byte) (len & 0xff);
        if (data != null && len > 0) {
            System.arraycopy(data, 0, buf, 6, len);
        }
        short checknum = _GetCheckSum(buf, 22);
        buf[22] = (byte) (checknum & 0xff);
        buf[23] = (byte) ((checknum >> 8) & 0xff);
//        if (mBtSocket != null) {
//            //蓝牙也就是串口发送2个指令要有点间隔
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException e) {
//            }
//            ret = mBtSocket.Write(buf, 24);
//        }
        if (mUsbDeviceConnection != null) {
            ret = _UsbSendBuf(buf, 24);
        }
        if (ret > 0) return 0;
        return -1;
    }

    private static int _RecvPacket(byte[] data, int timeout) {
        int size = 0;
        byte[] buf = new byte[24];

//        if (mBtSocket != null) {
//            size = mBtSocket.Read(buf, 0, 24, timeout);
//        }
        if (mUsbDeviceConnection != null) {
            size = _UsbRecvBuf(buf, 24, timeout);
        }
        //PrintfDebug("RecvPacket size:" + size);
        if (size >= 24) {
            int len = buf[5];
            if (buf[0] == (byte) (0xbb) && buf[1] == (byte) (0xaa) && len <= 16) {
                //包头和命令正确
                short checknum = _GetCheckSum(buf, 22);
                if (buf[22] == (byte) (checknum & 0xff) && buf[23] == (byte) ((checknum >> 8) & 0xff)) {
                    //校验和正确
                    if (data != null) {
                        System.arraycopy(buf, 6, data, 0, 16);
                    }
                    return len;
                }
            }
        } else {
            PrintfDebug("ERROR:size=" + size);
        }
        return 0;
    }

    public static int _ReadDataPack(int cmd, byte[] data, int offset, int size, int timeout) {
        int ret = 0;
        int recv_size = 0;
        byte[] buf = new byte[size + 64];
        byte[] cmddata = new byte[16];
        cmddata[0] = (byte) cmd;
        cmddata[1] = (byte) (offset & 0xff);
        cmddata[2] = (byte) ((offset >> 8) & 0xff);
        cmddata[3] = (byte) ((offset >> 16) & 0xff);
        cmddata[4] = (byte) ((offset >> 24) & 0xff);
        cmddata[5] = (byte) (size & 0xff);
        cmddata[6] = (byte) ((size >> 8) & 0xff);
        cmddata[7] = (byte) ((size >> 16) & 0xff);
        cmddata[8] = (byte) ((size >> 24) & 0xff);
        _SendPacket(XG_CMD_READ_DATA, cmddata, 9);
//        if (mBtSocket != null) {
//            recv_size = mBtSocket.Read(buf, 0, size + 2, timeout);
//        }
        if (mUsbDeviceConnection != null) {
            recv_size = _UsbRecvBuf(buf, size + 2, timeout);
        }
        //PrintfDebug("_ReadDataPack size:" + size);
        if (recv_size >= size + 2) {
            short CheckSum2 = (short) ((buf[size] & 0xff) + ((buf[size + 1] & 0xff) << 8));
            short CheckSum1 = _GetCheckSum(buf, size);
            //PrintfDebug("CheckSum2:" + CheckSum2 + ",CheckSum1:" + CheckSum1);
            if (CheckSum1 == CheckSum2) {
                System.arraycopy(buf, 0, data, offset, size);
                ret = size;
            } else {
                ret = 0;
            }
        } else {
            PrintfDebug("ERROR:recv_size=" + recv_size);
        }
        return ret;
    }

    public static int _WriteDataPack(int cmd, byte[] data, int offset, int size) {
        int ret;
        byte[] buf = new byte[size + 64];
        byte[] cmddata = new byte[16];
        cmddata[0] = (byte) cmd;
        cmddata[1] = (byte) (offset & 0xff);
        cmddata[2] = (byte) ((offset >> 8) & 0xff);
        cmddata[3] = (byte) ((offset >> 16) & 0xff);
        cmddata[4] = (byte) ((offset >> 24) & 0xff);
        cmddata[5] = (byte) (size & 0xff);
        cmddata[6] = (byte) ((size >> 8) & 0xff);
        cmddata[7] = (byte) ((size >> 16) & 0xff);
        cmddata[8] = (byte) ((size >> 24) & 0xff);
        ret = _SendPacket(XG_CMD_WRITE_DATA, cmddata, 9);
        if (ret != XG_ERR_SUCCESS) {
            //PrintfDebug("SendPacket XG_CMD_WRITE_DATA fail");
            return 0;
        }
        System.arraycopy(data, offset, buf, 0, size);
        short CheckSum = _GetCheckSum(buf, size);
        buf[size] = (byte) (CheckSum & 0xff);
        buf[size + 1] = (byte) ((CheckSum >> 8) & 0xff);
//        if (mBtSocket != null) {
//            ret = mBtSocket.Write(buf, size + 2);
//        }
        if (mUsbDeviceConnection != null) {
            ret = _UsbSendBuf(buf, size + 2);
        }
        //PrintfDebug("sendbuf ret:" + ret);
        ret = _RecvPacket(cmddata, 1000);
        //PrintfDebug("RecvPacket ret:" + ret + " cmddata[0] " + cmddata[0]);
        if (ret > 0 && cmddata[0] == XG_ERR_SUCCESS) {
            return size;
        }
        return 0;
    }

    public static int _ReadData(int cmd, byte[] data, int size, int timeout) {
        int ret = 0;
        int lPkNum, lPkRec, lDataMove, i;
        int reReadCnt = 0;
        int PackSize = 0;

//        if (mBtSocket != null) {
//            PackSize = 512 - 2;
//        }
        if (mUsbDeviceConnection != null) {
            PackSize = 4096 - 2;
        }
        //分包
        lPkNum = (size) / PackSize;
        lPkRec = (size) % PackSize;
        lDataMove = 0;
        //PrintfDebug("lPkNum:" + lPkNum + ", " + lPkRec);
        for (i = 0; i < lPkNum; i++) {
            ret = _ReadDataPack(cmd, data, lDataMove, PackSize, timeout);
            //PrintfDebug("_ReadDataPack ret:" + ret);
            if (ret == PackSize) {
                reReadCnt = 0;
                lDataMove += PackSize;
            } else {
                i--;
                if (reReadCnt++ > 2) {
                    return -2;
                }
            }
        }

        if (lPkRec > 0) {
            ret = _ReadDataPack(cmd, data, lDataMove, lPkRec, timeout);
            if (ret == lPkRec) {
                lDataMove += lPkRec;
            }
        }
        if (lDataMove == size) return XG_ERR_SUCCESS;
        return XG_ERR_FAIL;
    }

    public static int _WriteData(int cmd, byte[] data, int size) {
        int ret = 0;
        int lPkNum, lPkRec, lDataMove, i;
        int reWriteCnt = 0;
        int PackSize = 0;

//        if (mBtSocket != null) {
//            PackSize = 512 - 2;
//        }
        if (mUsbDeviceConnection != null) {
            PackSize = 4096 - 2;
        }
        //分包
        lPkNum = (size) / PackSize;
        lPkRec = (size) % PackSize;
        lDataMove = 0;
        //PrintfDebug("lPkNum:" + lPkNum + ", " + lPkRec);
        for (i = 0; i < lPkNum; i++) {
            ret = _WriteDataPack(cmd, data, lDataMove, PackSize);
            if (ret == PackSize) {
                reWriteCnt = 0;
                lDataMove += PackSize;
            } else {
                i--;
                PrintfDebug("reWriteCnt " + reWriteCnt);
                if (reWriteCnt++ > 2) {
                    return -2;
                }
            }
        }

        if (lPkRec > 0) {
            ret = _WriteDataPack(cmd, data, lDataMove, lPkRec);
            if (ret == lPkRec) {
                lDataMove += lPkRec;
            }
        }
        //PrintfDebug("lDataMove " + lDataMove);
        if (lDataMove == size) return XG_ERR_SUCCESS;
        return XG_ERR_FAIL;
    }

    //初始化USB通讯
    public static long initUsbCommunication(UsbDevice device, UsbManager mUsbManager) {
        int interfaceCount = device.getInterfaceCount();
        PrintfDebug("initCommunication interfaceCount " + interfaceCount);
        for (int interfaceIndex = 0; interfaceIndex < interfaceCount; interfaceIndex++) {
            UsbInterface usbInterface = device.getInterface(interfaceIndex);
            PrintfDebug("getInterfaceClass " + usbInterface.getInterfaceClass());
            int enConut = usbInterface.getEndpointCount();
            PrintfDebug("enConut " + enConut);
            for (int i = 0; i < enConut; i++) {
                UsbEndpoint ep = usbInterface.getEndpoint(i);
                PrintfDebug("ep.getType " + ep.getType());
                PrintfDebug("ep.getDirection " + ep.getDirection());
                if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                        mUsbEndpointOut = ep;
                    } else {
                        mUsbEndpointIn = ep;
                    }
                }
            }

            if ((null == mUsbEndpointIn) || (null == mUsbEndpointOut)) {
                PrintfDebug("endpoint is null");
                mUsbEndpointIn = null;
                mUsbEndpointOut = null;
            } else {
                PrintfDebug("endpoint out: " + mUsbEndpointOut + ",endpoint in: " + mUsbEndpointIn.getAddress());
                mUsbDeviceConnection = mUsbManager.openDevice(device);
                mUsbDeviceConnection.claimInterface(usbInterface, true);
                long ret = _ConnectDev("APPUSB", "00000000");
                if (ret == XG_ERR_SUCCESS) {
                    return mUsbDeviceConnection.hashCode();
                    //return HANDLE_APP_DEV_USB;
                } else {
                    return ret;
                }
            }
        }
        return -1;
    }

    ;

    //通过USB发送数据
    private static synchronized int _UsbSendBuf(byte[] buf, int size) {
        int ret;
        int timeout = 10000;
        if (false) //HID
        {
            while (true) {
                int datalen = size;
                int offset = 0;
                byte[] hid_buf = new byte[64];
                hid_buf[0] = (byte) 0x01;
                hid_buf[1] = (byte) 'X';
                hid_buf[3] = (byte) 0x00;
                if (datalen > 60) {
                    hid_buf[2] = (byte) 60;
                    System.arraycopy(buf, offset, hid_buf, 4, 60);
                    offset += 60;
                    datalen -= 60;
                } else {
                    hid_buf[2] = (byte) (datalen & 0xff);
                    System.arraycopy(buf, offset, hid_buf, 4, datalen);
                    offset += datalen;
                    datalen -= datalen;
                }
                ret = mUsbDeviceConnection.controlTransfer(0x21, 0x09, 0x301, 0, hid_buf, 64, timeout);
                PrintfDebug("controlTransfer:" + ret + ",size:" + offset);
                if (offset >= size) break;
            }

        } else {
            byte[] bycbw = new byte[31];
            byte[] bycsw = new byte[64];

            bycbw[0] = (byte) 0x55;
            bycbw[1] = (byte) 0x53;
            bycbw[2] = (byte) 0x42;
            bycbw[3] = (byte) 0x43;

            bycbw[4] = (byte) 'X';
            bycbw[5] = (byte) 'G';
            bycbw[6] = (byte) 'Z';
            bycbw[7] = (byte) 'X';

            bycbw[8] = (byte) (size & 0xFF);
            bycbw[9] = (byte) ((size >> 8) & 0xFF);
            bycbw[10] = (byte) ((size >> 16) & 0xFF);
            bycbw[11] = (byte) ((size >> 24) & 0xFF);

            bycbw[12] = (byte) 0x00;
            bycbw[13] = (byte) 0x00;
            bycbw[14] = (byte) 0x0A;

            bycbw[15] = (byte) 0x86;
            bycbw[16] = (byte) 0x00;

            ret = mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, bycbw, 31, timeout);
            if (ret != 31) {
                return 0;
            }
            ret = mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, buf, size, timeout);
            if (ret != size) {
                return 0;
            }
            ret = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, bycsw, bycsw.length, timeout);
            if (ret != 13) {
                return 0;
            }
        }
        //PrintfDebug("sendsize:" + size);
        return size;
    }

    ;

    //通过USB接收数据
    private static synchronized int _UsbRecvBuf(byte[] buf, int size, int timeout) {
        int ret;
        int Timeout = timeout * 100;
        int recvsize = 0;

        if (false) //HID
        {
            while (true) {
                int offset = 0;
                byte[] hid_buf = new byte[256];
                hid_buf[0] = (byte) 0x01;

                ret = mUsbDeviceConnection.controlTransfer(0xA1, 0x01, 0x302, 0, hid_buf, 64, timeout);
                recvsize = (int) hid_buf[2];
                PrintfDebug("controlTransfer:" + ret);
                if (hid_buf[1] == (byte) 'X' && recvsize > 0) {
                    System.arraycopy(hid_buf, 4, buf, offset, recvsize);
                    offset += recvsize;
                    if (offset >= size) break;
                }
            }

        } else {
            byte[] bycbw = new byte[31];
            byte[] bycsw = new byte[64];
            int receiveLen = 64;

            bycbw[0] = (byte) 0x55;
            bycbw[1] = (byte) 0x53;
            bycbw[2] = (byte) 0x42;
            bycbw[3] = (byte) 0x43;

            bycbw[4] = (byte) 'X';
            bycbw[5] = (byte) 'G';
            bycbw[6] = (byte) 'Z';
            bycbw[7] = (byte) 'X';

            bycbw[8] = (byte) (receiveLen & 0xFF);
            bycbw[9] = (byte) ((receiveLen >> 8) & 0xFF);
            bycbw[10] = (byte) ((receiveLen >> 16) & 0xFF);
            bycbw[11] = (byte) ((receiveLen >> 24) & 0xFF);

            bycbw[12] = (byte) 0x80;
            bycbw[13] = (byte) 0x00;
            bycbw[14] = (byte) 0x0A;

            bycbw[15] = (byte) 0x85;
            bycbw[16] = (byte) 0x00;

            ret = mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, bycbw, 31, Timeout);
            if (ret != 31) {
                return 0;
            }
            recvsize = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, buf, size, Timeout);
            if (recvsize < 2) {
                return 0;
            }
            ret = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, bycsw, bycsw.length, Timeout);
            if (ret != 13) {
                return 0;
            }
        }
        //PrintfDebug("recvsize:" + recvsize);
        return recvsize;
    }

    public static byte[] StrToGBKBytes(String str) {
        try {
            return str.getBytes("GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String BytestoGBKStr(byte[] bytes, int lBytes) {
        String strGBK = "";
        if (lBytes > 0) {
            try {
                strGBK = new String(bytes, 0, lBytes, "GBK");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return strGBK;
    }
}
