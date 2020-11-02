package cn.time24.kezhu;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;

import cn.time24.kezhu.service.AudioFocusManager;
import cn.time24.kezhu.service.MusicService;
import cn.time24.kezhu.utils.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;

import okhttp3.Call;
import okhttp3.Request;


public class SubscribePlayerActivity extends Activity implements View.OnClickListener{


	private SeekBar seekBar;
	public MusicService musicService = MainActivity.musicService;
	private TextView musicStatus;
	private TextView musicTime;
	private Button btnPlayOrPause;
	private Button btnRepeat;
	private SimpleDateFormat time = new SimpleDateFormat("m:ss");
	private Intent data;
	private ProgressDialog progressDialog;//加载界面的菊花
	private boolean isStartedPlay;
	private String currentUrl;
	private String paramUrl;
	public static SubscribePlayerActivity instance;
	public static boolean isRepeat =true;

	private AudioFocusManager audioFocusManager;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		instance =this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscribe_message);
		//弹出菊花
		progressDialog = new ProgressDialog(this);
		data = getIntent();
		paramUrl = data.getStringExtra("url");
		currentUrl = paramUrl;
		audioFocusManager = new AudioFocusManager(this);


		seekBar = (SeekBar)this.findViewById(R.id.MusicSeekBar);

		musicStatus = (TextView)this.findViewById(R.id.MusicStatus);
		musicTime = (TextView)this.findViewById(R.id.MusicTime);

		btnPlayOrPause = (Button)this.findViewById(R.id.BtnPlayorPause);
		btnRepeat = (Button)this.findViewById(R.id.BtnRepeat);


		downloadAndPlay(currentUrl,false);


	}


	public void downloadAndPlay(String url,boolean fromResume){
		String fileName = HttpUtils.getFileName(url);
		String path  = FileUtils.getMusicDir().concat(fileName);
		File file = new File(path);
		final boolean isResume = fromResume;
		if(!file.exists()){
			OkHttpUtils.get().url(url).build()
					.execute(new FileCallBack(FileUtils.getMusicDir(), fileName) {
						@Override
						public void onBefore(Request request, int id) {
							if (progressDialog != null && progressDialog.isShowing()) {
								progressDialog.dismiss();
							}

							progressDialog.setTitle("提示");
							progressDialog.setMessage("正在下载……"+(isResume?"onResume":"onCreate"));
							progressDialog.show();
						}

						@Override
						public void inProgress(float progress, long total, int id) {
							progressDialog.setMessage("正在下载……"+((float)Math.round(progress*100*100)/100)+"%");
						}

						@Override
						public void onResponse(File file, int id) {
							//隐藏菊花:不为空，正在显示。才隐藏
							if(progressDialog!=null&&progressDialog.isShowing()){
								progressDialog.dismiss();
							}
							doPlay(file,isResume);
						}

						@Override
						public void onError(Call call, Exception e, int id) {

						}

						@Override
						public void onAfter(int id) {

						}
					});
		}else {
			doPlay(file,fromResume);
			//异步比较一下文件是否完整
			MainActivity.instance.downloadUrl(url);

		}

	}



	private void doPlay(File targetFile,boolean fromResume){
		if(fromResume){
			musicService.reStart(targetFile.getAbsolutePath());
			return;
		}
		musicService.setUrl(targetFile.getAbsolutePath());
		seekBar.setProgress(musicService.mp.getCurrentPosition());
		seekBar.setMax(musicService.mp.getDuration());
		isStartedPlay=true;
	}
	public android.os.Handler handler = new android.os.Handler();
	public Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if(musicService.mp.isPlaying()) {
				musicStatus.setText(getResources().getString(R.string.playing));
				btnPlayOrPause.setText(getResources().getString(R.string.pause).toUpperCase());
			} else {
				musicStatus.setText(getResources().getString(R.string.pause));
				btnPlayOrPause.setText(getResources().getString(R.string.play).toUpperCase());
			}
			musicTime.setText(time.format(musicService.mp.getCurrentPosition()) + "/"
					+ time.format(musicService.mp.getDuration()));
			seekBar.setProgress(musicService.mp.getCurrentPosition());
			seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						musicService.mp.seekTo(seekBar.getProgress());
					}
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {

				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {

				}
			});
			handler.postDelayed(runnable, 100);
		}
	};

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	protected void onResume() {
		data = getIntent();
		paramUrl = data.getStringExtra("url");
		if(!paramUrl.equals(currentUrl)){
			downloadAndPlay(paramUrl,true);
			currentUrl = paramUrl;
		}
		audioFocusManager.requestAudioFocus();
		musicService.mp.start();
		if(musicService.mp.isPlaying()) {
			musicStatus.setText(getResources().getString(R.string.playing));
		} else {
			musicStatus.setText(getResources().getString(R.string.pause));
		}

		seekBar.setProgress(musicService.mp.getCurrentPosition());
		seekBar.setMax(musicService.mp.getDuration());
		handler.post(runnable);
		super.onResume();
		Log.d("hint", "handler post runnable");
	}

	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.BtnPlayorPause:
				musicService.playOrPause();
				break;
			case R.id.BtnStop:
				musicService.stop();
				seekBar.setProgress(0);
				break;
			case R.id.BtnRepeat:
				if(!isRepeat) {
					btnRepeat.setText("目前是[单曲循环]");
					musicService.mp.setLooping(true);
					isRepeat =true;
				}else {
					btnRepeat.setText("目前是[不重复]");
					musicService.mp.setLooping(false);
					isRepeat =false;
				}
				break;
			case R.id.BtnQuit:
				doFinish();
				break;
			case R.id.btnPre:
				musicService.preMusic();
				break;
			case R.id.btnNext:
				musicService.nextMusic();
				break;
			default:
				break;
		}
	}

	@Override
	public void onDestroy() {
		if(isStartedPlay) {
			musicService.stop();
		}
		super.onDestroy();
	}

	public void doFinish() {
		Intent intent2 = new Intent(SubscribePlayerActivity.this, MainActivity.class);
		startActivity(intent2);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK://返回键
				doFinish();
			default:
				break;
		}
		return super.onKeyDown(keyCode, event);//返回键的super处理的就是退出应用
	}
}