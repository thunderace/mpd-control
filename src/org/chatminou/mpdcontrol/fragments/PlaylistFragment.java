package org.chatminou.mpdcontrol.fragments;

import java.util.ArrayList;
import java.util.List;

import org.chatminou.mpdcontrol.mpd.AlbumInfo;
import org.chatminou.mpdcontrol.mpd.MPD;
import org.chatminou.mpdcontrol.mpd.MPDPlaylist;
import org.chatminou.mpdcontrol.mpd.MPDStatus;
import org.chatminou.mpdcontrol.mpd.Music;
import org.chatminou.mpdcontrol.mpd.event.StatusChangeListener;
import org.chatminou.mpdcontrol.mpd.exception.MPDServerException;
import org.chatminou.dragsortlistview.DragSortController;
import org.chatminou.dragsortlistview.DragSortListView;
import org.chatminou.mpdcontrol.MainApplication;
import org.chatminou.mpdcontrol.R;
import org.chatminou.mpdcontrol.cover.AlbumCoverDownloadListener;
import org.chatminou.mpdcontrol.helpers.CoverAsyncHelper;
import org.chatminou.mpdcontrol.helpers.RadioItem;
import org.chatminou.mpdcontrol.helpers.RadioStore;
import org.chatminou.mpdcontrol.helpers.SettingsHelper;
import org.chatminou.mpdcontrol.models.AbstractPlaylistMusic;
import org.chatminou.mpdcontrol.models.ListFragment;
import org.chatminou.mpdcontrol.models.PlaylistSong;
import org.chatminou.mpdcontrol.models.PlaylistStream;
import org.chatminou.mpdcontrol.tools.Tools;
import org.chatminou.mpdcontrol.view.holders.PlayQueueViewHolder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PlaylistFragment extends ListFragment  implements StatusChangeListener
{
	private View view = null;
	
    private ArrayList<AbstractPlaylistMusic> songlist = null;
    
    private DragSortListView list = null;
    private DragSortController controller = null;
    private ActionMode actionMode = null;
    
    private int lastPlayingID = -1;
    
    private RadioStore radioStore = RadioStore.getInstance();
	    
	private class QueueAdapter extends ArrayAdapter 
	{
        private MainApplication app;
        private SharedPreferences settings;
       
        public QueueAdapter(Context context, List<?> data, int resource) 
        {
            super(context, resource, data);
            app = (MainApplication) activity.getApplication();
            settings = PreferenceManager.getDefaultSharedPreferences(app);
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
                viewHolder.coverHelper = new CoverAsyncHelper(app, settings);
                
                final int height = viewHolder.cover.getHeight();
                viewHolder.coverHelper.setCoverMaxSize(height == 0 ? 128 : height);
                
                final AlbumCoverDownloadListener acd = new AlbumCoverDownloadListener(activity, viewHolder.cover);
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
    public void onActivityCreated(Bundle savedInstanceState) 
    {
        super.onActivityCreated(savedInstanceState);
        refreshListColorCacheHint();
    }
    
    @Override
    public void onStart() 
    {
        super.onStart();
        app.oMPDAsyncHelper.addStatusChangeListener(this);
        
    }

    @Override
    public void onStop() 
    {
        super.onStop();
        app.oMPDAsyncHelper.removeStatusChangeListener(this);
    }
    
    @Override
    public void onResume() 
    {
        super.onResume();
        new Thread(new Runnable() 
        {
            public void run() 
            {
                update();
            }
        }).start();
    }
        
    ///////////////////////////////////////////////////////////////////////////
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
    {
    	setHasOptionsMenu(true);
    	
    	view = inflater.inflate(R.layout.fragment_playlist, container, false);
    	
        list = (DragSortListView) view.findViewById(android.R.id.list);
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
        
        list.setDropListener(new DragSortListView.DropListener() 
	    {
	        public void drop(int from, int to) 
	        {
	            if (from == to) return;
	            AbstractPlaylistMusic itemFrom = songlist.get(from);
	            Integer songID = itemFrom.getSongId();
	            try 
	            {
	                app.oMPDAsyncHelper.oMPD.getPlaylist().move(songID, to);
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
													app.oMPDAsyncHelper.oMPD.getPlaylist().removeById(positions);
												} 
												catch (MPDServerException e) 
												{
													e.printStackTrace();
												}
											}
										});
							            break;
						        }
						    }
						};
						
						String text = (list.getCheckedItemCount() == 1) ? getResources().getString(R.string.playlist_confirm_delete_song) : getResources().getString(R.string.playlist_confirm_delete_songs);
						
						AlertDialog.Builder ad = new AlertDialog.Builder(activity);
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

        return view;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    
    private void refreshListColorCacheHint() 
    {
        if (app == null || list == null) return;
        list.setCacheColorHint(getResources().getColor(android.R.color.holo_blue_dark));
    }
    
    private AbstractPlaylistMusic getPlaylistItemSong(int songID) 
    {
        AbstractPlaylistMusic song = null;
        for (AbstractPlaylistMusic music : songlist) 
        {
            if (music.getSongId() == songID) 
            {
                song = music;
                break;
            }
        }
        return song;
    }
    
    private void refreshPlaylistItemView(final AbstractPlaylistMusic... playlistSongs) 
    {
        new AsyncTask<Void, Void, Void>() 
        {
            @Override
            protected Void doInBackground(Void... voids) 
            {
                return null;
            }

            @Override
            protected void onPostExecute(Void result) 
            {
                int start = list.getFirstVisiblePosition();
                for (int i = start, j = list.getLastVisiblePosition(); i <= j; i++) 
                {
                    AbstractPlaylistMusic playlistMusic = (AbstractPlaylistMusic) list.getAdapter().getItem(i);
                    for (AbstractPlaylistMusic song : playlistSongs) 
                    {
                        if (playlistMusic.getSongId() == song.getSongId()) 
                        {
                            View view = list.getChildAt(i - start);
                            list.getAdapter().getView(i, view, list);
                        }
                    }
                }
            }
        }.execute();
    }
 
    public void scrollToNowPlaying() 
    {
        new AsyncTask<Void, Void, Integer>() 
        {
            @Override
            protected Integer doInBackground(Void... voids) 
            {
                try 
                {
                    MPD mpd = ((MainApplication) activity.getApplication()).oMPDAsyncHelper.oMPD;
                    return mpd.getStatus().getSongPos();
                } catch (MPDServerException e) 
                {
                    Log.e(MainApplication.TAG, PlaylistFragment.class.getSimpleName() + ": Cannot find the current playing song position : " + e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Integer songIndex) 
            {
                if (songIndex != null) 
                {
                	getListView().setSelection(songIndex);
                } 
                else 
                {
                    Log.d(MainApplication.TAG, PlaylistFragment.class.getSimpleName() + ": Missing list item : " + songIndex);
                }
            }
        }.execute();
    }
    
    
    public void update() 
    {
        update(true);
    }

    protected void update(boolean forcePlayingIDRefresh) 
    {
        try 
        {
            final int firstVisibleElementIndex = list.getFirstVisiblePosition();
            View firstVisibleItem = list.getChildAt(0);
            final int firstVisiblePosition = (firstVisibleItem != null) ? firstVisibleItem.getTop() : 0;

            MPDPlaylist playlist = app.oMPDAsyncHelper.oMPD.getPlaylist();
            final ArrayList<AbstractPlaylistMusic> newSonglist = new ArrayList<AbstractPlaylistMusic>();
            List<Music> musics = playlist.getMusicList();
            if (lastPlayingID == -1 || forcePlayingIDRefresh) lastPlayingID = app.oMPDAsyncHelper.oMPD.getStatus().getSongId();
            int listPlayingID = -1;
            for (Music m : new ArrayList<Music>(musics)) 
            {
                if (m == null)  continue;
               
                AbstractPlaylistMusic item = m.isStream() ? new PlaylistStream(m) : new PlaylistSong(m);
                
                if (item.getSongId() == lastPlayingID) 
                {
                    item.setCurrentSongIconRefID(R.drawable.ic_action_play);
                    listPlayingID = newSonglist.size() - 1;
                } 
                else 
                {
                    item.setCurrentSongIconRefID(0);
                }
                newSonglist.add(item);
            }

            final int finalListPlayingID = listPlayingID;

            activity.runOnUiThread(new Runnable() 
            {
                public void run() 
                {
                    final ArrayAdapter songs = new QueueAdapter(activity, newSonglist, R.layout.playlist_item);
                    setListAdapter(songs);
                    songlist = newSonglist;
                    songs.notifyDataSetChanged();
                    
                    setListViewFastScrool(list, newSonglist.size() >= SettingsHelper.MIN_ITEMS_BEFORE_FASTSCROLL);
                    
                    if (actionMode != null) actionMode.finish();

                    // Restore the scroll bar position
                    if (firstVisibleElementIndex != 0 && firstVisiblePosition != 0) 
                    {
                        list.setSelectionFromTop(firstVisibleElementIndex, firstVisiblePosition);
                    } 
                    else 
                    {
                        // Only scroll if there is a valid song to scroll to. 0
                        // is a valid song but does not require scroll anyway.
                        // Also, only scroll if it's the first update. You don't
                        // want your playlist to scroll itself while you are
                        // looking at
                        // other
                        // stuff.
                        if (finalListPlayingID > 0 && getView() != null) 
                        {
                            setSelection(finalListPlayingID);
                        }
                    }
                }
            });

        } 
        catch (MPDServerException e) 
        {
            Log.e(MainApplication.TAG, PlaylistFragment.class.getSimpleName() + ": Playlist update failure : " + e);
        }
    }

    public void updateCover(AlbumInfo albumInfo) 
    {
        List<AbstractPlaylistMusic> musicsToBeUpdated = new ArrayList<AbstractPlaylistMusic>();

        for (AbstractPlaylistMusic playlistMusic : songlist) 
        {
            if (playlistMusic.getAlbumInfo().equals(albumInfo)) 
            {
                playlistMusic.setForceCoverRefresh(true);
                musicsToBeUpdated.add(playlistMusic);
            }
        }
        
        refreshPlaylistItemView(musicsToBeUpdated .toArray(new AbstractPlaylistMusic[musicsToBeUpdated.size()]));
    }
    
    ///////////////////////////////////////////////////////////////////////////
    
    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) 
    {
     }


    @Override
    public void libraryStateChanged(boolean updating)
    {
    }

    @Override
    public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) 
    {
        update();
    }

    @Override
    public void randomChanged(boolean random) 
    {
    }
    
    @Override
    public void repeatChanged(boolean repeating) 
    {
    }
    

    @Override
    public void stateChanged(MPDStatus mpdStatus, String oldState) 
    {
    }
    
    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) 
    {
    }
    
    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) 
    {
        if (songlist != null) 
        {
            for (AbstractPlaylistMusic song : songlist) 
            {
                int newPlay;
                if ((song.getSongId()) == mpdStatus.getSongId()) newPlay = R.drawable.ic_action_play;
                else newPlay = 0;
                if (song.getCurrentSongIconRefID() != newPlay) 
                {
                    song.setCurrentSongIconRefID(newPlay);
                    refreshPlaylistItemView(song);
                }
            }
        }
    }
    
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onListItemClick(final ListView l, View v, final int position, long id) 
    {
        new Thread(new Runnable() 
        {
            @Override
            public void run() 
            {
                MainApplication app = (MainApplication) activity.getApplication();
                final Integer song = ((AbstractPlaylistMusic) l.getAdapter().getItem(position)).getSongId();
                try 
                {
                    app.oMPDAsyncHelper.oMPD.skipToId(song);
                } 
                catch (MPDServerException e) 
                {
                }
            }
        }).start();
    }
    
    ///////////////////////////////////////////////////////////////////////////
        
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        Intent i;
        switch (item.getItemId()) 
        {
            case R.id.playlist_delete_all:
            	
            	if (songlist.size() > 0)
    			{
    				DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() 
    				{
    				    @Override
    				    public void onClick(DialogInterface dialog, int which) 
    				    {
    				        switch (which)
    				        {
    					        case DialogInterface.BUTTON_POSITIVE:
    					        	try 
    								{
    									app.oMPDAsyncHelper.oMPD.getPlaylist().clear();
    									songlist.clear();
    									if (isAdded()) 
    									{
    										Tools.notifyUser(getResources().getString(R.string.playlist_cleared), getActivity());
    									}
    									((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
    								} 
    								catch (MPDServerException e) 
    								{
    									e.printStackTrace();
    								}
    					            break;
    				        }
    				    }
    				};
    				
    				AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
    				ad.setCancelable(false);
    				ad.setMessage(getResources().getString(R.string.playlist_confirm_clear_all))
    					.setPositiveButton(getResources().getString(R.string.dialog_yes), dialogClickListener)
    					.setNegativeButton(getResources().getString(R.string.dialog_no), dialogClickListener)
    					.show();				
    			}
            	
                return true;
                
            case R.id.playlist_save:
            	
            	
            	
            	//TabDialog v = new TabDialog(activity);
            	//v.show();
            	
            	
            	final EditText input = new EditText(activity);
            	new AlertDialog.Builder(activity)
                    .setTitle(R.string.library_playlist_name)
                	.setMessage(R.string.library_new_playlist_prompt)
                	.setView(input)
                	.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() 
                	{
                		public void onClick(DialogInterface dialog, int whichButton) 
                		{
                			final String name = input.getText().toString().trim();
                			if (null != name && name.length() > 0) 
                			{
                				app.oMPDAsyncHelper.execAsync(new Runnable() 
                				{
                					@Override
                					public void run() 
                					{
                						try 
                						{
                							app.oMPDAsyncHelper.oMPD.getPlaylist().savePlaylist(name);
       										Tools.notifyUser(getResources().getString(R.string.playlist_saved), getActivity());
                						} 
                						catch (MPDServerException e) 
                						{
                							e.printStackTrace();
                						}
                					}
                				});
                			}
                		}
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() 
                    {
                    	public void onClick(DialogInterface dialog, int whichButton) 
                    	{
                    	}
                    })
                    .show();
                return true;
                
            default:
                return false;
        }
    }
    
}
