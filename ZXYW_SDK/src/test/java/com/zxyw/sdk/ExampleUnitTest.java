package com.zxyw.sdk;

import com.zxyw.sdk.auth.AESUtil;

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

//        final String sn = "1011220701100005";
//        final String key = "5ABD1E086CEC19F8";
//        final String code = Auth.getAuthCode(sn, 5, key);//A7CD93461355596F
//        System.out.println(code);

        final String cert = "f4fc58b2275edb6c18e6a20aa548dc1987992dfd53c839ee31cbe84297705a9d";
        final String decode = AESUtil.decode(cert);
        System.out.println(decode);
    }

    @Test
    public void arr() {
        final int column = 15;//行
        final int row = 16;//列
        final int[][] array = new int[column][row];
        int direction = 0;
        int count = 1;
        int _column = 0;//行
        int _row = 0;//列
        while (count <= column * row) {
            switch (direction) {
                case 0:
                    array[_column][_row++] = count++;
                    if (_row >= row || array[_column][_row] != 0) {
                        direction++;
                        _column++;
                        _row--;
                    }
                    break;
                case 1:
                    array[_column++][_row] = count++;
                    if (_column >= column || array[_column][_row] != 0) {
                        direction++;
                        _column--;
                        _row--;
                    }
                    break;
                case 2:
                    array[_column][_row--] = count++;
                    if (_row < 0 || array[_column][_row] != 0) {
                        direction++;
                        _column--;
                        _row++;
                    }
                    break;
                case 3:
                    array[_column--][_row] = count++;
                    if (_column < 0 || array[_column][_row] != 0) {
                        direction = 0;
                        _column++;
                        _row++;
                    }
                    break;
            }
        }
        for (int[] a : array) {
            for (int b : a) {
                System.out.printf("%4d", b);
            }
            System.out.println();
        }
    }
}