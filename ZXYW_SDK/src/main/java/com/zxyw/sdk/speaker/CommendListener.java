package com.zxyw.sdk.speaker;

public interface CommendListener {
    void onRecognize(String word, int score, int errorCode);
}
