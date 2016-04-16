package com.michael.sknotes;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.File;

/**
 * Created by Yumichael on 16-03-24.
 */
public class PlayAudio extends Service {

    private static final String LOGCAT = null;
    MediaPlayer mediaPlayer;

    public void onCreate(){
        super.onCreate();
        File path = new File(getFilesDir(), "audiofile");
        final File audioFile = new File(path, EditorActivity.audioFileName);
        mediaPlayer = MediaPlayer.create(this, Uri.parse(audioFile.getPath()));
    }

    public int onStartCommand(Intent intent, int flags, int startId){

        mediaPlayer.start();


        return 1;
    }

    public void onStop(){
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    public void onPause(){
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }
    public void onDestroy(){
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}