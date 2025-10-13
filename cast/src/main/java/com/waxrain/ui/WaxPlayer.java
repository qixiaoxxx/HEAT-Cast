package com.waxrain.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.waxrain.airplaydmr.*;
import com.waxrain.airplaydmr_SDK.R;
import com.waxrain.droidsender.delegate.*;
import com.waxrain.utils.*;
import com.waxrain.video.*;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class WaxPlayer extends FragmentActivity implements OnFocusChangeListener, OnTouchListener, OnClickListener/*, OnSystemUiVisibilityChangeListener*/ {
	private static final String LOG_TAG = com.waxrain.airplaydmr.WaxPlayService.LOG_TAG;
	public static final int MSG_CHANGEPS_WHOLELAYOUT = 1;
	public static final int MSG_SHOW_WHOLELAYOUT = 2;
	public static final int MSG_HIDE_WHOLELAYOUT = 3;
	public static final int MSG_ADD_PLAYER = 4;
	public static final int MSG_DEL_PLAYER = 5;
	public static final int MSG_RESORT_PLAYER = 6;
	public static final int MSG_SWITCH2PREV_PLAYER = 7;
	public static final int MSG_SWITCH2NEXT_PLAYER = 8;
	public static final int MSG_FINISH_ACTIVITY = 9;
	public static final int MSG_ADD_DIALOG = 10;
	public static final int MSG_DEL_DIALOG = 11;
	public static final int MSG_DLG_CONTINUE = 12;
	public static final int MSG_CHECK_VIDEOSIZE = 13;
	public static final int MSG_VIDEOSIZE_CHANGED = 14;

	private int dialogCount = 0;
	public WaxPlayer2[] MPlayerArray = new WaxPlayer2[]{null};
	// Snoopy 8/29/2022 : added
	public static float VideoRatioPhone = 0.75f;
	private int[] MPlayerWidthArray = null;
	private int[] MPlayerHeightArray = null;
	private float[] MPlayerRatioArray = null;
	// End 8/29/2022
	public int[] MPlayerResID = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public int[] MPlayerLayoutX = new int[WaxPlayService.MAX_PLAYERS_N1];
	public int[] MPlayerLayoutY = new int[WaxPlayService.MAX_PLAYERS_N1];
	public int[] MPlayerLayoutW = new int[WaxPlayService.MAX_PLAYERS_N1];
	public int[] MPlayerLayoutH = new int[WaxPlayService.MAX_PLAYERS_N1];
	public int[] MPlayerIdResort = new int[WaxPlayService.MAX_PLAYERS_N1];
	public int MPlayerCount = 0;
	public int MPlayerFocused = -1;
	public int MPlayerZoomed = -1;
	private int DropNext_KeyCode = -1;
	private static final AtomicInteger sNextGeneratedId2 = new AtomicInteger(1);
	private static final ReentrantLock MPlayerLock = new ReentrantLock();
	private static final ReentrantLock stopFromUser_Lock = new ReentrantLock();
	private static HashMap<String, Integer>[] stopFromUser_Map = new HashMap[WaxPlayer2.PROTOCOL_MAX];
	public WaxPlayer2 newPlayer = null;
	public Handler mainHandler = null;
	public static int inBackground = 0;
	public static int zoomState = Config.ZOOM;
	private boolean STATUS_EXITING = false;
	private Random _rand = new Random(System.currentTimeMillis());
	public static int screenMargin = 0;
	public static int screenWidth = 1280, screenHeight = 720; // current
	public static int screenXoff = 0, screenYoff = 0;
	public int dispWidthSaved = 0, dispHeightSaved = 0;
	public int orientationSaved = Configuration.ORIENTATION_UNDEFINED;
	public int orientationRequest = Configuration.ORIENTATION_UNDEFINED;
	public boolean orientationSetup_DONE = true;
	public int mainLayout_DONE = 0; // 0:NOT_LAYOUT/1:LAYOUT_DONE/2:LAYOUT_UPDATE
	public static WaxPlayService waxPlayService = null;
	public boolean activityRunning = true; // Init value must be true ?
	private GradientDrawable mainLayout_bg = null;
	private static final Object exitingLock = new Object();
	public RelativeLayout mainLayout = null;
	public boolean mDoodleHide = false;
	public Object mDoodleMenu = null;

	private static final String OSCA_DRM_ID = "请在这里填写版权保护id";
	private static final String OSCA_DRM_PUBLIC_KEY = "请在这里填写版权保护公钥";
	private static boolean OSCA_DRM_CHECKED = false;

	private void OSCA_AGC_DrmCheck() {
		OSCA_DRM_CHECKED = true;
	}

	static {
		for (int proto = 0; proto < WaxPlayer2.PROTOCOL_MAX; proto ++) {
			stopFromUser_Map[proto] = null;
			if ((proto&WaxPlayer2.PROTOCOL_ALL) == proto)
				stopFromUser_Map[proto] = new HashMap<String, Integer>();
		}
	}

	@Override
	protected void onPause() {
		Log.i(LOG_TAG, "MActivity onPause called");
		super.onPause();
		WaxPlayService.exitUpgrade(2/*from*/);
		WaxPlayService.exitNotice(2/*from*/);
		DoodleView_End(true, false);
		if (inBackground == 0) {
			if (WaxPlayService.exitOnActivityPause == 1) {
				if (Config.MIRROR_HWDEC != 0)
					WaxPlayService.displayToast2(5, false, getString(R.string.notice_hwdec_onpaused));
				if (isExiting() == false)
					EnterExit();
				gotoStop("*", true, 1, true, -1);
				DoRealFinish(true); // Avoid of exception while process MSG_DEL_PLAYER
				MPlayerLock.lock();
				for (int i = 0; i < MPlayerArray.length; i ++)
					MPlayerArray[i] = null;
				MPlayerLock.unlock();
			} else if (WaxPlayService.exitOnActivityPause == 0) {
				inBackground = 1; // Do set it before Stop() to let Play() get right result
				/*EnterExit();
				gotoStop("*", false, 1, false, -1);
				ClearExit();*/
			}
		}
	}

	@Override
	protected void onResume() {
		Log.i(LOG_TAG, "MActivity onResume called");
		super.onResume();
		if (activityRunning == false)
			return ;
		if (WaxPlayService.exitOnActivityPause == 0 && inBackground != 0) {
			inBackground = 0; // Do reset it before Play() to let Play()/UI get right state
			if (MPlayerCount <= 0/*Screen LOCKED -> onCreate -> onPause -> UNLOCK -> onResume*/) {
				DoRealFinish(true);
			} else {
				/*new Thread() {
					public void run() {
						MPlayerLock.lock();
						int i = 0;
						for (i = 0; i < MPlayerArray.length; i++) {
							if (MPlayerArray[i] != null && 
								MPlayerArray[i].isExiting() == false ) {
								if (MPlayerArray[i].mediaFormat != WaxPlayer2.FMT_PHOTO && MPlayerArray[i].mediaFormat != WaxPlayer2.FMT_UNKNOWN) {
									if (MPlayerArray[i].playback_State != WaxPlayer2.STATE_STOPPED) // Music gotoBackground() playing
										MPlayerArray[i].gotoStop(false, 1, false);
									if (MPlayerArray[i].playback_State == WaxPlayer2.STATE_STOPPED && MPlayerArray[i].mediaFormat != WaxPlayer2.FMT_PHOTO) { // Resume() on onResume()
										MPlayerArray[i].waxHandler.sendEmptyMessage(WaxPlayer2.MSG_SHOW_LOADING);
										if (MPlayerArray[i].Play(MPlayerArray[i].url, "", 0.0f, 0, MPlayerArray[i].contentSize, MPlayerArray[i].title, MPlayerArray[i].userAgent) == 1)
											waxPlayService.MediaResume(MPlayerArray[i].client_id);
									}
								}
							}
						}
						MPlayerLock.unlock();
					}
				}.start();*/
			}
		}
		OSCA_AGC_DrmCheck();
	}

	@Override
	protected void onDestroy() {
		Log.i(LOG_TAG, "MActivity onDestroy called");
		if (Config.TEXTUREVIEW_CHECKED == -1) {
			// A TextureView or a subclass can only be used with hardware acceleration enabled
			WaxPlayService._config.setTextureViewChecked(0);
			Activity ctx = WaxPlayer.this;
			try {
				Intent i = new Intent();
				i.setClassName(ctx.getPackageName(), WaxPlayerCheckTexture.class.getName());
				i.putExtras(ctx.getIntent());
				i.setData(ctx.getIntent().getData());
				i.putExtra("package", ctx.getPackageName());
				i.putExtra("className", ctx.getClass().getName());
				Global.startActivityWrap(ctx, i);
			} catch (Exception e) {
			}
		}
		WaxPlayService.mediaplayer = null; // Moved from DoRealFinish() logging "Finish DONE ..." here
		Global.letScreenOff();
		if (WaxPlayService.SYSTEM_MANUAL_ROTATE == true && Global.checkSU() == true)
			WaxPlayService.rcmd("setprop persist.sys.rotate 0", 1);
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "MActivity onCreate called");
		if (WaxPlayService.callingCreatePlayer == false) { // happens on FireTV
			EnterExit();
			if (WaxPlayService.exitOnActivityPause == 0)
				WaxPlayService.startSettingActivity(WaxPlayer.this);
			DoRealFinish(true);
			return ;
		}
		//Global.letScreenOn(this);
		//Global.letScreenUnlocked();
		inBackground = 0; /*Snoopy 8/25/2025 : RESET it here for onPause()*/
		orientationRequest = (Config.LANDSCAPE_MODE > 0) ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
		int orientVal = getResources().getConfiguration().orientation;
		if (Config.LANDSCAPE_MODE == 1 && orientVal != Configuration.ORIENTATION_LANDSCAPE) {
			orientationSetup_DONE = false;
			mainLayout_DONE = 0;
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // android:screenOrientation="landscape"
		} else if (Config.LANDSCAPE_MODE == 0 && orientVal != Configuration.ORIENTATION_PORTRAIT) {
			orientationSetup_DONE = false;
			mainLayout_DONE = 0;
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		mainHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (activityRunning == false) {
					super.handleMessage(msg);
					return ; // Avoid of pendding UI request
				}
				switch (msg.what) {
					case MSG_ADD_PLAYER: {
						boolean locked = MPlayerLock.tryLock();
						try {
							int index = SETUP_MPlayers(1, -1, (String)msg.obj);
							if (index >= 0 && index < MPlayerArray.length) {
								if (inBackground == 0) {
									LAYOUT_MPlayers();
									FragmentManager fgManager = getSupportFragmentManager();
									FragmentTransaction fragmentTransaction = fgManager.beginTransaction();
									fragmentTransaction.add(MPlayerResID[index], MPlayerArray[index]);
									Log.i(LOG_TAG,"MActivity ADD MPlayer["+(index+1)+"] = "+MPlayerResID[index]+","+MPlayerArray[index]);
									fragmentTransaction.commit();
								}
							}
						} catch (Exception e1) {
						} catch (Throwable e2) {
						}
						if (locked == true)
							MPlayerLock.unlock();
						Global.do_sleep(50); // Schedule Play()
						break;
					}
					case MSG_DEL_PLAYER: {
						boolean locked = MPlayerLock.tryLock();
						int index = -1;
						try {
							index = SETUP_MPlayers(-1, msg.arg1, (String)msg.obj);
							if (index >= 0 && index < MPlayerArray.length) {
								FragmentManager fgManager = getSupportFragmentManager();
								//WaxPlayer2 fgMp = (WaxPlayer2)fgManager.findFragmentById(MPlayerResID[index]);
								WaxPlayer2 fgMp = MPlayerArray[index];
								if (fgMp.inBackground == 0) {
									try { /* can't do it in onPaused() state */
										FragmentTransaction fragmentTransaction = fgManager.beginTransaction();
										fragmentTransaction.remove(fgMp);
										fragmentTransaction.commit();
									} catch (Exception e1) {
									}
									Log.i(LOG_TAG,"MActivity DEL MPlayer["+(index+1)+"] = "+MPlayerResID[index]+","+fgMp);
								}
							}
						} catch (Exception e1) {
						} catch (Throwable e2) {
						}
						try {
							if (index >= 0 && index < MPlayerArray.length) {
								MPlayerResID[index] = 0;
								MPlayerArray[index] = null;
								LAYOUT_MPlayers();
							}
						} catch (Exception e1) {
						} catch (Throwable e2) {
						}
						mainHandler.sendEmptyMessageDelayed(MSG_FINISH_ACTIVITY, 100);
						if (locked == true)
							MPlayerLock.unlock();
						Global.do_sleep(50); // Schedule Stop()
						break;
					}
					case MSG_RESORT_PLAYER: {
						boolean locked = MPlayerLock.tryLock();
						try {
							LAYOUT_MPlayers();
						} catch (Exception e1) {
						} catch (Throwable e2) {
						}
						if (locked == true)
							MPlayerLock.unlock();
						break;
					}
					case MSG_SWITCH2PREV_PLAYER: {
						onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
						break;
					}
					case MSG_SWITCH2NEXT_PLAYER: {
						onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
						break;
					}
					case MSG_FINISH_ACTIVITY: {
						boolean locked = MPlayerLock.tryLock();
						int i;
						for (i = 0; i < MPlayerArray.length; i++) {
							if (MPlayerArray[i] != null)
								break;
						}
						if (i == MPlayerArray.length) {
							EnterExit();
							DoRealFinish(true);
						}
						if (locked == true)
							MPlayerLock.unlock();
						break;
					}
					case MSG_ADD_DIALOG: {
						dialogCount ++;
						break;
					}
					case MSG_DEL_DIALOG: {
						if (dialogCount > 0)
							dialogCount --;
						if (dialogCount <= 0)
							enter_FULLSCREEN(WaxPlayer.this.getWindow());
						break;
					}
					case MSG_DLG_CONTINUE: {
						WaxPlayService.gotoUpgrade(WaxPlayer.this, 2/*from*/, WaxPlayService.dlgPriortyUpgrade);
						WaxPlayService.gotoNotice(WaxPlayer.this, 2/*from*/, WaxPlayService.dlgPriortyNotice);
						break;
					}
					case MSG_CHANGEPS_WHOLELAYOUT: {
						ChangeWholeLayoutWithAnimation(false);
						break;
					}
					case MSG_SHOW_WHOLELAYOUT: {
						ShowWholeLayoutWithAnimation();
						break;
					}
					case MSG_HIDE_WHOLELAYOUT: {
						HideWholeLayoutWithAnimation(1000);
						break;
					}
					case MSG_CHECK_VIDEOSIZE: {
						if (Config.LANDSCAPE_MODE == 3 && MPlayerCount == 1 && is_Stopped("*", -1) == false) {
							int width = GetVideoWidth("*");
							int height = GetVideoHeight("*");
							if (width > 0 && height > 0) {
								orientationRequest = (width > height) ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
								int orientVal = getResources().getConfiguration().orientation;
								if (orientationRequest != orientVal) {
									if (orientationRequest == Configuration.ORIENTATION_LANDSCAPE) {
										setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
										if (WaxPlayService.SYSTEM_MANUAL_ROTATE == true && Global.checkSU() == true)
											WaxPlayService.rcmd("setprop persist.sys.rotate 0", 1);
									} else if (orientationRequest == Configuration.ORIENTATION_PORTRAIT) {
										setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
										if (WaxPlayService.SYSTEM_MANUAL_ROTATE == true && Global.checkSU() == true)
											WaxPlayService.rcmd("setprop persist.sys.rotate 1", 1);
									}
								}
							}
							mainHandler.sendEmptyMessageDelayed(MSG_CHECK_VIDEOSIZE, 500);
						}
						break;
					}
					case MSG_VIDEOSIZE_CHANGED: {
						if (WaxPlayService.AUTO_LAYOUT_PLAYERS) {
							mainHandler.removeMessages(MSG_RESORT_PLAYER);
							mainHandler.sendEmptyMessage(MSG_RESORT_PLAYER);
						}
						break;
					}
					default:
						break;
				}
				super.handleMessage(msg);
			}
		};

		initUI();
		initAirDMRSerice();
		OSCA_AGC_DrmCheck();
	}

	private void initAirDMRSerice() {
		if (waxPlayService == null)
			return;
		else if (WaxPlayService.activityRestart == false) {
			// Loading dialog will use mediaplayer instance, set before MSG_SHOW_LOADING
			WaxPlayService.mediaplayer = WaxPlayer.this;
			//mainHandler.sendEmptyMessage(MSG_SHOW_LOADING); // Sent in Prepare()
			Log.i(LOG_TAG, "MActivity Init DONE ...");
		}
	}

	private void DoRealFinish(boolean doFinish) {
		Log.i(LOG_TAG, "MActivity Prepare Finish("+doFinish+")...");
		if (activityRunning == true) {
			activityRunning = false; // Avoid of double finish request
			if (doFinish == true) {
				if (WaxPlayService.ActivityFloatWindowMode == true)
					doFinish = HideWholeLayoutWithAnimation(1000);
				if (doFinish == true)
					finish();
			}
			if (SDNativeView.jniLoaded == true)
				SDNativeView.spq();
			WaxPlayService.send_MediaStop("*", WaxPlayer2.FMT_UNKNOWN, WaxPlayer2.PROTOCOL_ALL, 0, 1, 0);
			Log.i(LOG_TAG, "MActivity Finish DONE ...");
		}
		//WaxPlayService.activityRestart = false;
		WaxPlayService.vitamioChecking = false;
	}

	/*public void onSystemUiVisibilityChange(int paramInt) {
		if (paramInt != 8)
			getWindow().getDecorView().setSystemUiVisibility(8);
		Log.i(LOG_TAG, "MActivity onSystemUivisibitilychange = "+paramInt);
	}*/

	private void my_onScreenPosChanged(int left, int top, int right, int bottom) {
		FrameLayout.LayoutParams activityLayout = (FrameLayout.LayoutParams)mainLayout.getLayoutParams();
		activityLayout.setMargins(left, top, right, bottom);
		mainLayout.setLayoutParams(activityLayout);
	}

	private void my_onScreenSizeChanged(int width, int height, boolean done, boolean forceUpdate) {
		if (forceUpdate == true || 
			WaxPlayService.ActivityFloatWindowMode == false || (
			WaxPlayService.ActivityLayoutArray[0] == 0 || WaxPlayService.ActivityLayoutArray[1] == 0 ) ) {
			screenWidth = width - screenMargin*2; // Screen is delayed rotated after onCreate()
			screenHeight = height - screenMargin*2;
			int[] screenPos = new int[2];
			mainLayout.getLocationOnScreen(screenPos);
			screenXoff = screenPos[0];
			screenYoff = screenPos[1];
		}
		orientationSetup_DONE = done;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		int[] ssize = Global.getRealScreenSize(this, false);
		int newWidth = ssize[0];
		int newHeight = ssize[1];
		Log.i(LOG_TAG,"MActivity onDisplayChanged = "+newWidth+"x"+newHeight);
		if (newConfig.orientation == orientationRequest || mainLayout_DONE > 0)
			my_onScreenSizeChanged(newWidth, newHeight, true, false);
		if (newWidth != dispWidthSaved || newHeight != dispHeightSaved || orientationSaved != newConfig.orientation) {
			dispWidthSaved = newWidth;
			dispHeightSaved = newHeight;
			orientationSaved = newConfig.orientation;
			if (mainLayout_DONE > 0)
				mainLayout_DONE = 2;
		}
	}

	public void enter_FULLSCREEN2() {
		if (Global.ACTIVITY_FREEFORM == true || mainLayout == null)
			return;
		try	{
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
				mainLayout.getWindowInsetsController().hide(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());
		} catch (Exception e) {
		}
	}

	public void enter_FULLSCREEN(Window _window) {
		if (Global.ACTIVITY_FREEFORM == true)
			return;
		try	{
			if (android.os.Build.VERSION.SDK_INT >= 14) {
				//_window.getDecorView().setOnSystemUiVisibilityChangeListener(this);
				//_window.getDecorView().setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
				/*int hide_or_display = 0; // 0:Hide / 1:Show
				if (hide_or_display == 0) {
					// the below 2 lines are from RkVideoPlayer.apk in RK30@android 4.0.4
					_window.getDecorView().setSystemUiVisibility(4);
				} else if (hide_or_display == 1) { 
					// the below 3 lines are from RkVideoPlayer.apk in RK30@android 4.1.1
					_window.getDecorView().setSystemUiVisibility(8);
				}*/
				int dest_flag = 0;//_window.getDecorView().getSystemUiVisibility();
				int fullscreen_flag = 0;
				int fullscreen_flagEX = View.SYSTEM_UI_FLAG_FULLSCREEN;
				int fullscreen_flagEX2 = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
				dest_flag |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
				fullscreen_flagEX2 |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
				if (android.os.Build.VERSION.SDK_INT >= 21) {
					fullscreen_flag = fullscreen_flagEX | fullscreen_flagEX2 | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY/*Since Api Level 19*/;
				} else if (android.os.Build.VERSION.SDK_INT >= 16) {
					fullscreen_flag = fullscreen_flagEX | 0x00000008; // for android 4.1, SYSTEM_UI_FLAG_SHOW_FULLSCREEN is hidden in View.java
				} else {
					fullscreen_flag = 0x00000004; // for android 4.0
				}
				if (WaxPlayService.FullScreen_Mode_Advanced == true)
					dest_flag = dest_flag | fullscreen_flag;
				_window.getDecorView().setSystemUiVisibility(dest_flag);
				if (WaxPlayService.FullScreen_Mode_Advanced == true && _window == WaxPlayer.this.getWindow())
					_window.getDecorView().setBackgroundColor(-16777216);
			}
			if (WaxPlayService.ActivityFloatWindowMode == true && _window == WaxPlayer.this.getWindow()) // Should before setContentView()
				_window.setBackgroundDrawableResource(R.drawable.empty);
		} catch (Exception e1) {
		} catch (Throwable e2) {
		}
		enter_FULLSCREEN2();
	}

	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus)
			enter_FULLSCREEN(this.getWindow());
	}

	private void ShowWholeLayoutWithAnimation() {
		if (mainLayout.getVisibility() == View.VISIBLE)
			return ;
		Global.mySetAlpha(mainLayout, 1.0f);
	}

	private boolean HideWholeLayoutWithAnimation(int duration) {
		if (mainLayout.getVisibility() == View.INVISIBLE || mainLayout.getVisibility() == View.GONE)
			return true;
		overridePendingTransition(0, 0);
		return true;
	}

	public static void HideWholeLayoutSubview(View subview) {
		if (WaxPlayService.ActivityFloatWindowMode == true) {
			/*LayoutParams lp1 = subview.getLayoutParams();
			lp1.width = lp1.height = 1;
			subview.setLayoutParams(lp1);
			subview.requestLayout();
			subview.invalidate();*/
		} else {
			//subview.setVisibility(View.INVISIBLE);
		}
	}

	private void ChangeWholeLayoutWithAnimation(boolean initial) {
		if (WaxPlayService.ActivityFloatWindowMode == true) {
			if (initial == true) {
				//mainLayout.setVisibility(View.INVISIBLE);
				//Global.mySetAlpha(mainLayout, 0.0f);
				mainLayout_bg = new GradientDrawable();
				if (mainLayout_bg != null) {
					mainLayout_bg.setShape(GradientDrawable.RECTANGLE);
					mainLayout_bg.setCornerRadius(0);
					mainLayout_bg.setColor(Color.BLACK);
					mainLayout.setBackgroundDrawable(mainLayout_bg);
				}
			}
			if (WaxPlayService.ActivityLayoutArray[7] > 0 && WaxPlayService.ActivityLayoutArray[6] != 0) {
				int color = WaxPlayService.ActivityLayoutArray[6];
				int padding = WaxPlayService.ActivityLayoutArray[7];
				mainLayout.setPadding(padding, padding, padding, padding);
				if (mainLayout_bg != null) {
					mainLayout_bg.setStroke(padding, color);
					mainLayout.invalidate();
				} else {
					//mainLayout.setBackgroundResource(R.drawable.bg_green);
				}
			}
			if (WaxPlayService.ActivityLayoutArray[0] > 0 && WaxPlayService.ActivityLayoutArray[1] > 0) {
                mainLayout.getLayoutParams().width = WaxPlayService.ActivityLayoutArray[0];
				mainLayout.getLayoutParams().height = WaxPlayService.ActivityLayoutArray[1];
				mainLayout.requestLayout();
				my_onScreenPosChanged(WaxPlayService.ActivityLayoutArray[2], WaxPlayService.ActivityLayoutArray[3], WaxPlayService.ActivityLayoutArray[4], WaxPlayService.ActivityLayoutArray[5]);
				my_onScreenSizeChanged(WaxPlayService.ActivityLayoutArray[0], WaxPlayService.ActivityLayoutArray[1], orientationSetup_DONE, true);
				//if (is_Playing2("*", -1) == true)
				//	SETUP_MPlayers(0, -1, "");
			}
		}
	}

	public void send_changepsWholeLayout() {
		mainHandler.removeMessages(MSG_CHANGEPS_WHOLELAYOUT);
		mainHandler.sendEmptyMessage(MSG_CHANGEPS_WHOLELAYOUT);
	}

	public void send_showWholeLayout() {
		mainHandler.removeMessages(MSG_SHOW_WHOLELAYOUT);
		mainHandler.sendEmptyMessage(MSG_SHOW_WHOLELAYOUT);
	}

	public void send_hideWholeLayout() {
		mainHandler.removeMessages(MSG_HIDE_WHOLELAYOUT);
		mainHandler.sendEmptyMessage(MSG_HIDE_WHOLELAYOUT);
	}

	private void initUI() {
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (Global.ACTIVITY_FREEFORM == false) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			//getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else {
			setTheme(android.R.style.Theme_Translucent_NoTitleBar);
		}
		enter_FULLSCREEN(this.getWindow());
		try	{
			//screenMargin = (int)getResources().getDimension(R.dimen.scanover_margin);
		} catch (Exception e) {
		}

		try	{
			this.setContentView(R.layout.waxplayer_main_n1);
		} catch (Exception e) {
		}
		mainLayout = (RelativeLayout)this.findViewById(R.id.MainActivity);
		mainLayout.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() { // Called twice from PORTRAIT to LANDSCAPE
				if (mainLayout_DONE == 0 || mainLayout_DONE == 2) {
					Log.i(LOG_TAG,"MActivity onMainLayout = "+mainLayout.getWidth()+"x"+mainLayout.getHeight());
					if (mainLayout_DONE == 2)
						mainHandler.sendEmptyMessage(MSG_RESORT_PLAYER);
					if (orientationSetup_DONE == true)
						mainLayout_DONE = 1;
					//mainLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
			}
		});
		enter_FULLSCREEN2();
		int[] ssize = Global.getRealScreenSize(this, false);
		my_onScreenSizeChanged(ssize[0] - screenMargin*2, ssize[1] - screenMargin*2, orientationSetup_DONE, false);
		Log.i(LOG_TAG,"MActivity onDisplayStart = "+screenWidth+"x"+screenHeight+"[DIV="+WaxPlayService.MAX_PLAYERS_N1+"]");
		MPlayerArray = new WaxPlayer2[WaxPlayService.MAX_PLAYERS_N1];
		MPlayerRatioArray = new float[WaxPlayService.MAX_PLAYERS_N1];
		MPlayerWidthArray = new int[WaxPlayService.MAX_PLAYERS_N1];
		MPlayerHeightArray = new int[WaxPlayService.MAX_PLAYERS_N1];
		int i;
		for (i = 0; i < WaxPlayService.MAX_PLAYERS_N1; i ++) {
			MPlayerArray[i] = null;
			MPlayerWidthArray[i] = MPlayerHeightArray[i] = 0;
			MPlayerRatioArray[i] = 0.0f;
		}

		//if (WaxPlayer2.SDNativeView.jniLoaded == false)
		//	WaxPlayer2.sDNativeViewEnabled = false;
		if (WaxPlayService.cpu_Type == Global.CPU_ARM && WaxPlayService.cpuFeature < WaxPlayService.minCpuMIRROR)
			WaxPlayer2.sDNativeViewEnabled = false;
		//if (WaxPlayService.cpu_Type == Global.CPU_ARM && WaxPlayService.cpuFeature >= WaxPlayService.minCpuVITAMIO) {
			if (Config.VITAMIO_INIT != 0 && WaxPlayer2.sDVideoViewEnabled == false) {
				try {
					Log.i(LOG_TAG, "MActivity VITAMIO Checking ...");
					WaxPlayService.vitamioChecking = true;
					/*if (WaxPlayService.cpu_Type != Global.CPU_ARM)
						WaxPlayService._config.setVitamioInit(0); // For crashing on MIPS/x86*/
					long _start = System.currentTimeMillis();
					//WaxPlayer2.sDVideoViewEnabled = io.vov.vitamio.LibsChecker.checkVitamioLibs(this);//, 
					tv.danmaku.ijk.media.player.IjkMediaPlayer mMediaPlayer = new tv.danmaku.ijk.media.player.IjkMediaPlayer();
					WaxPlayer2.sDVideoViewEnabled = tv.danmaku.ijk.media.player.IjkMediaPlayer.mIsNativeInitialized && 
										tv.danmaku.ijk.media.player.IjkMediaPlayer.mIsLibLoaded;
						//"com.waxrain.ui.WaxPlayer" , // Vitamio 1.0
						//R.string.waxplayer_boot_sdvideoview, R.raw.libarm);
					long _epalsed = System.currentTimeMillis() - _start;
					Log.i(LOG_TAG, "MActivity VITAMIO Check return " + WaxPlayer2.sDVideoViewEnabled + " in " + _epalsed + " msec");
					/*if (WaxPlayer2.sDVideoViewEnabled == false) {
						// Activity will be restart after extracting vitamio libs
						WaxPlayService.activityRestart = true;
						return ;
					}
					if (WaxPlayService.cpu_Type != Global.CPU_ARM)
						WaxPlayService._config.setVitamioInit(1); // For crashing on MIPS/x86*/
				} catch (NoClassDefFoundError e) {
					WaxPlayer2.sDVideoViewEnabled = false;
				} catch (UnsatisfiedLinkError e) {
					WaxPlayer2.sDVideoViewEnabled = false;
				} catch (Exception e) {
					WaxPlayer2.sDVideoViewEnabled = false;
				}
				if (WaxPlayer2.sDVideoViewEnabled == false)
					WaxPlayService._config.setVitamioInit(0);
			}
		//} else if (Config.VITAMIO_INIT == 0) {
		//	WaxPlayer2.sDVideoViewEnabled = false;
		//}
		if (Config.TEXTUREVIEW_CHECKED == -1) {
			// It looks like startActivity() is in sync mode on Nexus Player
			WaxPlayService.activityRestart = true;
			WaxPlayer.this.finish();
			return ;
		}
		WaxPlayService.activityRestart = false;
		WaxPlayService.vitamioChecking = false;
		if (WaxPlayService.ActivityFloatWindowMode == true) {
			ChangeWholeLayoutWithAnimation(true);
		}
	}

	@Override
	public void onClick(View v) {
		//Log.i(LOG_TAG, "MActivity onClick called");
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (MPlayerFocused < 0 && MPlayerZoomed < 0)
			return true;
		boolean retVal = false;
		boolean should_process = false;
		boolean locked = MPlayerLock.tryLock();
		int id = MPlayerIdResort[MPlayerFocused];
		try {
			if (MPlayerCount == 1) {
				should_process = true;
			} else if (MPlayerZoomed >= 0 && MPlayerZoomed < MPlayerCount) {
				id = MPlayerIdResort[MPlayerZoomed];
				should_process = true;
			}
			if (should_process && MPlayerArray[id] != null)
				retVal = MPlayerArray[id].onGenericMotionEvent(event);
		} catch (Exception ex) {
			retVal = true;
		}
		if (locked == true)
			MPlayerLock.unlock();
		return retVal ? true : super.onGenericMotionEvent(event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (WaxPlayer2.debug_InputEvent == true)
			Log.i(LOG_TAG, "MActivity onKeyDown called = " + keyCode);
		DropNext_KeyCode = -1;
		if (inBackground != 0)
			return super.onKeyDown(keyCode, event);
		if (keyCode == KeyEvent.KEYCODE_HOME) { // Useless
			//WaxPlayService.exitOnActivityPause = 1;
			return super.onKeyDown(keyCode, event);
		}
		if (MPlayerFocused < 0)
			return true;
		if (MPlayerLock.tryLock() == false) // in Prepare()
			return true;
		boolean retVal = false;
		int id = MPlayerIdResort[MPlayerFocused];
		if (MPlayerCount <= 0/*LOADING*/) {
			retVal = true;
		} else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			if (MPlayerArray[id] != null && MPlayerCount > 1) {
				if (MPlayerArray[id].STATUS_CLAYOUT_HIDE == 1 && MPlayerZoomed < 0) {
					FULLSCREEN_MPlayer(true, false);
					DropNext_KeyCode = keyCode;
					retVal = true;
				} else if (MPlayerArray[id].STATUS_CLAYOUT_HIDE == 1 && MPlayerFocused == MPlayerZoomed) {
					FULLSCREEN_MPlayer(false, false);
					DropNext_KeyCode = keyCode;
					retVal = true;
				}
			}
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			if (MPlayerArray[id] != null && MPlayerCount > 1) {
				if (MPlayerArray[id].STATUS_CLAYOUT_HIDE == 1 && MPlayerZoomed < 0) {
					MPlayerFocused --;
					if (MPlayerFocused < 0)
						MPlayerFocused = MPlayerCount - 1;
					DropNext_KeyCode = keyCode;
					SWITCHTO_MPlayer(MPlayerIdResort[MPlayerFocused], false);
					retVal = true;
				}
			}
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			if (MPlayerArray[id] != null && MPlayerCount > 1) {
				if (MPlayerArray[id].STATUS_CLAYOUT_HIDE == 1 && MPlayerZoomed < 0) {
					MPlayerFocused ++;
					MPlayerFocused %= MPlayerCount;
					DropNext_KeyCode = keyCode;
					SWITCHTO_MPlayer(MPlayerIdResort[MPlayerFocused], false);
					retVal = true;
				}
			}
		}
		if (retVal == false) {
			if (MPlayerArray[id] != null)
				retVal = MPlayerArray[id].onKeyDown(keyCode, event);
		}
		MPlayerLock.unlock();
		return retVal;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (WaxPlayer2.debug_InputEvent == true)
			Log.i(LOG_TAG, "MActivity onKeyUp called = " + keyCode);
		if (inBackground != 0)
			return super.onKeyUp(keyCode, event);
		if (MPlayerFocused < 0)
			return true;
		if (MPlayerLock.tryLock() == false) // in Prepare()
			return true;
		boolean retVal = false;
		int id = MPlayerIdResort[MPlayerFocused];
		if (MPlayerCount <= 0) // LOADING
			retVal = true;
		else if (keyCode == DropNext_KeyCode)
			retVal = true;
		if (retVal == false) {
			if (MPlayerArray[id] != null)
				retVal = MPlayerArray[id].onKeyUp(keyCode, event);
		}
		MPlayerLock.unlock();
		return retVal;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		//Log.i(LOG_TAG, "MActivity onTouch called = " + event.getAction() + "@" + (int)event.getRawX() + "," + (int)event.getRawY());
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//Log.i(LOG_TAG, "MActivity onTouchEvent called");
		return false;
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		// TODO Auto-generated method stub
	}

	private void ChangeMPlayerLayoutWithAnimation(FrameLayout mplayerLayout, int x, int y, int w, int h, int duration) {
		FrameLayout.MarginLayoutParams mplayerLayoutParam = (FrameLayout.MarginLayoutParams)mplayerLayout.getLayoutParams();
		mplayerLayoutParam.leftMargin = x;
		mplayerLayoutParam.topMargin = y;
		mplayerLayoutParam.width = w;
		mplayerLayoutParam.height = h;
		mplayerLayout.setLayoutParams(mplayerLayoutParam);
		mplayerLayout.requestLayout();
		if ((x < 0 || y < 0) && mplayerLayout.getVisibility() == View.VISIBLE) {
			mplayerLayout.setVisibility(View.INVISIBLE);
		} else if ((x >= 0 && y >= 0) && mplayerLayout.getVisibility() == View.INVISIBLE) {
			mplayerLayout.setVisibility(View.VISIBLE);
			//mplayerLayout.bringToFront();
		}
	}

	public static int myGenerateViewId() {
		if (android.os.Build.VERSION.SDK_INT < 17) {
			for (;;) {
				final int result = sNextGeneratedId2.get();
				// aapt-generated IDs have the high byte nonzero; clamp to the range under that.
				int newValue = result + 1;
				if (newValue > 0x00FFFFFF)
					newValue = 1; // Roll over to 1, not 0.
				if (sNextGeneratedId2.compareAndSet(result, newValue))
					return result;
			}
		} else {
			return View.generateViewId();
		}
    }

	public int CHECKZOOM_MPlayer(int fragment_id, boolean doLock) {
		int zoom_state = 1;
		if (doLock == true)
			MPlayerLock.lock();
		if (MPlayerCount > 1) {
			if (MPlayerZoomed >= 0) {
				int id = MPlayerIdResort[MPlayerZoomed];
				if (MPlayerArray[id] != null && id == fragment_id)
					zoom_state = 11;
			} else {
				zoom_state = 10;
			}
		} else {
			int id = MPlayerIdResort[MPlayerFocused];
			if (MPlayerArray[id] != null && id == fragment_id)
				zoom_state = 1;
		}
		if (doLock == true)
			MPlayerLock.unlock();
		return zoom_state;
	}

	public void FULLSCREEN_MPlayer(boolean state, boolean doLock) {
		if (doLock == true)
			MPlayerLock.lock();
		if (state == true)
			MPlayerZoomed = MPlayerFocused;
		else
			MPlayerZoomed = -1;
		LAYOUT_MPlayers();
		if (doLock == true)
			MPlayerLock.unlock();
	}

	public void SWITCHTO_MPlayer(int fragment_id, boolean doLock) {
		if (doLock == true)
			MPlayerLock.lock();
		int i;
		if (fragment_id >= 0) { // MOVE
			for (i = 0; i < MPlayerCount; i ++) {
				if (MPlayerIdResort[i] == fragment_id) {
					MPlayerFocused = i;
					break;
				}
			}
		} else { // CHECK Focused Widget
			if (MPlayerFocused <= 0 || MPlayerFocused >= MPlayerCount)
				MPlayerFocused = 0;
			else if (MPlayerArray[MPlayerIdResort[MPlayerFocused]] == null)
				MPlayerFocused = 0;
		}
		if (MPlayerZoomed >= 0) { // CHECK Zoomed Widget
			if (MPlayerCount <= 1) {
				MPlayerZoomed = -1;
			} else {
				int id = MPlayerIdResort[MPlayerZoomed];
				if (MPlayerArray[id] == null)
					MPlayerZoomed = -1;
			}
		}
		Log.i(LOG_TAG,"MActivity FOCUS = "+MPlayerFocused+", ZOOM = "+MPlayerZoomed);
		for (i = 0; i < MPlayerCount; i ++) {
			int id = MPlayerIdResort[i];
			if (i == MPlayerFocused) {
				if (MPlayerArray[id] != null) {
					boolean highlight = true;
					if (MPlayerCount <= 1)
						highlight = false;
					if (i == MPlayerZoomed)
						highlight = false;
					MPlayerArray[id].gotFocus2(highlight);
					MPlayerArray[id].frame_layout.requestFocus();
				}
			} else {
				if (MPlayerArray[id] != null)
					MPlayerArray[id].lostFocus2();
			}
			if (i == MPlayerZoomed) {
				if (MPlayerArray[id] != null)
					MPlayerArray[id].gotZoom2();
			} else {
				MPlayerArray[id].lostZoom2();
			}
		}
		if (doLock == true)
			MPlayerLock.unlock();
	}

	private void RESORT_MPlayers() {
		int i = 0, j = 0;
		MPlayerCount = 0;
		for (i = 0; i < MPlayerArray.length; i++) {
			if (MPlayerArray[i] != null)
				MPlayerIdResort[MPlayerCount++] = i;
		}
		for (i = 0; i < MPlayerCount; i++) {
			for (j = i; j < MPlayerCount; j ++) {
				if (MPlayerArray[MPlayerIdResort[i]].player_startTs > MPlayerArray[MPlayerIdResort[j]].player_startTs) {
					int tmp = MPlayerIdResort[i];
					MPlayerIdResort[i] = MPlayerIdResort[j];
					MPlayerIdResort[j] = tmp;
				}
			}
		}
	}

	private boolean AUTO_LAYOUT_MPLayers_by_width(boolean is_Portrait) /*MUST after RESORT_MPlayers()*/ {
		int i = 0, j = 0;
		int MPlayerRatioCount = 0;
		boolean do_auto_layout = false;
		for (i = 0; i < MPlayerArray.length; i++) {
			MPlayerWidthArray[i] = MPlayerHeightArray[i] = 0;
			MPlayerRatioArray[i] = 0.0f;
			if (i >= MPlayerCount)
				continue;
			j = MPlayerIdResort[i];
			if (MPlayerArray[j] != null) {
				if (MPlayerArray[j].is_Playing2() && MPlayerArray[j].GetVideoWidth() > 0 && MPlayerArray[j].GetVideoHeight() > 0) {
					MPlayerWidthArray[i] = MPlayerArray[j].GetVideoWidth();
					MPlayerHeightArray[i] = MPlayerArray[j].GetVideoHeight();
					MPlayerRatioArray[i] = (float)MPlayerWidthArray[i]/(float)MPlayerHeightArray[i];
					MPlayerRatioCount ++;
				}
			}
		}
		if (MPlayerCount != MPlayerRatioCount)
			return false;
		if (MPlayerCount == 4 && MPlayerRatioArray != null) {
			if (MPlayerRatioArray[0] > 0.0f && MPlayerRatioArray[0] < VideoRatioPhone && 
				MPlayerRatioArray[1] > 0.0f && MPlayerRatioArray[1] < VideoRatioPhone && 
				MPlayerRatioArray[2] > 0.0f && MPlayerRatioArray[2] < VideoRatioPhone && 
				MPlayerRatioArray[3] > 0.0f && MPlayerRatioArray[3] < VideoRatioPhone )
				do_auto_layout = true;
		} else if (MPlayerCount == 3 && MPlayerRatioArray != null) {
			do_auto_layout = true;
		} else if (MPlayerCount == 2 && MPlayerRatioArray != null) {
			do_auto_layout = true;
		}
		if (do_auto_layout) {
			int off = 0;
			int cur_size = 0, total_size = 0;
			for (i = 0; i < MPlayerArray.length; i ++)
				total_size += ( (is_Portrait == false) ? MPlayerWidthArray[i] : MPlayerHeightArray[i] );
			for (i = 0; i < MPlayerArray.length; i ++) {
				if (i < MPlayerCount) {
					if (is_Portrait == false) {
						cur_size = (MPlayerWidthArray[i]*mainLayout.getWidth())/total_size;
						MPlayerLayoutX[i] = off;
						MPlayerLayoutY[i] = 0;
						MPlayerLayoutW[i] = cur_size;
						MPlayerLayoutH[i] = mainLayout.getHeight();
					} else {
						cur_size = (MPlayerHeightArray[i]*mainLayout.getHeight())/total_size;
						MPlayerLayoutX[i] = 0;
						MPlayerLayoutY[i] = off;
						MPlayerLayoutW[i] = mainLayout.getWidth();
						MPlayerLayoutH[i] = cur_size;
					}
					off += cur_size;
				} else {
					MPlayerLayoutX[i] = 0;
					MPlayerLayoutY[i] = 0;
					MPlayerLayoutW[i] = 0;
					MPlayerLayoutH[i] = 0;
				}
			}
		}
		return do_auto_layout;
	}

	private void LAYOUT_MPlayers() {
		RESORT_MPlayers();
		SWITCHTO_MPlayer(-1, false); // MUST AFTER RESORT_MPlayers()
		int i = 0, j = 0;
		boolean do_std_layout = true;
		boolean is_Portrait = (mainLayout.getHeight() > mainLayout.getWidth()) ? true : false;
		if (MPlayerCount > 1 && WaxPlayService.AUTO_LAYOUT_PLAYERS && MPlayerRatioArray != null)
			do_std_layout = !AUTO_LAYOUT_MPLayers_by_width(is_Portrait);
		if (MPlayerCount == 1) {
			for (i = 0; i < MPlayerArray.length; i ++) {
				if (i < MPlayerCount) {
					MPlayerLayoutX[i] = 0;
					MPlayerLayoutY[i] = 0;
					MPlayerLayoutW[i] = mainLayout.getWidth();
					MPlayerLayoutH[i] = mainLayout.getHeight();
				} else {
					MPlayerLayoutX[i] = 0;
					MPlayerLayoutY[i] = 0;
					MPlayerLayoutW[i] = 0;
					MPlayerLayoutH[i] = 0;
				}
			}
		} else if (MPlayerCount == 2 && do_std_layout) {
			for (i = 0, j = 0; i < MPlayerArray.length; i ++) {
				if (i < MPlayerCount) {
					if (is_Portrait == false) {
						MPlayerLayoutX[i] = j*mainLayout.getWidth()/2;
						MPlayerLayoutY[i] = 0;
						MPlayerLayoutW[i] = mainLayout.getWidth()/2;
						MPlayerLayoutH[i] = mainLayout.getHeight();
					} else {
						MPlayerLayoutX[i] = 0;
						MPlayerLayoutY[i] = j*mainLayout.getHeight()/2;
						MPlayerLayoutW[i] = mainLayout.getWidth();
						MPlayerLayoutH[i] = mainLayout.getHeight()/2;
					}
					j ++;
				} else {
					MPlayerLayoutX[i] = 0;
					MPlayerLayoutY[i] = 0;
					MPlayerLayoutW[i] = 0;
					MPlayerLayoutH[i] = 0;
				}
			}
		} else if (MPlayerCount == 3 && do_std_layout) {
			for (i = 0, j = 0; i < MPlayerArray.length; i ++) {
				if (i < MPlayerCount) {
					if (is_Portrait == false) {
						MPlayerLayoutX[i] = j*mainLayout.getWidth()/3;
						MPlayerLayoutY[i] = 0;
						MPlayerLayoutW[i] = mainLayout.getWidth()/3;
						MPlayerLayoutH[i] = mainLayout.getHeight();
					} else {
						MPlayerLayoutX[i] = 0;
						MPlayerLayoutY[i] = j*mainLayout.getHeight()/3;
						MPlayerLayoutW[i] = mainLayout.getWidth();
						MPlayerLayoutH[i] = mainLayout.getHeight()/3;
					}
					j ++;
				} else {
					MPlayerLayoutX[i] = 0;
					MPlayerLayoutY[i] = 0;
					MPlayerLayoutW[i] = 0;
					MPlayerLayoutH[i] = 0;
				}
			}
		} else if (MPlayerCount == 4 && do_std_layout) {
			for (i = 0, j = 0; i < MPlayerArray.length; i ++) {
				if (i < MPlayerCount) {
					MPlayerLayoutX[i] = (j%2)*mainLayout.getWidth()/2;
					MPlayerLayoutY[i] = (j/2)*mainLayout.getHeight()/2;
					MPlayerLayoutW[i] = mainLayout.getWidth()/2;
					MPlayerLayoutH[i] = mainLayout.getHeight()/2;
					j ++;
				} else {
					MPlayerLayoutX[i] = 0;
					MPlayerLayoutY[i] = 0;
					MPlayerLayoutW[i] = 0;
					MPlayerLayoutH[i] = 0;
				}
			}
		} else {
			// TODO : Handle more players
		}
		if (MPlayerZoomed >= 0 && MPlayerZoomed < MPlayerCount) {
			for (i = 0; i < MPlayerArray.length; i ++) {
				if (i == MPlayerZoomed) {
					MPlayerLayoutX[i] = 0;
					MPlayerLayoutY[i] = 0;
					MPlayerLayoutW[i] = mainLayout.getWidth();
					MPlayerLayoutH[i] = mainLayout.getHeight();
				} else if (i < MPlayerCount) {
					MPlayerLayoutX[i] -= mainLayout.getWidth();
					MPlayerLayoutY[i] -= mainLayout.getHeight();
				}
			}
		}
		for (i = 0; i < MPlayerCount; i ++) {
			j = MPlayerIdResort[i];
			if (MPlayerArray[j] != null) {
				Log.i(LOG_TAG,"MActivity LAYOUT MPlayer["+MPlayerArray[j].fragment_id+"] = "+MPlayerLayoutX[i]+","+MPlayerLayoutY[i]+"|"+MPlayerLayoutW[i]+"x"+MPlayerLayoutH[i]);
				ChangeMPlayerLayoutWithAnimation(MPlayerArray[j].frame_layout, MPlayerLayoutX[i], MPlayerLayoutY[i], MPlayerLayoutW[i], MPlayerLayoutH[i], 500);
				if (MPlayerCount <= 1) { // Force closing WaxPlayerPattern dialog in child
					if (MPlayerArray[j].dialogScale != null) 
						MPlayerArray[j].closeSubDialogs(0);
				}
			}
		}
		if (DoodleView_ShouldShow(-1) && mDoodleMenu == null) {
			try {
				org.xmlpull.v1.XmlPullParser parser = getResources().getXml(R.layout.waxplayer_main_doodle);
				AttributeSet attrs = android.util.Xml.asAttributeSet(parser);
				mDoodleMenu = (Object)new com.waxrain.droidsender.doodle.DoodleMenu(WaxPlayer.this, attrs);
				if (mainLayout.indexOfChild((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu) < 0) {
					RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
					lp.addRule(RelativeLayout.ALIGN_PARENT_TOP|RelativeLayout.ALIGN_PARENT_RIGHT);
					mainLayout.addView((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu, lp);
				}
				Global.mySetAlpha((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu, 0.8f);
			} catch (Exception e) {
				Log.i(LOG_TAG,"DoodleInit Error = "+e.toString());
			}
		}
		if (mDoodleMenu != null) {
			try {
				if (mDoodleHide == true) {
					((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu).RestoreAllMenus();
					mDoodleHide = false;
				}
				mainLayout.bringChildToFront((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu);
				((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu).bringToFront();
				if (DoodleView_ShouldShow(-1)) {
					if (((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu).getVisibility() != View.VISIBLE)
						((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu).setVisibility(View.VISIBLE);
					if (MPlayerCount == 1)
						MPlayerArray[0].DoodleView_Enter();
					else if (MPlayerZoomed >= 0 && MPlayerZoomed < MPlayerCount)
						MPlayerArray[MPlayerIdResort[MPlayerZoomed]].DoodleView_Enter();
				} else {
					if (((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu).getVisibility() == View.VISIBLE) {
						((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu).setVisibility(View.INVISIBLE);
						DoodleView_End(true, false);
					}
				}
			} catch (Exception e) {
			}
		}
	}

	private int SETUP_MPlayers(int action, int fragmentId, String clientId) {
		int retIndex = -1;
		int i = 0, j = 0;
		if (action == 1) { // ADD
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] == null) {
					retIndex = i;
					break;
				}
			}
			if (retIndex >= 0 && retIndex < MPlayerArray.length) {
				FrameLayout mplayerLayout = new FrameLayout(this);
				mplayerLayout.setId(myGenerateViewId());
				mainLayout.addView(mplayerLayout);
				MPlayerArray[retIndex] = new WaxPlayer2(this, retIndex+1, clientId, mplayerLayout);
				MPlayerResID[retIndex] = mplayerLayout.getId();
			}
		} else if (action == -1) { // REMOVE
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && MPlayerArray[i].fragment_id == fragmentId && MPlayerArray[i].frame_layout != null) {
					if (mainLayout.indexOfChild(MPlayerArray[i].frame_layout) >= 0) {
						HideWholeLayoutSubview(MPlayerArray[i].frame_layout);
						mainLayout.removeView(MPlayerArray[i].frame_layout);
					}
					retIndex = i;
					break;
				}
			}
		}
		return retIndex;
	}

	public boolean DoodleView_ShouldShow(int fragmentId) {
		if (Config.DOODLE_VIEW == 0)
			return false;
		boolean should_show = false;
		boolean locked = MPlayerLock.tryLock();
		try {
			if (MPlayerCount == 1) {
				should_show = true;
			} else if (MPlayerZoomed >= 0 && MPlayerZoomed < MPlayerCount && MPlayerArray[MPlayerIdResort[MPlayerZoomed]].mediaFormat != WaxPlayer2.FMT_AUDIO && MPlayerArray[MPlayerIdResort[MPlayerZoomed]].mediaFormat != WaxPlayer2.FMT_DOC) {
				if (fragmentId < 0)
					should_show = true;
				else if (MPlayerArray[MPlayerIdResort[MPlayerZoomed]].fragment_id == fragmentId)
					should_show = true;
			}
		} catch (Exception ex) {
		}
		if (locked == true)
			MPlayerLock.unlock();
		return should_show;
	}

	public void DoodleView_End(boolean clear, boolean reset) {
		if (mDoodleMenu != null) {
			Global.DoodleViewControl(WaxPlayer.this, "markend"+",");
			try {
				if (clear == true) {
					if (((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu).isDoodleMenuShown())
						((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu).onClick(
							((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu).btnDraw );
					else
						((com.waxrain.droidsender.doodle.DoodleMenu)mDoodleMenu).clearMenu();
				}
			} catch (Exception ex) {
			}
			if (reset == true)
				mDoodleMenu = null;
		}
	}

	private int waitMPlayerFinished(int index, boolean locked) {
		int retVal = 1;
		int timeout = 30000;
		if (locked == true)
			MPlayerLock.unlock();
		try	{
			while (MPlayerArray[index] != null && MPlayerArray[index].isExiting() == true && MPlayerArray[index].fragmentRunning == true && timeout > 0) {
				Global.do_sleep(50); // Schedule WaxPlayer2.finish() --> MSG_DEL_PLAYER
				timeout -= 50;
			}
			if (MPlayerArray[index] != null && MPlayerArray[index].fragmentRunning == true && timeout > 0) {
				retVal = 0; // ClearExit() and stay Stopped
			} else {
				//MPlayerArray[index] = null; // timeout == 0
				retVal = 1;
			}
		} catch (Exception ex) {
		} catch (Throwable th) {
		}
		if (locked == true)
			MPlayerLock.lock();
		return retVal;
	}

	public int mySendMessage(String clientId, int msgId, int delay) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			MPlayerLock.lock();
			int i = 0;
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false && (
					MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
					Message msg = new Message();
					msg.what = msgId;
					MPlayerArray[i].waxHandler.sendMessageDelayed(msg, delay);
					retVal ++;
					if (execAll == 0)
						break;
				}
			}
			MPlayerLock.unlock();
		}
		return retVal;
	}

	public int Prepare(String clientId, int start, int newfmt, int newproto) {
		int timeout = 3000;
		while ((orientationSetup_DONE == false || mainLayout_DONE == 0) && timeout > 0) {
			Global.do_sleep(50);
			timeout -= 50;
		}
		if (clientId == null || clientId.length() == 0) {
			int randnum = _rand.nextInt(253);
			clientId = "192.168.43." + Integer.toHexString(randnum);
		}
		int retVal = 0;
		int loadingMsg = -1;
		int msgDelay = 0;
		WaxPlayer2 mediaplayer = null;
		MPlayerLock.lock();
		int mPlayerIndex = MPlayerArray.length;
		int i = 0;
		if (false && start == 1 && newfmt == WaxPlayer2.FMT_MIRROR4) { // kill others
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].is_Stopped() == false ) {
					if (mediaplayer == null) {
						mediaplayer = MPlayerArray[i];
						mPlayerIndex = i;
						continue;
					}
					MPlayerArray[i].EnterExit();
					put_stopFromUser(MPlayerArray[i].client_id, MPlayerArray[i].mediaProto, 1);
					MPlayerArray[i].gotoStop(true, 0, true);
				}
			}
		}
		if (mediaplayer == null) { // replace existing one
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && (
					MPlayerArray[i].client_id.equals(clientId) /*|| MPlayerArray[i].mediaFormat == WaxPlayer2.FMT_MIRROR4*/) ) {
					mediaplayer = MPlayerArray[i];
					mPlayerIndex = i;
					break;
				}
			}
		}
		if (mediaplayer == null && i == MPlayerArray.length) { // find an empty one
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] == null) {
					mPlayerIndex = i;
					break;
				}
			}
		}
		if (mediaplayer == null && i == MPlayerArray.length) { // kick off earlist one
			long tmp_startTs = System.currentTimeMillis();
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null) {
					if (MPlayerArray[i].isLocked2 == true)
						continue;
					if (tmp_startTs > MPlayerArray[i].player_startTs) {
						mediaplayer = MPlayerArray[i];
						mPlayerIndex = i;
						tmp_startTs = MPlayerArray[i].player_startTs;
					}
				}
			}
			if (mediaplayer != null) {
				if (waxPlayService != null)
					waxPlayService.StopClientByProto(mediaplayer.client_id, WaxPlayer2.PROTOCOL_AIRPLAY|WaxPlayer2.PROTOCOL_DLNA, true);
				mediaplayer.player_startTs = System.currentTimeMillis();
				mainHandler.sendEmptyMessage(MSG_SWITCH2PREV_PLAYER);
				mainHandler.sendEmptyMessage(MSG_RESORT_PLAYER);
			}
		}
		try {
			if (start == 1) {
				if (mediaplayer != null && mediaplayer.isExiting() == false) {
					mediaplayer.waxHandler.removeMessages(WaxPlayer2.MSG_PLAY_DONE);
					mediaplayer.waxHandler.removeMessages(WaxPlayer2.MSG_GOTO_FINISH);
					mediaplayer.waxHandler.removeMessages(WaxPlayer2.MSG_DELAY_EXIT);
					if (mediaplayer.is_Stopped() == false) {
						mediaplayer.EnterExit(); // Clear flag below
						put_stopFromUser(mediaplayer.client_id, mediaplayer.mediaProto, 1); // update it
						if (newfmt == WaxPlayer2.FMT_PHOTO && mediaplayer.mediaFormat == WaxPlayer2.FMT_PHOTO) {
							mediaplayer.gotoStop(false, 0, true);
							loadingMsg = WaxPlayer2.MSG_SHOW_LOADING2;
							msgDelay = WaxPlayService.PHOTO_LOADING_ENDURE;
						} else {
							mediaplayer.gotoStop(false, 1, true); // Video/Audio
						}
						// onPause() called if another Activity goes foreground
						//if (mediaplayer != null)
						//	mediaplayer.gotoFinish(0);
						if (mediaplayer != null) {
							mediaplayer.ClearExit(); // Clear upper EnterExit()
						} else {
							// Stop() fail then finish(), recreate WaxPlayer2
						}
					} else {
						//mediaplayer.gotoFinish(1);
						// The below is called twice at Prepare2() then Prepare()
						if (newfmt == WaxPlayer2.FMT_PHOTO && mediaplayer.mediaFormat == WaxPlayer2.FMT_PHOTO) {
							//loadingMsg = WaxPlayer2.MSG_SHOW_LOADING2;
							//msgDelay = PHOTO_LOADING_ENDURE;
						}
					}
					if (WaxPlayService.PLAY_SWITCH_DELAY > 0)
						Global.do_sleep(WaxPlayService.PLAY_SWITCH_DELAY);
				} else if (mediaplayer != null && mediaplayer.isExiting() == true) {
					if (waitMPlayerFinished(mPlayerIndex, true) == 0) {
						Log.i(LOG_TAG, "Android ... MPlayer Prepare Obtained for " + clientId);
						//return 1; // MSG_PLAY_DONE
						if (newfmt == WaxPlayer2.FMT_PHOTO && mediaplayer.mediaFormat == WaxPlayer2.FMT_PHOTO) {
							loadingMsg = WaxPlayer2.MSG_SHOW_LOADING2;
							msgDelay = WaxPlayService.PHOTO_LOADING_ENDURE;
						}
					} else {
						mediaplayer = null;
					}
				}
				if (mediaplayer == null && mPlayerIndex < MPlayerArray.length) {
					newPlayer = null;
					Message msgInform = Message.obtain();
					msgInform.what = MSG_ADD_PLAYER;
					msgInform.obj = clientId;
					mainHandler.sendMessage(msgInform);
					timeout = WaxPlayService.WAIT_PLAY_TIMEOUT;
					while (newPlayer == null && timeout > 0) {
						Global.do_sleep(10);
						timeout -= 10;
					}
					mediaplayer = newPlayer;
					loadingMsg = WaxPlayer2.MSG_SHOW_LOADING;
				}
				if (mediaplayer != null) {
					mediaplayer.mediaFormat = newfmt;
					mediaplayer.mediaProto = newproto;
					mediaplayer.client_id = clientId;
					if (newfmt == WaxPlayer2.FMT_MIRROR || newfmt == WaxPlayer2.FMT_MIRROR2 || newfmt == WaxPlayer2.FMT_MIRROR3 || newfmt == WaxPlayer2.FMT_MIRROR4)
						loadingMsg = -1;
					else if (newfmt == WaxPlayer2.FMT_VIDEO || newfmt == WaxPlayer2.FMT_AUDIO)
						loadingMsg = WaxPlayer2.MSG_SHOW_LOADING;
					if (loadingMsg > 0) {
						Message msg = new Message();
						msg.what = loadingMsg;
						mediaplayer.waxHandler.sendMessageDelayed(msg, msgDelay);
						if (loadingMsg == WaxPlayer2.MSG_SHOW_LOADING2)
							mediaplayer.dialogLoading2Count ++;
					}
					retVal = 1;
				}
			} else if (start == 0) {
				if (mediaplayer != null && mediaplayer.isExiting() == false) {
					mediaplayer.EnterExit();
					mediaplayer.send_playFailure("1-00000000");
					waitMPlayerFinished(mPlayerIndex, true);
					retVal = 1;
				}
			}
		} catch (Exception ex) {
		} catch (Throwable th) {
		}
		MPlayerLock.unlock();
		return retVal;
	}

	public int Play(String clientId, String url1, String url2, float startPosition, int duration, int contentSize, String title1, String userAgent1) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			MPlayerLock.lock();
			int i = 0;
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false && 
					MPlayerArray[i].client_id.equals(clientId) ) {
					retVal = MPlayerArray[i].Play(url1, url2, startPosition, duration, contentSize, title1, userAgent1);
					if (retVal == 1)
						mainHandler.sendEmptyMessage(MSG_CHECK_VIDEOSIZE);
					break;
				}
			}
			MPlayerLock.unlock();
		}
		return retVal;
	}

	public int Seek(String clientId, final int wanttime, String newuri) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			MPlayerLock.lock();
			int i = 0;
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false && 
					MPlayerArray[i].client_id.equals(clientId) ) {
					retVal = MPlayerArray[i].Seek(wanttime, newuri);
					break;
				}
			}
			MPlayerLock.unlock();
		}
		return retVal;
	}

	public void gotoStop(String clientId, boolean destroy, int forceShow, boolean reportStop, int proto) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			MPlayerLock.lock();
			int i = 0;
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false && (
					MPlayerArray[i].mediaProto == (MPlayerArray[i].mediaProto&proto) || proto < 0) && (
					MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) && 
					MPlayerArray[i].is_Stopped() == false ) {
					MPlayerArray[i].gotoStop(false, forceShow, reportStop);
					if (destroy == true)
						MPlayerArray[i].gotoFinish(forceShow);
					retVal ++;
					if (execAll == 0)
						break;
				}
			}
			if (execAll == 1 && retVal == 0 && destroy == true)
				mainHandler.sendEmptyMessage(MSG_FINISH_ACTIVITY);
			MPlayerLock.unlock();
		}
	}

	public int Pause(String clientId, boolean forceShow, int reqLevel, int delayshow, boolean reportState) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			MPlayerLock.lock();
			int i = 0;
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false && (
					MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
					retVal = MPlayerArray[i].Pause(forceShow, reqLevel, delayshow);
					if (retVal == 1 && reportState == true)
						waxPlayService.MediaPause(MPlayerArray[i].client_id);
					if (execAll == 0)
						break;
				}
			}
			MPlayerLock.unlock();
		}
		return retVal;
	}

	public int Resume(String clientId, int reqLevel, boolean reportState) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			MPlayerLock.lock();
			int i = 0;
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false && (
					MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
					retVal = MPlayerArray[i].Resume(reqLevel);
					if (retVal == 1 && reportState == true)
						waxPlayService.MediaResume(MPlayerArray[i].client_id);
					if (execAll == 0)
						break;
				}
			}
			MPlayerLock.unlock();
		}
		return retVal;
	}

	public int GetVideoWidth(String clientId) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			int i = 0;
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false && (
					MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
					retVal = MPlayerArray[i].GetVideoWidth();
					break;
				}
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return retVal;
	}

	public int GetVideoHeight(String clientId) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			int i = 0;
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false && (
					MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
					retVal = MPlayerArray[i].GetVideoHeight();
					break;
				}
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return retVal;
	}

	public int GetDuration(String clientId) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			int i = 0;
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false && (
					MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
					retVal = MPlayerArray[i].GetDuration();
					break;
				}
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return retVal;
	}

	public int GetCurrentTime(String clientId) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			int i = 0;
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false && (
					MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
					retVal = MPlayerArray[i].GetCurrentTime();
					break;
				}
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return retVal;
	}

	public int GetVolume(String clientId) {
		int retVal = 50;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			int i = 0;
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false && (
					MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
					retVal = MPlayerArray[i].GetVolume();
					break;
				}
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return retVal;
	}

	public int SetVolume(String clientId, int volume, boolean ui) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			MPlayerLock.lock();
			int i = 0;
			for (i = 0; i < MPlayerArray.length; i++) {
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false && (
					MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
					retVal = MPlayerArray[i].SetVolume(volume, ui);
					break;
				}
			}
			MPlayerLock.unlock();
		}
		return retVal;
	}

	public static void put_stopFromUser(String clientId, int proto, int stopFromUser) {
		if (clientId != null && clientId.length() > 0 && 
			proto < WaxPlayer2.PROTOCOL_MAX && stopFromUser_Map[proto] != null) {
			stopFromUser_Lock.lock();
			stopFromUser_Map[proto].put(clientId, new Integer(stopFromUser));
			stopFromUser_Lock.unlock();
		}
	}

	public static int get_stopFromUser(String clientId, int proto) {
		int retVal = 0; // default value
		if (clientId != null && clientId.length() > 0 && 
			proto < WaxPlayer2.PROTOCOL_MAX && stopFromUser_Map[proto] != null) {
			stopFromUser_Lock.lock();
			Integer stopFromUser_Int = stopFromUser_Map[proto].get(clientId);
			if (stopFromUser_Int != null)
				retVal = (int)stopFromUser_Int;
			stopFromUser_Lock.unlock();
		}
		return retVal;
	}

	public void Scale(String clientId, int state, int zoomflag) {
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && 
						MPlayerArray[i].isExiting() == false && (
						MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
						MPlayerArray[i].Scale(state, zoomflag);
						if (execAll == 0)
							break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
	}

	public void SetAudioMetadata(String clientId, String cover, String album, String title, String artist, String lyric_url) {
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && 
						MPlayerArray[i].isExiting() == false && (
						MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
						MPlayerArray[i].UpdateAudioMetadata(false, true, cover, album, title, artist, lyric_url);
						if (execAll == 0)
							break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
	}

	public int FULLSCREEN_Player(String clientId, boolean isfullscreen) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0, j = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && 
						MPlayerArray[i].isExiting() == false &&
						MPlayerArray[i].client_id.equals(clientId) ) {
						for (j = 0; j < MPlayerCount; j ++) {
							if (MPlayerIdResort[j] == MPlayerArray[i].fragment_id - 1) {
								MPlayerFocused = j;
								SWITCHTO_MPlayer(MPlayerIdResort[MPlayerFocused], false);
								FULLSCREEN_MPlayer(isfullscreen, false);
								retVal = 1;
								break;
							}
						}
						break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return retVal;
	}

	public int get_whichPlayer(String clientId) {
		int retVal = WaxPlayer2.PLAYER_EXTERN;
		if (clientId != null && clientId.length() > 0) {
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && 
						MPlayerArray[i].isExiting() == false && 
						MPlayerArray[i].client_id.equals(clientId) ) {
						retVal = MPlayerArray[i].whichPlayer;
						break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return retVal;
	}

	public String get_StreamUrl(String clientId) {
		String retVal = "";
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && 
						MPlayerArray[i].isExiting() == false && (
						MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
						retVal = MPlayerArray[i].url;
						break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return retVal;
	}

	public int get_contentSize(String clientId) {
		int retVal = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && 
						MPlayerArray[i].isExiting() == false && (
						MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
						retVal = MPlayerArray[i].contentSize;
						break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return retVal;
	}

	public int get_mediaFormat(String clientId, int proto) {
		int retVal = WaxPlayer2.FMT_UNKNOWN;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && 
						MPlayerArray[i].isExiting() == false && (
						MPlayerArray[i].mediaProto == (MPlayerArray[i].mediaProto&proto) || proto < 0) && (
						MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
						retVal = MPlayerArray[i].mediaFormat;
						if (execAll == 0)
							break;
						if (retVal == WaxPlayer2.FMT_VIDEO /*|| retVal == WaxPlayer2.FMT_AUDIO*/)
							break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return retVal;
	}

    public boolean get_PlayerAvailable(String clientId, int proto) {
		int n = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && (
						MPlayerArray[i].client_id.equals(clientId) || MPlayerArray[i].isLocked2 == false ) )
						n ++;
					else if (MPlayerArray[i] == null)
						n ++;
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return (n > 0);
	}

	public boolean is_Caching(String clientId, int proto) {
		int n = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && 
						MPlayerArray[i].isExiting() == false && (
						MPlayerArray[i].mediaProto == (MPlayerArray[i].mediaProto&proto) || proto < 0) && (
						MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
						if (MPlayerArray[i].is_Caching() == true)
							n ++;
						if (execAll == 0)
							break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return (n > 0);
	}

	public boolean is_Playing2(String clientId, int proto) {
		int n = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && (
						MPlayerArray[i].isExiting() == false || MPlayerArray[i].inBackground != 0) && (
						MPlayerArray[i].mediaProto == (MPlayerArray[i].mediaProto&proto) || proto < 0) && (
						MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
						if (MPlayerArray[i].is_Playing2() == true)
							n ++;
						if (execAll == 0)
							break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return (n > 0);
	}

	public boolean is_Playing(String clientId, int proto) {
		int n = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && (
						MPlayerArray[i].isExiting() == false || MPlayerArray[i].inBackground != 0) && (
						MPlayerArray[i].mediaProto == (MPlayerArray[i].mediaProto&proto) || proto < 0) && (
						MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
						if (MPlayerArray[i].is_Playing() == true)
							n ++;
						if (execAll == 0)
							break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return (n > 0);
	}

	public boolean is_Stopped(String clientId, int proto) {
		int n = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && (
						MPlayerArray[i].isExiting() == false || MPlayerArray[i].inBackground != 0) && (
						MPlayerArray[i].mediaProto == (MPlayerArray[i].mediaProto&proto) || proto < 0) && (
						MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
						if (MPlayerArray[i].is_Stopped() == false)
							n ++;
						if (execAll == 0)
							break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return (n == 0);
	}

	public String PlayState2String(String states) {
		if (states == null)
			states = "";
		boolean locked = MPlayerLock.tryLock();
		try {
			int j = 0;
			for (j = 0; j < MPlayerCount; j ++) {
				int i = MPlayerIdResort[j];
				if (MPlayerArray[i] != null && 
					MPlayerArray[i].isExiting() == false ) {
					states += "ID:" + MPlayerArray[i].client_id + "|";
					states += "PROTOCOL:" + MPlayerArray[i].mediaProto + "|";
					states += "FORMAT:" + MPlayerArray[i].mediaFormat + "|";
					states += "PLAYER:" + MPlayerArray[i].whichPlayer + "|";
					states += "STATE:" + MPlayerArray[i].playback_State + "|";
					if (MPlayerArray[i].mediaFormat != WaxPlayer2.FMT_AUDIO && MPlayerArray[i].mediaFormat != WaxPlayer2.FMT_DOC)
						states += "ZOOM:" + MPlayerArray[i].zoomState + "|";
					int id = -1;
					if (MPlayerZoomed >= 0)
						id = MPlayerIdResort[MPlayerZoomed];
					states += "FULLSCREEN:" + ((id == i) ? 1 : 0) + "|";
					id = -1;
					if (MPlayerFocused >= 0)
						id = MPlayerIdResort[MPlayerFocused];
					states += "FOCUS:" + ((id == i) ? 1 : 0) + "|";
					states += "MUTE:" + ((MPlayerArray[i].isMuted2 == true) ? 1 : 0) + "|";
					states += "TIME:" + MPlayerArray[i].lastPos + "/" + MPlayerArray[i].duration + "\r\n";
				}
			}
		} catch (Exception e1) {
		} catch (Throwable e2) {
		}
		if (locked == true)
			MPlayerLock.unlock();
		return states;
	}

	public int setPlayerMute(String clientId, boolean isMute) {
		int n = 0;
		if (clientId != null && clientId.length() > 0) {
			int execAll = 0;
			if (clientId.equals("*"))
				execAll = 1;
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && (
						MPlayerArray[i].client_id.equals(clientId) || execAll != 0 ) ) {
						if (MPlayerArray[i].SetMuted(isMute) == 1)
							n ++;
						if (execAll == 0)
							break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
		return n;
	}

	public void setLockPlayer(String clientId, boolean isLock) {
		if (clientId != null && clientId.length() > 0) {
			boolean locked = MPlayerLock.tryLock();
			try {
				int i = 0;
				for (i = 0; i < MPlayerArray.length; i++) {
					if (MPlayerArray[i] != null && 
						MPlayerArray[i].isExiting() == false && 
						MPlayerArray[i].client_id.equals(clientId) ) {
						if (isLock == true)
							MPlayerArray[i].gotLock2();
						else
							MPlayerArray[i].lostLock2();
						break;
					}
				}
			} catch (Exception e1) {
			} catch (Throwable e2) {
			}
			if (locked == true)
				MPlayerLock.unlock();
		}
	}

	public void EnterExit() {
		synchronized (exitingLock) {
			STATUS_EXITING = true;
		}
		//Global.do_sleep(100); // Schedule another Request/State before really gotoStop()
	}

	public void ClearExit() {
		synchronized (exitingLock) {
			STATUS_EXITING = false;
		}
	}

	public boolean isExiting() {
		synchronized (exitingLock) {
			return STATUS_EXITING;
		}
	}
}
