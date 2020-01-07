package com.example.aliyunplayview;

import android.util.Log;

import com.aliyun.vodplayer.downloader.AliyunDownloadConfig;
import com.aliyun.vodplayer.downloader.AliyunDownloadInfoListener;
import com.aliyun.vodplayer.downloader.AliyunDownloadManager;
import com.aliyun.vodplayer.downloader.AliyunDownloadMediaInfo;
import com.aliyun.vodplayer.downloader.AliyunRefreshPlayAuthCallback;
import com.aliyun.vodplayer.media.AliyunPlayAuth;
import com.cedarsoftware.util.io.JsonReader;
import com.example.aliyunplayview.util.MapUtil;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AliyunVodDownloadModule extends ReactContextBaseJavaModule {
    private final String mStoragePath;

    private ReactApplicationContext mContext;
    private AliyunDownloadManager mDownloadManager;
    private Lock mAuthInfoLock = new ReentrantLock();
    private Condition mAuthInfoCond = mAuthInfoLock.newCondition();
    private boolean mAuthInfoSet = false;
    private String mAuthInfoVidId = "";
    private String mAuthInfoAuthStr = "";
    private DeviceEventManagerModule.RCTDeviceEventEmitter mDeviceEmitter;

    private static final String EVENT_UPDATE_AUTH = "AliyunVod.Downloader.UpdateAuth";
    private static final String EVENT_PREPARED = "AliyunVod.Downloader.Prepared";
    private static final String EVENT_START = "AliyunVod.Downloader.Start";
    private static final String EVENT_PROGRESS = "AliyunVod.Downloader.Progress";
    private static final String EVENT_COMPLETE = "AliyunVod.Downloader.Completed";
    private static final String EVENT_STOP = "AliyunVod.Downloader.Stop";
    private static final String EVENT_ERROR = "AliyunVod.Downloader.Error";

    AliyunVodDownloadModule(ReactApplicationContext context) {
        super(context);
        mContext = context;
        mDownloadManager = AliyunDownloadManager.getInstance(context);
        mStoragePath = context.getFilesDir().toString() + "/vodDownload/";
    }

    public String getName() {
        return "AliyunVodDownloadModule";
    }

    @Override
    public void initialize() {
        super.initialize();

        AliyunDownloadConfig config = new AliyunDownloadConfig();
        config.setDownloadDir(mStoragePath);
        config.setSecretImagePath("");
        this.mDownloadManager.setDownloadConfig(config);

        mDeviceEmitter = mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        this.setDownloadManagerRefreshAuth();
        this.bindDownloadEvents();
    }

    private void setDownloadManagerRefreshAuth() {
        this.mDownloadManager.setRefreshAuthCallBack(new AliyunRefreshPlayAuthCallback() {
            @Override
            public AliyunPlayAuth refreshPlayAuth(String vid, String quality, String format, String title, boolean encrypt) {
                WritableMap args = Arguments.createMap();
                args.putString("vidId", vid);
                args.putString("quality", quality);
                args.putString("format", format);
                args.putString("title", title);
                args.putBoolean("encrypt", encrypt);

                mDeviceEmitter.emit(EVENT_UPDATE_AUTH, args);

                mAuthInfoLock.lock();
                try {
                    while(!mAuthInfoSet) {
                        mAuthInfoCond.await();
                    }
                } catch (InterruptedException e) {
                    Log.e("ReactNative", "refreshPlayAuth: Interrupted" );
                    mAuthInfoSet = false;
                }
                mAuthInfoLock.unlock();

                AliyunPlayAuth.AliyunPlayAuthBuilder playAuthBuilder = new AliyunPlayAuth.AliyunPlayAuthBuilder();
                playAuthBuilder.setVid(mAuthInfoVidId);
                playAuthBuilder.setQuality(quality);
                playAuthBuilder.setPlayAuth(mAuthInfoAuthStr);
                playAuthBuilder.setFormat(format);
                playAuthBuilder.setTitle(title);
                playAuthBuilder.setIsEncripted(encrypt? 1 : 0);

                return playAuthBuilder.build();
            }
        });
    }

    private void bindDownloadEvents() {
        mDownloadManager.setDownloadInfoListener(new AliyunDownloadInfoListener() {
            @Override
            public void onPrepared(List<AliyunDownloadMediaInfo> list) {
                mDeviceEmitter.emit(EVENT_PREPARED, mediaInfosToWritableMap(list));
            }

            @Override
            public void onStart(AliyunDownloadMediaInfo aliyunDownloadMediaInfo) {
                Log.d("ReactNative", "onStart: ");
                mDeviceEmitter.emit(EVENT_START, mediaInfosToWritableMap(aliyunDownloadMediaInfo));
            }

            @Override
            public void onProgress(AliyunDownloadMediaInfo aliyunDownloadMediaInfo, int progress) {
                WritableMap args = Arguments.createMap();
                args.putMap("media", mediaInfosToWritableMap(aliyunDownloadMediaInfo));
                args.putInt("progress", progress);
                mDeviceEmitter.emit(EVENT_PROGRESS, args);
            }

            @Override
            public void onStop(AliyunDownloadMediaInfo aliyunDownloadMediaInfo) {
                mDeviceEmitter.emit(EVENT_STOP, mediaInfosToWritableMap(aliyunDownloadMediaInfo));
            }

            @Override
            public void onCompletion(AliyunDownloadMediaInfo aliyunDownloadMediaInfo) {
                mDeviceEmitter.emit(EVENT_COMPLETE, mediaInfosToWritableMap(aliyunDownloadMediaInfo));
            }

            @Override
            public void onError(AliyunDownloadMediaInfo aliyunDownloadMediaInfo, int i, String s, String s1) {
                WritableMap args = Arguments.createMap();
                args.putMap("media", mediaInfosToWritableMap(aliyunDownloadMediaInfo));
                args.putInt("i", i);
                args.putString("s1", s);
                args.putString("s2", s1);

                mDeviceEmitter.emit(EVENT_ERROR, args);
            }

            @Override
            public void onWait(AliyunDownloadMediaInfo aliyunDownloadMediaInfo) {

            }

            @Override
            public void onM3u8IndexUpdate(AliyunDownloadMediaInfo aliyunDownloadMediaInfo, int i) {

            }
        });
    }

    private WritableMap mediaInfosToWritableMap(List<AliyunDownloadMediaInfo> infos) {
        String json = AliyunDownloadMediaInfo.getJsonFromInfos(infos);
        Map rawMap = JsonReader.jsonToMaps(json);
        return MapUtil.toWritableMap(rawMap);
    }

    private WritableMap mediaInfosToWritableMap(AliyunDownloadMediaInfo infos[]) {
        return this.mediaInfosToWritableMap(Arrays.asList(infos));
    }

    private WritableMap mediaInfosToWritableMap(AliyunDownloadMediaInfo info) {
        return this.mediaInfosToWritableMap(new AliyunDownloadMediaInfo[]{info});
    }

    @ReactMethod
    public void setAuth(String vidId, String authStr) {
        this.mAuthInfoLock.lock();
        this.mAuthInfoVidId = vidId;
        this.mAuthInfoAuthStr = authStr;
        this.mAuthInfoSet = true;
        this.mAuthInfoCond.signalAll();
        this.mAuthInfoLock.unlock();
    }

    @ReactMethod
    public void startAuthDownload(String vidId, String authStr, String format, String quality, boolean encrypted) {
        AliyunPlayAuth.AliyunPlayAuthBuilder playAuthBuilder = new AliyunPlayAuth.AliyunPlayAuthBuilder();
        playAuthBuilder.setVid(vidId);
        playAuthBuilder.setPlayAuth(authStr);
        playAuthBuilder.setFormat(format);
        playAuthBuilder.setQuality(quality);
        playAuthBuilder.setIsEncripted(encrypted? 1:0);
        AliyunPlayAuth playAuth = playAuthBuilder.build();

        AliyunDownloadMediaInfo info = new AliyunDownloadMediaInfo();
        info.setVid(vidId);
        info.setFormat(format);
        info.setQuality(quality);
        info.setEncripted(encrypted? 1:0);
        info.setSavePath(mStoragePath);

        mDownloadManager.prepareDownloadMedia(playAuth);
        mDownloadManager.addDownloadMedia(info);
        mDownloadManager.startDownloadMedia(info);
    }
}