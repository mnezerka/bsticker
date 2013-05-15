package com.bluesoft.android;

import android.util.Log;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
//import android.util.AttributeSet;
import android.view.*;
import android.widget.Toast;
import android.support.v4.view.GestureDetectorCompat;

class PatternView extends View
{
	public static final int MAX_SIZE = 64;
	public static final int DEFAULT_SIZE = 4;
	public static final int DEFAULT_RESOLUTION = 4;
	public static final int DEFAULT_TEMPO = 60;

	private GestureDetectorCompat mDetector;
	private String mName = new String("Ptn");
	private int mSize = DEFAULT_SIZE;
	private float mTotalSize = 1;
	private int mResolution = DEFAULT_RESOLUTION;
	private int mPos = -1;
	private int mTimePos = 0;
	private boolean[] mBeats = new boolean[MAX_SIZE];
	private RectF mBounds;
	private Paint mStrokePaint;
	private Paint mBeatPaint;
	private Paint mBeatActivePaint;
	private Paint mTickPaint;
	private PatternContextMenuInfo mPatternContextMenuInfo;

	private int mTempo = DEFAULT_TEMPO;
	private int mTimeBeat = 240000 / (mResolution * mTempo);
	private int mTimeTotal = mSize * mTimeBeat;

	private boolean mTouchStarted = false;

	public PatternView(Context context)
	{
		super(context);

		mPatternContextMenuInfo = new PatternContextMenuInfo(this);

		setSize(DEFAULT_SIZE);
		for (int i = 0; i < MAX_SIZE; i = i + 1)
			mBeats[i] = false;

		mDetector = new GestureDetectorCompat(getContext(), new MyGestureListener(this));

		init();
	}

	class MyGestureListener extends GestureDetector.SimpleOnGestureListener
	{
		PatternView mPatternView;
		public MyGestureListener(PatternView patternView)
		{
			super();
			mPatternView = patternView;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent event)
		{ 
			//Log.d("BSTicker", "onSingleTapUp: " + event.toString() + "time: " +  (event.getDownTime() - event.getEventTime())); 
			//Log.d("BSTicker", "onSingleTapUp: " + event.toString() + "x: " + event.getX() + " rawx: " + event.getRawX());
			mPatternView.onTap(event);	
			return true;
		}

	}

	public int getSize()
	{
		return mSize;
	}

	public void setSize(int size)
	{
		Log.d("BSTicker", "Size was set to " + size);
		mSize = Math.min(MAX_SIZE, size);
	}

	public void setTotalSize(float size)
	{
		mTotalSize = size;
	}

	public boolean getBeat(int pos)
	{
		pos = Math.min(MAX_SIZE, pos);
		return mBeats[pos];
	}

	public void setBeat(int pos, boolean value)
	{
		Log.d("BSTicker", "setBeat pos: " + pos + " value: " + value);
		pos = Math.min(MAX_SIZE, pos);
		mBeats[pos] = value;
	}

	public String getName()
	{
		return mName;
	}

	public void setName(String name)
	{
		mName = name;
	}

	public int getResolution()
	{
		return mResolution;
	}

	public void setResolution(int resolution)
	{
		mResolution = resolution;
	}

	public void setTempo(int tempo)
	{
		mTempo = tempo;
	}

	public void setPos(int pos)
	{
		// do nothing if new pos is our of range or same as current value
		if (pos >= mSize || pos == mPos)
			return;
		 
		mPos = pos; 

		invalidate();
	}

	public int getPos()
	{
		return mPos;
	}

	public int getTimeLength(int tempo)
	{
		return getTimeBeat(tempo) * mSize; 
	}

	public int getTimeBeat(int tempo)
	{
		return 240000 / (mResolution * tempo);
	}

	private void init()
	{
		mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mStrokePaint.setStyle(Paint.Style.STROKE);
		mStrokePaint.setColor(Color.WHITE);
		mStrokePaint.setStrokeWidth(1);
		
		mBeatPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mBeatPaint.setStyle(Paint.Style.FILL);
		mBeatPaint.setColor(Color.GRAY);

		mBeatActivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mBeatActivePaint.setStyle(Paint.Style.FILL);
		mBeatActivePaint.setColor(Color.GREEN);

		mTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTickPaint.setStyle(Paint.Style.STROKE);
		mTickPaint.setColor(Color.WHITE);
	}

	/*
	public void setShowText(boolean showText)
	{
		mShowText = showText;
		invalidate();
		requestLayout();
	}
	*/

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		float beatWidth = mBounds.width() * (mTotalSize / mResolution);
		RectF beatRect = new RectF(1, 1, beatWidth - 2, mBounds.height() - 2); 

		for (int beatIx = 0; beatIx < getSize(); beatIx = beatIx + 1)
		{
			Paint beatPaint = mBeats[beatIx] ? mBeatActivePaint : mBeatPaint;

			canvas.drawRect(beatRect, beatPaint);

			// draw tick position
			if (beatIx == mPos)
			{
				//canvas.drawLine(beatRect.left, beatRect.top, beatRect.right, beatRect.bottom, mTickPaint);
				//canvas.drawLine(beatRect.right, beatRect.top, beatRect.left, beatRect.bottom, mTickPaint);
				canvas.drawRect(beatRect, mTickPaint);
			}
			beatRect.offset(beatWidth, 0);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		mBounds = new RectF(0, 0, w, h);
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		this.setMeasuredDimension(parentWidth, 40);
	}

	protected void onTap(MotionEvent event)
	{
		Log.d("BSTicker", "On tap occured");

		int beat = (int)event.getX() / (getWidth() / mSize);
		Log.d("BSTicker", "Beat to select/unselect: " + beat);

		setBeat(beat, !getBeat(beat));
		invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		//Log.d("BSTicker", "Touch even" + event);

		mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
	}

	public class PatternContextMenuInfo implements ContextMenu.ContextMenuInfo
	{

		private PatternView mPatternView;

		public PatternContextMenuInfo(PatternView patternView)
		{
			mPatternView = patternView;
		}

		public PatternView getPatternView()
		{
			return mPatternView;
		}
	}

	@Override
	protected ContextMenu.ContextMenuInfo getContextMenuInfo()
	{
		return mPatternContextMenuInfo;
	}
}



