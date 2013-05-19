package com.bluesoft.android;

import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import org.xmlpull.v1.XmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Spinner;
import android.widget.Toast;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;


public class BSTicker extends FragmentActivity
{
	/*
	public interface PatternDialogListener
	{
			void onFinishEditDialog(PatternView pattern);
	}
	*/
	private static final int maxTempo = 200;
	private static final String KEY_TEMPO = "METRONOME_TEMPO";
	private static final String PREFS = "bsticker.prefs";
	private static final int DEFAULT_TEMPO = 60;
	public static final int MAX_PATTERNS = 10;
	private static final int TIME_ATOM = 50;
	private static final int AUDIO_BUFFER_SIZE = 4096;
	private static final String fileName = "current.xml";

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

		load();

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

		save();
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
				// create an instance of the dialog fragment and show it
				DialogFragment dialog = new PatternDialogFragment(4, 4, null);
				dialog.show(getSupportFragmentManager(), "PatternDialogFragment");
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
		//AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		PatternView.PatternContextMenuInfo info = (PatternView.PatternContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId())
		{
			case R.id.edit:
				Log.d("BSTicker", "Editing existing item ");
				DialogFragment dialog = new PatternDialogFragment(
					info.getPatternView().getSize(),
					info.getPatternView().getResolution(),
					info.getPatternView());
				dialog.show(getSupportFragmentManager(), "PatternDialogFragment");
				return true;
			case R.id.delete:
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

    public void onFinishEditPatternDialog(PatternDialogFragment dialog)
	{
		//Toast.makeText(this, "Hi, " + inputText, Toast.LENGTH_SHORT).show();
		Log.d("BSTicker", "Dialog successfully closed");
		if (dialog.mPatternView == null)
		{
			// create new pattern
			Log.d("BSTicker", "Creating new item ");
			PatternView p = new PatternView(this);
			p.setSize(dialog.mSize);
			p.setResolution(dialog.mResolution + 1);
			addPattern(p);
		}
		else
		{
			Log.d("BSTicker", "Updating existing item");
			// update existing pattern
			dialog.mPatternView.setSize(dialog.mSize);
			dialog.mPatternView.setResolution(dialog.mResolution + 1);
		}
	}

	public class PatternDialogFragment extends DialogFragment
	{
		//PatternView mPattern;
		int mSize;
		int mResolution;
		PatternView mPatternView;
		
		public PatternDialogFragment(int size, int resolution, PatternView patternView)
		{
			super();

			mSize = size;
			mResolution = resolution;
			mPatternView = patternView;
		}

		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			View v = inflater.inflate(R.layout.dialog_pattern, container, false);
			final EditText ctrlSize = (EditText)v.findViewById(R.id.size);
			ctrlSize.setText(Integer.toString(mSize));
			final Spinner spinner = (Spinner)v.findViewById(R.id.resolution);
			// Create an ArrayAdapter using the string array and a default spinner layout
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.pattern_resolutions, android.R.layout.simple_spinner_item);
			// Specify the layout to use when the list of choices appears
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			//adapter.setSelection(2);
			// Apply the adapter to the spinner
			spinner.setAdapter(adapter);
			spinner.setSelection(mResolution - 1);
	        // watch for button clicks.
			final Button btnOk = (Button)v.findViewById(R.id.dialogButtonOK);
			btnOk.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
   					//((FragmentDialog)getActivity()).showDialog();
					if (ctrlSize.getText().toString().length() > 0 )
					{
						int size = 0;
						try {
							size = Integer.parseInt(ctrlSize.getText().toString());
							Log.d("BSTicker", "size is " + size);
						} catch (NumberFormatException e) {
							Log.d("BSTicker", "cannot parse int size");
							size = 0;
						}

						// if all values are ok
						if (size > 0 && size < PatternView.MAX_SIZE)
						{
							// update pattern and exit
							mSize = size;
							mResolution = spinner.getSelectedItemPosition();
							BSTicker activity = (BSTicker) getActivity();
							activity.onFinishEditPatternDialog(PatternDialogFragment.this);
							PatternDialogFragment.this.getDialog().dismiss();
						}
						else
						{
							Toast.makeText(getActivity(), "Size must number > 0", Toast.LENGTH_LONG).show();
						}
					}
					else
					{
						Toast.makeText(getActivity(), "Size must set", Toast.LENGTH_LONG).show();

					}
				}
			});

	        // Watch for cancel button clicks.
			final Button btnCancel = (Button)v.findViewById(R.id.dialogButtonCancel);
			btnCancel.setOnClickListener(new OnClickListener() { public void onClick(View v) { PatternDialogFragment.this.getDialog().cancel(); } });

			return v;
		}
	}

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
		//tempoVal.setText("Pos:" + mCurrentPos);
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


	/*
	public boolean isExternalStorageAvailabe()
	{
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state))
		{
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
		{
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but all we need
			//  to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}

		return mExternalStorageWriteable;
	}
	*/

	public void save()
	{
		String filePath = fileName;

		/*
		if (isExternalStorageAvailable)
		{
			String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
			// Not sure if the / is on the path or not
			filePath = baseDir + File.separator + fileName;
		}	
		*/

		Log.d("BSTicker", "Saving current state to " + filePath); 

		try {

			FileOutputStream fos = openFileOutput(fileName, Context.MODE_APPEND);
			XmlSerializer serializer = Xml.newSerializer();
			serializer.setOutput(fos, "UTF-8");
			serializer.startDocument(null, Boolean.valueOf(true));
			//serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			serializer.startTag(null, "state");

			for(int i = 0 ; i < mPatterns.size() ; i++)
			{
				serializer.startTag(null, "pattern");
				serializer.endTag(null, "pattern");
			}
			serializer.endTag(null, "state");
			serializer.endDocument();
			serializer.flush();
			fos.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void load()
	{
		// check if file exists
		String filePath = fileName;

		Log.d("BSTicker", "Loading current from " + filePath); 

		FileInputStream fis = null;
		//fis = openFileInput(fileName);
		try {
			fis = openFileInput(fileName);
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(fis, null);
			parser.nextTag();
			//return readXml(parser)
			readXml(parser);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException e) {
				;	
			}
		}
		/*
		public List parse(InputStream in) throws XmlPullParserException, IOException {
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);
			parser.nextTag();
			return readFeed(parser);
		} finally {
			in.close();
		}
		*/
	}

	private void readXml(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		parser.require(XmlPullParser.START_TAG, null, "state");
		while (parser.next() != XmlPullParser.END_TAG)
		{
			if (parser.getEventType() != XmlPullParser.START_TAG)
			{
				continue;
			}

			String name = parser.getName();
			// Starts by looking for the entry tag
			if (name.equals("pattern")) {
				Log.d("BSTicker", "xml-pattern");
				//entries.add(readEntry(parser));
			} else {
				xmlSkipElement(parser);
			}
		}  
		//return entries;

	}			

	private void xmlSkipElement(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		if (parser.getEventType() != XmlPullParser.START_TAG)
		{
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0)
		{
			switch (parser.next())
			{
				case XmlPullParser.END_TAG:
					depth--;
					break;
				case XmlPullParser.START_TAG:
					depth++;
					break;
			}
		}
	}
}
