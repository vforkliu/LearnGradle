package com.forkliu.venus;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.forkliu.base.VenusBase;
import com.forkliu.venus.math.VenusMath;
import com.orhanobut.logger.Logger;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String mathVersion = VenusMath.VERSION;
        Logger.d("Venus base version:" + VenusBase.getVersion());
        new LogTestThread().start();
    }
}
