package com.example.aliyunplayview;

import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.aliyun.player.AliPlayer;
import com.aliyun.player.AliPlayerFactory;
import com.aliyun.player.IPlayer;
import com.aliyun.player.bean.ErrorInfo;
import com.aliyun.player.bean.InfoBean;
import com.aliyun.player.source.UrlSource;
import com.aliyun.player.source.VidAuth;
import com.aliyun.player.source.VidSts;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class AliyunPlayManager extends SimpleViewManager<AliyunPlayerView> {
    private static final String TAG = "AliyunPlayManager";
    public static final String REACT_CLASS = "AliyunPlay";
    private static final String PLAYING_CALLBACK = "onPlayingCallback";
    private static final String EVENT_CALLBACK = "onEventCallback";

    enum EAliVidEventType {
        PrepareDone(0),
        Play(1),
        FirstFrame(2),
        Pause(3),
        Stop(4),
        Finish(5),
        BeginLoading(6),
        EndLoading(7),
        SeekDone(8),
        Error(9);

        private int val;
        EAliVidEventType(int i) {
            val = i;
        }

        public int getVal() {return val;}
    }


    //视频画面
    private SurfaceView mSurfaceView;
    // 组件view
    private AliyunPlayerView mAliyunPlayerView;
    //播放器
    private AliPlayer mAliPlayer;
    // 事件发送者
    private RCTEventEmitter mEventEmitter;
    private static final int VIDEO_PAUSE = 1;
    private static final int VIDEO_RESUME = 2;
    private static final int VIDEO_STOP = 3;
    private static final int VIDEO_SEEKTOTIME = 4;
    private static final int VIDEO_REPLAY = 5;

    @Override
    public String getName() {
        return REACT_CLASS;
    }


    @Override
    protected AliyunPlayerView createViewInstance(ThemedReactContext context) {
        Log.e("TAG", "组件创建了");
        //reactContext = context;
        this.mAliPlayer = AliPlayerFactory.createAliPlayer(context);

        mEventEmitter = context.getJSModule(RCTEventEmitter.class);
        AliyunPlayerView view = new AliyunPlayerView(context);
        mAliyunPlayerView = view;

        mSurfaceView = new SurfaceView(context);
        view.addView(mSurfaceView);
        SurfaceHolder holder = mSurfaceView.getHolder();

//        mAliyunVodPlayer = new AliyunVodPlayer(context);
//        mAliyunVodPlayer.setDisplay(holder);
//        mAliyunVodPlayer.setVideoScalingMode(IAliyunVodPlayer.VideoScalingMode.VIDEO_SCALING_MODE_SCALE_TO_FIT);

        //增加surfaceView的监听
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                mAliPlayer.setDisplay(surfaceHolder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width,
                                       int height) {
                mAliPlayer.redraw();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                mAliPlayer.setDisplay(null);
            }
        });

        return view;
    }

    /**
     * 准备视频(异步)
     */
    @ReactProp(name = "prepareAsyncParams")
    public void setPrepareAsyncParams(AliyunPlayerView view, ReadableMap options) {

        String type = options.getString("type");

        switch (type) {
            // 使用vid+STS方式播放（点播用户推荐使用）
            case "vidSts":
                String vid = options.getString("vid");
                String accessKeyId = options.getString("accessKeyId");
                String accessKeySecret = options.getString("accessKeySecret");
                String securityToken = options.getString("securityToken");

                VidSts vidSts = new VidSts();
                vidSts.setVid(vid);
                vidSts.setAccessKeyId(accessKeyId);
                vidSts.setAccessKeySecret(accessKeySecret);
                vidSts.setSecurityToken(securityToken);

                if (mAliPlayer != null) {
                    mAliPlayer.setDataSource(vidSts);
                    mAliPlayer.prepare();
                }
                break;
            case "url":
                String url = options.getString("url");

                UrlSource urlSource = new UrlSource();
                urlSource.setUri(url);

                if(mAliPlayer != null) {
                    mAliPlayer.setDataSource(urlSource);
                    mAliPlayer.prepare();
                }
                break;
            case "vidAuth":
                String playAuthVid = options.getString("vid");
                String playAuthStr = options.getString("playAuth");
                String qualityStr = options.getString(("quality"));
//                Boolean forceQuality = options.getBoolean("forceQuality");
//                if(forceQuality == null) { forceQuality = false; }

                VidAuth vidAuth = new VidAuth();
                vidAuth.setPlayAuth(playAuthStr);
                vidAuth.setVid(playAuthVid);
                if(qualityStr != null) {
                    vidAuth.setQuality(qualityStr, false);
                }

                if(mAliPlayer != null) {
                    mAliPlayer.setDataSource(vidAuth);
                    mAliPlayer.prepare();
                }
                break;
            default:
                Log.e(TAG, "prepareAsync" + type);
                break;
        }
    }


    @Override
    protected void addEventEmitters(ThemedReactContext reactContext, AliyunPlayerView view) {
        this.onListener(reactContext, view);
    }

    /**
     * 播放器监听事件
     *
     * @param reactContext
     * @param view
     */
    private void onListener(final ThemedReactContext reactContext, AliyunPlayerView view) {
        this.mAliPlayer.setOnPreparedListener(new IPlayer.OnPreparedListener() {
            @Override
            public void onPrepared() {
                mAliPlayer.start();
                //准备完成触发

                // TODO：待优化的 listener 处理，应该新建个独立文件处理？
                WritableMap body = Arguments.createMap();
                body.putInt("event", EAliVidEventType.PrepareDone.getVal());
                mEventEmitter.receiveEvent(mAliPlayer.hashCode(), EVENT_CALLBACK, body);
            }
        });

        // 第一帧显示
        this.mAliPlayer.setOnRenderingStartListener(new IPlayer.OnRenderingStartListener() {
            @Override
            public void onRenderingStart() {
                // 开始启动更新进度的定时器
                // TODO：待优化的 listener 处理，应该新建个独立文件处理？
                WritableMap body = Arguments.createMap();
                body.putInt("event", EAliVidEventType.FirstFrame.getVal());
                mEventEmitter.receiveEvent(mAliPlayer.hashCode(), EVENT_CALLBACK, body);
            }
        });

        mAliPlayer.setOnErrorListener(new IPlayer.OnErrorListener() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                android.util.Log.d("ReactNative", "onError: " + errorInfo.getMsg());
                Log.e(TAG, "onError" + errorInfo.getMsg());

                WritableMap body = Arguments.createMap();
                body.putString("msg", errorInfo.getMsg());
                body.putString("extra", errorInfo.getExtra());
                body.putInt("code", errorInfo.getCode().getValue());
                body.putInt("event", EAliVidEventType.Error.getVal());
                mEventEmitter.receiveEvent(mAliPlayer.hashCode(), EVENT_CALLBACK, body);
            }
        });

        mAliPlayer.setOnCompletionListener(new IPlayer.OnCompletionListener() {
            @Override
            public void onCompletion() {
                WritableMap body = Arguments.createMap();
                body.putInt("event", EAliVidEventType.Finish.getVal());
                mEventEmitter.receiveEvent(mAliyunPlayerView.getId(), EVENT_CALLBACK, body);
            }
        });

        mAliPlayer.setOnInfoListener((new IPlayer.OnInfoListener() {
            @Override
            public void onInfo(InfoBean infoBean) {
                switch (infoBean.getCode()){
                    case CurrentPosition:
                        long currentTime = infoBean.getExtraValue();
                        long duration = mAliPlayer.getDuration();
                        WritableMap body = Arguments.createMap();
                        body.putString("currentTime", currentTime + "");
                        body.putString("duration", duration + "");
                        mEventEmitter.receiveEvent(mAliyunPlayerView.getId(), PLAYING_CALLBACK, body);
                        break;
                }
            }
        }));
    }

    @Nullable
    @Override
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        builder.put(EVENT_CALLBACK, MapBuilder.of("registrationName", EVENT_CALLBACK));
        builder.put(PLAYING_CALLBACK, MapBuilder.of("registrationName", PLAYING_CALLBACK));
        return builder.build();
    }

    @Override
    public void receiveCommand(AliyunPlayerView root, int commandId, @Nullable ReadableArray args) {
        switch (commandId) {
            case VIDEO_PAUSE:
                if (mAliPlayer != null) {
                    mAliPlayer.pause();
                }
                break;
            case VIDEO_RESUME:
                if (mAliPlayer != null) {
                    mAliPlayer.start();
                }
                break;
            case VIDEO_STOP:
                if (mAliPlayer != null) {
                    mAliPlayer.stop();
                }
                break;
            case VIDEO_SEEKTOTIME:
                if (mAliPlayer != null) {
                    mAliPlayer.seekTo(args.getInt(0));
                }
                break;
            case VIDEO_REPLAY:
                Log.e("TAG", "重新播放");
                if (mAliPlayer != null) {
                    mAliPlayer.reset();
                }
                break;
        }
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        Map<String, Integer> map = this.CreateMap(
                "pause", VIDEO_PAUSE,
                "resume", VIDEO_RESUME,
                "stop", VIDEO_STOP,
                "seekToTime", VIDEO_SEEKTOTIME,
                "rePlay", VIDEO_REPLAY
        );

        return map;
    }

    private <K, V> Map<K, V> CreateMap(
            K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        Map map = new HashMap<K, V>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }

    @Override
    public void onDropViewInstance(AliyunPlayerView view) {
        Log.e("TAG", "组件销毁了");
        mAliPlayer.stop();
        mAliPlayer.release();
        mAliPlayer = null;
        view.destroyDrawingCache();
        super.onDropViewInstance(view);
    }
}
