package org.chatminou.mpdcontrol.fragments;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.chatminou.mpdcontrol.mpd.AlbumInfo;
import org.chatminou.mpdcontrol.mpd.MPD;
import org.chatminou.mpdcontrol.mpd.MPDStatus;
import org.chatminou.mpdcontrol.mpd.Music;
import org.chatminou.mpdcontrol.mpd.event.StatusChangeListener;
import org.chatminou.mpdcontrol.mpd.event.TrackPositionListener;
import org.chatminou.mpdcontrol.mpd.exception.MPDServerException;
import org.chatminou.mpdcontrol.MainActivity;
import org.chatminou.mpdcontrol.MainApplication;
import org.chatminou.mpdcontrol.R;
import org.chatminou.mpdcontrol.cover.AlbumCoverDownloadListener;
import org.chatminou.mpdcontrol.cover.CoverManager;
import org.chatminou.mpdcontrol.helpers.CoverAsyncHelper;
import org.chatminou.mpdcontrol.helpers.RadioItem;
import org.chatminou.mpdcontrol.helpers.RadioStore;
import org.chatminou.mpdcontrol.helpers.SettingsHelper;
import org.chatminou.mpdcontrol.models.Fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.PopupMenuCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

public class PlayingFragment extends Fragment implements StatusChangeListener, TrackPositionListener, OnSharedPreferenceChangeListener, OnMenuItemClickListener 
{
	private View view = null;
	
	private ImageView coverArt = null;
    private ProgressBar coverArtProgress = null;
    
    private View songLayout = null;
    private TextView songTitle = null;
	private TextView songArtist = null;
	private TextView songAlbum = null;
	private TextView songYear = null;
    
	private TextView trackTime = null;
	private TextView trackTotalTime = null;
	private SeekBar trackProgress = null;
	
	private ImageButton buttonShuffle = null;
	private ImageButton buttonPrev = null;
	private ImageButton buttonPlayPause = null;
	private ImageButton buttonStop = null;
	private ImageButton buttonNext = null;
	private ImageButton buttonRepeat = null;
	
	private SeekBar volumeProgress = null;
	private ImageView volumeIcon = null;
	
    private CoverAsyncHelper oCoverAsyncHelper = null;
    private AlbumCoverDownloadListener coverArtListener = null;
    private ButtonEventHandler buttonEventHandler = null;
    private Handler handler = null;
    private PopupMenu coverMenu = null;
    private PopupMenu songMenu = null;
    private View.OnTouchListener songMenuTouchListener = null;
    
    private RadioStore radioStore = RadioStore.getInstance();
	    
    private Timer volTimer = new Timer();
    private TimerTask volTimerTask = null;
    private Timer posTimer = null;
    private TimerTask posTimerTask = null;
    private String lastArtist = "";
	private String lastAlbum = "";
	private boolean shuffleCurrent = true;
	private boolean repeatCurrent = true;
	private Music currentSong = null;
	private AlbumInfo currentAlbumInfo = null;
	private long lastSongTime = 0;
	private long lastElapsedTime = 0;
	
	private float ALPHA_DISABLE = 0.3f;
	
	private static final int POPUP_ARTIST = 0;
    private static final int POPUP_ALBUM = 1;
    private static final int POPUP_CURRENT = 3;
	private static final int POPUP_COVER_BLACKLIST = 4;
    private static final int POPUP_COVER_SELECTIVE_CLEAN = 5;
		
