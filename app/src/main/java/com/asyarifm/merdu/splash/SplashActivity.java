package com.asyarifm.merdu.splash;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.asyarifm.merdu.music.activity.MusicActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //display splash then go to music activity
        startActivity(new Intent(SplashActivity.this, MusicActivity.class));
        finish();
    }
}
