package org.thunder.mpdcontrol.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ExpandableListView;

public class AlbumExpandableListView extends ExpandableListView 
{

	public AlbumExpandableListView(Context context) 
	{
		super(context);
	}

	public AlbumExpandableListView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
	}

	public AlbumExpandableListView(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);
	}

	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
    {
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(2000, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
	
}
