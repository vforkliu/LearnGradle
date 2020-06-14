package com.forkliu.venus;

import com.orhanobut.logger.Logger;

class LogTestThread extends Thread {
    @Override
    public void run() {
        int count = 1000;
        int i = 0;
        while(i < count){
            Logger.d("LogTestThread::run|" + i);
            Logger.i("LogTestThread::run|" + i);
            try{
                Thread.sleep(100);
            }catch (Exception e){
                e.printStackTrace();
            }
            i++;
        }

        Logger.d("LogTestThread::run FINISHED");
        Logger.i("LogTestThread::run FINISHED");
    }
}
