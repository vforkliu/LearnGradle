package com.forkliu.base.logger;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.CsvFormatStrategy;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.LogAdapter;
import com.orhanobut.logger.Logger;
import static com.forkliu.base.logger.FLLogUtils.checkNotNull;

public class FLFileLogAdapter implements LogAdapter {
    // 默认只输出INFO级别及以上的日志
    private int logLevel = Logger.DEBUG;
    @NonNull private final FormatStrategy formatStrategy;
    public FLFileLogAdapter(Context context) throws FLLoggerException{
        formatStrategy = FLRotatingFormatStrategy.newBuilder()
                .logDir(context.getFilesDir().getAbsolutePath())
                .build();
    }
    public FLFileLogAdapter(FormatStrategy formatStrategy){
        this.formatStrategy = checkNotNull(formatStrategy);
    }

    public void setLogLevel(int logLevel){
        this.logLevel = logLevel;
    }

    @Override
    public boolean isLoggable(int priority, @Nullable String tag) {
        if (priority < logLevel) return false;
        return true;
    }

    @Override
    public void log(int priority, @Nullable String tag, @NonNull String message) {
        formatStrategy.log(priority,tag,message);
    }
}
