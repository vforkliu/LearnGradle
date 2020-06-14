package com.forkliu.base.logger;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.LogStrategy;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import static com.forkliu.base.logger.FLTimedRotatingFormatStrategy.Builder.D;
import static com.forkliu.base.logger.FLTimedRotatingFormatStrategy.Builder.H;
import static com.forkliu.base.logger.FLTimedRotatingFormatStrategy.Builder.M;
import static com.forkliu.base.logger.FLTimedRotatingFormatStrategy.Builder.S;
import static com.forkliu.base.logger.FLTimedRotatingFormatStrategy.Builder.UNIT_D;
import static com.forkliu.base.logger.FLTimedRotatingFormatStrategy.Builder.UNIT_H;
import static com.forkliu.base.logger.FLTimedRotatingFormatStrategy.Builder.UNIT_M;
import static com.forkliu.base.logger.FLTimedRotatingFormatStrategy.Builder.UNIT_S;

class FLTimedRotatingStrategy implements LogStrategy {
    @NonNull
    private Handler handler;

    public FLTimedRotatingStrategy(@NonNull Handler handler) {
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
        private int backupCount;
        private long lastRollTimestamp = System.currentTimeMillis(); // ms
        private long rollInterval = 0;
        private String suffix;
        private SimpleDateFormat suffixFormat;

        WriteHandler(
                @NonNull Looper looper,
                @NonNull String folder,
                String logName,
                int backupCount,
                int when,
                int interval
        ) {
            super(looper);
            this.folder = folder;
            this.logName = logName;
            this.backupCount = backupCount;
            switch (when){
                case S:
                    rollInterval = UNIT_S * interval * 1000;
                    suffixFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.CHINA);
                    suffix = "^\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}$";
                    break;
                case M:
                    rollInterval = UNIT_M * interval * 1000;
                    suffixFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm",Locale.CHINA);
                    suffix = "^\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}$";
                    break;
                case H:
                    rollInterval = UNIT_H * interval * 1000;
                    suffixFormat = new SimpleDateFormat("yyyy-MM-dd_HH",Locale.CHINA);
                    suffix = "^\\d{4}-\\d{2}-\\d{2}_\\d{2}$";
                    break;
                case D:
                    rollInterval = UNIT_D * interval * 1000;
                    suffixFormat = new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA);
                    suffix = "^\\d{4}-\\d{2}-\\d{2}$";
                    break;
                default:
                    rollInterval = UNIT_D * 1000;
                    suffixFormat = new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA);
                    suffix = "^\\d{4}-\\d{2}-\\d{2}$";
                    break;
            }
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

        private static final class MyFileFilter implements FilenameFilter {
            private String prefix;
            private String suffix;
            public MyFileFilter(String prefix,String suffix){
                this.prefix = prefix;
                this.suffix = suffix;
            }

            @Override
            public boolean accept(File dir, String filename) {
                if (filename.startsWith(prefix)){
                    int index = filename.lastIndexOf(".");
                    if (index > 0) {
                        String suffix = filename.substring(index + 1);
                        if (suffix.matches(this.suffix)){
                            return true;
                        }
                    }
                }
                return false;
            }
        }

        private void deleteFiles(){
            File dir = new File(folder);
            File[] files = dir.listFiles(new MyFileFilter(logName,suffix));
            if (files == null || files.length <= backupCount) return;
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    // 升序
                    if (o1.lastModified() > o2.lastModified()) return 1;
                    else if (o1.lastModified() < o2.lastModified()) return -1;
                    else return 0;
                }
            });
            int deleteCount = files.length - backupCount;
            for (int i = 0; i < deleteCount; i++){
                files[i].delete();
            }

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
            if (newFile.exists()){
                long currentTimestamp = System.currentTimeMillis();
                long lastModifyTimestamp = newFile.lastModified();
                if (lastRollTimestamp > lastModifyTimestamp){
                    // 取最小的时间
                    lastRollTimestamp = lastModifyTimestamp;
                }
                if (currentTimestamp - lastRollTimestamp >= rollInterval){
                    // 开始滚动
                    Date date = new Date(lastRollTimestamp);
                    File dst = new File(folder,String.format(Locale.UK,"%s.%s",fileName,suffixFormat.format(date)));
                    if (dst.exists()) dst.delete();
                    newFile.renameTo(dst);
                    lastRollTimestamp = currentTimestamp;

                    deleteFiles();
                }

            }
            return newFile;
        }
    }
}
