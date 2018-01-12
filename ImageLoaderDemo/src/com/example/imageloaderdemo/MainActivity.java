package com.example.imageloaderdemo;
import com.example.imageloader.ImageLoader;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

public class MainActivity extends Activity {
	ImageView iv;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		String url1 = "http://pic1.win4000.com/wallpaper/2018-01-11/5a570986b283b.jpg";
		String url2 = "http://pic1.win4000.com/wallpaper/2018-01-11/5a5709a9f15da.jpg";
		String url3 = "http://pic1.win4000.com/wallpaper/2018-01-11/5a5709ad8932e.jpg";
		String url4 = "http://pic1.win4000.com/wallpaper/2018-01-11/5a5709b8b9d91.jpg";
		iv = (ImageView)findViewById(R.id.img1);
		ImageLoader.getInstance(this).loadImageByUrl(url1, iv);
		iv = (ImageView)findViewById(R.id.img2);
		ImageLoader.getInstance(this).loadImageByUrl(url2, iv);
		iv = (ImageView)findViewById(R.id.img3);
		ImageLoader.getInstance(this).loadImageByUrl(url3, iv);
		iv = (ImageView)findViewById(R.id.img4);
		ImageLoader.getInstance(this).loadImageByUrl(url4, iv);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
