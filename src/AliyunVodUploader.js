import { NativeModules, NativeEventEmitter } from "react-native";
const UploadModule = NativeModules.AliyunVodUploadModule;
let AliyunVodUploader = (() => {
    class AliyunVodUploader {
        constructor() {
            this.initialized = false;
            this.mEventEmitter = new NativeEventEmitter(UploadModule);
            this.mVidId = "";
        }
        setAuthRefreshHandler(handler) {
            this.mEventEmitter.addListener(AliyunVodUploader.EVENT_TOKEN_EXPIRED, () => {
                handler({
                    vidId: this.mVidId,
                });
            });
            this.initialized = true;
        }
        setAuth(auth) {
            UploadModule.setAuth(auth);
        }
        startAuthUpload(vidId, address, auth) {
            if (!this.initialized) {
                throw new Error("Module is not initialized, please call setAuthRefreshHandler first");
            }
            this.mVidId = vidId;
            UploadModule.startAuthUpload(address, auth);
        }
        onStart(handler) {
            this.mEventEmitter.addListener(AliyunVodUploader.EVENT_STARTED, handler);
        }
        onProgress(handler) {
            this.mEventEmitter.addListener(AliyunVodUploader.EVENT_PROGRESS, handler);
        }
        onError(handler) {
            this.mEventEmitter.addListener(AliyunVodUploader.EVENT_FAILED, handler);
        }
        onComplete(handler) {
            this.mEventEmitter.addListener(AliyunVodUploader.EVENT_COMPLETED, handler);
        }
    }
    AliyunVodUploader.EVENT_COMPLETED = "AliyunVod.Uploader.Completed";
    AliyunVodUploader.EVENT_FAILED = "AliyunVod.Uploader.Failed";
    AliyunVodUploader.EVENT_PROGRESS = "AliyunVod.Uploader.Progress";
    AliyunVodUploader.EVENT_TOKEN_EXPIRED = "AliyunVod.Uploader.TokenExpired";
    AliyunVodUploader.EVENT_STARTED = "AliyunVod.Uploader.Started";
    return AliyunVodUploader;
})();
export { AliyunVodUploader };
//# sourceMappingURL=AliyunVodUploader.js.map