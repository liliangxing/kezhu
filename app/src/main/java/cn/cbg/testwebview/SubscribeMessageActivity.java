package cn.cbg.testwebview;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;


public class SubscribeMessageActivity extends Activity implements View.OnClickListener{


	private SeekBar seekBar;
	private MusicService musicService;
	private TextView musicStatus;
	private TextView musicTime;
	private Button btnPlayOrPause;
	private SimpleDateFormat time = new SimpleDateFormat("m:ss");
	private Intent data;
	private ProgressDialog progressDialog;//加载界面的菊花
	private File targetFile;
	private File targetFile2;
	private String total;
	private String total2;
	private boolean isStartedPlay;
	private boolean needChanged;
	private String currentUrl;
	private String paramUrl;

	private ServiceConnection sc = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			musicService = ((MusicService.MyBinder)iBinder).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			musicService = null;
		}
	};
	private void bindServiceConnection() {
		Intent intent = new Intent(SubscribeMessageActivity.this, MusicService.class);
		startService(intent);
		bindService(intent, sc, this.BIND_AUTO_CREATE);
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscribe_message);
		//弹出菊花
		progressDialog = new ProgressDialog(this);
		data = getIntent();
		paramUrl = data.getStringExtra("url");
		currentUrl = paramUrl;
		total = data.getStringExtra("total");

		//开播的条件
		String title=HttpUtils.getFileName(paramUrl);
		targetFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),DownloadService.DOWNLOAD_PATH+"/"+title );

		if (!(targetFile.length() + "").equals(total)) {
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}

			progressDialog.setTitle("提示");
			progressDialog.setMessage("正在努力下载……");
			progressDialog.show();

		}


		seekBar = (SeekBar)this.findViewById(R.id.MusicSeekBar);
		seekBar.setProgress(musicService.mp.getCurrentPosition());
		seekBar.setMax(musicService.mp.getDuration());

		musicStatus = (TextView)this.findViewById(R.id.MusicStatus);
		musicTime = (TextView)this.findViewById(R.id.MusicTime);

		btnPlayOrPause = (Button)this.findViewById(R.id.BtnPlayorPause);

		if((targetFile.length()+"").equals(total)){
			doPlay();
		}

	}

	private void doPlay(){
		//隐藏菊花:不为空，正在显示。才隐藏
		if(progressDialog!=null&&progressDialog.isShowing()){
			progressDialog.dismiss();
		}
		musicService = new MusicService(targetFile.getAbsolutePath());
		bindServiceConnection();
		isStartedPlay=true;
	}
	public android.os.Handler handler = new android.os.Handler();
	public Runnable runnable = new Runnable() {
		@Override
		public void run() {

			if(needChanged&&
					targetFile2!=null&&compareFileEqual(targetFile2.length(),total2)){
				musicService.reStart(targetFile2.getAbsolutePath());
				progressDialog.dismiss();
				needChanged =false;
			}

			if (!(targetFile.length() + "").equals(total) && total!=null) {
				long current = targetFile.length();
				double progress = Math.floor(1000.0 * current / Integer.parseInt(total)) / 10;
				progressDialog.setMessage("正在努力下载……" + progress + "% \n" +
						"为了节省流量，第二次打开不需流量\n" +
						"文件路径：内存卡\\Music\\kezhu的文件夹，\n" +
						"即"+targetFile.getAbsolutePath());
			}else{
					if(!isStartedPlay) {
						doPlay();
					}
				}
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

	private String downloadParamUrl(String url){
		//开播的条件
		String title=HttpUtils.getFileName(url);
		 targetFile2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),DownloadService.DOWNLOAD_PATH+"/"+title );
		return targetFile2.toURI().toString();
	}

	private boolean compareFileEqual(long fileLength,String total){
		if (!(fileLength+ "").equals(total) && total!=null){
			return false;
		}
		return true;
	}
	@Override
	protected void onResume() {
		data = getIntent();
		paramUrl = data.getStringExtra("url");
		total2 = data.getStringExtra("total");
		if(!paramUrl.equals(currentUrl)){
			String paramUrlLocal = downloadParamUrl(paramUrl);
			//url变化
			needChanged = true;
			progressDialog.setMessage("正在努力保存文件……" +paramUrlLocal);
			progressDialog.show();
			MusicService.musicDir[1] = paramUrlLocal;
		}
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
		unbindService(sc);
		super.onDestroy();
	}

	public void doFinish() {
		Intent intent2 = new Intent(SubscribeMessageActivity.this, MainActivity.class);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			doFinish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}