package cn.time24.kezhu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.time24.kezhu.R;


public class MainActivity extends Activity implements View.OnClickListener{

	private WebView videowebview;
	private LinearLayout linearLayout;
	private ProgressDialog progressDialog;//加载界面的菊花
	private EditText etUrl;

	private static final String FILE_NAME = "test.txt";
	private static final String HOME_PAGE = "http://www.time24.cn/kezhu.html";
	private boolean downloaded;
	private ClipboardManager cm;

	private FrameLayout videoview;// 全屏时视频加载view
	private Button videolandport;
	private Boolean islandport = false;//true表示此时是竖屏，false表示此时横屏。
	private View xCustomView;
	private xWebChromeClient xwebchromeclient;
	private WebChromeClient.CustomViewCallback     xCustomViewCallback;

	private ImageView ivPlay;
	private LinearLayout ivLayOut;
	private JSInterface jsInterface;
	private String LAST_OPEN_URL;
	private Handler handler1;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		checkPermission();
		initView();
		jsInterface = new JSInterface();
		videowebview.addJavascriptInterface(
				jsInterface
				, "itcast");

		initwidget();
		initListener();

		handler1 = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				String url = LAST_OPEN_URL;
				if (url!=null) {
					showToastMsg(url);
				}
			}
		};

		String url = readFileData(FILE_NAME); // 读取文件
		if(url.equals("")){url=HOME_PAGE;}
		etUrl.setText(url);
		videowebview.loadUrl(url);
		linearLayout.setVisibility(View.GONE);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
	/**
	 * 初始化控件
	 */
	private void initView() {
		videowebview = (WebView) findViewById(R.id.webview);
		etUrl = (EditText) findViewById(R.id.et_url);
		linearLayout = findViewById(R.id.ll_web);
		ivPlay = findViewById(R.id.iv_play);
		ivLayOut = findViewById(R.id.iv_play_layout);
	}


	private void checkPermission() {
		int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					Constants.PERMISSIONS_REQUEST_STORAGE);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case Constants.PERMISSIONS_REQUEST_STORAGE: {
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				} else {
					Toast.makeText(MainActivity.this,"Please give me storage permission!",Toast.LENGTH_LONG).show();
				}
				return;
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK://返回键
				//全屏模式，退出
				if (inCustomView()) {
					hideCustomView();
					return true;
				}
				if(islandport){
					if(videolandport.getVisibility()==View.GONE ) {
						videolandport.setVisibility(View.VISIBLE);
						return true;
					}else {
						videolandport.callOnClick();
					}
				}
				if(videolandport.getVisibility()==View.VISIBLE ) {
					videolandport.setVisibility(View.GONE);
					return true;
				}
				//Webview能不能后退
				if(videowebview.canGoBack()){
					//返回上一个页
					videowebview.goBack();
					return true;//消费返回键
				}else {
					moveTaskToBack(true);
					return true;
				}

			default:
				break;
		}
		return super.onKeyDown(keyCode, event);//返回键的super处理的就是退出应用
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		System.out.println("onCreateOptionsMenu");
		//前进，后退，刷新、测试
		menu.addSubMenu(0, 0, 0, "前进");
		menu.addSubMenu(0, 1, 1, "后退");
		menu.addSubMenu(0, 2, 2, "刷新");
		menu.addSubMenu(0, 3, 3, "显/隐地址栏");
		menu.addSubMenu(0, 4, 4, "设为主页");
		menu.addSubMenu(0, 5, 5, "分享到微信");
		return true;//自己处理，显示菜单
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		System.out.println("onOptionsItemSelected");
		switch (item.getItemId()) {
			case 0://前进
				if(videowebview.canGoForward()){
					videowebview.goForward();
				}else{
					Toast.makeText(MainActivity.this,
							"已经是最后一个界面了", Toast.LENGTH_SHORT).show();
				}

				break;
			case 1://后退
				if(videowebview.canGoBack()){
					videowebview.goBack();
				}else{
					Toast.makeText(MainActivity.this,
							"已经是第一页了", Toast.LENGTH_SHORT).show();
				}

				break;
			case 2://刷新
				videowebview.reload();//重新加载

				break;
			case 3://地址栏
				if(linearLayout.getVisibility()==View.GONE) {
					linearLayout.setVisibility(View.VISIBLE);
				}else {
					linearLayout.setVisibility(View.GONE);
				}
				break;
			case 4://测试
				setHomePage();
				break;
			case 5://测试
				shareWeixin();
				break;

			default:
				break;
		}
		return true;//自己处理
	}

	private void shareWeixin(){
		Intent intent = new Intent(MainActivity.this, SubscribeMiniProgramMsgActivity.class);
		intent.putExtra("title", videowebview.getTitle());
		intent.putExtra("url", videowebview.getUrl());

		//获取剪贴板管理器：
		cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData cmData = cm.getPrimaryClip();
		ClipData.Item item = cmData.getItemAt(0);
		String content = item.getText().toString();
		intent.putExtra("content", content);
		startActivity(intent);
	}
	private final class JSInterface{
		@SuppressLint("JavascriptInterface")
		@JavascriptInterface
		public void showToast(String url){
			Message message = new Message();
			LAST_OPEN_URL=url;
			handler1.sendMessage(message);
		}
	}

	private void showToastMsg(String url){
		LAST_OPEN_URL=url;
		if(!downloaded) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					checkDownload(MusicService.musicDir[0]);
					checkDownload(MusicService.musicDir[2]);
					downloaded = true;
				}
			}).start();
		}
		Intent intent2 = new Intent(MainActivity.this, SubscribeMessageActivity.class);
		intent2.putExtra("url", url);
		startActivity(intent2);
		ivLayOut.setVisibility(View.VISIBLE);
	}
	private String checkDownload(String url){
		if(!HttpUtils.checkUrl(url)){
			return url;
		}
		downloadUrl(url);
		return getDownloadFilePath(url);
	}
	private void downloadUrl(String url){
		Intent intent = new Intent(MainActivity.this, DownloadService.class);
		intent.putExtra("url",url);
		intent.putExtra("title",HttpUtils.getFileName(url));
		intent.putExtra("total",HttpUtils.getTotal(url));
		startService(intent);
	}

	public static String  getDownloadFilePath(String url){
		String title =HttpUtils.getFileName(url);
		File targetFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),DownloadService.DOWNLOAD_PATH+"/"+title );
		return targetFile.toURI().toString();
	}

	private void setHomePage(){
		String currentUrl = videowebview.getUrl();
		etUrl.setText(currentUrl);
		writeFileData(FILE_NAME,  currentUrl); // 写入文件
		Toast.makeText(MainActivity.this,
				"设置成功", Toast.LENGTH_SHORT).show();
	}


	//向指定的文件中写入指定的数据
	public void writeFileData(String filename, String content){
		try {
			FileOutputStream fos = this.openFileOutput(filename, MODE_PRIVATE);
			//将要写入的字符串转换为byte数组
			byte[]  bytes = content.getBytes();
			fos.write(bytes);//将byte数组写入文件
			fos.close();//关闭文件输出流
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//打开指定文件，读取其数据，返回字符串对象
	public String readFileData(String fileName){
		String result="";
		try{
			FileInputStream fis = this.openFileInput(fileName);
			//获取文件长度
			int lenght = fis.available();
			byte[] buffer = new byte[lenght];
			fis.read(buffer);
			//将byte数组转换成指定格式的字符串
			result = new String(buffer, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return  result;
	}
	/**
	 * 跳转操作
	 * @param view
	 */
	public void toChange(View view){
		//1.获取地址
		String url = etUrl.getText().toString().trim();
		if(TextUtils.isEmpty(url)){
			url = HOME_PAGE;//如果为空，赋默认值：tomcat首页
		}
		//2.webview展示地址
		videowebview.loadUrl(url);
	}

    public void toStopAndHide(View view){
		SubscribeMessageActivity.instance.musicService.mp.pause();
		ivLayOut.setVisibility(View.GONE);
    }


	/**
	 * 处理Javascript的对话框、网站图标、网站标题以及网页加载进度等
	 * @author
	 */
	public class xWebChromeClient extends WebChromeClient {
		private Bitmap xdefaltvideo;
		private View xprogressvideo;
		@Override
		//播放网络视频时全屏会被调用的方法
		public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback)
		{
			videowebview.setVisibility(View.GONE);
			//如果一个视图已经存在，那么立刻终止并新建一个
			if (xCustomView != null) {
				callback.onCustomViewHidden();
				return;
			}
			videoview.addView(view);
			xCustomView = view;
			xCustomViewCallback = callback;
			videoview.setVisibility(View.VISIBLE);
			videoview.bringToFront();
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
		}

		@Override
		//视频播放退出全屏会被调用的
		public void onHideCustomView() {

			if (xCustomView == null)//不是全屏播放状态
				return;
			xCustomView.setVisibility(View.GONE);

			// Remove the custom view from its container.
			videoview.removeView(xCustomView);
			xCustomView = null;
			videoview.setVisibility(View.GONE);
			xCustomViewCallback.onCustomViewHidden();

			videowebview.setVisibility(View.VISIBLE);
			// Hide the custom view.
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//清除全屏

			//Log.i(LOGTAG, "set it to webVew");
		}
		//视频加载添加默认图标
		@Override
		public Bitmap getDefaultVideoPoster() {
			//Log.i(LOGTAG, "here in on getDefaultVideoPoster");
			if (xdefaltvideo == null) {
				xdefaltvideo = BitmapFactory.decodeResource(
						getResources(), R.drawable.ic_launcher);
			}
			return xdefaltvideo;
		}
		//视频加载时进程loading
		@Override
		public View getVideoLoadingProgressView() {
			//Log.i(LOGTAG, "here in on getVideoLoadingPregressView");

			if (xprogressvideo == null) {
				LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
				xprogressvideo = inflater.inflate(R.layout.alert_dialog_menu_layout, null);
			}
			return xprogressvideo;
		}
		//网页标题
		@Override
		public void onReceivedTitle(WebView view, String title) {
			(MainActivity.this).setTitle(title);
		}

//         @Override
//       //当WebView进度改变时更新窗口进度
//         public void onProgressChanged(WebView view, int newProgress) {
//             (MainActivity.this).getWindow().setFeatureInt(Window.FEATURE_PROGRESS, newProgress*100);
//         }

		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			if(progressDialog!=null&&progressDialog.isShowing())
				progressDialog.setMessage("正在努力加载……"+newProgress+"%");
		}
		/**
		 * 重写alert、confirm和prompt的回调
		 */
		/**
		 * Webview加载html中有alert()执行的时候，回调
		 * url:当前Webview显示的url
		 * message：alert的参数值
		 * JsResult：java将结果回传到js中
		 */
		@Override
		public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setTitle("提示");
			builder.setMessage(message);//这个message就是alert传递过来的值
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which) {
					//处理确定按钮了
					result.confirm();//通过jsresult传递，告诉js点击的是确定按钮
				}
			});
			builder.show();

			return true;//自己处理
		}
		@Override
		public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setTitle("提示");
			builder.setMessage(message);//这个message就是alert传递过来的值
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which) {
					//处理确定按钮了
					result.confirm();//通过jsresult传递，告诉js点击的是确定按钮
				}
			});
			builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which) {
					//处理取消按钮
					//告诉js点击的是取消按钮
					result.cancel();

				}
			});
			builder.show();
			return true;//自己处理
		}
		/**
		 * defaultValue就是prompt的第二个参数值，输入框的默认值
		 * JsPromptResult：向js回传数据
		 */
		@Override
		public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
								  final JsPromptResult result) {
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setTitle("提示");
			builder.setMessage(message);//这个message就是alert传递过来的值
			//添加一个EditText
			final EditText editText = new EditText(MainActivity.this);
			editText.setText(defaultValue);//这个就是prompt 输入框的默认值
			//添加到对话框
			builder.setView(editText);
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which) {
					//获取edittext的新输入的值
					String newValue = editText.getText().toString().trim();
					//处理确定按钮了
					result.confirm(newValue);//通过jsresult传递，告诉js点击的是确定按钮(参数就是输入框新输入的值，我们需要回传到js中)
				}
			});
			builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which) {
					//处理取消按钮
					//告诉js点击的是取消按钮
					result.cancel();

				}
			});
			builder.show();
			return true;//自己处理
		}
	}
	/**
	 * 处理各种通知、请求等事件
	 * @author
	 */
	/**
	 * 验证网址Url
	 *
	 * @param 待验证的字符串
	 * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
	 */
	public static boolean IsUrl(String file) {
		String suffixes = "avi|mpeg|3gp|mp3|mp4|wav|jpeg|gif|jpg|png|apk|exe|txt|java|doc";
		Pattern pat = Pattern.compile("([\\w|-]+)[\\.](" + suffixes + ")");//正则判断
		Matcher mc = pat.matcher(file);
		while(mc.find()){
			return true;
		}
		return false;
	}
	public class xWebViewClientent extends WebViewClient {
		/**
		 * 当打开超链接的时候，回调的方法
		 * WebView：自己本身videowebview
		 * url：即将打开的url
		 */
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if(IsUrl(url)){
				Intent intent = new Intent(MainActivity.this, FullScreenActivity.class);
				intent.putExtra("url",url);
				startActivity(intent);
				return true;
			}
			if(url.contains("playVideo=1")){
				videolandport.setVisibility(View.VISIBLE);
			}else {
				videolandport.setVisibility(View.GONE);
			}
			videowebview.loadUrl(url);
			return true;//true就是自己处理
		}
		//重写页面打开和结束的监听。添加友好，弹出菊花
		/**
		 * 界面打开的回调
		 */
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			System.out.println("onPageStarted");
			if(progressDialog!=null&&progressDialog.isShowing()){
				progressDialog.dismiss();
			}
			//弹出菊花
			progressDialog = new ProgressDialog(MainActivity.this);
			progressDialog.setTitle("提示");
			progressDialog.setMessage("正在努力加载……");
			progressDialog.show();

		}
		/**
		 * 界面打开完毕的回调
		 */
		@Override
		public void onPageFinished(WebView view, String url) {
			System.out.println("onPageFinished");
			//隐藏菊花:不为空，正在显示。才隐藏
			if(progressDialog!=null&&progressDialog.isShowing()){
				progressDialog.dismiss();
			}

		}
	}
	/**
	 * 当横竖屏切换时会调用该方法
	 * @author
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.i("testwebview", "=====<<<  onConfigurationChanged  >>>=====");
		super.onConfigurationChanged(newConfig);

		if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
			Log.i("webview", "   现在是横屏1");
		}else if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			Log.i("webview", "   现在是竖屏1");
		}
	}


	private void initListener() {
		// TODO Auto-generated method stub
		videolandport.setOnClickListener(this);
		ivPlay.setOnClickListener(this);
	}

	private void initwidget() {
		// TODO Auto-generated method stub
		videoview = (FrameLayout) findViewById(R.id.video_view);
		videolandport = (Button) findViewById(R.id.video_landport);
		videowebview = (WebView) findViewById(R.id.webview);
		WebSettings ws = videowebview.getSettings();
		/**
		 * setAllowFileAccess 启用或禁止WebView访问文件数据 setBlockNetworkImage 是否显示网络图像
		 * setBuiltInZoomControls 设置是否支持缩放 setCacheMode 设置缓冲的模式
		 * setDefaultFontSize 设置默认的字体大小 setDefaultTextEncodingName 设置在解码时使用的默认编码
		 * setFixedFontFamily 设置固定使用的字体 setJavaSciptEnabled 设置是否支持Javascript
		 * setLayoutAlgorithm 设置布局方式 setLightTouchEnabled 设置用鼠标激活被选项
		 * setSupportZoom 设置是否支持变焦
		 * */
		ws.setBuiltInZoomControls(true);// 隐藏缩放按钮
		ws.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);// 排版适应屏幕
		ws.setUseWideViewPort(true);// 可任意比例缩放
		ws.setLoadWithOverviewMode(true);// setUseWideViewPort方法设置webview推荐使用的窗口。setLoadWithOverviewMode方法是设置webview加载的页面的模式。
		ws.setSavePassword(true);
		ws.setSaveFormData(true);// 保存表单数据
		ws.setJavaScriptEnabled(true);
		ws.setGeolocationEnabled(true);// 启用地理定位
		ws.setGeolocationDatabasePath("/data/data/org.itri.html5webview/databases/");// 设置定位的数据库路径
		ws.setDomStorageEnabled(true);

		ws.setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);//显示自己的html
		ws.setMediaPlaybackRequiresUserGesture(false);
		if (android.os.Build.VERSION.SDK_INT >= 19) {
			ws.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		}
		xwebchromeclient = new xWebChromeClient();
		videowebview.setWebChromeClient(xwebchromeclient);
		videowebview.setWebViewClient(new xWebViewClientent());
	}


		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
				case R.id.video_landport:
					if (islandport) {
						setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					}else {
						setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
						videolandport.setVisibility(View.GONE);
					}

					islandport = !islandport;
					break;
				case R.id.iv_play:
					showToastMsg(LAST_OPEN_URL);
					break;
				default:
					break;
			}
		}

	/**
	 * 判断是否是全屏
	 * @return
	 */
	public boolean inCustomView() {
		return (xCustomView != null);
	}
	/**
	 * 全屏时按返加键执行退出全屏方法
	 */
	public void hideCustomView() {
		xwebchromeclient.onHideCustomView();
	}

}