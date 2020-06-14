package com.forkliu.venus;

import android.app.Application;

import com.forkliu.base.logger.FLFileLogAdapter;
import com.forkliu.base.logger.FLRotatingFormatStrategy;
import com.forkliu.base.logger.FLTimedRotatingFormatStrategy;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;


public class FLApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化日志系统
        Logger.addLogAdapter(new AndroidLogAdapter());
        try {
            // 按照大小分割日志
            FLRotatingFormatStrategy rotatingFormatStrategy = FLRotatingFormatStrategy
                    .newBuilder()
                    .backupCount(7)
                    .maxBytes(1024)  // 1024
                    .logDir(getFilesDir().getAbsolutePath())  // 路径必须指定
                    .logName("venus_size.log")
                    .tag("Venus")
                    .build();
            FLFileLogAdapter sizeLogAdapter = new FLFileLogAdapter(rotatingFormatStrategy);
            sizeLogAdapter.setLogLevel(Logger.DEBUG);
            Logger.addLogAdapter(sizeLogAdapter);

            // 按照时间分割日志
            FLTimedRotatingFormatStrategy timedRotatingFormatStrategy = FLTimedRotatingFormatStrategy
                    .newBuilder()
                    .logDir(getFilesDir().getAbsolutePath())
                    .logName("venus_time.log")
                    .tag("Venus")
                    .when(FLTimedRotatingFormatStrategy.Builder.S)
                    .interval(10)
                    .backupCount(6)
                    .build();
            FLFileLogAdapter timedLogAdapter = new FLFileLogAdapter(timedRotatingFormatStrategy);
            timedLogAdapter.setLogLevel(Logger.INFO);
            Logger.addLogAdapter(timedLogAdapter);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
