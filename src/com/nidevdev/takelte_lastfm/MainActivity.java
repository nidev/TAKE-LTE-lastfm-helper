package com.nidevdev.takelte_lastfm;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {
	
	OnSharedPreferenceChangeListener onUpdateListener = null;
	
	public String getNowPlaying()
	{
		SharedPreferences songinfo = getSharedPreferences(Rebroadcaster.LAST_SONG, MODE_PRIVATE);
		String artist = songinfo.getString("artist", "(unknown)");
		String title = songinfo.getString("track",  "(unknown)");
		String base = "#NowPlaying [ " + artist + " - " + title + " ] from #TAKE_LTE";
		return base;
		
	}
	
	public void updateMainScreen()
	{
		SharedPreferences songinfo = getSharedPreferences(Rebroadcaster.LAST_SONG, MODE_PRIVATE);
		String artist = songinfo.getString("artist", "(unknown)");
		String title = songinfo.getString("track",  "(unknown)");
		String album = songinfo.getString("album",  "(unknown)");
		
		TextView t_artist = (TextView) findViewById(R.id.main_textview_recent_artist);
		TextView t_title = (TextView) findViewById(R.id.main_textview_recent_title);
		TextView t_album = (TextView) findViewById(R.id.main_textview_recent_album);
		
		t_artist.setText(artist);
		t_title.setText(title);
		t_album.setText(album);
		
		EditText nowplaying_preview = (EditText) findViewById(R.id.main_edittext_preview);
		nowplaying_preview.setText(getNowPlaying());
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		((Button) findViewById(R.id.main_button_nowplaying)).setOnClickListener(this);
		((Button) findViewById(R.id.main_button_about)).setOnClickListener(this);
		
		SharedPreferences songinfo = getSharedPreferences(Rebroadcaster.LAST_SONG, MODE_PRIVATE);
		onUpdateListener = new OnSharedPreferenceChangeListener() {
			
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
					String key) {
				// TODO Auto-generated method stub
				updateMainScreen();
			}
		};
		
		// on update of status
		songinfo.registerOnSharedPreferenceChangeListener(onUpdateListener);
		
		// last shot.
		updateMainScreen();
		
	}
	
	public void onClick(View v)
	{
		if (v.getId() == R.id.main_button_nowplaying)
		{
			String nowplaying = getNowPlaying();
			Intent toBeTexted = new Intent();
			toBeTexted.setAction(Intent.ACTION_SEND);
			toBeTexted.setType("text/plain");
			//toBeTexted.putExtra(Intent.EXTRA_SUBJECT, "NowPlaying");
			toBeTexted.putExtra(Intent.EXTRA_TEXT, nowplaying);
			startActivity(Intent.createChooser(toBeTexted, "NowPlaying"));
		}
		else if (v.getId() == R.id.main_button_about)
		{
			Intent intent = new Intent(this, About.class);
			startActivity(intent);
		}
		else
		{
			// nothing to do
		}
	}
	
	public void onDestory()
	{
		if (onUpdateListener != null)
		{
			// remove handler
			SharedPreferences songinfo = getSharedPreferences(Rebroadcaster.LAST_SONG, MODE_PRIVATE);
			songinfo.unregisterOnSharedPreferenceChangeListener(onUpdateListener);
		}
	}
}

