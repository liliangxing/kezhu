package cn.time24.kezhu.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import cn.time24.kezhu.MainActivity;

public class MusicService extends Service {
    private static final long TIME_UPDATE = 300L;
    public final IBinder binder = new MyBinder();
    public class MyBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }
    private AudioFocusManager audioFocusManager;
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

    public void setUrl(String url) {
        start(url,false);
        mp.setLooping(true);
    }
    private Handler handler;

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
            mp.start();
            if(isSeek) {
                mp.seekTo(0);
            }
            startPlayer();
            handler.post(mPublishRunnable);
        } catch (Exception e) {
            Log.d("hint","can't get to the song");
            e.printStackTrace();
        }
    }

    private Runnable mPublishRunnable = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this, TIME_UPDATE);
        }
    };

    public void startPlayer() {
        audioFocusManager.requestAudioFocus();
        if (mp.isPlaying()) {
            return;
        }else {
            mp.start();
            if (MainActivity.instance.ivLayOut.getVisibility() != View.VISIBLE) {
                MainActivity.instance.ivLayOut.setVisibility(View.VISIBLE);
            }
        }
    }

    public void pausePlayer() {
        pausePlayer(true);
     }
    public void pausePlayer(boolean abandonAudioFocus) {
        if (mp.isPlaying()) {
            mp.pause();
            if(MainActivity.instance.ivLayOut.getVisibility() != View.GONE) {
                MainActivity.instance.ivLayOut.setVisibility(View.GONE);
            }
        }else{
            return;
        }
        if (abandonAudioFocus) {
            audioFocusManager.abandonAudioFocus();
        }
    }
    public void playOrPause() {
        if(mp.isPlaying()){
            pausePlayer();
        } else {
            startPlayer();
        }
    }
    public void stop() {
        handler.removeCallbacks(mPublishRunnable);
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
                boolean looping =mp.isLooping();
                mp.reset();
                mp.setLooping(looping);
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
                boolean looping =mp.isLooping();
                mp.reset();
                mp.setLooping(looping);
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

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        QuitTimer.get().init(this);
        audioFocusManager = new AudioFocusManager(this);
        startForeground( 0x111, buildNotification(this));
    }

    private Notification buildNotification(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentIntent(pendingIntent);
        return builder.build();
    }

    public static void startCommand(Context context, String action) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(action);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "cn.time24.kezhu.ACTION_STOP":
                    stopTimer();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    private void stopTimer() {
        mp.stop();
        QuitTimer.get().stop();
    }
}
