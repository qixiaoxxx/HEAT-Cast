package com.waxrain.ui;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.TextView;

import com.waxrain.airplaydmr_SDK.R;

public class WaxPlayerSpeed extends Dialog implements OnClickListener {
	private static final String LOG_TAG = com.waxrain.airplaydmr.WaxPlayService.LOG_TAG;
	RadioButton radioSpeed0 = null;
	RadioButton radioSpeed1 = null;
	RadioButton radioSpeed2 = null;
	RadioButton radioSpeed3 = null;
	public WaxPlayer2 mediaplayer = null;
	int saved_speed = -1;
	private Context ctxSpeed = null;
	Window window = null;

	public WaxPlayerSpeed(Context context, int layout, int style, int screenWidth, int screenHeight, WaxPlayer2 mp) {
		super(context, style);

		window = getWindow();
		//window.requestFeature(Window.FEATURE_NO_TITLE);
		ctxSpeed = context;
		setContentView(layout);
		mediaplayer = mp;
		saved_speed = mp.playSpeedIndex;
		
		radioSpeed0 = (RadioButton) this.findViewById(R.id.speed0);
		radioSpeed1 = (RadioButton) this.findViewById(R.id.speed1);
		radioSpeed2 = (RadioButton) this.findViewById(R.id.speed2);
		radioSpeed3 = (RadioButton) this.findViewById(R.id.speed3);
		radioSpeed0.setFocusable(true);
		radioSpeed1.setFocusable(true);
		radioSpeed2.setFocusable(true);
		radioSpeed3.setFocusable(true);
		if (saved_speed == 0) {
			radioSpeed0.setChecked(true);
			radioSpeed1.setChecked(false);
			radioSpeed2.setChecked(false);
			radioSpeed3.setChecked(false);
			radioSpeed0.requestFocus();
		} else if (saved_speed == 1) {
			radioSpeed0.setChecked(false);
			radioSpeed1.setChecked(true);
			radioSpeed2.setChecked(false);
			radioSpeed3.setChecked(false);
			radioSpeed1.requestFocus();
		} else if (saved_speed == 2) {
			radioSpeed0.setChecked(false);
			radioSpeed1.setChecked(false);
			radioSpeed2.setChecked(true);
			radioSpeed3.setChecked(false);
			radioSpeed2.requestFocus();
		} else {
			radioSpeed0.setChecked(false);
			radioSpeed1.setChecked(false);
			radioSpeed2.setChecked(false);
			radioSpeed3.setChecked(true);
			radioSpeed3.requestFocus();
		}
		radioSpeed0.setOnClickListener(this);
		radioSpeed1.setOnClickListener(this);
		radioSpeed2.setOnClickListener(this);
		radioSpeed3.setOnClickListener(this);

		WindowManager.LayoutParams params = window.getAttributes();
		params.alpha = 0.8f;
		params.width = WaxPlayer2.fontSize*8;
		if (params.width > screenWidth - 20) 
			params.width = screenWidth - 20;
		params.height = (int) WaxPlayer2.fontSize*8 + WaxPlayer2.fontSize+15/*Title*/;
		params.gravity = Gravity.CENTER;
		window.setWindowAnimations(R.style.About_dialog);
		window.setAttributes(params);
		setCanceledOnTouchOutside(true);
		window.getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				try {
					if (mediaplayer != null)
						mediaplayer.MoveDialog2(WaxPlayerSpeed.this, R.id.speeddlg_bg, true);
				} catch (Exception ex) {
				}
				//window.getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});
		
		initUI();
		Log.i(LOG_TAG,"DSP["+((mediaplayer!=null)?mediaplayer.fragment_id:0)+"]enter");
	}

	private void initUI() {
		String title = null;
		title = ctxSpeed.getString(R.string.speed_dialog_title);
		//window.setTitle(title);
		TextView title_text = (TextView) findViewById(R.id.speed_title_text);
		title_text.setText(title);
		radioSpeed0.setText(mediaplayer.playSpeedStr[0]);
		radioSpeed1.setText(mediaplayer.playSpeedStr[1]);
		radioSpeed2.setText(mediaplayer.playSpeedStr[2]);
		radioSpeed3.setText(mediaplayer.playSpeedStr[3]);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		/*if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_MENU) {
			try {
				if (mediaplayer != null && mediaplayer.dialogSpeed != null) {
					mediaplayer.dialogSpeed = null;
					WaxPlayerSpeed.this.cancel();
					WaxPlayerSpeed.this.dismiss();
				}
			} catch (Exception ex) {
			}
			return true;
		}*/
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int id = v.getId();
		if (id == R.id.speed0) {
			radioSpeed0.setChecked(true);
			radioSpeed1.setChecked(false);
			radioSpeed2.setChecked(false);
			radioSpeed3.setChecked(false);
			if (saved_speed != 0 && mediaplayer != null)
				mediaplayer.SetPlaySpeed(0);
		} else if (id == R.id.speed1) {
			radioSpeed0.setChecked(false);
			radioSpeed1.setChecked(true);
			radioSpeed2.setChecked(false);
			radioSpeed3.setChecked(false);
			if (saved_speed != 1 && mediaplayer != null)
				mediaplayer.SetPlaySpeed(1);
		} else if (id == R.id.speed2) {
			radioSpeed0.setChecked(false);
			radioSpeed1.setChecked(false);
			radioSpeed2.setChecked(true);
			radioSpeed3.setChecked(false);
			if (saved_speed != 2 && mediaplayer != null)
				mediaplayer.SetPlaySpeed(2);
		} else if (id == R.id.speed3) {
			radioSpeed0.setChecked(false);
			radioSpeed1.setChecked(false);
			radioSpeed2.setChecked(false);
			radioSpeed3.setChecked(true);
			if (saved_speed != 3 && mediaplayer != null)
				mediaplayer.SetPlaySpeed(3);
		}
		leave();
	}

	private void leave() {
		Log.i(LOG_TAG,"DSP["+((mediaplayer!=null)?mediaplayer.fragment_id:0)+"]leave");
		try {
			if (mediaplayer != null && mediaplayer.dialogSpeed != null) {
				mediaplayer.dialogSpeed = null;
				WaxPlayerSpeed.this.cancel();
				WaxPlayerSpeed.this.dismiss();
			}
		} catch (Exception ex) {
		}
	}
}
