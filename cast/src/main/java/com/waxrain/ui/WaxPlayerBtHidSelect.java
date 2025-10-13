package com.waxrain.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.waxrain.airplaydmr.*;
import com.waxrain.airplaydmr_SDK.R;
import com.waxrain.droidsender.adapter.*;
import com.waxrain.droidsender.delegate.*;
import com.waxrain.utils.*;

import java.util.ArrayList;
import java.util.List;

public class WaxPlayerBtHidSelect extends Dialog implements OnClickListener {
	private static final String LOG_TAG = com.waxrain.airplaydmr.WaxPlayService.LOG_TAG;
	private Context ctxBtHid = null;
	public WaxPlayer2 mediaplayer = null;
	Window window = null;
	private ListView bthiddevlistvw;
	private DeviceAdapter bthiddevAdapter;
	private List<Global$DeviceObj> btdevlist = new ArrayList<Global$DeviceObj>();

	public WaxPlayerBtHidSelect(Context context, int layout, int style, int screenWidth, int screenHeight, WaxPlayer2 mp) {
		super(context, style);

		window = getWindow();
		//window.requestFeature(Window.FEATURE_NO_TITLE);
		ctxBtHid = context;
		setContentView(layout);
		mediaplayer = mp;

		WindowManager.LayoutParams params = window.getAttributes();
		params.alpha = 0.8f;
		params.width = WaxPlayer2.fontSize*12;
		if (params.width > screenWidth - 20) 
			params.width = screenWidth - 20;
		params.height = (int) WaxPlayer2.fontSize*10 + WaxPlayer2.fontSize+15/*Title*/;
		params.gravity = Gravity.CENTER;
		window.setWindowAnimations(R.style.About_dialog);
		window.setAttributes(params);
		setCanceledOnTouchOutside(true);
		window.getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				try {
					if (mediaplayer != null)
						mediaplayer.MoveDialog2(WaxPlayerBtHidSelect.this, R.id.bthidselectdlg_bg, true);
				} catch (Exception ex) {
				}
				//window.getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
			}
		});

		initUI();
		Log.i(LOG_TAG,"DBH["+((mediaplayer!=null)?mediaplayer.fragment_id:0)+"]enter");
	}

	private void initUI() {
		String title = null, hardplay = null, softplay_swdec = null, softplay_hwdec = null, extplay = null;
		title = ctxBtHid.getString(R.string.bthidselectdlg_dialog_title);
		//window.setTitle(title);
		TextView title_text = (TextView) findViewById(R.id.bthidselectdlg_title_text);
		title_text.setText(title);
		bthiddevAdapter = new DeviceAdapter(ctxBtHid, btdevlist, Color.YELLOW);
		bthiddevlistvw = (ListView) findViewById(R.id.bthiddevlist);
		bthiddevlistvw.setAdapter(bthiddevAdapter);
		bthiddevlistvw.setOnItemClickListener(deviceitemlistner);
		bthiddevlistvw.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		bthiddevlistvw.requestFocus();
		updateDevList();
	}

	private void updateDevList() {
		List<Global$DeviceObj> btdevlist2 = Global.getBtHidDevListFound();
		int i = 0, j = 0, matched = 0;
		for (i = 0; i < btdevlist.size(); i++) {
			for (j = 0; j < btdevlist2.size(); j++) {
				if (btdevlist.get(i).localip != null && btdevlist2.get(j).localip != null && btdevlist.get(i).localip.equals(btdevlist2.get(j).localip) ) {
					matched ++;
					break;
				}
			}
		}
		if (matched != btdevlist2.size() || btdevlist.size() != btdevlist2.size()) {
			btdevlist.clear();
			btdevlist.addAll(btdevlist2);
			bthiddevAdapter.notifyDataSetChanged();
		}
		//if (Global.checkBtHidDevAvailable() <= 0)
		//	WaxPlayService.checkInitBtHidAll(ctxBtHid, -1, true);
		(new Handler()).postDelayed(new Runnable() {
			public void run() {
				if (WaxPlayerBtHidSelect.this.isShowing())
					updateDevList();
			}
		}, 3000);
	}

	private AdapterView.OnItemClickListener deviceitemlistner = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
			Global$DeviceObj dev = btdevlist.get(pos);
			Global.initBtHidConnect(ctxBtHid, dev.localip, dev.hostname);
			leave();
		}
	};

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
		//leave();
	}

	private void leave() {
		Log.i(LOG_TAG,"DBH["+((mediaplayer!=null)?mediaplayer.fragment_id:0)+"]leave");
		try {
			if (mediaplayer != null && mediaplayer.dialogBtHidSelect != null) {
				mediaplayer.dialogBtHidSelect = null;
				WaxPlayerBtHidSelect.this.cancel();
				WaxPlayerBtHidSelect.this.dismiss();
			}
		} catch (Exception ex) {
		}
	}
}
