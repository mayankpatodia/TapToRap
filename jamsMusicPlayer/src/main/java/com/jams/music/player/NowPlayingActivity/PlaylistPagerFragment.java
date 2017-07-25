/*
 * Copyright (C) 2014 Saravan Pantham
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jams.music.player.NowPlayingActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.jams.music.player.Dialogs.ABRepeatDialog;
import com.jams.music.player.EqualizerActivity.EqualizerActivity;
import com.jams.music.player.Helpers.SongHelper;
import com.jams.music.player.Helpers.SongHelper.AlbumArtLoadedListener;
import com.jams.music.player.Helpers.TypefaceHelper;
import com.jams.music.player.ImageTransformers.PicassoMirrorReflectionTransformer;
import com.jams.music.player.R;
import com.jams.music.player.Utils.Common;
import com.musixmatch.lyrics.musiXmatchLyricsConnector;

import org.apache.commons.io.output.TaggedOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PlaylistPagerFragment extends Fragment implements AlbumArtLoadedListener {

	private Context mContext;
	private Common mApp;
	private ViewGroup mRootView;
	private int mPosition;
	private SongHelper mSongHelper;
	private PopupMenu popup;

    private RelativeLayout bottomDarkPatch;
    private RelativeLayout songInfoLayout;
	private TextView songNameTextView;
	private TextView artistAlbumNameTextView;
	private ImageView coverArt;
	private ImageView overflowIcon;
	
	private boolean mAreLyricsVisible = false;
	private ScrollView mLyricsScrollView;
	private TextView mLyricsTextView;
	private TextView mLyricsEmptyTextView;
    public NowPlayingActivity nowPlayingActivity;
    public musiXmatchLyricsConnector mLyricsPlugin = null;

    public PlaylistPagerFragment()
    {

    }

    public PlaylistPagerFragment(NowPlayingActivity temp){
        nowPlayingActivity = temp;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        mContext = getActivity();
        mApp = (Common) mContext.getApplicationContext();

        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_playlist_pager_fill, container, false);
        mPosition = getArguments().getInt("POSITION");

        mLyricsPlugin = new musiXmatchLyricsConnector(this.getActivity());

        overflowIcon = (ImageView) mRootView.findViewById(R.id.now_playing_overflow_icon);
    	coverArt = (ImageView) mRootView.findViewById(R.id.coverArt);
        bottomDarkPatch = (RelativeLayout) mRootView.findViewById(R.id.bottomDarkPatch);
        songInfoLayout = (RelativeLayout) mRootView.findViewById(R.id.songInfoLayout);
        songNameTextView = (TextView) mRootView.findViewById(R.id.songName);
    	artistAlbumNameTextView = (TextView) mRootView.findViewById(R.id.artistAlbumName);
    	
    	mLyricsScrollView = (ScrollView) mRootView.findViewById(R.id.lyrics_scroll_view);
    	mLyricsTextView = (TextView) mRootView.findViewById(R.id.lyrics);
    	mLyricsEmptyTextView = (TextView) mRootView.findViewById(R.id.lyrics_empty);

    	mLyricsTextView.setTypeface(TypefaceHelper.getTypeface(mContext, "Roboto-Regular"));
    	mLyricsEmptyTextView.setTypeface(TypefaceHelper.getTypeface(mContext, "Roboto-Regular"));
    	songNameTextView.setTypeface(TypefaceHelper.getTypeface(mContext, "Roboto-Regular"));
    	artistAlbumNameTextView.setTypeface(TypefaceHelper.getTypeface(mContext, "Roboto-Regular"));

        //Allow the TextViews to scroll if they extend beyond the layout margins.
        songNameTextView.setSelected(true);
        artistAlbumNameTextView.setSelected(true);
    	
    	//Initialize the pop up menu.
    	popup = new PopupMenu(getActivity(), overflowIcon);
		popup.getMenuInflater().inflate(R.menu.now_playing_overflow_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(menuItemClickListener);

        mSongHelper = new SongHelper();
        mSongHelper.setAlbumArtLoadedListener(this);

        if (mApp.getOrientation()==Common.ORIENTATION_LANDSCAPE)
            mSongHelper.populateSongData(mContext, mPosition);
		else
            mSongHelper.populateSongData(mContext, mPosition, new PicassoMirrorReflectionTransformer());

    	songNameTextView.setText(mSongHelper.getTitle());
    	artistAlbumNameTextView.setText(mSongHelper.getAlbum() + " - " + mSongHelper.getArtist());
        overflowIcon.setOnClickListener(overflowClickListener);



        //Kitkat padding.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int navigationBarHeight = Common.getNavigationBarHeight(mContext);
            int bottomPadding = songInfoLayout.getPaddingBottom();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bottomDarkPatch.getLayoutParams();

            if (navigationBarHeight > 0) {
                /* The nav bar already has padding, so remove the extra 15dp
                 * padding that was applied in the layout file.
                 */
                int marginPixelsValue = (int) mApp.convertDpToPixels(15, mContext);
                bottomPadding -= marginPixelsValue;
                params.height -= marginPixelsValue;
            }

            bottomPadding += navigationBarHeight;
            songInfoLayout.setPadding(0, 0, 0, bottomPadding);

            params.height += navigationBarHeight;
            bottomDarkPatch.setLayoutParams(params);

        }
    	
        return mRootView;
    }

    /**
     * Overflow button click listener.
     */
    private OnClickListener overflowClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            //Hide the "Current queue" item if it's already visible.
            if (mApp.isTabletInLandscape())
                popup.getMenu().findItem(R.id.current_queue).setVisible(false);

            popup.show();
        }

    };

    /**
     * Menu item click listener for the overflow pop up menu.
     */
    private PopupMenu.OnMenuItemClickListener menuItemClickListener = new PopupMenu.OnMenuItemClickListener() {

        @Override
        public boolean onMenuItemClick(MenuItem item) {

            switch (item.getItemId()) {
                case R.id.equalizer:
                    Intent intent = new Intent(getActivity(), EqualizerActivity.class);
                    startActivity(intent);
                    break;
                case R.id.save_clear_current_position:
                    String songId = mApp.getService().getCurrentSong().getId();
                    if (item.getTitle().equals(mContext.getResources().getString(R.string.save_current_position))) {
                        item.setTitle(R.string.clear_saved_position);

                        long currentPositionMillis = mApp.getService().getCurrentMediaPlayer().getCurrentPosition();
                        String message = mContext.getResources().getString(R.string.track_will_resume_from);
                        message += " " + mApp.convertMillisToMinsSecs(currentPositionMillis);
                        message += " " + mContext.getResources().getString(R.string.next_time_you_play_it);

                        mApp.getDBAccessHelper().setLastPlaybackPosition(songId, currentPositionMillis);
                        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();

                    } else {
                        item.setTitle(R.string.save_current_position);
                        mApp.getDBAccessHelper().setLastPlaybackPosition(songId, -1);
                        Toast.makeText(mContext, R.string.track_start_from_beginning_next_time_play, Toast.LENGTH_LONG).show();

                    }

                    //Requery the database and update the service cursor.
                    mApp.getPlaybackKickstarter().updateServiceCursor();

                    break;
                case R.id.show_embedded_lyrics:
                    nowPlayingActivity.lyrics();

                    break;
                case R.id.a_b_repeat:
                    FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                    ABRepeatDialog dialog1 = new ABRepeatDialog();
                    dialog1.show(ft, "repeatSongRangeDialog");
                    break;
                case R.id.current_queue:
                    ((NowPlayingActivity) getActivity()).toggleCurrentQueueDrawer();
                    break;
               case R.id.share_song:
                   Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                   sharingIntent.setType("audio/mp3");

                   Uri audioUri = Uri.parse("file://"+
                           mApp.getService().getCurrentSong().getFilePath());

                   sharingIntent.putExtra(Intent.EXTRA_STREAM, audioUri);
                   startActivity(Intent.createChooser(sharingIntent, "Share audio using"));
                   break;
                case R.id.hide_extract:
                   /* try {
                        Toast.makeText(mContext, mApp.getService().getCurrentSong().getFilePath(),Toast.LENGTH_LONG).show();
                        String MYFILE = mApp.getService().getCurrentSong().getFilePath();
                        String strText = "Pramod And Mayank";

                        // append to file
                        FileOutputStream fos = new FileOutputStream(MYFILE, true);


                        fos.write(strText.getBytes());
                        fos.close();
                    } catch (IOException e) {
                        e.toString();
                    } */
                    String path;
                    try{

                        if(detect(mApp.getService().getCurrentSong().getFilePath())) {
                            MaterialDialog dialog = new MaterialDialog.Builder(mContext)
                                    .title("EXTRACT")
                                    .customView(R.layout.dialog_steg_extract, true)
                                    .theme(Theme.DARK)
                                    .autoDismiss(false)
                                    .callback(new MaterialDialog.ButtonCallback() {
                                        @Override
                                        public void onPositive(MaterialDialog dialog) {
                                            super.onPositive(dialog);
                                            View v = dialog.getCustomView();
                                            EditText password = (EditText)v.findViewById(R.id.txtPass);
                                            password.setTypeface(TypefaceHelper.getTypeface(mContext, "Roboto-Regular"));
                                            try{
                                                String s  = stegExtract(mApp.getService().getCurrentSong().getFilePath(), password.getText().toString());
                                                if(s==null)
                                                    Toast.makeText(mContext, "Incorrect Password", Toast.LENGTH_LONG).show();
                                                else{
                                                    showDialog(s);
                                                    dialog.dismiss();
                                                }



                                            } catch(IOException e){
                                                Toast.makeText(mContext, e.toString(), Toast.LENGTH_LONG).show();
                                            }

                                        }

                                        @Override
                                        public void onNegative(MaterialDialog dialog) {
                                            super.onNegative(dialog);
                                            dialog.dismiss();
                                        }
                                    })
                                    .positiveText("Submit")
                                    .negativeText("Cancel")
                                    .show();

                        }
                        else
                        {
                            MaterialDialog dialog = new MaterialDialog.Builder(mContext)
                                    .title("HIDE")
                                    .customView(R.layout.dialog_steg_hide, true)
                                    .theme(Theme.DARK)
                                    .autoDismiss(false)
                                    .callback(new MaterialDialog.ButtonCallback() {
                                        @Override
                                        public void onPositive(MaterialDialog dialog) {
                                            super.onPositive(dialog);
                                            View v = dialog.getCustomView();
                                            EditText message = (EditText) v.findViewById(R.id.txtMsg);
                                            EditText password = (EditText) v.findViewById(R.id.txtPass);
                                            password.setTypeface(TypefaceHelper.getTypeface(mContext, "Roboto-Regular"));
                                            try {
                                                stegHide(mApp.getService().getCurrentSong().getFilePath(), message.getText().toString(), password.getText().toString());
                                                Toast.makeText(mContext, "Message hidden successfully", Toast.LENGTH_LONG).show();

                                            } catch (IOException e) {
                                                Toast.makeText(mContext, e.toString(), Toast.LENGTH_LONG).show();
                                            }
                                            dialog.dismiss();
                                        }

                                        @Override
                                        public void onNegative(MaterialDialog dialog) {
                                            super.onNegative(dialog);
                                            dialog.dismiss();
                                        }
                                    })
                                    .positiveText("Submit")
                                    .negativeText("Cancel")
                                    .show();
                        }

                    } catch(IOException e){
                        Toast.makeText(mContext, e.toString(), Toast.LENGTH_LONG).show();
                    }
                    break;
            }

            return false;
        }



    };

    public void showDialog(final String s){
        MaterialDialog dialog = new MaterialDialog.Builder(mContext)
                .title("MESSAGE")
                .content(s)
                .theme(Theme.DARK)
                .autoDismiss(false)

                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        ClipboardManager clip = (ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData myClip = ClipData.newPlainText("text", s);
                        clip.setPrimaryClip(myClip);
                        Toast.makeText(mContext, "Text copied to clipboard", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        dialog.dismiss();
                    }
                })
                .positiveText("COPY")
                .negativeText("DISMISS")
                .show();
    }

    public boolean detect(String path) throws IOException
    {
        short blockSize = 0;
        int blockId = 0, readBlockId = 0;
        long fileSize = 0;
        RandomAccessFile file;
        File f;
        byte block[]= new byte[4];
        ByteBuffer bb;

        if(ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) //Detect endianness and assign correct value for detecting steg block.
            blockId= 0x47455453;                            //Hex for "STEG".
        else
            blockId= 0x53544547;                            //Hex for "GETS".

        f = new File(path);                      //Create file object to..
        fileSize = f.length();                   //..calculate size of file..
        file = new RandomAccessFile(f, "rw");   //..and open a RandomAccessFile object for random reading and writing.

        file.seek(fileSize - 2);    //Go back two bytes from end-of-file for the block size...
        blockSize = file.readShort();  //..and read the block size.

        if (blockSize <= 1024)
        {
            file.seek(fileSize - blockSize - 6);    //Go to beginning of block. 6 = Width of block id and width of blockSize variable.
            byte b= file.readByte();
            file.seek(fileSize - blockSize - 6);
            file.read(block, 0, 4);                 //Read block id into a byte buffer...
            bb = ByteBuffer.wrap(block);
            readBlockId = bb.getInt();              //..and convert it to an integer..
            file.close();
            return readBlockId == blockId;          //Return the boolean of comparison of readBlockId and actual blockId.
        }
        else
        {
            file.close();
            return false;
        }
    }

    public byte stegHide(String path, String text, String password) throws IOException
    {
        short passSize = (short) password.length(), blockSize = (short) (2 + passSize + text.length());
        File f= new File(path);
        RandomAccessFile file = new RandomAccessFile(path, "rw");

        file.seek(f.length());
        file.writeBytes("STEG");
        file.writeShort(passSize);
        file.writeBytes(password);
        file.writeBytes(text);
        file.writeShort(blockSize);
        file.close();
        return 0;
    }

    public String stegExtract(String path, String password) throws IOException
    {
        short blockSize = 0, readSize = 0;
        File f = new File(path);
        RandomAccessFile file = new RandomAccessFile(f, "rw");
        long fileSize = f.length();
        String temp;

        file.seek(fileSize - 2);
        blockSize = file.readShort();
        file.seek(fileSize - 2 - blockSize);
        readSize = file.readShort();
        byte[] block = new byte[readSize];
        file.readFully(block, 0, readSize);
        temp= new String(block, "US-ASCII");
        if(temp.equals(password))
        {
            readSize= (short) (blockSize - 2 - readSize);
            block = new byte[readSize];
            file.readFully(block, 0, readSize);
            temp = new String(block, "US-ASCII");
            file.close();
            return temp;
        }
        else
        {
            file.close();
            return null;
        }
    }


    /**
     * "Go to" popup menu item click listener.
     */
    private PopupMenu.OnMenuItemClickListener goToMenuClickListener = new PopupMenu.OnMenuItemClickListener() {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.go_to_this_artist:
                    break;
                case R.id.go_to_this_album_artist:
                    break;
                case R.id.go_to_this_album:
                    break;
                case R.id.go_to_this_genre:
                    break;
            }

            return false;
        }

    };

    /**
     * Callback method for album art loading.
     */
	@Override
	public void albumArtLoaded() {
		coverArt.setImageBitmap(mSongHelper.getAlbumArt());
	}

    /**
     * Reads lyrics from the audio file's tag and displays them.
     */
	/*
    class AsyncLoadLyricsTask extends AsyncTask<Boolean, Boolean, Boolean> {

		String mLyrics = "";
		
		@Override
		protected Boolean doInBackground(Boolean... arg0) {
			String songFilePath = mApp.getService().getCurrentSong().getFilePath();
			AudioFile audioFile = null;
			Tag tag = null;
			try {
				audioFile = AudioFileIO.read(new File(songFilePath));
				
				if (audioFile!=null)
					tag = audioFile.getTag();
				else
					return false;
				
				if (tag!=null)
					mLyrics = tag.getFirst(FieldKey.LYRICS);
				else
					return false;
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return true;
		}
		
		@Override
		public void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			
			if (mLyrics!=null && !mLyrics.isEmpty()) {
				mLyricsTextView.setText(mLyrics);
				mLyricsTextView.setVisibility(View.VISIBLE);
				mLyricsEmptyTextView.setVisibility(View.INVISIBLE);
			} else {
				mLyrics = mContext.getResources().getString(R.string.no_embedded_lyrics_found);
				mLyricsTextView.setText(mLyrics);
				mLyricsTextView.setVisibility(View.INVISIBLE);
				mLyricsEmptyTextView.setVisibility(View.VISIBLE);
			}
			
			//Slide up the album art to show the lyrics.
	    	TranslateAnimation slideUpAnimation = new TranslateAnimation(coverArt, 400, new AccelerateInterpolator(), 
					 													 View.INVISIBLE,
					 													 Animation.RELATIVE_TO_SELF, 0.0f, 
					 													 Animation.RELATIVE_TO_SELF, 0.0f, 
					 													 Animation.RELATIVE_TO_SELF, 0.0f, 
					 													 Animation.RELATIVE_TO_SELF, -2.0f);

	    	slideUpAnimation.animate();
	    	
		}
		
	}
    */
    /**
     * Slides down the album art to hide lyrics.
     */

    /*
    private void hideLyrics() {
        TranslateAnimation slideDownAnimation = new TranslateAnimation(coverArt, 400, new DecelerateInterpolator(2.0f),
                                                                       View.VISIBLE,
                                                                       Animation.RELATIVE_TO_SELF, 0.0f,
                                                                       Animation.RELATIVE_TO_SELF, 0.0f,
                                                                       Animation.RELATIVE_TO_SELF, -2.0f,
                                                                       Animation.RELATIVE_TO_SELF, 0.0f);

        slideDownAnimation.animate();
    }
    */
}
