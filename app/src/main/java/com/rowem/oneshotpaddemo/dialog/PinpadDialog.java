package com.rowem.oneshotpaddemo.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rowem.oneshotpaddemo.R;

import java.util.ArrayList;
import java.util.Collections;

public class PinpadDialog extends Dialog implements View.OnClickListener, OnCancelListener {

	private static final int MODE_JOIN = 1;
	private static final int MODE_JOIN_2 = 2;
	private static final int MODE_LOGIN = 3;

	private View mTitleView;
	private String mSubTitle;
	private String mLabel1;
	private String mLabel2;

	private String mPassword;
	private String mPrevPw;

	private ImageView[] mPwInputImgViews;
	private TextView[] mDigitViews;
	private ArrayList<Integer> mUsedDigit;
	private OnInputListener mListener;

	private Dialog mProgressDlg;

	private int mMode = MODE_LOGIN;

	////////////////////////////////////
	// Constructors
	PinpadDialog(Context context) {
		super(context);
	}

	PinpadDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
	}

	PinpadDialog(Context context, int theme) {
		super(context, theme);
	}

	public static PinpadDialog getSetDialog(Context ctx, OnInputListener listener) {
		PinpadDialog dlg = new PinpadDialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		dlg.mListener = listener;
		dlg.mMode = MODE_JOIN;
		return dlg;
	}

	public static PinpadDialog getCertDialog(Context ctx, OnInputListener listener) {
		PinpadDialog dlg = new PinpadDialog(ctx, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
		dlg.mListener = listener;
		dlg.mMode = MODE_LOGIN;
		return dlg;
	}

	///////////////////////////////////
	// Functions
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.layout_digit_pad_dlg);
		setOnCancelListener(this);

		initViews();
	}

	private void initViews() {
		TextView tv;

		// 타이틀바 처리...
		RelativeLayout rl = (RelativeLayout) findViewById(R.id.layout_title_bar);
		if (mTitleView != null) {
			RelativeLayout.LayoutParams lParam = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
			rl.addView(mTitleView, lParam);
		} else {
			rl.setVisibility(View.GONE);
		}

		// 서브 타이틀 처리.
		tv = (TextView) findViewById(R.id.tv_sub_title);
		if (TextUtils.isEmpty(mSubTitle) == false) {
			tv.setText(mSubTitle);
		} else {
			tv.setVisibility(View.GONE);
		}

		// 라벨 1 처리.
		tv = (TextView) findViewById(R.id.tv_label1);
		if (TextUtils.isEmpty(mLabel1) == false) {
			tv.setText(mLabel1);
		} else {
			tv.setVisibility(View.GONE);
		}

		// 라벨 2 처리.
		tv = (TextView) findViewById(R.id.tv_label2);
		if (TextUtils.isEmpty(mLabel2) == false) {
			tv.setText(mLabel2);
		} else {
			tv.setVisibility(View.GONE);
		}

		// 입력된 패스워드 창.
		mPwInputImgViews = new ImageView[4];
		mPwInputImgViews[0] = (ImageView) findViewById(R.id.iv_input1);
		mPwInputImgViews[1] = (ImageView) findViewById(R.id.iv_input2);
		mPwInputImgViews[2] = (ImageView) findViewById(R.id.iv_input3);
		mPwInputImgViews[3] = (ImageView) findViewById(R.id.iv_input4);

		mPassword = "";
		updatePwView();

		// 닫기 버튼 처리
		findViewById(R.id.btn_close).setOnClickListener(this);
		
		// 숫자키 처리.
		mDigitViews = new TextView[10];
		Resources res = getContext().getResources();
		String pn = getContext().getPackageName();
		for (int i = 0; i < 10; i++) {
			mDigitViews[i] = (TextView) findViewById(res.getIdentifier("tv_num" + i, "id", pn));
			mDigitViews[i].setOnClickListener(this);
		}

		// del 키 처리.
		findViewById(R.id.tv_num_back).setOnClickListener(this);

		// 확인 키 처리.
		findViewById(R.id.tv_num_ok).setOnClickListener(this);

		// 숫자패드 섞기.
		shuffleDigit();
	}

	private void shuffleDigit() {
		if (mUsedDigit == null) {
			mUsedDigit = new ArrayList<Integer>();
			for (int i = 0; i < 10; i++)
				mUsedDigit.add(i);
		}
		Collections.shuffle(mUsedDigit);

		for (int i = 0; i < 10; i++) {
			Integer it = mUsedDigit.get(i);
			mDigitViews[i].setText(it.toString());
			mDigitViews[i].setTag(it);
		}
	}

	private void updatePwView() {
		int pwLen = mPassword.length();

		for (int i = 0; i < mPwInputImgViews.length; i++) {
			if (i < pwLen)
				mPwInputImgViews[i].setImageResource(R.drawable.icon_ball);
			else
				mPwInputImgViews[i].setImageDrawable(null);
		}
	}

	public PinpadDialog setTitleView(View v) {
		mTitleView = v;
		return this;
	}

	public PinpadDialog setSubTitle(String subTitle) {
		mSubTitle = subTitle;

		if (isShowing() == true) {
			TextView tv = (TextView) findViewById(R.id.tv_sub_title);
			if (TextUtils.isEmpty(mSubTitle) == false) {
				tv.setText(mSubTitle);
			} else {
				tv.setVisibility(View.GONE);
			}
		}

		return this;
	}

	public PinpadDialog setSubTitle(int subTitle) {
		return setSubTitle(getContext().getString(subTitle));
	}

	public PinpadDialog setLabel1(String label) {
		mLabel1 = label;

		// 다이얼로그가 보여지고 있는 상태라면 바로 화면에 적용하자.
		if (isShowing() == true) {
			TextView tv = (TextView) findViewById(R.id.tv_label1);
			if (TextUtils.isEmpty(mLabel1) == false) {
				tv.setText(mLabel1);
			} else {
				tv.setVisibility(View.GONE);
			}
		}

		return this;
	}

	public PinpadDialog setLabel1(int label) {
		return setLabel1(getContext().getString(label));
	}

	public PinpadDialog setLabel2(String label) {
		mLabel2 = label;

		if (isShowing() == true) {
			TextView tv = (TextView) findViewById(R.id.tv_label2);
			if (TextUtils.isEmpty(mLabel2) == false) {
				tv.setText(mLabel2);
			} else {
				tv.setVisibility(View.GONE);
			}
		}

		return this;
	}

	public PinpadDialog setLabel2(int label) {
		return setLabel2(getContext().getString(label));
	}

	@Override
	public void onClick(View v) {
		int vId = v.getId();
		if (vId == R.id.btn_close) {
			dismiss();
		} else if (vId == R.id.tv_num_back) {
			procDelKey();
		} else if (vId == R.id.tv_num_ok) {
			performInput();
		} else {
			if (v.getTag() instanceof Integer)
				addDigit((Integer) v.getTag());
		}

	}

	private void performInput() {
		int chkValidRtn = checkInputPw();

		// 일단 입력체크~
		if (chkValidRtn != InterfaceCodes.ERR_OK) {
			if (mListener != null)
				mListener.onInvalidInput(this, chkValidRtn);
			return;
		}

		switch (mMode) {
		case MODE_JOIN:
			mMode = MODE_JOIN_2;
			mPrevPw = mPassword;
			mPassword = "";
			updatePwView();
			shuffleDigit();

			if (mListener != null)
				mListener.onStep1Finished(this);
			break;

		case MODE_JOIN_2:
			if (mListener != null)
				mListener.onInputPw(this, mPassword);
			break;

		case MODE_LOGIN:
			if (mListener != null)
				mListener.onInputPw(this, mPassword);
			break;
		}

	}

	public void clearInputPw() {
		mPassword = "";
		updatePwView();
	}

	private int checkInputPw() {
		if (TextUtils.isEmpty(mPassword) == true)
			return InterfaceCodes.ERR_TOO_SHORT;
		if (mPassword.length() != 4)
			return InterfaceCodes.ERR_TOO_SHORT;

		if (mMode == MODE_JOIN_2) {
			if (mPassword.equals(mPrevPw) == false)
				return InterfaceCodes.ERR_NOT_MATCH;
		}

		return InterfaceCodes.ERR_OK;
	}

	private void procDelKey() {
		if (TextUtils.isEmpty(mPassword) == true)
			return;

		mPassword = mPassword.substring(0, mPassword.length() - 1);
		// MLog.d("최종 조합 : " + mPassword);
		updatePwView();
	}

	private void addDigit(int d) {
		// MLog.d("누른 숫자 : " + d);
		if (mPassword != null && mPassword.length() >= 4)
			return;

		if (mPassword == null)
			mPassword = "" + d;
		else
			mPassword += d;

		// MLog.d("최종 조합 : " + mPassword);
		updatePwView();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if (mListener != null)
			mListener.onCanceled(this);
	}

	public void showProgress() {
		if (mProgressDlg == null) {
			mProgressDlg = new Dialog(getContext());
			mProgressDlg.setCancelable(false);
			mProgressDlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			ProgressBar pb = new ProgressBar(getContext());
			mProgressDlg.setContentView(pb);
		}
		if (mProgressDlg.isShowing() == true) {
			return;
		}

		mProgressDlg.show();
	}

	public void hideProgress() {
		if (mProgressDlg == null || mProgressDlg.isShowing() == false) {
			return;
		}

		mProgressDlg.dismiss();
	}

	public interface OnInputListener {
		public void onStep1Finished(PinpadDialog dlg);

		public void onInputPw(PinpadDialog dlg, String pw);

		public void onCanceled(PinpadDialog dlg);

		public void onInvalidInput(PinpadDialog dlg, int code);
	}

}
