package com.waxrain.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

import com.waxrain.airplaydmr.*;
import com.waxrain.airplaydmr_SDK.R;
import com.waxrain.droidsender.delegate.Global;
import com.waxrain.utils.Config;

import java.lang.ref.WeakReference;

public class WaxPlayerCheckTexture extends Activity {
	private static final String LOG_TAG = com.waxrain.airplaydmr.WaxPlayService.LOG_TAG;
	private ProgressDialog mPD = null;
	private UIHandler uiHandler;
	private TextureView mTextureView = null;
	private boolean textureCreated = false;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "TEXTURE Checking ...");
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (Global.ACTIVITY_FREEFORM == true)
			setTheme(android.R.style.Theme_Translucent_NoTitleBar);
		uiHandler = new UIHandler(this);
		if (Config.LANDSCAPE_MODE > 0)
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // android:screenOrientation="landscape"
		else
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		try {
			mTextureView = new TextureView(WaxPlayerCheckTexture.this);
			//mTextureView.setBackgroundColor(0x00000000); // TextureView doesn't support displaying a background drawable since Android 8.0
			mTextureView.setSurfaceTextureListener(new SurfaceTextureListener() {
				@Override
				public void onSurfaceTextureAvailable(SurfaceTexture surface, int w, int h) {
					Log.i(LOG_TAG,"textureCreatedC to " + w + " x " + h);
					textureCreated = true;
				}

				@Override
				public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int w, int h) {
					Log.i(LOG_TAG,"textureChangedC to " + w + " x " + h);
				}

				@Override
				public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
					Log.i(LOG_TAG,"textureDestroyedC");
					return false;
				}

				@Override
				public void onSurfaceTextureUpdated(SurfaceTexture surface) {
					//Log.i(LOG_TAG,"textureUpdated_C");
				}
			} );
			LayoutParams param = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			WaxPlayerCheckTexture.this.addContentView(mTextureView, param);
		} catch (Exception e) {
			e.printStackTrace();
		}

		new AsyncTask<Object, Object, Boolean>() {
			@Override
			protected void onPreExecute() {
				mPD = new ProgressDialog(WaxPlayerCheckTexture.this);
				mPD.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mPD.setCancelable(false);
				mPD.setMessage(getString(R.string.waxplayer_boot_sdvideoview));
				//mPD.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
				mPD.show();
			}

			@Override
			protected Boolean doInBackground(Object... params) {
				int timeout = 3000;
				do {
					Global.do_sleep(100);
					timeout -= 100;
				} while (textureCreated == false && timeout > 0);
				if (textureCreated == true)
					WaxPlayService._config.setTextureViewChecked(1);
				else
					WaxPlayService._config.setTextureViewChecked(0);
				Log.i(LOG_TAG, "TEXTURE Done ...");
				return true;
			}

			@Override
			protected void onPostExecute(Boolean done) {
				//if (done) {
					uiHandler.removeMessages(0);
					uiHandler.sendEmptyMessage(0);
				//}
			}
		}.execute();
	}

	@Override
	protected void onDestroy() {
		Log.i(LOG_TAG, "TEXTURE Leaving ...");
		try {
			if (mPD != null) {
				mPD.cancel();
				mPD.dismiss();
				mPD = null;
			}
		} catch (Exception ex) {
		}
		// It looks like startActivity() is in sync mode on Nexus Player
		Intent src = getIntent();//ctx.getIntent();
		Intent i = new Intent();
		i.setClassName(src.getStringExtra("package"), src.getStringExtra("className"));
		i.setData(src.getData());
		i.putExtras(src);
		Global.startActivityWrap(this, i);
		super.onDestroy();
	}

	private static class UIHandler extends Handler {
		private WeakReference<Context> mContext;

		public UIHandler(Context c) {
			mContext = new WeakReference<Context>(c);
		}

		public void handleMessage(Message msg) {
			WaxPlayerCheckTexture ctx = (WaxPlayerCheckTexture) mContext.get();
			switch (msg.what) {
			case 0:
				ctx.finish();
				break;
			default :
				break;
			}
		}
	}
}
