package com.rowem.oneshotpaddemo.sample;

public interface SpEncKeyListener<T extends SpEncKeyResponse> {
	public void onResult(T res);
}
