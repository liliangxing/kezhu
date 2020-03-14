package cn.time24.kezhu;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import cn.time24.kezhu.utils.FileUtils;

/**
 * 下载服务 IntentService
 * 生命周期：
 * 1>当第一次启动IntentService时，Android容器
 *  将会创建IntentService对象。
 * 2>IntentService将会在工作线程中轮循消息队列，
 *  执行每个消息对象中的业务逻辑。
 * 3>如果消息队列中依然有消息，则继续执行，
 *  如果消息队列中的消息已经执行完毕，
 *  IntentService将会自动销毁，执行onDestroy方法。
 */
public class DownloadService extends IntentService {
    private static final int NOTIFICATION_ID = 100;
    public DownloadService(){
        super("download");
    }
    public DownloadService(String name) {
        super(name);
    }
    /**
     * 该方法中的代码将会在工作线程中执行
     * 每当调用startService启动IntentService后，
     * IntentService将会把OnHandlerIntent中的
     * 业务逻辑放入消息队列等待执行。
     * 当工作线程轮循到该消息对象时，将会
     * 执行该方法。
     */

    protected void onHandleIntent(Intent intent) {
        //发送Http请求 执行下载业务
        //1. 获取音乐的路径
        String url=intent.getStringExtra("url");
        String title=intent.getStringExtra("title");
        String totalSize = HttpUtils.getTotal(url);
        File targetFile = new File(FileUtils.getMusicDir(),title);

        //文件存在，大小
        if(targetFile.exists()){
            //网络异常，获取不了链接
            if((targetFile.length()+"").equals(totalSize)) {
                doChangeFileLink(url);
                return;
            }
                Log.i("info", "音乐大小不一致，删除旧文件");

        }
        //下载并替换播放列表
        if(!targetFile.getParentFile().exists()){
            targetFile.getParentFile().mkdirs();
        }
        try {
            sendNotification("音乐开始下载", "音乐开始下载");
            //3. 发送Http请求，获取InputStream
            InputStream is = HttpUtils.getInputStream(url);
            //4. 边读取边保存到File对象中
            FileOutputStream fos = new FileOutputStream(targetFile);
            byte[] buffer = new byte[1024*100];
            int length=0;
            int current = 0;
            int total = Integer.parseInt(totalSize);
            while((length=is.read(buffer)) != -1){
                fos.write(buffer, 0, length);
                fos.flush();
                current += length;
                //通知下载的进度
                double progress = Math.floor(1000.0*current/total)/10;
                sendNotification("音乐开始下载", "下载进度："+progress+"%");
            }
            //5. 文件下载完成
            fos.close();
            cancelNotification(); //重新出现滚动消息
            sendNotification("音乐下载完成", "音乐下载完毕");
            doChangeFileLink(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void doChangeFileLink(String url){
        if(url.equals(MusicService.musicDir[0])){
            MusicService.musicDir[0] = MainActivity.getDownloadFilePath(url);
        }else if(url.equals(MusicService.musicDir[2])){
            MusicService.musicDir[2] = MainActivity.getDownloadFilePath(url);
        }
    }
    /**
     * 发通知
     */
    public void sendNotification(String ticker, String text){
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("音乐下载")
                .setContentText(text)
                .setTicker(ticker);
        Notification n = builder.build();
        manager.notify(NOTIFICATION_ID, n);
    }
    /**
     * 取消通知
     */
    public void cancelNotification(){
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);
    }
}
