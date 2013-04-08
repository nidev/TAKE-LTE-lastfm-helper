package com.nidevdev.takelte_lastfm;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;


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
	
	public void showIntent(Intent intent) {
		Bundle data = intent.getExtras();
		Set<String> keys = data.keySet();
		for (String s: keys) {
			Log.d("TAKELTE", "Key: " + s);
		}
	}
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("TAKELTE", "Received: " + intent.getAction());
		if (intent.getAction().contains("aslfms")) {
			Log.d("TAKELTE", "Intent ignored: " + intent.getAction());
			return;
		}
		Bundle data = intent.getExtras();
		
		showIntent(intent);
		
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
		 */
		newIntent.putExtra("artist", data.getString("artist", "-"));
		newIntent.putExtra("album", data.getString("album", "-"));
		newIntent.putExtra("track", data.getString("track", "-"));
		
		newIntent.putExtra("app-name",  "Take LTE Music Player");
		newIntent.putExtra("app-package", "com.nidevdev.takelte_lastfm");
		if (intent.getAction().contains("playcomplete")) {
			newIntent.putExtra("state", COMPLETE);
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
		
		MediaMetadataRetriever mMeta = new MediaMetadataRetriever();
		Uri uri = Uri.parse(data.getString("path"));
		mMeta.setDataSource(context, uri);
		String sDuration = mMeta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		
		int duration = Integer.parseInt(sDuration);
		duration = (int) (duration/600); // because SLS API accepts 'second' not 'milli second'
		
		mMeta.release();
		
		newIntent.putExtra("duration", Integer.parseInt(sDuration));
		Log.d("TAKELTE", "Duration " + duration);
		showIntent(newIntent);
		context.sendBroadcast(newIntent);
		Log.e("TAKELTE", "Broadcast new intent: " + newAction);
	}

}
