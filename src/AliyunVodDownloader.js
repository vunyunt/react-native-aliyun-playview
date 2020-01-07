import { NativeModules, DeviceEventEmitter } from "react-native";
const DownloaderModule = NativeModules.AliyunVodDownloadModule;
export class AliyunVodDownloader {
    constructor() {
        this.initialized = false;
    }
    setAuthRefreshHandler(handler) {
        DeviceEventEmitter.addListener(AliyunVodDownloader.EVENT_UPDATE_AUTH, handler);
        this.initialized = true;
    }
    setAuth(vidId, authStr) {
        DownloaderModule.setAuth(vidId, authStr);
    }
    onStart(handler) {
        DeviceEventEmitter.addListener(AliyunVodDownloader.EVENT_START, handler);
    }
    onProgress(handler) {
        DeviceEventEmitter.addListener(AliyunVodDownloader.EVENT_PROGRESS, handler);
    }
    onComplete(handler) {
        DeviceEventEmitter.addListener(AliyunVodDownloader.EVENT_COMPLETE, handler);
    }
    onError(handler) {
        DeviceEventEmitter.addListener(AliyunVodDownloader.EVENT_ERROR, handler);
    }
    startAuthDownload(vidId, authStr, format, quality, encrypted) {
        if (!this.initialized) {
            throw new Error("Module is not initialized, please call setAuthRefreshHandler first");
        }
        DownloaderModule.startAuthDownload(vidId, authStr, format, quality, encrypted);
    }
}
AliyunVodDownloader.EVENT_UPDATE_AUTH = "AliyunVod.Downloader.UpdateAuth";
AliyunVodDownloader.EVENT_PREPARED = "AliyunVod.Downloader.Prepared";
AliyunVodDownloader.EVENT_START = "AliyunVod.Downloader.Start";
AliyunVodDownloader.EVENT_PROGRESS = "AliyunVod.Downloader.Progress";
AliyunVodDownloader.EVENT_COMPLETE = "AliyunVod.Downloader.Completed";
AliyunVodDownloader.EVENT_STOP = "AliyunVod.Downloader.Stop";
AliyunVodDownloader.EVENT_ERROR = "AliyunVod.Downloader.Error";
//# sourceMappingURL=AliyunVodDownloader.js.map