import { NativeModules, NativeEventEmitter } from "react-native";

const UploadModule = NativeModules.AliyunVodUploadModule;

export interface IAliyunVodUploadInfo {
  bucket: string;
  endpoint: string;
  filePath: string;
  object: string;
  coverUrl: string;
  desc: string;
  fileName: string;
  fileSize: string;
  title: string;
  userData: string;
  fileType: number;
  status: string;
}

export class AliyunVodUploader {
  public static EVENT_COMPLETED = "AliyunVod.Uploader.Completed";
  public static EVENT_FAILED = "AliyunVod.Uploader.Failed";
  public static EVENT_PROGRESS = "AliyunVod.Uploader.Progress";
  public static EVENT_TOKEN_EXPIRED = "AliyunVod.Uploader.TokenExpired";
  public static EVENT_STARTED = "AliyunVod.Uploader.Started";

  private initialized = false;
  private mEventEmitter = new NativeEventEmitter(UploadModule);
  private mVidId: string = "";

  constructor() {}

  public setAuthRefreshHandler(handler: (args: { vidId: string }) => void) {
    this.mEventEmitter.addListener(
      AliyunVodUploader.EVENT_TOKEN_EXPIRED,
      () => {
        handler({
          vidId: this.mVidId,
        });
      }
    );
    this.initialized = true;
  }

  public setAuth(auth: string) {
    UploadModule.setAuth(auth);
  }

  public startAuthUpload(vidId: string, address: string, auth: string) {
    if (!this.initialized) {
      throw new Error(
        "Module is not initialized, please call setAuthRefreshHandler first"
      );
    }

    this.mVidId = vidId;
    UploadModule.startAuthUpload(address, auth);
  }

  public onStart(handler: (args: IAliyunVodUploadInfo) => void) {
    this.mEventEmitter.addListener(AliyunVodUploader.EVENT_STARTED, handler);
  }

  public onProgress(
    handler: (args: IAliyunVodUploadInfo & { progress: number }) => void
  ) {
    this.mEventEmitter.addListener(AliyunVodUploader.EVENT_PROGRESS, handler);
  }

  public onError(
    handler: (
      args: IAliyunVodUploadInfo & { code: string; message: string }
    ) => void
  ) {
    this.mEventEmitter.addListener(AliyunVodUploader.EVENT_FAILED, handler);
  }

  public onComplete(handler: (args: IAliyunVodUploadInfo) => void) {
    this.mEventEmitter.addListener(AliyunVodUploader.EVENT_COMPLETED, handler);
  }
}
