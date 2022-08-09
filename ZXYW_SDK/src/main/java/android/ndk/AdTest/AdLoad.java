//package com.sz.android.sz_fpmatch_and;
package android.ndk.AdTest;

public class AdLoad {
    static {
        System.loadLibrary("FpCore");
    }

    /**
     * 指纹模板比对
     * @param p_pTemplate1 待比对模板1
     * @param p_pTemplate2 待比对模板2
     * @param p_nSecLevel [1-5]
     * @param p_nMatchRes 比对结果
     * @param p_pTempBuffer ？缓存
     * @return 方法执行结果
     */
    public native int FPMatch(byte[] p_pTemplate1, byte[] p_pTemplate2, int p_nSecLevel, int[] p_nMatchRes, byte[] p_pTempBuffer);
}