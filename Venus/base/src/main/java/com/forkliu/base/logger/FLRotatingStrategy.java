package com.forkliu.base.logger;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.LogStrategy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

class FLRotatingStrategy implements LogStrategy {
    @NonNull
    private Handler handler;

    public FLRotatingStrategy(@NonNull Handler handler) {
        this.handler = handler;
    }

    @Override
    public void log(int level, @Nullable String tag, @NonNull String message) {
        // do nothing on the calling thread, simply pass the tag/msg to the background thread
        handler.sendMessage(handler.obtainMessage(level, message));
    }

    static class WriteHandler extends Handler {

        private String folder;
        private String logName;
        private  int maxFileSize;
        private int backupCount;

        WriteHandler(
                @NonNull Looper looper,
                @NonNull String folder,
                String logName,
                int maxFileSize,
                int backupCount
        ) {
            super(looper);
            this.folder = folder;
            this.logName = logName;
            this.maxFileSize = maxFileSize;
            this.backupCount = backupCount;
        }

        @SuppressWarnings("checkstyle:emptyblock")
        @Override
        public void handleMessage(@NonNull Message msg) {
            String content = (String) msg.obj;

            FileWriter fileWriter = null;
            File logFile;
            try {
                logFile = getLogFile(folder, logName);
            }catch (Exception e){
                e.printStackTrace();
                return;
            }

            try {
                fileWriter = new FileWriter(logFile, true);
                writeLog(fileWriter, content);
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                if (fileWriter != null) {
                    try {
                        fileWriter.flush();
                        fileWriter.close();
                    } catch (IOException e1) { /* fail silently */ }
                }
            }
        }

        /**
         * This is always called on a single background thread.
         * Implementing classes must ONLY write to the fileWriter and nothing more.
         * The abstract class takes care of everything else including close the stream and catching IOException
         *
         * @param fileWriter an instance of FileWriter already initialised to the correct file
         */
        private void writeLog(@NonNull FileWriter fileWriter, @NonNull String content) throws IOException {
            fileWriter.append(content);
        }

        private File getLogFile(@NonNull String folderName, @NonNull String fileName) throws FLLoggerException{
            File folder = new File(folderName);
            if (!folder.exists()) {
                boolean r = folder.mkdirs();
                if (!r){
                    throw new FLLoggerException("folder mkdirs fail:" + folder.getAbsolutePath());
                }
            }

            File newFile = new File(folder, fileName);
            if (newFile.exists() && newFile.length() >= maxFileSize){
                // 开始滚动
                for(int i = backupCount - 1; i > 0; i--){
                    File src = new File(folder, String.format(Locale.UK,"%s.%d", fileName, i));
                    File dst = new File(folder, String.format(Locale.UK,"%s.%d", fileName, i+1));
                    if (src.exists()){
                        if (dst.exists()) dst.delete();
                        src.renameTo(dst);
                    }
                }
                File dst = new File(folder, String.format(Locale.UK,"%s.%d", fileName, 1));
                if (dst.exists()) dst.delete();
                newFile.renameTo(dst);
            }

            return newFile;
        }
    }
}
