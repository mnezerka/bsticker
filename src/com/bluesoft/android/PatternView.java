package com.bluesoft.android;

import android.util.Log;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
//import android.util.AttributeSet;
import android.view.*;

class PatternView extends View
{
	public static final int MAX_SIZE = 64;
	public static final int DEFAULT_SIZE = 4;
	public static final int DEFAULT_RESOLUTION = 4;
	public static final int DEFAULT_TEMPO = 60;

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

	private int mTempo = DEFAULT_TEMPO;
	private int mTimeBeat = 240000 / (mResolution * mTempo);
	private int mTimeTotal = mSize * mTimeBeat;

	public PatternView(Context context)
	{
		super(context);

		setSize(DEFAULT_SIZE);
		for (int i = 0; i < MAX_SIZE; i = i + 1)
			mBeats[i] = false;

		init();
	}

	public int getSize()
	{
		return mSize;
	}

	public void setSize(int size)
	{
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

		/*
		mPiePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPiePaint.setStyle(Paint.Style.FILL);
		mPiePaint.setTextSize(mTextHeight);

		mShadowPaint = new Paint(0);
		mShadowPaint.setColor(0xff101010);
		mShadowPaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));
		*/
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
}



