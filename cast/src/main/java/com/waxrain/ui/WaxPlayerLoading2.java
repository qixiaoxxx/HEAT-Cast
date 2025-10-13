package com.waxrain.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.waxrain.airplaydmr.WaxPlayService;
import com.waxrain.airplaydmr_SDK.R;
import com.waxrain.utils.*;

public class WaxPlayerLoading2 extends Dialog {
	private static final String LOG_TAG = com.waxrain.airplaydmr.WaxPlayService.LOG_TAG;
	private Context ctxLoading2 = null;
	public WaxPlayer2 mediaplayer = null;
	Window window = null;

	public WaxPlayerLoading2(Context context, int layout, int style, int screenWidth, int screenHeight, WaxPlayer2 mp) {
		super(context, style);
		if (WaxPlayService.mediaplayer != null)
			WaxPlayService.mediaplayer.enter_FULLSCREEN(getWindow());

		window = getWindow();
		ctxLoading2 = context;
		mediaplayer = mp;

		initUI();
		Log.i(LOG_TAG,"DLD2["+((mediaplayer!=null)?mediaplayer.fragment_id:0)+"]enter");

		WindowManager.LayoutParams params = window.getAttributes();
		params.alpha = 0.8f;
		params.dimAmount = 0.0f;
		int margin = (int)context.getResources().getDimension(R.dimen.scanover_margin);
		int lsize = 30;
		if (screenWidth > 800)
			lsize = 60;
		params.width = WindowManager.LayoutParams.WRAP_CONTENT;//lsize;
		params.height = WindowManager.LayoutParams.WRAP_CONTENT;//lsize;
		params.gravity = Gravity.TOP | Gravity.LEFT;
		params.x = margin*13;
		//params.verticalMargin = 0.1f;
		params.y = margin*13;
		//params.horizontalMargin = 0.1f;
		window.setWindowAnimations(R.style.About_dialog);
		window.setAttributes(params);
		setCanceledOnTouchOutside(false);
		window.getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				try {
					if (mediaplayer != null)
						mediaplayer.MoveDialog2(WaxPlayerLoading2.this, 0, false);
				} catch (Exception ex) {
				}
				//window.getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});
	}

	private void initUI() {
		LinearLayout linearLayout = new LinearLayout(ctxLoading2);
		linearLayout.setBackgroundColor(Color.TRANSPARENT);
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		linearLayout.setGravity(Gravity.CENTER);
		ProgressBar progressbar = new ProgressBar(ctxLoading2, null, android.R.attr.progressBarStyleSmall);
		linearLayout.addView(progressbar);
		setContentView(linearLayout);
	}
}
