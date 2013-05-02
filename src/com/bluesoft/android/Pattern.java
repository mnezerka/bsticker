package com.bluesoft.android;

import java.lang.Math;

public class Pattern 
{
	public static final int MAX_SIZE = 64;
	public static final int DEFAULT_SIZE = 4;

	private String mName = new String("Ptn");
	private int mSize = DEFAULT_SIZE;
	private boolean[] mBeats = new boolean[MAX_SIZE];
	private int mResolution = 4;

	public Pattern()
	{
		setSize(DEFAULT_SIZE);
		for (int i = 0; i < MAX_SIZE; i = i + 1)
			mBeats[i] = false;
	}

	public int getSize()
	{
		return mSize; 
	}

	public void setSize(int size)
	{
		mSize = Math.min(MAX_SIZE, size);
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
}
