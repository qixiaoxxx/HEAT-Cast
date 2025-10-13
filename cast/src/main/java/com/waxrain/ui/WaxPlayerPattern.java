package com.waxrain.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.waxrain.airplaydmr.*;
import com.waxrain.airplaydmr_SDK.R;
import com.waxrain.droidsender.delegate.*;
import com.waxrain.utils.Config;

public class WaxPlayerPattern extends Dialog implements OnClickListener {
	private static final String LOG_TAG = com.waxrain.airplaydmr.WaxPlayService.LOG_TAG;
	RadioButton radioHardPlay = null, radioSoftPlay_swdec = null, radioSoftPlay_hwdec = null, radioExtPlay = null;
	RadioButton radioHardDec = null, radioSoftDec = null, radioSoftDec2 = null;
	private boolean softDecEnable = true;
	private Context ctxPattern = null;
	public WaxPlayer2 mediaplayer = null;
	private boolean paramChanged = false;
	private int lastPlayerId = WaxPlayer2.PLAYER_NONE;
	private boolean lastVitamioHwDec = Config.VITAMIO_HWDEC_ENABLE;
	private int lastHwDec = Config.MIRROR_HWDEC;
	private int lastSharpen = Config.MIRROR_SHARPEN;
	private int sourceSelect = 1; // 1:WaxPlayer/2:WaxPlayerSetting
	private int modeSelect = 1; // 1:Player/2:Decoder
	Window window = null;

	public WaxPlayerPattern(Context context, int layout, int style, int playerid, boolean vitamioHwDec, boolean softenable, int source, int screenWidth, int screenHeight, int mode) {
		this(context, layout, style, playerid, vitamioHwDec, softenable, source, screenWidth, screenHeight, mode, null);
	}

