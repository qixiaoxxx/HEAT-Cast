package com.waxrain.ui;

import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Locale;

import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.os.Message;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.WindowManager;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.RelativeLayout;
import android.util.Log;

import com.waxrain.droidsender.delegate.*;
import com.waxrain.airplaydmr.*;
import com.waxrain.utils.*;

public class WaxPlayer4Hws {
	/*///////////////////// HuaWei Cast Plus /////////////////////*/
	private static final String HWS_LOG_TAG = com.waxrain.airplaydmr.WaxPlayService.LOG_TAG;
	public static final String HWS_CLIENTID = "127.0.0.123";
	private static final String HWS_PLAYURL = "hms://mirroring";
	private static boolean HWS_PARAM_needpass = true;
	private static boolean HWS_PARAM_isnewpass = true;
	private static int HWS_PARAM_needpin = 0; // -1/0/1
	private static String HWS_PARAM_passstr = "123456";
	public static String HWS_PARAM_devnamestr = "CastPlusTestDevice";
	public static int HWS_PARAM_screenwidth = 1920, HWS_PARAM_screenheight = 1080;
	private static int HWS_PARAM_videowidth = 1080, HWS_PARAM_videoheight = 1920;
	private static int HWS_PARAM_framerate = 30;
	public static final String HWS_BROADCAST_ACTION_DISCONNECT = "castplus.intent.action.disconnect";
	public static final String HWS_BROADCAST_ACTION_PLAY = "castplus.intent.action.play";
	public static final String HWS_BROADCAST_ACTION_PAUSE = "castplus.intent.action.pause";
	public static final String HWS_BROADCAST_ACTION_FINISH_PIN_ACTIVITY = "castplus.intent.action.finishpinactivity";
	public static final String HWS_BROADCAST_ACTION_FINISH_PLAY_ACTIVITY = "castplus.intent.action.finishplayactivity";
	public static final String HWS_BROADCAST_ACTION_NETWORK_QUALITY = "castplus.intent.action.networkquality";
	private static final int HWS_OPTIMIZATION_TAG_CODEC_CONFIGURE_FLAG = 1;
	private static final int HWS_OPTIMIZATION_TAG_MEDIA_FORMAT_INTEGER = 2;
	private static final int HWS_OPTIMIZATION_TAG_MEDIA_FORMAT_FLOAT = 4;
	private static final int HWS_OPTIMIZATION_TAG_MEDIA_FORMAT_LONG = 8;
	private static final int HWS_OPTIMIZATION_TAG_MEDIA_FORMAT_STRING = 16;
	private static final ReentrantLock HWS_ServLock = new ReentrantLock();
	private static boolean HWS_IsFinishSelfBehavior = true;
	private static boolean HWS_PinCodeShown = false;
	private static String HWS_mHostname = "";
	private static boolean HWS_mCastServiceInitDone = false;
	private static boolean HWS_mCastServiceReady = false;
	public static boolean HWS_isSurfaceReady = false;
	private static BroadcastReceiver HWS_mBroadcastReceiver = null;
	private static HWS_CallbackHandler HWS_mCallbackHandler = null;
	public static int HWS_NETWORK_QUALITY_EXCEPTION = 0; 
	public static int HWS_NETWORK_QUALITY_WORSE = 1;
	public static int HWS_NETWORK_QUALITY_BAD = 2;
	public static int HWS_NETWORK_QUALITY_GENERAL = 3;
	public static int HWS_NETWORK_QUALITY_GOOD = 4;
    private static Object HWS_mHiSurfaceView = null;
	private static Object HWS_mProjectionDevice = null;
	private static Object HWS_mPlayerClient = null;
	private static Object HWS_mCallback = null;

