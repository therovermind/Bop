package com.sahdeepsingh.Bop.ui;


import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import com.sahdeepsingh.Bop.R;
import com.sahdeepsingh.Bop.fragments.FragmentAlbum;
import com.sahdeepsingh.Bop.fragments.FragmentGenre;
import com.sahdeepsingh.Bop.fragments.FragmentPlaylist;
import com.sahdeepsingh.Bop.fragments.FragmentSongs;
import com.sahdeepsingh.Bop.notifications.NotificationMusic;
import com.sahdeepsingh.Bop.playerMain.Main;
import com.sahdeepsingh.Bop.playerMain.SingleToast;
import com.sahdeepsingh.Bop.view.ProgressView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.Objects;


public class MainScreen extends ActivityMaster implements MediaController.MediaPlayerControl, ActionBar.TabListener, FragmentSongs.OnListFragmentInteractionListener, FragmentPlaylist.OnListFragmentInteractionListener, FragmentGenre.OnListFragmentInteractionListener, FragmentAlbum.OnListFragmentInteractionListener {


    public static final String BROADCAST_ACTION = "lol";

    /**
     * How long to wait to disable double-pressing to quit
     */
    private static final int BACK_PRESSED_DELAY = 2000;

    private static final float BLUR_RADIUS = 25f;

    ImageView aa;
    TextView name;
    ImageButton pp;
    private boolean playbackPaused = false;
    private TextView mTimeView;
    private TextView mDurationView;
    private ProgressView mProgressView;

    ChangeSongBR changeSongBR;

    private boolean backPressedOnce = false;
    /**
     * Action that actually disables double-pressing to quit
     */
    private final Runnable backPressedTimeoutAction = new Runnable() {
        @Override
        public void run() {
            backPressedOnce = false;
        }
    };
    private Handler backPressedHandler = new Handler();
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    SlidingUpPanelLayout slidingUpPanelLayout;

    public static void addNowPlayingItem() {

        if (Main.mainMenuHasNowPlayingItem)
            return;

        Main.mainMenuHasNowPlayingItem = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        Main.settings.load(this);
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_main_screen);

        slidingUpPanelLayout = findViewById(R.id.sliding_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        name = findViewById(R.id.bottomtextView);
        pp = findViewById(R.id.bottomImagebutton);
        aa = findViewById(R.id.bottomImageview);

        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        mViewPager = findViewById(R.id.container);

        setupViewPager(mViewPager);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        changeSongBR = new ChangeSongBR();
        slidingUpPanelLayoutListen();

    }

    private void slidingUpPanelLayoutListen() {

    }

    private void setupViewPager(ViewPager viewPager) {
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        viewPager.setAdapter(mSectionsPagerAdapter);
    }