	///////////////////////////////////////////////////////////////////////////
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
		super.onCreate(savedInstanceState);
        handler = new Handler();
        getSettings().registerOnSharedPreferenceChangeListener(this);
    }
	
	@Override
    public void onDestroy() 
	{
		getSettings().unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
    }
	
    @Override
    public void onStart() 
    {
    	super.onStart();
        app.oMPDAsyncHelper.addStatusChangeListener(this);
        app.oMPDAsyncHelper.addTrackPositionListener(this);
    }

    @Override
    public void onStop() 
    {
    	app.oMPDAsyncHelper.removeStatusChangeListener(this);
        app.oMPDAsyncHelper.removeTrackPositionListener(this);
        stopPosTimer();
        super.onStop();
    }
    
    @Override
    public void onResume() 
    {
        super.onResume();
        
        try 
        {
            updateTrackInfo();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
        
        updateStatus(null);
        if (currentAlbumInfo != null) downloadCover(currentAlbumInfo);
    }
	
	///////////////////////////////////////////////////////////////////////////
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
    {
    	setHasOptionsMenu(false);
    	
    	view = inflater.inflate(getLayoutID(), container, false);
    	
		coverArt = (ImageView) view.findViewById(R.id.albumCover);
		coverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);
		setupCover();
		
		oCoverAsyncHelper = new CoverAsyncHelper(app, getSettings());
        oCoverAsyncHelper.setCoverMaxSizeFromScreen(activity);
        oCoverAsyncHelper.setCachedCoverMaxSize(coverArt.getWidth());
        coverArtListener = new AlbumCoverDownloadListener(activity, coverArt, coverArtProgress, true);
        oCoverAsyncHelper.addCoverDownloadListener(coverArtListener);
        
        songLayout = view.findViewById(R.id.songInfo);
        songTitle = (TextView) view.findViewById(R.id.songName);
		songArtist = (TextView) view.findViewById(R.id.artistName);
		songAlbum = (TextView) view.findViewById(R.id.albumName);
		songYear = (TextView) view.findViewById(R.id.albumYear);
		setupSong();

		buttonShuffle = (ImageButton) view.findViewById(R.id.shuffle);
		buttonPrev = (ImageButton) view.findViewById(R.id.prev);
		buttonPlayPause = (ImageButton) view.findViewById(R.id.playpause);
		buttonStop = (ImageButton) view.findViewById(R.id.stop);
		buttonNext = (ImageButton) view.findViewById(R.id.next);
		buttonRepeat = (ImageButton) view.findViewById(R.id.repeat);
		
		trackTime = (TextView) view.findViewById(R.id.trackTime);
		trackTotalTime = (TextView) view.findViewById(R.id.trackTotalTime);
		trackProgress = (SeekBar) view.findViewById(R.id.progress_track);
		setupProgress();
		
		volumeProgress = (SeekBar) view.findViewById(R.id.progress_volume);
		volumeIcon = (ImageView) view.findViewById(R.id.volume_icon);
		setupVolume();

		buttonEventHandler = new ButtonEventHandler();
		buttonShuffle.setOnClickListener(buttonEventHandler);
		buttonPrev.setOnClickListener(buttonEventHandler);
		buttonPlayPause.setOnClickListener(buttonEventHandler);
		buttonPlayPause.setOnLongClickListener(buttonEventHandler);
		buttonStop.setOnClickListener(buttonEventHandler);
		buttonNext.setOnClickListener(buttonEventHandler);
		buttonRepeat.setOnClickListener(buttonEventHandler);
		
		applyViewVisibility(getSettings(), buttonStop, SettingsHelper.APP_SHOW_STOP_BUTTON);

        songTitle.setText(activity.getResources().getString(R.string.playing_not_connected));
      
        return view;
    }

    @Override
    public void onDestroyView() 
    {
    	lastArtist = "";
		lastAlbum = "";
    	if (coverArt != null) 
    	{
    		coverArt.setImageResource(R.drawable.no_cover_art);
    		coverArt = null;
    	}
        if (coverArtListener != null) 
        {
        	coverArtListener.freeCoverDrawable();
        	coverArtListener = null;
        }
        super.onDestroyView();
    }
    
    ///////////////////////////////////////////////////////////////////////////
	
    private void setupCover()
    {
    	coverArt.setOnClickListener(new OnClickListener() 
        {
            @Override
            public void onClick(View view) 
            {
                scrollToNowPlaying();
            }
        });
    	
    	coverMenu = new PopupMenu(activity, coverArt);
        coverMenu.getMenu().add(Menu.NONE, POPUP_COVER_BLACKLIST, Menu.NONE, R.string.playing_popup_other_cover);
        coverMenu.getMenu().add(Menu.NONE, POPUP_COVER_SELECTIVE_CLEAN, Menu.NONE, R.string.playing_popup_reset_cover);
        coverMenu.setOnMenuItemClickListener(PlayingFragment.this);
        coverArt.setOnLongClickListener(new View.OnLongClickListener() 
        {
            @Override
            public boolean onLongClick(View view) 
            {
                if (currentSong != null && !currentSong.isStream()) 
                {
                    coverMenu.show();
                }
                return true;
            }
        });
    } 
    
    private void setupSong()
    {
    	songMenu = new PopupMenu(activity, songLayout);
    	songMenu.getMenu().add(Menu.NONE, POPUP_ALBUM, Menu.NONE, R.string.playing_popup_go_album);
    	songMenu.getMenu().add(Menu.NONE, POPUP_ARTIST, Menu.NONE, R.string.playing_popup_go_artist);
    	songMenu.getMenu().add(Menu.NONE, POPUP_CURRENT, Menu.NONE, R.string.playing_popup_go_current);
    	songMenu.setOnMenuItemClickListener(PlayingFragment.this);

    	songMenuTouchListener = PopupMenuCompat.getDragToOpenListener(songMenu);

        songLayout.setOnClickListener(new OnClickListener() 
        {
            @Override
            public void onClick(View v) 
            {
                if (currentSong == null) return;
                if (!currentSong.isStream())  songMenu.show();
             }
        });
    }
    
    private void setupProgress()
    {
    	trackProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() 
    	{
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) 
            {
            }

            public void onStartTrackingTouch(SeekBar seekBar) 
            {
            }

            public void onStopTrackingTouch(final SeekBar seekBar) 
            {
                new Thread(new Runnable() 
                {
                    @Override
                    public void run() 
                    {
                        try 
                        {
                            app.oMPDAsyncHelper.oMPD.seek(seekBar.getProgress());
                        } 
                        catch (MPDServerException e) 
                        {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }
    

    private void setupVolume()
    {
    	volumeProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() 
    	{
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) 
            {
            }

            public void onStartTrackingTouch(SeekBar seekBar) 
            {
                volTimerTask = new TimerTask() 
                {
                    int lastSentVol = -1;
                    SeekBar progress;
                    public void run() 
                    {
                        if (lastSentVol != progress.getProgress()) 
                        {
                            lastSentVol = progress.getProgress();
                            new Thread(new Runnable() 
                            {
                                @Override
                                public void run() 
                                {
                                    try 
                                    {
                                        app.oMPDAsyncHelper.oMPD.setVolume(lastSentVol);
                                    } 
                                    catch (MPDServerException e) 
                                    {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        }
                    }

                    public TimerTask setProgress(SeekBar prg) 
                    {
                        progress = prg;
                        return this;
                    }
                }.setProgress(seekBar);

                volTimer.scheduleAtFixedRate(volTimerTask, 0, 100);
            }

            public void onStopTrackingTouch(SeekBar seekBar) 
            {
                volTimerTask.cancel();
                volTimerTask.run();
            }
        });
    }
        
    private void applyViewVisibility(SharedPreferences sharedPreferences, View view, String property) 
    {
        if (view == null) return;
        view.setVisibility(sharedPreferences.getBoolean(property, false) ? View.VISIBLE : View.GONE);
    }

    private PlaylistFragment getPlaylistFragment() 
    {
        return (PlaylistFragment) activity.getSupportFragmentManager().findFragmentById(R.id.tab_playlist);
    }

    public SeekBar getProgressBarTrack() 
    {
        return trackProgress;
    }
    
	private int getLayoutID()
	{
		int idLayout;
		if (((MainActivity) activity).isTablet())
		{
			if (((MainActivity) activity).isPortrait()) idLayout = R.layout.fragment_playing_land;
			else idLayout = R.layout.fragment_playing_port;
		}
		else
		{
			if (((MainActivity) activity).isPortrait()) idLayout = R.layout.fragment_playing_port;
			else idLayout = R.layout.fragment_playing_land;
		}
		
		return idLayout;
	}
	
	private void scrollToNowPlaying() 
    {
        PlaylistFragment playlistFragment = getPlaylistFragment();
        if (playlistFragment != null) 
        {
            playlistFragment.scrollToNowPlaying();
        }
    }
	
    ///////////////////////////////////////////////////////////////////////////
    	
    private class ButtonEventHandler implements Button.OnClickListener, Button.OnLongClickListener 
    {
    	@Override
        public void onClick(View v) 
        {
            final MPD mpd = app.oMPDAsyncHelper.oMPD;
            Intent i = null;

            switch (v.getId()) 
            {
                case R.id.stop:
                    new Thread(new Runnable() 
                    {
                        @Override
                        public void run() 
                        {
                            try 
                            {
                                mpd.stop();
                            } 
                            catch (MPDServerException e) 
                            {
                                Log.w(MainApplication.TAG, e.getMessage());
                            }
                        }
                    }).start();
                    break;
                    
                case R.id.next:
                    new Thread(new Runnable() 
                    {
                        @Override
                        public void run() 
                        {
                            try 
                            {
                                mpd.next();
                            } 
                            catch (MPDServerException e) 
                            {
                                Log.w(MainApplication.TAG, e.getMessage());
                            }
                        }
                    }).start();
                    break;
                    
                case R.id.prev:
                    new Thread(new Runnable() 
                    {
                        @Override
                        public void run() 
                        {
                            try 
                            {
                                mpd.previous();
                            } 
                            catch (MPDServerException e) 
                            {
                                Log.w(MainApplication.TAG, e.getMessage());
                            }
                        }
                    }).start();
                    break;
                    
                case R.id.playpause:
                    new Thread(new Runnable() 
                    {
                        @Override
                        public void run() 
                        {
                            String state;
                            try 
                            {
                                state = mpd.getStatus().getState();
                                if (state.equals(MPDStatus.MPD_STATE_PLAYING) || state.equals(MPDStatus.MPD_STATE_PAUSED)) 
                                {
                                    mpd.pause();
                                } 
                                else 
                                {
                                    mpd.play();
                                }
                            } 
                            catch (MPDServerException e) 
                            {
                                Log.w(MainApplication.TAG, e.getMessage());
                            }
                        }
                    }).start();
                    break;
                    
                case R.id.shuffle:
                    try 
                    {
                        mpd.setRandom(!mpd.getStatus().isRandom());
                    } 
                    catch (MPDServerException e) 
                    {
                    }
                    break;
                    
                case R.id.repeat:
                    try 
                    {
                        mpd.setRepeat(!mpd.getStatus().isRepeat());
                    } 
                    catch (MPDServerException e) 
                    {
                    }
                    break;
            }
        }

        public boolean onLongClick(View v) 
        {
            MPD mpd = app.oMPDAsyncHelper.oMPD;
            try 
            {
                switch (v.getId()) 
                {
                    case R.id.playpause:
                        mpd.stop();
                        break;
                        
                    default:
                        return false;
                }
                return true;
            } 
            catch (MPDServerException e) 
            {
            }
            return true;
        }
        
    }

    ///////////////////////////////////////////////////////////////////////////
    
    private void setRepeatButton(boolean state, boolean force) 
    {
    	if (repeatCurrent != state || force) 
        {
            if (buttonRepeat != null) buttonRepeat.setAlpha(state ? 1.0F : ALPHA_DISABLE);
            repeatCurrent = state;
        }
    }

    private void setShuffleButton(boolean state, boolean force)
    {
    	if (currentSong != null && currentSong.isStream()) state = false;
    	
        if (shuffleCurrent != state || force) 
        {
            if (buttonShuffle != null) buttonShuffle.setAlpha(state ? 1.0F : ALPHA_DISABLE);
        	shuffleCurrent = state;
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    private void startPosTimer(long start) 
    {
        stopPosTimer();
        posTimer = new Timer();
        posTimerTask = new TrackTimerTask(start);
        posTimer.scheduleAtFixedRate(posTimerTask, 0, 1000);
    }
   
    private void stopPosTimer() 
    {
        if (null != posTimer) 
        {
            posTimer.cancel();
            posTimer = null;
        }
    }

    private class TrackTimerTask extends TimerTask 
    {
        Date date = new Date();
        long start = 0;
        long ellapsed = 0;

        public TrackTimerTask(long start) 
        {
            this.start = start;
        }

        @Override
        public void run() 
        {
            Date now = new Date();
            ellapsed = start + ((now.getTime() - date.getTime()) / 1000);
            if (currentSong != null && !currentSong.isStream()) 
            {
            	if (trackProgress.isEnabled()) trackProgress.setProgress((int) ellapsed);
                ellapsed = ellapsed > lastSongTime ? lastSongTime : ellapsed;
            }
            
            handler.post(new Runnable() 
            {
                @Override
                public void run() 
                {
                    trackTime.setText(timeToString(ellapsed));
                    trackTotalTime.setText(timeToString(lastSongTime));
                }
            });
            lastElapsedTime = ellapsed;
        }
    }
    
    public void updateTrackInfo() 
    {
        new updateTrackInfoAsync().execute((MPDStatus[]) null);
    }

    public void updateTrackInfo(MPDStatus status) 
    {
        new updateTrackInfoAsync().execute(status);
    }
    
    private void updatePlaylistCovers(AlbumInfo albumInfo) 
    {
        PlaylistFragment playlistFragment  = getPlaylistFragment();
        if (playlistFragment != null) 
        {
            playlistFragment.updateCover(albumInfo);
        }
    }

    private void updateStatus(MPDStatus status) 
    {
        if (activity == null) return;
        if (status == null)
        {
            status = app.getApplicationState().currentMpdStatus;
            if (status == null) 
            {
            	setShuffleButton(false, true);
                setRepeatButton(false, true);
            	trackProgress.setEnabled(false);
            	volumeProgress.setEnabled(false);
            	return;
            }
        } 
        else 
        {
            app.getApplicationState().currentMpdStatus = status;
        }
        
        lastElapsedTime = status.getElapsedTime();
        lastSongTime = status.getTotalTime();
        trackTime.setText(timeToString(lastElapsedTime));
        trackTotalTime.setText(timeToString(lastSongTime));
        if (trackProgress.isEnabled()) trackProgress.setProgress((int) status.getElapsedTime());
         
        if (status.getState() != null) 
        {
            if (status.getState().equals(MPDStatus.MPD_STATE_PLAYING)) 
            {
            	startPosTimer(status.getElapsedTime());
                ImageButton button = (ImageButton) getView().findViewById(R.id.playpause);
                button.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_action_pause));
            } 
            else 
            {
            	stopPosTimer();
                ImageButton button = (ImageButton) getView().findViewById(R.id.playpause);
                button.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_action_play));
            }
        }
        
        setShuffleButton(status.isRandom(), false);
        setRepeatButton(status.isRepeat(), false);
     
        if (status.getVolume() == -1)
        {
            // volume is -1 when output device does not support a volume control, e.g. Optical Output
        	volumeIcon.setAlpha(ALPHA_DISABLE);
        	volumeProgress.setEnabled(false);
        	//volumeIcon.setVisibility(View.GONE);
            //progressBarVolume.setVisibility(View.GONE);
        }
        else
        {
        	volumeIcon.setAlpha(1.0f);
        	volumeProgress.setEnabled(true);
            //progressBarVolume.setVisibility(View.VISIBLE);
            //volumeIcon.setVisibility(View.VISIBLE);
        }

        if (currentSong != null && currentAlbumInfo != null) 
        {
        	trackTotalTime.setVisibility(currentSong.isStream() ? View.GONE : View.VISIBLE);
        	trackProgress.setEnabled(!currentSong.isStream());
        	
        	if (coverMenu != null)
        	{
        		coverMenu.getMenu().setGroupVisible(Menu.NONE, currentAlbumInfo.isValid());
        	}

            if (songMenu != null)
            {
            	songMenu.getMenu().findItem(POPUP_ALBUM).setVisible(!TextUtils.isEmpty(currentSong.getAlbum()));
            	songMenu.getMenu().findItem(POPUP_ARTIST) .setVisible(!TextUtils.isEmpty(currentSong.getArtist()));
            	songLayout.setOnTouchListener(songMenuTouchListener);
            }
        } 
        else 
        {
            songLayout.setOnTouchListener(null);
        }
    }

    public class updateTrackInfoAsync extends AsyncTask<MPDStatus, Void, Boolean> 
    {
        Music actSong = null;
        MPDStatus status = null;

        @Override
        protected Boolean doInBackground(MPDStatus... params)
        {
            if (params == null) 
            {
                try 
                {
                    return doInBackground(app.oMPDAsyncHelper.oMPD.getStatus(true));
                } 
                catch (MPDServerException e) 
                {
                    e.printStackTrace();
                }
                return false;
            }
            if (params[0] != null) 
            {
                String state = params[0].getState();
                if (state != null) 
                {
                    int songPos = params[0].getSongPos();
                    if (songPos >= 0) 
                    {
                        actSong = app.oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
                        status = params[0];
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) 
        {
        	if (result == null || !result || activity == null) 
        	{
            	songTitle.setText(R.string.playing_no_song_info);
				songArtist.setText("");
				songAlbum.setText("");
				songYear.setText("");
				trackProgress.setMax(0);
				return;
            }
        	
        	boolean noSong = actSong == null || status.getPlaylistLength() == 0;
        	if (noSong) 
        	{
        		 currentSong = null;
        		 currentAlbumInfo = null;
        		 lastArtist = "";
        		 lastAlbum = "";
        		 
        		 songTitle.setText(R.string.playing_no_song_info);
        		 songArtist.setText("");
        		 songAlbum.setText("");
        		 songYear.setText("");
        		 trackProgress.setMax(0);
        		 
        		 updateStatus(status);
         
        		 trackTime.setText(timeToString(0));
        		 trackTotalTime.setText(timeToString(0));
        		 
        		 downloadCover(new AlbumInfo("", ""));
        		 return;
        	}
        	
        	currentSong = actSong;
        	currentAlbumInfo = currentSong.getAlbumInfo();
        	
        	RadioItem radio = null;
        	String title = null;
        	String artist = null;
            String album = null;
            String date = null;
            int time = 0;
            
            
        	if (actSong.isStream())
        	{
        		String url = actSong.getFullpath();
            	radio = radioStore.findUrl(url);
            	if (radio == null) radio = radioStore.defaultRadio();
            	            	
	        	Music s = radio.parseSong(actSong);
	        	title = s.getTitle();
	        	artist = s.getArtist();
	        	album = s.getAlbum();
	        	date = null;
	        	time = 0;
        	}
        	else
        	{
        		title = actSong.getTitle();
        		artist = actSong.getArtist();
                album = actSong.getAlbum();
                if (actSong.getDate() == -1) date = null;
                else date = Long.toString(actSong.getDate());
                time = (int) actSong.getTime();
        	}
        	
        	title = title == null ? "" : title;
        	artist = artist == null ? "" : artist;
            album = album == null ? "" : album;
            date = date == null ? "" : " (" + date + ")";
            songTitle.setText(title);
			songArtist.setText(artist);
			songAlbum.setText(album);
			songYear.setText(date);
			trackProgress.setMax(time);
			
			updateStatus(status);
    
            if (actSong.isStream()) 
            {
            	lastArtist = RadioItem.TAG;
                lastAlbum = radio.getCover();
            	currentAlbumInfo.setArtist(lastArtist);
            	currentAlbumInfo.setAlbum(lastAlbum);
                
            	trackTime.setText(timeToString(0));
                trackTotalTime.setText(timeToString(0));
                downloadCover(new AlbumInfo(lastArtist, lastAlbum));
            } 
            else if (!lastAlbum.equals(album) || !lastArtist.equals(artist)) 
            {
            	lastArtist = artist;
                lastAlbum = album;
                
                coverArt.setImageResource(R.drawable.no_cover_art_big);
                downloadCover(actSong.getAlbumInfo());
            }
            
        }
    }
    
    private static String timeToString(long seconds)
	{
		if (seconds < 0) seconds = 0;
		long hours = seconds / 3600;
		seconds -= 3600 * hours;
		long minutes = seconds / 60;
		seconds -= minutes * 60;
		if (hours == 0)
		{
			return String.format("%02d:%02d", minutes, seconds);
		}
		else
		{
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		}
	}
    
    ///////////////////////////////////////////////////////////////////////////
    
    @Override
    public void stateChanged(MPDStatus mpdStatus, String oldState) 
    {
    	updateStatus(mpdStatus);
    }
    
    @Override
    public void trackChanged(MPDStatus mpdStatus, int oldTrack) 
    {
        updateTrackInfo(mpdStatus);
    }

    @Override
    public void trackPositionChanged(MPDStatus status) 
    {
        startPosTimer(status.getElapsedTime());
    }
    
    @Override
    public void connectionStateChanged(boolean connected, boolean connectionLost) 
    {
    	connected = ((MainApplication) activity.getApplication()).oMPDAsyncHelper.oMPD.isConnected();
        if (connected) 
        {
        	songTitle.setText(activity.getResources().getString(R.string.playing_no_song_info));
        } 
        else 
        {
        	songTitle.setText(activity.getResources().getString(R.string.playing_not_connected));
        }
    }

    @Override
    public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) 
    {
        try 
        {
            updateTrackInfo();
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
    }

    @Override
    public void randomChanged(boolean random) 
    {
    	setShuffleButton(random, false);
    }

    @Override
    public void repeatChanged(boolean repeating) 
    {
        setRepeatButton(repeating, false);
    }
    
    @Override
    public void libraryStateChanged(boolean updating) 
    {
    }   
    
    @Override
    public void volumeChanged(MPDStatus mpdStatus, int oldVolume) 
    {
    	volumeProgress.setProgress(mpdStatus.getVolume());
    }

	///////////////////////////////////////////////////////////////////////////
	
    private void downloadCover(AlbumInfo albumInfo) 
    {
        oCoverAsyncHelper.downloadCover(albumInfo, true);
    }
    

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
    {
        if (key.equals(SettingsHelper.COVER_USE_CACHE) || 
        		key.equals(SettingsHelper.COVER_USE_LOCAL) || 
        		key.equals(SettingsHelper.COVER_USE_INTERNET)) 
        {
            oCoverAsyncHelper.setCoverRetrieversFromPreferences();
        } 
        else if (key.equals(SettingsHelper.APP_SHOW_STOP_BUTTON)) 
        {
            applyViewVisibility(sharedPreferences, buttonStop, key);
        } 
    }

	///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onMenuItemClick(MenuItem item) 
    {
    	LibraryFragment fragment = (LibraryFragment) activity.getSupportFragmentManager().findFragmentById(R.id.tab_library);
    	if (fragment == null) return true;
    	int pos = ((MainActivity) activity).getLibraryFragmentPosition();
    	if (pos == -1) return true;
    	
        switch (item.getItemId()) 
        {
            case POPUP_ARTIST:
            	
            	fragment.displayArtist(currentSong.getArtistAsArtist());
				activity.getActionBar().setSelectedNavigationItem(pos);
				break;
            
            case POPUP_ALBUM:
            	fragment.displayAlbum(currentSong.getAlbumAsAlbum());
				activity.getActionBar().setSelectedNavigationItem(pos);
                break;
                                
            case POPUP_CURRENT:
                scrollToNowPlaying();
                break;
                
            case POPUP_COVER_BLACKLIST:
                CoverManager.getInstance(app,PreferenceManager.getDefaultSharedPreferences(activity)).markWrongCover(currentAlbumInfo);
                downloadCover(currentAlbumInfo);
                updatePlaylistCovers(currentAlbumInfo);
                break;
                
            case POPUP_COVER_SELECTIVE_CLEAN:
                CoverManager.getInstance(app,PreferenceManager.getDefaultSharedPreferences(activity)).clear(currentAlbumInfo);
                downloadCover(currentAlbumInfo);
                updatePlaylistCovers(currentAlbumInfo);
                break;
                
            default:
                return false;
        }
        return true;
    }
}