	public WaxPlayerPattern(Context context, int layout, int style, int playerid, boolean vitamioHwDec, boolean softenable, int source, int screenWidth, int screenHeight, int mode, WaxPlayer2 mp) {
		super(context, style);
		if (WaxPlayService.mediaplayer != null && sourceSelect == 1)
			WaxPlayService.mediaplayer.enter_FULLSCREEN(getWindow());

		window = getWindow();
		//window.requestFeature(Window.FEATURE_NO_TITLE);
		ctxPattern = context;
		mediaplayer = mp;
		setContentView(layout);
		softDecEnable = softenable;
		sourceSelect = source;
		modeSelect = mode;
		lastPlayerId = playerid;
		lastVitamioHwDec = vitamioHwDec;

		radioHardPlay = (RadioButton) this.findViewById(R.id.hardplay);
		radioSoftPlay_swdec = (RadioButton) this.findViewById(R.id.softplay_swdec);
		radioSoftPlay_hwdec = (RadioButton) this.findViewById(R.id.softplay_hwdec);
		radioExtPlay = (RadioButton) this.findViewById(R.id.extplay);
		radioHardPlay.setFocusable(true);
		radioSoftPlay_swdec.setFocusable(true);
		radioSoftPlay_hwdec.setFocusable(true);
		radioExtPlay.setFocusable(true);
		if (playerid == WaxPlayer2.PLAYER_INNER) {
			radioHardPlay.setChecked(true);
			radioSoftPlay_swdec.setChecked(false);
			radioSoftPlay_hwdec.setChecked(false);
			radioExtPlay.setChecked(false);
			radioHardPlay.requestFocus();
		} else if (playerid == WaxPlayer2.PLAYER_VITAMIO && vitamioHwDec == false) {
			radioHardPlay.setChecked(false);
			radioSoftPlay_swdec.setChecked(true);
			radioSoftPlay_hwdec.setChecked(false);
			radioExtPlay.setChecked(false);
			radioSoftPlay_swdec.requestFocus();
		} else if (playerid == WaxPlayer2.PLAYER_VITAMIO && vitamioHwDec == true) {
			radioHardPlay.setChecked(false);
			radioSoftPlay_swdec.setChecked(false);
			radioSoftPlay_hwdec.setChecked(true);
			radioExtPlay.setChecked(false);
			radioSoftPlay_hwdec.requestFocus();
		} else if (playerid == WaxPlayer2.PLAYER_EXTERN) {
			radioHardPlay.setChecked(false);
			radioSoftPlay_swdec.setChecked(false);
			radioSoftPlay_hwdec.setChecked(false);
			radioExtPlay.setChecked(true);
			radioExtPlay.requestFocus();
		}
		if (softenable == false) {
			radioSoftPlay_swdec.setEnabled(false);
			radioSoftPlay_swdec.setFocusable(false);
			radioSoftPlay_hwdec.setEnabled(false);
			radioSoftPlay_hwdec.setFocusable(false);
			if (playerid == WaxPlayer2.PLAYER_EXTERN)
				radioExtPlay.requestFocus();
			else
				radioHardPlay.requestFocus();
		}
		radioHardPlay.setOnClickListener(this);
		radioSoftPlay_swdec.setOnClickListener(this);
		radioSoftPlay_hwdec.setOnClickListener(this);
		radioExtPlay.setOnClickListener(this);
		if (android.os.Build.VERSION.SDK_INT < 16) {
			radioSoftPlay_hwdec.setEnabled(false);
			radioSoftPlay_hwdec.setFocusable(false);
			radioSoftPlay_hwdec.setVisibility(View.GONE);
		}

		WindowManager.LayoutParams params = window.getAttributes();
		params.alpha = 0.8f;
		if (sourceSelect == 1 || modeSelect == 1)
			params.width = WaxPlayer2.fontSize*18;
		else
			params.width = WaxPlayer2.fontSize*25;
		if (params.width > screenWidth - 20) 
			params.width = screenWidth - 20;
		if (sourceSelect == 1 || modeSelect == 1) {
			if (android.os.Build.VERSION.SDK_INT < 16)
				params.height = (int) WaxPlayer2.fontSize*6 + WaxPlayer2.fontSize+15/*Title*/;
			else
				params.height = (int) WaxPlayer2.fontSize*8 + WaxPlayer2.fontSize+15/*Title*/;
		} else {
			params.height = (int) WaxPlayer2.fontSize*6 + WaxPlayer2.fontSize+15/*Title*/;
		}
		params.gravity = Gravity.CENTER;
		window.setWindowAnimations(R.style.About_dialog);
		window.setAttributes(params);
		setCanceledOnTouchOutside(true);
		window.getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				try {
					if (mediaplayer != null)
						mediaplayer.MoveDialog2(WaxPlayerPattern.this, R.id.patterndlg_bg, true);
				} catch (Exception ex) {
				}
				//window.getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});
		
		initUI();
		if (sourceSelect == 1 || android.os.Build.VERSION.SDK_INT < 16) {
			setHwDecLayout(false);
			setPatternLayout(true);
		} else {
			radioHardDec = (RadioButton) this.findViewById(R.id.harddecode);
			radioSoftDec = (RadioButton) this.findViewById(R.id.softdecode);
			radioSoftDec2 = (RadioButton) this.findViewById(R.id.softdecode2);
			radioHardDec.setFocusable(true);
			radioSoftDec.setFocusable(true);
			radioSoftDec2.setFocusable(true);
			if (lastHwDec == 1) {
				radioHardDec.setChecked(true);
				radioSoftDec.setChecked(false);
				radioSoftDec2.setChecked(false);
				radioHardDec.requestFocus();
			} else if (lastHwDec == 0 && lastSharpen == 0) {
				radioHardDec.setChecked(false);
				radioSoftDec.setChecked(true);
				radioSoftDec2.setChecked(false);
				radioSoftDec.requestFocus();
			} else if (lastHwDec == 0 && lastSharpen >= 1) {
				radioHardDec.setChecked(false);
				radioSoftDec.setChecked(false);
				radioSoftDec2.setChecked(true);
				radioSoftDec2.requestFocus();
			}
			radioHardDec.setOnClickListener(this);
			radioSoftDec.setOnClickListener(this);
			radioSoftDec2.setOnClickListener(this);

			initUI2();
			setPatternLayout(true);
			setHwDecLayout(true);
		}
		if (modeSelect == 1)
			setHwDecLayout(false);
		else 
			setPatternLayout(false);
		Log.i(LOG_TAG,"DPT["+((mediaplayer!=null)?mediaplayer.fragment_id:0)+"]enter");
	}

	private void setPatternLayout(boolean show) {
		TextView title_text = (TextView) findViewById(R.id.pattern_title_text);
		LinearLayout patternRadioLayout = (LinearLayout) findViewById(R.id.pattern_radio_layout);
		if (show == false) {
			title_text.setVisibility(View.GONE);
			patternRadioLayout.setVisibility(View.GONE);
		} else {
			ViewGroup.LayoutParams lp = title_text.getLayoutParams();
			lp.height = WaxPlayer2.fontSize+15;
			title_text.setLayoutParams(lp);
			lp = patternRadioLayout.getLayoutParams();
			if (android.os.Build.VERSION.SDK_INT < 16)
				lp.height = WaxPlayer2.fontSize*6;
			else
				lp.height = WaxPlayer2.fontSize*8;
			patternRadioLayout.setLayoutParams(lp);
		}
	}

