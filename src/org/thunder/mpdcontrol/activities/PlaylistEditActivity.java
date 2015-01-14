package org.thunder.mpdcontrol.activities;

import java.util.ArrayList;
import java.util.List;

import org.thunder.mpdcontrol.R;
import org.thunder.dragsortlistview.DragSortController;
import org.thunder.dragsortlistview.DragSortListView;
import org.thunder.mpdcontrol.MainApplication;
import org.thunder.mpdcontrol.TabPagerAdapter;
import org.thunder.mpdcontrol.cover.AlbumCoverDownloadListener;
import org.thunder.mpdcontrol.fragments.PlaylistFragment;
import org.thunder.mpdcontrol.helpers.CoverAsyncHelper;
import org.thunder.mpdcontrol.helpers.RadioItem;
import org.thunder.mpdcontrol.helpers.RadioStore;
import org.thunder.mpdcontrol.helpers.SettingsHelper;
import org.thunder.mpdcontrol.models.AbstractPlaylistMusic;
import org.thunder.mpdcontrol.models.Activity;
import org.thunder.mpdcontrol.models.PlaylistSong;
import org.thunder.mpdcontrol.models.PlaylistStream;
import org.thunder.mpdcontrol.mpd.AlbumInfo;
import org.thunder.mpdcontrol.mpd.Music;
import org.thunder.mpdcontrol.mpd.exception.MPDServerException;
import org.thunder.mpdcontrol.tools.Tools;
import org.thunder.mpdcontrol.view.holders.PlayQueueViewHolder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.MultiChoiceModeListener;

public class PlaylistEditActivity extends Activity 
{
	private String playlist = null;
	
	private ArrayList<AbstractPlaylistMusic> songlist = null;
	
	private DragSortListView list = null;
    private DragSortController controller = null;
    private ActionMode actionMode = null;
    
    private RadioStore radioStore = RadioStore.getInstance();
	    
