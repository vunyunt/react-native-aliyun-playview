package com.example.aliyunplayview;

import android.util.Log;

import com.alibaba.sdk.android.vod.upload.VODUploadCallback;
import com.alibaba.sdk.android.vod.upload.VODUploadClient;
import com.alibaba.sdk.android.vod.upload.VODUploadClientImpl;
import com.alibaba.sdk.android.vod.upload.model.UploadFileInfo;
import com.alibaba.sdk.android.vod.upload.model.VodInfo;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AliyunVodUploadModule extends ReactContextBaseJavaModule {
    public static final String EVENT_COMPLETED = "AliyunVod.Uploader.Completed";
    public static final String EVENT_FAILED = "AliyunVod.Uploader.Failed";
    public static final String EVENT_PROGRESS = "AliyunVod.Uploader.Progress";
    public static final String EVENT_TOKEN_EXPIRED  = "AliyunVod.Uploader.TokenExpired";
    public static final String EVENT_STARTED = "AliyunVod.Uploader.Started";

    private VODUploadClient mUploadClient;
    private ReactApplicationContext mContext;
    private DeviceEventManagerModule.RCTDeviceEventEmitter mDeviceEmitter;
    private Lock mAuthLock = new ReentrantLock();
    private Condition mAuthCond = mAuthLock.newCondition();
    private boolean mAuthSet = false;
    private String mAddress = "";
    private String mAuth = "";

    AliyunVodUploadModule(ReactApplicationContext context) {
        super(context);

        mContext = context;
    }

    @Override
    public String getName() {
        return "AliyunVodUploadModule";
    }

    private WritableMap infoToMap(UploadFileInfo info) {
        WritableMap map = Arguments.createMap();
        this.infoToMap(info, map);
        return map;
    }

    private void infoToMap(UploadFileInfo info, WritableMap map) {
        VodInfo vodInfo = info.getVodInfo();
        map.putString("bucket", info.getBucket());
        map.putString("endpoint", info.getEndpoint());
        map.putString("filePath", info.getFilePath());
        map.putString("object", info.getObject());
        map.putString("coverUrl", vodInfo.getCoverUrl());
        map.putString("desc", vodInfo.getDesc());
        map.putString("fileName", vodInfo.getFileName());
        map.putString("fileSize", vodInfo.getFileSize());
        map.putString("title", vodInfo.getTitle());
        map.putString("userData", vodInfo.getUserData());
        map.putInt("fileType", info.getFileType());
        map.putString("status", info.getStatus().name());
    }

    @ReactMethod
    public void setAuth(String auth) {
        this.mAuthLock.lock();
        this.mAuth = auth;
        this.mAuthSet = true;
        this.mAuthCond.signalAll();
        this.mAuthLock.unlock();
    }

    @ReactMethod
    public void startAuthUpload(String address, String auth) {
        this.mAuth = auth;
        this.mAddress = address;
        this.mUploadClient.start();
    }

    @Override
    public void initialize() {
        super.initialize();

        mDeviceEmitter = mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

        mUploadClient = new VODUploadClientImpl(mContext);
        mUploadClient.init(new VODUploadCallback() {
            @Override
            public void onUploadSucceed(UploadFileInfo info) {
                mDeviceEmitter.emit(EVENT_COMPLETED, infoToMap(info));
            }

            @Override
            public void onUploadFailed(UploadFileInfo info, String code, String message) {
                WritableMap args = infoToMap(info);

                args.putString("code", code);
                args.putString("message", message);

                mDeviceEmitter.emit(EVENT_FAILED, args);
            }

            @Override
            public void onUploadProgress(UploadFileInfo info, long uploadedSize, long totalSize) {
                WritableMap args = infoToMap(info);

                args.putDouble("progress", uploadedSize / totalSize);

                mDeviceEmitter.emit(EVENT_PROGRESS, args);
            }

            @Override
            public void onUploadTokenExpired() {
                WritableMap args = Arguments.createMap();

                mDeviceEmitter.emit(EVENT_TOKEN_EXPIRED, args);

                mAuthLock.lock();
                try {
                    while(!mAuthSet) {
                        mAuthCond.await();
                    }
                } catch(InterruptedException e) {
                    Log.e("ReactNative", EVENT_TOKEN_EXPIRED + ": Interrupted");
                }
                mAuthLock.unlock();

                mUploadClient.resumeWithAuth(mAuth);
            }

            @Override
            public void onUploadRetry(String code, String message) {
            }

            @Override
            public void onUploadRetryResume() {
            }

            @Override
            public void onUploadStarted(UploadFileInfo uploadFileInfo) {
                mUploadClient.setUploadAuthAndAddress(uploadFileInfo, mAuth, mAddress);
                mDeviceEmitter.emit(EVENT_STARTED, infoToMap(uploadFileInfo));
            }
        });
    }
}