	private void setHwDecLayout(boolean show) {
		TextView title_text = (TextView) findViewById(R.id.hwdec_title_text);
		LinearLayout hwdecRadioLayout = (LinearLayout) findViewById(R.id.hwdec_radio_layout);
		if (show == false) {
			title_text.setVisibility(View.GONE);
			hwdecRadioLayout.setVisibility(View.GONE);
		} else {
			ViewGroup.LayoutParams lp = title_text.getLayoutParams();
			lp.height = WaxPlayer2.fontSize+15;
			title_text.setLayoutParams(lp);
			lp = hwdecRadioLayout.getLayoutParams();
			lp.height = WaxPlayer2.fontSize*6;
			hwdecRadioLayout.setLayoutParams(lp);
		}
	}

	private void initUI() { 
		String title = null, hardplay = null, softplay_swdec = null, softplay_hwdec = null, extplay = null;
		title = ctxPattern.getString(R.string.pattern_dialog_title);
		hardplay = ctxPattern.getString(R.string.pattern_dialog_hardplay);
		softplay_swdec = ctxPattern.getString(R.string.pattern_dialog_softplay_swdec);
		softplay_hwdec = ctxPattern.getString(R.string.pattern_dialog_softplay_hwdec);
		extplay = ctxPattern.getString(R.string.pattern_dialog_extplay);
		//window.setTitle(title);
		TextView title_text = (TextView) findViewById(R.id.pattern_title_text);
		title_text.setText(title);
		radioHardPlay.setText(hardplay);
		radioSoftPlay_swdec.setText(softplay_swdec);
		radioSoftPlay_hwdec.setText(softplay_hwdec);
		radioExtPlay.setText(extplay);
	}

	private void initUI2() { 
		String title = null, hardDecode = null, softDecode = null, softDecode2 = null;
		title = ctxPattern.getString(R.string.hwdec_dialog_title);
		hardDecode = ctxPattern.getString(R.string.hwdec_dialog_harddecode);
		softDecode = ctxPattern.getString(R.string.hwdec_dialog_softdecode);
		softDecode2 = ctxPattern.getString(R.string.hwdec_dialog_softdecode2);
		//window.setTitle(title);
		TextView title_text = (TextView) findViewById(R.id.hwdec_title_text);
		title_text.setText(title);
		radioHardDec.setText(hardDecode);
		radioSoftDec.setText(softDecode);
		radioSoftDec2.setText(softDecode2);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		/*if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_MENU) {
			leave();
			return true;
		}*/
		return super.onKeyDown(keyCode, event);
	}

