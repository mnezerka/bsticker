package com.bluesoft.android;

import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.Date;
import java.io.InputStream;
import java.io.IOException;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;
import android.os.Handler;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioFormat;

public class BSTicker extends Activity
{
	private static final int maxTempo = 200;
	private static final String KEY_TEMPO = "METRONOME_TEMPO";
	private static final String PREFS = "bsticker.prefs";
	private static final int DEFAULT_TEMPO = 60;
	public static final int MAX_PATTERNS = 10;
	private static final int TIME_ATOM = 50;
	private static final int AUDIO_BUFFER_SIZE = 4096;

	private ArrayList<PatternView> mPatterns = new ArrayList<PatternView>();
	private Timer mTimer = new Timer();
	private int mCurrentPos = 0;
	private int mCurrentPattern = 0;
	private int mCurrentRes = 0;
	private int mTotalLength = 0;
	private final Handler mHandler = new Handler();
	private int mTick;
	private TimerThread mTimerThread;
	private AudioTrack mAudioTrack = null;
	private byte[] mAudioTickBuffer = null; 
	private int mAudioTickBufferSize = 0;
	private AssetManager mAssetManager;
	float mVolume;
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
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// bind volume keys to MUSIC stream (which is used for metronome ticks)
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MetronomeLock");

		mStartStopButton = (Button)findViewById(R.id.startstop);
	       
		mSeekBar = (SeekBar) findViewById(R.id.tempo);
		mSeekBar.setMax(maxTempo + 1);
		tempoVal = (TextView) findViewById(R.id.tempoval);
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

		mAssetManager = getAssets();

		// load sound files into buffers
		try {
			//FileInputStream fin = new FileInputStream(filepath + "/tick.wav");
			//DataInputStream dis = new DataInputStream(fin);

			String path = "tick.wav";
			AssetFileDescriptor ad = mAssetManager.openFd(path);

			int fileSize = (int)ad.getLength();
			Log.d("BSTicker", "Audio file has " + fileSize + " bytes");

			//byte[] buffer = new byte[AUDIO_BUFFER_SIZE];
			mAudioTickBuffer = new byte[fileSize];
			InputStream audioStream = mAssetManager.open(path);
			// skip wav header
			audioStream.read(mAudioTickBuffer, 0, 0x2C);
			mAudioTickBufferSize = audioStream.read(mAudioTickBuffer, 0, fileSize);
			Log.d("BSTicker", "Read " + mAudioTickBufferSize + " bytes");

			//bytesRead = audioStream.read(buffer, 0, AUDIO_BUFFER_SIZE);
			/*
			mAudioTrack.play();
			int bytesWritten = mAudioTrack.write(buffer, 0, bytesRead);
			Log.d("BSTicker", "Written " + bytesWritten + " bytes");
			mAudioTrack.stop();
			*/

		} catch (IOException e) {
			Log.e("BSTicker", "Cannot play audio: " + e);
		}	

		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 22050, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, AUDIO_BUFFER_SIZE, AudioTrack.MODE_STREAM);


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

		restart();
	}

	@Override
	public void onDestroy()
	{
		if (mRunning)
			changeState();

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.add_pattern:
				PatternView pv = new PatternView(this);
				addPattern(pv);
				return true;
			case R.id.help:
				//showHelp();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, view, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.pattern, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId())
		{
			case R.id.set_size:
				// info.id
				return true;
			case R.id.set_resolution:
				// info.id
				return true;
			case R.id.delete:
				// info.id
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	/** 
	 * TimerThread implementation
	 */
	class TimerThread extends Thread
	{
		private boolean running = false;
		private BSTicker mTicker;
		private long mTimeStart; 
		private long mTimeExpected; 

		@Override public void run()
		{
			mTimeStart = new Date().getTime(); 
			mTimeExpected = mTimeStart; 

			while(running)
			{
				Log.d("BSTicker", "Tick");

				PatternView pv = mPatterns.get(mCurrentPattern);

				// change current pattern if current pattern is at the end (last tick of pattern has been handled (played)
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

				// Play current beat (with check if sounds are loaded already)
				if (pv.getBeat(mCurrentPos) && mAudioTickBufferSize > 0)
				{
					Log.d("BSTicker", "Playing beat: cPos:" + mCurrentPos);
					mAudioTrack.play();
					int bytesWritten = mAudioTrack.write(mAudioTickBuffer, 0, mAudioTickBufferSize);
					Log.d("BSTicker", "Written " + bytesWritten + " bytes");
					mAudioTrack.stop();
				}

				mHandler.post(new Runnable() {
					public void run() {
						updateOnBeat();
					}
				});
	
				// wait for one beat 
				int sleepLen = pv.getTimeBeat(mTempo);

				long currentTime = new Date().getTime();
				long timeDiff = currentTime - mTimeExpected;
				Log.d("BSTicker", "Time diff: " + timeDiff); 
				mTimeExpected += sleepLen;
				sleepLen -= timeDiff;	
				Log.d("BSTicker", "New sleepLen: " + sleepLen); 

				// TODO:: do time corrections caused by execution
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

		registerForContextMenu(pv);

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
