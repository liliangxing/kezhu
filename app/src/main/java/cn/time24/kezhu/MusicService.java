package cn.time24.kezhu;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service {
    public final IBinder binder = new MyBinder();
    public class MyBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public static String[] musicDir = new String[]{
            "https://tw3.amtb.de/media/mp3/61/61-126/61-126-0001.mp3",
            "https://tw3.amtb.de/media/mp3/61/61-126/61-126-0001.mp3",
            "https://tw3.amtb.de/media/mp3/61/61-232/61-232-0001.mp3"};
    private static int musicIndex = 1;

    public static MediaPlayer mp = new MediaPlayer();
    public MusicService() {
        this(null);
    }
    public MusicService(String url) {
        start(url,false);
        mp.setLooping(true);
    }

    public void reStart(String url) {
        start(url,true);
    }

    public void start(String url,boolean isSeek) {
        try {
            musicIndex = 1;
            if(url!=null){musicDir[musicIndex]=url;}
            mp.reset();
            mp.setDataSource(musicDir[musicIndex]);
            mp.prepare();
            if(isSeek) {
                mp.seekTo(0);
            }
            mp.start();
        } catch (Exception e) {
            Log.d("hint","can't get to the song");
            e.printStackTrace();
        }
    }

    public void playOrPause() {
        if(mp.isPlaying()){
            mp.pause();
        } else {
            mp.start();
        }
    }
    public void stop() {
        if(mp != null) {
            mp.stop();
            try {
                mp.prepare();
                mp.seekTo(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void nextMusic() {
        if(mp != null && musicIndex < (musicDir.length-1)) {
            mp.stop();
            try {
                mp.reset();
                mp.setDataSource(musicDir[musicIndex+1]);
                musicIndex++;
                mp.prepare();
                mp.seekTo(0);
                mp.start();
            } catch (Exception e) {
                Log.d("hint", "can't jump next music");
                e.printStackTrace();
            }
        }
    }
    public void preMusic() {
        if(mp != null && musicIndex > 0) {
            mp.stop();
            try {
                mp.reset();
                mp.setDataSource(musicDir[musicIndex-1]);
                musicIndex--;
                mp.prepare();
                mp.seekTo(0);
                mp.start();
            } catch (Exception e) {
                Log.d("hint", "can't jump pre music");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        mp.stop();
        mp.release();
        super.onDestroy();
    }
}
