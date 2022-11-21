package com.zxyw.libCan;

import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CanUtils {

    private static final String TAG = "CanUtils";
    private static final Map<String, CanSocket> socketList = new HashMap<>();
    private static final Map<String, CanSocket.CanInterface> canInfList = new HashMap<>();

    public static void initCan(String name ) {
        for (String cmd : getCmdList(name)) {
            try {
                Log.e(TAG, "cmd: " + cmd);
                Log.e(TAG, ShellExecute.execute(cmd));
                CanSocket socket = new CanSocket(CanSocket.Mode.RAW);
                CanSocket.CanInterface canInterface = new CanSocket.CanInterface(socket, name);
                socket.bind(canInterface);
                socketList.put(name, socket);
                canInfList.put(name, canInterface);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String[] getCmdList(String name) {
        return new String[]{
                "su 0 ip link set " + name + " down",
                "su 0 ip link set " + name + " type can bitrate 100000 dbitrate 100000 fd on",
                "su 0 ip link set " + name + " up"
        };
    }

    public static CanSocket.CanFrame revData(String name) {
        try {
            final CanSocket canSocket = socketList.get(name);
            if (canSocket != null) {
                return canSocket.recv();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void sendData(String name, int target, byte[] data) {
        final CanSocket canSocket = socketList.get(name);
        final CanSocket.CanInterface canInterface = canInfList.get(name);
        if (canSocket == null || canInterface == null) {
            return;
        }
        try {
            CanSocket.CanId id = new CanSocket.CanId(target);
            int i = 0;
            byte[] currentData;
            for (; i * 8 < data.length - 8; i++) {
                currentData = Arrays.copyOfRange(data, i * 8, (i + 1) * 8);
                canSocket.send(new CanSocket.CanFrame(canInterface, id, currentData));
            }
            currentData = Arrays.copyOfRange(data, i * 8, data.length);
            canSocket.send(new CanSocket.CanFrame(canInterface, id, currentData));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
