package com.zxyw.sdk.simpleFingerLib;

public interface FingerEnrollListener {
    enum Step {
        ERROR, STEP1, STEP2, STEP3, RELEASE, DONE, RETRY, ERROR_RETRY
    }

    void enrollStep(Step step, byte[] template);
}
