package com.zxyw.sdk.simplevein;

public interface EnrollCallback {

    /**
     * 录入结束
     * @param success 是否成功录入
     * @param template 指静脉模板，成功录入时为模板的base64编码字符串，失败时为提示文字
     */
    void enrollFinish(boolean success, String template);

    /**
     * 录入过程中步骤提示
     * @param count 第几次放入手指
     * @param removeFinger true-请移开手指；false-请放入手指
     */
    void enrollStep(int count, boolean removeFinger);
}
