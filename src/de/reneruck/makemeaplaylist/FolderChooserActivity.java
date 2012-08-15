package de.reneruck.makemeaplaylist;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class FolderChooserActivity extends Activity {

	private static final String TAG = "Make me a Playlist";
	private ListView folderContent;
	private BaseAdapter fileAdapter; 
	private File currentParentFolder;
	private File[] currentFileList;
	private Mode currentMode;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.currentMode = (Mode) getIntent().getExtras().get("mode");
        
        setCurrentFolderToDefault();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        this.folderContent = new ListView(getApplicationContext());
        this.folderContent.setOnItemClickListener(this.onFileEntryClicklistener);
        setContentView(this.folderContent);
        displayChildEntries(this.currentParentFolder);
    }

	private void setCurrentFolderToDefault() {
		if(this.currentParentFolder == null)
        {
        	this.currentParentFolder = Environment.getExternalStorageDirectory();
        }
	}

    private void displayChildEntries(File fileToDisplayContent) {
    	this.currentParentFolder = fileToDisplayContent;
    	this.currentFileList = this.currentParentFolder.listFiles();
    	
    	this.fileAdapter = new ArrayAdapter<File>(getApplicationContext(), android.R.layout.simple_list_item_1, this.currentFileList);
    	this.folderContent.setAdapter(this.fileAdapter);
    	this.folderContent.invalidate();
    }

    private OnItemClickListener onFileEntryClicklistener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			File clickedFile = getClickedFile(arg2);
			if(clickedFile != null)
			{
				handleClickedFile(clickedFile);
			}
		}
	};
	
	private void handleClickedFile(File clickedFile) {
		if(isPlaylist(clickedFile)){
			playPlaylist(clickedFile);
		} else if(clickedFile.isDirectory())
		{
			displayChildEntries(clickedFile);
		} else {
			Toast.makeText(getApplicationContext(), R.string.no_valid_file_oder_folder, Toast.LENGTH_SHORT).show();
		}
	}
	
	private String getPlaylistIdForName(String fileName)
	{
		Map<String, String> playlistIds = getPlaylistIds();
		return playlistIds.get(fileName);
	}
	
	private Map<String, String> getPlaylistIds() {
		Map<String, String> playListIds = new HashMap<String, String>();
		String playListId;
		String playListName;
		Cursor cursor = getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null, null, null, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					playListId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
					playListName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists.NAME));
					playListIds.put(playListName, playListId);
				} while (cursor.moveToNext());
				cursor.close();
			}
			return playListIds;
		}
		return playListIds;
	}
	
	
	
	private void playPlaylist(File clickedFile) {
		Toast.makeText(getApplicationContext(), R.string.coming_soon, Toast.LENGTH_LONG).show();
		
//		String playlistIdForName = getPlaylistIdForName(getPlainPlaylistName(clickedFile.getName()));
//		
//		Intent intent = new Intent(Intent.ACTION_VIEW); 
//		intent.setComponent(new ComponentName 
//		("com.android.music","com.android.music.PlaylistBrowserActivity")); 
//		intent.setType(MediaStore.Audio.Playlists.CONTENT_TYPE); 
//		intent.setFlags(0x10000000); 
//		intent.putExtra("oneshot", false); 
//		if(playlistIdForName != null){
//			intent.putExtra("playlist", playlistIdForName); 
//		}
//		startActivity(intent); 
	}

	private String getPlainPlaylistName(String name) {
		name.replace(".m3u", " ");
		return name.trim();
	}

	private boolean isPlaylist(File file) {
		String fileName = file.getName();
		if(fileName.endsWith(".m3u") | fileName.endsWith(".pls"))
		{
			return true;
		} else {
			return false;
		}
	}
	
	private File getClickedFile(int position) {
		return Arrays.asList(this.currentFileList).get(position);
	}
	
	private void generatePlaylist() throws IOException
	{
		if(this.currentFileList.length > 0){
			
			String playlistFilenamePath = this.currentParentFolder.getAbsolutePath();
			String filename = this.currentParentFolder.getParentFile().getName() + "-" + this.currentParentFolder.getName();
			String playlistFileName = playlistFilenamePath + File.separator + filename + ".m3u";
			
			File playlistFile = new File(playlistFileName);
			playlistFile.createNewFile();
			FileOutputStream outputStream = new FileOutputStream(playlistFile);
			
			for (File currentFile : this.currentFileList) {
				if(currentFile != null && !currentFile.isDirectory() && isMediaFile(currentFile))
				{
					writeEntryToPlaylist(outputStream, currentFile);
				}
			}
			outputStream.flush();
			outputStream.close();
		} else {
			Toast.makeText(getApplicationContext(), R.string.no_media_files, Toast.LENGTH_SHORT).show();
		}
	}
	
	private boolean isMediaFile(File currentFile) {
		if(currentFile.getName().endsWith(".mp3")
			| currentFile.getName().endsWith("wav")
			| currentFile.getName().endsWith("wave")
			| currentFile.getName().endsWith("ogg"))
		{
			return true;
		}
		return false;
	}

	private void writeEntryToPlaylist(FileOutputStream outputStream, File currentFile) throws IOException {
		String entryText = currentFile.getAbsolutePath() + "\r\n";
		outputStream.write(entryText.getBytes(Charset.forName("UTF-8")));
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
        case android.R.id.home:
        	File parentFile = this.currentParentFolder.getParentFile();
        	if(parentFile != null) {
        		displayChildEntries(parentFile);
        	} else {
        		System.exit(1);
        	}
        	break;
        case R.id.generate_playlist:
        	handleGeneratePlaylistOption();
        	break;
		}
		return true;
	}

	private void handleGeneratePlaylistOption() {
		try {
			if(oldPlaylistExists())
			{
				Toast.makeText(getApplicationContext(), R.string.found_existing_playlist, Toast.LENGTH_SHORT).show();
			} else {
				switch (this.currentMode) {
					case book_mode:
						generatePlaylistForAudiobook();
						break;
					case cd_mode:
						generatePlaylistForSingleFolder();
						break;
				}
				Log.d(TAG, "trigger media scan");
				triggerMediaScan();
				displayChildEntries(this.currentParentFolder);
			}
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), R.string.error_generating_playlist, Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	private void generatePlaylistForSingleFolder() throws IOException {
		if(hasMediaEntries())
		{
			Log.d(TAG, "Starting playlist generation");
			generatePlaylist();
			Log.d(TAG, "playlist generation successful");
		}
	}

	private void generatePlaylistForAudiobook() throws IOException {
		generatePlaylistForSingleFolder();
		if(hasSubFolders()){
			generatePlaylistOverAllSubfolders();
		}
	}

	private void generatePlaylistOverAllSubfolders() {
		List<String> allFiles = new LinkedList<String>();
		for (File currentFile : this.currentFileList) {
			if(currentFile.isDirectory())
			{
				allFiles.addAll(getAllMediaFileNames(currentFile));
			}
		}
	}


	private List<String> getAllMediaFileNames(File currentFile) {
		List<String> mediaFiles = new LinkedList<String>();
		for (File currentSubFile : this.currentFileList) {
			if(isMediaFile(currentSubFile))
			{
				mediaFiles.add(currentSubFile.getAbsolutePath());
			}
		}
		return mediaFiles;
	}

	private boolean hasSubFolders() {
		for (File currentFile : this.currentFileList) {
			if(currentFile.isDirectory())
			{
				return true;
			}
		}
		return false;
	}

	private boolean hasMediaEntries() {
		for (File currentFile : this.currentFileList) {
			if(isMediaFile(currentFile))
			{
				return true;
			}
		}
		return false;
	}

	private boolean oldPlaylistExists() {
		for (File currentFile : this.currentFileList) {
			if(isPlaylist(currentFile)){
				return true;
			}
		}
		return false;
	}

	private void triggerMediaScan() {
		sendBroadcast( new Intent(Intent.ACTION_MEDIA_MOUNTED,
				Uri.parse("file://" + this.currentParentFolder.getAbsolutePath())));
	}
    
}