	private class QueueAdapter extends ArrayAdapter 
	{
        public QueueAdapter(Context context, List<?> data, int resource) 
        {
            super(context, resource, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) 
        {
            PlayQueueViewHolder viewHolder = null;
            
            if (convertView == null) 
            {
            	convertView = LayoutInflater.from(getContext()).inflate( R.layout.playlist_item, null);
            	
                viewHolder = new PlayQueueViewHolder();
                viewHolder.artist = (TextView) convertView.findViewById(android.R.id.text2);
                viewHolder.title = (TextView) convertView.findViewById(android.R.id.text1);
                viewHolder.play = (ImageView) convertView.findViewById(R.id.picture);
                viewHolder.cover = (ImageView) convertView.findViewById(R.id.cover);
                viewHolder.coverHelper = new CoverAsyncHelper(app, getSettings());
                
                final int height = viewHolder.cover.getHeight();
                viewHolder.coverHelper.setCoverMaxSize(height == 0 ? 128 : height);
                
                final AlbumCoverDownloadListener acd = new AlbumCoverDownloadListener(getThis(), viewHolder.cover);
                final AlbumCoverDownloadListener oldAcd = (AlbumCoverDownloadListener) viewHolder.cover.getTag(R.id.AlbumCoverDownloadListener);
                if (oldAcd != null) oldAcd.detach();
                
                viewHolder.cover.setTag(R.id.AlbumCoverDownloadListener, acd);
                viewHolder.cover.setTag(R.id.CoverAsyncHelper, viewHolder.coverHelper);
                viewHolder.coverHelper.addCoverDownloadListener(acd);
                
                viewHolder.icon = convertView.findViewById(R.id.icon);
                
                convertView.setTag(viewHolder);
            } 
            else 
            {
                viewHolder = (PlayQueueViewHolder) convertView.getTag();
            }
            
            RadioItem radio = null;

            AbstractPlaylistMusic music = (AbstractPlaylistMusic) getItem(position);
            if (music.isStream())
            {
            	radio = radioStore.findUrl(music.getPlaylistSubLine());
            	viewHolder.title.setText((radio != null) ? radio.getName() : music.getPlayListMainLine());
            	viewHolder.artist.setText(music.getPlaylistSubLine());
            }
            else
            {
            	viewHolder.title.setText(music.getPlayListMainLine());
            	viewHolder.artist.setText(music.getPlaylistSubLine());
            }
            
            viewHolder.icon.setVisibility(View.VISIBLE);
            viewHolder.play.setImageResource(music.getCurrentSongIconRefID());

            if (music.isForceCoverRefresh() || viewHolder.cover.getTag() == null || !viewHolder.cover.getTag().equals(music.getAlbumInfo().getKey())) 
            {
                if (!music.isForceCoverRefresh()) 
                {
                    viewHolder.cover.setImageResource(R.drawable.no_cover_art);
                }
                music.setForceCoverRefresh(false);
                
                if (music.isStream())
                {
                	if (radio == null) radio = radioStore.defaultRadio();
                	viewHolder.coverHelper.downloadCover(new AlbumInfo(RadioItem.TAG, radio.getCover()), false);
                }
                else viewHolder.coverHelper.downloadCover(music.getAlbumInfo(), false);
            }
            
            return convertView;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		playlist = getIntent().getStringExtra("playlist");
		
		TabPagerAdapter adapter = new TabPagerAdapter(this);
		if (!adapter.isTablet()) setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		setContentView(R.layout.activity_playlist_edit);
		setTitle(playlist);
		
		list = (DragSortListView) findViewById(android.R.id.list);
        list.requestFocus();
        
        controller = new DragSortController(list);
        controller.setDragHandleId(R.id.cover);
        controller.setRemoveEnabled(false);
        controller.setSortEnabled(true);
        controller.setDragInitMode(1);
        
        list.setFloatViewManager(controller);
        list.setOnTouchListener(controller);
        list.setDragEnabled(true);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        
        refreshListColorCacheHint();
		
		update();
		
		list.setDropListener(new DragSortListView.DropListener() 
	    {
	        public void drop(int from, int to) 
	        {
	            if (from == to) return;
	            try 
	            {
	                app.oMPDAsyncHelper.oMPD.movePlaylistSong(playlist, from, to);
	                update();
	            } 
	            catch (MPDServerException e) 
	            {
	            }
	        }
	    });
        
        list.setMultiChoiceModeListener(new MultiChoiceModeListener() 
        {
        	@Override
            public boolean onPrepareActionMode(ActionMode mode, android.view.Menu menu) 
        	{
                actionMode = mode;
                controller.setSortEnabled(false);
                return false;
            }
        	
        	@Override
            public void onDestroyActionMode(ActionMode mode) 
        	{
                actionMode = null;
                controller.setSortEnabled(true);
            }
        	
        	@Override
            public boolean onCreateActionMode(ActionMode mode, android.view.Menu menu) 
            {
                final android.view.MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.playlist_actionmode, menu);
                return true;
            }
        	
            @Override
            public boolean onActionItemClicked(ActionMode mode, android.view.MenuItem item) 
            {
                final SparseBooleanArray checkedItems = list.getCheckedItemPositions();
                final int count = list.getCount();
                final ListAdapter adapter = list.getAdapter();
                int j = 0;
                final int positions[];

                switch (item.getItemId()) 
                {
                    case R.id.menu_delete:
                        positions = new int[list.getCheckedItemCount()];
                        for (int i = 0; i < count && j < positions.length; i++) 
                        {
                            if (checkedItems.get(i)) 
                            {
                                positions[j] = ((AbstractPlaylistMusic) adapter.getItem(i)) .getSongId();
                                j++;
                            }
                        }
                        
                        mode.finish();
                        
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() 
						{
						    @Override
						    public void onClick(DialogInterface dialog, int which) 
						    {
						        switch (which)
						        {
							        case DialogInterface.BUTTON_POSITIVE:
							        	app.oMPDAsyncHelper.execAsync(new Runnable() 
										{
											@Override
											public void run() 
											{
												try 
												{
													for (int i = positions.length-1; i >= 0; i--) 
							                        {
														app.oMPDAsyncHelper.oMPD.removeFromPlaylist(playlist, positions[i]);
													}
												} 
												catch (MPDServerException e) 
												{
													e.printStackTrace();
												}
												
												getThis().runOnUiThread(new Runnable() 
									            {
									                public void run() 
									                {
									                	update();
									                }
									            });
											}
										});
							            break;
						        }
						    }
						};
						
						String text = (list.getCheckedItemCount() == 1) ? getResources().getString(R.string.playlist_confirm_delete_song) : getResources().getString(R.string.playlist_confirm_delete_songs);
						
						AlertDialog.Builder ad = new AlertDialog.Builder(getThis());
						ad.setCancelable(false);
						ad.setMessage(text)
							.setPositiveButton(getResources().getString(R.string.dialog_yes), dialogClickListener)
							.setNegativeButton(getResources().getString(R.string.dialog_no), dialogClickListener)
							.show();

                        return true;
      
                    default:
                        return false;
                }
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) 
            {
                final int selectCount = list.getCheckedItemCount();
                if (selectCount == 0) mode.finish();
                if (selectCount == 1)  mode.setTitle(R.string.playlist_song_selected);
                else mode.setTitle(getString(R.string.playlist_songs_selected, selectCount));
            }
            
        });
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	

    ///////////////////////////////////////////////////////////////////////////
    
    private void refreshListColorCacheHint() 
    {
        if (list == null) return;
        list.setCacheColorHint(getResources().getColor(android.R.color.holo_blue_dark));
    }

    ///////////////////////////////////////////////////////////////////////////

    protected void update() 
    {
        try 
        {
            final int firstVisibleElementIndex = list.getFirstVisiblePosition();
            View firstVisibleItem = list.getChildAt(0);
            final int firstVisiblePosition = (firstVisibleItem != null) ? firstVisibleItem.getTop() : 0;

            List<Music> musics = app.oMPDAsyncHelper.oMPD.getPlaylistSongs(playlist);
            final ArrayList<AbstractPlaylistMusic> newSonglist = new ArrayList<AbstractPlaylistMusic>();
            for (Music m : new ArrayList<Music>(musics)) 
            {
                if (m == null)  continue;
                AbstractPlaylistMusic item = m.isStream() ? new PlaylistStream(m) : new PlaylistSong(m);
                item.setCurrentSongIconRefID(0);
                newSonglist.add(item);
            }

            ArrayAdapter songs = new QueueAdapter(this, newSonglist, R.layout.playlist_item);
            list.setAdapter(songs);
            songlist = newSonglist;
            songs.notifyDataSetChanged();
                    
            setListViewFastScrool(list, newSonglist.size() >= SettingsHelper.MIN_ITEMS_BEFORE_FASTSCROLL);
                    
            if (actionMode != null) actionMode.finish();

             if (firstVisibleElementIndex != 0 && firstVisiblePosition != 0) 
            {
                list.setSelectionFromTop(firstVisibleElementIndex, firstVisiblePosition);
            } 
            else 
            {
      
            }
        } 
        catch (MPDServerException e) 
        {
            Log.e(MainApplication.TAG, PlaylistFragment.class.getSimpleName() + ": Playlist update failure : " + e);
        }
    }
    
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
    	super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.playlist_edit, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
		switch (item.getItemId()) 
		{
			case R.id.activity_playlist_add:
				displayAddDialog();
	            return true;
	            
			case android.R.id.home:
	            finish();
	            return true;
	         
			default:
                return super.onOptionsItemSelected(item);
                
		}
    }
 
