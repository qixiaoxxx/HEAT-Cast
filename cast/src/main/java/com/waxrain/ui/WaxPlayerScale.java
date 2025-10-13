package com.waxrain.ui;

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
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.waxrain.airplaydmr.WaxPlayService;
import com.waxrain.airplaydmr_SDK.R;
import com.waxrain.utils.*;

public class WaxPlayerScale extends Dialog implements OnClickListener {
	private static final String LOG_TAG = com.waxrain.airplaydmr.WaxPlayService.LOG_TAG;
	RadioButton radioStretch = null, radioScale = null, radioZoom = null;
	CheckBox fullscreenMode = null, lockMode = null;
	private Context ctxScale = null;
	public WaxPlayer2 mediaplayer = null;
	Window window = null;

	public WaxPlayerScale(Context context, int layout, int style, int scaleMode, int screenWidth, int screenHeight, boolean zoomEnable) {
		this(context, layout, style, scaleMode, screenWidth, screenHeight, zoomEnable, null);
	}

	public WaxPlayerScale(Context context, int layout, int style, int scaleMode, int screenWidth, int screenHeight, boolean zoomEnable, WaxPlayer2 mp) {
		super(context, style);
		if (WaxPlayService.mediaplayer != null)
			WaxPlayService.mediaplayer.enter_FULLSCREEN(getWindow());

		window = getWindow();
		//window.requestFeature(Window.FEATURE_NO_TITLE);
		ctxScale = context;
		mediaplayer = mp;
		setContentView(layout);
		
		radioStretch = (RadioButton) this.findViewById(R.id.stretchmode);
		radioScale = (RadioButton) this.findViewById(R.id.scalemode);
		radioZoom = (RadioButton) this.findViewById(R.id.zoommode);
		fullscreenMode = (CheckBox) this.findViewById(R.id.fullscreenmode);
		lockMode = (CheckBox) this.findViewById(R.id.lockmode);
		radioStretch.setFocusable(true);
		radioScale.setFocusable(true);
		radioZoom.setFocusable(true);
		fullscreenMode.setFocusable(true);
		if (zoomEnable == false)
			radioZoom.setVisibility(View.GONE);
		if (scaleMode == WaxPlayer2.LAYOUT_STRETCH) {
			radioStretch.setChecked(true);
			radioScale.setChecked(false);
			radioZoom.setChecked(false);
			radioStretch.requestFocus();
		} else if (scaleMode == WaxPlayer2.LAYOUT_SCALE) {
			radioStretch.setChecked(false);
			radioScale.setChecked(true);
			radioZoom.setChecked(false);
			radioScale.requestFocus();
		} else if (scaleMode == WaxPlayer2.LAYOUT_ZOOM && zoomEnable == true) {
			radioStretch.setChecked(false);
			radioScale.setChecked(false);
			radioZoom.setChecked(true);
			radioZoom.requestFocus();
		}
		radioStretch.setOnClickListener(this);
		radioScale.setOnClickListener(this);
		radioZoom.setOnClickListener(this);
		fullscreenMode.setOnClickListener(this);
		fullscreenMode.setChecked(false);
		lockMode.setOnClickListener(this);
		lockMode.setChecked(false);
		boolean disable_fullscreen = false;
		if (WaxPlayService.mediaplayer != null) {
			int zoom_state = WaxPlayService.mediaplayer.CHECKZOOM_MPlayer(mediaplayer.fragment_id-1, true);
			if (zoom_state == 1) {
				fullscreenMode.setChecked(true);
				disable_fullscreen = true;
			} else if (zoom_state == 10) {
				fullscreenMode.setChecked(false);
				disable_fullscreen = false;
			} else if (zoom_state == 11) {
				fullscreenMode.setChecked(true);
				disable_fullscreen = false;
			}
			lockMode.setChecked(mediaplayer.isLocked2);
		}
		if (disable_fullscreen == true) {
			fullscreenMode.setEnabled(false);
			fullscreenMode.setClickable(false);
		}

		WindowManager.LayoutParams params = window.getAttributes();
		params.alpha = 0.8f;
		params.width = WaxPlayer2.fontSize*15;
		if (params.width > screenWidth - 20)
			params.width = screenWidth - 20;
		params.height = (int) WaxPlayer2.fontSize*8 + (WaxPlayer2.fontSize+15)*2/*Title*/;
		if (WaxPlayService.MAX_PLAYERS_N1 <= 1)
			params.height /= 2;
		if (zoomEnable == true)
			params.height += (int)WaxPlayer2.fontSize * 2;
		params.gravity = Gravity.CENTER;
		window.setWindowAnimations(R.style.About_dialog);
		window.setAttributes(params);
		setCanceledOnTouchOutside(true);
		window.getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				try {
					if (mediaplayer != null)
						mediaplayer.MoveDialog2(WaxPlayerScale.this, R.id.scaledlg_bg, true);
				} catch (Exception ex) {
				}
				//window.getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});
		
		initUI(zoomEnable);
		Log.i(LOG_TAG,"DSC["+((mediaplayer!=null)?mediaplayer.fragment_id:0)+"]enter");
	}

