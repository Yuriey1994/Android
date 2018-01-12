package com.example.imageloader;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

public class ImageLruCache extends LruCache<String, Bitmap> {
	
	@Override
	protected int sizeOf(String key, Bitmap value) {
		// TODO Auto-generated method stub
		return value.getByteCount()/1024;
	}
	public ImageLruCache(int maxSize) {
		super(maxSize);
		// TODO Auto-generated constructor stub
	}
	public Bitmap get(String key,int displayLog){
		Bitmap bm = super.get(key);
		Log.i("debug", "ImageLruCache--->get() key="+key);
		return bm;
	}
	public Bitmap put(String key, Bitmap bm, int displayLog){
		Bitmap _bm = super.put(key, bm);
		Log.i("debug", "ImageLruCache--->put() key="+key);
		return _bm;
	}
}
