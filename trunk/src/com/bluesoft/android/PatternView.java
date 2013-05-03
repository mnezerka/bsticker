package com.bluesoft.android;

import android.util.Log;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
//import android.util.AttributeSet;
import android.view.*;

class PatternView extends View
{
	private Pattern mPattern;
        private RectF mBounds;
	private float mTextHeight = 0.0f;
	private int mTextColor = 0xff000000;
	private Paint mTextPaint;
	private Paint mStrokePaint;
	private Paint mFillPaint;

	public PatternView(Context context, Pattern pattern)
	{
		super(context);

		mPattern = pattern;

		init();
	}

	private void init()
	{
		mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setColor(mTextColor);
		if (mTextHeight == 0)
		{
			mTextHeight = mTextPaint.getTextSize();
		} else {
			mTextPaint.setTextSize(mTextHeight);
		}

		mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mStrokePaint.setStyle(Paint.Style.STROKE);
		mStrokePaint.setColor(Color.WHITE);
		mStrokePaint.setStrokeWidth(1);
		
		mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mFillPaint.setStyle(Paint.Style.FILL);
		mFillPaint.setColor(Color.GRAY);


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

		Log.i("PatternView", "onDraw");
		Log.i("PatternView", mBounds.toString());

		float beatWidth = mBounds.width() / mPattern.getSize();
		RectF beatRect = new RectF(1, 1, beatWidth - 2, mBounds.height() - 2); 

		for (int beatIx = 0; beatIx < mPattern.getSize(); beatIx = beatIx + 1)
		{
			canvas.drawRect(beatRect, mFillPaint);
			beatRect.offset(beatWidth, 0);
			Log.e("onDraw beat", beatRect.toString());
		}
	}

	@Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		mBounds = new RectF(0, 0, w, h);
		Log.e("PatternView", "onSizeChanged");
		Log.e("PatternView", mBounds.toString());
        }

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		this.setMeasuredDimension(parentWidth, 20);
	}
}



