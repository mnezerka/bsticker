package com.bluesoft.android;

import java.util.ArrayList;

public class Song
{
	public static final int MAX_SIZE = 10;
	private ArrayList<Pattern> mPatterns = new ArrayList<Pattern>();

	public Song()
	{
		// add one pattern as default song content
		addPattern();
	}

	public int getSize()
	{
		return mPatterns.size();
	}

	public Pattern getPattern(int pos)
	{
		if (pos < 0 || pos > mPatterns.size())
			return null;
		return mPatterns.get(pos);
	}

	public Pattern addPattern()
	{
		Pattern p = new Pattern();
		mPatterns.add(p);
		return p;
	}

	int getResolution()
	{
		int result = 1; 
		for (int i = 0; i < mPatterns.size(); i = i + 1)
			if (mPatterns.get(i).getResolution() > result)
				result = mPatterns.get(i).getResolution();
		return result;
	}
}
