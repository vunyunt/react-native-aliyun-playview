//
//  AliyunVodDownloadModule.m
//  AliyunPlayView
//
//  Created by vun on 2020/1/11.
//

#import "AliyunVodDownloadModule.h"

@implementation AliyunVodDownloadModule
{
    NSString * const EVENT_UPDATE_AUTH = @"AliyunVod.Downloader.UpdateAuth";
    NSString * const EVENT_PREPARED = @"AliyunVod.Downloader.Prepared";
    NSString * const EVENT_START = @"AliyunVod.Downloader.Start";
    NSString * const EVENT_PROGRESS = @"AliyunVod.Downloader.Progress";
    NSString * const EVENT_COMPLETE = @"AliyunVod.Downloader.Completed";
    NSString * const EVENT_STOP = @"AliyunVod.Downloader.Stop";
    NSString * const EVENT_ERROR = @"AliyunVod.Downloader.Error";

    boolean * mAuthInfoSet = NO;
    NSCondition * mAuthInfoLock;
    NSString * mAuthInfoVidId = "";
    NSString * mAuthInfoAuthStr = "";
    AliyunVodDownloadManager * mDownloadManager;
}

RCT_EXPORT_MODULE()

-(id) init {
    self = [super init];
    if(self) {
        mAuthInfoLock = [[NSCondition alloc] init];
    }
    return self;
}

-(void) initalize {
    NSLog(@"ios initialize");
    
    [[AliyunVodDownLoadManager shareManager] setDownloadDelegate:self];
    NSString *path = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES)[0];
    [[AliyunVodDownLoadManager shareManager] setDownLoadPath:path];
}

-(NSString*) onGetPlayAuth:(NSString*)vid format:(NSString*)format quality:(AliyunVodPlayerVideoQuality)quality {
    [self sendEventWithName:EVENT_UPDATE_AUTH body:@{@"vidId": vid, @"quality": quality, @"format": format}];
    [mAuthInfoLock lock];
    while(!mAuthInfoSet) {
        [mAuthInfoLock wait];
    }
    
    return mAuthInfoAuthStr;
}

RCT_EXPORT_METHOD((void) setAuth:(NSString*)vidId authStr:(NSString*)authStr) {
    [mAuthInfoLock lock];
    
    mAuthInfoVidId = vidId;
    mAuthInfoAuthStr = authStr;
    mAuthInfoSet = YES;
    
    [mAuthInfoLock signal];
    [mAuthInfoLock unlock];
}

-(void) onPrepare:(NSArray<AliyunDownloadMediaInfo*>*)mediaInfos {
    [self sendEventWithName:EVENT_PREPARED body:mediaInfos];
}

-(void) onStart:(AliyunDownloadMediaInfo*)mediaInfo {
    [self sendEventWithName:EVENT_START body:@[mediaInfo]];
}

-(void) onProgress:(AliyunDownloadMediaInfo*)mediaInfo {
    [self sendEventWithName:EVENT_PROGRESS body:@[mediaInfo]];
}

-(void) onStop:(AliyunDownloadMediaInfo*)mediaInfo {
    [self sendEventWithName:EVENT_STOP body:@[mediaInfo]];
}

-(void) onCompletion:(AliyunDownloadMediaInfo*)mediaInfo {
    [self sendEventWithName:EVENT_COMPLETE body:@[mediaInfo]];
}

-(void)onError:(AliyunDownloadMediaInfo*)mediaInfo code:(int)code msg:(NSString *)msg{
    [self sendEventWithName:EVENT_COMPLETE body:@{
        @"media": mediaInfo,
        @"i": code,
        @"s1": msg
    }];
}

RCT_EXPORT_METHOD(startAuthDownload:(NSString*)vidId, authStr:(NSString*)authStr, format:(NSString*)format, quality:(NSString*)quality, encrypted:(boolean*)encrypted) {
    AliyunDataSource* source = [[AliyunDataSource alloc] init];
    source.vid = vidId;
    source.playAuth = authStr;
    source.quality = quality;
    source.format = format;
    
    [[AliyunVodDownLoadManager shareManager] startDownloadMedia:source];
}

@end
