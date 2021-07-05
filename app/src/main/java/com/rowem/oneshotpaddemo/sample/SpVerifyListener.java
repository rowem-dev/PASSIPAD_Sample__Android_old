package com.rowem.oneshotpaddemo.sample;

public interface SpVerifyListener<T extends SpVerifyResponse> {
	public void onResult(T res);
}
