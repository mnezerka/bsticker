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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;
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
	private static final int MAX_TEMPO = 200;
	private static final String KEY_TEMPO = "METRONOME_TEMPO";
	private static final String PREFS = "bsticker.prefs";
	private static final int DEFAULT_TEMPO = 60;
	public static final int MAX_PATTERNS = 10;
	private static final int TIME_ATOM = 50;
	private static final int AUDIO_BUFFER_SIZE = 4096;
	private static final String fileName = "current.xml";
	private static final String PRESET_PREFIX = "preset_";
	private static final String PRESET_POSTFIX = ".xml";
	private static final String BPM = " bpm";

	private ArrayList<PatternView> mPatterns = new ArrayList<PatternView>();
	private Timer mTimer = new Timer();
	private int mCurrentPos = 0;
	private int mCurrentPattern = 0;
	private int mCurrentRes = 0;
	private int mTotalLength = 0;
	private final Handler mHandler = new Handler();
	private int mTick;
	private long mLastTapTempoClick; 
	private TimerThread mTimerThread;
	private AudioTrack mAudioTrack = null;
	private byte[] mAudioTickBuffer = null; 
	private int mAudioTickBufferSize = 0;
	private AssetManager mAssetManager;
	float mVolume;
	boolean mRunning = false;
	ToggleButton mStartStopButton;
	SeekBar mSeekBar;
	TextView tempoVal;
	Button mPlus;
	Button mMinus;
	Button mTapTempo;
	PowerManager.WakeLock mWakeLock;
	ArrayList<PatternView> patternViews;
	
	private int mTempo = DEFAULT_TEMPO;

	private void restart()
	{
		mSeekBar.setProgress(mTempo);
		tempoVal.setText("" + mTempo + BPM);
		mMinus.setClickable(mTempo > 0);
		mPlus.setClickable(mTempo < MAX_TEMPO);
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

		mStartStopButton = (ToggleButton)findViewById(R.id.startstop);
	       
		mSeekBar = (SeekBar) findViewById(R.id.tempo);
		mSeekBar.setMax(MAX_TEMPO + 1);
		tempoVal = (TextView) findViewById(R.id.tempoval);
		mMinus = (Button) findViewById(R.id.minus);
		mPlus = (Button) findViewById(R.id.plus);
		mTapTempo = (Button) findViewById(R.id.taptempo);
		mLastTapTempoClick = 0;

		SharedPreferences settings = getSharedPreferences(PREFS, 0);
		mTempo = settings.getInt(KEY_TEMPO, DEFAULT_TEMPO);

		//bindPeriodButtons();
        
		mMinus.setOnClickListener(new Button.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (mTempo > 1) 
					{
						--mTempo;
						restart();
					}
				}
			});

		mPlus.setOnClickListener(new Button.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (mTempo < MAX_TEMPO)
					{
						++mTempo;
						restart();
					}
				}
			});
        
		mTapTempo.setOnClickListener(new Button.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					long timeClick = new Date().getTime(); 
					long diff = timeClick - mLastTapTempoClick;
					if (diff > 0)
					{
						long guessBpm = 60000 / diff;
						Log.d("BSTicker", "Tap click at " + timeClick + " last click at " + mLastTapTempoClick + " bpm guess is " + guessBpm);
						if (guessBpm > 0 && guessBpm < MAX_TEMPO)
						{
							mTempo = (int)guessBpm;	
							Log.d("BSTicker", "Setting new tempo to " + guessBpm);
							restart();
						}
					}
					mLastTapTempoClick = timeClick;
				}
			});
     
		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        		{
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch)
				{
					mTempo = progress;
					tempoVal.setText("" + mTempo + BPM);
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

    
		/*
		mStartStopButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View view)
			{
				changeState();
			}
		});
		*/

		mStartStopButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				if (isChecked)
				{
					changeState();
					// The toggle is enabled
				} else {
					// The toggle is disabled
					changeState();
        		}
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

			case R.id.load:
				DialogFragment dialogLoad = new DialogLoad();
				dialogLoad.show(getSupportFragmentManager(), "DialogLoad");
				return true;

			case R.id.save:
				DialogFragment dialogSave= new DialogSave();
				dialogSave.show(getSupportFragmentManager(), "DialogSave");
				return true;

			case R.id.about:
				DialogAbout about = new DialogAbout(this);
				about.setTitle("about this app");
				about.show();
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
				Log.d("BSTicker", "Remove existing item ");
				removePattern(info.getPatternView());
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
			dialog.mPatternView.invalidate();
		}
	}

    public void onFinishDialogSave(DialogSave dialog)
	{
		//Toast.makeText(this, "Hi, " + inputText, Toast.LENGTH_SHORT).show();
		Log.d("BSTicker", "Dialog save successfully closed with preset name " + dialog.mPresetName);
		save(PRESET_PREFIX + dialog.mPresetName + PRESET_POSTFIX);
	}

    public void onFinishDialogLoad(DialogLoad dialog)
	{
		//Toast.makeText(this, "Hi, " + inputText, Toast.LENGTH_SHORT).show();
		Log.d("BSTicker", "Dialog load successfully closed with preset name " + dialog.mPresetName);
		load(PRESET_PREFIX + dialog.mPresetName + PRESET_POSTFIX);
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
			String[] sizeItems = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
				"11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
				"21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32" };

			View v = inflater.inflate(R.layout.dialog_pattern, container, false);

			// Size Spinner
			final Spinner ctrlSize = (Spinner)v.findViewById(R.id.size);
			//ctrlSize.setText(Integer.toString(mSize));
			ArrayAdapter<String> sizeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, sizeItems);
			// Specify the layout to use when the list of choices appears
			sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			ctrlSize.setAdapter(sizeAdapter);
			ctrlSize.setSelection(mSize - 1);

			// Resolution Spinner
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
					// update pattern and exit
					mSize = ctrlSize.getSelectedItemPosition() + 1;
					mResolution = spinner.getSelectedItemPosition();
					BSTicker activity = (BSTicker) getActivity();
					activity.onFinishEditPatternDialog(PatternDialogFragment.this);
					PatternDialogFragment.this.getDialog().dismiss();
				}
				//Toast.makeText(getActivity(), "Size must number > 0", Toast.LENGTH_LONG).show();
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
			//mStartStopButton.setText(R.string.stop);

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
			//mStartStopButton.setText(R.string.start);
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

	// add pattern to the current song (at the end)
	public void addPattern(PatternView pv)
	{
		mPatterns.add(pv);

		LinearLayout ll = (LinearLayout)findViewById(R.id.songlayout);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		ll.addView(pv, lp);

		afterSongContentModification();

		registerForContextMenu(pv);
	}

	public void removePattern(PatternView pv)
	{
		LinearLayout ll = (LinearLayout)findViewById(R.id.songlayout);
		ll.removeView(pv);

		mPatterns.remove(pv);

		afterSongContentModification();
	}

	private void afterSongContentModification()
	{
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
		save(fileName);
	}

	public void save(String filePath)
	{
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

			FileOutputStream fos = openFileOutput(filePath, Context.MODE_PRIVATE);
			XmlSerializer serializer = Xml.newSerializer();
			serializer.setOutput(fos, "UTF-8");
			serializer.startDocument(null, Boolean.valueOf(true));
			//serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			serializer.startTag(null, "song");

			for(int i = 0 ; i < mPatterns.size() ; i++)
			{
				PatternView pv = mPatterns.get(i);
				String beats = "";
				for (int beatIx = 0; beatIx < pv.getSize(); beatIx++)
					beats += pv.getBeat(beatIx) ? "1" : "0";	
				serializer.startTag(null, "pattern");
				serializer.attribute(null, "resolution", Integer.toString(pv.getResolution()));
				serializer.attribute(null, "size", Integer.toString(pv.getSize()));
				serializer.attribute(null, "beats", beats);
				serializer.endTag(null, "pattern");
			}
			serializer.endTag(null, "song");
			serializer.endDocument();
			serializer.flush();
			fos.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void load()
	{
		load(fileName);
	}

	public void load(String filePath)
	{
		Log.d("BSTicker", "Loading current from " + filePath); 

		FileInputStream fis = null;
		//fis = openFileInput(fileName);
		try {
			fis = openFileInput(filePath);
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(fis, null);
			//parser.nextTag();
			readXml(parser);
		} catch (Exception e) {
			;
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException e) {
				;	
			}
		}
	}

	private void readXml(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		Log.d("BSTicker", "looking for song tag");
		while (parser.next() != XmlPullParser.END_DOCUMENT)
		{
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String name = parser.getName();
			if (name.equals("song")) {
				Log.d("BSTicker", "xml song found");
				readXmlSong(parser);
				return; // look for first song end exit after parsing it
			} else {
				xmlSkipElement(parser);
			}
		}
	}			

	private void readXmlSong(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		parser.require(XmlPullParser.START_TAG, null, "song");
		while (mPatterns.size() > 0)
		{
			removePattern(mPatterns.get(0));
		}
			
		while (parser.next() != XmlPullParser.END_TAG)
		{
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String name = parser.getName();
			if (name.equals("pattern")) {
				readXmlPattern(parser);
			}	
			else
			{
				xmlSkipElement(parser);
			}
		}
	}

	private void readXmlPattern(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		Log.d("BSTicker", "readXmlPattern");
		parser.require(XmlPullParser.START_TAG, null, "pattern");

		// get pattern params from attributes
		String resStr = parser.getAttributeValue(null, "resolution");
		String sizeStr = parser.getAttributeValue(null, "size");
		String beats = parser.getAttributeValue(null, "beats");

		if (resStr != null && sizeStr != null && beats != null)
		{
			int resInt = Integer.parseInt(resStr);
			int sizeInt = Integer.parseInt(sizeStr);
			if (resInt > 0 && sizeInt > 0)
			{
				PatternView p = new PatternView(this);
				p.setResolution(resInt);
				p.setSize(sizeInt);

				for (int beatIx = 0; beatIx < beats.length(); beatIx++)
					if (beats.charAt(beatIx) == '1')
						p.setBeat(beatIx, true);
				addPattern(p);
			}	
		}
		/*
		PatternView p = new PatternView(this);
		p.setResolution(4);
		p.setSize(4);
		p.setBeat(0, true);
		p.setBeat(2, true);
		addPattern(p);
		*/

		xmlSkipElement(parser);
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
			Log.d("BSTicker", "skipping next item, depth is " + depth);
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

	public ArrayList<String> getPresetNames()
	{
		String[] savedFiles = getApplicationContext().fileList();
		ArrayList<String> presetFiles = new ArrayList<String>();
		for (int i = 0; i < savedFiles.length; i++)
			if (savedFiles[i].startsWith(PRESET_PREFIX) && savedFiles[i].endsWith(PRESET_POSTFIX))
			{
				int postfixPos = savedFiles[i].lastIndexOf(PRESET_POSTFIX);
				if (postfixPos <= 0)
					continue;
				String presetName = savedFiles[i].substring(0, postfixPos);
				presetName = presetName.substring(PRESET_PREFIX.length());
				presetFiles.add(presetName);
			}
		return presetFiles;
	}
}