    @Override
	public void onClick(View v) {
		if (v.getId() == R.id.hardplay) {
			if (lastPlayerId == WaxPlayer2.PLAYER_INNER)
				return;
			paramChanged = true;
			radioHardPlay.setChecked(true);
			radioSoftPlay_swdec.setChecked(false);
			radioSoftPlay_hwdec.setChecked(false);
			radioExtPlay.setChecked(false);
			if (sourceSelect == 2) {
				WaxPlayService._config.setPlayerSwitch(WaxPlayer2.PLAYER_INNER);
				WaxPlayService._config.setVitamioHwdecEnabled(false);
			} else if (sourceSelect == 1) {
				if (mediaplayer != null && mediaplayer.is_Proxy_HLS == true && Config.PLAYER_HLS_DISCARD == false)
					WaxPlayService._config.setPlayerHlsDiscard(true);
			}
			if (mediaplayer != null && WaxPlayService.mediaplayer != null && WaxPlayService.mediaplayer.isExiting() == false && WaxPlayService.mediaplayer.inBackground == 0)
				mediaplayer.SwitchPlayer(WaxPlayer2.PLAYER_INNER, false);
		} else if (v.getId() == R.id.softplay_swdec) {
			if (lastPlayerId == WaxPlayer2.PLAYER_VITAMIO && lastVitamioHwDec == false)
				return;
			if (softDecEnable == false)
				return;
			paramChanged = true;
			radioHardPlay.setChecked(false);
			radioSoftPlay_swdec.setChecked(true);
			radioSoftPlay_hwdec.setChecked(false);
			radioExtPlay.setChecked(false);
			if (sourceSelect == 2) {
				WaxPlayService._config.setPlayerSwitch(WaxPlayer2.PLAYER_VITAMIO);
				WaxPlayService._config.setVitamioHwdecEnabled(false);
				WaxPlayService._config.setVitamioHwdecSetup(true);
			}
			if (mediaplayer != null && WaxPlayService.mediaplayer != null && WaxPlayService.mediaplayer.isExiting() == false && WaxPlayService.mediaplayer.inBackground == 0)
				mediaplayer.SwitchPlayer(WaxPlayer2.PLAYER_VITAMIO, false);
		} else if (v.getId() == R.id.softplay_hwdec) {
			if (lastPlayerId == WaxPlayer2.PLAYER_VITAMIO && lastVitamioHwDec == true)
				return;
			if (softDecEnable == false)
				return;
			paramChanged = true;
			radioHardPlay.setChecked(false);
			radioSoftPlay_swdec.setChecked(false);
			radioSoftPlay_hwdec.setChecked(true);
			radioExtPlay.setChecked(false);
			if (sourceSelect == 2) {
				WaxPlayService._config.setPlayerSwitch(WaxPlayer2.PLAYER_VITAMIO);
				WaxPlayService._config.setVitamioHwdecEnabled(true);
				WaxPlayService._config.setVitamioHwdecSetup(true);
			}
			if (mediaplayer != null && WaxPlayService.mediaplayer != null && WaxPlayService.mediaplayer.isExiting() == false && WaxPlayService.mediaplayer.inBackground == 0)
				mediaplayer.SwitchPlayer(WaxPlayer2.PLAYER_VITAMIO, true);
		} else if (v.getId() == R.id.extplay) {
			if (lastPlayerId == WaxPlayer2.PLAYER_EXTERN)
				return;
			paramChanged = true;
			radioSoftPlay_swdec.setChecked(false);
			radioSoftPlay_hwdec.setChecked(false);
			radioHardPlay.setChecked(false);
			radioExtPlay.setChecked(true);
			if (sourceSelect == 2) {
				WaxPlayService._config.setPlayerSwitch(WaxPlayer2.PLAYER_EXTERN);
				WaxPlayService._config.setVitamioHwdecEnabled(false);
			} else if (sourceSelect == 1) {
				if (mediaplayer != null && mediaplayer.is_Proxy_HLS == true && Config.PLAYER_HLS_DISCARD == false)
					WaxPlayService._config.setPlayerHlsDiscard(true);
			}
			if (mediaplayer != null && WaxPlayService.mediaplayer != null && WaxPlayService.mediaplayer.isExiting() == false && WaxPlayService.mediaplayer.inBackground == 0) {
				mediaplayer.whichPlayer = WaxPlayer2.PLAYER_EXTERN;
				mediaplayer.waxHandler.sendEmptyMessage(WaxPlayer2.MSG_SWITCH_EXTPLAYER);
			}
		} else if (v.getId() == R.id.harddecode) {
			if (lastHwDec == 1 && lastSharpen == 0)
				return;
			paramChanged = true;
			radioHardDec.setChecked(true);
			radioSoftDec.setChecked(false);
			radioSoftDec2.setChecked(false);
			WaxPlayService._config.setHwDec(1);
			WaxPlayService._config.setSharpen(0);
			WaxPlayService.hws /*= com.waxrain.video.SDNativeView.hws*/ = 1; // libnview may be not extracted before setting
			if (WaxPlayService.airplayerLoaded == true)
				WaxPlayService.rvd();
		} else if (v.getId() == R.id.softdecode) {
			if (lastHwDec == 0 && lastSharpen == 0)
				return;
			paramChanged = true;
			radioHardDec.setChecked(false);
			radioSoftDec.setChecked(true);
			radioSoftDec2.setChecked(false);
			WaxPlayService._config.setHwDec(0);
			WaxPlayService._config.setSharpen(0);
			WaxPlayService.hws /*= SDNativeView.hws*/ = 0; // libnview may be not extracted before setting
			if (WaxPlayService.airplayerLoaded == true)
				WaxPlayService.rvd();
		} else if (v.getId() == R.id.softdecode2) {
			if (lastHwDec == 0 && lastSharpen >= 1)
				return;
			paramChanged = true;
			radioHardDec.setChecked(false);
			radioSoftDec.setChecked(false);
			radioSoftDec2.setChecked(true);
			WaxPlayService._config.setHwDec(0);
			WaxPlayService._config.setSharpen(2);
			WaxPlayService.hws /*= SDNativeView.hws*/ = 0; // libnview may be not extracted before setting
			if (WaxPlayService.airplayerLoaded == true)
				WaxPlayService.rvd();
		}
		leave();
	}

	private void leave() {
		Log.i(LOG_TAG,"DPT["+((mediaplayer!=null)?mediaplayer.fragment_id:0)+"]leave");
		try {
			if (sourceSelect == 1 && mediaplayer != null)
				mediaplayer.dialogPattern = null;
			WaxPlayerPattern.this.cancel();
			WaxPlayerPattern.this.dismiss();
		} catch (Exception ex) {
		}
	}
}
