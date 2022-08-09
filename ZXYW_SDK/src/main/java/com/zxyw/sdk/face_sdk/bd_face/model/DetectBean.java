package com.zxyw.sdk.face_sdk.bd_face.model;

import com.baidu.idl.main.facesdk.FaceInfo;
import com.baidu.idl.main.facesdk.model.BDFaceImageInstance;

public class DetectBean {
    public BDFaceImageInstance rgbImage;
    public byte[] irData;
    public FaceInfo[] fastFaceInfos;
}
