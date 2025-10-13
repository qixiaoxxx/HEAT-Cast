package com.waxrain.ui;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.waxrain.airplaydmr.*;
import com.waxrain.airplaydmr_SDK.R;
import com.waxrain.utils.*;

public class WaxPlayerLoading extends Dialog {
	private static final String LOG_TAG = com.waxrain.airplaydmr.WaxPlayService.LOG_TAG;
	private Context ctxLoading = null;
	public WaxPlayer2 mediaplayer = null;
	Window window = null;

	public WaxPlayerLoading(Context context, int layout, int style, int strId, int screenWidth, int screenHeight, WaxPlayer2 mp) {
		super(context, style);
		if (WaxPlayService.mediaplayer != null)
			WaxPlayService.mediaplayer.enter_FULLSCREEN(getWindow());

		window = getWindow();
		ctxLoading = context;
		mediaplayer = mp;
		setContentView(R.layout.waxplayer_loading_n1);

		initUI(strId);
		Log.i(LOG_TAG,"DLD["+((mediaplayer!=null)?mediaplayer.fragment_id:0)+"]enter");

		WindowManager.LayoutParams params = window.getAttributes();
		params.alpha = 0.8f;
		params.gravity = Gravity.CENTER;
		window.setWindowAnimations(R.style.About_dialog);
		window.setAttributes(params);
		window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		setCanceledOnTouchOutside(false);
		setCancelable(false);
		window.getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				try {
					if (mediaplayer != null)
						mediaplayer.MoveDialog2(WaxPlayerLoading.this, R.id.loadingdlg_bg, true);
				} catch (Exception ex) {
				}
				//window.getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});
	}

	private void initUI(int strId) {
		String title = null;
		title = ctxLoading.getString(strId);
		//window.setTitle(title);
		TextView title_text = (TextView) findViewById(R.id.loading_title_text);
		title_text.setText(title);

		ImageView localImage = (ImageView)this.findViewById(R.id.loadingpic);
		localImage.setVisibility(View.GONE);

		ProgressBar progressbar = (ProgressBar)this.findViewById(R.id.loadingbar); // Middle
		ProgressBar progressbar2 = (ProgressBar)this.findViewById(R.id.loadingbar2); // Large
		//if (screenWidth > 1366) {
		//	progressbar.setVisibility(View.GONE);
		//	progressbar2.setVisibility(View.VISIBLE);
		//} else {
			progressbar.setVisibility(View.VISIBLE);
			progressbar2.setVisibility(View.GONE);
		//}

		/*ImageView localImage = (ImageView)this.findViewById(R.id.loadingpic);
		BitmapFactory.Options options = new BitmapFactory.Options();
		if (WaxPlayService.mediaplayer == null) // onCreate() -> onPause() -> onCreate() in WaxPlayer
			WaxPlayService.mediaplayer = (WaxPlayer)ctxLoading;
		if (adsMode == true) {
			if (WaxPlayService.mediaplayer.adsBitmap == null) {
				String adsPath = WaxPlayService.ssp + "/" + WaxPlayService.apn;
				File adsFile = new File(adsPath);
				if (adsFile.exists()) {
					try	{
						options.inJustDecodeBounds = true;
						options.outWidth = options.outHeight = -1;
						BitmapFactory.decodeFile(adsPath, options);
						if (options.outWidth > 0 && options.outHeight > 0) {
							default_width = options.outWidth;
							default_height = options.outHeight;
						}
						options.inJustDecodeBounds = false;
						WaxPlayService.mediaplayer.adsBitmap = BitmapFactory.decodeFile(adsPath, options);
					} catch (Exception e) {
					}
				}
				if (WaxPlayService.mediaplayer.adsBitmap != null)
					adsFile.delete();
			}
			if (WaxPlayService.mediaplayer.adsBitmap != null) {
				localImage.setImageBitmap(WaxPlayService.mediaplayer.adsBitmap);
				WaxPlayService.mediaplayer.adsStartTime = System.currentTimeMillis();
			} else {
				WaxPlayer.adsMode = false;
			}
		}
		if (WaxPlayer.adsMode == false) {
			options.inJustDecodeBounds = true;
			options.outWidth = options.outHeight = -1;
			BitmapFactory.decodeResource(ctxLoading.getResources(), R.drawable.loadingpic_cn, options);
			if (options.outWidth > 0 && options.outHeight > 0) {
				default_width = options.outWidth;
				default_height = options.outHeight;
			}
			if (Config.LOCALE == Config.CHINESE_LOCAL) {
				if (WaxPlayService.drawable_loadingpic_cn != null )
					localImage.setImageBitmap(WaxPlayService.drawable_loadingpic_cn);
				else
					localImage.setImageResource(R.drawable.loadingpic_cn);
			} else {
				if (WaxPlayService.drawable_loadingpic_en != null )
					localImage.setImageBitmap(WaxPlayService.drawable_loadingpic_en);
				else
					localImage.setImageResource(R.drawable.loadingpic_en);
			}
		}*/
	}

	public void updateUI(String title2) {
		if (title2 != null && title2.length() > 0) {
			TextView title_text = (TextView) findViewById(R.id.loading_title_text);
			title_text.setText(title2);
		}
	}

	public void updateUI(int percent) {
		String title = null;
		title = ctxLoading.getString(R.string.waxplayer_toast_loaded) + Integer.toString(percent) + "%";
		TextView title_text = (TextView) findViewById(R.id.loading_title_text);
		title_text.setText(title);
	}
}
