package com.zxyw.sdk.simplevein;

public interface ReadCallback {

    /**
     * 读取到指静脉时的回调
     *
     * @param template 指静脉特征模板
     */
    void readTemplate(String template);
}
