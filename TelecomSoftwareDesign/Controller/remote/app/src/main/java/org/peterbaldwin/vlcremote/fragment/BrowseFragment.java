/*-
 *  Copyright (C) 2011 Peter Baldwin
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.vlcremote.fragment;

import org.peterbaldwin.client.android.vlcremote.PlaybackActivity;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.loader.DirectoryLoader;
import org.peterbaldwin.vlcremote.model.Directory;
import org.peterbaldwin.vlcremote.model.File;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Remote;
import org.peterbaldwin.vlcremote.net.MediaServer;
import org.peterbaldwin.vlcremote.widget.DirectoryAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BrowseFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Remote<Directory>> {
    private interface Data {
        int DIRECTORY = 1;
    }

    private interface State {
        String DIRECTORY = "vlc:directory";
    }

    private DirectoryAdapter mAdapter;

    private MediaServer mMediaServer;

    private MediaServer mFileServer;

    private String mDirectory = "~";

    private Preferences mPreferences;

    private TextView mTitle;

    private TextView mEmpty;

    private EditText mEditHostname;

    private EditText mEditPort;

    private int mPort;

    private static final String TAG = "PickFileServer";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Context context = getActivity();
        mPreferences = Preferences.get(context);
        if (savedInstanceState == null) {
            mDirectory = mPreferences.getBrowseDirectory();
        } else {
            mDirectory = savedInstanceState.getString(State.DIRECTORY);
        }
        // NAVEEN: Added default File server port
        mPort = 8080;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.browse, root, false);
        mTitle = (TextView) view.findViewById(android.R.id.title);
        mEmpty = (TextView) view.findViewById(android.R.id.empty);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(State.DIRECTORY, mDirectory);
    }

    public void setMediaServer(MediaServer mediaServer) {
        mMediaServer = mediaServer;
    }

    public void setFileServer(MediaServer fileServer) {
        mFileServer = fileServer;
    }

    @Override
    public void setEmptyText(CharSequence text) {
        mEmpty.setText(text);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Context context = getActivity();
        mAdapter = new DirectoryAdapter(context);
        setListAdapter(mAdapter);
        registerForContextMenu(getListView());
/* NAVEEN: Removed loading director from the media server
        if (mMediaServer != null) {
            getLoaderManager().initLoader(Data.DIRECTORY, Bundle.EMPTY, this);
        }
*/
        // NAVEEN: This would load the directory list from the File server
        if (mFileServer != null) {
            getLoaderManager().initLoader(Data.DIRECTORY, Bundle.EMPTY, this);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        File file = mAdapter.getItem(position);
        if (file.isDirectory()) {
            openDirectory(file);
        } else {
            String mrl = file.getmUri();
            mMediaServer.status().command.input.play(mrl, file.getOptions());
        }
    }

    private void openDirectory(File file) {
        openDirectory(file.getPath());
    }

    public void openDirectory(String path) {
        mDirectory = path;
        mAdapter.clear();
        getLoaderManager().restartLoader(Data.DIRECTORY, null, this);
    }

    private boolean isDirectory(ContextMenuInfo menuInfo) {
        if (menuInfo instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            if (info.position < mAdapter.getCount()) {
                File file = mAdapter.getItem(info.position);
                return file.isDirectory();
            }
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.browse_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                getLoaderManager().restartLoader(Data.DIRECTORY, Bundle.EMPTY, this);
                return true;
            case R.id.menu_parent:
                // NAVEEN
                final Context context = getActivity();
                PlaybackActivity act = (PlaybackActivity) context;
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.add_server);
                LayoutInflater inflater = act.getLayoutInflater();
                View view = inflater.inflate(R.layout.add_server, null);
                mEditHostname = (EditText) view.findViewById(R.id.edit_hostname);
                mEditPort = (EditText) view.findViewById(R.id.edit_port);
                builder.setTitle("Add File Server");
                builder.setView(view);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String hostname = getHostname();
                        int port = getPort();
                        String FileServer = hostname + ":" + port;
                        mFileServer = new MediaServer(context, FileServer);
                        reload();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                } );
                builder.show();
                /* openParentDirectory(); */
                return true;
            case R.id.menu_home:
                mDirectory = mPreferences.getHomeDirectory();
                if (mFileServer != null)    // NAVEEN Added
                    getLoaderManager().restartLoader(Data.DIRECTORY, Bundle.EMPTY, this);
                return true;
            case R.id.menu_set_home:
                mPreferences.setHomeDirectory(mDirectory);
                showSetHomeToast();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openParentDirectory() {
        for (int position = 0, n = mAdapter.getCount(); position < n; position++) {
            File file = mAdapter.getItem(position);
            if (file.isDirectory() && "..".equals(file.getName())) {
                openDirectory(file);
                return;
            }
        }

        // Open the list of drives if there is no parent directory entry
        openDirectory("");
    }

    private void showSetHomeToast() {
        Context context = getActivity();
        CharSequence message = getString(R.string.sethome, getTitle());
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.browse_context, menu);
        menu.findItem(R.id.browse_context_open).setVisible(isDirectory(menuInfo));
        menu.findItem(R.id.browse_context_play).setVisible(!isDirectory(menuInfo));    //  2015.11.29 Tim added: if directory, don't show play option
        menu.findItem(R.id.browse_context_enqueue).setVisible(!isDirectory(menuInfo)); //  2015.11.29 Tim added: if directory, don't show "add to list" option

