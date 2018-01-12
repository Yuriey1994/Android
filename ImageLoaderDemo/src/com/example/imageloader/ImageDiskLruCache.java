package com.example.imageloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class ImageDiskLruCache<K, V> {
	private long diskLruCacheSize;
	private long size;
	private File file;
	private String cachePath = null;
	private Context context = null;
	private ImageDiskLruCache(){}
	public ImageDiskLruCache(Context context,long diskLruCacheSize) {
		// TODO Auto-generated constructor stub
		this.diskLruCacheSize = diskLruCacheSize;
		this.context = context;
		this.size = 0;
		this.cachePath = context.getCacheDir().getAbsolutePath();
		initial();
	}
	public final Bitmap get(String key) {
		synchronized (this) {
			Log.i("debug", "ImageDiskLruCache--->get() key="+key);
			String configStr = getConfig();
			if(!configStr.contains(key)) {
				return null;
			}else {
				//已存在缓存，更新
				String[] arr = configStr.split("\\|");
				String[] arrTemp = new String[arr.length+1];
				int index = -1;
				StringBuilder strBuilder = new StringBuilder();
				for(int i = 0; i < arr.length; i++) {
					arrTemp[i] = arr[i] + "|";
					if(key!=null && arr[i].contains(key)) {
						arrTemp[i] = "";
						index = i;
					}
					strBuilder.append(arrTemp[i]);
				}
				if(index != -1) {
					arrTemp[arr.length] = arr[index] + "|";
					strBuilder.append(arrTemp[arr.length]);
					updateConfig(strBuilder.toString());
				}
			}
			return getCacheFromDisk(key);
		}
	}
	private void deleteFromConfig(String key) {
		Log.i("debug","ImageDiskLruCache--->deleteFromConfig() key=" + key);
		String configStr = getConfig();
		String[] arr = configStr.split("\\|");
		int index;
		for(index=0;index<arr.length;index++) {
			if(arr[index].contains(key)){
				break;
			}
		}
		size -= Integer.parseInt(arr[index].split("&")[1]);
		configStr = configStr.replaceFirst(configStr.substring(0, configStr.indexOf("|")), size + "");
		configStr = configStr.replaceFirst(arr[index] + "\\|", "");
		updateConfig(configStr);
	}
	private Bitmap getCacheFromDisk(String key) {
		Log.i("debug","ImageDiskLruCache--->getCacheFromDisk() key=" + key);
		FileInputStream fis = null;
		try {
			file = new File(cachePath+"/img/" + key);
			if(!file.exists()) {
				deleteFromConfig(key);
				return null;
			}
			fis = new FileInputStream(file);
			Bitmap bm = BitmapFactory.decodeFileDescriptor(fis.getFD());
			fis.close();
			Log.i("debug", "ImageDiskLruCache--->getCacheFromDisk() original image size:" + file.length()/1024);
			Log.i("debug", "ImageDiskLruCache--->getCacheFromDisk() decoded bitmap size：" + bm.getByteCount()/1024);
			return bm;
			//return BitmapFactory.decodeFileDescriptor(fis.getFD(), null, opts);
			//return BitmapFactory.decodeByteArray(bmBuff, 0, bmBuff.length);	
		}catch (Exception e) {
			e.printStackTrace();
			try {
				fis.close();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return null;
	}
	private String getConfig(){
		String configStr = null;
		FileInputStream fis = null;
		try {
			Log.i("debug", "ImageDiskLruCache--->getConfig()");
			file = new File(cachePath + "/img/cache.conf");
			fis = new FileInputStream(file);
			byte[] confBuff = new byte[(int) file.length()];
			fis.read(confBuff);
			fis.close();
			configStr = new String(confBuff);
			Log.i("debug", "ImageDiskLruCache--->getConfig() configStr=" + configStr);
		}catch (Exception e) {
			// TODO Auto-generated catch block
			Log.i("debug", e.getMessage());
			try {
				fis.close();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return configStr;
	}
	private void updateConfig(String configStr) {
		Log.i("debug", "ImageDiskLruCache--->updateConfig() configStr:" + configStr);
		FileOutputStream fos = null;
		try{
			fos = new FileOutputStream(cachePath + "/img/cache.conf");
			if(fos != null) {
				fos.write(configStr.getBytes());
				fos.flush();
				fos.close();
			}
		}catch(Exception e) {
			e.printStackTrace();
			try {
				fos.close();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public final void put(String key, Bitmap bm) {
		synchronized (this) {
			Log.i("debug", "ImageDiskLruCache--->put() key="+key);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
	        byte[] bmByteArr = baos.toByteArray();
	        String configStr = getConfig();
			if(bmByteArr.length/1024 > diskLruCacheSize) {
				try {
					Log.i("debug", "ImageDiskLruCache--->put() image too larger to cache !Max disk cache size is " + diskLruCacheSize + "KB,image size is " + bm.getByteCount()/1024 + "KB");
					throw new Exception("ImageDiskLruCache.put() image too larger to cache !Max disk cache size is " + diskLruCacheSize + "KB,image size is " + bm.getByteCount()/1024 + "KB");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				while(diskLruCacheSize < (size + (bmByteArr.length/1024))) {
					String[] temp = configStr.split("\\|");
					if(configStr.length() > 2) {
						if(removeCacheFromDisk(temp[2].split("&")[0])) {
							size -= Integer.parseInt(temp[2].split("&")[1]);
							configStr = configStr.replaceFirst(temp[2] + "\\|", "");
						}
					}
				}
				if(!configStr.contains(key)) {
					if(addCacheToDisk(key, bmByteArr)) {
						size += (bmByteArr.length/1024);
						configStr = configStr.replaceFirst(configStr.substring(0, configStr.indexOf("|")), size + "");
						configStr += (key + "&" + bmByteArr.length/1024 + "|");
					}
				}
				updateConfig(configStr);
			}
		}
	}

	private boolean removeCacheFromDisk(String key) {
		if(key != null) {
			try {
				return new File(cachePath+"/img/" + key).delete();
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		return false;
	}
	private boolean addCacheToDisk(String key, byte[] bmByteArr) {
		try {
			Log.i("debug", "ImageDiskLruCache--->addCacheToDisk(),key="+key);
			file = new File(cachePath+"/img/" + key);
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(bmByteArr);
			fos.flush();
			fos.close();
			return true;
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	private boolean createDiskCache() {
		Log.i("debug", "ImageDiskLruCache--->createDiskCache()");
		boolean createSuccess = false;
		try {
			file = new File(cachePath+"/img");
			File newConfigFile = new File(cachePath + "/img/cache.conf");
			if(!file.exists()) {
				createSuccess = file.mkdirs()?newConfigFile.createNewFile():false;
			}else {
				for(File f:file.listFiles()) {
					f.delete();
				}
				createSuccess = newConfigFile.createNewFile();
			}
			if(createSuccess) {
				updateConfig(size + "|" + diskLruCacheSize + "|");
				Log.i("debug", "ImageDiskLruCache--->createDiskCache() config:" + (size + "|" + diskLruCacheSize + "|"));
				Log.i("debug", "ImageDiskLruCache--->createDiskCache() success!");
			}
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return createSuccess;
	}
	
	private void initial(){
		synchronized (this) {
			FileInputStream fis = null;
			try {
				file = new File(cachePath+"/img/cache.conf");
				if(file.exists()) {
					fis = new FileInputStream(file);
					byte[] confBuff = new byte[(int) file.length()];
					fis.read(confBuff);
					String confStr = new String(confBuff);
					String[] arr = confStr.split("\\|");
					if(arr.length > 1) {
						int oldSize = Integer.parseInt(arr[0]);
						int oldMaxSize = Integer.parseInt(arr[1]);
						size = oldSize;
						if(diskLruCacheSize != oldMaxSize) {
							resize(diskLruCacheSize);
						}
					}else {
						Log.i("debug", "ImageDiskLruCache--->initial() 文件为空，创建");
						createDiskCache();
					}
					fis.close();
				}else {
					Log.i("debug", "ImageDiskLruCache--->initial() 文件不存在，创建");
					createDiskCache();
				}
			}catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				try {
					fis.close();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}
	public void resize(long diskLruCacheSize){
		
	}
}
