package com.zxyw.sdk;

import com.zxyw.sdk.auth.Auth;

import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
//        assertEquals(4, 2 + 2);

        final String sn = "1011220701100005";
        final String key = "5ABD1E086CEC19F8";
        final String code = Auth.getAuthCode(sn, 5, key);//A7CD93461355596F
        System.out.println(code);
    }
}