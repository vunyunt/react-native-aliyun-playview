//
//  AliyunVodDownloadModule.h
//  AliyunPlayView
//
//  Created by vun on 2020/1/11.
//  Copyright © 2020 何晏波. All rights reserved.
//

#ifndef AliyunVodDownloadModule_h
#define AliyunVodDownloadModule_h

#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>
#import <AliyunVodPlayerSDK/AliyunVodPlayerSDK.h>
#import <AliyunPlayer/AliyunPlayer.h>
#import <AliyunMediaDownloader/AliyunMediaDownloader.h>

@interface AliyunVodDownloadModule : RCTEventEmitter <RCTBridgeModule, AMDDelegate>

@end

#endif /* AliyunVodDownloadModule_h */
