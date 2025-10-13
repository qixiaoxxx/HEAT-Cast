package com.waxrain.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.waxrain.airplaydmr.*;
import com.waxrain.airplaydmr_SDK.R;
import com.waxrain.droidsender.delegate.*;
import com.waxrain.utils.*;
import com.waxrain.video.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class WaxPlayer2 extends Fragment implements OnFocusChangeListener, OnTouchListener, OnClickListener {
	private static final String LOG_TAG = com.waxrain.airplaydmr.WaxPlayService.LOG_TAG;
	private static final int MSG_HDPLAYER_FAILURE = 1;
	public static final int MSG_SDPLAYER_FAILURE = 2;
	public static final int MSG_SHOW_LOADING = 3;
	public static final int MSG_HIDE_LOADING = 4;
	public static final int MSG_SHOW_LOADING2 = 5;
	public static final int MSG_HIDE_LOADING2 = 6;
	private static final int MSG_UPDATE_CPOSITION = 7;
	private static final int MSG_UPDATE_DURATION = 8;
	private static final int MSG_START_PLAY = 9;
	private static final int MSG_START_SEEK = 10;
	private static final int MSG_UPDATE_ICON_PLAY = 11;
	public static final int MSG_PLAY_DONE = 12;
	public static final int MSG_DELAY_EXIT = 13;
	public static final int MSG_UPDATE_ICON_END = 14;
	public static final int MSG_GOTO_FINISH = 15;
	private static final int MSG_VIDEOSIZE_CHANGE = 16;
	private static final int MSG_BUFFER_UPDATE = 17;
	private static final int MSG_TITLE_UPDATE = 18;
	private static final int MSG_SWITCH_PLAYER = 19;
	private static final int MSG_GOTO_RESTART = 20; // For Stop()
	public static final int MSG_DECODE_IMAGE_DONE = 21; // For async load image
	private static final int MSG_REMOVE_CURSOR = 22;
	public static final int MSG_SWITCH_EXTPLAYER = 23;
	public static final int MSG_PENDING_MICE_L_UP = 24;
	public static final int MSG_PENDING_MICE_L_CLICK = 25;
	public static final int MSG_PROMPT_DIALOG_PLAY = 26;
	public static final int MSG_OPEN_DOC = 27;
	public static final int MSG_INNERTEXT_UPDATE = 28;
	public static final int MSG_UPDATE_DBGMSG = 29;
	public static final int MSG_UPDATE_PLAYSPEED = 30;
	public static final int MSG_DRAW_SCREENSHOT = 31;
	public static final int MSG_SHOW_HOSTNAME = 32;
	public static final int MSG_SHOW_TYPEICON = 33;
	public static final int MSG_VIDEO_SCALE = 34;
	public static final int MSG_DESTROY_ADMOB_ON_PLAY = 35;
	public static final int MSG_UPDATE_AUDIO_METADATA = 36;
	public static final int MSG_CHECK_BTHID_INIT = 37;
	public Handler waxHandler = null;
	public boolean fragmentRunning = true; // Init value must be true ?
	public long player_startTs = System.currentTimeMillis();
	private Context activityContext = null;
	public int fragment_id = 1; // 1-4
	public String client_id = "";
	public FrameLayout frame_layout = null;

	public int screenWidth = 1, screenHeight = 1;
	public int screenXoff = 0, screenYoff = 0;
	private int videoWidth = 1, videoHeight = 1;
	public String url = null, url2 = null, url_hd = "", url_sd = "";
	public String lyric_url = "", music_cover = "", music_album = "", music_title = "", music_artist = "";
	public String title = "";
	public String userAgent = "";
	public String tls_key = null; // ascii hex
	public int servPort = 0;
	private int seekTime = 0, seekTime2 = 0/*for Resume Playback*/;
	public boolean exitOnBackKey2 = false; // exit playback on return key pressed
	public boolean is_Proxy_HLS = false;
	private float startPercent = 0.0f;
	public int contentSize = 0;
	public int duration = 0;
	public String hostname = "";
	private static final int TIMER_SCHED = 500;
	private static final int BLOCK_CHECK_INTERVAL = 4000;
	private int STATUS_REBUFFERING = 0;
	private int COUNT_REBUFFERING = 0;
	public int lastPos = 0, lastPos_save = 0;
	public int lastPos_try = 0;
	private int lastBuffProg = 0;
	private boolean plistCheck = false; // For play continuation
	private int requestFinished = 0;
	private int STATUS_LOADING = 0;
	public int STATUS_CLAYOUT_HIDE = 1; // default is invisible
	private boolean STATUS_EXITING = false;
	public int inBackground = 0;
	private boolean isResuming = false;
	private	boolean stopPlayback = true; // For Stop()
	private boolean stopFinished = false; // For WaxPlayer.stopBackground
	public long lastTime_BackKey = 0, lastTime_EnterKey = 0;
	private boolean duringSeeking = false;
	public int pauseReqLevel = -1; // default value
	private final Object exitingLock = new Object();
	private final Object playLock = new Object();
	private final Object stopLock = new Object();
	private final Object ctrlLock = new Object();
	private static ArrayList<String> supportSrtFmts = null;

	int RES_drawable_mute_on = R.drawable.mute_on;
	int RES_drawable_mute2_on = R.drawable.mute2_on;
	int RES_drawable_mute = R.drawable.mute;
	int RES_drawable_mute2 = R.drawable.mute2;
	public LayoutParams savedHdVideoViewLayout = null;
	public AttributeSet savedHdVideoViewAttrs = null;
	public int savedHdVideoViewStyle = 0;
	public LayoutParams savedSdVideoViewLayout = null;
	public AttributeSet savedSdVideoViewAttrs = null;
	public int savedSdVideoViewStyle = 0;
	public LayoutParams savedSdNativeViewLayout = null;
	public AttributeSet savedSdNativeViewAttrs = null;
	public int savedSdNativeViewStyle = 0;
	public LayoutParams savedGalleryViewLayout = null;
	public AttributeSet savedGalleryViewAttrs = null;
	public int savedGalleryViewStyle = 0;
	public LayoutParams savedHdImageViewLayout = null;
	public AttributeSet savedHdImageViewAttrs = null;
	public int savedHdImageViewStyle = 0;
	private ProgressDialog loadingChrome_PD = null;
	public Object mChromeView = null;
	final String chromeView_jscode = "(function(){'use strict';this.cast=this.cast||{};this.cast.__platform__=this.cast.__platform__||{};this.cast.__platform__.queryPlatformValue=function(key){var platform_values={'port-for-web-server':'8008','max-video-resolution-vpx':'1920x1080'};if(key in platform_values)return platform_values[key];};if ('function'==typeof Promise){this.cast.__platform__.setTouchInputSupport=function(s){var p=new Promise(function(resolve, reject){resolve();});return p;};}else{this.cast.__platform__.setTouchInputSupport={};}}).call(window);";
	public GalleryView mGalleryView = null;
	public HDVideoView hDVideoView = null;
	public SDVideoView sDVideoView = null;
	public HDImageView hDImageView = null; // Used in HDVideoView
	public SDNativeView sDNativeView = null;
	private int sb_last_progress = -1;
	private boolean sb_touch_progress = false;
	private LinearLayout metadataLayout = null;
	private LinearLayout metainfoLayout = null;
	private ImageView metadataCover = null;
	private TextView metadataTitle = null;
	private TextView metadataArtist = null;
	private TextView metadataAlbum = null;
	private TextView metadataLyric1 = null;
	private TextView metadataLyric2 = null;
	private TextView srtText = null;
	private RelativeLayout srtLayout = null;
	private RelativeLayout hostnameLayout = null;
	private TextView hostnameText = null, hostnameText2 = null, timetickText = null;
	private ImageButton playButton = null;
	private ImageButton stopButton = null;
	private ImageButton muteButton = null;
	private ImageButton scaleButton = null;
	private ImageButton settingButton = null;
	private ImageButton patternButton = null;
	private View focusedView = null;
	private SeekBar videoProgressBar = null;
	public LinearLayout ctrlLayout = null;
	public LinearLayout ctrlBtns = null;
	public LinearLayout ctrlSpeed = null;
	public ImageButton ctrlSpeedButton = null;
	private TextView ctrlSpeedText = null;
	public static String playSpeedStr[] = {"0.5X", "1X", "1.5X", "2X", "3X"};
	public static Float playSpeedVal[] = {0.5f, 1.0f, 1.5f, 2.0f, 3.0f};
	public int playSpeedIndex = 1;
	private TextView durationTView = null;
	private TextView currentPTView = null;
	private WaxPlayerLoading dialogLoading = null;
	private WaxPlayerLoading2 dialogLoading2 = null;
	public int dialogLoading2Count = 0;
	public WaxPlayerSpeed dialogSpeed = null;
	public WaxPlayerPattern dialogPattern = null;
	public WaxPlayerScale dialogScale = null;
	public WaxPlayerBtHidSelect dialogBtHidSelect = null;
	public boolean dialogBtHidSelectStarted = false;
	public int mBtHidDevWidth = 0, mBtHidDevHeight = 0, mBtHidDevStep = 0;
	public int mBtHidDevXPos = 0, mBtHidDevYPos = 0;
	public String mBtHidBondedDevMac = null;
	public CustomDialog dialogSetting = null;
	public CustomDialog dialogBackground = null;
	public CustomDialog dialogContinue = null;
	public RelativeLayout wholeLayout = null;
	private int wholeLayout_bordercolor = Color.BLACK;
	private TextView borderTop = null;
	private TextView borderBottom = null;
	private TextView borderLeft = null;
	private TextView borderRight = null;
	public boolean isFocused2 = false;
	public boolean isZoomed2 = false;
	public boolean isLocked2 = false;
	public boolean isMuted2 = false;
	private View wholeView = null;
	public TimerDialog promptDialog = null;
	private SrtParse Lyric_Parse = null, Lyric_Parse2 = null;
	private SrtParse Srt_Parse = null;
	private int lastSrtID = -1;
	private Timer tickTimer = null;
	private TimerTask tickTask = null;
	private Timer ctrlbarTimer = null;
	private TimerTask ctrlbarTask = null;
	public static boolean NETWORK_DEBUG_SHOW = false;
	private LayoutParams debugMsgViewLayout = null;
	private TextView debugMsgView = null;
	private final boolean AdMob_REMOVED = true;
	private View adMobView = null;
	private ImageView adMobView2 = null;
	private Timer adMobTimer = null;
	private TimerTask adMobTask = null;
	private int adMobLoadCount = 0;
	public static int adMobShowTimeout = 15*1000;
	private boolean adMobViewPrepared = false;
	private boolean adMobView2Prepared = false;
	public static int DRAWABLE_play = R.drawable.play;
	public static int DRAWABLE_play_on = R.drawable.play_on;
	public static int DRAWABLE_pause = R.drawable.pause;
	public static int DRAWABLE_pause_on = R.drawable.pause_on;

	public static final int PLAYER_NONE = 0;
	public static final int PLAYER_INNER = 1;
	public static final int PLAYER_VITAMIO = 2;
	public static final int PLAYER_EXTERN = 3;
	public int whichPlayer = Config.PLAYER_SWITCH;
	public boolean vitamioHwdecEnable = Config.VITAMIO_HWDEC_ENABLE;
	private int whichPlayerSaved = Config.PLAYER_SWITCH; // For Photo
	public static final int STATE_STOPPED = 0;
	public static final int STATE_PREPARING = 1;
	public static final int STATE_PREPARED = 2;
	public static final int STATE_PLAYING = 3;
	public static final int STATE_PAUSED = 4;
	public int playback_State = STATE_STOPPED;
	public int playback_timeout = 0;
	public int playback_sderror_retry = 0; // retry after SDVideoView.onError()
	private boolean need_Reset_State = false;
	public static final int LAYOUT_STRETCH = 1;
	public static final int LAYOUT_SCALE = 2;
	public static final int LAYOUT_ZOOM = 3;
	public int zoomState = Config.ZOOM;
	public static final int FMT_UNKNOWN = 0; // Belows must be same with AvPlayer.h
	public static final int FMT_VIDEO = 1; // Video + GIF
	public static final int FMT_AUDIO = 2;
	public static final int FMT_PHOTO = 3;
	public static final int FMT_MIRROR = 4; // DLNA Mirroring from PC/Android
	public static final int FMT_MIRROR2 = 5; // AirPlay Mirroring from iOS/MacOS
	public static final int FMT_MIRROR3 = 6; // GoogleCast Mirroring
	public static final int FMT_MIRROR4 = 7; // Huawei Cast+ Mirroring
	public static final int FMT_MIRROR5 = 8; // USB(iOS/Android) Mirroring
	public static final int FMT_DOC = 9; // App + Documents
	public int mediaFormat = FMT_UNKNOWN;
	public static final int PROTOCOL_AIRPLAY = 0x01;
	public static final int PROTOCOL_DLNA = 0x02;
	public static final int PROTOCOL_DIAL = 0x04;
	public static final int PROTOCOL_LOCAL = 0x08;
	public static final int PROTOCOL_ALL = PROTOCOL_AIRPLAY|PROTOCOL_DLNA|PROTOCOL_DIAL|PROTOCOL_LOCAL;
	public static final int PROTOCOL_MAX = PROTOCOL_LOCAL+1;
	public int mediaProto = PROTOCOL_AIRPLAY;
	public int mirrorServerType = -1;
	public static final int MinDuration = 800; // ms
	public static final int MaxDurationAV = 64*3600000; // AirTunes live stream
	public static final int RecDurationAV = 30*1000; // 30 seconds
	private static final int MaxDurationPHOTO = 1*3600000;

	private static final int MICE_MOVE = 1;
	private static final int MICE_R_CLICK = 2;
	private static final int MICE_L_DOWN = 3;
	private static final int MICE_L_UP = 4;
	private static final int MICE_DBL_CLICK = 5; // not used
	private static final int MICE_MOVETO = 6;
	private static final int SCROLL_MIN_THRESHOLD = 1;
	private static final int SCROLL_MAX_THRESHOLD = 100;
	private static final Object cursorLock = new Object();
	private static final int MICE_MOVE_DELTA = 5;
	private static final int MICE_DISPLAY_TO = 500;
	private long mice_SCROLL_LastTimestamp = 0;
	private boolean mice_isLongPress = false;
	private boolean mice_isDragMode = false;
	private boolean key_isLongPress = false;
	private int mice_moveCount = 0;
	public int AXIS_X_Range = -1, AXIS_Y_Range = -1;
	private int pressed_X = -1, pressed_Y = -1;
	public static boolean debug_InputEvent = false;
	public boolean mMarkScreenMode = false;
	public static boolean report_TouchEvent_WithoutUpCheck = true; // GestureDetector in Fragment doesn't have onSingleTapUp() for the last click
	public static boolean VERIFY_TOUCH_BY_MOTIONRANGE = false; // TouchArea -> ScreenArea
	private WindowManager.LayoutParams mCursorParams = null;
	private WindowManager mWinMgr = null; // It will always stay on screen
	private GestureDetector mGestureDetector = null;
	private ImageView mCursor1 = null;
	private ImageView mCursor2 = null;
	private ImageView lastCursor = null;
	private Timer cursorTimer = null;
	private TimerTask cursorTask = null;
	private Timer miceDragTimer = null;
	private TimerTask miceDragTask = null;

	public static int fontSize = 0;
	public static boolean ENABLE_PlayControlByKey = true; // Use LEFT/RIGHT/UP/DOWN/OK to control av play
	public static final boolean showPhotoLoading = true;
	public static final boolean stopBackground = true; // For Stop()
	public static final boolean playContinue = true;
	public static boolean enableBackground = true; // Audio in background
	public static boolean IgnoreMediaDuration = true; // duration < 0
	public static boolean decodeImageAsync = true; // For async load image
	public static boolean CURSOR_DIRECT_MODE = true;
	public static boolean CURSOR_DIRECT_MODE_SAVED = true;
	public static boolean sDNativeViewEnabled = true;
	private BroadcastReceiver HWS_BroadcastReceiver = null;
	private ImageView HWS_mWlanImageView = null;
    private Drawable HWS_mDrawableNetworkWorse = null;
    private Drawable HWS_mDrawableNetworkBad = null;
    private Drawable HWS_mDrawableNetworkGeneral = null;
    private Drawable HWS_mVectorAnimDrawableNetworkWorse = null;
    private Drawable HWS_mVectorAnimDrawableNetworkGeneral = null;
    private Drawable HWS_mVectorAnimDrawableNetworkBad = null;
    private boolean HWS_needVectorAnimShow = true;
    private Object HWS_animatedImageDrawable = null;
	public static boolean sDVideoViewEnabled = false;

	public WaxPlayer2() {
		// For exception while restoring activity after KILLED or goto HOME: 
		// "Unable to instantiate fragment" / "Make sure class name exists, is public, and has an empty constructor that is public"
	}

	public WaxPlayer2(Context _activityContext, int _fragment_id, String _client_id, FrameLayout _frame_layout) {
		activityContext = _activityContext;
		fragment_id = _fragment_id;
		client_id = _client_id;
		frame_layout = _frame_layout;

		waxHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false) {
					super.handleMessage(msg);
					return ; // Avoid of pendding UI request
				}
				switch (msg.what) {
				case MSG_UPDATE_AUDIO_METADATA: {
					if (metadataCover != null && metainfoLayout != null && metadataLayout != null) {
						int info_visible = View.INVISIBLE;
						if (music_cover != null && music_cover.length() > 0) {
							Bitmap mBitmap = BitmapFactory.decodeFile(music_cover, null);
							metadataCover.setImageBitmap(mBitmap);
						} else {
							metadataCover.setBackgroundResource(R.drawable.album_def);
						}
						Global.mySetAlpha(metadataCover, 0.8f);
						if (music_title != null && music_title.length() > 0) {
							metadataTitle.setText(music_title);
							info_visible = View.VISIBLE;
						} else {
							metadataTitle.setText("");
						}
						if (music_artist != null && music_artist.length() > 0) {
							metadataArtist.setText(music_artist);
							info_visible = View.VISIBLE;
						} else {
							metadataArtist.setText("");
						}
						if (music_album != null && music_album.length() > 0) {
							metadataAlbum.setText(music_album);
							info_visible = View.VISIBLE;
						} else {
							metadataAlbum.setText("");
						}
						if (lyric_url != null && lyric_url.length() > 0) {
							if (Lyric_Parse == null && Lyric_Parse2 == null) {
								Lyric_Parse2 = new SrtParse();
								new Thread() {
									public void run() {
										if (Lyric_Parse2.parseSRT2(lyric_url, 15000) > 0) {
											if (WaxPlayService.AIRPIN_IS_LITE == true && WaxPlayer.waxPlayService != null)
												WaxPlayer.waxPlayService.PostDisplayToast(8, false, WaxPlayer2.this.getContext().getString(R.string.set_srt_disabled_lite), 0);
											Lyric_Parse = Lyric_Parse2; // Async access conflict
											return ;
										}
										try { // Maybe Stop() have set Lyric_Parse2 null
											Lyric_Parse2.destroySRT();
											Lyric_Parse2 = null;
										} catch (Exception ex) {
										}
									}
								}.start();
							}
						} else {
							metadataLyric1.setText("");
							metadataLyric1.setVisibility(View.GONE);
							metadataLyric2.setText("");
							metadataLyric2.setVisibility(View.GONE);
						}
						metainfoLayout.setVisibility(info_visible);
						if (playback_State != STATE_STOPPED && mediaFormat == FMT_AUDIO && mediaProto == PROTOCOL_DLNA && 
							mGalleryView != null && mGalleryView.getVisibility() == View.VISIBLE) {
							metadataLayout.setVisibility(View.VISIBLE);
							metadataLayout.bringToFront();
							ResetFrontLayout();
						} else {
							metadataLayout.setVisibility(View.GONE);
						}
					} else {
						waxHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_METADATA, 500);
					}
				}	break;
				case MSG_DESTROY_ADMOB_ON_PLAY: {
					DestroyAdMob(false, true, true);
				}	break;
				case MSG_VIDEO_SCALE: {
					Scale(msg.arg1/*state*/, msg.arg2/*zoomflag*/);
				}	break;
				case MSG_VIDEOSIZE_CHANGE: {
					if (zoomState == LAYOUT_ZOOM && (videoWidth != msg.arg1 || videoHeight != msg.arg2))
						Scale(0, Config.ZOOM);/*Snoopy 11/19/2024 : restore state for rotation*/
					videoWidth = msg.arg1;
					videoHeight = msg.arg2;
					boolean updateFmt = false;
					if (mediaFormat == FMT_AUDIO && (videoWidth > 0 || videoHeight > 0)) {
						mediaFormat = FMT_VIDEO;
						updateFmt = true;
					} else if (mediaFormat == FMT_VIDEO && (videoWidth <= 0 && videoHeight <= 0)) {
						// On some hardware, some video format is always with zero dimension
						//mediaFormat = FMT_AUDIO; // Unknown format is treated as VIDEO
						//updateFmt = true;
					}
					if (updateFmt == true)
						updateIconPlay(1, 0);
				}	break;
				case MSG_BUFFER_UPDATE: {
					if (msg.arg1 != lastBuffProg) {
						lastBuffProg = msg.arg1;
						if (lastBuffProg < 0)
							lastBuffProg = 0;
						if (lastBuffProg > 100)
							lastBuffProg = 100;
						if (STATUS_LOADING == 1 && dialogLoading != null)
							dialogLoading.updateUI(lastBuffProg);
						WaxPlayService.send_MediaBuffering(client_id, lastBuffProg);
					}
				}	break;
				case MSG_TITLE_UPDATE: {
					if (STATUS_LOADING == 1 && dialogLoading != null)
						dialogLoading.updateUI((String)msg.obj);
				}	break;
				case MSG_REMOVE_CURSOR: {
					removeCursor(); // Force remove
				}	break;
				case MSG_HIDE_LOADING: {
					closeStartProgress();
				}	break;
				case MSG_SHOW_LOADING: {
					showStartProgress(0);
				}	break;
				case MSG_HIDE_LOADING2: {
					closeStartProgress2(0);
				}	break;
				case MSG_SHOW_LOADING2: {
					showStartProgress2();
				}	break;
				case MSG_UPDATE_CPOSITION: {
					UpdatePG();
				}	break;
				case MSG_SHOW_TYPEICON: {
				}	break;
				case MSG_UPDATE_DURATION/*Enter STATE_PLAYING*/: {
					waxHandler.sendEmptyMessage(MSG_SHOW_TYPEICON);
					UpdateDT();
				}	break;
				case MSG_CHECK_BTHID_INIT: {
					int delay = 1000;
					if (WaxPlayService.BTHID_SERV > 0 && isExiting() == false && inBackground == 0) {
						if (dialogBtHidSelectStarted == false && Global.checkBtHidDevAvailable() <= 0) {
							WaxPlayService.checkInitBtHidAll(WaxPlayer2.this.getActivity(), -1, true);
							delay = 3000;
						} else if (dialogBtHidSelectStarted == false && dialogBtHidSelect == null && Global.checkBtHidDevConnected() == false ) {
							dialogBtHidSelect = new WaxPlayerBtHidSelect(WaxPlayer2.this.getContext(),
									R.layout.waxplayer_bthidselect, Global.RES_style_WaxDialog, screenWidth, screenHeight, WaxPlayer2.this);
							dialogBtHidSelect.show(); // MoveDialog2() is excuted in WaxPlayerBtHidSelect.java
							dialogBtHidSelectStarted = true;
						}
						if (remoteCtrlSupport() == 2) {
							mBtHidBondedDevMac = Global.mBtSelectedDevMac;
							//mBtHidDevStep = 5;
							//mBtHidDevWidth = 600;
							//mBtHidDevHeight = 1000;
							//mBtHidDevXPos = 2*mBtHidDevWidth;
							//mBtHidDevYPos = 2*mBtHidDevHeight;
							if (mBtHidDevWidth > 0 && mBtHidDevHeight > 0 && mBtHidDevStep > 0) {
								BtHidMouseGotoXY(0, 0);
								//BtHidMouseGotoXY(mBtHidDevWidth-1, mBtHidDevHeight-1); /*TEST*/
							} else {
								CURSOR_DIRECT_MODE = false;
							}
						} else {
							waxHandler.sendEmptyMessageDelayed(MSG_CHECK_BTHID_INIT, delay);
						}
					}
				}	break;
				case MSG_START_SEEK: {
					if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING || isExiting() == true)
						break; // During Stop() or Prepare()
					if (msg.arg1 < 0 || (GetDuration() > 0 && msg.arg1 > GetDuration()))
						break;
					//if (msg.arg2 == 1)
					//	showStartProgress(msg.arg2);
					//else {
						dialogLoading2Count ++;
						showStartProgress2();
					//}
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Start Seekto " + msg.arg1);
					long _start = System.currentTimeMillis();
					synchronized (ctrlLock) {
						if (playback_State == STATE_PAUSED) {
							playback_State = STATE_PLAYING; // Force Playing: Seeking during Paused
							if (WaxPlayer.waxPlayService != null)
								WaxPlayer.waxPlayService.MediaResume(client_id); // start() is called after seekTo()
							WaxPlayService.send_MediaResumed(client_id);
						}
						send_updateIconPlay(0, 0, -1);
						try {
							if (whichPlayer == PLAYER_INNER && hDVideoView != null) {
								hDVideoView.seekTo(msg.arg1);
								hDVideoView.start();
							} else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null) {
								sDVideoView.seekTo(msg.arg1);
								sDVideoView.start();
							}
							//if (mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO)
								WaxPlayService.requestAudioFocus();
						} catch (Exception e) {
						}
					}
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Start Seekto done in " + (System.currentTimeMillis() - _start) + " msec");
				}	break;
				case MSG_DECODE_IMAGE_DONE: {
					if (isExiting() == false && inBackground == 0 && hDImageView != null) {
						int sessionId = msg.arg1;
						hDImageView.switchToNext(sessionId);
					}
				}	break;
				case MSG_SWITCH_EXTPLAYER: {
					try {
						if (url != null) {
							Intent intent = new Intent(Intent.ACTION_VIEW);
							Uri _uri = Uri.parse(url);
							if (mediaFormat == FMT_AUDIO)
								intent.setDataAndType(_uri, "audio/mpeg");
							else if (mediaFormat == FMT_PHOTO)
								intent.setDataAndType(_uri, "image/jpeg");
							else
								intent.setDataAndType(_uri, "video/mp4");
							Global.startActivityWrap(WaxPlayer2.this.getActivity(), intent);
						}
					} catch (Exception e) {
						WaxPlayService.displayToast2(8, false, getString(R.string.waxplayer_toast_extplayer_fail));
					}
					EnterExit();
					gotoFinish(0);
				}	break;
				case MSG_OPEN_DOC: {
					String filePath = msg.obj.toString();
					EnterExit();
					WaxPlayService.OpenDocumentWithPath(WaxPlayer2.this.getActivity(), filePath);
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......DLNA_DOC (" + filePath + ")");
					gotoFinish(0);
				}	break;
				case MSG_SWITCH_PLAYER: {
					if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING || isExiting() == true)
						break; // During Stop() or Prepare()
					showStartProgress(0);
				}
				case MSG_START_PLAY: {
					if (playback_State == STATE_STOPPED) {
						updateSrtText(-1); // Clear the SRT layout
						updateLyricText(-1, true);
					}
					//ctrlLayout.invalidate(); // TODO: Why getBottom() is not updated after redraw?
					//if (screenHeight < ctrlLayout.getBottom()) // For Amlogic Android 4.0.4 based LeTV-T1
					//	screenHeight = ctrlLayout.getBottom();
					if (inBackground == 1)
						break;
					if (wholeLayout == null)
						break;
					isMuted2 = false;
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Start Prepare for "+whichPlayer);
					long _start = System.currentTimeMillis();
					synchronized (ctrlLock) {
						if (mediaFormat == FMT_MIRROR4) {
							DestroyAllVideoViews();
							playback_State = STATE_PLAYING;
							initHwCastView();
						} else if (mediaProto == PROTOCOL_DIAL && mediaFormat == FMT_VIDEO/*ChromeView*/) {
							DestroyAllVideoViews();
							playback_State = STATE_PREPARING;
							initChromeView(url);
							exitOnBackKey2 = true;
						} else {
							if (whichPlayer == PLAYER_VITAMIO && WaxPlayService.FORCE_HWDEC_FOR_VITAMIO == true)
								vitamioHwdecEnable = (android.os.Build.VERSION.SDK_INT >= 16) ? true : Config.VITAMIO_HWDEC_ENABLE;
							try {
								if (playback_sderror_retry == 0) {
									if (whichPlayer == PLAYER_INNER)
										initHdVideoView();
									else if (whichPlayer == PLAYER_VITAMIO)
										initSdVideoView();
								} else {
									sDVideoView.stopPlayback(inBackground);
								}
								if (need_Reset_State == true)
									playback_State = STATE_PREPARING;//STATE_STOPPED; // After initVideoView()
								need_Reset_State = false;
								if (playback_State == STATE_STOPPED && requestFinished > 0) {
									playback_State = STATE_PREPARING;
									Global.do_sleep(50); // Schedule Play()
								}
								if (whichPlayer == PLAYER_INNER) {
									url = (url_hd != null && url_hd.length() > 0) ? url_hd : url;
									if (userAgent != null && userAgent.length() > 0) // For AirPlay from 'http://movietrailers.apple.com'
										hDVideoView.setUserAgent(userAgent);
									hDVideoView.setVideoPath(url);
									if (seekTime > 0)
										hDVideoView.seekTo(seekTime);
									hDVideoView.start();
								} else if (whichPlayer == PLAYER_VITAMIO) {
									url = (url_sd != null && url_sd.length() > 0) ? url_sd : url;
									if (mediaFormat == FMT_PHOTO) {
										byte[] data = WaxPlayService.pd;
										if (url.startsWith("bytearray://") && data != null && data.length > 0) {
											url = url.substring("bytearray://".length());
											try	{ // Vitamio doesn't support byte array
												File ofile = new File(url);
												ofile.createNewFile();
												OutputStream output = new FileOutputStream(ofile);
												output.write(data, 0, data.length);
												output.close();
											} catch (FileNotFoundException e) {
											} catch (IOException e) {
											}
										}
									}
									if (userAgent != null && userAgent.length() > 0) // For AirPlay from 'http://movietrailers.apple.com'
										sDVideoView.setUserAgent(userAgent);
									sDVideoView.setVideoPath(url);
									if (mediaFormat == FMT_AUDIO)
										sDVideoView.setBufferSize(32*1024); // Low delay for Audio
									if (seekTime > 0)
										sDVideoView.seekTo(seekTime);
									sDVideoView.start();
								}
							} catch (Exception e) {
								whichPlayer = PLAYER_NONE; // Exit Play()/Seek()
							}
							playSpeedIndex = 1;
							waxHandler.sendEmptyMessage(MSG_UPDATE_PLAYSPEED);
						}
					}
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Start Prepare done in " + (System.currentTimeMillis() - _start) + " msec");
				}	break;
				case MSG_INNERTEXT_UPDATE: {
					if (Srt_Parse == null && Lyric_Parse == null) { // Outside subtitle is preferred
						String textStr = msg.obj.toString();
						Spanned srtStr = Html.fromHtml(textStr);
						updateSrtTextView(srtStr);
					}
				}	break;
				case MSG_UPDATE_DBGMSG: {
					try {
						if (debugMsgViewLayout == null) {
							debugMsgViewLayout = debugMsgView.getLayoutParams();
							debugMsgViewLayout.width = screenWidth/2;
							debugMsgViewLayout.height = screenHeight/2;
							debugMsgView.setLayoutParams(debugMsgViewLayout);
						}
						wholeLayout.bringChildToFront(debugMsgView);
						LayoutAdMob();
						debugMsgView.append(msg.obj.toString());
						int offset = debugMsgView.getLineCount() * debugMsgView.getLineHeight();
						if (offset > debugMsgView.getHeight() - debugMsgView.getLineHeight())
							debugMsgView.scrollTo(0, offset - debugMsgView.getHeight());
					} catch (Exception e) {
					}
				}	break;
				case MSG_HDPLAYER_FAILURE: {
					String str = msg.obj.toString();
					//WaxPlayService.displayToast2(8, false, getString(R.string.waxplayer_toast_switchdecode) + str);
				}	break;
				case MSG_SDPLAYER_FAILURE: {
					closeStartProgress();
					updateIconStop(0);
					WaxPlayer.put_stopFromUser(client_id, mediaProto, 0); // update it
					gotoStop(false, 1, true);
					if (isExiting())
						ClearExit();
					Message msgInform = Message.obtain();
					msgInform.what = MSG_DELAY_EXIT;
					String str = msg.obj.toString();
					if (str != null && str.length() > 0)
						msgInform.obj = null;//getString(R.string.waxplayer_toast_failure) + str;
					waxHandler.sendMessageDelayed(msgInform, WaxPlayService.finishTimerTimeout); // Schedule Play()/Seek(), delay finish
					if (Config.LOG_DEBUG == true) { // Output the log
						try {
							Runtime.getRuntime().exec("logcat -d -f /mnt/sdcard/AirPin_debug.txt");
						} catch (IOException e1) {
						}
					}
				}	break;
				case MSG_DELAY_EXIT: {
					if (isExiting() == false)
						EnterExit();
					if (msg.obj != null) {
						String str = msg.obj.toString();
						if (str != null && str.length() > 0)
							WaxPlayService.displayToast2(8, false, str);
					}
					gotoFinish(0);
				}	break;
				case MSG_UPDATE_ICON_PLAY: {
					updateIconPlay(msg.arg1, msg.arg2);
				}	break;
				case MSG_UPDATE_ICON_END: {
					updateIconStop(msg.arg1);
				}	break;
				case MSG_PLAY_DONE: {
					//WaxPlayService.displayToast2(8, false, R.string.waxplayer_toast_finished));
					if (mediaFormat == FMT_PHOTO) {
						updateIconStop(0);
					} else {
						//WaxPlayer.put_stopFromUser(client_id, mediaProto, 1); // update it
						if (mediaFormat == FMT_VIDEO /* Skip MP3 with VBR */ && 
							duration > RecDurationAV && duration < MaxDurationAV && lastPos < duration - RecDurationAV)
							break; // For XunLei Kankan with Skyworth E700S
						if (isExiting() == false) {
							EnterExit();
							gotoStop(true, 1, true);
						}
					}
				}	break;
				case MSG_GOTO_FINISH: {
					DoodleView_Quit(true);
					if (fragmentRunning == true) {
						if (isExiting() == false) // Should be TRUE
							EnterExit();
						if (hDImageView != null && mediaFormat == FMT_PHOTO)
							hDImageView.finish_destroy();
						Message msgInform = Message.obtain();
						msgInform.what = WaxPlayer.MSG_DEL_PLAYER;
						msgInform.arg1 = fragment_id;
						msgInform.obj = client_id;
						if (WaxPlayService.mediaplayer != null)
							WaxPlayService.mediaplayer.mainHandler.sendMessage(msgInform);
						fragmentRunning = false;
					}
				}	break;
				case MSG_GOTO_RESTART: {
					//WaxPlayService.displayToast2(8, false, getString(R.string.waxplayer_toast_restart));
					gotoFinish(0);
					//Global.SelfRestart(5000, null);
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"] Restart DONE ...");
				}	break;
				case MSG_PENDING_MICE_L_UP: {
					if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true || !is_Playing2())
						break;
					if (STATUS_CLAYOUT_HIDE == 1 && remoteCtrlSupport() > 0) {
						if (CURSOR_DIRECT_MODE == true)
							updateCursor(mCursor2, MICE_L_UP, msg.arg1, msg.arg2, MICE_DISPLAY_TO);
						else if (remoteCtrlSupport() == 2)
							updateCursor(mCursor2, MICE_L_UP, 0, 0, MICE_DISPLAY_TO);
					}
				}	break;
				case MSG_PENDING_MICE_L_CLICK: {
					onCursorLeftClick(msg.arg1, msg.arg2, false);
				}	break;
				case MSG_UPDATE_PLAYSPEED: {
					if ((mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO ) && ( (
						whichPlayer == PLAYER_INNER && android.os.Build.VERSION.SDK_INT >= 26) || whichPlayer == PLAYER_VITAMIO ) ) {
						ctrlSpeed.setVisibility(View.VISIBLE);
						ctrlSpeedText.setText(playSpeedStr[playSpeedIndex]);
						if (WaxPlayService.AIRPIN_IS_LITE == true) {
							ctrlSpeedButton.setEnabled(false);
							ctrlSpeedButton.setFocusable(false);
						} else {
							ctrlSpeedButton.setEnabled(true);
							ctrlSpeedButton.setFocusable(true);
						}
					} else {
						ctrlSpeed.setVisibility(View.GONE);
					}
				}	break;
				case MSG_PROMPT_DIALOG_PLAY: {
					try {
						promptDialog = new TimerDialog(WaxPlayer2.this.getContext(), WaxPlayer2.this);
						promptDialog.setTitle("UNKNOWN");
						if (msg.arg1 == PROTOCOL_AIRPLAY)
							promptDialog.setTitle("AirPlay");
						else if (msg.arg1 == PROTOCOL_DLNA)
							promptDialog.setTitle("DLNA");
						else if (msg.arg1 == PROTOCOL_DIAL)
							promptDialog.setTitle("DIAL");
						promptDialog.setMessage(getString(R.string.waxplayer_prompt_dlg_play_msg1) + msg.obj.toString() +
												getString(R.string.waxplayer_prompt_dlg_play_msg2));
						promptDialog.setButtonType(Dialog.BUTTON_NEGATIVE, msg.arg2, true);
						promptDialog.show(); // Must before Dialog.getButton()
						MoveDialog2(promptDialog.mDialog, Global.RES_id_adg_bgview, true);
					} catch (Exception e) {
					}
				}	break;
				case MSG_DRAW_SCREENSHOT: {
					try {
						if (WaxPlayService.airplayerLoaded == true && Global.checkSU() == true) {
							new Thread() {
								public void run() {
									String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
									if (path.lastIndexOf('/') == path.length()-1)
										path = path.substring(0, path.lastIndexOf('/'));
									SimpleDateFormat timesdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
									String filename = "ScreenCap_" + timesdf.format(new Date()).toString() + ".png";
									String filepath = path + "/" + filename;
									String fullcmd = "screencap -p "+filepath;
									int ret = WaxPlayService.rcmd(fullcmd, 1);
									boolean fileexist = true;//new File(filepath)).exists();
									if (ret >= 0 && fileexist == true) {
										if (WaxPlayer.waxPlayService != null)
											WaxPlayer.waxPlayService.PostDisplayToast(8, false, WaxPlayer2.this.getContext().getString(R.string.alert_drawscreencap_saved)+filename, 0);
										Uri saveUri = Uri.fromFile(new File(filepath));
										Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, saveUri);
										WaxPlayer2.this.getContext().sendBroadcast(mediaScanIntent);
									}
								}
							}.start();
						}
					} catch (Exception e1) {
					} catch (Throwable e2) {
					}
					try {
						((com.waxrain.droidsender.doodle.DoodleMenu)WaxPlayService.mediaplayer.mDoodleMenu).RestoreAllMenus();
						WaxPlayService.mediaplayer.mDoodleHide = false;
					} catch (Exception e) {
					}
				}	break;
				case MSG_SHOW_HOSTNAME: {
					if (hostnameText != null && hostnameLayout != null) {
						if (hostname != null && hostname.length() > 0 && isZoomed2 == false && WaxPlayService.MAX_PLAYERS_N1 > 1) {
							hostnameText.setText(hostname);
							hostnameLayout.setVisibility(View.VISIBLE);
							hostnameLayout.bringToFront();
						} else {
							hostnameLayout.setVisibility(View.GONE);
						}
					} else {
						send_showHostname(hostname, 500);
					}
				}	break;
				default:
					break;
				}
				super.handleMessage(msg);
			}
		};
	}

	public int remoteCtrlSupport() {
		if (WaxPlayService.AIRPIN_IS_LITE == false && zoomState != LAYOUT_ZOOM) {
			if (WaxPlayService.BTHID_SERV > 0 && Global.checkBtHidDevConnected() && (mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || mediaFormat == FMT_MIRROR4) )
				return 2;
			if (WaxPlayService.ENABLE_REMOTECONTROL == true && mediaFormat == FMT_MIRROR)
				return 1;
		}
		return 0;
	}

	private void ShowBorder(boolean is_show) {
		if (borderTop == null || borderBottom == null || borderLeft == null || borderRight == null)
			return ;
		borderTop.setBackgroundColor(wholeLayout_bordercolor);
		borderBottom.setBackgroundColor(wholeLayout_bordercolor);
		borderLeft.setBackgroundColor(wholeLayout_bordercolor);
		borderRight.setBackgroundColor(wholeLayout_bordercolor);
		if (is_show == false) {
			borderTop.setVisibility(View.GONE);
			borderBottom.setVisibility(View.GONE);
			borderLeft.setVisibility(View.GONE);
			borderRight.setVisibility(View.GONE);
		} else {
			borderTop.setVisibility(View.VISIBLE);
			borderBottom.setVisibility(View.VISIBLE);
			borderLeft.setVisibility(View.VISIBLE);
			borderRight.setVisibility(View.VISIBLE);
			borderTop.bringToFront();
			borderBottom.bringToFront();
			borderLeft.bringToFront();
			borderRight.bringToFront();
		}
	}

	public boolean DoodleView_Enter() {
		try {
			((com.waxrain.droidsender.doodle.DoodleMenu)WaxPlayService.mediaplayer.mDoodleMenu).setDoodleMenuListener(new com.waxrain.droidsender.doodle.DoodleMenu$DoodleMenuListener() {
				@Override
				public void onDrawMenuClick(int shown) {
					if (is_Playing2() == false)
						return ;
					if (shown != 0) {
						Global.DoodleViewControl(WaxPlayService.mediaplayer, "markstart"+",");
						CURSOR_DIRECT_MODE_SAVED = CURSOR_DIRECT_MODE;
						CURSOR_DIRECT_MODE = true;
						mMarkScreenMode = true;
					} else {
						DoodleView_Quit(false);
					}
				}
				@Override
				public void onDrawTypeChoice(int type) {
					if (is_Playing2() == false)
						return ;
					if (type == 1)
						Global.DoodleViewControl(WaxPlayService.mediaplayer, "marksettype"+","+"type=rect");
					else if (type == 2)
						Global.DoodleViewControl(WaxPlayService.mediaplayer, "marksettype"+","+"type=doodle");
				}
				@Override
				public void onDrawColorChoice(int color) {
					if (is_Playing2() == false)
						return ;
					Global.DoodleViewControl(WaxPlayService.mediaplayer, "marksetpen"+","+"col="+(color&0x00ffffff));
				}
				@Override
				public void onDrawSizeChoice(int size) {
					if (is_Playing2() == false)
						return ;
					Global.DoodleViewControl(WaxPlayService.mediaplayer, "marksetpen"+","+"wid="+size);
				}
				@Override
				public void onDrawScreenShot() {
					if (is_Playing2() == false)
						return ;
					try {
						((com.waxrain.droidsender.doodle.DoodleMenu)WaxPlayService.mediaplayer.mDoodleMenu).HideAllMenus();
						WaxPlayService.mediaplayer.mDoodleHide = true;
					} catch (Exception e) {
					}
					waxHandler.removeMessages(MSG_DRAW_SCREENSHOT);
					waxHandler.sendEmptyMessageDelayed(MSG_DRAW_SCREENSHOT, 50);
				}
				@Override
				public void onDrawRevertPath() {
					if (is_Playing2() == false)
						return ;
					Global.DoodleViewControl(WaxPlayService.mediaplayer, "markback"+",");
				}
				@Override
				public void onDrawClearPath() {
					if (is_Playing2() == false)
						return ;
					Global.DoodleViewControl(WaxPlayService.mediaplayer, "markclear"+",");
				}
				@Override
				public void onDrawKeyPress(int keycode) {
					if (is_Playing2() == false)
						return ;
					// HOME/MENU/BACK keys
				}
			});
			return true;
		} catch (Exception ex) {
		}
		return false;
	}

	private void DoodleView_Quit(boolean clear) {
		if (mMarkScreenMode == true) {
			if (WaxPlayService.mediaplayer != null && WaxPlayService.mediaplayer.mDoodleMenu != null)
				WaxPlayService.mediaplayer.DoodleView_End(clear, false);
			CURSOR_DIRECT_MODE = CURSOR_DIRECT_MODE_SAVED;
			mMarkScreenMode = false;
		}
	}

	public void gotFocus2(boolean highlight) {
		isFocused2 = true;
		if (highlight == true) {
			if (isLocked2 == true)
				wholeLayout_bordercolor = 0xFFFFC1C1;/*LIGHT PINK*/
			else
				wholeLayout_bordercolor = 0xFFFFFF99;/*LIGHT YELLOW*/
		} else {
			wholeLayout_bordercolor = Color.BLACK;
		}
		ShowBorder(true);
	}

	public void lostFocus2() {
		isFocused2 = false;
		wholeLayout_bordercolor = Color.BLACK;
		ShowBorder(false);
	}

	public void gotZoom2() {
		isZoomed2 = true;
		send_showHostname(hostname, 10);
	}

	public void lostZoom2() {
		isZoomed2 = false;
		send_showHostname(hostname, 10);
		if (WaxPlayService.mediaplayer != null) {
			DoodleView_Quit(true);
			int zoom_state = WaxPlayService.mediaplayer.CHECKZOOM_MPlayer(fragment_id-1, false);
			if (zoom_state == 1)
				isZoomed2 = true;
		}
	}

	public void gotLock2() {
		isLocked2 = true;
		if (isZoomed2 == false && isFocused2 == true)
			gotFocus2(true);
	}

	public void lostLock2() {
		isLocked2 = false;
		if (isZoomed2 == false && isFocused2 == true)
			gotFocus2(true);
	}

	@Override
	public void onPause() {
		Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onPause called");
		super.onPause();
		closeSubDialogs(0); // Stopped but child dialog openned
		removeCursor();
		if (inBackground == 0) {
			if (WaxPlayService.exitOnActivityPause == 1) {
				if (isExiting() == false) {
					EnterExit();
					if (playback_State != STATE_STOPPED)
						gotoStop(false, 1, true);
				} else {
					int timeout = 3000;
					while (whichPlayer != PLAYER_NONE && timeout > 0) {
						Global.do_sleep(50);
						timeout -= 50;
					}
					Global.do_sleep(500); // Wait Stop() finished
				}
				if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == true)
					gotoFinish(0);
			} else if (WaxPlayService.exitOnActivityPause == 0) {
				inBackground = 1; // Do set it before Stop() to let Play() get right result
				if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true)
					return ;
				if (mediaFormat != FMT_PHOTO && mediaFormat != FMT_UNKNOWN) {
					EnterExit();
					//WaxPlayer.put_stopFromUser(client_id, mediaProto, 1);
					if (mGalleryView != null) // For Audio Streaming
						DestroyGalleryView();
					if (playback_State != STATE_STOPPED) { // Pause() on onPause(), Pause() will be done in Play() while LOADING
						gotoStop(false, 1, false);
						if (WaxPlayer.waxPlayService != null)
							WaxPlayer.waxPlayService.MediaPause(client_id);
					}
					//if (WaxPlayService.AudioPlayerMap.size() > 0 && mediaFormat == FMT_MIRROR2/*AirParrot doesn't TEARDOWN connection*/)
					//	WaxPlayService.AO_streamAudioFocused = false;
					ClearExit();
				} else {
					HideControllor(); // For FMT_PHOTO
				}
			}
		}
	}

	@Override
	public void onResume() {
		Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onResume called");
		super.onResume();
		if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true)
			return ;
		if (WaxPlayService.exitOnActivityPause == 0 && inBackground != 0) {
			isResuming = true;
			inBackground = 0; // Do reset it before Play() to let Play()/UI get right state
			if (mediaFormat != FMT_PHOTO && mediaFormat != FMT_UNKNOWN) {
				if (playback_State != STATE_STOPPED) // Music gotoBackground() playing
					gotoStop(false, 1, false);
				if (playback_State == STATE_STOPPED && mediaFormat != FMT_PHOTO) { // Resume() on onResume()
					new Thread() {
						public void run() {
							waxHandler.sendEmptyMessage(MSG_SHOW_LOADING);
							if (Play(url, "", 0.0f, 0, contentSize, title, userAgent) == 1) {
								if (WaxPlayer.waxPlayService != null) {
									WaxPlayer.waxPlayService.ResumeClientByProto(client_id, WaxPlayer2.PROTOCOL_AIRPLAY|WaxPlayer2.PROTOCOL_DLNA); /*Snoopy 8/18/2023 added for audio*/
									WaxPlayer.waxPlayService.MediaResume(client_id);
								}
							}
							isResuming = false;
						}
					}.start();
				} else {
					isResuming = false;
				}
			} else {
				isResuming = false;
			}
		}
	}

	@Override
	public void onDestroy() {
		Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onDestroy called");
		DestroyAdMob(true, true, false);
		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onCreate called");
		wholeView = inflater.inflate(R.layout.waxplayer_main2, container, false);
		wholeView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				int[] screenPos = new int[2];
				wholeView.getLocationOnScreen(screenPos);
				int new_x = screenPos[0];
				int new_y = screenPos[1];
				int new_w = wholeView.getWidth();
				int new_h = wholeView.getHeight();
				if (screenXoff != new_x || screenYoff != new_y || screenWidth != new_w || screenHeight != new_h)
					my_onScreenSizeChanged(new_x, new_y, new_w, new_h);
				//wholeView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});
		/*wholeView.getViewTreeObserver().addOnGlobalFocusChangeListener(new OnGlobalFocusChangeListener() {
			@Override
			public void onGlobalFocusChanged(View oldFocus, View newFocus) {
				Log.i(LOG_TAG,"onFocusChanged = "+oldFocus+"/"+newFocus);
			}
        });*/
		return wholeView;
	}

	private int BtHidMouseEvent(int click, int dx, int dy, int wheels) {
		int retVal = Global.sendBtHidMouseReport(WaxPlayer2.this.getContext(), mBtHidBondedDevMac, click, dx, dy, wheels);
		if (retVal > 0) {
			mBtHidDevXPos += dx;
			if (mBtHidDevXPos < 0)
				mBtHidDevXPos = 0;
			if (mBtHidDevXPos >= mBtHidDevWidth)
				mBtHidDevXPos = mBtHidDevWidth - 1;
			mBtHidDevYPos += dy;
			if (mBtHidDevYPos < 0)
				mBtHidDevYPos = 0;
			if (mBtHidDevYPos >= mBtHidDevHeight)
				mBtHidDevYPos = mBtHidDevHeight - 1;
			Global.do_sleep(1);
		}
		return retVal;
	}

	private void BtHidMouseGotoXY(int xDst, int yDst) {
		while (mBtHidDevXPos != xDst || mBtHidDevYPos != yDst) {
			int dx = xDst - mBtHidDevXPos;
			if (dx < 0 && dx < -mBtHidDevStep)
				dx = -mBtHidDevStep;
			else if (dx > 0 && dx > mBtHidDevStep)
				dx = mBtHidDevStep;
			int dy = yDst - mBtHidDevYPos;
			if (dy < 0 && dy < -mBtHidDevStep)
				dy = -mBtHidDevStep;
			else if (dy > 0 && dy > mBtHidDevStep)
				dy = mBtHidDevStep;
			BtHidMouseEvent(0, dx, dy, 0);
		}
	}

	private void destroyMiceLeftClickTimer() {
		waxHandler.removeMessages(MSG_PENDING_MICE_L_CLICK);
	}

	private void recreateMiceLeftClickTimer(int x, int y, int delay) {
		destroyMiceLeftClickTimer();
		Message msgInform = Message.obtain();
		msgInform.arg1 = x;
		msgInform.arg2 = y;
		msgInform.what = MSG_PENDING_MICE_L_CLICK;
		waxHandler.sendMessageDelayed(msgInform, delay);
	}

	private void destroyMiceDragTimer() {
		if (miceDragTimer != null) {
			miceDragTimer.cancel();
			miceDragTimer = null;
		}
	}

	private void recreateMiceDragTimer(int timeo) {
		destroyMiceDragTimer();
		if (miceDragTimer == null) {
			miceDragTimer = new Timer(true);
			try {
				miceDragTask = new TimerTask() {
					@Override
					public void run() {
						Message msgInform = Message.obtain();
						msgInform.arg1 = pressed_X;
						msgInform.arg2 = pressed_Y;
						msgInform.what = MSG_PENDING_MICE_L_UP;
						waxHandler.sendMessage(msgInform);
					}
				};
				miceDragTimer.schedule(miceDragTask, timeo);
			} catch (IllegalArgumentException ex) {
			} catch (IllegalStateException ex) {
			}
		}
	}

	private void destroyCursorTimer() {
		if (cursorTimer != null) {
			cursorTimer.cancel();
			cursorTimer = null;
		}
	}

	private void recreateCursorTimer(int destroyTo) {
		destroyCursorTimer();
		if (cursorTimer == null) {
			cursorTimer = new Timer(true);
			try {
				cursorTask = new TimerTask() {
					@Override
					public void run() {
						waxHandler.sendEmptyMessage(MSG_REMOVE_CURSOR);
					}
				};
				cursorTimer.schedule(cursorTask, destroyTo);
			} catch (IllegalArgumentException ex) {
			} catch (IllegalStateException ex) {
			}
		}
	}

	private void removeCursor() {
		if (CURSOR_DIRECT_MODE == false)
			return;
		synchronized (cursorLock) {
			destroyCursorTimer();
			if (lastCursor != null) {
				mWinMgr.removeView(lastCursor);
				lastCursor = null;
			}
		}
	}

	private void onCursorLeftClick(int x, int y, boolean key_emu) {
		if (key_emu == true || CURSOR_DIRECT_MODE == false) {
			updateCursor(mCursor1, MICE_L_DOWN, 0, 0, 0);
			updateCursor(mCursor2, MICE_L_UP, 0, 0, MICE_DISPLAY_TO);
		} else if (CURSOR_DIRECT_MODE == true) {
			updateCursor(mCursor1, MICE_L_DOWN, x, y, 0);
			updateCursor(mCursor2, MICE_L_UP, x, y, MICE_DISPLAY_TO);
		}
	}

	private void onCursorRightClick(int x, int y, boolean key_emu) {
		if (key_emu == true || CURSOR_DIRECT_MODE == false)
			updateCursor(mCursor2, MICE_R_CLICK, 0, 0, MICE_DISPLAY_TO);
		else if (CURSOR_DIRECT_MODE == true)
			updateCursor(mCursor2, MICE_R_CLICK, x, y, MICE_DISPLAY_TO);
	}

	private void updateCursor(ImageView iv, int event, int x, int y, int destroyTo) {
		int absCursor_Xs = -1, absCursor_Xe = -1;
		int absCursor_Ys = -1, absCursor_Ye = -1;
		if (sDNativeView != null) {
			absCursor_Xs = sDNativeView.absCursor_Xs;
			absCursor_Xe = sDNativeView.absCursor_Xe;
			absCursor_Ys = sDNativeView.absCursor_Ys;
			absCursor_Ye = sDNativeView.absCursor_Ye;
		}
		int w = absCursor_Xe - absCursor_Xs + 1;
		int h = absCursor_Ye - absCursor_Ys + 1;
		if (w <= 1 || h <= 1)
			return ;
		if (VERIFY_TOUCH_BY_MOTIONRANGE == true && (AXIS_X_Range > 0 && AXIS_Y_Range > 0)) {
			x = x*w/AXIS_X_Range;
			y = y*h/AXIS_Y_Range;
		}
		if (CURSOR_DIRECT_MODE == true) {
			//if (event != MICE_MOVE && (x < absCursor_Xs || x > absCursor_Xe || y < absCursor_Ys || y > absCursor_Ye))
			//	return ;
			removeCursor();
			synchronized (cursorLock) {
				mCursorParams.x = x;
				mCursorParams.y = y;
				if (activityContext != null && ((WaxPlayer)activityContext).MPlayerZoomed < 0) {
					//mWinMgr.addView(iv, mCursorParams);
					//lastCursor = iv;
				}
			}
			//if (destroyTo > 0)
			//	recreateCursorTimer(destroyTo);
			if (event != MICE_MOVE) { /*maybe click released outside of view*/
				x = x - absCursor_Xs;
				y = y - absCursor_Ys;
				if (x <= 0)
					x = 1;
				if (y <= 0)
					y = 1;
				if (x >= w)
					x = w - 1;
				if (y >= h)
					y = h - 1;
			}
		}
		if (sDVideoView != null) {
			if (debug_InputEvent == true)
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] PutMiceEvent: " + event + "@" + x + "," + y + "/" + w + "x" + h);
			if (remoteCtrlSupport() == 2) {
				int click = 0; // 0=Move/1=Left/2=Right/3=Middle
				if (event == MICE_L_DOWN || (event == MICE_MOVE && mice_isDragMode == true) ) {
					click = 1;
					if (CURSOR_DIRECT_MODE == true && event == MICE_L_DOWN) {
						int xDst = ((x*mBtHidDevWidth/w+mBtHidDevStep-1)/mBtHidDevStep)*mBtHidDevStep;
						int yDst = ((y*mBtHidDevHeight/h+mBtHidDevStep-1)/mBtHidDevStep)*mBtHidDevStep;
						BtHidMouseGotoXY(xDst, yDst);
						if (BtHidMouseEvent(1, 0, 0, 0) > 0)
							return ;
					}
				} else if (event == MICE_L_UP) {
					x = y = 0;
				} else if (event == MICE_R_CLICK) {
					click = 2;
					BtHidMouseEvent(click, -x, -y, 0);
					x = y = 0;
					click = 0;
				}
				if (BtHidMouseEvent(click, -x, -y, 0) > 0)
					return ;
			}
			sDVideoView.putMiceEvent(event, x, y, w, h);
		}
	}

	private void sendKeyEvent(int KeyCode) {
		if (sDVideoView != null) {
			if (debug_InputEvent == true)
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] PutKeyEvent: " + KeyCode);
			if (remoteCtrlSupport() == 2) {
				int ModifierByte = 0;
				if (Global.sendBtHidKeyReport(WaxPlayer2.this.getContext(), mBtHidBondedDevMac, ModifierByte, KeyCode) > 0)
					return ;
			}
			sDVideoView.putKeyEvent(0, KeyCode);
		}
	}

	public void my_onScreenSizeChanged(int x, int y, int w, int h) {
		screenXoff = x;
		screenYoff = y;
		screenWidth = w;
		screenHeight = h;
		if (wholeLayout == null) {
			Log.i(LOG_TAG,"MPlayer["+fragment_id+"] initUI = "+screenWidth+"x"+screenHeight+"@"+screenXoff+","+screenYoff);
			initUI();
		} else {
			Log.i(LOG_TAG,"MPlayer["+fragment_id+"] Resize = "+screenWidth+"x"+screenHeight+"@"+screenXoff+","+screenYoff);
			closeSubDialogs(0); // Should be repainted in right position
			Scale(2, zoomState);
		}
		updateAudioMetadataLayout();
		updateSubtitleTextSize();
		if (hDVideoView != null)
			savedHdVideoViewLayout = hDVideoView.getLayoutParams();
		if (sDVideoView != null)
			savedSdVideoViewLayout = sDVideoView.getLayoutParams();
		if (sDNativeView != null)
			savedSdNativeViewLayout = sDNativeView.getLayoutParams();
	}

	public void DestroyAdMob(boolean destroy_view, boolean destroy_timer, boolean reset_state) {
		if (AdMob_REMOVED == true)
			return ;
	}

	public void AfterShowAdMobs() {
		adMobTimer = new Timer(true);
		try {
			adMobTask = new TimerTask() {
				@Override
				public void run() {
					try { // runOnUiThread() on a null object reference
						if (WaxPlayer2.this != null && WaxPlayer2.this.fragmentRunning == true) {
							WaxPlayer2.this.getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									DestroyAdMob(true, false, false);
								}
							});
						}
					} catch (Exception ex) {
					} catch (Throwable th) {
					}
				}
			};
			adMobTimer.schedule(adMobTask, adMobShowTimeout);
		} catch (IllegalArgumentException ex) {
		} catch (IllegalStateException ex) {
		}
	}

	public void StartAdMob(boolean recreate) {
		if (adMobLoadCount == 0/*first time*/ || (recreate == true && adMobView == null && adMobView2 == null)/*ShowControlBar*/) {
			if (WaxPlayService.AIRPIN_IS_LITE == true) {
				DestroyAdMob(true, true, false);
				ShowAdMob();
				adMobLoadCount ++;
			}
		}
	}

	public void LayoutAdMob() {
		if (adMobView != null && adMobViewPrepared == true)
			wholeLayout.bringChildToFront(adMobView);
		if (adMobView2 != null && adMobView2Prepared == true)
			wholeLayout.bringChildToFront(adMobView2);
	}

	public void ShowAdMob2() {
		adMobView2 = (ImageView) this.getView().findViewById(R.id.ad_view2);
		int adw = 555, adh = 166;
		float ratio = (float)screenWidth / (float)adw;
		if (ratio > 2.0f || ratio < 1.0f) {
			try {
				LayoutParams adlp = adMobView2.getLayoutParams();
				adlp.width = screenWidth/2;
				adlp.height = adlp.width*adh/adw;
				adMobView2.setLayoutParams(adlp);
			} catch (Exception e) {
			}
		}
		adMobView2.setVisibility(View.VISIBLE);
		Global.mySetAlpha(adMobView2, 0.9f);
		adMobView2Prepared = true;
		LayoutAdMob();
		AfterShowAdMobs();
	}

	public void ShowAdMob() {
		if (AdMob_REMOVED == true)
			return ;
	}

	private void initUI() {
		// HDVideoView() is constructed in setContentView()
		savedHdVideoViewAttrs = null;
		savedHdVideoViewStyle = 0;
		savedSdVideoViewAttrs = null;
		savedSdVideoViewStyle = 0;
		savedSdNativeViewAttrs = null;
		savedSdNativeViewStyle = 0;
		savedGalleryViewAttrs = null;
		savedGalleryViewStyle = 0;
		savedHdImageViewAttrs = null;
		savedHdImageViewStyle = 0;
		hDVideoView = (HDVideoView)this.getView().findViewById(R.id.HDVideoView);
		savedHdVideoViewLayout = hDVideoView.getLayoutParams();
		savedHdVideoViewAttrs = hDVideoView.savedViewAttrs;
		savedHdVideoViewStyle = hDVideoView.savedViewStyle;
		hDVideoView.mediaplayer = this;
		sDVideoView = (SDVideoView)this.getView().findViewById(R.id.SDVideoView);
		savedSdVideoViewLayout = sDVideoView.getLayoutParams();
		savedSdVideoViewAttrs = sDVideoView.savedViewAttrs;
		savedSdVideoViewStyle = sDVideoView.savedViewStyle;
		sDVideoView.mediaplayer = this;
		mGalleryView = (GalleryView)this.getView().findViewById(R.id.GalleryView);
		savedGalleryViewLayout = mGalleryView.getLayoutParams();
		savedGalleryViewAttrs = mGalleryView.savedViewAttrs;
		savedGalleryViewStyle = mGalleryView.savedViewStyle;
		mGalleryView.mediaplayer = this;
		hDImageView = (HDImageView)this.getView().findViewById(R.id.HDImageView);
		savedHdImageViewLayout = hDImageView.getLayoutParams();
		savedHdImageViewAttrs = hDImageView.savedViewAttrs;
		savedHdImageViewStyle = hDImageView.savedViewStyle;
		hDImageView.mediaplayer = this;
		if (sDNativeViewEnabled == true) {
			sDNativeView = (SDNativeView)this.getView().findViewById(R.id.SDNativeView);
			savedSdNativeViewLayout = sDNativeView.getLayoutParams();
			savedSdNativeViewAttrs = sDNativeView.savedViewAttrs;
			savedSdNativeViewStyle = sDNativeView.savedViewStyle;
			sDNativeView.mediaplayer = this;
		}
		if (NETWORK_DEBUG_SHOW == true) {
			try {
				debugMsgView = (TextView)this.getView().findViewById(R.id.debugText);
				debugMsgView.setMovementMethod(ScrollingMovementMethod.getInstance());
				debugMsgView.setVisibility(View.VISIBLE);
				Global.mySetAlpha(debugMsgView, 0.8f);
			} catch (Exception e) {
			}
			new Thread() {
				public void run() {
					try {
						while (WaxPlayService.mediaplayer != null && WaxPlayService.mediaplayer.activityRunning == true && !is_Playing2())
							Global.do_sleep(50); // Wait for starting
						Uri _uri = Uri.parse(url);
						final Process p = Runtime.getRuntime().exec("ping " + _uri.getHost());
						new Thread(new Runnable() {
							public void run() {
								try {
									BufferedReader bReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(p.getErrorStream()), "UTF-8"));
									String line = "";
									while((line=bReader.readLine()) != null)
										Thread.sleep(10);
									bReader.close();
								} catch(Exception ex) {
								}
							}
						} ).start();
						InputStream input = p.getInputStream();
						BufferedReader in = new BufferedReader(new InputStreamReader(input));
						String outputLine = "";
						while (WaxPlayService.mediaplayer != null && WaxPlayService.mediaplayer.activityRunning == true && (outputLine = in.readLine()) != null)
							send_debugMsgUpdated("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "]" + outputLine + "\r\n");
						p.destroy();
					} catch (Exception e) {
					}
				}
			}.start();
		}
		mWinMgr = (WindowManager)this.getActivity().getBaseContext().getSystemService(Context.WINDOW_SERVICE);
		mCursor1 = new ImageView(this.getActivity().getBaseContext());
		mCursor1.setImageResource(R.drawable.cursor1);
		mCursor2 = new ImageView(this.getActivity().getBaseContext());
		mCursor2.setImageResource(R.drawable.cursor2);
		mCursorParams = new WindowManager.LayoutParams();
		mCursorParams.gravity = Gravity.LEFT | Gravity.TOP;
		mCursorParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mCursorParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		mCursorParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		mCursorParams.format = PixelFormat.TRANSLUCENT;
		mCursorParams.type = WindowManager.LayoutParams.TYPE_TOAST;
		wholeLayout = (RelativeLayout)this.getView().findViewById(R.id.WholeLayout);
		wholeLayout.setFocusable(false);
		wholeLayout.setFocusableInTouchMode(false);
		wholeLayout.setOnTouchListener(this);
		ctrlLayout = (LinearLayout)this.getView().findViewById(R.id.ctrl_layout);
		ctrlBtns = (LinearLayout)this.getView().findViewById(R.id.ctrl_button_layout_btns);
		ctrlSpeed = (LinearLayout)this.getView().findViewById(R.id.ctrl_playspeed_layout);
		ctrlSpeedButton = (ImageButton)this.getView().findViewById(R.id.ctrl_playspeed_icon);
		ctrlSpeedText = (TextView)this.getView().findViewById(R.id.ctrl_playspeed_list);
		setControllorTransparent(true);
		srtLayout = (RelativeLayout)this.getView().findViewById(R.id.srt_layout);
		srtText = (TextView)this.getView().findViewById(R.id.srtText);
		//srtText.getPaint().setFakeBoldText(true);
		updateSubtitleTextSize();
		borderTop = (TextView)this.getView().findViewById(R.id.borderTop);
		borderBottom = (TextView)this.getView().findViewById(R.id.borderBottom);
		borderLeft = (TextView)this.getView().findViewById(R.id.borderLeft);
		borderRight = (TextView)this.getView().findViewById(R.id.borderRight);
		ShowBorder(isFocused2);
		metadataLayout = (LinearLayout)this.getView().findViewById(R.id.music_metadata_layout);
		metainfoLayout = (LinearLayout)this.getView().findViewById(R.id.music_metainfo);
		metadataTitle = (TextView)this.getView().findViewById(R.id.music_title);
		metadataArtist = (TextView)this.getView().findViewById(R.id.music_artist);
		metadataAlbum = (TextView)this.getView().findViewById(R.id.music_album);
		metadataLyric1 = (TextView)this.getView().findViewById(R.id.music_lyric1);
		metadataLyric2 = (TextView)this.getView().findViewById(R.id.music_lyric2);
		metadataCover = (ImageView)this.getView().findViewById(R.id.music_cover);
		if (WaxPlayService.ActivityFloatWindowMode == true) {
			WaxPlayService.noSettingButton = true;
			WaxPlayService.noPatternButton = true;
			WaxPlayService.noPatternButton = true;
		}
		durationTView = (TextView)this.getView().findViewById(R.id.duration);
		currentPTView = (TextView)this.getView().findViewById(R.id.currentpos);
		durationTView.setText("--:--:--");
		currentPTView.setText("00:00:00");
		if (fontSize <= 0)
			fontSize = (int)durationTView.getTextSize();
		hostnameLayout = (RelativeLayout)this.getView().findViewById(R.id.hostname_layout);
		GradientDrawable hostLayout_bg = new GradientDrawable();
		hostLayout_bg.setShape(GradientDrawable.RECTANGLE);
		hostLayout_bg.setCornerRadius(10);
		hostLayout_bg.setColor(Color.argb(0x4F,0x99,0xCC,0xCC)/*be save with waxplayer_main2.xml*/);
		hostnameLayout.setBackgroundDrawable(hostLayout_bg);
		hostnameText = (TextView)this.getView().findViewById(R.id.hostnameText);
		hostnameText2 = (TextView)this.getView().findViewById(R.id.hostnameText2);
		if (Config.DOODLE_VIEW == 1) { /*WRAP_CONTENT makes doodle menu flicking*/
			try { 
				currentPTView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
				LinearLayout.LayoutParams txtlp = (LinearLayout.LayoutParams)currentPTView.getLayoutParams();
				txtlp.width = currentPTView.getMeasuredWidth();//"00:00:00".length()*fontSize;
				currentPTView.setLayoutParams(txtlp);
			} catch (Exception ex) {
			}
		}
		playButton = (ImageButton)this.getView().findViewById(R.id.playbutton);
		stopButton = (ImageButton)this.getView().findViewById(R.id.stopbutton);
		muteButton = (ImageButton)this.getView().findViewById(R.id.mutebutton);;
		scaleButton = (ImageButton)this.getView().findViewById(R.id.scalebutton);
		patternButton = (ImageButton)this.getView().findViewById(R.id.patternbutton);
		settingButton = (ImageButton)this.getView().findViewById(R.id.settingbutton);
		if (WaxPlayService.noPatternButton) {
			scaleButton.setEnabled(false);
			scaleButton.setFocusable(false);
			scaleButton.setVisibility(View.GONE);
		}
		if (WaxPlayService.noPatternButton) {
			patternButton.setEnabled(false);
			patternButton.setFocusable(false);
			patternButton.setVisibility(View.GONE);
		}
		if (WaxPlayService.noSettingButton) {
			settingButton.setEnabled(false);
			settingButton.setFocusable(false);
			settingButton.setVisibility(View.GONE);
		}
		setListener();
		videoProgressBar = (SeekBar)this.getView().findViewById(R.id.progressbar);
		if (WaxPlayService.noSeekingButton) {
			videoProgressBar.setEnabled(false);
			videoProgressBar.setFocusable(false);
			videoProgressBar.setVisibility(View.INVISIBLE);
		}
		videoProgressBar.setProgress(0);
		videoProgressBar.setMax(0);
		videoProgressBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					recreateCtrlbarTimer();
					sb_last_progress = progress;
					String currText = Global.GetTimeString(progress/1000);
					currentPTView.setText(currText);
					if (sb_touch_progress == false) {
						send_seekRequest(sb_last_progress, 0);
						sb_last_progress = -1;
					}
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				sb_touch_progress = true;
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if (sb_last_progress >= 0 && sb_last_progress <= videoProgressBar.getMax()) {
					send_seekRequest(sb_last_progress, 0);
					sb_last_progress = -1;
				}
				sb_touch_progress = false;
			}
		});
		if (WaxPlayService.noControllorBar == true) {
			ctrlLayout.setEnabled(false);
			ctrlLayout.setFocusable(false);
			ctrlLayout.setVisibility(View.GONE);
			STATUS_CLAYOUT_HIDE = 1;
		}

		mGestureDetector = new GestureDetector(this.getContext(), new OnGestureListener() {
			@Override
			public boolean onDown(MotionEvent e) {
				if (debug_InputEvent == true)
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onDown: " + (int)e.getRawX() + "," + (int)e.getRawY());
				if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true || !is_Playing2())
					return false;
				if (STATUS_CLAYOUT_HIDE == 1 && remoteCtrlSupport() > 0) {
					mice_isLongPress = false;
					mice_isDragMode = false;
					pressed_X = -1;
					pressed_Y = -1;
					mice_moveCount = 0;
					if (WaxPlayService.TOUCH_SINGLETAPUP_SUPPORT == false)
						recreateMiceLeftClickTimer((int)e.getRawX(), (int)e.getRawY(), 1500);
					return true; // MUST return true in Fragment
				}
				return false;
			}
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				if (debug_InputEvent == true)
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onFling: " + (int)e2.getRawX() + "," + (int)e2.getRawY());
				if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true || !is_Playing2())
					return false;
				if (STATUS_CLAYOUT_HIDE == 1 && remoteCtrlSupport() > 0) {
					destroyMiceDragTimer();
					destroyMiceLeftClickTimer();
					if (mice_isDragMode == true) { // Already sent MICE_L_DOWN
						if (CURSOR_DIRECT_MODE == true)
							updateCursor(mCursor2, MICE_L_UP, (int)e2.getRawX(), (int)e2.getRawY(), MICE_DISPLAY_TO);
						else if (remoteCtrlSupport() == 2)
							updateCursor(mCursor2, MICE_L_UP, 0, 0, MICE_DISPLAY_TO);
					}
					removeCursor();
					//return true;
				}
				return false;
			}
			@Override
			public void onLongPress(MotionEvent e) {
				if (debug_InputEvent == true)
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onLongPress: " + (int)e.getRawX() + "," + (int)e.getRawY());
				if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true || !is_Playing2())
					return ;
				if (STATUS_CLAYOUT_HIDE == 1 && remoteCtrlSupport() > 0) {
					mice_isLongPress = true;
					//if (WaxPlayService.TOUCH_SINGLETAPUP_SUPPORT == false || report_TouchEvent_WithoutUpCheck == true) {
						destroyMiceLeftClickTimer(); // Should be called after 500ms since onDown()
						onCursorRightClick((int)e.getRawX(), (int)e.getRawY(), false);
					//}
				}
			}
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				if (debug_InputEvent == true)
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onScroll: " + (int)e2.getRawX() + "," + (int)e2.getRawY() + " / " + (int)distanceX + "," + (int)distanceY);
				if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true || !is_Playing2())
					return false;
				if (STATUS_CLAYOUT_HIDE == 1 && remoteCtrlSupport() > 0) {
					mice_moveCount ++;
					if (Math.abs((int)distanceX) < SCROLL_MIN_THRESHOLD && Math.abs((int)distanceY) < SCROLL_MIN_THRESHOLD)
						return false;//true;
					if (Math.abs((int)distanceX) > SCROLL_MAX_THRESHOLD || Math.abs((int)distanceY) > SCROLL_MAX_THRESHOLD)
						return false;//true;
					if (mice_moveCount < WaxPlayService.mice_moveInitDropCount)
						return false;//true;
					if (CURSOR_DIRECT_MODE == true) {
						if (pressed_X != -1 && pressed_Y != -1 && mice_isDragMode == false) {
							mice_isDragMode = true; // Will not have onSingleTapUp() but onFling()
							updateCursor(mCursor1, MICE_L_DOWN, pressed_X, pressed_Y, 0);
						}
					}
					//long currTimestamp = System.currentTimeMillis();
					//if (Math.abs(currTimestamp - mice_SCROLL_LastTimestamp) < 50)
					//	return false;
					//mice_SCROLL_LastTimestamp = currTimestamp;
					pressed_X = (int)e2.getRawX();
					pressed_Y = (int)e2.getRawY();
					recreateMiceDragTimer(700);
					if (mice_moveCount > 1) {
						if (CURSOR_DIRECT_MODE == true)
							updateCursor(mCursor1, MICE_MOVETO, pressed_X, pressed_Y, 0);
						else
							updateCursor(mCursor1, MICE_MOVE, (int)distanceX, (int)distanceY, MICE_DISPLAY_TO);
					}
					//return true;
				}
				return false;
			}
			@Override
			public void onShowPress(MotionEvent e) {
				if (debug_InputEvent == true)
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onShowPress: " + (int)e.getRawX() + "," + (int)e.getRawY());
				if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true || !is_Playing2())
					return ;
				if (STATUS_CLAYOUT_HIDE == 1 && remoteCtrlSupport() > 0) {
					pressed_X = (int)e.getRawX();
					pressed_Y = (int)e.getRawY();
					removeCursor();
					if (CURSOR_DIRECT_MODE == false) {
						mice_isDragMode = true;
						updateCursor(mCursor1, MICE_L_DOWN, 0, 0, 0);
					}
				}
			}
			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				if (debug_InputEvent == true)
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onSingleTapUp: " + (int)e.getRawX() + "," + (int)e.getRawY());
				if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true || !is_Playing2())
					return false;
				if (STATUS_CLAYOUT_HIDE == 1 && remoteCtrlSupport() > 0) {
					destroyMiceLeftClickTimer();
					if (WaxPlayService.TOUCH_SINGLETAPUP_SUPPORT == false)
						WaxPlayService.TOUCH_SINGLETAPUP_SUPPORT = true;
					if (mice_isLongPress == true)
						onCursorRightClick((int)e.getRawX(), (int)e.getRawY(), false);
					else
						onCursorLeftClick((int)e.getRawX(), (int)e.getRawY(), false);
					//return true;
				}
				return false;
			}
		});
		mGestureDetector.setOnDoubleTapListener(new OnDoubleTapListener() {
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				if (debug_InputEvent == true)
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onDoubleTap: " + (int)e.getRawX() + "," + (int)e.getRawY());
				if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true || !is_Playing2())
					return false;
				if (STATUS_CLAYOUT_HIDE == 1 && remoteCtrlSupport() > 0) {
					//updateCursor(mCursor2, MICE_DBL_CLICK, 0, 0, MICE_DISPLAY_TO);
					if (report_TouchEvent_WithoutUpCheck == true)
						onCursorLeftClick((int)e.getRawX(), (int)e.getRawY(), false); // onSingleTapUp() -> onDoubleTap()
					//return true;
				}
				return false;
			}
			@Override
			public boolean onDoubleTapEvent(MotionEvent e) {
				return false;
			}
			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				return false;
			}
		});

		if (WaxPlayService.mediaplayer != null)
			WaxPlayService.mediaplayer.newPlayer = this;
	}

	private void updateAudioMetadataLayout() {
		LinearLayout.LayoutParams metainfoLP = new LinearLayout.LayoutParams(metainfoLayout.getLayoutParams());
		LayoutParams coverLP = metadataCover.getLayoutParams();
		if (screenWidth > screenHeight) {
			metadataLayout.setOrientation(LinearLayout.HORIZONTAL);
			metadataLayout.setGravity(Gravity.CENTER_VERTICAL);
			coverLP.width = coverLP.height = (screenWidth/4);
			metainfoLP.width = (screenWidth>400)?(screenWidth/2-100):(screenWidth/2);
			metainfoLP.leftMargin = (int)metadataLyric2.getTextSize()*2;
			metainfoLP.topMargin = 0;
		} else {
			metadataLayout.setOrientation(LinearLayout.VERTICAL);
			metadataLayout.setGravity(Gravity.CENTER_HORIZONTAL);
			coverLP.width = coverLP.height = (screenWidth/2);
			metainfoLP.width = (screenWidth>200)?(screenWidth-200):screenWidth;
			metainfoLP.leftMargin = 0;
			metainfoLP.topMargin = (int)metadataLyric2.getTextSize()*2;
		}
		metainfoLayout.setLayoutParams(metainfoLP);
		metadataCover.setLayoutParams(coverLP);
	}

	private void updateSubtitleTextSize() {
		if (srtText != null) {
			float srtTextSize = (float)screenWidth/35F;
			srtText.setTextSize(TypedValue.COMPLEX_UNIT_PX, srtTextSize); // Fix text size
			srtText.setShadowLayer(srtTextSize/5F, 0F, 0F, Color.BLACK);
		}
		if (debugMsgView != null) {
			float dbgTextSize = (float)screenWidth/160F;
			debugMsgView.setTextSize(TypedValue.COMPLEX_UNIT_PX, dbgTextSize);
		}
	}

	private void setListener() {
		playButton.setOnClickListener(this);
		stopButton.setOnClickListener(this);
		muteButton.setOnClickListener(this);
		if (WaxPlayService.noPatternButton == false)
			scaleButton.setOnClickListener(this);
		if (WaxPlayService.noPatternButton == false)
			patternButton.setOnClickListener(this);
		if (WaxPlayService.noSettingButton == false)
			settingButton.setOnClickListener(this);
		ctrlSpeedButton.setOnClickListener(this);
		ctrlSpeedText.setOnClickListener(this);
		playButton.setOnFocusChangeListener(this);
		stopButton.setOnFocusChangeListener(this);
		muteButton.setOnFocusChangeListener(this);
		if (WaxPlayService.noPatternButton == false)
			scaleButton.setOnFocusChangeListener(this);
		if (WaxPlayService.noPatternButton == false)
			patternButton.setOnFocusChangeListener(this);
		if (WaxPlayService.noSettingButton == false)
			settingButton.setOnFocusChangeListener(this);
		ctrlSpeedButton.setOnFocusChangeListener(this);
		ctrlSpeedButton.setNextFocusUpId(R.id.progressbar);
		ctrlSpeedButton.setNextFocusLeftId(R.id.settingbutton);
		ctrlSpeedButton.setNextFocusRightId(R.id.stopbutton);
	}

	@Override
	public void onClick(View v) {
		if (isFocused2 == false) {
			if (WaxPlayService.mediaplayer != null)
				WaxPlayService.mediaplayer.SWITCHTO_MPlayer(fragment_id-1, true);
			return;
		}
		if (inBackground != 0)
			return ;
		if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true)
			return ;
		if (STATUS_CLAYOUT_HIDE == 1) {
			if (v.getId() == wholeView.getId() || v.getId() == R.id.WholeLayout || v.getId() == R.id.HDVideoView || v.getId() == R.id.SDVideoView || v.getId() == R.id.GalleryView || v.getId() == R.id.HDImageView || v.getId() == R.id.SDNativeView)
				ShowControllor(true);/*Snoopy 11/20/2024 : first OK key is dealed as stop button click*/
			return ;
		}
		int id = v.getId();
		if (id == R.id.playbutton) {
			if (playback_State != STATE_PLAYING && playback_State != STATE_PAUSED)
				return;
			if (is_Playing()) {
				Pause(true, 1, 0);
				if (WaxPlayer.waxPlayService != null)
					WaxPlayer.waxPlayService.MediaPause(client_id);
			} else {
				Resume(1);
				if (WaxPlayer.waxPlayService != null)
					WaxPlayer.waxPlayService.MediaResume(client_id);
			}
		} else if (id == R.id.stopbutton) {
			if (isExiting() == false) {
				WaxPlayer.put_stopFromUser(client_id, mediaProto, 1);
				EnterExit();
				gotoStop(false, 1, true);
				gotoFinish(0);
			}
		} else if (id == R.id.mutebutton) {
			if (WaxPlayer.waxPlayService != null)
				WaxPlayer.waxPlayService.setPlayerMute(client_id, !isMuted2);
		} else if (id == R.id.scalebutton) {
			if (playback_State != STATE_PLAYING && playback_State != STATE_PAUSED)
				return;
			dialogScale = new WaxPlayerScale(WaxPlayer2.this.getContext(),
					R.layout.waxplayer_scale_n1, Global.RES_style_WaxDialog, zoomState, screenWidth, screenHeight, isZoomSupport(), this);
			dialogScale.show(); // MoveDialog2() is excuted in WaxPlayerScale.java
		} else if (id == R.id.patternbutton) {
			dialogPattern = new WaxPlayerPattern(WaxPlayer2.this.getContext(),
					R.layout.waxplayer_pattern, Global.RES_style_WaxDialog, whichPlayerSaved,
					vitamioHwdecEnable, sDVideoViewEnabled, 1, screenWidth, screenHeight, 1, this);
			dialogPattern.show(); // MoveDialog2() is excuted in WaxPlayerPattern.java
		} else if (id == R.id.settingbutton) {
			gotoSetting();
		} else if (id == R.id.ctrl_playspeed_list || id == R.id.ctrl_playspeed_icon) {
			dialogSpeed = new WaxPlayerSpeed(WaxPlayer2.this.getContext(),
					R.layout.waxplayer_speed, Global.RES_style_WaxDialog, screenWidth, screenHeight, this);
			dialogSpeed.show(); // MoveDialog2() is excuted in WaxPlayerSpeed.java
		}
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (inBackground != 0)
			return ;
		int id = v.getId();
		if (id == R.id.playbutton) {
			if (hasFocus == true) {
				if (is_Playing())
					playButton.setBackgroundDrawable(getResources().getDrawable(DRAWABLE_pause_on));
				else
					playButton.setBackgroundDrawable(getResources().getDrawable(DRAWABLE_play_on));
			} else {
				if (is_Playing())
					playButton.setBackgroundDrawable(getResources().getDrawable(DRAWABLE_pause));
				else
					playButton.setBackgroundDrawable(getResources().getDrawable(DRAWABLE_play));
			}
		} else if (id == R.id.stopbutton) {
			if (hasFocus == true)
				stopButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.stop_on));
			else
				stopButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.stop));
		} else if (id == R.id.mutebutton) {
			if (hasFocus == true) {
				if (isMuted2 == true)
					muteButton.setBackgroundDrawable(getResources().getDrawable(RES_drawable_mute_on));
				else
					muteButton.setBackgroundDrawable(getResources().getDrawable(RES_drawable_mute2_on));
			} else {
				if (isMuted2 == true)
					muteButton.setBackgroundDrawable(getResources().getDrawable(RES_drawable_mute));
				else
					muteButton.setBackgroundDrawable(getResources().getDrawable(RES_drawable_mute2));
			}
		} else if (id == R.id.scalebutton) {
			if (hasFocus == true)
				scaleButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.scale_on));
			else
				scaleButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.scale));
		} else if (id == R.id.patternbutton) {
			if (hasFocus == true)
				patternButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.pattern_on));
			else
				patternButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.pattern));
		} else if (id == R.id.settingbutton) {
			if (hasFocus == true)
				settingButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.setting_on));
			else
				settingButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.setting));
		} else if (id == R.id.ctrl_playspeed_icon) {
			if (hasFocus == true)
				ctrlSpeedButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.speed_on));
			else
				ctrlSpeedButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.speed_off));
		}
	}

	private void showStartProgress(int mode) {
		if (inBackground != 0)
			return ;
		if (activityContext != null && ((WaxPlayer)activityContext).MPlayerZoomed >= 0)
			return ;
		if (WaxPlayService.ActivityFloatWindowMode == true)
			return ;
		if (WaxPlayService.noLoadingUi == true)
			return ;
		if (mode != 2)
			closeRebufferProgress(false);
		closeStartProgress2(1);
		if (mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || mediaFormat == FMT_MIRROR4)
			return ;
		if (STATUS_LOADING == 0) {
			int titleString = R.string.waxplayer_toast_loading;
			if (mode == 1)
				titleString = R.string.waxplayer_toast_continuation;
			focusedView = ctrlLayout.getFocusedChild();
			dialogLoading = new WaxPlayerLoading(WaxPlayer2.this.getContext(), 
					0, R.style.WaxDialog2/*R.style.WaxDialog*/, titleString, screenWidth, screenHeight, this);
			if (mode == 2) {
				//dialogLoading.setCancelable(true);
				dialogLoading.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						if (dialogLoading != null) {
							//dialogLoading.cancel();
							dialogLoading.dismiss();
							dialogLoading = null;
							STATUS_REBUFFERING = STATUS_LOADING = 0;
						}
					}
				});
				dialogLoading.setOnKeyListener(new DialogInterface.OnKeyListener() {
					@Override
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) && event.getAction() == KeyEvent.ACTION_DOWN) {
							if (dialogLoading != null) {
								dialogLoading.cancel();
								dialogLoading.dismiss();
								dialogLoading = null;
								STATUS_REBUFFERING = STATUS_LOADING = 0;
							}
							//return true; // Will cause parent deal with it again
						}
						return false;
					}
				});
			}
			dialogLoading.show(); // MoveDialog2() is excuted in WaxPlayerLoading.java
			STATUS_LOADING = 1;
		}
	}

	private void closeStartProgress() {
		if (inBackground != 0)
			return ;
		if (WaxPlayService.ActivityFloatWindowMode == true)
			return ;
		//closeStartProgress2(1); // Seek/onSeekCompletion/Stop/onError/onCompletion
		if (STATUS_LOADING == 1) {
			if (dialogLoading != null) {
				dialogLoading.cancel();
				dialogLoading.dismiss();
				dialogLoading = null;
			}
			if (STATUS_CLAYOUT_HIDE == 0 && isFocused2 == true) {
				if (focusedView != null && focusedView.isEnabled())
					focusedView.requestFocus();
				else
					ctrlLayout.requestFocus();
			}
			STATUS_LOADING = 0;
		}
	}

	private void showStartProgress2() {
		if (inBackground != 0)
			return ;
		if (activityContext != null && ((WaxPlayer)activityContext).MPlayerZoomed >= 0)
			return ;
		if (WaxPlayService.ActivityFloatWindowMode == true)
			return ;
		if (showPhotoLoading == false)
			return ;
		closeRebufferProgress(false);
		if (dialogLoading2 == null) {
			focusedView = ctrlLayout.getFocusedChild();
			dialogLoading2 = new WaxPlayerLoading2(WaxPlayer2.this.getContext(), 0, R.style.WaxDialog2, screenWidth, screenHeight, this);
			dialogLoading2.setCancelable(false);
			dialogLoading2.show(); // MoveDialog2() is excuted in WaxPlayerScale.java
		}
	}

	private void closeStartProgress2(int forceClose) {
		if (forceClose == 1)
			dialogLoading2Count = 0;
		if (dialogLoading2Count > 0)
			dialogLoading2Count --;
		if (inBackground != 0)
			return ;
		if (WaxPlayService.ActivityFloatWindowMode == true)
			return ;
		if (showPhotoLoading == false)
			return ;
		if (dialogLoading2Count == 0) {
			if (waxHandler != null) // Called from onCreate() -> DoRealFinish(true)
				waxHandler.removeMessages(MSG_SHOW_LOADING2); // remove pending msg
			if (dialogLoading2 != null)	{
				dialogLoading2.cancel();
				dialogLoading2.dismiss();
				dialogLoading2 = null;
				if (forceClose == 0) { // Exiting
					if (STATUS_CLAYOUT_HIDE == 0 && isFocused2 == true) {
						if (focusedView != null && focusedView.isEnabled())
							focusedView.requestFocus();
						else
							ctrlLayout.requestFocus();
					}
				}
			}
		}
	}

	public void closeSubDialogs(int isStarting) {
		if (inBackground != 0)
			return ;
		if (WaxPlayService.ActivityFloatWindowMode == true)
			return ;
		if (dialogSpeed != null) {
			dialogSpeed.cancel();
			dialogSpeed.dismiss();
			dialogSpeed = null;
		}
		if (dialogPattern != null) {
			dialogPattern.cancel();
			dialogPattern.dismiss();
			dialogPattern = null;
		}
		if (dialogScale != null) {
			dialogScale.cancel();
			dialogScale.dismiss();
			dialogScale = null;
		}
		if (dialogBtHidSelect != null) {
			dialogBtHidSelect.cancel();
			dialogBtHidSelect.dismiss();
			dialogBtHidSelect = null;
		}
		if (dialogSetting != null) {
			dialogSetting.cancel();
			dialogSetting.dismiss();
			dialogSetting = null;
		}
		if (dialogBackground != null) {
			dialogBackground.cancel();
			dialogBackground.dismiss();
			dialogBackground = null;
		}
		if (dialogContinue != null) {
			dialogContinue.cancel();
			dialogContinue.dismiss();
			dialogContinue = null;
		}
		if (promptDialog != null) {
			promptDialog.cancel();
			//promptDialog = null; // Play() is waiting for 'result'
		}
		if (STATUS_LOADING == 1 && isStarting == 0)
			closeStartProgress();
	}

	private void initTickTimer(int delay, int period) {
		if (inBackground != 0)
			return ;
		if (tickTimer == null) {
			tickTimer = new Timer(true);
			try {
				tickTask = new TimerTask() {
					@Override
					public void run() {
						waxHandler.sendEmptyMessage(MSG_UPDATE_CPOSITION);
					}
				};
				tickTimer.schedule(tickTask, delay, period);
			} catch (IllegalArgumentException ex) {
			} catch (IllegalStateException ex) {
			}
		}
	}

	private void destroyTickTimer() {
		if (inBackground != 0 && tickTimer == null)
			return ;
		if (tickTimer != null) {
			//tickTask.cancel();
			tickTimer.cancel();
			//Global.do_sleep(TIMER_SCHED + 100); // Schedule pending Timer
			tickTimer = null;
		}
	}

	private void showRebufferProgress() {
		if (inBackground != 0)
			return ;
		if (WaxPlayService.ActivityFloatWindowMode == true)
			return ;
		if (COUNT_REBUFFERING >= 1)
			return ;
		if (STATUS_REBUFFERING == 0) {
			STATUS_REBUFFERING = 1;
			COUNT_REBUFFERING ++;
			showStartProgress(2);
		}
		if (STATUS_LOADING == 1 && dialogLoading != null)
			dialogLoading.updateUI(lastBuffProg);
	}

	private void closeRebufferProgress(boolean background) {
		if (inBackground != 0)
			return ;
		if (WaxPlayService.ActivityFloatWindowMode == true)
			return ;
		if (STATUS_REBUFFERING == 1) {
			STATUS_REBUFFERING = 0;
			if (background == true)
				waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
			else
				closeStartProgress();
		}
		lastPos_try = 0; // Reset checker
		lastPos_save = 0;
	}

	private void UpdateDT() {
		if (inBackground != 0)
			return ;
		if (wholeLayout == null)
			return ;
		int duration = GetDuration();
		if (duration <= 0)
			return;
		if ((mediaFormat == FMT_PHOTO && duration > MinDuration) || 
			(mediaFormat != FMT_PHOTO && duration >= MaxDurationAV)) {
			durationTView.setText("--:--:--");
			videoProgressBar.setEnabled(false);
			videoProgressBar.setFocusable(false);
		} else {
			String dura = Global.GetTimeString(duration/1000);
			videoProgressBar.setMax(duration);
			videoProgressBar.setKeyProgressIncrement(duration > 1000000 ? 30000 : 5000);
			durationTView.setText(dura);
			videoProgressBar.setEnabled(true);
			videoProgressBar.setFocusable(true);
		}
	}

	private void UpdatePG() {
		if (inBackground != 0)
			return ;
		StartAdMob(false);
		if (timetickText != null) {
			//timetickText.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()).toString());
			//if (timetickText.getVisibility() != View.VISIBLE)
			//	timetickText.setVisibility(View.VISIBLE);
		}
		int curr = 0;
		if (playback_State != STATE_PLAYING || GetDuration() <= MinDuration)
			return;
		if (isExiting() == true)
			return;
		if (duringSeeking == true)
			return; // During SeekBar seeking
		if (mediaProto == PROTOCOL_DIAL && mediaFormat == FMT_VIDEO/*ChromeView*/)
			return;
		curr = GetCurrentTime();
		if (curr < 0 || curr > this.duration - 1000)
			return;
		if (playback_State == STATE_PLAYING && (mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO)) {
			if (lastPos_save == 0)
				lastPos_save = curr;
			lastPos_try ++;
			if (lastPos_try >= BLOCK_CHECK_INTERVAL/TIMER_SCHED) {
				int diff = Math.abs(curr - lastPos_save);
				if (diff > TIMER_SCHED && STATUS_REBUFFERING == 1)
					closeRebufferProgress(false);
				else if (diff < TIMER_SCHED && STATUS_REBUFFERING == 0)
					showRebufferProgress();
				lastPos_try = 0;
				lastPos_save = curr;
			}
		}
		if (curr > 1000) {
			// For delayed onVideoSizeChanged() event
			if (mediaFormat == FMT_AUDIO) {
				int width = GetVideoWidth();
				int height = GetVideoHeight();
				if (width > 0 || height > 0) {
					Log.i(LOG_TAG,"MPlayer["+fragment_id+"] Detect VideoSizeChanged(" + width + "x" + height + ")");
					send_videoSizeChanged(width, height);
				}
			}
			// Check play continuation after play really started
			if (startPercent > 0.0f) {
				if ((mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO) && 
					this.duration > RecDurationAV && this.duration < MaxDurationAV/*Live*/) {
					if (startPercent >= 100.0f) // seconds
						this.seekTime2 = (int)(startPercent-100.0f)*1000;
					else // percent
						this.seekTime2 = (int)((float)this.duration*startPercent);
					if ((int)this.seekTime2 > RecDurationAV && (int)this.seekTime2 < this.duration - RecDurationAV)
						send_seekRequest((int)this.seekTime2, 1);
				}
				startPercent = 0;
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] Continue playing mode1 "+this.seekTime2);
			} else if (this.plistCheck == true) {
				if ((mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO) && playContinue == true && 
					this.duration > RecDurationAV && this.duration < MaxDurationAV/*Live*/) {
					int[] checkResult = PlayList.plg(this.url);
					if (checkResult != null) {
						this.seekTime2 = checkResult[0];
						if (this.duration == checkResult[1] && 
							this.seekTime2 > RecDurationAV && this.seekTime2 < this.duration - RecDurationAV) {
							if (Config.AUTO_RESUME == 1) {
								send_seekRequest(this.seekTime2, 1);
							} else {
								gotoContinuePlay();
								recreateCtrlbarTimer();
							}
						}
					}
				}
				this.plistCheck = false;
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] Continue playing mode2 "+this.seekTime2);
			}
			if (dialogContinue == null && WaxPlayService.mediaplayer != null)
				WaxPlayService.mediaplayer.mainHandler.sendEmptyMessage(WaxPlayer.MSG_DLG_CONTINUE);
		}
		if (mediaFormat == FMT_VIDEO && Srt_Parse != null && Srt_Parse.getCount() > 0) {
			int srtId = Srt_Parse.findSRT(curr);
			updateSrtText(srtId);
		} else if (mediaFormat == FMT_AUDIO && Lyric_Parse != null && Lyric_Parse.getCount() > 0) {
			int srtId = Lyric_Parse.findSRT(curr+400/*Prefetch ahead*/);
			updateLyricText(srtId, false);
		}
		String currText = Global.GetTimeString(curr/1000);
		currentPTView.setText(currText);
		this.seekTime = curr;
		if (mediaFormat == FMT_AUDIO && this.duration >= MaxDurationAV)
			return;
		videoProgressBar.setProgress(curr);
		WaxPlayService.send_MediaPosition(client_id, curr, this.duration);
		if (is_Proxy_HLS == true && curr + TIMER_SCHED >= this.duration && this.duration < MaxDurationAV) {
			PauseInner();
			waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
			send_playFinish(0);
		}
	}

	private void updateSrtTextView(Spanned textStr) {
		if (WaxPlayService.AIRPIN_IS_LITE == true)
			return ;
		if (textStr == null || textStr.length() == 0) {
			srtText.setText("");
			if (srtLayout.getVisibility() == View.VISIBLE)
				srtLayout.setVisibility(View.INVISIBLE);
		} else {
			srtText.setText(textStr);
			if (srtLayout.getVisibility() != View.VISIBLE)
				srtLayout.setVisibility(View.VISIBLE);
		}
	}

	private void updateSrtText(int curId) {
		if (inBackground != 0)
			return ;
		if (lastSrtID == curId)
			return;
		lastSrtID = curId;
		Spanned srtStr = Html.fromHtml("");
		if (curId >= 0 && Srt_Parse != null)
			srtStr = Srt_Parse.getSRT(curId);
		updateSrtTextView(srtStr);
	}

	private void updateLyricTextView(String textStr1, String textStr2) {
		if (WaxPlayService.AIRPIN_IS_LITE == true)
			return ;
		if (textStr1 == null || textStr1.length() == 0) {
			metadataLyric1.setText("");
			if (metadataLyric1.getVisibility() == View.VISIBLE)
				metadataLyric1.setVisibility(View.INVISIBLE);
		} else {
			metadataLyric1.setText(textStr1);
			if (metadataLyric1.getVisibility() != View.VISIBLE)
				metadataLyric1.setVisibility(View.VISIBLE);
			if (metainfoLayout.getVisibility() != View.VISIBLE)
				metainfoLayout.setVisibility(View.VISIBLE);
		}
		if (textStr2 == null || textStr2.length() == 0) {
			metadataLyric2.setText("");
			if (metadataLyric2.getVisibility() == View.VISIBLE)
				metadataLyric2.setVisibility(View.INVISIBLE);
		} else {
			metadataLyric2.setText(textStr2);
			if (metadataLyric1.getVisibility() != View.VISIBLE)
				metadataLyric1.setVisibility(View.VISIBLE);
			if (metadataLyric2.getVisibility() != View.VISIBLE)
				metadataLyric2.setVisibility(View.VISIBLE);
			if (metainfoLayout.getVisibility() != View.VISIBLE)
				metainfoLayout.setVisibility(View.VISIBLE);
		}
	}

	private void updateLyricText(int curId, boolean reset) {
		if (inBackground != 0)
			return ;
		if (curId >= 0 && lastSrtID == curId)
			return;
		lastSrtID = curId;
		String srtStr1 = "", srtStr2 = "";
		if (reset == false) {
			if (curId >= 0 && Lyric_Parse != null) {
				srtStr1 = Lyric_Parse.getSRT2(curId);
				srtStr2 = Lyric_Parse.getSRT2(curId+1);
			} else if (curId < 0 && Lyric_Parse != null) {
				srtStr2 = Lyric_Parse.getSRT2(0);
			}
		}
		updateLyricTextView(srtStr1, srtStr2);
	}

	private boolean isZoomSupport() {
		return ((WaxPlayService.mediaplayer.MPlayerCount == 1 || isZoomed2 == true) && (isExiting() == false) && 
				(mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3) && 
				((float)videoHeight/(float)videoWidth)>1.5f && ((float)screenWidth/(float)screenHeight)>1.5f ) ? true : false;
	}

	private boolean processEnterKeyZoom() {
		if (isZoomSupport() == false)
			return false;
		long currTime = System.currentTimeMillis();
		if (lastTime_EnterKey != 0 && currTime - lastTime_EnterKey < 1000) {
			if (zoomState != LAYOUT_ZOOM)
				Scale(0, LAYOUT_ZOOM);
			else
				Scale(0, Config.ZOOM);
			lastTime_EnterKey = 0;
		} else {
			lastTime_EnterKey = currTime;
		}
		return true;
	}

	private void processBackKeyExit() {
		long currTime = System.currentTimeMillis();
		if (lastTime_BackKey != 0 && currTime - lastTime_BackKey < 2000) {
			if (isExiting() == false) {
				EnterExit();
				gotoStop(false, 1, true);
				gotoFinish(0);
			}
			lastTime_BackKey = 0;
		} else {
			WaxPlayService.displayToast3(8, false, getString(R.string.waxplayer_toast_backkeytwice), 2000, false);
			lastTime_BackKey = currTime;
		}
	}

	//@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (debug_InputEvent == true)
			Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onKeyUp called = "+keyCode+" from "+((android.os.Build.VERSION.SDK_INT >= 9) ? event.getSource() : -1));
		if (isFocused2 == false)
			return false;
		if (inBackground != 0)
			return false;//super.onKeyUp(keyCode, event);
		boolean isKeyCode_DIRECTION = ( keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
										keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN );
		boolean isKeyCode_ENTER = ( keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER ) || (
									WaxPlayService.KeyCodeEnter2 > 0 && keyCode == WaxPlayService.KeyCodeEnter2 );
		boolean isKeyCodeSupported = ( keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && 
										keyCode != KeyEvent.KEYCODE_CALL && keyCode != KeyEvent.KEYCODE_ENDCALL );
		if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true)
			return false;//super.onKeyUp(keyCode, event);
		if (STATUS_CLAYOUT_HIDE == 1 && isKeyCode_ENTER == true && isZoomSupport() == true)/*Snoopy 11/19/2024 : proceed in onKeyDown()*/
			return true;
		if (isKeyCodeSupported == true) {
			if (STATUS_CLAYOUT_HIDE == 1) {
				if (remoteCtrlSupport() > 0) {
					if (isKeyCode_DIRECTION == true)
						return true; // Proceed in onKeyDown()
					if (isKeyCode_ENTER == true) {
						if (key_isLongPress == false) // Long Press of ENTER key is proceed in onKeyDown()
							onCursorLeftClick(0, 0, true);
						return true;
					}
				}
				if (WaxPlayService.exitOnBackKey == true || exitOnBackKey2 == true) {
					if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
						processBackKeyExit();
						return true;
					}
				}
				if (ENABLE_PlayControlByKey == true && (isKeyCode_ENTER == true || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) )
					return true; // Proceed in onKeyDown()
				if (WaxPlayService.ENABLE_VolumeControl == true && keyCode == KeyEvent.KEYCODE_DPAD_UP && mediaFormat != FMT_PHOTO) {
					GetVolume(); // update WaxPlayService.realVolume
					SetVolume((WaxPlayService.realVolume+1)*100/WaxPlayService.totalVolume, true);
				} else if (WaxPlayService.ENABLE_VolumeControl == true && keyCode == KeyEvent.KEYCODE_DPAD_DOWN && mediaFormat != FMT_PHOTO) {
					GetVolume(); // update WaxPlayService.realVolume
					SetVolume((WaxPlayService.realVolume-1)*100/WaxPlayService.totalVolume, true);
				//} else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
				//	Scale(0, (zoomState==LAYOUT_SCALE)? LAYOUT_STRETCH : LAYOUT_SCALE);
				} else {
					AdjustControllor(); // show at any key pressed
					if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) && mediaFormat == FMT_AUDIO) {

					}
				}
				return true;
			} else {
				if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_MENU) {
					AdjustControllor(); // hide
					return true;
				} else {
					recreateCtrlbarTimer();
				}
			}
		}
		//if (mGalleryView != null && mGalleryView.started() == true)
		//	return true; // Don't process GalleryView
		return false;//super.onKeyUp(keyCode, event);
	}

	//@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (debug_InputEvent == true)
			Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onKeyDown called = "+keyCode+" from "+((android.os.Build.VERSION.SDK_INT >= 9) ? event.getSource() : -1));
		if (isFocused2 == false)
			return false;
		if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true)
			return false;
		boolean isKeyCode_DIRECTION = ( keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
										keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN );
		boolean isKeyCode_ENTER = ( keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER ) || (
									WaxPlayService.KeyCodeEnter2 > 0 && keyCode == WaxPlayService.KeyCodeEnter2 );
		if (STATUS_CLAYOUT_HIDE == 1 && isKeyCode_ENTER == true && processEnterKeyZoom() == true)/*Snoopy 11/19/2024 : added*/
			return true;
		if (isKeyCode_DIRECTION == true || isKeyCode_ENTER == true) {
			if (STATUS_CLAYOUT_HIDE == 1 && remoteCtrlSupport() > 0) {
				int i;
				int step = 1;
				int count = 0;
				if (event.getRepeatCount() == 0) {
					key_isLongPress = false;
					event.startTracking();
					count = 1; // Single Click ?
				} else {
					count = event.getRepeatCount();
				}
				if (key_isLongPress == false && count > 2 && isKeyCode_ENTER == true) {
					onCursorRightClick(0, 0, true);
					key_isLongPress = true;
					return true;
				}
				int deltaX = 0, deltaY = 0;
				if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
					deltaX = MICE_MOVE_DELTA;
				} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
					deltaX = -MICE_MOVE_DELTA;
				} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
					deltaY = MICE_MOVE_DELTA;
				} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
					deltaY = -MICE_MOVE_DELTA;
				} else { // KEYCODE_ENTER
					return true;
				}
				if (count > 1) { /* Reduce network sending count */
					deltaX *= 2;
					deltaY *= 2;
					step = 2;
				}
				for (i = 0; i < count; i += step)
					updateCursor(mCursor1, MICE_MOVE, deltaX, deltaY, MICE_DISPLAY_TO);
				return true;
			}
			if (ENABLE_PlayControlByKey == true && STATUS_CLAYOUT_HIDE == 1 && (mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO) && duration < MaxDurationAV) {
				boolean show_ctrlbar = false;
				if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
					sb_last_progress = videoProgressBar.getProgress();
					sb_last_progress -= ( (videoProgressBar.getMax() > 300*1000) ? 60*1000 : 10*1000 );
					if (sb_last_progress >= 0 && sb_last_progress <= videoProgressBar.getMax())
						send_seekRequest(sb_last_progress, 0);
					else
						sb_last_progress = 0;
					videoProgressBar.setProgress(sb_last_progress);
				} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
					sb_last_progress = videoProgressBar.getProgress();
					sb_last_progress += ( (videoProgressBar.getMax() > 300*1000) ? 60*1000 : 10*1000 );
					if (sb_last_progress >= 0 && sb_last_progress <= videoProgressBar.getMax())
						send_seekRequest(sb_last_progress, 0);
					else
						sb_last_progress = videoProgressBar.getMax();
					videoProgressBar.setProgress(sb_last_progress);
				} else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
					if (is_Playing()) {
						Pause(true, 1, 0);
						if (WaxPlayer.waxPlayService != null)
							WaxPlayer.waxPlayService.MediaPause(client_id);
					} else {
						Resume(1);
						if (WaxPlayer.waxPlayService != null)
							WaxPlayer.waxPlayService.MediaResume(client_id);
						return true;
					}
				} else if (/*keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||*/ keyCode == KeyEvent.KEYCODE_MENU) {
					show_ctrlbar = true;
				}
				if (WaxPlayService.noControllorBar == false && show_ctrlbar == true)
					ShowControllor(true);
			}
		}
		return false;//super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (debug_InputEvent == true)
			Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onTouch called = "+event.getAction()+"@"+(int)event.getRawX()+","+(int)event.getRawY()+" from "+((android.os.Build.VERSION.SDK_INT >= 9) ? event.getSource() : -1));
		if (isFocused2 == false) {
			if (WaxPlayService.mediaplayer != null)
				WaxPlayService.mediaplayer.SWITCHTO_MPlayer(fragment_id-1, true);
			return true;
		}
		if (inBackground != 0)
			return true;
		if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true)
			return true;
		if (WaxPlayService.exitOnBackKey == true || exitOnBackKey2 == true) {
			try {
				if (android.os.Build.VERSION.SDK_INT >= 14 && event.getButtonState() == MotionEvent.BUTTON_SECONDARY && event.getAction() == MotionEvent.ACTION_DOWN) {
					processBackKeyExit();
					return true;
				}
			} catch (Exception ex) {
			}
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN && ( (
			zoomState != LAYOUT_ZOOM && (v.getId() == wholeView.getId() || v.getId() == R.id.WholeLayout) ) || (
			zoomState == LAYOUT_ZOOM && (v.getId() == wholeView.getId() || v.getId() == R.id.WholeLayout || v.getId() == R.id.SDNativeView) ) ) ) {
			if (processEnterKeyZoom() == true)/*Snoopy 11/19/2024 : added*/
				return true;
		}
		if (v.getId() == R.id.SDNativeView && mGestureDetector != null && ( ( remoteCtrlSupport() > 0 && 
			STATUS_CLAYOUT_HIDE == 1 && (WaxPlayService.mediaplayer.MPlayerCount == 1 || isZoomed2 == true) ) || 
			WaxPlayService.mediaplayer.DoodleView_ShouldShow(fragment_id) ) ) {
			/*if (android.os.Build.VERSION.SDK_INT >= 9 && event.getSource() == InputDevice.SOURCE_MOUSE) {
				if (event.getAction() == MotionEvent.ACTION_DOWN)
					onCursorLeftClick((int)event.getRawX(), (int)event.getRawY(), false);
				return true;
			}*/
			if (VERIFY_TOUCH_BY_MOTIONRANGE == true && (AXIS_X_Range < 0 || AXIS_Y_Range < 0) && 
				android.os.Build.VERSION.SDK_INT >= 12 ) {
				try {
					AXIS_X_Range = (int)event.getDevice().getMotionRange(MotionEvent.AXIS_X, event.getSource()).getRange() * screenWidth / WaxPlayer.screenWidth;
					AXIS_Y_Range = (int)event.getDevice().getMotionRange(MotionEvent.AXIS_Y, event.getSource()).getRange() * screenHeight / WaxPlayer.screenHeight;
				} catch (Exception ex) {
				}
			}
			if (mMarkScreenMode == true) { /*For actually emulate touch screen event*/
				int event2 = -1;
				if (event.getAction() == MotionEvent.ACTION_DOWN)
					event2 = MotionEvent.ACTION_DOWN;//MICE_L_DOWN
				if (event.getAction() == MotionEvent.ACTION_MOVE)
					event2 = MotionEvent.ACTION_MOVE;//MICE_MOVETO
				else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
					event2 = MotionEvent.ACTION_UP;//MICE_L_UP
				if (event2 >= 0) {
					try {
						com.waxrain.droidsender.doodle.DoodleWindow$DrawEvent draw_event2 = new com.waxrain.droidsender.doodle.DoodleWindow$DrawEvent(
							event2, (int)event.getRawX(), (int)event.getRawY(), SystemClock.uptimeMillis());
						Global.DoodleViewDraw((Object)draw_event2);
					} catch (Exception e) {
					}
				}
				return true;
			}
			/*if (WaxPlayer.CURSOR_DIRECT_MODE == true && mice_isDragMode == true) {
				if (event.getAction() == MotionEvent.ACTION_MOVE)
					updateCursor(mCursor1, MICE_MOVE, (int)event.getRawX(), (int)event.getRawY(), 0);
				else if (event.getAction() == MotionEvent.ACTION_UP)
					updateCursor(mCursor2, MICE_L_UP, (int)event.getRawX(), (int)event.getRawY(), 0);
			}*/
			return mGestureDetector.onTouchEvent(event);
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN && (
			v.getId() == R.id.HDVideoView || v.getId() == R.id.SDVideoView || v.getId() == R.id.GalleryView || v.getId() == R.id.HDImageView || v.getId() == R.id.SDNativeView) ) {
			AdjustControllor();
			return true;
		}
		if (mGalleryView != null && mGalleryView.started() == true)
			return true; // Don't process GalleryView
		return false;//super.onTouchEvent(event);
	}

	public boolean onGenericMotionEvent(MotionEvent event) {
		if (android.os.Build.VERSION.SDK_INT >= 12) {
			try {
				// The input source is a pointing device associated with a display
				if (0 != (event.getSource() & InputDevice.SOURCE_CLASS_POINTER)) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_SCROLL: /* process the scroll wheel movement */
						if (STATUS_CLAYOUT_HIDE == 1 && remoteCtrlSupport() > 0) {
							if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f)
								sendKeyEvent(mirrorServerType == 1 ? 0x22 : KeyEvent.KEYCODE_PAGE_DOWN);
							else
								sendKeyEvent(mirrorServerType == 1 ? 0x21 : KeyEvent.KEYCODE_PAGE_UP);
							return true;
						}
						break;
					}
				}
			} catch (Exception ex) {
				return true;
			}
		}
		return false;
	}

	private void send_promptDlgPlayRequest(int proto, String title, int timeout) {
		Message msgInform = Message.obtain();
		msgInform.obj = title;
		msgInform.arg1 = proto;
		msgInform.arg2 = timeout;
		msgInform.what = MSG_PROMPT_DIALOG_PLAY;
		waxHandler.sendMessage(msgInform);
	}

	private void updateIconPlay(int forceHide, int forceShow) {
		if (inBackground != 0)
			return ;
		if (wholeLayout == null)
			return ;
		if (forceShow == 1)
			closeRebufferProgress(false);
		closeSubDialogs(1); // Stopped or Paused but child dialog openned
		if (WaxPlayService.noControllorBar == true)
			return ;
		if (mediaFormat != FMT_MIRROR && mediaFormat != FMT_MIRROR2 && mediaFormat != FMT_MIRROR3 && mediaFormat != FMT_MIRROR4 && mediaFormat != FMT_PHOTO) {
			if (WaxPlayService.noPatternButton == false) {
				patternButton.setEnabled(true);
				patternButton.setFocusable(true);
				patternButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.pattern));
			}
			playButton.setEnabled(true);
			playButton.setFocusable(true);
			playButton.setVisibility(View.VISIBLE);
			if (playButton.isFocused()) {
				if (is_Playing())
					playButton.setBackgroundDrawable(getResources().getDrawable(DRAWABLE_pause_on));
				else
					playButton.setBackgroundDrawable(getResources().getDrawable(DRAWABLE_play_on));
			} else {
				if (is_Playing())
					playButton.setBackgroundDrawable(getResources().getDrawable(DRAWABLE_pause));
				else
					playButton.setBackgroundDrawable(getResources().getDrawable(DRAWABLE_play));
			}
			if (STATUS_CLAYOUT_HIDE == 0 && isFocused2 == true)
				playButton.requestFocus();
		} else {
			if (WaxPlayService.noPatternButton == false) {
				patternButton.setEnabled(false);
				patternButton.setFocusable(false);
				patternButton.setVisibility(View.GONE);
			}
			playButton.setEnabled(false);
			playButton.setFocusable(false);
			playButton.setVisibility(View.GONE);
			if (STATUS_CLAYOUT_HIDE == 0 && isFocused2 == true)
				stopButton.requestFocus();
		}
		if (WaxPlayService.AIRPIN_IS_LITE == true) {
			patternButton.setEnabled(false);
			patternButton.setFocusable(false);
			videoProgressBar.setEnabled(false);
			videoProgressBar.setFocusable(false);
			muteButton.setEnabled(false);
			muteButton.setFocusable(false);
		} else {
			if (muteButton.isFocused()) {
				if (isMuted2 == true)
					muteButton.setBackgroundDrawable(getResources().getDrawable(RES_drawable_mute_on));
				else
					muteButton.setBackgroundDrawable(getResources().getDrawable(RES_drawable_mute2_on));
			} else {
				if (isMuted2 == true)
					muteButton.setBackgroundDrawable(getResources().getDrawable(RES_drawable_mute));
				else
					muteButton.setBackgroundDrawable(getResources().getDrawable(RES_drawable_mute2));
			}
		}
		if (mediaFormat == FMT_MIRROR4) {
			((LinearLayout)this.getView().findViewById(R.id.ctrl_showtime)).setVisibility(View.GONE);
			scaleButton.setEnabled(false);
			scaleButton.setFocusable(false);
			scaleButton.setVisibility(View.GONE);
		}
		if (mediaFormat == FMT_AUDIO) {
			if (WaxPlayService.noPatternButton == false) {
				scaleButton.setEnabled(false);
				scaleButton.setFocusable(false);
				scaleButton.setVisibility(View.GONE);
			}
			if (inBackground == 0) {
				if (mGalleryView == null) {
					CreateGalleryView();
					mGalleryView.start();
					UpdateAudioMetadata(true, false, "", "", "", "", "");/*MUST after INIT/RESET of mGalleryView*/
				}
				LayoutAdMob();
			}
		} else {
			if (WaxPlayService.noPatternButton == false) {
				scaleButton.setEnabled(true);
				scaleButton.setFocusable(true);
				scaleButton.setVisibility(View.VISIBLE);
				scaleButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.scale));
			}
			if (mGalleryView != null) {
				DestroyGalleryView();
				/*if (whichPlayer == PLAYER_INNER && hDVideoView != null && wholeLayout.indexOfChild(hDVideoView) >= 0)
					wholeLayout.bringChildToFront(hDVideoView);
				else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null && wholeLayout.indexOfChild(sDVideoView) >= 0)
					wholeLayout.bringChildToFront(sDVideoView);
				LayoutAdMob();*/
			}
		}
		if (forceHide == 1)
			HideControllor();
		if (forceShow == 1)
			ShowControllor(false);
	}

	private void updateIconStop(int forceShow) {
		if (mGalleryView != null)
			DestroyGalleryView(); // HOME key pressed then onPause() -> set 'inBackground = 1'
		if (inBackground != 0)
			return ;
		if (wholeLayout == null)
			return ;
		closeSubDialogs(0);
		if (WaxPlayService.noControllorBar == true)
			return ;
		playButton.setEnabled(false);
		playButton.setFocusable(false);
		if (mediaFormat == FMT_AUDIO || mediaFormat == FMT_VIDEO || mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || forceShow == 1) {
			if (WaxPlayService.noPatternButton == false) {
				scaleButton.setEnabled(false);
				scaleButton.setFocusable(false);
			}
		}
		durationTView.setText("--:--:--");
		currentPTView.setText("00:00:00");
		videoProgressBar.setProgress(0);
		videoProgressBar.setMax(0);
		if (forceShow == 1) {
			ShowControllor(false);
		} else {
			if (mediaFormat == FMT_PHOTO)
				HideControllor();
		}
		if (STATUS_CLAYOUT_HIDE == 0 && isFocused2 == true)
			stopButton.requestFocus();
	}

	private void send_updateIconPlay(int forceHide, int forceShow, int delay) {
		Message msgInform = Message.obtain();
		msgInform.arg1 = forceHide;
		msgInform.arg2 = forceShow;
		msgInform.what = MSG_UPDATE_ICON_PLAY;
		waxHandler.removeMessages(MSG_UPDATE_ICON_END);
		waxHandler.removeMessages(MSG_UPDATE_ICON_PLAY);
		if (delay > 0) {
			Message msgInform2 = new Message();
			msgInform2.copyFrom(msgInform);
			msgInform2.arg2 = 0;
			waxHandler.sendMessage(msgInform2); // Update button state first whether it is showing
			waxHandler.sendMessageDelayed(msgInform, delay);
		} else {
			waxHandler.sendMessage(msgInform);
		}
	}

	private void send_updateIconStop(int forceShow, int delay) {
		if (forceShow != 0)
			forceShow = 0; // Always hide control bar until user calls it
		Message msgInform = Message.obtain();
		msgInform.arg1 = forceShow;
		msgInform.what = MSG_UPDATE_ICON_END;
		waxHandler.removeMessages(MSG_UPDATE_ICON_PLAY);
		waxHandler.removeMessages(MSG_UPDATE_ICON_END);
		if (delay > 0) {
			Message msgInform2 = new Message();
			msgInform2.copyFrom(msgInform);
			msgInform2.arg1 = 0;
			waxHandler.sendMessage(msgInform2); // Update button state first whether it is showing
			waxHandler.sendMessageDelayed(msgInform, delay);
		} else {
			waxHandler.sendMessage(msgInform);
		}
	}

	public void send_openDoc(String filePath) {
		Message msgInform = Message.obtain();
		msgInform.obj = filePath;
		msgInform.what = MSG_OPEN_DOC;
		waxHandler.removeMessages(MSG_OPEN_DOC);
		waxHandler.sendMessage(msgInform);
	}

	public void send_playFailure(String errString) {
		Message msgInform = Message.obtain();
		msgInform.obj = errString;
		msgInform.what = MSG_SDPLAYER_FAILURE;
		waxHandler.removeMessages(MSG_SDPLAYER_FAILURE);
		waxHandler.sendMessage(msgInform);
	}

	public void send_showHostname(String hostname2, int delay) {
		hostname = hostname2;
		waxHandler.sendEmptyMessageDelayed(MSG_SHOW_HOSTNAME, delay);		
	}

	private void send_videoSizeChanged(int width, int height) {
		Message msgInform = Message.obtain();
		msgInform.arg1 = width;
		msgInform.arg2 = height;
		msgInform.what = MSG_VIDEOSIZE_CHANGE;
		waxHandler.removeMessages(MSG_VIDEOSIZE_CHANGE);
		waxHandler.sendMessage(msgInform);
		// Snoopy 8/29/2022 : added
		if (WaxPlayService.mediaplayer != null) {
			WaxPlayService.mediaplayer.mainHandler.removeMessages(WaxPlayer.MSG_VIDEOSIZE_CHANGED);
			WaxPlayService.mediaplayer.mainHandler.sendEmptyMessage(WaxPlayer.MSG_VIDEOSIZE_CHANGED);
		}
		// End 8/29/2022
	}

	public void send_videoScale(int state, int zoomflag) {
		Message msgInform = Message.obtain();
		msgInform.arg1 = state;
		msgInform.arg2 = zoomflag;
		msgInform.what = MSG_VIDEO_SCALE;
		waxHandler.removeMessages(MSG_VIDEO_SCALE);
		waxHandler.sendMessage(msgInform);
	}

	private void send_bufferUpdated(int percent) {
		Message msgInform = Message.obtain();
		msgInform.arg1 = percent;
		msgInform.what = MSG_BUFFER_UPDATE;
		waxHandler.removeMessages(MSG_BUFFER_UPDATE);
		waxHandler.sendMessage(msgInform);
	}

	private void send_titleUpdated(String title2) {
		Message msgInform = Message.obtain();
		msgInform.obj = title2;
		msgInform.what = MSG_TITLE_UPDATE;
		waxHandler.removeMessages(MSG_TITLE_UPDATE);
		waxHandler.sendMessage(msgInform);
	}

	private void send_innerTextUpdated(String textStr) {
		Message msgInform = Message.obtain();
		msgInform.obj = textStr;
		msgInform.what = MSG_INNERTEXT_UPDATE;
		waxHandler.removeMessages(MSG_INNERTEXT_UPDATE);
		waxHandler.sendMessage(msgInform);
	}

	private void send_debugMsgUpdated(String textStr) {
		Message msgInform = Message.obtain();
		msgInform.obj = textStr;
		msgInform.what = MSG_UPDATE_DBGMSG;
		waxHandler.sendMessage(msgInform);
	}

	private void send_seekRequest(int seektime, int mode) {
		duringSeeking = true;
		this.seekTime = seektime;
		if (this.plistCheck == true)
			this.plistCheck = false;
		Message msgInform = Message.obtain();
		msgInform.arg1 = seektime;
		msgInform.arg2 = mode;
		msgInform.what = MSG_START_SEEK;
		waxHandler.removeMessages(MSG_START_SEEK);
		if (mode == 0/* from SeekBar */)
			waxHandler.sendMessageDelayed(msgInform, 1000);
		else
			waxHandler.sendMessage(msgInform);
		WaxPlayService.send_MediaSeek(client_id, seektime, mode);
	}

	public void send_playFinish(int excode) {
		int delay = 1000;
		if (WaxPlayService.finishTimerTimeout < delay)
			delay = WaxPlayService.finishTimerTimeout;
		if (excode == -2 && mediaFormat == FMT_MIRROR && WaxPlayService.PcMirrorAutoReconnect == true) 
			delay = 30*1000; // Be same with PcSender/BitmapFile.h
		Message msgInform = Message.obtain();
		msgInform.what = MSG_PLAY_DONE;
		waxHandler.sendMessageDelayed(msgInform, delay);
		//waxHandler.sendEmptyMessage(MSG_PLAY_DONE);
	}

	private void destroyCtrlbarTimer() {
		if (inBackground != 0 && ctrlbarTimer == null)
			return ;
		if (ctrlbarTimer != null) {
			ctrlbarTimer.cancel();
			ctrlbarTimer = null;
		}
	}

	private void recreateCtrlbarTimer() {
		destroyCtrlbarTimer();
		if (inBackground != 0)
			return ;
		if (ctrlbarTimer == null) {
			ctrlbarTimer = new Timer(true);
			try {
				ctrlbarTask = new TimerTask() {
					@Override
					public void run() {
						ctrlbarTimer = null;
						if (playback_State == STATE_PLAYING)
							send_updateIconPlay(1, 0, -1);
					}
				};
				ctrlbarTimer.schedule(ctrlbarTask, WaxPlayService.CtrlbarTimeout);
			} catch (IllegalArgumentException ex) {
			} catch (IllegalStateException ex) {
			}
		}
	}

	private void setControllorTransparent(boolean flag) {
		if (inBackground != 0)
			return ;
		if (flag == true)
			Global.mySetAlpha(ctrlLayout, 0.8f);
		else
			Global.mySetAlpha(ctrlLayout, 1.0f);
	}

	private boolean HideControllor() {
		if (inBackground != 0 || WaxPlayService.noControllorBar == true)
			return true;
		if (STATUS_CLAYOUT_HIDE == 0) {
			Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onHideControllor called");
			setControllorTransparent(false);
			ctrlLayout.startAnimation(AnimationUtils.loadAnimation(this.getContext(), android.R.anim.fade_out));
			ctrlLayout.setVisibility(View.INVISIBLE);
			STATUS_CLAYOUT_HIDE = 1;
			return true;
		}
		return false;
	}

	private boolean ShowControllor(boolean autoHide) {
		if (inBackground != 0 || WaxPlayService.noControllorBar == true)
			return false;
		if (wholeLayout == null)
			return false;
		//StartAdMob(true);
		if (STATUS_CLAYOUT_HIDE == 1) {
			Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onShowControllor called");
			if (mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || mediaFormat == FMT_MIRROR4 || mediaFormat == FMT_PHOTO) {
				playButton.setEnabled(false);
				playButton.setFocusable(false);
				playButton.setVisibility(View.GONE);
				patternButton.setEnabled(false);
				patternButton.setFocusable(false);
				patternButton.setVisibility(View.GONE);
				videoProgressBar.setEnabled(false);
				videoProgressBar.setFocusable(false);
				videoProgressBar.setVisibility(View.INVISIBLE);
			} else {
				playButton.setEnabled(true);
				playButton.setFocusable(true);
				playButton.setVisibility(View.VISIBLE);
				if (WaxPlayService.noPatternButton == false) {
					patternButton.setEnabled(true);
					patternButton.setFocusable(true);
					patternButton.setVisibility(View.VISIBLE);
				}
				if (WaxPlayService.noSeekingButton == false) {
					videoProgressBar.setEnabled(true);
					videoProgressBar.setFocusable(true);
					videoProgressBar.setVisibility(View.VISIBLE);
				}
			}
			if (mediaFormat == FMT_PHOTO || mediaFormat == FMT_MIRROR4) {
				muteButton.setEnabled(false);
				muteButton.setFocusable(false);
				muteButton.setVisibility(View.GONE);
			} else {
				muteButton.setEnabled(true);
				muteButton.setFocusable(true);
				muteButton.setVisibility(View.VISIBLE);
				if (WaxPlayService.AIRPIN_IS_LITE == true) {
					muteButton.setEnabled(false);
					muteButton.setFocusable(false);
				}
			}
			waxHandler.sendEmptyMessage(MSG_UPDATE_PLAYSPEED);
			ctrlLayout.setVisibility(View.VISIBLE);
			ctrlLayout.startAnimation(AnimationUtils.loadAnimation(this.getContext(), android.R.anim.fade_in));
			setControllorTransparent(true);
			STATUS_CLAYOUT_HIDE = 0;
			if (STATUS_CLAYOUT_HIDE == 0 && isFocused2 == true) {
				//if (playButton.isEnabled() == true)
				//	playButton.requestFocus();
				//else
					stopButton.requestFocus();
			}
			if (autoHide == true)
				recreateCtrlbarTimer();
			return true;
		}
		return false;
	}

	private void AdjustControllor() {
		if (inBackground != 0 || WaxPlayService.noControllorBar == true)
			return ;
		if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING)
			return ;
		if (HideControllor() == true) {
			destroyCtrlbarTimer();
			return ;
		} else {
			ShowControllor(true);
		}
	}

	private void reconfigVitamioPlayer() {
		try {
			if (whichPlayer == PLAYER_VITAMIO && 
				mediaProto == PROTOCOL_AIRPLAY && ( 
				url.toLowerCase().endsWith(".mov") || url.toLowerCase().indexOf(".mov?") > 0 ) && 
				android.os.Build.MANUFACTURER.equalsIgnoreCase("Amazon") && android.os.Build.MODEL.toLowerCase().startsWith("aft") && 
				android.os.Build.VERSION.SDK_INT >= 16 && vitamioHwdecEnable == false && Config.VITAMIO_HWDEC_SETUP == false ) {
				vitamioHwdecEnable = true;
			}
		} catch (Exception e) {
		}
	}

	private void CheckBufferingPlaying_Inner(int delay) {
		if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || isExiting() == true || playback_State == STATE_STOPPED)
			return ;
		if (delay > 0)
			ResumeInner();
		if (tickTimer != null) {
			playback_State = STATE_PLAYING; // Switching Player?
			waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
			send_updateIconPlay(1, 0, -1);
		} else {
			if (playback_State != STATE_PLAYING) // resumed playing
				playback_State = STATE_PREPARED;
			Global.do_sleep(50); // Schedule Play()
		}
	}

	private void CheckBufferingPlaying() {
		int dura = WaxPlayService._config.MP_BUFFERDURATION;
		if (dura < 0 || mediaFormat != FMT_VIDEO)
			dura = 0;
		final int delay = dura * 1000;
		if (delay <= 0) { // Photo callback in HDImageView will be blocked
			CheckBufferingPlaying_Inner(0);
			return ;
		}
		new Handler(WaxPlayer2.this.getContext().getMainLooper()).postDelayed(new Runnable() {
			public void run() {
				CheckBufferingPlaying_Inner(delay);
			}
		}, delay);
		if (delay > 0)
			PauseInner();
	}

	private void ResetFrontLayout() {
		if (WaxPlayService.noControllorBar == false)
			ctrlLayout.bringToFront();
		srtLayout.bringToFront();
	}

	private void DestroyHdVideoView() {
		if (hDVideoView != null) {
			if (playback_State == STATE_PREPARING)
				hDVideoView.release(true);
			else if (playback_State != STATE_STOPPED)
				hDVideoView.stopPlayback(inBackground);
			if (inBackground == 0) {
				hDVideoView.mediaplayer = null;
				if (WaxPlayService.destroyVideoView == true) {
					if (wholeLayout.indexOfChild(hDVideoView) >= 0) {
						WaxPlayer.HideWholeLayoutSubview(hDVideoView);
						wholeLayout.removeView(hDVideoView);
					}
					hDVideoView = null;
				} else {
					hDVideoView.setVisibility(View.GONE);
				}
			}
		}
		if (inBackground != 0)
			return ;
		if (hDImageView != null && 
			(mediaFormat != FMT_PHOTO || whichPlayer != PLAYER_INNER) ) {
			//if (playback_State != STATE_STOPPED)
				hDImageView.stop(true);
			hDImageView.finish_destroy();
			hDImageView.mediaplayer = null;
			if (WaxPlayService.destroyVideoView == true) {
				if (wholeLayout.indexOfChild(hDImageView) >= 0) {
					WaxPlayer.HideWholeLayoutSubview(hDImageView);
					wholeLayout.removeView(hDImageView);
				}
				hDImageView = null;
			} else {
				hDImageView.setVisibility(View.GONE);
			}
		}
	}

	private void DestroySdVideoView() {
		if (sDVideoView != null) {
			if (playback_State == STATE_PREPARING)
				sDVideoView.release(true);
			else if (playback_State != STATE_STOPPED)
				sDVideoView.stopPlayback(inBackground);
			if (inBackground == 0) {
				sDVideoView.mediaplayer = null;
				if (WaxPlayService.destroyVideoView == true) {
					if (wholeLayout.indexOfChild(sDVideoView) >= 0) {
						WaxPlayer.HideWholeLayoutSubview(sDVideoView);
						wholeLayout.removeView(sDVideoView);
					}
					sDVideoView = null;
				} else {
					sDVideoView.setVisibility(View.GONE);
				}
			}
		}
		if (sDNativeViewEnabled == true) {
			if (sDNativeView != null) {
				/*if (playback_State == STATE_PREPARING)
					sDNativeView.release(true);
				else*/ if (playback_State != STATE_STOPPED)
					sDNativeView.stopPlayback(inBackground, false);
				if (inBackground == 0) {
					sDNativeView.mediaplayer = null;
					if (WaxPlayService.destroyVideoView == true) {
						if (wholeLayout.indexOfChild(sDNativeView) >= 0) {
							WaxPlayer.HideWholeLayoutSubview(sDNativeView);
							wholeLayout.removeView(sDNativeView);
						}
						sDNativeView = null;
					} else {
						sDNativeView.setVisibility(View.GONE);
					}
				}
			}
		}
	}

	private void DestroyGalleryView() {
		if (mGalleryView != null) {
			if (mGalleryView.started() == true)
				mGalleryView.cancel(); // gone background after GalleryView.cancel()
			if (inBackground == 0) {
				mGalleryView.mediaplayer = null;
				if (WaxPlayService.destroyVideoView == true) {
					if (wholeLayout.indexOfChild(mGalleryView) >= 0) {
						WaxPlayer.HideWholeLayoutSubview(mGalleryView);
						wholeLayout.removeView(mGalleryView);
					}
					mGalleryView = null;
				} else {
					mGalleryView.setVisibility(View.GONE);
				}
			}
		}
	}

	private void DestroyChromeView() {
		if (mChromeView != null) {
			if (inBackground == 0) {
				try {
					if (android.os.Build.VERSION.SDK_INT >= 11)
						((android.webkit.WebView)mChromeView).onPause();
				} catch (Exception e) {
				} catch (Throwable e) {
				}
				try {
					((android.webkit.WebView)mChromeView).clearHistory();
					((android.webkit.WebView)mChromeView).clearCache(true);
					((android.webkit.WebView)mChromeView).loadUrl("about:blank");
					((android.webkit.WebView)mChromeView).freeMemory(); 
					((android.webkit.WebView)mChromeView).destroy();
				} catch (Exception e) {
				} catch (Throwable e) {
				}
				try {
					if (WaxPlayService.destroyVideoView == true) {
						if (wholeLayout.indexOfChild((android.webkit.WebView)mChromeView) >= 0) {
							WaxPlayer.HideWholeLayoutSubview((android.webkit.WebView)mChromeView);
							wholeLayout.removeView((android.webkit.WebView)mChromeView);
						}
						mChromeView = null;
					} else {
						((android.webkit.WebView)mChromeView).setVisibility(View.GONE);
					}
				} catch (Exception e) {
				} catch (Throwable e) {
				}
			}
		}
	}

	private void DestroyHwCastView() {
		WaxPlayService.HWS_DestroyView(wholeLayout, inBackground, WaxPlayService.destroyVideoView);
	}

	private void DestroyAllVideoViews() {
		DestroyHdVideoView();
		DestroySdVideoView();
		DestroyGalleryView();
		DestroyChromeView();
		DestroyHwCastView();
	}

	private void CreateHdVideoView() {
		if (hDVideoView == null) {
			if (savedHdVideoViewAttrs == null || inBackground != 0) {
				hDVideoView = new HDVideoView(this.getContext());
				if (inBackground != 0)
					return ;
			} else {
				try { /*NullPointerException@XmlBlock.nativeGetStyleAttribute on Android TV Box*/
					hDVideoView = new HDVideoView(this.getContext(), savedHdVideoViewAttrs, savedHdVideoViewStyle);
				} catch (Exception e) {
					hDVideoView = new HDVideoView(this.getContext());
				}
			}
			savedHdVideoViewLayout.width = LayoutParams.FILL_PARENT; // Scale changed layout params
			savedHdVideoViewLayout.height = LayoutParams.FILL_PARENT;
			hDVideoView.setLayoutParams(savedHdVideoViewLayout);
			hDVideoView.setId(R.id.HDVideoView);
		}
		hDVideoView.mediaplayer = this;
		if (inBackground != 0)
			return ;
		if (WaxPlayService.mediaplayer.mainLayout.getVisibility() == View.INVISIBLE || WaxPlayService.mediaplayer.mainLayout.getVisibility() == View.GONE)
			hDVideoView.pause2();
		hDVideoView.setFocusable(false);
		hDVideoView.setFocusableInTouchMode(false);
		hDVideoView.setOnTouchListener(this);
		// Add HDImageView to HDVideoView flow
		if (mediaFormat == FMT_PHOTO) {
			if (hDImageView == null) {
				if (savedHdImageViewAttrs == null) {
					hDImageView = new HDImageView(this.getContext());
				} else {
					try { /*NullPointerException@XmlBlock.nativeGetStyleAttribute on Android TV Box*/
						hDImageView = new HDImageView(this.getContext(), savedHdImageViewAttrs, savedHdImageViewStyle);
					} catch (Exception e) {
						hDImageView = new HDImageView(this.getContext());
					}
				}
				savedHdImageViewLayout.width = LayoutParams.FILL_PARENT; // Scale changed layout params
				savedHdImageViewLayout.height = LayoutParams.FILL_PARENT;
				hDImageView.setLayoutParams(savedHdImageViewLayout);
				hDImageView.setId(R.id.HDImageView);
			}
			hDImageView.mediaplayer = this;
			hDImageView.setFocusable(false);
			hDImageView.setFocusableInTouchMode(false);
			hDImageView.setOnTouchListener(this);
			if (WaxPlayService.destroyVideoView == true) {
				if (wholeLayout.indexOfChild(hDImageView) < 0)
					wholeLayout.addView(hDImageView);
			} else {
				hDImageView.setVisibility(View.VISIBLE);
			}
			wholeLayout.bringChildToFront(hDImageView);
		} else { /* Moved here to avoid ImageView fading splash */
			if (WaxPlayService.destroyVideoView == true) {
				if (wholeLayout.indexOfChild(hDVideoView) < 0)
					wholeLayout.addView(hDVideoView);
			} else {
				hDVideoView.setVisibility(View.VISIBLE);
			}
			wholeLayout.bringChildToFront(hDVideoView);
		}
		ResetFrontLayout();
	}

	private void CreateSdVideoView() {
		if (sDVideoView == null) {
			if (savedSdVideoViewAttrs == null || inBackground != 0) {
				sDVideoView = new SDVideoView(this.getContext());
			} else {
				try { /*NullPointerException@XmlBlock.nativeGetStyleAttribute on Android TV Box*/
					sDVideoView = new SDVideoView(this.getContext(), savedSdVideoViewAttrs, savedSdVideoViewStyle);
				} catch (Exception e) {
					sDVideoView = new SDVideoView(this.getContext());
				}
			}
			savedSdVideoViewLayout.width = LayoutParams.FILL_PARENT; // Scale changed layout params
			savedSdVideoViewLayout.height = LayoutParams.FILL_PARENT;
			sDVideoView.setLayoutParams(savedSdVideoViewLayout);
			sDVideoView.setId(R.id.SDVideoView);
		}
		sDVideoView.mediaplayer = this;
		if (inBackground != 0)
			return ;
		if (WaxPlayService.mediaplayer.mainLayout.getVisibility() == View.INVISIBLE || WaxPlayService.mediaplayer.mainLayout.getVisibility() == View.GONE)
			sDVideoView.pause2();
		sDVideoView.setFocusable(false);
		sDVideoView.setFocusableInTouchMode(false);
		sDVideoView.setOnTouchListener(this);
		if (WaxPlayService.destroyVideoView == true) {
			if (wholeLayout.indexOfChild(sDVideoView) < 0)
				wholeLayout.addView(sDVideoView);
		} else {
			sDVideoView.setVisibility(View.VISIBLE);
		}
		wholeLayout.bringChildToFront(sDVideoView);
		// Add SDNativeView to SDVideoView flow
		if (mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || mediaFormat == FMT_MIRROR4) {
			if (sDNativeViewEnabled == true) {
				if (sDNativeView == null) {
					if (savedSdNativeViewAttrs == null) {
						sDNativeView = new SDNativeView(this.getContext());
					} else {
						try { /*NullPointerException@XmlBlock.nativeGetStyleAttribute on Android TV Box*/
							sDNativeView = new SDNativeView(this.getContext(), savedSdNativeViewAttrs, savedSdNativeViewStyle);
						} catch (Exception e) {
							sDNativeView = new SDNativeView(this.getContext());
						}
					}
					savedSdNativeViewLayout.width = LayoutParams.FILL_PARENT; // Scale changed layout params
					savedSdNativeViewLayout.height = LayoutParams.FILL_PARENT;
					sDNativeView.setLayoutParams(savedSdNativeViewLayout);
					sDNativeView.setId(R.id.SDNativeView);
				}
				if (WaxPlayService.mediaplayer.mainLayout.getVisibility() == View.INVISIBLE || WaxPlayService.mediaplayer.mainLayout.getVisibility() == View.GONE)
					sDNativeView.pause2();
				sDNativeView.mediaplayer = this;
				sDNativeView.setFocusable(false);
				sDNativeView.setFocusableInTouchMode(false);
				sDNativeView.setOnTouchListener(this);
				if (WaxPlayService.destroyVideoView == true) {
					if (sDVideoView != null && wholeLayout.indexOfChild(sDVideoView) >= 0) {
						WaxPlayer.HideWholeLayoutSubview(sDVideoView);
						wholeLayout.removeView(sDVideoView);
					}
					if (wholeLayout.indexOfChild(sDNativeView) < 0)
						wholeLayout.addView(sDNativeView);
				} else {
					sDVideoView.setVisibility(View.GONE);
					sDNativeView.setVisibility(View.VISIBLE);
				}
				wholeLayout.bringChildToFront(sDNativeView);
				initSdNativeView();
			}
		}
		ResetFrontLayout();
	}

	private void CreateGalleryView() {
		if (inBackground != 0)
			return ;
		if (mGalleryView == null) {
			if (savedGalleryViewAttrs == null) {
				mGalleryView = new GalleryView(this.getContext());
			} else {
				try { /*NullPointerException@XmlBlock.nativeGetStyleAttribute on Android TV Box*/
					mGalleryView = new GalleryView(this.getContext(), savedGalleryViewAttrs, savedGalleryViewStyle);
				} catch (Exception e) {
					mGalleryView = new GalleryView(this.getContext());
				}
			}
			savedGalleryViewLayout.width = LayoutParams.FILL_PARENT; // Scale changed layout params
			savedGalleryViewLayout.height = LayoutParams.FILL_PARENT;
			mGalleryView.setLayoutParams(savedGalleryViewLayout);
			mGalleryView.setId(R.id.GalleryView);
			mGalleryView.setVisibility(View.INVISIBLE);
		}
		mGalleryView.mediaplayer = this;
		mGalleryView.setFocusable(false);
		mGalleryView.setFocusableInTouchMode(false);
		mGalleryView.setOnTouchListener(this);
		if (WaxPlayService.destroyVideoView == true) {
			if (wholeLayout.indexOfChild(mGalleryView) < 0)
				wholeLayout.addView(mGalleryView);
		} else {
			mGalleryView.setVisibility(View.VISIBLE);
		}
		wholeLayout.bringChildToFront(mGalleryView);
		ResetFrontLayout();
	}

	private void initSdNativeView() {
		if (inBackground != 0)
			return ;
		if (sDNativeViewEnabled == false || sDNativeView == null)
			return;

		sDNativeView.setOnPreparedListener(new com.waxrain.video.SDNativeView$OnPreparedListener() {
			@Override
			public void onPrepared(int serverType, String serverName) {
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] prepare2.......begin\n");
				if (whichPlayer != PLAYER_VITAMIO || playback_State == STATE_STOPPED)
					return ;
				whichPlayerSaved = PLAYER_VITAMIO;
				if (playback_State != STATE_PLAYING) // resumed playing
					playback_State = STATE_PREPARED;
				Global.do_sleep(50); // Schedule Play()
				send_videoScale(1, Config.ZOOM);
				if (serverType >= 0) {
					mirrorServerType = serverType;
					/*if (serverType != 0) // WINDOWS
						CURSOR_DIRECT_MODE = CURSOR_DIRECT_MODE_SAVED = false;
					else // ANDROID
						CURSOR_DIRECT_MODE = CURSOR_DIRECT_MODE_SAVED = true;*/
				}
				send_showHostname(serverName, 10);
				waxHandler.sendEmptyMessageDelayed(MSG_CHECK_BTHID_INIT, 1000);
			}
		});
		sDNativeView.setOnCompletionListener(new com.waxrain.video.SDNativeView$OnCompletionListener() {
			@Override
			public void onCompletion(int excode) {
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onCompletion2.......\n");
				if (whichPlayer != PLAYER_VITAMIO || playback_State == STATE_STOPPED || isExiting() == true)
					return ;
				waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
				send_playFinish(excode);
			}
		});
		sDNativeView.setOnErrorListener(new com.waxrain.video.SDNativeView$OnErrorListener() {
			@Override
			public boolean onError(int errcode) {
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onError2.......\n");
				if (whichPlayer != PLAYER_VITAMIO || playback_State == STATE_STOPPED || isExiting() == true)
					return true;
				waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
				if (isExiting() == false) {
					EnterExit();
					send_playFailure(Integer.toHexString(2) + "-" + Integer.toHexString(-errcode));
				}
				return true;
			}
		});
	}

	private void initHdVideoView() {
		DestroyAllVideoViews();
		CreateHdVideoView();

		hDVideoView.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
				// Error during STATE_PLAYING and Seek() and Play()
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] Error0.......begin\n");
				if (whichPlayer != PLAYER_INNER || playback_State == STATE_STOPPED || isExiting() == true)
					return true;
				if (sDVideoViewEnabled == true && playback_State == STATE_PREPARING && tickTimer == null) {
					if (WaxPlayService.Inner_As_Vitamio == false) {
						whichPlayer = PLAYER_VITAMIO; // Change the Player
						reconfigVitamioPlayer();
					}
					waxHandler.sendEmptyMessage(MSG_START_PLAY);
				} else {
					// For Seek()/SwitchPlayer()/Play() or in STATE_PLAYING
					if (sDVideoViewEnabled == true && (mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO)) {
						if (WaxPlayService.Inner_As_Vitamio == false) {
							whichPlayer = PLAYER_VITAMIO;
							reconfigVitamioPlayer();
						}
						waxHandler.sendEmptyMessage(MSG_SHOW_LOADING);
						waxHandler.sendEmptyMessage(MSG_START_PLAY);
						return true;
					}
					waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
					if (isExiting() == false) {
						EnterExit(); // Avoid of multi onError message
						send_playFailure(Integer.toHexString(what) + "-" + Integer.toHexString(extra));
					}
				}
				return true;
			}
		});
		hDVideoView.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(android.media.MediaPlayer mp) {
				// onPrepared during Play() or STATE_PLAYING
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] prepare0.......begin\n");
				if (whichPlayer != PLAYER_INNER || playback_State == STATE_STOPPED)
					return ;
				whichPlayerSaved = PLAYER_INNER;
				if (playback_State != STATE_PREPARING) {
					waxHandler.sendEmptyMessage(MSG_HIDE_LOADING); // State incorrect
					send_updateIconPlay(0, 0, -1); // Switching Player : restart GalleryView
				} else {
					CheckBufferingPlaying();
				}
				send_videoScale(1, Config.ZOOM);
			}
		});
		hDVideoView.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(android.media.MediaPlayer mp) {
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onCompletion0.......\n");
				if (whichPlayer != PLAYER_INNER || playback_State == STATE_STOPPED || isExiting() == true)
					return ;
				waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
				send_playFinish(0);
			}
		});
		hDVideoView.setOnSeekCompleteListener(new android.media.MediaPlayer.OnSeekCompleteListener() {
			@Override
			public void onSeekComplete(android.media.MediaPlayer mp) {
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onSeekComplete0.......\n");
				if (whichPlayer != PLAYER_INNER || playback_State == STATE_STOPPED)
					return ;
				waxHandler.sendEmptyMessage(MSG_HIDE_LOADING2);
				ResumeFromSeekCompletion(true);
			}
		});
		hDVideoView.setOnVideoSizeChangedListener(new android.media.MediaPlayer.OnVideoSizeChangedListener() {
			@Override
			public void onVideoSizeChanged(android.media.MediaPlayer mp, int width, int height) {
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onVideoSizeChanged0: (" + width + "x" + height + ")");
				if (whichPlayer != PLAYER_INNER || playback_State == STATE_STOPPED)
					return ;
				send_videoSizeChanged(width, height);
			}
		});
		hDVideoView.setOnBufferingUpdateListener(new android.media.MediaPlayer.OnBufferingUpdateListener() {
			@Override
			public void onBufferingUpdate(android.media.MediaPlayer mp, int percent) {
				if (whichPlayer != PLAYER_INNER || playback_State == STATE_STOPPED)
					return ;
				send_bufferUpdated(percent);
			}
		});
	}

	private void initSdVideoView() {
		DestroyAllVideoViews();
		CreateSdVideoView();

		sDVideoView.setOnErrorListener(new tv.danmaku.ijk.media.player.IMediaPlayer$OnErrorListener() {
			@Override
			public boolean onError(tv.danmaku.ijk.media.player.IMediaPlayer mp, int what, int extra) {
				// Error during STATE_PLAYING and Seek() and Play()
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] Error1.......begin\n");
				if (whichPlayer != PLAYER_VITAMIO || playback_State == STATE_STOPPED || isExiting() == true)
					return true;
				waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
				if (playback_sderror_retry < Config.PLAYER_MAX_RETRY && (seekTime+30*1000) < duration && duration < MaxDurationAV && (
					playback_State == STATE_PLAYING || playback_State == STATE_PAUSED ) ) {
					seekTime += 1000;
					playback_sderror_retry ++;
					waxHandler.sendEmptyMessage(MSG_START_PLAY);
				} else {
					if (isExiting() == false) {
						EnterExit();
						send_playFailure(Integer.toHexString(what) + "-" + Integer.toHexString(extra));
					}
				}
				return true;
			}
		});
		sDVideoView.setOnPreparedListener(new tv.danmaku.ijk.media.player.IMediaPlayer$OnPreparedListener() {
			@Override
			public void onPrepared(tv.danmaku.ijk.media.player.IMediaPlayer arg0) {
				// onPrepared during Play() or STATE_PLAYING
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] prepare1.......begin\n");
				if (whichPlayer != PLAYER_VITAMIO || playback_State == STATE_STOPPED)
					return ;
				whichPlayerSaved = PLAYER_VITAMIO;
				if (playback_State != STATE_PREPARING) {
					waxHandler.sendEmptyMessage(MSG_HIDE_LOADING); // State incorrect
					send_updateIconPlay(0, 0, -1); // Switching Player : restart GalleryView
				} else {
					CheckBufferingPlaying();
				}
				send_videoScale(1, Config.ZOOM);
			}
		});
		sDVideoView.setOnCompletionListener(new tv.danmaku.ijk.media.player.IMediaPlayer$OnCompletionListener() {
			@Override
			public void onCompletion(tv.danmaku.ijk.media.player.IMediaPlayer mp) {
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onCompletion1.......\n");
				if (whichPlayer != PLAYER_VITAMIO || playback_State == STATE_STOPPED || isExiting() == true)
					return ;
				waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
				send_playFinish(0);
			}
		});
		sDVideoView.setOnSeekCompleteListener(new tv.danmaku.ijk.media.player.IMediaPlayer$OnSeekCompleteListener() {
			@Override
			public void onSeekComplete(tv.danmaku.ijk.media.player.IMediaPlayer mp) {
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onSeekComplete1.......\n");
				if (whichPlayer != PLAYER_VITAMIO || playback_State == STATE_STOPPED)
					return ;
				waxHandler.sendEmptyMessage(MSG_HIDE_LOADING2);
				ResumeFromSeekCompletion(true);
			}
		});
		sDVideoView.setOnTimedTextListener(new tv.danmaku.ijk.media.player.IMediaPlayer$OnTimedTextListener() {
			@Override
			public void onTimedText(tv.danmaku.ijk.media.player.IMediaPlayer mp, tv.danmaku.ijk.media.player.IjkTimedText text) {
				//Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onTimedText\n");
				if (whichPlayer != PLAYER_VITAMIO || playback_State == STATE_STOPPED)
					return ;
				String textStr = "";
				if (text != null && text.getText() != null)
					textStr = text.getText();
				send_innerTextUpdated(textStr);
			}
		});
		sDVideoView.setOnVideoSizeChangedListener(new tv.danmaku.ijk.media.player.IMediaPlayer$OnVideoSizeChangedListener() {
			@Override
			public void onVideoSizeChanged(tv.danmaku.ijk.media.player.IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"] onVideoSizeChanged1: (" + width + "x" + height + ")");
				if (whichPlayer != PLAYER_VITAMIO || playback_State == STATE_STOPPED)
					return ;
				send_videoSizeChanged(width, height);
			}
		});
		sDVideoView.setOnBufferingUpdateListener(new tv.danmaku.ijk.media.player.IMediaPlayer$OnBufferingUpdateListener() {
			@Override
			public void onBufferingUpdate(tv.danmaku.ijk.media.player.IMediaPlayer mp, int percent) {
				if (whichPlayer != PLAYER_VITAMIO || playback_State == STATE_STOPPED)
					return ;
				send_bufferUpdated(percent);
			}
		});
	}

	private void loadChromeViewJs2(android.webkit.WebView webview, String jscode) {
		try {
			final android.webkit.WebView webview2 = webview;
			final String jscode2 = jscode;
			webview.post(new Runnable() {
				public void run() {
					loadChromeViewJs(webview2, jscode2);
				}
			});
		} catch (Exception e) {
		} catch (Throwable e) {
		}
	}

	private void loadChromeViewJs(android.webkit.WebView webview, String jscode) {
		try {
			if (android.os.Build.VERSION.SDK_INT >= 19) {
				//webview.evaluateJavascript(jscode, null);
				return;
			}
			//webview.loadUrl("javascript:" + jscode);
		} catch (Exception e) {
		} catch (Throwable e) {
		}
	}

	private void initChromeView(String url) {
		if (inBackground != 0)
			return ;
		if (mChromeView == null) {
			try {
				mChromeView = (Object)new android.webkit.WebView(WaxPlayer2.this.getActivity());
				android.webkit.WebSettings settings = ((android.webkit.WebView)mChromeView).getSettings();
				//settings.setPluginState(android.webkit.WebSettings.PluginState.ON);
				//if (android.os.Build.VERSION.SDK_INT < 33 && WaxPlayer2.this.getContext().getApplicationInfo().targetSdkVersion < 33)
				//	settings.setAppCacheEnabled(true);
				settings.setDatabaseEnabled(true);
				settings.setDomStorageEnabled(true);
				settings.setJavaScriptEnabled(true);
				settings.setJavaScriptCanOpenWindowsAutomatically(true);
				settings.setUseWideViewPort(true);
				settings.setLoadWithOverviewMode(true);
				settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
				settings.setAllowFileAccess(false);
				if (android.os.Build.VERSION.SDK_INT >= 11)
					settings.setAllowContentAccess(false);
				if (android.os.Build.VERSION.SDK_INT >= 16) {
					settings.setAllowFileAccessFromFileURLs(false);
					settings.setAllowUniversalAccessFromFileURLs(false);
				}
				if (android.os.Build.VERSION.SDK_INT >= 21) {
					settings.setMediaPlaybackRequiresUserGesture(false);
					settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
				}
				if (userAgent != null && userAgent.length() > 0)
					settings.setUserAgentString(userAgent);
				((android.webkit.WebView)mChromeView).setWebViewClient(new android.webkit.WebViewClient() {
					@Override
					public void onReceivedError(android.webkit.WebView view, int errorCode, String description, String failingUrl) {
						super.onReceivedError(view, errorCode, description, failingUrl);
						EnterExit();
						send_playFailure(Integer.toHexString(3) + "-" + Integer.toHexString(1));
					}
					@Override
					public void onPageStarted(android.webkit.WebView view, String url, Bitmap favicon) {
						super.onPageStarted(view, url, favicon);
						if (playback_State == STATE_PREPARED) {
							playback_State = STATE_PLAYING;
							WaxPlayer2.this.duration = MaxDurationAV/2;
						}
					}
					@Override
					public void onPageFinished(android.webkit.WebView view, String url) {
						super.onPageFinished(view, url);
						try {
							if (mChromeView != null)
								((android.webkit.WebView)mChromeView).setVisibility(View.VISIBLE);
						} catch (Exception e) {
						} catch (Throwable e) {
						}
						waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
						loadChromeViewJs2(view, chromeView_jscode);
					}
					@Override
					public android.webkit.WebResourceResponse shouldInterceptRequest(android.webkit.WebView view, String str) {
						if (android.os.Build.VERSION.SDK_INT >= 21 && (
							str.toLowerCase().endsWith("cast_receiver.js") || str.toLowerCase().endsWith("cast_receiver_framework.js") ) ) {
							loadChromeViewJs2(view, chromeView_jscode);
						}
						return super.shouldInterceptRequest(view, str);
					}
				});
				((android.webkit.WebView)mChromeView).setWebChromeClient(new android.webkit.WebChromeClient() {
					@Override
					public void onCloseWindow(android.webkit.WebView webView) {
						// TODO
					}
					@Override
					public void onPermissionRequest(android.webkit.PermissionRequest permissionRequest) {
						if (android.os.Build.VERSION.SDK_INT >= 21) {
							String[] resources = permissionRequest.getResources();
							for (String equals : resources) {
								if ("android.webkit.resource.PROTECTED_MEDIA_ID".equals(equals)) {
									permissionRequest.grant(new String[]{"android.webkit.resource.PROTECTED_MEDIA_ID"});
									return;
								}
							}
						}
						super.onPermissionRequest(permissionRequest);
					}
				});
				RelativeLayout.LayoutParams webViewLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
				webViewLP.addRule(RelativeLayout.CENTER_IN_PARENT);
				((android.webkit.WebView)mChromeView).setLayoutParams(webViewLP);
				((android.webkit.WebView)mChromeView).setVisibility(View.INVISIBLE);
			} catch (Exception e) {
			} catch (Throwable e) {
			}
		}
		try {
			((android.webkit.WebView)mChromeView).setFocusable(false);
			((android.webkit.WebView)mChromeView).setFocusableInTouchMode(false);
			//((android.webkit.WebView)mChromeView).setOnTouchListener(this);
			if (WaxPlayService.destroyVideoView == true) {
				if (wholeLayout.indexOfChild((android.webkit.WebView)mChromeView) < 0)
					wholeLayout.addView((android.webkit.WebView)mChromeView);
			} else {
				((android.webkit.WebView)mChromeView).setVisibility(View.VISIBLE);
			}
		} catch (Exception e) {
		} catch (Throwable e) {
		}
		wholeLayout.bringChildToFront((android.webkit.WebView)mChromeView);
		ResetFrontLayout();
		ctrlLayout.setVisibility(View.GONE);
		try {
			((android.webkit.WebView)mChromeView).requestFocus();
			if (android.os.Build.VERSION.SDK_INT < 21)
				loadChromeViewJs((android.webkit.WebView)mChromeView, chromeView_jscode);
			((android.webkit.WebView)mChromeView).loadUrl(url);
		} catch (Exception e) {
		} catch (Throwable e) {
		}
		playback_State = STATE_PREPARED;
	}

	private void initHwCastView() {
		if (inBackground != 0)
			return ;
		WaxPlayService.HWS_CreateView(this, wholeLayout, R.id.SDNativeView, WaxPlayService.destroyVideoView);
		ResetFrontLayout();
		if (Config.HWS_ENABLED == 0)
			return ;
		try {
			if (HWS_mWlanImageView == null) {
				HWS_mWlanImageView = this.getView().findViewById(R.id.hws_wlan_imageview);
				try {
					HWS_mDrawableNetworkWorse = getResources().getDrawable(R.drawable.hws_ic_network_worse_icon);
					HWS_mDrawableNetworkBad = getResources().getDrawable(R.drawable.hws_ic_network_bad_icon);
					HWS_mDrawableNetworkGeneral = getResources().getDrawable(R.drawable.hws_ic_network_general_icon);
					HWS_mVectorAnimDrawableNetworkWorse = getResources().getDrawable(R.drawable.hws_vector_anim_worse_show);
					HWS_mVectorAnimDrawableNetworkGeneral = getResources().getDrawable(R.drawable.hws_vector_anim_general_show);
					HWS_mVectorAnimDrawableNetworkBad = getResources().getDrawable(R.drawable.hws_vector_anim_bad_show);
				} catch (Exception e) {
				}
			}
			HWS_dispatchDrawableSet(WaxPlayService.HWS_NETWORK_QUALITY_GOOD);
			HWS_mWlanImageView.bringToFront();
			if (HWS_BroadcastReceiver == null) {
				HWS_BroadcastReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						try {
							String action = intent.getAction();
							if (WaxPlayService.HWS_BROADCAST_ACTION_FINISH_PLAY_ACTIVITY.equals(action)) {
								waxHandler.sendEmptyMessage(MSG_PLAY_DONE);
							} else if (WaxPlayService.HWS_BROADCAST_ACTION_NETWORK_QUALITY.equals(action)) {
								int networkQuality = intent.getIntExtra("networkquality", -1000/*INVALID_NETWORK_QUALITY*/);
								if (networkQuality == WaxPlayService.HWS_NETWORK_QUALITY_EXCEPTION) {
									HWS_mWlanImageView.setVisibility(View.GONE);
									HWS_networkBreakToast();
								} else if (networkQuality == WaxPlayService.HWS_NETWORK_QUALITY_WORSE || 
									networkQuality == WaxPlayService.HWS_NETWORK_QUALITY_BAD || 
									networkQuality == WaxPlayService.HWS_NETWORK_QUALITY_GENERAL || 
									networkQuality == WaxPlayService.HWS_NETWORK_QUALITY_GOOD ) {
									HWS_dispatchDrawableSet(networkQuality);
								}
							}
						} catch (Exception e) {
						}
					}
				};
			}
			IntentFilter broadcastFilter = new IntentFilter();
			broadcastFilter.addAction(WaxPlayService.HWS_BROADCAST_ACTION_FINISH_PLAY_ACTIVITY);
			broadcastFilter.addAction(WaxPlayService.HWS_BROADCAST_ACTION_NETWORK_QUALITY);
			if (android.os.Build.VERSION.SDK_INT >= 26 && WaxPlayer2.this.getContext().getApplicationInfo().targetSdkVersion >= 34)
				WaxPlayer2.this.getContext().registerReceiver(HWS_BroadcastReceiver, broadcastFilter, Context.RECEIVER_EXPORTED);
			else
				WaxPlayer2.this.getContext().registerReceiver(HWS_BroadcastReceiver, broadcastFilter);
		} catch (Exception e) {
		}
	}

	private void HWS_dispatchDrawableSet(int networkQuality) {
		if (Config.HWS_ENABLED == 0)
			return ;
		try {
			if (networkQuality == WaxPlayService.HWS_NETWORK_QUALITY_GOOD) {
				HWS_mWlanImageView.clearAnimation();
				HWS_mWlanImageView.setVisibility(View.GONE);
				HWS_needVectorAnimShow = true;
			} else {
				HWS_mWlanImageView.setVisibility(View.VISIBLE);
				if (HWS_needVectorAnimShow) {
					HWS_setVectorAnimDrawable(networkQuality);
					HWS_needVectorAnimShow = false;
				} else {
					HWS_setVectorDrawable(networkQuality);
				}
			}
		} catch (Exception e) {
		}
	}

	private void HWS_setVectorAnimDrawable(int networkQuality) {
		if (Config.HWS_ENABLED == 0)
			return ;
		if (networkQuality == WaxPlayService.HWS_NETWORK_QUALITY_WORSE && HWS_mVectorAnimDrawableNetworkWorse != null) {
			HWS_mWlanImageView.setImageDrawable(HWS_mVectorAnimDrawableNetworkWorse);
			HWS_startVectorAnimator();
		} else if (networkQuality == WaxPlayService.HWS_NETWORK_QUALITY_BAD && HWS_mVectorAnimDrawableNetworkBad != null) {
			HWS_mWlanImageView.setImageDrawable(HWS_mVectorAnimDrawableNetworkBad);
			HWS_startVectorAnimator();
		} else if (networkQuality == WaxPlayService.HWS_NETWORK_QUALITY_GENERAL && HWS_mVectorAnimDrawableNetworkGeneral != null) {
			HWS_mWlanImageView.setImageDrawable(HWS_mVectorAnimDrawableNetworkGeneral);
			HWS_startVectorAnimator();
		}
	}

    private void HWS_startVectorAnimator() {
		if (Config.HWS_ENABLED == 0)
			return ;
		if (android.os.Build.VERSION.SDK_INT >= 21) {
			HWS_animatedImageDrawable = (Object)HWS_mWlanImageView.getDrawable();
			((AnimatedVectorDrawable)HWS_animatedImageDrawable).start();
		}
	}

	private void HWS_setVectorDrawable(int networkQuality) {
		if (Config.HWS_ENABLED == 0)
			return ;
		if (networkQuality == WaxPlayService.HWS_NETWORK_QUALITY_WORSE && HWS_mDrawableNetworkWorse != null) {
			HWS_mWlanImageView.setImageDrawable(HWS_mDrawableNetworkWorse);
		} else if (networkQuality == WaxPlayService.HWS_NETWORK_QUALITY_BAD && HWS_mDrawableNetworkBad != null) {
			HWS_mWlanImageView.setImageDrawable(HWS_mDrawableNetworkBad);
		} else if (networkQuality == WaxPlayService.HWS_NETWORK_QUALITY_GENERAL && HWS_mDrawableNetworkGeneral != null) {
			HWS_mWlanImageView.setImageDrawable(HWS_mDrawableNetworkGeneral);
		}
	}

	private void HWS_networkBreakToast() {
		if (Config.HWS_ENABLED == 0)
			return ;
		ImageView imageView = new ImageView(this.getActivity().getApplicationContext());
		imageView.setImageDrawable(getResources().getDrawable(R.drawable.hws_ic_toast));
		Toast toast = new Toast(this.getActivity().getApplicationContext());
		toast.setGravity(Gravity.BOTTOM,0,10);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.setView(imageView);
		toast.show();
	}

	private int PlayInner(int i, int EXTRA_WAIT_PLAY, boolean threadMode) {
		int ret = 0;
		for ( ; i < 2 && inBackground != 1; i++) {
			int timeout = WaxPlayService.WAIT_PLAY_TIMEOUT/3;
			while (inBackground != 1 && whichPlayer != PLAYER_NONE && playback_State == STATE_STOPPED && timeout > 0) {
				Global.do_sleep(50); // Schedule message handler to switch STATE_PREPARING
				timeout -= 50;
			}
			playback_timeout = 0;
			while (inBackground != 1 && whichPlayer != PLAYER_NONE && playback_State == STATE_PREPARING && playback_timeout < ((WaxPlayService.WAIT_PLAY_TIMEOUT+EXTRA_WAIT_PLAY)*4/5)) {
				Global.do_sleep(50);
				playback_timeout += 50;
			}
			if (inBackground != 1 && whichPlayer != PLAYER_NONE && playback_State == STATE_PREPARED) {
				while (inBackground != 1 && whichPlayer != PLAYER_NONE && playback_State != STATE_STOPPED && !is_Playing2() && playback_timeout < (WaxPlayService.WAIT_PLAY_TIMEOUT+EXTRA_WAIT_PLAY)) {
					Global.do_sleep(50);
					playback_timeout += 50;
				}
				if (whichPlayer != PLAYER_NONE && playback_State != STATE_STOPPED && playback_timeout < (WaxPlayService.WAIT_PLAY_TIMEOUT+EXTRA_WAIT_PLAY)) {
					if (whichPlayer == PLAYER_INNER && GetDuration() < 0 && mediaFormat != FMT_PHOTO && sDVideoViewEnabled == true) {
						if (IgnoreMediaDuration == true) { // For live broadcast on Mi1S Box
							Log.i(LOG_TAG,"MPlayer["+fragment_id+"]...duration = " + GetDuration());
							this.duration = MaxDurationAV;
						} else { // For Sony MT15i Phone Android 4.0.4
							whichPlayer = PLAYER_VITAMIO;
							need_Reset_State = true;
							waxHandler.sendEmptyMessage(MSG_START_PLAY);
							Global.do_sleep(1000); // Enough for schedule to reset state
							continue;
						}
					} else if (GetDuration() <= 0 && mediaFormat != FMT_PHOTO) { // For devices which can't get right duration
						int timeout2 = ( (whichPlayer == PLAYER_INNER) ? 5000 : 3000 );
						while (timeout2 > 0 && whichPlayer != PLAYER_NONE && playback_State != STATE_STOPPED) {
							Global.do_sleep(50);
							timeout2 -= 50;
							if (GetDuration() > 0)
								break;
						}
						if (timeout2 == 0)
							this.duration = MaxDurationAV; // For PPLive live broadcast
					}
				}
				if (whichPlayer != PLAYER_NONE && playback_State != STATE_STOPPED && playback_timeout < (WaxPlayService.WAIT_PLAY_TIMEOUT+EXTRA_WAIT_PLAY)) {
					playback_State = STATE_PLAYING;
					waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
					waxHandler.sendEmptyMessage(MSG_UPDATE_DURATION);
					send_updateIconPlay(1, 0, -1);
				}
			}
			ret = 0;
			if (whichPlayer != PLAYER_NONE && playback_State == STATE_PLAYING) {
				if (mediaFormat == FMT_PHOTO)
					waxHandler.sendEmptyMessage(MSG_PLAY_DONE);
				if (mediaFormat == FMT_VIDEO && servPort >= 56789 && servPort <= 56798) {
					// For XunLei KanKan: it returns video stream and cause onCompletion() when requesting subtitle file
					if (supportSrtFmts == null) {
						supportSrtFmts = new ArrayList<String>();
						supportSrtFmts.add(".srt");
						supportSrtFmts.add(".ssa");
						supportSrtFmts.add(".ass");
					}
					if (WaxPlayService._config.getSrtEnable() == true) {
						new Thread() {
							public void run() {
								int j = 0;
								for (j = 0; j < supportSrtFmts.size() && playback_State != STATE_STOPPED; j ++) {
									String srtPath = url.substring(0, url.lastIndexOf('.')) + supportSrtFmts.get(j);
									Srt_Parse = new SrtParse();
									if (Srt_Parse.parseSRT(srtPath, 15000) > 0) {
										if (WaxPlayService.AIRPIN_IS_LITE == true && WaxPlayer.waxPlayService != null)
											WaxPlayer.waxPlayService.PostDisplayToast(8, false, WaxPlayer2.this.getContext().getString(R.string.set_srt_disabled_lite), 0);
										break;
									}
									Srt_Parse.destroySRT();
									Srt_Parse = null;
								}
							}
						}.start();
					}
				}
				initTickTimer(TIMER_SCHED, TIMER_SCHED);
				ret = 1;
				// Do the real job after Player is finally started
				//if (WaxPlayService.AudioPlayerMap.size() > 0 && (mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO) ) // Fake Pause AirTunes
				//	WaxPlayService.AO_streamAudioFocused = false;
				WaxPlayService.START_TRACKER_Volume_AudioFocus();
				String reportUrl = this.url;
				if (mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || mediaFormat == FMT_MIRROR4)
					reportUrl = "http://mirroring_" + mediaFormat;
				if (is_Proxy_HLS == true)
					reportUrl = this.url2;
				WaxPlayService.send_MediaStart(client_id, mediaFormat, mediaProto, reportUrl, this.duration);
				if (inBackground == 1) {
					gotoStop(false, 1, false);
					if (WaxPlayer.waxPlayService != null)
						WaxPlayer.waxPlayService.MediaPause(client_id);
				} else {
					if (mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO)
						WaxPlayService.requestAudioFocus();
				}
			} else if ((sDVideoViewEnabled == true || WaxPlayService.Inner_As_Vitamio == true) && 
						whichPlayer == PLAYER_INNER && playback_State != STATE_STOPPED) { // Timeout for INNER
				if (WaxPlayService.Inner_As_Vitamio == false)
					whichPlayer = PLAYER_VITAMIO; // Change the Player
				need_Reset_State = true;
				waxHandler.sendEmptyMessage(MSG_START_PLAY);
				Global.do_sleep(1000); // Enough for schedule to reset state
				continue;
			} else if (i == 0 && playback_State != STATE_STOPPED && whichPlayer == PLAYER_VITAMIO) { // onPrepared() then onError() for INNER
				continue;
			}
			break;
		}
		if (inBackground == 1 && playback_State != STATE_STOPPED)
			playback_State = STATE_STOPPED; // Reset it again
		requestFinished --;
		if (ret == 0 && threadMode == true) {
			waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
			if (isExiting() == false) {
				EnterExit();
				send_playFailure("");
			}
		}
		return ret;
	}

	public int Play(String url1, String url2, float startPosition, int duration, int contentSize, String title1, String userAgent1) {
		synchronized (playLock) {
			if (playback_State != STATE_STOPPED || url1 == null)
				return 0;
			if (wholeView == null) {
				gotoFinish(0);
				return -2;
			}
			if ((client_id == null || client_id.startsWith("127.0.0.") == false) && (
				(WaxPlayService.last_startAirplay == 0 && mediaProto == PROTOCOL_AIRPLAY) || 
				(WaxPlayService.last_startDLNA == 0 && mediaProto == PROTOCOL_DLNA) || 
				(WaxPlayService.last_startDial == 0 && mediaProto == PROTOCOL_DIAL) ) )
				return -1;
			int EXTRA_WAIT_PLAY = 0;
			//if ((mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO) && url1.startsWith("/") == false)
			//	Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Play (" + url1 + ") from " + startPosition);
			UpdateAudioMetadata(true, true, "", "", "", "", "");/*MUST after INIT/RESET of mGalleryView*/
			WaxPlayService.playerSessionId ++;
			this.url = this.url_hd = this.url_sd = url1;
			this.url2 = url2;
			this.title = title1;
			this.userAgent = userAgent1;
			this.seekTime = 0;
			this.seekTime2 = 0;
			this.startPercent = startPosition;
			this.contentSize = contentSize;
			this.duration = 0;
			this.lastPos = 0;
			this.lastBuffProg = 0;
			this.videoWidth = 0;
			this.videoHeight = 0;
			this.sb_last_progress = -1;
			this.sb_touch_progress = false;
			this.duringSeeking = false;
			this.pauseReqLevel = -1;
			this.COUNT_REBUFFERING = 0;
			this.plistCheck = true;//false; // User seek is prior
			this.need_Reset_State = false;
			this.playback_sderror_retry = 0;
			this.servPort = 0; // 56789-56798: AirPin Senders
			this.mBtHidBondedDevMac = null;
			this.mBtHidDevWidth = this.mBtHidDevHeight = 0;
			this.dialogBtHidSelectStarted = false;
			this.zoomState = WaxPlayer.zoomState = Config.ZOOM;
			if (url != null && url.length() > 10 && url.startsWith("http") && url.lastIndexOf('.') > 0) {
				try {
					int index = url.startsWith("https://") ? 8 : 7;
					int index2 = url.substring(index).indexOf("/");
					if (index2 > 0) {
						int index1 = url.substring(index, index+index2).indexOf(":");
						if (index1 > 0) {
							servPort = Integer.parseInt(url.substring(index+index1+1, index+index2));
							Log.i(LOG_TAG,"MPlayer["+fragment_id+"] Found port: " + servPort);
						}
					}
				} catch (Exception e) {
				}
			}
			WaxPlayService._config.MP_BUFFERDURATION = WaxPlayService._config.getMpBufferDuration();
			waxHandler.sendEmptyMessage(MSG_DESTROY_ADMOB_ON_PLAY);
			WaxPlayer.put_stopFromUser(client_id, mediaProto, 0);
			send_showHostname("", 10); /*Snoopy 8/18/2023 added*/
			waxHandler.sendEmptyMessageDelayed(MSG_SHOW_TYPEICON, 10); /*Snoopy 8/18/2023 added*/
			if (mediaFormat == FMT_VIDEO && this.title != null && this.title.length() > 0)
				send_titleUpdated(this.title);
			if ((this.mediaProto == PROTOCOL_DLNA || this.mediaProto == PROTOCOL_DIAL) && WaxPlayService.pd4p != 0) {
				if (WaxPlayService.ENABLE_PromptDialogForPlay == false)
					return -1;
				int timeout3 = 15000;
				if (title1 == null || title1.length() == 0)
					this.title = url1.substring(0, (url1.length() < 60) ? url1.length() : 60);
				if (this.title != null && this.title.length() > 60)
					this.title = this.title.substring(0, 60) + "...";
				Spanned title_Spanned = com.waxrain.droidsender.delegate.Global.HtmlUnicode_To_String(this.title);
				if (title_Spanned != null)
					this.title = title_Spanned.toString();
				promptDialog = null;
				send_promptDlgPlayRequest(this.mediaProto, title1, timeout3/1000);
				while (timeout3 > 0) {
					if (promptDialog != null && promptDialog.result != -1)
						break;
					Global.do_sleep(50);
					timeout3 -= 50;
				}
				if (promptDialog == null || promptDialog.result != 1) {
					if (isExiting() == false && inBackground == 0/*onPause() called*/) {
						EnterExit();
						send_playFailure("");
					}
					return -1; // ret=0 is used for other states
				}
				WaxPlayService.pd4p = 0;
			}
			//ClearExit(); // Reset in Prepare()
			//mediaFormat = FMT_VIDEO; // mediaFormat is preset in Prepare()
			//waxHandler.sendEmptyMessage(MSG_SHOW_LOADING); // Sent in Prepare()
			if (whichPlayer == PLAYER_NONE) { // Avoid of resetting for onResume() Playing
				whichPlayer = Config.PLAYER_SWITCH;
				vitamioHwdecEnable = Config.VITAMIO_HWDEC_ENABLE;
			}
			if (mediaFormat == FMT_UNKNOWN)
				mediaFormat = WaxPlayService.MediaFormat(url);
			if((this.url.startsWith("http://"+WaxPlayService._ipAddress+":") || this.url.startsWith("http://127.0.0.1:") ) && 
				/*this.url.toLowerCase().endsWith(".m3u8") &&*/ (servPort >= 55570 && servPort <= 55580) ) {
				if (this.url.toLowerCase().endsWith(".m3u8") == false)
					this.url += ".m3u8";
				is_Proxy_HLS = true;
			}
			if (is_Proxy_HLS == true) {
				//if (Config.PLAYER_HLS_DISCARD == false) {
					whichPlayer = PLAYER_VITAMIO;
					vitamioHwdecEnable = (android.os.Build.VERSION.SDK_INT >= 16 && WaxPlayService.amr >= 5) ? true : Config.VITAMIO_HWDEC_ENABLE;
				//}
				EXTRA_WAIT_PLAY = 90000;
			}
			if (whichPlayer == PLAYER_EXTERN) {
				if (mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO) {
					waxHandler.sendEmptyMessage(MSG_SWITCH_EXTPLAYER);
					Global.do_sleep(100); // Schedule message handler to startActivity()
					return 1;
				}
				whichPlayer = PLAYER_INNER;
			}
			if (mediaFormat == FMT_DOC) {
				int _dot = url.lastIndexOf(".");
				if (_dot < 0)
					return 1;
				String filePath = "";
				long retDownload = 0;
				try {
					/*if (title1 != null && title1.length() > 0) {
						if (url.lastIndexOf(".") > 0)
							title1 = title1.substring(0, title1.lastIndexOf('.'));
						filePath = WaxPlayService.sdcardPath + "/" + title1 + url.substring(_dot).toLowerCase();
					} else {*/
						filePath = WaxPlayService.sdcardPath + "/" + System.currentTimeMillis() + url.substring(_dot).toLowerCase();
					//}
					retDownload = Global.doHttpRequest(null, "GET", "", url, "", null, null, "", 0, 0, 1, filePath);
					Global.do_chmod(filePath);
				} catch (Exception e) {
				}
				if (retDownload <= 0 || (new File(filePath)).exists() == false)
					filePath = "";
				send_openDoc(filePath);
				return 1;
			}
			if (mediaFormat == FMT_PHOTO && whichPlayer == PLAYER_VITAMIO)
				whichPlayer = PLAYER_INNER;
			try {
				if (mediaProto == PROTOCOL_DLNA && mediaFormat == FMT_AUDIO)
					GalleryView.mPlayIndex ++;
			} catch (Exception ex) {
			}
			requestFinished ++; // MUST before MSG_START_PLAY
			send_updateIconPlay(1, 0, -1);
			if (whichPlayer == PLAYER_VITAMIO && sDVideoViewEnabled == false)
				whichPlayer = PLAYER_INNER;
			if (mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || mediaFormat == FMT_MIRROR4) {
				CURSOR_DIRECT_MODE = Config.CURSOR_MODE;
				if (sDNativeViewEnabled == true)
					whichPlayer = PLAYER_VITAMIO;
				else
					mediaFormat = FMT_VIDEO;
			}
			if (mediaProto == PROTOCOL_AIRPLAY && mediaFormat == FMT_VIDEO && 
				url2 != null && url2.startsWith("https://*:7001/tls-psk-video/")/*AppleTV4*/ ) {
				whichPlayer = PLAYER_VITAMIO; // before IjkMediaPlayer.native_startHttpProxy()
				tls_key = url2.substring("https://*:7001/tls-psk-video/".length()); // ascii hex
				url2 = url;
			}
			int i = 0;
			waxHandler.sendEmptyMessage(MSG_START_PLAY);
			if (whichPlayer == PLAYER_VITAMIO)
				i = 1; // Only loop once
			if (userAgent != null && userAgent.length() > 0 && 
				mediaProto == PROTOCOL_AIRPLAY && mediaFormat == FMT_VIDEO && 
				whichPlayer == PLAYER_INNER && sDVideoViewEnabled == true && ( 
				this.url.toLowerCase().endsWith(".mp4") || this.url.toLowerCase().indexOf(".mp4?") > 0 || 
				this.url.toLowerCase().endsWith(".mov")|| this.url.toLowerCase().indexOf(".mov?") > 0 ) ) {
				try {
					url_hd = tv.danmaku.ijk.media.player.IjkMediaPlayer.native_startHttpProxy(url, userAgent, WaxPlayService._ipAddress, WaxPlayService.ssp, WaxPlayService.ijkPlayer_debug);
					if (url_hd != null && url_hd.length() > 0)
						Log.i(LOG_TAG,"MPlayer["+fragment_id+"]...added proxy");
				} catch (Exception ex) {
				} catch (Throwable th) {
				}
			}
			int timeout = WaxPlayService.WAIT_PLAY_TIMEOUT/2;
			while (inBackground != 1 && whichPlayer != PLAYER_NONE && playback_State == STATE_STOPPED && timeout > 0) {
				Global.do_sleep(50); // Schedule message handler to switch STATE_PREPARING
				timeout -= 50;
			}
			int ret = 0;
			// Snoopy 12/21/2022 : added for Youtube issue
			if (is_Proxy_HLS == false) {
				ret = PlayInner(i, EXTRA_WAIT_PLAY, false);
				if (is_Proxy_HLS == false && ret != 1 && playback_timeout < (WaxPlayService.WAIT_PLAY_TIMEOUT/3)) // AirPlay 'unhandledURLRequest': 403, 404
					ret = -403;
			} else {
				final int i2 = i;
				final int EXTRA_WAIT_PLAY2 = EXTRA_WAIT_PLAY;
				new Thread() {
					public void run() {
						PlayInner(i2, EXTRA_WAIT_PLAY2, true);
					}
				}.start();
				while (timeout > 0 && playback_State != STATE_STOPPED && playback_State != STATE_PLAYING) {
					Global.do_sleep(50);
					timeout -= 50;
				}
				ret = (playback_State != STATE_STOPPED) ? 1 : 0;
			}
			// End 12/21/2022
			Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Play Return "+ret+", counter = "+WaxPlayService.player_Counter);
			return ret;
		}
	}

    private int ResumeFromSeekCompletion(boolean inCallback) {
		duringSeeking = false;
		if (WaxPlayService.mediaplayer == null || WaxPlayService.mediaplayer.activityRunning == false || playback_State == STATE_STOPPED || isExiting() == true)
			return 0;
		if (inCallback == true)
			Global.do_sleep(50); // Schedule Seek()
		return 1;
    }

	public int Seek(final int wanttime, String newuri) {
		if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING || /*|| GetDuration() >= MaxDurationAV*/
			mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || mediaFormat == FMT_MIRROR4 || mediaFormat == FMT_PHOTO) {
			return 0;
		}
		Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Seek");
		if (duringSeeking == true || Math.abs(wanttime - this.seekTime) < 15000)
			return 0;
		requestFinished ++;
		send_seekRequest(wanttime, 0);
		new Thread() {
			public void run() {
				int timeout = WaxPlayService.WAIT_PLAY_TIMEOUT/2;
				while (whichPlayer != PLAYER_NONE && timeout > 0 && playback_State != STATE_STOPPED && duringSeeking == true) {
					Global.do_sleep(50);
					timeout -= 50;
				}
				Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Seek Return");
				requestFinished --;
				waxHandler.sendEmptyMessage(MSG_HIDE_LOADING2); // multiple canceling
				ResumeFromSeekCompletion(false);
			}
		}.start();
		return 1;
	}

	private void StopInner(boolean do_stop) {
		Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Stop "+do_stop);
		if (mediaProto == PROTOCOL_DIAL && mediaFormat == FMT_VIDEO/*ChromeView*/) {
			new Handler(WaxPlayer2.this.getContext().getMainLooper()).post(new Runnable() {
				public void run() {
					DestroyChromeView();
				}
			});
		} else if (mediaFormat == FMT_MIRROR4) {
			try {
				Intent disconnectIntent = new Intent();
				disconnectIntent.setAction(WaxPlayService.HWS_BROADCAST_ACTION_DISCONNECT);
				WaxPlayer2.this.getContext().sendBroadcast(disconnectIntent);
				int hws_wait_to = 1500;
				if (stopBackground == true) {
					while (hws_wait_to > 0 && WaxPlayService.HWS_isSurfaceReady == true) {
						Global.do_sleep(10);
						hws_wait_to -= 10;
					}
				} else {
					Global.do_sleep(hws_wait_to);
				}
				if (HWS_BroadcastReceiver != null)
					WaxPlayer2.this.getContext().unregisterReceiver(HWS_BroadcastReceiver);
			} catch (Exception e) {
			}
		} else {
			try {
				if (do_stop == true) {
					if (whichPlayer == PLAYER_INNER && hDVideoView != null)
						hDVideoView.stopPlayback(inBackground);
					else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
						sDVideoView.stopPlayback(inBackground);
				} else {
					if (whichPlayer == PLAYER_INNER && hDVideoView != null)
						hDVideoView.release(true);
					else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
						sDVideoView.release(true);
				}
			} catch (Exception e) {
			}
		}
		// Snoopy 5/23/2023 : Stop background AUDIO for iOS
		if (WaxPlayer.waxPlayService != null && inBackground != 0) /*Snoopy 8/18/2023 added for audio*/
			WaxPlayer.waxPlayService.PauseClientByProto(client_id, WaxPlayer2.PROTOCOL_AIRPLAY|WaxPlayer2.PROTOCOL_DLNA);
		else if (WaxPlayer.waxPlayService != null)
			WaxPlayer.waxPlayService.StopClientByProto(client_id, WaxPlayer2.PROTOCOL_AIRPLAY|WaxPlayer2.PROTOCOL_DLNA, true);
		// End 5/23/2023
		if (WaxPlayService.BTHID_SERV > 0 && mBtHidBondedDevMac != null)
			Global.stopBtHidDevConnect(WaxPlayer2.this.getContext(), mBtHidBondedDevMac);
		mBtHidBondedDevMac = null;
		stopFinished = true;
		Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Stop Return1");
	}

	public int Stop(boolean destroy, int forceShow) {
		synchronized (stopLock) {
			Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Stopping, counter = "+WaxPlayService.player_Counter);
			// Stop() could be multiple entered
			//if (WaxPlayService.AudioPlayerMap.size() > 0) // Fake Resume AirTunes
			//	WaxPlayService.AO_streamAudioFocused = true;
			WaxPlayService.STOP_TRACKER_Volume_AudioFocus();
			waxHandler.sendEmptyMessage(MSG_HIDE_LOADING);
			waxHandler.sendEmptyMessage(MSG_HIDE_LOADING2);
			waxHandler.sendEmptyMessage(MSG_REMOVE_CURSOR);
			if (zoomState == LAYOUT_ZOOM)
				Scale(0, Config.ZOOM);/*Snoopy 11/19/2024 : OTHERWISE surfaceView will not be released*/
			//if (destroy == false || forceShow == 1) {
				send_updateIconStop(forceShow, -1); // Show icon and Destroy GalleryView
				if (forceShow == 1)
					Global.do_sleep(100); // Called from onPause() and WaxPlayService
			//}
			synchronized (ctrlLock) {
				if (playback_State != STATE_STOPPED) {
					// Check play continuation after play really started
					if ((mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO) && playContinue == true && 
						this.duration > RecDurationAV && this.duration < MaxDurationAV/*Live*/ ) {
						if (this.lastPos > RecDurationAV && this.lastPos < this.duration - RecDurationAV)
							PlayList.pls(this.url, this.lastPos, this.duration);
						else if (this.lastPos > 0)
							PlayList.pls(this.url, -1, this.duration); // delete
					}
					stopFinished = false;
					stopPlayback = true;
					if (playback_State == STATE_PREPARING)
						stopPlayback = false; // Preparing or Switching Player
					playback_State = STATE_STOPPED;
					if (stopBackground == true) {
						new Thread() {
							public void run() {
								StopInner(stopPlayback);
							}
						}.start();
					} else {
						StopInner(stopPlayback);
					}
					int timeout = (whichPlayer == PLAYER_VITAMIO) ? WaxPlayService.WAIT_PLAY_TIMEOUT*2 : WaxPlayService.WAIT_PLAY_TIMEOUT;
					while (stopFinished == false && timeout > 0) {
						Global.do_sleep(50); // Schedule
						timeout -= 50;
					}
					if (stopFinished == false && destroy == true)
						waxHandler.sendEmptyMessage(MSG_GOTO_RESTART);
					destroyTickTimer();
					destroyCtrlbarTimer();
					destroyMiceDragTimer();
					destroyCursorTimer();
					if (Srt_Parse != null)
						Srt_Parse.destroySRT();
					if (Lyric_Parse != null)
						Lyric_Parse.destroySRT();
					Srt_Parse = null;
					Lyric_Parse = Lyric_Parse2 = null;
					lastSrtID = -1;
					whichPlayer = PLAYER_NONE; // Exit Play()/Seek()
					timeout = 5000;
					while (requestFinished > 0 && timeout > 0) {
						Global.do_sleep(50); // Schedule
						timeout -= 50;
					}
				}
			}
			closeRebufferProgress(true/*Must be TRUE*/); // Do it again if we started a new pending event during Stop()
			if (destroy == true)
				gotoFinish(0);
			WaxPlayService.send_MediaStop(client_id, mediaFormat, mediaProto, lastPos, WaxPlayer.get_stopFromUser(client_id, mediaProto), (WaxPlayService.mediaplayer!=null&&WaxPlayService.mediaplayer.MPlayerCount>0)?(WaxPlayService.mediaplayer.MPlayerCount-1):0);
			Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Stop Return");
			return 1;
		}
	}

	public int gotoFinish(int forceShow) {
		Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......gotoFinish");
		if (forceShow == 1)	{
			send_updateIconStop(1, -1);
			Global.do_sleep(100);
		}
		waxHandler.removeMessages(MSG_GOTO_FINISH); // remove pending msg
		waxHandler.sendEmptyMessage(MSG_GOTO_FINISH);
		return 1;
	}

	public synchronized int PauseInner() {
		try {
			if (whichPlayer == PLAYER_INNER && hDVideoView != null) {
				hDVideoView.pause();
				return 1;
			} else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null) {
				sDVideoView.pause();
				return 1;
			}
		} catch (Exception e) {
		}
		return 0;
	}

	public int Pause(boolean forceShow, int reqLevel, int delayshow) {
		synchronized (ctrlLock) {
			if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING || 
				mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || mediaFormat == FMT_MIRROR4 || mediaFormat == FMT_PHOTO)
				return 0;
			if (playback_State == STATE_PAUSED)
				return 1;
			if (reqLevel < pauseReqLevel)
				return 0;
			pauseReqLevel = reqLevel;
			int execdone = PauseInner();
			Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Pause = "+execdone);
			if (execdone == 1) {
				playback_State = STATE_PAUSED;
				//if (WaxPlayService.releaseAudioFocusOnPauseState == true && (mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO))
				//	WaxPlayService.releaseAudioFocus();
				if (forceShow == true)
					send_updateIconPlay(0, 1, delayshow);
				WaxPlayService.send_MediaPaused(client_id);
			}
			return execdone;
		}
	}

	public synchronized int ResumeInner() {
		try {
			if (whichPlayer == PLAYER_INNER && hDVideoView != null) {
				hDVideoView.resume();
				hDVideoView.start();
				return 1;
			} else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null) {
				sDVideoView.resume();
				sDVideoView.start();
				return 1;
			}
		} catch (Exception e) {
		}
		return 0;
	}

	public int Resume(int reqLevel) {
		synchronized (ctrlLock) {
			if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING || 
				mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || mediaFormat == FMT_MIRROR4 || mediaFormat == FMT_PHOTO)
				return 0;
			if (playback_State != STATE_PAUSED)
				return 1;
			Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......Resume");
			if (reqLevel < pauseReqLevel)
				return 0;
			pauseReqLevel = -1;
			int execdone = ResumeInner();
			if (execdone == 1) {
				//if (mediaFormat == FMT_VIDEO || mediaFormat == FMT_AUDIO)
					WaxPlayService.requestAudioFocus();
				playback_State = STATE_PLAYING;
				send_updateIconPlay(1, 0, -1);
				WaxPlayService.send_MediaResumed(client_id);
			}
			return execdone;
		}
	}

	public int SetMuted(boolean isMute) {
		if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING)
			return 0;
		if (isMuted2 == isMute)
			return 1;
		isMuted2 = isMute; // Save the state for WaxPlayer.PlayState2String()
		send_updateIconPlay(1, 0, -1);
		if (mediaFormat == FMT_PHOTO || mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || mediaFormat == FMT_MIRROR4)
			return 0;
		float vol = (isMuted2 == true) ? 0.0f : 1.0f;
		try {
			if (whichPlayer == PLAYER_INNER && hDVideoView != null)
				hDVideoView.setVolume(vol, vol);
			else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
				sDVideoView.setVolume(vol, vol);
			WaxPlayService.send_MediaMuted(client_id, isMute);
			return 1;
		} catch (Exception e) {
		}
		return 0;
	}

	public int GetVideoWidth() {
		if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING)
			return videoWidth;
		try {
			if (whichPlayer == PLAYER_INNER && hDVideoView != null)
				return hDVideoView.getVideoWidth();
			else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
				return sDVideoView.getVideoWidth();
		} catch (Exception e) {
		}
		return 0;
	}

	public int GetVideoHeight() {
		if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING)
			return videoHeight;
		try {
			if (whichPlayer == PLAYER_INNER && hDVideoView != null)
				return hDVideoView.getVideoHeight();
			else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
				return sDVideoView.getVideoHeight();
		} catch (Exception e) {
		}
		return 0;
	}

	public int GetDuration() {
		if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING)
			return this.duration;
		if (this.duration <= 0) {
			try {
				if (whichPlayer == PLAYER_INNER && hDVideoView != null)
					this.duration = (int) hDVideoView.getDuration();
				else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
					this.duration = (int) sDVideoView.getDuration();
			} catch (Exception e) {
			}
		}
		if (mediaFormat == FMT_PHOTO && this.duration > MaxDurationPHOTO)
			this.duration = MinDuration; // For UpdatePG()
		return this.duration;
	}

	public int GetCurrentTime() {
		if (playback_State == STATE_STOPPED)
			return lastPos;
		if (playback_State == STATE_PREPARING/*Switching Player*/ || !is_Playing2())
			return lastPos;
		try {
			if (whichPlayer == PLAYER_INNER && hDVideoView != null)
				lastPos = (int) hDVideoView.getCurrentPosition();
			else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
				lastPos = (int) sDVideoView.getCurrentPosition();
		} catch (Exception e) {
		}
		return lastPos;
	}

	public boolean is_Caching() {
		if (whichPlayer != PLAYER_NONE)
			return (playback_State == STATE_PREPARING);
		return false;
	}

	public boolean is_Playing() {
		if (whichPlayer != PLAYER_NONE)
			return (playback_State == STATE_PLAYING);
		return false;
	}

	public boolean is_Playing2() {
		if (inBackground == 1)
			return true;
		if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING)
			return false;
		if (mediaProto == PROTOCOL_DIAL && mediaFormat == FMT_VIDEO/*ChromeView*/)
			return true;
		if (mediaFormat == FMT_PHOTO)
			return true;
		try {
			if (whichPlayer == PLAYER_INNER && hDVideoView != null)
				return hDVideoView.isPlaying();
			else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
				return sDVideoView.isPlaying();
		} catch (Exception e) {
		}
		return false;
	}

	public boolean is_Stopped() {
		if (inBackground == 1 || isResuming == true)
			return false;
		return (playback_State == STATE_STOPPED);
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

	public void UpdateAudioMetadata(boolean force, boolean set_str, String cover, String album, String title, String artist, String lyric_url) {
		if (force == false && mediaFormat != FMT_AUDIO)
			return ;
		if (set_str == true) {
			this.music_cover = cover;
			this.music_album = Global.NcrWebUnicode2String(album);
			this.music_title = Global.NcrWebUnicode2String(title);
			this.music_artist = Global.NcrWebUnicode2String(artist);
			this.lyric_url = lyric_url;
		}
		waxHandler.sendEmptyMessage(MSG_UPDATE_AUDIO_METADATA);
	}

	public int GetVolume() {
		int curvol = 50;
		try {
			if (WaxPlayService.useSystemVolume == true) {
				if (WaxPlayer.waxPlayService != null)
					WaxPlayer.waxPlayService.Android_GetVolume();
				curvol = WaxPlayService.realVolume*100/WaxPlayService.totalVolume;
			} else {
				if (playback_State == STATE_STOPPED)
					return curvol;
			}
		} catch (Exception e) {
		}
		return curvol;
	}

	public int SetVolume(int volume, boolean ui) {
		try {
			if (WaxPlayService.useSystemVolume == true) {
				if (WaxPlayer.waxPlayService != null)
					WaxPlayer.waxPlayService.Android_SetVolume(volume, ui);
			} else {
				if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING)
					return 0;
				if (volume < 0)
					volume = 0;
				if (volume > 100)
					volume = 100;
				if (whichPlayer == PLAYER_INNER && hDVideoView != null)
					hDVideoView.setVolume((float)volume, (float)volume);
				else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
					sDVideoView.setVolume((float)volume, (float)volume);
			}
		} catch (Exception e) {
		}
		return 1;
	}

	public void SwitchPlayer(int newPlayer, boolean hwdecEnable) {
		if (whichPlayer == newPlayer && vitamioHwdecEnable == hwdecEnable)
			return;
		if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING || 
			mediaFormat == FMT_PHOTO || mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || mediaFormat == FMT_MIRROR4) {
			return;
		}
		whichPlayer = whichPlayerSaved = newPlayer;
		vitamioHwdecEnable = hwdecEnable;
		Log.i(LOG_TAG,"MPlayer["+fragment_id+"].......SWITCH to "+whichPlayer);
		if (duringSeeking == true) {
			duringSeeking = false;
			int timeout = 5000;
			while (requestFinished > 0 && timeout > 0) {
				Global.do_sleep(50); // During Seek()
				timeout -= 50;
			}
		}
		waxHandler.sendEmptyMessage(MSG_SWITCH_PLAYER);
	}

	public void SetPlaySpeed(int speed) {
		if (speed < 0 || speed >= playSpeedVal.length)
			return ;
		try {
			float speedf = playSpeedVal[speed];
			if (whichPlayer == PLAYER_INNER && hDVideoView != null)
				hDVideoView.setPlaybackSpeed(speedf);
			else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
				sDVideoView.setPlaybackSpeed(speedf);
			playSpeedIndex = speed;
			waxHandler.sendEmptyMessage(MSG_UPDATE_PLAYSPEED);
		} catch (Exception e) {
		}
	}

	// 0:Prepareing/1:Playing/2:Force
	public void Scale(int state, int zoomflag) { // Double entered in Play()
		if (zoomState == zoomflag && state == 0)
			return;
		if (playback_State == STATE_STOPPED || playback_State == STATE_PREPARING || mediaFormat == FMT_AUDIO)
			return;
		if (state == 1 && (mediaFormat == FMT_PHOTO || mediaFormat == FMT_MIRROR || mediaFormat == FMT_MIRROR2 || mediaFormat == FMT_MIRROR3 || mediaFormat == FMT_MIRROR4)) {
			if (WaxPlayService.PcMirror_FullScreen == true && mediaFormat == FMT_MIRROR) {
				zoomflag = LAYOUT_STRETCH;
			} else {
				//zoomState = WaxPlayer.zoomState = LAYOUT_SCALE;
				//return; // auto scale already executed for PHOTO in play() for HDImageView
			}
		}
		if (zoomflag != LAYOUT_ZOOM)
			WaxPlayService._config.setZoom(zoomflag);
		zoomState = WaxPlayer.zoomState = zoomflag;
		try {
			if (zoomState == LAYOUT_STRETCH) {
				if (whichPlayer == PLAYER_INNER && hDVideoView != null && state != 1)
					hDVideoView.setVideoLayout(HDVideoView.VIDEO_LAYOUT_STRETCH,0);/*default is ZOOM in HDVideoView but don't set ZOOM for MTK5502 onPrepared()*/
				else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
					sDVideoView.setVideoLayout(SDVideoView.VIDEO_LAYOUT_STRETCH,0);
			} else if (zoomState == LAYOUT_SCALE) {
				if (whichPlayer == PLAYER_INNER && hDVideoView != null)
					hDVideoView.setVideoLayout(HDVideoView.VIDEO_LAYOUT_SCALE,0);
				else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
					sDVideoView.setVideoLayout(SDVideoView.VIDEO_LAYOUT_SCALE,0);
			} else if (zoomState == LAYOUT_ZOOM) {
				if (whichPlayer == PLAYER_INNER && hDVideoView != null)
					hDVideoView.setVideoLayout(HDVideoView.VIDEO_LAYOUT_ZOOM,0);
				else if (whichPlayer == PLAYER_VITAMIO && sDVideoView != null)
					sDVideoView.setVideoLayout(SDVideoView.VIDEO_LAYOUT_ZOOM,0);
			}
		} catch (Exception e) {
		}
	}

	public void gotoStop(boolean destroy, int forceShow, boolean reportStop) {
		Log.i(LOG_TAG,"MPlayer["+fragment_id+"] gotoStop("+destroy+", "+forceShow+", "+reportStop+")");
		Stop(false, forceShow);
		if (reportStop == true && WaxPlayer.waxPlayService != null)
			WaxPlayer.waxPlayService.MediaStop(client_id, WaxPlayer.get_stopFromUser(client_id, mediaProto));
		if (destroy == true)
			gotoFinish(forceShow);
	}

	public void MoveDialog2(Dialog dlg, int layout_id, boolean center) {
		// NAVIGATION bar will be reshown if activity lose focus(view is removed, or dialog is created)
		if (WaxPlayService.mediaplayer != null)
			WaxPlayService.mediaplayer.enter_FULLSCREEN(dlg.getWindow());
		Window window = dlg.getWindow();
		WindowManager.LayoutParams lp = window.getAttributes();
        int dlgW = 0, dlgH = 0;
		try {
			View dlgView = window.getDecorView();
			if (layout_id != 0) {
				dlgView = dlg.findViewById(layout_id);
				dlgW = dlgView.getWidth();
				dlgH = dlgView.getHeight();
			} else {
				dlgW = lp.width;
				dlgH = lp.height;
			}
			if (dlgW <= 0 || dlgH <= 0) {
				dlgView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
				dlgW = dlgView.getMeasuredWidth();
				dlgH = dlgView.getMeasuredHeight();
			}
		} catch (Exception e) {
		}
		window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		lp.alpha = 0.8f;
		if (dlgW > 0 && dlgH > 0) {
			int abs_x = (screenXoff >= Global.FREEFORM_X) ? (screenXoff - Global.FREEFORM_X) : screenXoff;
			int abs_y = (screenYoff >= Global.FREEFORM_Y) ? (screenYoff - Global.FREEFORM_Y) : screenYoff;
			if (center == true) {
				lp.x = abs_x + (screenWidth - dlgW) / 2;
				lp.y = abs_y + (screenHeight - dlgH) / 2;
			} else { // Keep its alignment
				if (lp.x < abs_x || lp.y < abs_y) {
					lp.x += abs_x;
					lp.y += abs_y;
				}
			}
			lp.gravity = Gravity.LEFT | Gravity.TOP;
			//Log.i(LOG_TAG,"MPlayer["+fragment_id+"] LAYOUT Dialog = "+lp.x+","+lp.y+"|"+dlgW+"x"+dlgH);
		}
		window.setAttributes(lp);
	}

	public void gotoBackgroundExec() {
		if (mGalleryView != null)
			DestroyGalleryView();
		inBackground = 2; // Never go foreground until Video comes
		if (dialogBackground != null) {
			dialogBackground.cancel();
			dialogBackground.dismiss();
			dialogBackground = null;
		}
		Intent i = new Intent(Intent.ACTION_MAIN);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.addCategory(Intent.CATEGORY_HOME);
		startActivity(i);
	}

	public void gotoBackground2() {
		if (enableBackground == false)
			return ;
		if (WaxPlayService.ActivityFloatWindowMode == true)
			return ;
		dialogBackground = new CustomDialog$Builder(WaxPlayer2.this.getContext(), Global.RES_style_WaxDialog, Global.RES_style_About_dialog, 0.8f, Global.RES_layout_dialog_alert,
				Global.RES_id_adg_root_view, Global.RES_id_adg_title_text, Global.RES_id_adg_message, Global.RES_id_adg_messageP, Global.RES_id_adg_confirm_btn, Global.RES_id_adg_cancel_btn, Global.RES_id_adg_left_padding, R.id.adg_right_padding )
			.setTitle(R.string.waxplayer_alert_gotosetting_title)
			.setMessage(R.string.waxplayer_alert_gotobackground_msg2)
			.setPositiveButton(R.string.waxplayer_alert_gotobackground_type2, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					WaxPlayService._config.setAudioBgPlay(true);
					gotoBackgroundExec();
					return;
				}
			} )
			.setNegativeButton(R.string.waxplayer_alert_gotobackground_type1, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					gotoBackgroundExec();
					return;
				}
			} )
			.setOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					try {
						if (WaxPlayer2.this != null && dialogBackground != null)
							WaxPlayer2.this.MoveDialog2(dialogBackground, Global.RES_id_adg_bgview, true);
					} catch (Exception ex) {
					}
					//dialogBackground.getWindow().getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
			} )
			.CreateAndShow(false);
		dialogBackground.show();
		MoveDialog2(dialogBackground, Global.RES_id_adg_bgview, true);
	}

	public void gotoBackground() {
		if (enableBackground == false)
			return ;
		if (WaxPlayService.ActivityFloatWindowMode == true)
			return ;
		dialogBackground = new CustomDialog$Builder(WaxPlayer2.this.getContext(), Global.RES_style_WaxDialog, Global.RES_style_About_dialog, 0.8f, Global.RES_layout_dialog_alert,
				Global.RES_id_adg_root_view, Global.RES_id_adg_title_text, Global.RES_id_adg_message, Global.RES_id_adg_messageP, Global.RES_id_adg_confirm_btn, Global.RES_id_adg_cancel_btn, Global.RES_id_adg_left_padding, R.id.adg_right_padding )
			.setTitle(R.string.waxplayer_alert_gotosetting_title)
			.setMessage(R.string.waxplayer_alert_gotobackground_msg)
			.setPositiveButton(R.string.alertdlg_confirm, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					if (dialogBackground != null) {
						dialogBackground.cancel();
						dialogBackground.dismiss();
						dialogBackground = null;
					}
					gotoBackground2();
					return;
				}
			} )
			.setNegativeButton(R.string.alertdlg_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					if (dialogBackground != null) {
						dialogBackground.cancel();
						dialogBackground.dismiss();
						dialogBackground = null;
					}
					return;
				}
			} )
			.setOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					try {
						if (WaxPlayer2.this != null && dialogBackground != null)
							WaxPlayer2.this.MoveDialog2(dialogBackground, Global.RES_id_adg_bgview, true);
					} catch (Exception ex) {
					}
					//dialogBackground.getWindow().getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
			} )
			.CreateAndShow(false);
		dialogBackground.show();
		MoveDialog2(dialogBackground, Global.RES_id_adg_bgview, true);
	}

	public void gotoSetting() {
		dialogSetting = new CustomDialog$Builder(WaxPlayer2.this.getContext(), Global.RES_style_WaxDialog, Global.RES_style_About_dialog, 0.8f, Global.RES_layout_dialog_alert,
				Global.RES_id_adg_root_view, Global.RES_id_adg_title_text, Global.RES_id_adg_message, Global.RES_id_adg_messageP, Global.RES_id_adg_confirm_btn, Global.RES_id_adg_cancel_btn, Global.RES_id_adg_left_padding, R.id.adg_right_padding )
			.setTitle(R.string.waxplayer_alert_gotosetting_title)
			.setMessage(R.string.waxplayer_alert_gotosetting_msg)
			.setPositiveButton(R.string.alertdlg_confirm, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					if (isExiting() == false) {
						EnterExit();
						gotoStop(false, 1, true);
						gotoFinish(0);
						WaxPlayService.startSettingActivity(WaxPlayer2.this.getActivity());
					}
					if (dialogSetting != null) {
						dialogSetting.cancel();
						dialogSetting.dismiss();
						dialogSetting = null;
					}
					return;
				}
			} )
			.setNegativeButton(R.string.alertdlg_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					if (dialogSetting != null) {
						dialogSetting.cancel();
						dialogSetting.dismiss();
						dialogSetting = null;
					}
					return;
				}
			} )
			.setOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					try {
						if (WaxPlayer2.this != null && dialogSetting != null)
							WaxPlayer2.this.MoveDialog2(dialogSetting, Global.RES_id_adg_bgview, true);
					} catch (Exception ex) {
					}
					//dialogSetting.getWindow().getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
			} )
			.CreateAndShow(false);
		dialogSetting.show();
		MoveDialog2(dialogSetting, Global.RES_id_adg_bgview, true);
	}

	public void gotoContinuePlay() {
		dialogContinue = new CustomDialog$Builder(WaxPlayer2.this.getContext(), Global.RES_style_WaxDialog, Global.RES_style_About_dialog, 0.8f, Global.RES_layout_dialog_alert,
				Global.RES_id_adg_root_view, Global.RES_id_adg_title_text, Global.RES_id_adg_message, Global.RES_id_adg_messageP, Global.RES_id_adg_confirm_btn, Global.RES_id_adg_cancel_btn, Global.RES_id_adg_left_padding, R.id.adg_right_padding )
			.setTitle(R.string.waxplayer_alert_gotosetting_title)
			.setMessage(R.string.waxplayer_toast_continuation_check)
			.setPositiveButton(R.string.alertdlg_confirm, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					if (isExiting() == false)
						send_seekRequest(seekTime2, 1);
					if (dialogContinue != null) {
						dialogContinue.cancel();
						dialogContinue.dismiss();
						dialogContinue = null;
					}
					return;
				}
			} )
			.setNegativeButton(R.string.alertdlg_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					if (dialogContinue != null) {
						dialogContinue.cancel();
						dialogContinue.dismiss();
						dialogContinue = null;
					}
					return;
				}
			} )
			.setOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					try {
						if (WaxPlayer2.this != null && dialogContinue != null)
							WaxPlayer2.this.MoveDialog2(dialogContinue, Global.RES_id_adg_bgview, true);
					} catch (Exception ex) {
					}
					//dialogContinue.getWindow().getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
				}
			} )
			.CreateAndShow(false);
		dialogContinue.show();
		MoveDialog2(dialogContinue, Global.RES_id_adg_bgview, true);
	}
}