    ///////////////////////////////////////////////////////////////////////////
    
    public void displayAddDialog()
    {
		final String[] entries = new String[radioStore.getCount()];
		final boolean[] entriesValues = new boolean[radioStore.getCount()];
		for (int i = 0; i < radioStore.getCount(); i++)
		{
			RadioItem r = radioStore.getAt(i);
			entries[i] = r.getName();
			entriesValues[i] = false;;

		}
		
		final Activity theThis = this;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.activity_playlist_addtitle)
        	.setMultiChoiceItems(entries, entriesValues, new DialogInterface.OnMultiChoiceClickListener()
        	{
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) 
				{
					entriesValues[which] = isChecked;
				}
        	})
        	.setPositiveButton(R.string.activity_playlist_add, new DialogInterface.OnClickListener() 
        	{
        		public void onClick(DialogInterface dialog, int whichButton) 
        		{
        			List<Music> tmpList = new ArrayList<Music>();
        			for (int i = 0; i < entries.length; i++)
        			{
        				if (entriesValues[i])
        				{
        					RadioItem r = radioStore.getAt(i);
        					Music m = new Music();
        					m.setFullpath(r.getUrl());
        					tmpList.add(m);
        				}
        			}
        			
        			try 
        			{
        				app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, tmpList);
        				Tools.notifyUser(String.format(getResources().getString(R.string.activity_playlist_added), tmpList.size()), theThis);
        				update();
        			} 
        			catch (MPDServerException e) 
        			{
        				e.printStackTrace();
        			}
        		}
        	})
        	.create();
		builder.show();
    
    }
    	
}
