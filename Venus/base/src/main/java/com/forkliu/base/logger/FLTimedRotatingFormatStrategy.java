package com.forkliu.base.logger;

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

public class FLTimedRotatingFormatStrategy implements FormatStrategy {
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String NEW_LINE_REPLACEMENT = " <br> ";
    private static final String SEPARATOR = ",";

    @NonNull private final SimpleDateFormat dateFormat;
    @NonNull private final LogStrategy logStrategy;
    @Nullable private final String tag;

    private FLTimedRotatingFormatStrategy(@NonNull Builder builder){
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

    public static final class Builder {
        public static final int S = 1;
        public static final int M = 2;
        public static final int H = 3;
        public static final int D = 4;
        public static final int UNIT_S = 1;
        public static final int UNIT_M = 60;
        public static final int UNIT_H = 60 * 60;
        public static final int UNIT_D = 24 * 60 * 60;


        SimpleDateFormat dateFormat;
        LogStrategy logStrategy;
        String tag;
        String logDir;
        String logName;
        int backupCount = 0;
        int when = D;
        int interval = 1;

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

        @NonNull public Builder when(int val) {
            this.when = val;
            return this;
        }
        @NonNull public Builder interval(int val) {
            this.interval = val;
            return this;
        }

        @NonNull public FLTimedRotatingFormatStrategy build() throws FLLoggerException{
            if (logDir == null){
                throw new FLLoggerException("logDir is null");
            }
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.CHINA);
            }
            if (logName == null){
                logName = "flog.log";
            }
            if (backupCount <= 0){
                backupCount = 7;
            }
            if (interval <= 0) interval = 1;
            if (!(when >= S && when <= D)) when = D;

            if (logStrategy == null) {
                String folder = logDir + File.separatorChar + "logger";

                HandlerThread ht = new HandlerThread("AndroidFileLogger." + folder);
                ht.start();
                Handler handler = new FLTimedRotatingStrategy.WriteHandler(
                        ht.getLooper(),
                        folder,
                        logName,
                        backupCount,
                        when,
                        interval
                );
                logStrategy = new FLTimedRotatingStrategy(handler);
            }
            return new FLTimedRotatingFormatStrategy(this);
        }
    }

    // logDir
    // logName 文件名
    // when 时间间隔
    // interval 间隔几个
    // backupCount 保留日志的文件个数
}
