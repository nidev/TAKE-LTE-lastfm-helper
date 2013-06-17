package com.nidevdev.takelte_lastfm;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
	
	final private int NOTIFICATION_ID = 10010;
	
	public void showIntent(Intent intent) {
		Bundle data = intent.getExtras();
		Set<String> keys = data.keySet();
		for (String s: keys) {
			Log.d("TAKELTE", "Key: " + s);
		}
	}
	
	public void sendNotification(Context context)
	{
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		PendingIntent launchApplication = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
		
		Notification.Builder notifier = new Notification.Builder(context);
		notifier.setTicker(context.getText(R.string.notify_title));
		notifier.setContentTitle(context.getText(R.string.notify_title));
		notifier.setContentText(context.getText(R.string.notify_contents));
		notifier.setAutoCancel(true);
		notifier.setSmallIcon(R.drawable.ic_launcher);
		notifier.setWhen(System.currentTimeMillis());
		notifier.setContentIntent(launchApplication);
		Notification notice = notifier.build();
		
		manager.notify(NOTIFICATION_ID, notice);
		
	}
	
	public void clearNotification(Context context)
	{
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(NOTIFICATION_ID);
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
			Log.e("TAKELTE", "Intercepted. changed to new song");
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
			// If it's not being played.
			clearNotification(context);
		}
		
		int duration = 0;
		MediaMetadataRetriever mMeta = new MediaMetadataRetriever();
		try
		{
			Uri uri = Uri.parse(data.getString("path"));
			mMeta.setDataSource(context, uri);
			String sDuration = mMeta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			
			duration = Integer.parseInt(sDuration);
			duration = (int) (duration/1000); // because SLS API accepts 'second' not 'milli second'
		}
		catch (NullPointerException e)
		{
			duration = -1;
		}
		finally
		{
			mMeta.release();
		}
		
		if (duration > 0)
		{
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
			
			// send notification
			sendNotification(context);
		}
	}
}
