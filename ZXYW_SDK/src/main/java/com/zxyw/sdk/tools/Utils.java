package com.zxyw.sdk.tools;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.LinkAddress;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static android.content.Context.ALARM_SERVICE;

public class Utils {

    /**
     * 获取root
     */
    public static void requestSU() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream out = new DataOutputStream(process.getOutputStream());
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 延时重启
     *
     * @param delay 延时毫秒
     */
    public static void reboot(long delay) {
        if (delay <= 0) runRootCommand("reboot");
        else {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    runRootCommand("reboot");
                }
            }, delay + 2000);
        }
    }

    private static void reboot() {
        String cmd = "su reboot";
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void screenAwake() {
        runRootCommand("input keyevent 26");
    }

    public static boolean runRootCommand(String command) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception ignore) {
            }
        }
        return true;
    }

    /**
     * 获取本机IP
     *
     * @return IP字符串
     */
    public static String getHostIP() {
        String hostIp = null;
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            MyLog.i("yao", "SocketException");
            e.printStackTrace();
        }
        return hostIp;
    }

    /**
     * 设置屏幕透明度
     *
     * @param activity 页面
     * @param alpha    0~1
     */
    public static void setAlpha(Activity activity, float alpha) {
        if (activity == null) return;
        Window window = activity.getWindow();
        if (window == null) return;
        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;
        WindowManager.LayoutParams params = window.getAttributes();
        params.alpha = alpha;
        window.setAttributes(params);
    }

    /**
     * 获取网关
     *
     * @return 网关
     */
    public static String getGateWay() {
        String[] arr;
        try {
            Process process = Runtime.getRuntime().exec("ip route list table 0");
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String string = in.readLine();
            arr = string.split("\\s+");
            return arr[2];
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "error";
    }

    /**
     * 获取子网掩码
     *
     * @return 子网掩码
     */
    public static String getNetMask() {
        try {
            Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();    //获取本机所有的网络接口
            while (networkInterfaceEnumeration.hasMoreElements()) { //判断 Enumeration 对象中是否还有数据
                NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement(); //获取 Enumeration 对象中的下一个数据
                if (!networkInterface.isUp() && !"eth0".equals(networkInterface.getDisplayName())) { //判断网口是否在使用，判断是否时我们获取的网口
                    continue;
                }
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {    //
                    if (interfaceAddress.getAddress() instanceof Inet4Address) {    //仅仅处理ipv4
                        return calcMaskByPrefixLength(interfaceAddress.getNetworkPrefixLength());   //获取掩码位数，通过 calcMaskByPrefixLength 转换为字符串
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "error";
    }

    private static String calcMaskByPrefixLength(int length) {
        int mask = 0xffffffff << (32 - length);
        int partsNum = 4;
        int bitsOfPart = 8;
        int[] maskParts = new int[partsNum];
        int selector = 0x000000ff;

        for (int i = 0; i < maskParts.length; i++) {
            int pos = maskParts.length - 1 - i;
            maskParts[pos] = (mask >>> (i * bitsOfPart)) & selector;
        }

        StringBuilder result = new StringBuilder();
        result.append(maskParts[0]);
        for (int i = 1; i < maskParts.length; i++) {
            result.append(".").append(maskParts[i]);
        }
        return result.toString();
    }

    @SuppressWarnings("all")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void updateIP(Context context, final String IP, final String mask, final String gateway) {
        try {
            Class<?> staticIpConfigClass = Class.forName("android.net.StaticIpConfiguration");
            Object staticIpConfigInstance = staticIpConfigClass.newInstance();
            Class<?> networkUtilsClass = Class.forName("android.net.NetworkUtils");
            Method numericToInetAddress = networkUtilsClass.getDeclaredMethod("numericToInetAddress", String.class);
            Inet4Address inetAddr = (Inet4Address) numericToInetAddress.invoke(null, IP);
            Constructor<?> linkAddressConstructor = LinkAddress.class.getDeclaredConstructor(InetAddress.class, int.class);

            int maskInt = subnetMask2Int(mask);

            //实例化带String类型的构造方法
            LinkAddress linkAddress = (LinkAddress) linkAddressConstructor.newInstance(inetAddr, maskInt);
            Field ipAddress = staticIpConfigClass.getDeclaredField("ipAddress");
            ipAddress.set(staticIpConfigInstance, linkAddress);

            InetAddress gatewayAddr = (InetAddress) numericToInetAddress.invoke(null, gateway);
            Field gatewayField = staticIpConfigClass.getDeclaredField("gateway");
            gatewayField.set(staticIpConfigInstance, gatewayAddr);

            InetAddress dnsAddr = (InetAddress) numericToInetAddress.invoke(null, "8.8.8.8");

            Field dnsServers = staticIpConfigClass.getDeclaredField("dnsServers");
            ArrayList<InetAddress> dnsList = (ArrayList<InetAddress>) dnsServers.get(staticIpConfigInstance);
            dnsList.add(dnsAddr);

            //获取ETHERNET_SERVICE参数
            String ETHERNET_SERVICE = (String) Context.class.getField("ETHERNET_SERVICE").get(null);
            Class<?> ethernetManagerClass = Class.forName("android.net.EthernetManager");
            //获取ethernetManager服务对象
            Object ethernetManager = context.getSystemService(ETHERNET_SERVICE);

            Class<?> ipConfigurationClass = Class.forName("android.net.IpConfiguration");
            Class<?> ipAssignmentClass = Class.forName("android.net.IpConfiguration$IpAssignment");
            Class<?> proxySettingsClass = Class.forName("android.net.IpConfiguration$ProxySettings");
            Field NONE = proxySettingsClass.getDeclaredField("NONE");
            Field STATIC = ipAssignmentClass.getDeclaredField("STATIC");

            //获取ipConfiguration类的构造方法
            Constructor<?>[] ipConfigConstructors = ipConfigurationClass.getDeclaredConstructors();
            Object ipConfigurationInstance = null;

            for (Constructor constru : ipConfigConstructors) {
                //获取ipConfiguration类的4个参数的构造方法
                if (constru.getParameterTypes().length == 4) {//设置以上四种类型
                    //初始化ipConfiguration对象,设置参数
                    ipConfigurationInstance = constru.newInstance(STATIC.get(null), NONE.get(null), staticIpConfigInstance, null);
                    break;
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Method setConfiguration = ethernetManagerClass.getDeclaredMethod("setConfiguration", String.class, ipConfigurationClass);
                setConfiguration.invoke(ethernetManager, "eth0", ipConfigurationInstance);
            } else {
                Method setConfiguration = ethernetManagerClass.getDeclaredMethod("setConfiguration", ipConfigurationClass);
                setConfiguration.invoke(ethernetManager, ipConfigurationInstance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int subnetMask2Int(String mask) {
        if (TextUtils.isEmpty(mask)) {
            return 24;
        }
        int value = 24;
        String[] maskSubs = mask.split("\\.");
        if (maskSubs.length != 4) {
            return 24;
        }
        for (int i = 0; i < 4; i++) {
            int maskSubValue;
            try {
                maskSubValue = Integer.parseInt(maskSubs[i]);
            } catch (Exception e) {
                return 24;
            }
            if (maskSubValue <= 0) {
                maskSubValue = 0;
            } else if (maskSubValue < 192) {
                maskSubValue = 128;
            } else if (maskSubValue < 224) {
                maskSubValue = 192;
            } else if (maskSubValue < 240) {
                maskSubValue = 224;
            } else if (maskSubValue < 248) {
                maskSubValue = 240;
            } else if (maskSubValue < 252) {
                maskSubValue = 248;
            } else if (maskSubValue < 254) {
                maskSubValue = 252;
            } else {
                maskSubValue = 255;
            }
            maskSubs[i] = maskSubValue + "";
        }
        if ("0".equals(maskSubs[0])) {
            maskSubs[0] = "192";
        }
        for (int i = 3; i > 0; i--) {
            if (!"0".equals(maskSubs[i])) {
                maskSubs[i - 1] = "255";
            }
        }
        String maskStr = maskSubs[0] + "." + maskSubs[1] + "." + maskSubs[2] + "." + maskSubs[3];

        switch (maskStr) {
            case "128.0.0.0":
                value = 1;
                break;
            case "192.0.0.0":
                value = 2;
                break;
            case "224.0.0.0":
                value = 3;
                break;
            case "240.0.0.0":
                value = 4;
                break;
            case "248.0.0.0":
                value = 5;
                break;
            case "252.0.0.0":
                value = 6;
                break;
            case "254.0.0.0":
                value = 7;
                break;
            case "255.0.0.0":
                value = 8;
                break;
            case "255.128.0.0":
                value = 9;
                break;
            case "255.192.0.0":
                value = 10;
                break;
            case "255.224.0.0":
                value = 11;
                break;
            case "255.240.0.0":
                value = 12;
                break;
            case "255.248.0.0":
                value = 13;
                break;
            case "255.252.0.0":
                value = 14;
                break;
            case "255.254.0.0":
                value = 15;
                break;
            case "255.255.0.0":
                value = 16;
                break;
            case "255.255.128.0":
                value = 17;
                break;
            case "255.255.192.0":
                value = 18;
                break;
            case "255.255.224.0":
                value = 19;
                break;
            case "255.255.240.0":
                value = 20;
                break;
            case "255.255.248.0":
                value = 21;
                break;
            case "255.255.252.0":
                value = 22;
                break;
            case "255.255.254.0":
                value = 23;
                break;
            case "255.255.255.0":
                value = 24;
                break;
            case "255.255.255.128":
                value = 25;
                break;
            case "255.255.255.192":
                value = 26;
                break;
            case "255.255.255.224":
                value = 27;
                break;
            case "255.255.255.240":
                value = 28;
                break;
            case "255.255.255.248":
                value = 29;
                break;
            case "255.255.255.252":
                value = 30;
                break;
            case "255.255.255.254":
                value = 31;
                break;
            case "255.255.255.255":
                value = 32;
                break;
        }
        return value;
    }

    /**
     * 图片文件转base64字符串
     *
     * @param context context
     * @param picPath 图片文件路径
     * @return base64编码后的字符串
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static String imageToBase64(Context context, String picPath) {
        if (TextUtils.isEmpty(picPath)) return "";
        InputStream in;
        byte[] data = null;
        // 读取图片字节数组
        try {
//            List<File> files = Luban.with(context).load(new File(picPath)).get();
//            String absolutePath = files.get(0).getAbsolutePath();
//            in = new FileInputStream(absolutePath);
            in = new FileInputStream(picPath);
            data = new byte[in.available()];
            in.read(data);
            in.close();
//            if (!picPath.equals(absolutePath)) {
//                files.get(0).delete();
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 对字节数组Base64编码
        if (data == null)
            return "";
        return Base64.encodeToString(data, Base64.DEFAULT);// 返回Base64编码过的字节数组字符串
    }

    /**
     * base64字符串转为图片文件
     *
     * @param imgStr  base64格式字符串
     * @param picPath 图片文件存储的路径
     * @return 是否转成功
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean Base64ToImage(String imgStr, String picPath) {
        // System.out.println(picPath);
        if (TextUtils.isEmpty(imgStr) || TextUtils.isEmpty(picPath)) // 图像数据为空
            return false;
        try {
            // Base64解码
            byte[] b = Base64.decode(imgStr, Base64.DEFAULT);
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {// 调整异常数据
                    b[i] += 256;
                }
            }
            if (b.length < 1) {
                return false;
            }
            // 生成jpeg图片
            File f = new File(picPath);
            File parent = new File(f.getParent());
            if (!parent.exists() || !parent.isDirectory()) {
                parent.mkdir();
            }
            // File f2 = new File(f.getPath(),f.getName());
            if (f.exists()) f.delete();
            f.createNewFile();
            OutputStream out = new FileOutputStream(picPath);
            out.write(b);
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取版本名
     */
    public static String getAppVersionName(Context context) {
        String versionName = "";
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
            if (TextUtils.isEmpty(versionName)) {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionName;
    }

    /**
     * 获取cpu序列号
     *
     * @return cpu序列号
     */
    public static String getCPUSerial() {
        String str, strCPU, cpuAddress = "0000000000000000";
        try {// 读取CPU信息
            Process pp = Runtime.getRuntime().exec("cat /proc/cpuinfo");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            // 查找CPU序列号
            for (int i = 1; i < 100; i++) {
                str = input.readLine();
                if (str != null) {
                    if (str.contains("Serial")) {// 查找到序列号所在行
                        strCPU = str.substring(str.indexOf(":") + 1);// 提取序列号
                        cpuAddress = strCPU.trim();// 去空格
                        break;
                    }
                } else {
                    break;// 文件结尾
                }
            }
        } catch (IOException ex) {
            // 赋予默认值
            ex.printStackTrace();
        }
        return cpuAddress;
    }

    /**
     * 将文件中的字符串读出来
     *
     * @param file 文件
     * @return 字符串
     */
    public static String getStringFromFile(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return "";
        }
        char[] chars = new char[10240];
        try (FileReader reader = new FileReader(file)) {
            int len = reader.read(chars);
            StringBuilder builder = new StringBuilder();
            while (len > 0) {
                builder.append(new String(chars, 0, len));
                len = reader.read(chars);
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static void silentInstall(final String apkPath) {
        if (TextUtils.isEmpty(apkPath)) {
            return;
        }
        Thread t = new Thread() {
            @Override
            public void run() {
                DataOutputStream dataOutputStream = null;
//                BufferedReader errorStream = null;
                try {
                    Process process = Runtime.getRuntime().exec("su");
                    dataOutputStream = new DataOutputStream(process.getOutputStream());
                    String command = "pm install -r " + apkPath + "\n";
                    dataOutputStream.write(command.getBytes());
                    dataOutputStream.flush();
                    dataOutputStream.writeBytes("exit\n");
                    dataOutputStream.flush();
                    process.waitFor();
//                    errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//                    StringBuilder msg = new StringBuilder();
//                    String line;
//                    // 读取命令的执行结果
//                    while ((line = errorStream.readLine()) != null) {
//                        msg.append(line);
//                    }
//                    Log.d("TAG", "install msg is " + msg);
//                    // 如果执行结果中包含Failure字样就认为是安装失败，否则就认为安装成功
//                    if (msg.toString().toLowerCase().contains("success")) {
//                        Speaker.getInstance().speak("安装成功");
//                    } else {
//                        Speaker.getInstance().speak("安装失败");
//                    }
                } catch (Exception e) {
                    Log.e("TAG", e.getMessage(), e);
                } finally {
                    try {
                        if (dataOutputStream != null) {
                            dataOutputStream.close();
                        }
//                        if (errorStream != null) {
//                            errorStream.close();
//                        }
                    } catch (Exception e) {
                        Log.e("TAG", e.getMessage(), e);
                    }
                }
            }
        };
        t.start();
    }

    /**
     * 转换指纹类型
     *
     * @param type 1 主指纹；2 备份指纹；3胁迫指纹
     */
    public static String getConvertFingerId(String fingerId, int type) {
        if (fingerId == null || fingerId.length() != 9) return null;
        if (type != 1 && type != 2 && type != 3) return null;
        char[] chars = fingerId.toCharArray();
        chars[1] = (char) ('0' + type);
        return new String(chars);
    }

    /**
     * 转换指纹类型
     *
     * @param type 1 主指纹；2 备份指纹；3胁迫指纹
     */
    public static int getConvertFingerId(int fingerId, int type) {
        String s = getConvertFingerId(String.valueOf(fingerId), type);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 获取指纹类型
     *
     * @param fingerId 指纹ID
     * @return 指纹类型 1主指纹 2备份指纹 3胁迫指纹 0其他
     */
    public static int getFingerType(String fingerId) {
        if (fingerId == null || fingerId.length() != 9) return 0;
        return fingerId.charAt(1) - '0';
    }

    public static void deleteFiles(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File value : files) {
                    deleteFiles(value);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    public static void deleteFiles(File file, long time) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File value : files) {
                    deleteFiles(value);
                }
            }
        }
        if ((System.currentTimeMillis() - file.lastModified()) > time) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    /**
     * 16进制字符串转成字节数组，字符串需要特定的格式，每个字节以空格分开或者连在一起，不能有“0x”或者别的分隔符号
     */
    public static byte[] hexStringToByteArray(String hexString) {
        hexString = hexString.replaceAll(" ", "");
        int len = hexString.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个字节
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }

    /**
     * 检查ip是否正确
     *
     * @param ip ip
     * @return result
     */
    public static boolean checkIpLegal(String ip) {
        if (TextUtils.isEmpty(ip)) return false;
        Pattern pattern = Pattern.compile("\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b");
        Matcher m = pattern.matcher(ip);
        return m.matches();
    }

    public static boolean checkPortLegal(String port) {
        try {
            int p = Integer.parseInt(port);
            return p > 1024 && p < 65536;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 字节数组转成字符串，以空格作为每个字节的分隔符
     */
    public static String bytes2string(byte[] bytes) {
        if (bytes == null) return "";
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        StringBuilder builder = new StringBuilder();
        char[] temp = new char[2];
        for (byte aByte : bytes) {
            int v = aByte & 0xFF;
            temp[0] = hexArray[v >>> 4];
            temp[1] = hexArray[v & 0x0F];
//            builder.append("0x");
            builder.append(temp);
            builder.append(" ");
        }
        return builder.toString();
    }

    public static String getEqueID() {
        //获取cpu序列号
        String CPUSerial = getCPUSerial();
        String result = CPUSerial.substring(0, 8);
        result = result + CPUSerial.substring(CPUSerial.length() - 8);
        return result;
    }

    public static void setSystemTime(Context context, Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        long when = calendar.getTimeInMillis();
        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setTime(when);
        AlarmManager alarm = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarm.setTimeZone("Asia/Shanghai");
    }

    public static int getNumFromString(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            return 0;
        }
    }

    public static String getMacAddress() {
        String macSerial = null;
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/eth0/address");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            while (null != str) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return macSerial;
    }

    public static byte[] crcModbus(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        int CRC = 0x0000ffff;
        int POLYNOMIAL = 0x0000a001;
        int i, j;
        for (i = 0; i < bytes.length; i++) {
            CRC ^= ((int) bytes[i] & 0x000000ff);
            for (j = 0; j < 8; j++) {
                if ((CRC & 0x00000001) == 1) {
                    CRC >>= 1;
                    CRC ^= POLYNOMIAL;
                } else {
                    CRC >>= 1;
                }
            }
        }
//        高低位转换
//        CRC = ((CRC & 0xFF00) >> 8) | ((CRC & 0x00FF) << 8);
        return int2Bytes(CRC);
    }

    private static byte[] int2Bytes(int i) {
        byte[] result = new byte[2];
        result[1] = (byte) ((i >>> 8) & 0xFF);
        result[0] = (byte) (i & 0xFF);
        return result;
    }

    /**
     * 16进制字符串转字节数组
     */
    public static byte[] string2bytes(String s) {
        if (s == null) return new byte[]{};
        if (s.length() % 2 != 0) {
            s = "0" + s;
        }
        s = s.toLowerCase();
        byte[] bytes = new byte[s.length() / 2];
        String a = "0123456789abcdef";
        for (int i = 0; i < bytes.length; i += 2) {
            bytes[i] = (byte) ((a.indexOf(s.charAt(i)) & 0x0F) << 4 | a.indexOf(s.charAt(i + 1)) & 0x0F);
        }
        return bytes;
    }

    public static void sizeCompress(Bitmap bmp, File file) {
        // 尺寸压缩倍数,值越大，图片尺寸越小
        int ratio = 2;
        Bitmap result = Bitmap.createBitmap(bmp.getWidth() / ratio, bmp.getHeight() / ratio, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(result);
        Rect rect = new Rect(0, 0, bmp.getWidth() / ratio, bmp.getHeight() / ratio);
        canvas.drawBitmap(bmp, null, rect, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.compress(Bitmap.CompressFormat.JPEG, 30, baos);
        try {
            if (!file.exists() || file.isDirectory()) {
                if (!file.createNewFile()) {
                    return;
                }
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String string2MD5(String inStr) {
        if (TextUtils.isEmpty(inStr)) return "";
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        char[] charArray = inStr.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++)
            byteArray[i] = (byte) charArray[i];
        byte[] md5Bytes = md5.digest(byteArray);
        StringBuilder hexValue = new StringBuilder();
        for (byte md5Byte : md5Bytes) {
            int val = ((int) md5Byte) & 0xff;
            if (val < 16)
                hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    public static int bytes2Int(byte[] bytes) {
        int a = 0;
        for (int i = 0; i < 4; i++) {
            try {
                a += (bytes[i] & 0x000000FF) << (24 - 8 * i);
            } catch (Exception e) {
                break;
            }
        }
        return a;
    }

    public static void zipFiles(File srcFile, File zipFile) {
        ZipOutputStream outZip = null;
        try {
            outZip = new ZipOutputStream(new FileOutputStream(zipFile));
            zipFiles(srcFile.getParent() + File.separator, srcFile.getName(), outZip);
            outZip.finish();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outZip != null) {
                try {
                    outZip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void zipFiles(String folderString, String fileString, ZipOutputStream zipOutputSteam) throws Exception {
        if (zipOutputSteam == null)
            return;
        File file = new File(folderString + fileString);
        if (file.isFile()) {
            ZipEntry zipEntry = new ZipEntry(fileString);
            FileInputStream inputStream = new FileInputStream(file);
            zipOutputSteam.putNextEntry(zipEntry);
            int len;
            byte[] buffer = new byte[4096];
            while ((len = inputStream.read(buffer)) != -1) {
                zipOutputSteam.write(buffer, 0, len);
            }
            zipOutputSteam.closeEntry();
        } else {
            //文件夹
            String[] fileList = file.list();
            //没有子文件和压缩
            if (fileList.length <= 0) {
                ZipEntry zipEntry = new ZipEntry(fileString + File.separator);
                zipOutputSteam.putNextEntry(zipEntry);
                zipOutputSteam.closeEntry();
            }
            //子文件和递归
            for (String s : fileList) {
                zipFiles(folderString + fileString + "/", s, zipOutputSteam);
            }
        }
    }

    public static int getCrc(byte[] bytes) {
        int CRC = 0x0000ffff;
        int POLYNOMIAL = 0x0000a001;
        int i, j;
        for (i = 0; i < bytes.length - 2; i++) {
            CRC ^= ((int) bytes[i] & 0x000000ff);
            for (j = 0; j < 8; j++) {
                if ((CRC & 0x00000001) == 1) {
                    CRC >>= 1;
                    CRC ^= POLYNOMIAL;
                } else {
                    CRC >>= 1;
                }
            }
        }
        return CRC;
    }

    /**
     * 生成16进制随机数字符串
     *
     * @param size 字符串长度
     * @return 。
     */
    public static String generateRandomHexString(int size) {
        char[] chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            stringBuilder.append(chars[random.nextInt(16)]);
        }
        return stringBuilder.toString();
    }

    /**
     * 是否是屏保状态
     * @param context 上下文
     * @return true 息屏；false 亮屏
     */
    public static boolean isScreenOff(Context context){
        return !((PowerManager)context.getSystemService(Context.POWER_SERVICE)).isInteractive();
    }
}