//2015.11.28 Tim disabled:
//                disable stream option
        //menu.findItem(R.id.browse_context_stream).setVisible(!isDirectory(menuInfo));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuInfo menuInfo = item.getMenuInfo();
        if (menuInfo instanceof AdapterContextMenuInfo) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            if (info.position < mAdapter.getCount()) {
                File file = mAdapter.getItem(info.position);
                switch (item.getItemId()) {
                    case R.id.browse_context_open:
                        openDirectory(file);
                        return true;
                    case R.id.browse_context_play:
                        mMediaServer.status().command.input.play(file.getMrl(), file.getOptions());
                        return true;

//2015.11.28 Tim disabled:
//                disable stream option
//                    case R.id.browse_context_stream:
//                        mMediaServer.status().command.input.play(file.getMrl(),
//                                file.getStreamingOptions());
//                        Intent intent = file.getIntentForStreaming(mMediaServer.getAuthority());
//                        startActivity(intent);
//                        return true;
                    case R.id.browse_context_enqueue:
                        mMediaServer.status().command.input.enqueue(file.getMrl());
                        return true;
                }
            }
        }
        return super.onContextItemSelected(item);
    }

    /** {@inheritDoc} */
    public Loader<Remote<Directory>> onCreateLoader(int id, Bundle args) {
        Context context = getActivity();
        mPreferences.setBrowseDirectory(mDirectory);
        setEmptyText(getText(R.string.loading));
        /* return new DirectoryLoader(context, mMediaServer, mDirectory); */
        return new DirectoryLoader(context, mFileServer, mDirectory);
    }

    /** {@inheritDoc} */
    public void onLoadFinished(Loader<Remote<Directory>> loader, Remote<Directory> result) {
        mAdapter.setDirectory(result.data);
        setEmptyText(getText(R.string.connection_error));
        setTitle(result.data != null ? result.data.getPath() : null);
        if (isEmptyDirectory(result.data)) {
            handleEmptyDirectory();
        }
    }

    private void setTitle(CharSequence title) {
        mTitle.setText(title);
    }

    public CharSequence getTitle() {
        return mTitle.getText();
    }

    private boolean isEmptyDirectory(Directory directory) {
        if (directory != null) {
            return directory.isEmpty();
        } else {
            // The directory could not be retrieved
            return false;
        }
    }

    private void handleEmptyDirectory() {
        showEmptyDirectoryError();
        openDirectory("~");
    }

    private void showEmptyDirectoryError() {
        Context context = getActivity();
        Toast toast = Toast.makeText(context, R.string.browse_empty, Toast.LENGTH_LONG);
        toast.show();
    }

    /** {@inheritDoc} */
    public void onLoaderReset(Loader<Remote<Directory>> loader) {
        mAdapter.setDirectory(null);
    }

    public void reload() {
        /* if (mMediaServer != null) { */
        if (mFileServer != null) {
            getLoaderManager().restartLoader(Data.DIRECTORY, Bundle.EMPTY, this);
        }
    }

    // TODO: Automatically reload directory when connection is restored

    private String getHostname() {
        return mEditHostname.getText().toString();
    }

    private int getPort() {
        String value = String.valueOf(mEditPort.getText());
        if (!TextUtils.isEmpty(value)) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid port number: " + value);
            }
        }
        return mPort;
    }

}
