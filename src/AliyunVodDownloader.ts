import { NativeModules, DeviceEventEmitter } from "react-native";

const DownloaderModule = NativeModules.AliyunVodDownloadModule;

export type TAliyunVodQuality = "OD" | string;
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

export class AliyunVodDownloader {
  public static EVENT_UPDATE_AUTH = "AliyunVod.Downloader.UpdateAuth";
  public static EVENT_PREPARED = "AliyunVod.Downloader.Prepared";
  public static EVENT_START = "AliyunVod.Downloader.Start";
  public static EVENT_PROGRESS = "AliyunVod.Downloader.Progress";
  public static EVENT_COMPLETE = "AliyunVod.Downloader.Completed";
  public static EVENT_STOP = "AliyunVod.Downloader.Stop";
  public static EVENT_ERROR = "AliyunVod.Downloader.Error";

  private initialized = false;

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
    DeviceEventEmitter.addListener(
      AliyunVodDownloader.EVENT_UPDATE_AUTH,
      handler
    );
    this.initialized = true;
  }

  public setAuth(vidId: string, authStr: string) {
    DownloaderModule.setAuth(vidId, authStr);
  }

  public onStart(
    handler: (args: { ["@items"]: IAliyunDownloadMediaInfo[] }) => void
  ) {
    DeviceEventEmitter.addListener(AliyunVodDownloader.EVENT_START, handler);
  }

  public onProgress(
    handler: (args: {
      progress: number;
      media: {
        ["@items"]: IAliyunDownloadMediaInfo[];
      };
    }) => void
  ) {
    DeviceEventEmitter.addListener(AliyunVodDownloader.EVENT_PROGRESS, handler);
  }

  public onComplete(
    handler: (args: { ["@items"]: IAliyunDownloadMediaInfo[] }) => void
  ) {
    DeviceEventEmitter.addListener(AliyunVodDownloader.EVENT_COMPLETE, handler);
  }

  public onError(
    handler: (args: {
      media: string;
      i: number;
      s1: string;
      s2: string;
    }) => void
  ) {
    DeviceEventEmitter.addListener(AliyunVodDownloader.EVENT_ERROR, handler);
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
