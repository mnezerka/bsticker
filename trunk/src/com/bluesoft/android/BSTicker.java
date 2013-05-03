package com.bluesoft.android;

import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;

public class BSTicker extends Activity
{
	private static final int maxTempo = 200;
	private static final String KEY_TEMPO = "METRONOME_TEMPO";
	private static final String PREFS = "bsticker.prefs";
	private static final int DEFAULT_TEMPO = 60;
	
	private Timer mTimer = new Timer();
	private TimerTask mTimerTask;
	private int mCounter = 0;
	private final Handler mHandler = new Handler();
	private int mSoundId;
	private SoundPool mSoundPool;
	boolean mSoundLoaded = false;
	float mVolume;
	private Song mSong = new Song();
	boolean mRunning = false;
	Button mStartStopButton;
	SeekBar mSeekBar;
	TextView tempoVal;
	Button mPlus;
	Button mMinus;
	PowerManager.WakeLock mWakeLock;
	ArrayList<PatternView> patternViews;
	
	private int mTempo = DEFAULT_TEMPO;

	private void updateSongGui()
	{
		LinearLayout ll = (LinearLayout)findViewById(R.id.songlayout);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

		for (int ptnIx = 0; ptnIx < mSong.getSize(); ptnIx = ptnIx + 1)
		{
			Pattern ptn = mSong.getPattern(ptnIx);

			PatternView pv = new PatternView(this, ptn);
			ll.addView(pv, lp);

			patternViews.add(pv);

			/*
			TextView ptnLabel = new TextView(this);
			ptnLabel.setText("Ptn" + ptnIx);
			ll.addView(ptnLabel, lp);

			for (int beatIx = 0; beatIx < ptn.getSize(); beatIx = beatIx + 1)
			{
				Button myButton = new Button(this);
				myButton.setText("B" + beatIx);
				ll.addView(myButton, lp);
			}
			*/
		}

		//PatternView pv = new PatternView(this, 4);
		//ll.addView(pv, lp);
	}

	private void restart()
	{
		mSeekBar.setProgress(mTempo);
		tempoVal.setText("" + mTempo);
		mMinus.setClickable(mTempo > 0);
		mPlus.setClickable(mTempo < maxTempo);

	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		//Log.v("Metronome", "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// bind volume keys to MUSIC stream (which is used for metronome ticks)
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MetronomeLock");

		mStartStopButton = (Button)findViewById(R.id.startstop);
	       
		mSeekBar = (SeekBar) findViewById(R.id.tempo);
		mSeekBar.setMax(maxTempo + 1);
		tempoVal = (TextView) findViewById(R.id.text);
		mMinus = (Button) findViewById(R.id.minus);
		mPlus = (Button) findViewById(R.id.plus);
		//mPeriodLabel = (TextView) findViewById(R.id.period);
        
		SharedPreferences settings = getSharedPreferences(PREFS, 0);
		mTempo = settings.getInt(KEY_TEMPO, DEFAULT_TEMPO);

		//bindPeriodButtons();
        
		mMinus.setOnClickListener(new Button.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (mTempo > 1) --mTempo;
					restart();
				}
			});

		mPlus.setOnClickListener(new Button.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (mTempo < maxTempo) ++mTempo;
					restart();
				}
			});
        
      
		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        		{
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch)
				{
					mTempo = progress;
					tempoVal.setText("" + mTempo);
						// TODO Auto-generated method stub
						
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar)
				{
							
					mTempo = seekBar.getProgress();
					restart();
				}


				@Override
				public void onStartTrackingTouch(SeekBar seekBar)
				{
					// TODO Auto-generated method stub
				}
        			
		});
        
		mStartStopButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				changeState();
			}
		});

		mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener()
		{
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status)
			{
				mSoundLoaded = true;
			}
		});
		mSoundId = mSoundPool.load(this, R.raw.tick, 1);	

		// Getting the user sound settings
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mVolume = actualVolume / maxVolume;

		patternViews = new ArrayList<PatternView>();


		updateSongGui();
    
		restart();
	}

	@Override
	public void onDestroy()
	{
		if (mRunning)
			changeState();

		// release sound pool resources
		mSoundPool.release();
		mSoundPool = null;
	    
		super.onDestroy();
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();

		// stop metronome if it is running
		if (mRunning)
			changeState();

		SharedPreferences settings = getSharedPreferences(PREFS, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(KEY_TEMPO, mTempo);
		editor.commit();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		// stop metronome if it is running
		if (mRunning)
			changeState();	
	}
	
    
	private void changeState()
	{
		mRunning = !mRunning;

		if (mRunning)
		{
			mWakeLock.acquire();
			mStartStopButton.setText(R.string.stop);
			mTimerTask = new TimerTask() {
				public void run() {
					mHandler.post(new Runnable() {
						public void run() {
							mCounter++;
				
						}
					});
				}
			};

			int res = mSong.getResolution();

			// min lenghth interval in sec = (60s * 4) / (BPM * RES) 
			// 60s is used because BPM is related to minute
			// multiplication by 4 is used because of BPM is related to lenght of quoter note
			int tickInterval = 240000 / (res * mTempo);
			Log.e("Run", "res is " + res + ", tickInterval is " + tickInterval + ", mTempo is " + mTempo);

			mTimer.schedule(mTimerTask, 0, tickInterval);

		} else {
			mWakeLock.release();
			mTimerTask.cancel();
			mStartStopButton.setText(R.string.start);
		}
	}

	public void playSound()
	{
 		// Is the sound loaded already?
		if (mSoundLoaded)
		{
			tempoVal.setText("Timer: " + mCounter);
			mSoundPool.play(mSoundId, mVolume, mVolume, 1, 0, 1f);
			Log.e("Test", "Played sound");
		}
		
		// colorize beat 
	}
    
}
