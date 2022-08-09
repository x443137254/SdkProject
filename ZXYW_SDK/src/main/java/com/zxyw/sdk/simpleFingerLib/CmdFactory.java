package com.zxyw.sdk.simpleFingerLib;

final class CmdFactory {
    static byte[] cancel(){
        byte[] b = new byte[24];
        b[0] = PacketType.COMMAND[0];
        b[1] = PacketType.COMMAND[1];
        b[2] = Command.CANCEL[0];
        b[3] = Command.CANCEL[1];
        return FingerTools.sum(b);
    }

    static byte[] enroll(){
        byte[] b = new byte[24];
        b[0] = PacketType.COMMAND[0];
        b[1] = PacketType.COMMAND[1];
        b[2] = Command.ENROLL[0];
        b[3] = Command.ENROLL[1];
        b[4] = 0x02;
        b[6] = 0x01;
        return FingerTools.sum(b);
    }

    static byte[] clear(){
        byte[] b = new byte[24];
        b[0] = PacketType.COMMAND[0];
        b[1] = PacketType.COMMAND[1];
        b[2] = Command.CLEAR[0];
        b[3] = Command.CLEAR[1];
        return FingerTools.sum(b);
    }

    static byte[] getData(){
        byte[] b = new byte[24];
        b[0] = PacketType.COMMAND[0];
        b[1] = PacketType.COMMAND[1];
        b[2] = Command.GET_DATA[0];
        b[3] = Command.GET_DATA[1];
        return FingerTools.sum(b);
    }

    static byte[] read(){
        byte[] b = new byte[24];
        b[0] = PacketType.COMMAND[0];
        b[1] = PacketType.COMMAND[1];
        b[2] = Command.READ[0];
        b[3] = Command.READ[1];
        b[4] = 0x02;
        b[6] = 0x01;
        return FingerTools.sum(b);
    }
}
