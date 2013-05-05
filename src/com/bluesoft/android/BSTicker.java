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
	public static final int MAX_PATTERNS = 10;
	private static final int TIME_ATOM = 50;

	private ArrayList<PatternView> mPatterns = new ArrayList<PatternView>();
	private Timer mTimer = new Timer();
	private int mCurrentPos = 0;
	private int mCurrentPattern = 0;
	private int mCurrentRes = 0;
	private int mTotalLength = 0;
	private final Handler mHandler = new Handler();
	private int mSoundId;
	private int mTick;
	private TimerThread mTimerThread;
	private SoundPool mSoundPool;
	boolean mSoundLoaded = false;
	float mVolume;
	//private Song mSong = new Song();
	boolean mRunning = false;
	Button mStartStopButton;
	SeekBar mSeekBar;
	TextView tempoVal;
	Button mPlus;
	Button mMinus;
	PowerManager.WakeLock mWakeLock;
	ArrayList<PatternView> patternViews;
	
	private int mTempo = DEFAULT_TEMPO;

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

		mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
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

		PatternView p1 = new PatternView(this);
		p1.setResolution(8);
		p1.setSize(8);
		p1.setBeat(0, true);
		p1.setBeat(2, true);
		p1.setBeat(4, true);
		p1.setBeat(6, true);
		addPattern(p1);

		PatternView p2 = new PatternView(this);
		p2.setBeat(0, true);
		p2.setBeat(2, true);
		addPattern(p2);

		PatternView p3 = new PatternView(this);
		p3.setResolution(3);
		p3.setSize(3);
		p3.setBeat(0, true);
		addPattern(p3);

		PatternView p4 = new PatternView(this);
		p4.setResolution(12);
		p4.setSize(9);
		p4.setBeat(0, true);
		p4.setBeat(2, true);
		p4.setBeat(3, true);
		p4.setBeat(5, true);
		p4.setBeat(6, true);
		p4.setBeat(8, true);
		addPattern(p4);

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

	class TimerThread extends Thread
	{
		private boolean running = false;
		private BSTicker mTicker;

		@Override public void run()
		{
			while(running)
			{
				Log.d("BSTicker", "Tick");

				PatternView pv = mPatterns.get(mCurrentPattern);



				// cancel current timer if current pattern is at the end (last tick of pattern has been handled (played)
				if (mCurrentPos >= pv.getSize())
				{
					// go to next pattern
					mCurrentPattern++;
					if (mCurrentPattern >= mPatterns.size())
						mCurrentPattern = 0;
					pv = mPatterns.get(mCurrentPattern);

					// start from zero position of new pattern
					mCurrentPos = 0;
				}

				Log.d("BSTicker", "Do something cPos" + mCurrentPos);
				mHandler.post(new Runnable() {
					public void run() {
						updateOnBeat();
					}
				});

	
				// wait for one beat 
				int sleepLen = pv.getTimeBeat(mTempo);
				try { sleep(sleepLen); } catch (InterruptedException e) { e.printStackTrace(); }

				mCurrentPos++;

			}
		}

		void setRunning(boolean b)
		{
			running = b;
		}
	}
    
	private void changeState()
	{
		mRunning = !mRunning;

		if (mRunning)
		{
			mWakeLock.acquire();
			mStartStopButton.setText(R.string.stop);

			mCurrentPattern = 0;
			mCurrentPos = 0;
			//mTick = 0;
			//mCurrentRes = getResolution();

			// set position on first beat of first pattern
			PatternView pv = mPatterns.get(mCurrentPattern);
			pv.setPos(0);	

			// min lenghth interval in sec = (60s * 4) / (BPM * RES) 
			// 60s is used because BPM is related to minute
			// multiplication by 4 is used because of BPM is related to lenght of quoter note
			//int tickInterval = 240000 / (mCurrentRes * mTempo);
			//Log.e("BSTicker", "current res:" + mCurrentRes + ", tickInterval is " + tickInterval + ", mTempo is " + mTempo);
			//int tickInterval = 240000 / (pv.getResolution() * mTempo);
			//Log.d("BSTicker", "Starting timer: tLen:" + mTotalLength + " tickInterval:" + tickInterval + " ticks:" + pv.getSize() + " mTempo:" + mTempo);

			mTimerThread = new TimerThread();
			mTimerThread.setRunning(true);
			mTimerThread.start();

			//mTimerTask = new TickTimerTask();
			//mTimer.scheduleAtFixedRate(mTimerTask, 0, TIME_ATOM);

		} else {
			mWakeLock.release();
			//mTimerTask.cancel();
			mTimerThread.setRunning(false);
			mTimerThread = null;
			mStartStopButton.setText(R.string.start);
		}
	}

	public void updateOnBeat()
	{
		// get current pattern - pattern that is played
		PatternView pv = mPatterns.get(mCurrentPattern);

		// Play current beat (with check if sounds are loaded already)
		if (pv.getBeat(mCurrentPos) && mSoundLoaded)
		{
			Log.d("BSTicker", "Play beat: cPos:" + mCurrentPos);
			mSoundPool.play(mSoundId, mVolume, mVolume, 1, 0, 1f);
		}

		// if previous pattern is different than current
		int prevPatternIx = mCurrentPattern > 0 ? mCurrentPattern - 1 : mPatterns.size() - 1; 
		//Log.i("BSTicker", "current pattern ix:" + mCurrentPattern + " prev pattern ix:" + prevPatternIx);
		if (prevPatternIx != mCurrentPattern)
		{
			// if position in previous pattern is valid (>= 0) 
			if (mPatterns.get(prevPatternIx).getPos() >= 0)
				mPatterns.get(prevPatternIx).setPos(-1);
		}
		pv.setPos(mCurrentPos);
		tempoVal.setText("Pos:" + mCurrentPos);
	}

	public void addPattern(PatternView pv)
	{
		mPatterns.add(pv);

		LinearLayout ll = (LinearLayout)findViewById(R.id.songlayout);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		ll.addView(pv, lp);

		// recalculate size of all patterns
		float totalSize = 0;
		for (int i = 0; i < mPatterns.size(); i++)
		{
			float patternSize = mPatterns.get(i).getSize() / mPatterns.get(i).getResolution();

			if (patternSize > totalSize)
				totalSize = patternSize;
		}

		// set total size for all patterns
		for (int i = 0; i < mPatterns.size(); i++)
			mPatterns.get(i).setTotalSize(totalSize);	
	}

	int getResolution()
	{
		int result = 0; 
		for (int i = 0; i < mPatterns.size(); i++)
			if (mPatterns.get(i).getResolution() > result)
				result = mPatterns.get(i).getResolution();
		return result;
	}
}
