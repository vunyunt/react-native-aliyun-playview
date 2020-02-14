import { NativeModules, NativeEventEmitter } from "react-native";

const DownloaderModule = NativeModules.AliyunVodDownloadModule;

// SD is actually 720p, HD is 1080p,
export type TAliyunVodQuality = "OD" | "SD" | "HD" | string;
export type TAliyunVodFormat = "flv" | "mp4";
export interface IAliyunDownloadMediaInfo {
  status: string;
  savePath: string;
  title: string;
  coverUrl: string;
  format: string;
  quality: string;
  vid: string;
}
export interface IAliyunMediaInfoList {
  ["@items"]: IAliyunDownloadMediaInfo[];
}
export interface IAliyunDownloadProgress {
  progress: number;
  media: IAliyunMediaInfoList;
}

export class AliyunVodDownloader {
  public static EVENT_UPDATE_AUTH = "AliyunVod.Downloader.UpdateAuth";
  public static EVENT_PREPARED = "AliyunVod.Downloader.Prepared";
  public static EVENT_START = "AliyunVod.Downloader.Start";
  public static EVENT_PROGRESS = "AliyunVod.Downloader.Progress";
  public static EVENT_COMPLETE = "AliyunVod.Downloader.Completed";
  public static EVENT_STOP = "AliyunVod.Downloader.Stop";
  public static EVENT_ERROR = "AliyunVod.Downloader.Error";

  private initialized = false;
  private mEventEmitter = new NativeEventEmitter(DownloaderModule);

  constructor() {}

  public setAuthRefreshHandler(
    handler: (args: {
      vidId: string;
      quality: TAliyunVodQuality;
      format: TAliyunVodFormat;
      title: string;
      encrypt: boolean;
    }) => void
  ) {
    this.mEventEmitter.addListener(
      AliyunVodDownloader.EVENT_UPDATE_AUTH,
      handler
    );
    this.initialized = true;
  }

  public setAuth(vidId: string, authStr: string) {
    DownloaderModule.setAuth(vidId, authStr);
  }

  public onStart(handler: (args: IAliyunMediaInfoList) => void) {
    this.mEventEmitter.addListener(AliyunVodDownloader.EVENT_START, handler);
  }

  public onProgress(handler: (args: IAliyunDownloadProgress) => void) {
    this.mEventEmitter.addListener(AliyunVodDownloader.EVENT_PROGRESS, handler);
  }

  public onComplete(handler: (args: IAliyunMediaInfoList) => void) {
    this.mEventEmitter.addListener(AliyunVodDownloader.EVENT_COMPLETE, handler);
  }

  public onError(
    handler: (args: {
      media: IAliyunMediaInfoList;
      i: number;
      s1: string;
      s2: string;
    }) => void
  ) {
    this.mEventEmitter.addListener(AliyunVodDownloader.EVENT_ERROR, handler);
  }

  public startAuthDownload(
    vidId: string,
    authStr: string,
    format: TAliyunVodFormat,
    quality: TAliyunVodQuality,
    encrypted: boolean
  ) {
    if (!this.initialized) {
      throw new Error(
        "Module is not initialized, please call setAuthRefreshHandler first"
      );
    }

    DownloaderModule.startAuthDownload(
      vidId,
      authStr,
      format,
      quality,
      encrypted
    );
  }
}
