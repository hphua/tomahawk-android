/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Christopher Reichert <creichert07@gmail.com>
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.tomahawk_android.fragments;

import com.google.common.collect.Sets;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.MultiColumnClickListener;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * The base class for {@link AlbumsFragment}, {@link TracksFragment}, {@link ArtistsFragment},
 * {@link PlaylistsFragment} and {@link SearchPagerFragment}. Provides all sorts of functionality to
 * those classes, related to displaying {@link TomahawkListItem}s in whichever needed way.
 */
public abstract class TomahawkFragment extends TomahawkListFragment
        implements MultiColumnClickListener, AbsListView.OnScrollListener {

    public static final String TOMAHAWK_ALBUM_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_album_id";

    public static final String TOMAHAWK_ALBUMARRAY_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_albumarray_id";

    public static final String TOMAHAWK_ARTIST_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_artist_id";

    public static final String TOMAHAWK_ARTISTARRAY_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_artistarray_id";

    public static final String TOMAHAWK_PLAYLIST_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_playlist_id";

    public static final String TOMAHAWK_USER_ID
            = "org.tomahawk.tomahawk_android.tomahawk_user_id";

    public static final String TOMAHAWK_USERARRAY_ID
            = "org.tomahawk.tomahawk_android.tomahawk_userarray_id";

    public static final String TOMAHAWK_SOCIALACTION_ID
            = "org.tomahawk.tomahawk_android.tomahawk_socialaction_id";

    public static final String TOMAHAWK_PLAYLISTENTRY_ID
            = "org.tomahawk.tomahawk_android.tomahawk_playlistentry_id";

    public static final String TOMAHAWK_QUERY_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_query_id";

    public static final String TOMAHAWK_QUERYARRAY_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_querykeysarray_id";

    public static final String TOMAHAWK_PREFERENCEID_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_preferenceid_key";

    public static final String TOMAHAWK_SHOWDELETE_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_showdelete_key";

    public static final String TOMAHAWK_TOMAHAWKLISTITEM_KEY
            = "org.tomahawk.tomahawk_android.tomahawk_tomahawklistitem_id";

    public static final String TOMAHAWK_TOMAHAWKLISTITEM_TYPE
            = "org.tomahawk.tomahawk_android.tomahawk_tomahawklistitem_type";

    public static final String TOMAHAWK_FROMPLAYBACKFRAGMENT
            = "org.tomahawk.tomahawk_android.tomahawk_fromplaybackfragment";

    public static final String TOMAHAWK_USERNAME_STRING
            = "org.tomahawk.tomahawk_android.tomahawk_username_string";

    public static final String TOMAHAWK_PASSWORD_STRING
            = "org.tomahawk.tomahawk_android.tomahawk_password_string";

    public static final String SHOW_MODE
            = "org.tomahawk.tomahawk_android.show_mode";

    protected static final int RESOLVE_QUERIES_REPORTER_MSG = 1336;

    protected static final long RESOLVE_QUERIES_REPORTER_DELAY = 100;

    protected static final int ADAPTER_UPDATE_MSG = 1337;

    protected static final long ADAPTER_UPDATE_DELAY = 500;

    private TomahawkListAdapter mTomahawkListAdapter;

    protected boolean mIsResumed;

    protected Set<String> mCorrespondingRequestIds =
            Sets.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private HashSet<TomahawkListItem> mResolvingItems = new HashSet<TomahawkListItem>();

    protected Set<Query> mCorrespondingQueries
            = Sets.newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    protected ArrayList<Query> mShownQueries = new ArrayList<Query>();

    protected ArrayList<Query> mSearchSongs;

    protected ArrayList<Album> mSearchAlbums;

    protected ArrayList<Artist> mSearchArtists;

    protected ArrayList<User> mSearchUsers;

    protected ArrayList<PlaylistEntry> mShownPlaylistEntries = new ArrayList<PlaylistEntry>();

    protected Collection mCollection;

    protected Album mAlbum;

    protected Artist mArtist;

    protected Playlist mPlaylist;

    protected User mUser;

    protected Query mQuery;

    private int mFirstVisibleItemLastTime = -1;

    private int mVisibleItemCount = 0;

    protected int mShowMode;

    protected final Handler mResolveQueriesHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            removeMessages(msg.what);
            resolveVisibleItems();
        }
    };

    // Handler which reports the PipeLine's and InfoSystem's results in intervals
    private final Handler mAdapterUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            removeMessages(msg.what);
            updateAdapter();
        }
    };

    @SuppressWarnings("unused")
    public void onEvent(PipeLine.ResultsEvent event) {
        if (mCorrespondingQueries.contains(event.mQuery)) {
            if (!mAdapterUpdateHandler.hasMessages(ADAPTER_UPDATE_MSG)) {
                mAdapterUpdateHandler.sendEmptyMessageDelayed(ADAPTER_UPDATE_MSG,
                        ADAPTER_UPDATE_DELAY);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(InfoSystem.ResultsEvent event) {
        if (mCorrespondingRequestIds.contains(event.mInfoRequestData.getRequestId())) {
            if (!mAdapterUpdateHandler.hasMessages(ADAPTER_UPDATE_MSG)) {
                mAdapterUpdateHandler.sendEmptyMessageDelayed(ADAPTER_UPDATE_MSG,
                        ADAPTER_UPDATE_DELAY);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PlaybackService.PlayingTrackChangedEvent event) {
        onTrackChanged();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PlaybackService.PlayStateChangedEvent event) {
        onPlaystateChanged();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PlaybackService.PlayingPlaylistChangedEvent event) {
        onPlaylistChanged();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CollectionManager.UpdatedEvent event) {
        if (event.mUpdatedItem != null) {
            if (mPlaylist == event.mUpdatedItem || mAlbum == event.mUpdatedItem
                    || mArtist == event.mUpdatedItem || mQuery == event.mUpdatedItem) {
                updateAdapter();
            }
        } else {
            updateAdapter();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(DatabaseHelper.PlaylistsUpdatedEvent event) {
        if (mPlaylist != null && mPlaylist.getId().equals(event.mPlaylistId)) {
            refreshCurrentPlaylist();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PlaybackService.ReadyEvent event) {
        onPlaybackServiceReady();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null) {
            if (getArguments().containsKey(TOMAHAWK_ALBUM_KEY)
                    && !TextUtils.isEmpty(getArguments().getString(TOMAHAWK_ALBUM_KEY))) {
                mAlbum = Album.getAlbumByKey(getArguments().getString(TOMAHAWK_ALBUM_KEY));
                if (mAlbum == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    mCorrespondingRequestIds.add(InfoSystem.getInstance().resolve(mAlbum));
                }
            }
            if (getArguments().containsKey(TOMAHAWK_PLAYLIST_KEY) && !TextUtils.isEmpty(
                    getArguments().getString(TOMAHAWK_PLAYLIST_KEY))) {
                mPlaylist = Playlist
                        .getPlaylistById(getArguments().getString(TOMAHAWK_PLAYLIST_KEY));
                if (mPlaylist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    refreshCurrentPlaylist();
                }
            }
            if (getArguments().containsKey(TOMAHAWK_ARTIST_KEY) && !TextUtils
                    .isEmpty(getArguments().getString(TOMAHAWK_ARTIST_KEY))) {
                mArtist = Artist.getArtistByKey(getArguments().getString(TOMAHAWK_ARTIST_KEY));
                if (mArtist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    ArrayList<String> requestIds = InfoSystem.getInstance().resolve(mArtist, false);
                    for (String requestId : requestIds) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            }
            if (getArguments().containsKey(TOMAHAWK_USER_ID) && !TextUtils
                    .isEmpty(getArguments().getString(TOMAHAWK_USER_ID))) {
                mUser = User.get(getArguments().getString(TOMAHAWK_USER_ID));
                if (mUser.getName() == null) {
                    mCorrespondingRequestIds.add(InfoSystem.getInstance().resolve(mUser));
                }
            }
            if (getArguments().containsKey(CollectionManager.COLLECTION_ID)) {
                mCollection = CollectionManager.getInstance()
                        .getCollection(getArguments().getString(CollectionManager.COLLECTION_ID));
            }
            if (getArguments().containsKey(TOMAHAWK_QUERY_KEY) && !TextUtils
                    .isEmpty(getArguments().getString(TOMAHAWK_QUERY_KEY))) {
                mQuery = Query.getQueryByKey(getArguments().getString(TOMAHAWK_QUERY_KEY));
                if (mQuery == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    ArrayList<String> requestIds =
                            InfoSystem.getInstance().resolve(mQuery.getArtist(), false);
                    for (String requestId : requestIds) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            }
            if (getArguments().containsKey(TOMAHAWK_USERARRAY_ID)) {
                mSearchUsers = new ArrayList<User>();
                for (String userId : getArguments().getStringArrayList(TOMAHAWK_USERARRAY_ID)) {
                    mSearchUsers.add(User.get(userId));
                }
            }
            if (getArguments().containsKey(TOMAHAWK_ARTISTARRAY_KEY)) {
                mSearchArtists = new ArrayList<Artist>();
                for (String userId : getArguments().getStringArrayList(TOMAHAWK_ARTISTARRAY_KEY)) {
                    Artist artist = Artist.getArtistByKey(userId);
                    if (artist != null) {
                        mSearchArtists.add(artist);
                    }
                }
            }
            if (getArguments().containsKey(TOMAHAWK_ALBUMARRAY_KEY)) {
                mSearchAlbums = new ArrayList<Album>();
                for (String userId : getArguments().getStringArrayList(TOMAHAWK_ALBUMARRAY_KEY)) {
                    Album album = Album.getAlbumByKey(userId);
                    if (album != null) {
                        mSearchAlbums.add(album);
                    }
                }
            }
            if (getArguments().containsKey(TOMAHAWK_QUERYARRAY_KEY)) {
                mSearchSongs = new ArrayList<Query>();
                for (String userId : getArguments().getStringArrayList(TOMAHAWK_QUERYARRAY_KEY)) {
                    Query query = Query.getQueryByKey(userId);
                    if (query != null) {
                        mSearchSongs.add(query);
                    }
                }
            }
        }

        StickyListHeadersListView list = getListView();
        if (list != null) {
            list.setOnScrollListener(this);
        }

        onPlaylistChanged();

        mIsResumed = true;
    }

    @Override
    public void onPause() {
        super.onPause();

        for (Query query : mCorrespondingQueries) {
            if (ThreadManager.getInstance().stop(query)) {
                mCorrespondingQueries.remove(query);
            }
        }

        mAdapterUpdateHandler.removeCallbacksAndMessages(null);

        mIsResumed = false;
    }

    @Override
    public abstract void onItemClick(View view, Object item);

    /**
     * Called every time an item inside a ListView or GridView is long-clicked
     *
     * @param item the Object which corresponds to the long-click
     */
    @Override
    public boolean onItemLongClick(View view, Object item) {
        TomahawkListItem contextItem = null;
        if (mAlbum != null) {
            contextItem = mAlbum;
        } else if (mArtist != null) {
            contextItem = mArtist;
        } else if (mPlaylist != null) {
            contextItem = mPlaylist;
        }
        return FragmentUtils.showContextMenu((TomahawkMainActivity) getActivity(), item,
                contextItem);
    }

    /**
     * Get the {@link BaseAdapter} associated with this activity's ListView.
     */
    public TomahawkListAdapter getListAdapter() {
        return mTomahawkListAdapter;
    }

    /**
     * Set the {@link BaseAdapter} associated with this activity's ListView.
     */
    public void setListAdapter(TomahawkListAdapter adapter) {
        super.setListAdapter(adapter);
        mTomahawkListAdapter = adapter;
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    protected abstract void updateAdapter();

    /**
     * This method _MUST_ be called at the end of updateAdapter (with the exception of
     * PlaybackFragment)
     */
    protected void onUpdateAdapterFinished() {
        updateShowPlaystate();
        forceAutoResolve();
        setupNonScrollableSpacer();
        setupScrollableSpacer();
        setupAnimations();
    }

    /**
     * If the PlaybackService signals, that it is ready, this method is being called
     */
    protected void onPlaybackServiceReady() {
        updateShowPlaystate();
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver received a Broadcast indicating that the
     * playlist has changed inside our PlaybackService
     */
    protected void onPlaylistChanged() {
        updateShowPlaystate();
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver in PlaybackFragment received a Broadcast
     * indicating that the playState (playing or paused) has changed inside our PlaybackService
     */
    protected void onPlaystateChanged() {
        updateShowPlaystate();
    }

    /**
     * Called when the PlaybackServiceBroadcastReceiver received a Broadcast indicating that the
     * track has changed inside our PlaybackService
     */
    protected void onTrackChanged() {
        updateShowPlaystate();
    }

    protected void updateShowPlaystate() {
        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        if (getListAdapter() != null) {
            if (playbackService != null) {
                getListAdapter().setShowPlaystate(true);
                getListAdapter().setHighlightedItemIsPlaying(playbackService.isPlaying());
                getListAdapter().setHighlightedEntry(playbackService.getCurrentEntry());
                getListAdapter().setHighlightedQuery(playbackService.getCurrentQuery());
            } else {
                getListAdapter().setShowPlaystate(false);
            }
            getListAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        super.onScrollStateChanged(view, scrollState);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);

        mVisibleItemCount = visibleItemCount;
        if (mFirstVisibleItemLastTime != firstVisibleItem) {
            mFirstVisibleItemLastTime = firstVisibleItem;
            mResolveQueriesHandler.removeCallbacksAndMessages(null);
            mResolveQueriesHandler.sendEmptyMessageDelayed(RESOLVE_QUERIES_REPORTER_MSG,
                    RESOLVE_QUERIES_REPORTER_DELAY);
        }
    }

    protected void forceAutoResolve() {
        mResolveQueriesHandler.removeCallbacksAndMessages(null);
        mResolveQueriesHandler.sendEmptyMessageDelayed(RESOLVE_QUERIES_REPORTER_MSG,
                RESOLVE_QUERIES_REPORTER_DELAY);
    }

    private void resolveVisibleItems() {
        resolveQueriesFromTo(mFirstVisibleItemLastTime - 5,
                mFirstVisibleItemLastTime + mVisibleItemCount + 5);
        resolveItemsFromTo(mFirstVisibleItemLastTime,
                mFirstVisibleItemLastTime + mVisibleItemCount + 1);
    }

    private void resolveQueriesFromTo(final int start, final int end) {
        Set<Query> qs = new HashSet<>();
        for (int i = (start < 0 ? 0 : start); i < end && i < mShownQueries.size(); i++) {
            Query q = mShownQueries.get(i);
            if (!q.isSolved() && !mCorrespondingQueries.contains(q)) {
                qs.add(q);
            }
        }
        if (!qs.isEmpty()) {
            HashSet<Query> queries = PipeLine.getInstance().resolve(qs);
            mCorrespondingQueries.addAll(queries);
        }
    }

    private void resolveItemsFromTo(final int start, final int end) {
        if (mTomahawkListAdapter != null) {
            for (int i = (start < 0 ? 0 : start); i < end && i < mTomahawkListAdapter.getCount();
                    i++) {
                Object object = mTomahawkListAdapter.getItem(i);
                if (object instanceof List) {
                    for (Object item : (List) object) {
                        if (item instanceof TomahawkListItem) {
                            resolveItem((TomahawkListItem) item);
                        }
                    }
                } else if (object instanceof TomahawkListItem) {
                    resolveItem((TomahawkListItem) object);
                }
            }
        }
    }

    protected void resolveItem(TomahawkListItem item) {
        InfoSystem infoSystem = InfoSystem.getInstance();
        if (!mResolvingItems.contains(item)) {
            mResolvingItems.add(item);
            if (item instanceof SocialAction) {
                resolveItem(((SocialAction) item).getTargetObject());
                resolveItem(((SocialAction) item).getUser());
            } else if (item instanceof Album && item.getImage() == null) {
                mCorrespondingRequestIds.add(infoSystem.resolve((Album) item));
            } else if (item instanceof Artist && item.getImage() == null) {
                mCorrespondingRequestIds.addAll(infoSystem.resolve((Artist) item, false));
            } else if (item instanceof User && item.getImage() == null) {
                mCorrespondingRequestIds.add(infoSystem.resolve((User) item));
            }
        }
    }

    protected void refreshCurrentPlaylist() {
        ThreadManager.getInstance().execute(
                new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_VERYHIGH) {
                    @Override
                    public void run() {
                        HatchetAuthenticatorUtils authenticatorUtils
                                = (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                        if (mUser != authenticatorUtils.getLoggedInUser()) {
                            mCorrespondingRequestIds
                                    .add(InfoSystem.getInstance().resolve(mPlaylist));
                        } else {
                            Playlist playlist =
                                    DatabaseHelper.getInstance().getPlaylist(mPlaylist.getId());
                            if (playlist != null) {
                                mPlaylist = playlist;
                                CollectionManager.UpdatedEvent event
                                        = new CollectionManager.UpdatedEvent();
                                event.mUpdatedItem = mPlaylist;
                                EventBus.getDefault().post(event);
                            }
                        }
                    }
                }
        );
    }
}

