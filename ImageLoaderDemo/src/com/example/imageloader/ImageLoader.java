package com.example.imageloader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

class Utils {
	public static String StringToMd5(String psw) {  
        {  
            try {  
                MessageDigest md5 = MessageDigest.getInstance("MD5");  
                md5.update(psw.getBytes("UTF-8"));  
                byte[] encryption = md5.digest();  
  
                StringBuffer strBuf = new StringBuffer();  
                for (int i = 0; i < encryption.length; i++) {  
                    if (Integer.toHexString(0xff & encryption[i]).length() == 1) {  
                        strBuf.append("0").append(Integer.toHexString(0xff & encryption[i]));  
                    } else {  
                        strBuf.append(Integer.toHexString(0xff & encryption[i]));  
                    }  
                }  
                return strBuf.toString();  
            } catch (NoSuchAlgorithmException e) {  
                return "";  
            } catch (UnsupportedEncodingException e) {  
                return "";  
            }  
        }  
    }
}

public class ImageLoader {
	//内存缓存大小，KB
	private int lruCacheSize;
	//磁盘缓存大小，KB
	private long diskLruCacheSize;
	
	private static ImageLruCache imageLruCache = null;
	private static ImageDiskLruCache imageDiskLruCache = null;
	private static Context context = null;
	volatile private static ImageLoader imageLoaderInst = null;
	
	class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
		private String url;
		private View view;
		public ImageLoaderTask(String url, View view) {
			this.url = url;
			this.view = view;
		}
		private Bitmap downloadImage(){
			Log.i("debug", "ImageLoaderTask--->downloadImage()");
			URL mURL;
			try {
				mURL = new URL(url);
				return BitmapFactory.decodeStream(mURL.openConnection().getInputStream());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		@Override
		protected Bitmap doInBackground(String... params) {
			// TODO Auto-generated method stub
			Log.i("debug", "ImageLoaderTask--->doInBackground() params[0]" + params[0]);
			Bitmap bm = null;
			if(params.length > 0) {
				String key = Utils.StringToMd5(params[0]);
				bm = imageLruCache.get(key, 1);
				if(bm == null){
					bm = imageDiskLruCache.get(key);
					if(bm == null) {
						bm = downloadImage();
						if(bm != null) {
							imageLruCache.put(key, bm, 1);
							imageDiskLruCache.put(key, bm);
						}
					}else {
						imageLruCache.put(key, bm);
					}
				}
			}
			return bm;
		}
		@Override
	    protected void onPostExecute(Bitmap result) {
			Log.i("debug", "ImageLoaderTask--->onPostExecute() load finish.");
			if(view != null)((ImageView) view).setImageBitmap(result);
			Log.i("debug", "ImageLoaderTask--->onPostExecute() update view finish.");
		}
	}
	private ImageLoader(){}
	private ImageLoader(Context context) {
		this.lruCacheSize = (int) (Runtime.getRuntime().maxMemory()/(1024*8));
		this.diskLruCacheSize = context.getCacheDir().getUsableSpace()/(1024*4);
		Log.i("debug", "ImageLoader--->ImageLoader() lruCacheSize="+lruCacheSize+",diskLruCacheSize"+diskLruCacheSize);
		this.context = context;
		imageLruCache = new ImageLruCache(lruCacheSize);
		imageDiskLruCache = new ImageDiskLruCache(context,diskLruCacheSize);
	}
	
	public static ImageLoader getInstance(Context context){
		//return (imageLoaderInst != null)?imageLoaderInst:(imageLoaderInst = new imageLoaderInst(context));
		if(imageLoaderInst != null) {
			
		}else {
			synchronized(ImageLoader.class) {
				if(imageLoaderInst == null) {
					imageLoaderInst = new ImageLoader(context);
				}
			}
		}
		return imageLoaderInst;
	}
	
	public void loadImageByUrl(String url, View view) {
		//executorPool.execute(new ImageLoaderTask(url, view));
		new ImageLoaderTask(url, view).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
	}
	
	public void resizeCache(int lruCacheSize, long diskLruCacheSize) {
		if(Runtime.getRuntime().maxMemory()/(1024*8) < lruCacheSize || lruCacheSize < 0) {
			try {
				throw new Exception("lruCacheSize should not greater than 1/8 of JVM max memory or less than 0!");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(context.getCacheDir().getUsableSpace()/(1024*4) < diskLruCacheSize || diskLruCacheSize < 0) {
			try {
				throw new Exception("diskLruCacheSize should not greater than 1/4 usable cache space or less than 0!");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		imageLruCache.resize(lruCacheSize);
		imageDiskLruCache.resize(diskLruCacheSize);

		this.lruCacheSize = lruCacheSize;
		this.diskLruCacheSize = diskLruCacheSize;
	}
}
