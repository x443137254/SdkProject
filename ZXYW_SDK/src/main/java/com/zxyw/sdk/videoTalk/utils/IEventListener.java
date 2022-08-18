package com.zxyw.sdk.videoTalk.utils;

public interface IEventListener {
    void dispatchEvent(String aEventID, boolean success, Object eventObj);
}