	private void initUI(boolean zoomEnable) {
		String title = null, stretch = null, scale = null, zoom = null, title2 = null, fullscreen2 = null, lock = null;
		title = ctxScale.getString(R.string.scale_dialog_title);
		stretch = ctxScale.getString(R.string.scale_dialog_fullscreen);
		scale = ctxScale.getString(R.string.scale_dialog_original);
		zoom = ctxScale.getString(R.string.scale_dialog_zoom);
		title2 = ctxScale.getString(R.string.scale_dialog_title2);
		fullscreen2 = ctxScale.getString(R.string.scale_dialog_fullscreen2);
		lock = ctxScale.getString(R.string.scale_dialog_locked);
		//window.setTitle(title);

		TextView title_text = (TextView) findViewById(R.id.scale_title_text);
		title_text.setText(title);
		ViewGroup.LayoutParams lp = title_text.getLayoutParams();
		lp.height = WaxPlayer2.fontSize+15;
		title_text.setLayoutParams(lp);
		radioStretch.setText(stretch);
		radioScale.setText(scale);
		radioZoom.setText(zoom);
		LinearLayout scaleLayout = (LinearLayout) findViewById(R.id.scale_main_layout);
		lp = scaleLayout.getLayoutParams();
		lp.height = WaxPlayer2.fontSize*4;
		if (zoomEnable == true)
			lp.height += (int)WaxPlayer2.fontSize * 2;
		scaleLayout.setLayoutParams(lp);

		TextView title_text2 = (TextView) findViewById(R.id.scale_title_text2);
		title_text2.setText(title2);
		lp = title_text2.getLayoutParams();
		lp.height = WaxPlayer2.fontSize+15;
		title_text2.setLayoutParams(lp);
		fullscreenMode.setText(fullscreen2);
		lockMode.setText(lock);
		LinearLayout controlLayout = (LinearLayout) findViewById(R.id.scale_control_layout);
		lp = controlLayout.getLayoutParams();
		lp.height = WaxPlayer2.fontSize*4;
		controlLayout.setLayoutParams(lp);
		if (WaxPlayService.MAX_PLAYERS_N1 <= 1) {
			title_text2.setVisibility(View.GONE);
			controlLayout.setVisibility(View.GONE);
		}
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
		// TODO Auto-generated method stub
		int id = v.getId();
		if (id == R.id.scalemode) {
			radioScale.setChecked(true);
			radioStretch.setChecked(false);
			radioZoom.setChecked(false);
			try {
				if (mediaplayer != null && mediaplayer.isExiting() == false && WaxPlayService.mediaplayer != null && WaxPlayService.mediaplayer.inBackground == 0)
					mediaplayer.Scale(0, WaxPlayer2.LAYOUT_SCALE);
			} catch (Exception ex) {
			}
		} else if (id == R.id.stretchmode) {
			radioScale.setChecked(false);
			radioStretch.setChecked(true);
			radioZoom.setChecked(false);
			try {
				if (mediaplayer != null && mediaplayer.isExiting() == false && WaxPlayService.mediaplayer != null && WaxPlayService.mediaplayer.inBackground == 0)
					mediaplayer.Scale(0, WaxPlayer2.LAYOUT_STRETCH);
			} catch (Exception ex) {
			}
		} else if (id == R.id.zoommode) {
			radioScale.setChecked(false);
			radioStretch.setChecked(false);
			radioZoom.setChecked(true);
			try {
				if (mediaplayer != null && mediaplayer.isExiting() == false && WaxPlayService.mediaplayer != null && WaxPlayService.mediaplayer.inBackground == 0)
					mediaplayer.Scale(0, WaxPlayer2.LAYOUT_ZOOM);
			} catch (Exception ex) {
			}
		} else if (id == R.id.fullscreenmode) {
			if (fullscreenMode.isChecked() == false) {
				fullscreenMode.setChecked(false);
				if (WaxPlayService.mediaplayer != null)
					WaxPlayService.mediaplayer.FULLSCREEN_MPlayer(false, true);
			} else {
				fullscreenMode.setChecked(true);
				if (WaxPlayService.mediaplayer != null)
					WaxPlayService.mediaplayer.FULLSCREEN_MPlayer(true, true);
			}
		} else if (id == R.id.lockmode) {
			if (lockMode.isChecked() == false) {
				lockMode.setChecked(false);
				if (mediaplayer != null && mediaplayer.isExiting() == false)
					mediaplayer.lostLock2();
			} else {
				lockMode.setChecked(true);
				if (mediaplayer != null && mediaplayer.isExiting() == false)
					mediaplayer.gotLock2();
			}
		}
		leave();
	}

	private void leave() {
		Log.i(LOG_TAG,"DSC["+((mediaplayer!=null)?mediaplayer.fragment_id:0)+"]leave");
		try {
			if (mediaplayer != null && mediaplayer.dialogScale != null) {
				mediaplayer.dialogScale = null;
				WaxPlayerScale.this.cancel();
				WaxPlayerScale.this.dismiss();
			}
		} catch (Exception ex) {
		}
	}
}