    @Override
    public void onListFragmentInteraction(int position, String type) {

        Intent intent = new Intent(this, PlayingNow.class);

        switch (type) {
            case "singleSong":
                Main.musicList.clear();
                Main.musicList.add(Main.songs.songs.get(position));
                Main.nowPlayingList = Main.musicList;
                intent.putExtra("songPosition", position);
                startActivity(intent);


                break;
            case "playlist":
                Main.musicList.clear();
                String selectedPlaylist = Main.songs.playlists.get(position).getName();
                Main.musicList = Main.songs.getSongsByPlaylist(selectedPlaylist);
                Main.nowPlayingList = Main.musicList;
                intent.putExtra("playlistName", selectedPlaylist);
                startActivity(intent);

                break;
            case "GenreList":
                Main.musicList.clear();
                String selectedGenre = Main.songs.getGenres().get(position);
                Main.musicList = Main.songs.getSongsByGenre(selectedGenre);
                Main.nowPlayingList = Main.musicList;
                intent.putExtra("genreName", selectedGenre);
                startActivity(intent);
                break;
            case "AlbumList":
                Main.musicList.clear();
                String selectedAlbum = Main.songs.getAlbums().get(position);
                Main.musicList = Main.songs.getSongsByAlbum(selectedAlbum);
                Main.nowPlayingList = Main.musicList;
                intent.putExtra("albumName", selectedAlbum);
                startActivity(intent);
                break;
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Main.startMusicService(this);
    }

    @Override
    public void onBackPressed() {
        if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        else {
            if (this.backPressedOnce) {
                // Default behavior, quit it
                super.onBackPressed();
                Main.forceExit(this);
                finishAffinity();
                return;
            }

            this.backPressedOnce = true;

            SingleToast.show(this, getString(R.string.menu_main_back_to_exit), Toast.LENGTH_SHORT);

            backPressedHandler.postDelayed(backPressedTimeoutAction, BACK_PRESSED_DELAY);
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * When destroying the Activity.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (backPressedHandler != null)
            backPressedHandler.removeCallbacks(backPressedTimeoutAction);

        NotificationMusic.cancelAll(this);
/*
        no
        Main.stopMusicService(this);
*/
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_ACTION);
        registerReceiver(changeSongBR, intentFilter);
        if (isPlaying()) {
            Main.musicService.notifyCurrentSong();
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            slidingUpPanelLayout.setCoveredFadeColor(getResources().getColor(R.color.transparent));
        }

        if (isPlaying()) {
            if (playbackPaused) {
                playbackPaused = false;
            }
            workonSlidingPanel();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(changeSongBR);
    }

    private void setControlListeners() {

        pp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Main.musicService.togglePlayback();
                if (!Main.musicService.isPaused()) {
                    pp.setImageResource(R.mipmap.ic_pause);
                } else {
                    pp.setImageResource(R.mipmap.ic_play);
                }
            }
        });

    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
/*
            return PlaceholderFragment.newInstance(position + 1);
*/
            switch (position) {
                case 0:
                    return new FragmentSongs();
                case 1:
                    return new FragmentPlaylist();
                case 2:
                    return new FragmentGenre();
                case 3:
                    return new FragmentAlbum();

                default:
                    return new FragmentSongs();


            }
        }

        @Override
        public int getCount() {
            // Show 4 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Songs";
                case 1:
                    return "PlayList";
                case 2:
                    return "Genre";
                case 3:
                    return "Albums";
            }
            return null;
        }
    }


    private void workonSlidingPanel() {

        setControlListeners();
        prepareSeekBar();
    }
    private void prepareSeekBar() {

        mTimeView = findViewById(R.id.mtimeview);
        mDurationView = findViewById(R.id.mdurationview);
        mProgressView = findViewById(R.id.mprogressview);
        mProgressView.setMax((int) Main.musicService.currentSong.getDurationSeconds());
        mDurationView.setText(DateUtils.formatElapsedTime(Main.musicService.currentSong.getDurationSeconds()));
        final Handler handler = new Handler();
        MainScreen.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isPlaying()) {
                    mProgressView.setProgress(getCurrentPosition() / 1000);
                    mTimeView.setText(DateUtils.formatElapsedTime(getCurrentPosition() / 1000));
                }
                handler.postDelayed(this, 1000);
            }
        });
    }

    class ChangeSongBR extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            name.setText(Main.musicService.currentSong.getTitle());
            name.setSelected(true);
            if (Main.musicService.isPaused()) {
                pp.setImageResource(R.mipmap.ic_play);
            } else {
                pp.setImageResource(R.mipmap.ic_pause);
            }
            Bitmap newImage;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 4;
            newImage = BitmapFactory.decodeFile(Main.songs.getAlbumArt(Main.musicService.currentSong));
            if (newImage != null)
                aa.setImageBitmap(newImage);
            else aa.setImageResource(R.mipmap.ic_launcher_round);
        }

    }


    @Override
    public void start() {
        Main.musicService.unpausePlayer();
    }

    /**
     * Callback to when the user pressed the `pause` button.
     */
    @Override
    public void pause() {
        Main.musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if (Main.musicService != null && Main.musicService.musicBound
                && Main.musicService.isPlaying())
            return Main.musicService.getDuration();
        else
            return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (Main.musicService != null && Main.musicService.musicBound
                && Main.musicService.isPlaying())
            return Main.musicService.getPosition();
        else
            return 0;
    }

    @Override
    public void seekTo(int position) {
        Main.musicService.seekTo(position);
    }

    @Override
    public boolean isPlaying() {
        return Main.musicService != null && Main.musicService.musicBound && Main.musicService.isPlaying();

    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return Main.musicService.getAudioSession();
    }

}