	private static class HWS_CallbackHandler extends Handler {
		public HWS_CallbackHandler(Looper mainLooper) {
			super(mainLooper);
		}
		@Override
		public void handleMessage(Message msg) {
			if (Config.HWS_ENABLED == 0)
				return ;
			if (msg == null)
				return;
			if (Global.mDebugLevel >= 2)
				Log.i(HWS_LOG_TAG, "msg " + msg.what);
			switch (msg.what) {
			case com.huawei.castpluskit.Constant.EVENT_ID_SERVICE_BIND_SUCCESS:
				HWS_mCastServiceInitDone = true;
				try {
					HWS_setCapability(HWS_PARAM_screenwidth, HWS_PARAM_screenheight, HWS_PARAM_videowidth, HWS_PARAM_videoheight, HWS_PARAM_framerate, HWS_OPTIMIZATION_TAG_CODEC_CONFIGURE_FLAG);
					HWS_setDiscoverable(true, HWS_PARAM_devnamestr);
					HWS_setAuthMode(HWS_PARAM_needpass, HWS_PARAM_passstr, HWS_PARAM_isnewpass);
				} catch (Exception ex) {
				}
				break;
			case com.huawei.castpluskit.Constant.EVENT_ID_CONNECT_REQ: {
				HWS_HidePinCode();
				try {
					com.huawei.castpluskit.DisplayInfo displayInfo = (com.huawei.castpluskit.DisplayInfo) msg.obj;
					if (displayInfo != null) {
						HWS_mProjectionDevice = (Object)displayInfo.getProjectionDevice();
						HWS_startPlayActivity();
					} else {
						if (Global.mDebugLevel >= 2)
							Log.e(HWS_LOG_TAG, "displayInfo is null.");
					}
				} catch (Exception ex) {
				} catch (Throwable th) {
				}
			}	break;
			case com.huawei.castpluskit.Constant.EVENT_ID_PIN_CODE_SHOW: {
				try {
					com.huawei.castpluskit.DisplayInfo displayInfo = (com.huawei.castpluskit.DisplayInfo) msg.obj;
					if ((displayInfo != null) && (HWS_mProjectionDevice = (Object)displayInfo.getProjectionDevice()) != null) {
						String pinCode = displayInfo.getPinCode();
						HWS_mHostname = ((com.huawei.castpluskit.ProjectionDevice)HWS_mProjectionDevice).getDeviceName();
						if (HWS_mPlayerClient != null) {
							if (HWS_PARAM_needpin < 0)
								((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).setConnectRequestChooseResult(new com.huawei.castpluskit.ConnectRequestChoice(com.huawei.castpluskit.Constant.CONNECT_REQ_CHOICE_REJECT, (com.huawei.castpluskit.ProjectionDevice)HWS_mProjectionDevice));
							else if (HWS_PARAM_needpin == 0)
								((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).setConnectRequestChooseResult(new com.huawei.castpluskit.ConnectRequestChoice(com.huawei.castpluskit.Constant.CONNECT_REQ_CHOICE_ALWAYS, (com.huawei.castpluskit.ProjectionDevice)HWS_mProjectionDevice));
							else if (HWS_PARAM_needpin == 1)
								((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).setConnectRequestChooseResult(new com.huawei.castpluskit.ConnectRequestChoice(com.huawei.castpluskit.Constant.CONNECT_REQ_CHOICE_ONCE, (com.huawei.castpluskit.ProjectionDevice)HWS_mProjectionDevice));
						} else {
							if (Global.mDebugLevel >= 2)
								Log.e(HWS_LOG_TAG, "HWS_mPlayerClient is null.");
						}
						if (Global.mDebugLevel >= 2)
							Log.e(HWS_LOG_TAG, "PINCODE is : "+ pinCode);
						HWS_ShowPinCode(pinCode);
					} else {
						if (Global.mDebugLevel >= 2)
							Log.e(HWS_LOG_TAG, "displayInfo is null.");
					}
				} catch (Exception ex) {
				} catch (Throwable th) {
				}
			}	break;
			case com.huawei.castpluskit.Constant.EVENT_ID_PIN_CODE_SHOW_FINISH:
				HWS_HidePinCode();
				break;
			case com.huawei.castpluskit.Constant.EVENT_ID_DEVICE_CONNECTED:
				break;
			case com.huawei.castpluskit.Constant.EVENT_ID_CASTING:
				break;
			case com.huawei.castpluskit.Constant.EVENT_ID_PAUSED:
				HWS_mCastServiceReady = true;
				HWS_startPlay();
				break;
			case com.huawei.castpluskit.Constant.EVENT_ID_DEVICE_DISCONNECTED:
				HWS_HidePinCode();
				HWS_IsFinishSelfBehavior = false;
				HWS_sendBroadcast(HWS_BROADCAST_ACTION_FINISH_PLAY_ACTIVITY);
				break;
			case com.huawei.castpluskit.Constant.EVENT_ID_NETWORK_QUALITY: {
				try {
					com.huawei.castpluskit.DisplayInfo displayInfo = (com.huawei.castpluskit.DisplayInfo) msg.obj;
					if (displayInfo != null) {
						int networkQuality = displayInfo.getNetworkQuality();
						if (Global.mDebugLevel >= 2)
							Log.i(HWS_LOG_TAG, "networkquality update: " + networkQuality);
						Intent intent = new Intent();
						intent.setAction(HWS_BROADCAST_ACTION_NETWORK_QUALITY);
						intent.putExtra("networkquality", networkQuality);
						((WaxPlayService)WaxPlayService._self).sendBroadcast(intent);
					}
				} catch (Exception ex) {
				} catch (Throwable th) {
				}
			}	break;
			case com.huawei.castpluskit.Constant.EVENT_ID_SET_SURFACE:
				break;
			default:
				break;
			}
		}
	}

	public static void HWS_InitService() {
		if (Config.HWS_ENABLED == 0)
			return ;
		try {
			if (HWS_mBroadcastReceiver == null) {
				HWS_mBroadcastReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						String action = intent.getAction();
						if (Global.mDebugLevel >= 2)
							Log.i(HWS_LOG_TAG, "Broadcast received, action: " + action);
						if (HWS_BROADCAST_ACTION_DISCONNECT.equals(action)) {
							if (HWS_IsFinishSelfBehavior)
								HWS_disconnectDevice();
						} else if (HWS_BROADCAST_ACTION_PAUSE.equals(action)) {
							HWS_pausePlay();
						} else if (HWS_BROADCAST_ACTION_PLAY.equals(action)) {
							HWS_startPlay();
						}
					}
				};
			}
			IntentFilter broadcastFilter = new IntentFilter();
			broadcastFilter.addAction(HWS_BROADCAST_ACTION_DISCONNECT);
			broadcastFilter.addAction(HWS_BROADCAST_ACTION_PLAY);
			broadcastFilter.addAction(HWS_BROADCAST_ACTION_PAUSE);
			if (android.os.Build.VERSION.SDK_INT >= 26 && ((WaxPlayService)WaxPlayService._self).getApplicationInfo().targetSdkVersion >= 34)
				((WaxPlayService)WaxPlayService._self).registerReceiver(HWS_mBroadcastReceiver, broadcastFilter, Context.RECEIVER_EXPORTED);
			else
				((WaxPlayService)WaxPlayService._self).registerReceiver(HWS_mBroadcastReceiver, broadcastFilter);
			if (HWS_mCallbackHandler == null)
				HWS_mCallbackHandler = new HWS_CallbackHandler(((WaxPlayService)WaxPlayService._self).getMainLooper());
		} catch (Exception ex) {
		}
		HWS_NETWORK_QUALITY_EXCEPTION = com.huawei.castpluskit.Constant.NETWORK_QUALITY_EXCEPTION; 
		HWS_NETWORK_QUALITY_WORSE = com.huawei.castpluskit.Constant.NETWORK_QUALITY_WORSE;
		HWS_NETWORK_QUALITY_BAD = com.huawei.castpluskit.Constant.NETWORK_QUALITY_BAD;
		HWS_NETWORK_QUALITY_GENERAL = com.huawei.castpluskit.Constant.NETWORK_QUALITY_GENERAL;
		HWS_NETWORK_QUALITY_GOOD = com.huawei.castpluskit.Constant.NETWORK_QUALITY_GOOD;
	}

