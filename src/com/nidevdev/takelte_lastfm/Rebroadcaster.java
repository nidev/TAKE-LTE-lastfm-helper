package com.nidevdev.takelte_lastfm;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;


// Reference: http://code.google.com/p/a-simple-lastfm-scrobbler/wiki/Developers#When_to_send
// Intents of TAKE LTE's music player can be achieved with reverse engineering.

public class Rebroadcaster extends BroadcastReceiver {
	/*
    public static final String PACKAGE_NAME = "com.android.music";
    public static final String NAME = "Android Music Player";


    public static final String ACTION_ANDROID_PLAYSTATECHANGED = "com.android.music.playstatechanged";
    public static final String ACTION_ANDROID_STOP = "com.android.music.playbackcomplete";
    public static final String ACTION_ANDROID_METACHANGED = "com.android.music.metachanged";

    static final String GOOGLE_MUSIC_PACKAGE = "com.google.android.music";
    */
	
	final static int START = 0;
	final static int RESUME = 1;
	final static int PAUSE = 2;
	final static int COMPLETE = 3;
	
	final static String LAST_SONG = "com.nidevdev.takelte_lastfm.STORAGE";
	
	public void showIntent(Intent intent) {
		Bundle data = intent.getExtras();
		Set<String> keys = data.keySet();
		for (String s: keys) {
			Log.d("TAKELTE", "Key: " + s);
		}
	}
	
	public void saveCurrentSong(Context context) {
		
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("TAKELTE", "Received: " + intent.getAction());
		SharedPreferences previousSong = context.getSharedPreferences(LAST_SONG, Context.MODE_PRIVATE);
		Editor changes = previousSong.edit();
		
		Bundle data = intent.getExtras();
		
		//showIntent(intent);
		// 음악 이어폰 빼서 정지시 알림이 그대로 남음
		// com.kttech.music.playstatechanged
		
		String newAction = "com.adam.aslfms.notify.playstatechanged"; // SLS API
		
		Intent newIntent = new Intent();
		newIntent.setAction(newAction);
		
		/* 
		 * Add extra bundle data for SLS API
		 * 
		 * (ready) app-name : application name
		 * (ready) app-package : package info
		 * (acquired) aritst
		 * (acquired) track
		 * (acquired) album
		 * 
		 * (implemented) state - not provided by TAKE LTE Music app.
		 * 
		 * (implemented) duration <- sh*t. TAKE LTE doesn't provide this. need to get it manually.
		 * 
		 * 
		 * XXX: TAKE LTE player, doesn't send intent having action 'com.kttech.music.playbackcomplete'
		 * It would be generated with unnatural way.
		 */
		newIntent.putExtra("artist", data.getString("artist", "-"));
		newIntent.putExtra("album", data.getString("album", "-"));
		newIntent.putExtra("track", data.getString("track", "-"));
		
		newIntent.putExtra("app-name",  "Take LTE Music Player");
		newIntent.putExtra("app-package", "com.nidevdev.takelte_lastfm");
		if (intent.getAction().contains("metachanged")) {
			newIntent.putExtra("state", START);

			// 	restore previous playing info and destroy them
			Intent prevsongIntent = new Intent();
			prevsongIntent.setAction(newAction);
			String prevArtist = previousSong.getString("artist", "-");
			String prevTrack = previousSong.getString("track",  "-");
			String prevAlbum = previousSong.getString("track", "-");
			int prevDuration = previousSong.getInt("duration", 0);
			
			prevsongIntent.putExtra("artist", prevArtist);
			prevsongIntent.putExtra("album", prevAlbum);
			prevsongIntent.putExtra("track", prevTrack);
			prevsongIntent.putExtra("duration", prevDuration);
			prevsongIntent.putExtra("app-name",  "Take LTE Music Player");
			prevsongIntent.putExtra("app-package", "com.nidevdev.takelte_lastfm");
			prevsongIntent.putExtra("state", COMPLETE);
			context.sendBroadcast(prevsongIntent);
			showIntent(prevsongIntent);
			Log.e("TAKELTE", "Unnatural COMPLETE of song");
			Log.e("TAKELTE", "Broadcast new intent: " + newAction);
			// .... and let the app send new song intent after exiting this code
			// So, on 'metachanged' intent, this class broadcasts two intents.
			// One for previously played song, the other one for currently and newly played song. 
		}
		else {
			if (data.getBoolean("isplaying", false)) {
				if (data.getString("sub_what") != null) {
					newIntent.putExtra("state", START);
				}
				else {
					newIntent.putExtra("state", RESUME);
				}
			}
			else {
				newIntent.putExtra("state", PAUSE);
			}
		}
		
		int duration = 0;
		try
		{
			MediaMetadataRetriever mMeta = new MediaMetadataRetriever();
			Uri uri = Uri.parse(data.getString("path"));
			mMeta.setDataSource(context, uri);
			String sDuration = mMeta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			
			duration = Integer.parseInt(sDuration);
			duration = (int) (duration/1000); // because SLS API accepts 'second' not 'milli second'
			mMeta.release();
		}
		catch (Exception e)
		{
			Toast.makeText(context, "TAKELTE_LastFM Exception: " + e.toString(), Toast.LENGTH_LONG).show();
			return;
		}
		

		// Save current song info.
		changes.putString("artist", data.getString("artist", "-"));
		changes.putString("track", data.getString("track", "-"));
		changes.putString("album", data.getString("album", "-"));
		changes.putInt("duration", duration);
		changes.putLong("lastplay", SystemClock.elapsedRealtime());
		changes.commit();
		
		
		newIntent.putExtra("duration", duration);
		//showIntent(intent);
		context.sendBroadcast(newIntent);
		Log.e("TAKELTE", "Broadcast new intent: " + newAction);
	}

}
