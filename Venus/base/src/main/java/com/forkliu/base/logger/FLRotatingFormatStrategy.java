package com.forkliu.base.logger;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.LogStrategy;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.forkliu.base.logger.FLLogUtils.checkNotNull;


public class FLRotatingFormatStrategy implements FormatStrategy {

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String NEW_LINE_REPLACEMENT = " <br> ";
    private static final String SEPARATOR = ",";

    @NonNull private final SimpleDateFormat dateFormat;
    @NonNull private final LogStrategy logStrategy;
    @Nullable private final String tag;

    private FLRotatingFormatStrategy(@NonNull Builder builder){
        dateFormat = builder.dateFormat;
        logStrategy = builder.logStrategy;
        tag = builder.tag;
    }

    @NonNull public static Builder newBuilder() {
        return new Builder();
    }
    
    @Override
    public void log(int priority, @Nullable String onceOnlyTag, @NonNull String message) {
        checkNotNull(message);

        String tag = formatTag(onceOnlyTag);

        Date date = new Date();
        date.setTime(System.currentTimeMillis());

        StringBuilder builder = new StringBuilder();

        // machine-readable date/time
        builder.append(Long.toString(date.getTime()));

        // human-readable date/time
        builder.append(SEPARATOR);
        builder.append(dateFormat.format(date));

        // level
        builder.append(SEPARATOR);
        builder.append(FLLogUtils.logLevel(priority));

        // tag
        builder.append(SEPARATOR);
        builder.append(tag);

        // message
        if (message.contains(NEW_LINE)) {
            // a new line would break the CSV format, so we replace it here
            message = message.replaceAll(NEW_LINE, NEW_LINE_REPLACEMENT);
        }
        builder.append(SEPARATOR);
        builder.append(message);

        // new line
        builder.append(NEW_LINE);

        logStrategy.log(priority, tag, builder.toString());
    }

    @Nullable private String formatTag(@Nullable String tag) {
        if (!FLLogUtils.isEmpty(tag) && !FLLogUtils.equals(this.tag, tag)) {
            return this.tag + "-" + tag;
        }
        return this.tag;
    }
    // logDir
    // logName
    // maxBytes 当文件大小超过maxBytes就会创建一个日志文件
    // backupCount 保留的日志文件个数

    public static final class Builder {
        private static final int MAX_BYTES = 500 * 1024; // 500K averages to a 4000 lines per file

        SimpleDateFormat dateFormat;
        LogStrategy logStrategy;
        String tag;
        String logDir;
        String logName;
        int maxBytes = 0;
        int backupCount = 0;

        private Builder() {
        }


        @NonNull public Builder dateFormat(@Nullable SimpleDateFormat val) {
            dateFormat = val;
            return this;
        }

        @NonNull public Builder logStrategy(@Nullable LogStrategy val) {
            logStrategy = val;
            return this;
        }

        @NonNull public Builder tag(@Nullable String tag) {
            this.tag = tag;
            return this;
        }

        @NonNull public Builder logDir(@NonNull String val) {
            this.logDir = val;
            return this;
        }

        @NonNull public Builder logName(@Nullable String val) {
            this.logName = val;
            return this;
        }

        @NonNull public Builder backupCount(int val) {
            this.backupCount = val;
            return this;
        }

        @NonNull public Builder maxBytes(int val) {
            this.maxBytes = val;
            return this;
        }

        @NonNull public FLRotatingFormatStrategy build() throws FLLoggerException{
            if (logDir == null){
                throw new FLLoggerException("logDir is null");
            }
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.CHINA);
            }
            if (logName == null){
                logName = "log.log";
            }
            if (maxBytes <= 0){
                maxBytes = MAX_BYTES;
            }
            if (backupCount <= 0){
                backupCount = 7;
            }
            if (logStrategy == null) {
                String folder = logDir + File.separatorChar + "logger";

                HandlerThread ht = new HandlerThread("AndroidFileLogger." + folder);
                ht.start();
                Handler handler = new FLRotatingStrategy.WriteHandler(
                        ht.getLooper(),
                        folder,
                        logName,
                        maxBytes,
                        backupCount
                );
                logStrategy = new FLRotatingStrategy(handler);
            }
            return new FLRotatingFormatStrategy(this);
        }
    }
}
