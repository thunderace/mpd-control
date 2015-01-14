package org.thunder.mpdcontrol.view.holders;

import org.thunder.mpdcontrol.helpers.CoverAsyncHelper;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class PlayQueueViewHolder extends AbstractViewHolder 
{

	public ImageView play;
    public TextView title;
    public TextView artist;
    public ImageView cover;
    public View icon;
    public CoverAsyncHelper coverHelper;

}