	public static void HWS_DestroyService() {
		if (Config.HWS_ENABLED == 0)
			return ;
		if (HWS_mBroadcastReceiver != null)
			((WaxPlayService)WaxPlayService._self).unregisterReceiver(HWS_mBroadcastReceiver);
	}

	public static void HWS_StartService() {
		if (Config.HWS_ENABLED == 0)
			return ;
		HWS_ServLock.lock();
		try {
			if (HWS_mCallback == null) {
				HWS_mCallback = (Object)new com.huawei.castpluskit.IEventListener.Stub() {
					public boolean onEvent(com.huawei.castpluskit.Event event) {
						int eventId = event.getEventId();
						if (Global.mDebugLevel >= 2)
							Log.e(HWS_LOG_TAG, "eventId: " + eventId);
						Message msg = HWS_mCallbackHandler.obtainMessage();
						msg.what = eventId;
						msg.obj = event;
						msg.sendToTarget();
						return true;
					}
					public boolean onDisplayEvent(int eventId, com.huawei.castpluskit.DisplayInfo displayInfo) {
						if (Global.mDebugLevel >= 2)
							Log.e(HWS_LOG_TAG, "handleDisplayEvent: " + eventId);
						Message msg = HWS_mCallbackHandler.obtainMessage();
						msg.what = eventId;
						msg.obj = displayInfo;
						msg.sendToTarget();
						return true;
					}
				};
			}
			HWS_mPlayerClient = (Object)com.huawei.castpluskit.PlayerClient.getInstance();
			((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).registerCallback((com.huawei.castpluskit.IEventListener)HWS_mCallback);
			((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).init(WaxPlayService._self);
		} catch (Exception ex) {
		} catch (Throwable th) {
		}
		HWS_ServLock.unlock();
	}

	public static void HWS_StopService() {
		if (Config.HWS_ENABLED == 0)
			return ;
		HWS_ServLock.lock();
		HWS_mCastServiceInitDone = false;
		try {
			HWS_setDiscoverable(false, HWS_PARAM_devnamestr);
			if (HWS_mPlayerClient != null) {
				((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).unregisterCallback((com.huawei.castpluskit.IEventListener)HWS_mCallback);
				((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).deinit();
			} else {
				if (Global.mDebugLevel >= 2)
					Log.i(HWS_LOG_TAG, "HWS_mPlayerClient is null.");
			}
		} catch (Exception ex) {
		} catch (Throwable th) {
		}
		HWS_ServLock.unlock();
	}

	public static void HWS_SetCastParams(Context mContext, String mDeviceName, boolean mAuthMode, String mPassword, int mScreenWidth, int mScreenHeight) {
		if (Config.HWS_ENABLED == 0)
			return ;
		HWS_PARAM_isnewpass = true;
		HWS_PARAM_needpass = mAuthMode;
		HWS_PARAM_passstr = mPassword;
		HWS_PARAM_devnamestr = mDeviceName;
		HWS_PARAM_screenwidth = HWS_PARAM_videowidth = mScreenWidth;
		HWS_PARAM_screenheight = HWS_PARAM_videoheight = mScreenHeight;
		if (HWS_mCastServiceInitDone == false)
			return ;
		HWS_StopService();
		HWS_StartService();
	}

	private static void HWS_HidePinCode() {
		if (HWS_PinCodeShown) {
			try {
				((WaxPlayService)WaxPlayService._self).pe(HWS_CLIENTID, 7511, "".getBytes("GBK"));
			} catch (Exception ex) {
			}
			HWS_PinCodeShown = false;
		}
	}

	private static void HWS_ShowPinCode(String pinCode) {
		try {
			((WaxPlayService)WaxPlayService._self).pe(HWS_CLIENTID, 7511, pinCode.getBytes("GBK"));
			HWS_PinCodeShown = true;
		} catch (Exception ex) {
		}
	}

	private static void HWS_startPlay() {
		if (Config.HWS_ENABLED == 0)
			return ;
		if (!HWS_mCastServiceReady || !HWS_isSurfaceReady || HWS_mHiSurfaceView == null)
			return;
		try {
			if (HWS_mPlayerClient != null) {
				((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).setHiSightSurface(((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView).getHolder().getSurface());
				((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).play(new com.huawei.castpluskit.TrackControl(((com.huawei.castpluskit.ProjectionDevice)HWS_mProjectionDevice).getDeviceId()));
				HWS_mCastServiceReady = false;
			} else {
				if (Global.mDebugLevel >= 2)
					Log.e(HWS_LOG_TAG, "HWS_mPlayerClient is null.");
			}
		} catch (Exception ex) {
		} catch (Throwable th) {
		}
	}

	private static void HWS_pausePlay() {
		if (Config.HWS_ENABLED == 0)
			return ;
		try {
			if (HWS_mPlayerClient != null) {
				if (Global.mDebugLevel >= 2)
					Log.i(HWS_LOG_TAG, "pausePlay() called.");
				((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).pause(new com.huawei.castpluskit.TrackControl(((com.huawei.castpluskit.ProjectionDevice)HWS_mProjectionDevice).getDeviceId()));
			}
		} catch (Exception ex) {
		} catch (Throwable th) {
		}
	}

	private static void HWS_startPlayActivity() {
		if (Config.HWS_ENABLED == 0)
			return ;
		if (Global.mDebugLevel >= 2)
			Log.i(HWS_LOG_TAG, "HWS_startPlayActivity() called.");
		HWS_IsFinishSelfBehavior = true;
		HWS_isSurfaceReady = false;
		HWS_mHiSurfaceView = null;
		if (WaxPlayService.airplayerLoaded == true) {
			new Thread() {
				public void run() {
					((WaxPlayService)WaxPlayService._self).pp(HWS_CLIENTID, HWS_PLAYURL, 1, WaxPlayer2.FMT_MIRROR4, WaxPlayer2.PROTOCOL_DLNA);
					((WaxPlayService)WaxPlayService._self).py(HWS_CLIENTID, HWS_PLAYURL, "", 0.0f, 0, 0, HWS_PLAYURL.substring(HWS_PLAYURL.lastIndexOf('/')+1), "");
				}
			}.start();
		}
	}

	private static void HWS_setAuthMode(boolean needPassword, String password, boolean isNewPassword) {
		if (Config.HWS_ENABLED == 0)
			return ;
		if (Global.mDebugLevel >= 2)
			Log.i(HWS_LOG_TAG, "setAuthMode() called.");
		try {
			com.huawei.castpluskit.AuthInfo authInfo = null;
			if (needPassword) {
				if (Global.mDebugLevel >= 2)
					Log.i(HWS_LOG_TAG, "password: $" + password + "$");
				authInfo = new com.huawei.castpluskit.AuthInfo(com.huawei.castpluskit.AuthInfo.AUTH_MODE_PWD, password, isNewPassword);
			} else {
				authInfo = new com.huawei.castpluskit.AuthInfo(com.huawei.castpluskit.AuthInfo.AUTH_MODE_GENERIC);
			}
			if (HWS_mPlayerClient != null) {
				((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).setAuthMode(authInfo);
			} else {
				if (Global.mDebugLevel >= 2)
					Log.e(HWS_LOG_TAG, "HWS_mPlayerClient is null.");
			}
		} catch (Exception ex) {
		} catch (Throwable th) {
		}
	}

	private static void HWS_setDiscoverable(boolean isDiscoverable, String deviceName) {
		if (Config.HWS_ENABLED == 0)
			return ;
		if (Global.mDebugLevel >= 2)
			Log.i(HWS_LOG_TAG, "setDiscoverable() called.");
		try {
			com.huawei.castpluskit.DeviceInfo deviceInfo = new com.huawei.castpluskit.DeviceInfo(deviceName, com.huawei.castpluskit.DeviceInfo.TYPE_TV);
			if (HWS_mPlayerClient != null) {
				((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).setDiscoverable(isDiscoverable, deviceInfo);
			} else {
				if (Global.mDebugLevel >= 2)
					Log.e(HWS_LOG_TAG, "HWS_mPlayerClient is null.");
			}
		} catch (Exception ex) {
		} catch (Throwable th) {
		}
	}

	private static void HWS_setCapability(int screenWidth, int screenHeight, int videoWidth, int videoHeight, int framerate, int mode) {
		if (Config.HWS_ENABLED == 0)
			return ;
		if (Global.mDebugLevel >= 2)
			Log.i(HWS_LOG_TAG, "setCapability() called.");
		try {
			com.huawei.castpluskit.HiSightCapability capability = new com.huawei.castpluskit.HiSightCapability(screenWidth, screenHeight, videoWidth, videoHeight);
			capability.setVideoFps(framerate);
			// different HiSightCapability codec configuration
			if ((mode & HWS_OPTIMIZATION_TAG_CODEC_CONFIGURE_FLAG) != 0) {
				//capability.setMediaCodecConfigureFlag(2);/*Snoopy 11/8/2024 : NOT WORKING for Android 11 on YuanDong Box?*/
			}
			if ((mode & HWS_OPTIMIZATION_TAG_MEDIA_FORMAT_INTEGER) != 0) {
			}
			if ((mode & HWS_OPTIMIZATION_TAG_MEDIA_FORMAT_FLOAT) != 0) {
			}
			if ((mode & HWS_OPTIMIZATION_TAG_MEDIA_FORMAT_LONG) != 0) {
			}
			if ((mode & HWS_OPTIMIZATION_TAG_MEDIA_FORMAT_STRING) != 0) {
			}
			if (HWS_mPlayerClient != null) {
				((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).setCapability(capability);
			} else {
				if (Global.mDebugLevel >= 2)
					Log.e(HWS_LOG_TAG, "HWS_mPlayerClient is null.");
			}
		} catch (Exception ex) {
		} catch (Throwable th) {
		}
	}

	private static void HWS_disconnectDevice() {
		if (Config.HWS_ENABLED == 0)
			return ;
		if (Global.mDebugLevel >= 2)
			Log.i(HWS_LOG_TAG, "disconnectDevice() called.");
		try {
			if (HWS_mPlayerClient != null) {
				((com.huawei.castpluskit.PlayerClient)HWS_mPlayerClient).disconnectDevice((com.huawei.castpluskit.ProjectionDevice)HWS_mProjectionDevice);
			} else {
				if (Global.mDebugLevel >= 2)
					Log.e(HWS_LOG_TAG, "HWS_mPlayerClient is null.");
			}
		} catch (Exception ex) {
		} catch (Throwable th) {
		}
	}

	private static void HWS_sendBroadcast(String broadcastAction) {
		if (Config.HWS_ENABLED == 0)
			return ;
		if (Global.mDebugLevel >= 2)
			Log.i(HWS_LOG_TAG, "HWS_sendBroadcast call "+broadcastAction);
		try {
			Intent intent = new Intent();
			intent.setAction(broadcastAction);
			((WaxPlayService)WaxPlayService._self).sendBroadcast(intent);
		} catch (Exception ex) {
		}
	}

	public static void HWS_DestroyView(RelativeLayout wholeLayout, int inBackground, boolean destroyVideoView) {
		if (Config.HWS_ENABLED == 0)
			return ;
		try {
			if (HWS_mHiSurfaceView != null) {
				if (inBackground == 0) {
					if (destroyVideoView == true) {
						if (wholeLayout.indexOfChild((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView) >= 0) {
							WaxPlayer.HideWholeLayoutSubview((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView);
							wholeLayout.removeView((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView);
						}
						HWS_mHiSurfaceView = null;
					} else {
						((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView).setVisibility(View.GONE);
					}
				}
			}
		} catch (Exception ex) {
		} catch (Throwable th) {
		}
	}

	public static void HWS_CreateView(WaxPlayer2 mFragment, RelativeLayout wholeLayout, int resId, boolean destroyVideoView) {
		if (Config.HWS_ENABLED == 0)
			return ;
		try {
			if (HWS_mHiSurfaceView == null) {
				SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
					@Override
					public void surfaceCreated(SurfaceHolder holder) {
						if (Global.mDebugLevel >= 2)
							Log.i(HWS_LOG_TAG, "surfaceCreated3() called.");
						HWS_isSurfaceReady = true;
						HWS_sendBroadcast(HWS_BROADCAST_ACTION_PLAY);
					}
					@Override
					public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
						Log.i(HWS_LOG_TAG, "surfaceChanged3 to "+width+"x"+height);
						HWS_isSurfaceReady = true;
					}
					@Override
					public void surfaceDestroyed(SurfaceHolder holder) {
						if (Global.mDebugLevel >= 2)
							Log.i(HWS_LOG_TAG, "surfaceDestroyed3() called.");
						HWS_isSurfaceReady = false;
						HWS_mHiSurfaceView = null;
						HWS_sendBroadcast(HWS_BROADCAST_ACTION_PAUSE);
					}
				};
				HWS_mHiSurfaceView = (Object)new com.huawei.castpluskit.HiSightSurfaceView(mFragment.getContext());
				if (HWS_mHiSurfaceView != null) {
					//((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView).setSecure(true);
					SurfaceHolder surfaceHolder = ((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView).getHolder();
					if (surfaceHolder != null) {
						surfaceHolder.addCallback(mSurfaceHolderCallback);
					} else {
						if (Global.mDebugLevel >= 2)
							Log.e(HWS_LOG_TAG, "surfaceHolder is null.");
					}
					RelativeLayout.LayoutParams LP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
					LP.addRule(RelativeLayout.CENTER_IN_PARENT);
					((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView).setLayoutParams(LP);
					((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView).setId(resId);
				} else {
					if (Global.mDebugLevel >= 2)
						Log.e(HWS_LOG_TAG, "mHiView is null.");
				}
			}
			((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView).setFocusable(false);
			((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView).setFocusableInTouchMode(false);
			((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView).setOnTouchListener(mFragment);
			if (destroyVideoView == true) {
				if (wholeLayout.indexOfChild((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView) < 0)
					wholeLayout.addView((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView);
			} else {
				((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView).setVisibility(View.VISIBLE);
			}
			wholeLayout.bringChildToFront((com.huawei.castpluskit.HiSightSurfaceView)HWS_mHiSurfaceView);
			mFragment.send_showHostname(HWS_mHostname, 10);
		} catch (Exception ex) {
		} catch (Throwable th) {
		}
	}
}
