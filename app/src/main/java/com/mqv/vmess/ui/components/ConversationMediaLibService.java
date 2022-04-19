package com.mqv.vmess.ui.components;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;

import com.google.common.util.concurrent.ListenableFuture;
import com.mqv.vmess.util.Logging;

public class ConversationMediaLibService extends MediaLibraryService {
    private static final String TAG = ConversationMediaLibService.class.getSimpleName();
    private final Player player;

    public ConversationMediaLibService(Player player) {
        this.player = player;
    }

    @Nullable
    @Override
    public MediaLibrarySession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return new MediaLibrarySession.Builder(this, player, getCallback())
                                      .setMediaItemFiller(new CustomMediaFiler())
                                      .build();
    }

    private MediaLibrarySession.MediaLibrarySessionCallback getCallback() {
        return new MediaLibrarySession.MediaLibrarySessionCallback() {
            @Override
            public MediaSession.ConnectionResult onConnect(MediaSession session, MediaSession.ControllerInfo controller) {
                Logging.debug(TAG, "Media Session connected");
                return MediaLibrarySession.MediaLibrarySessionCallback.super.onConnect(session, controller);
            }

            @Override
            public void onPostConnect(MediaSession session, MediaSession.ControllerInfo controller) {
                Logging.debug(TAG, "Media Session post connect");
                MediaLibrarySession.MediaLibrarySessionCallback.super.onPostConnect(session, controller);
            }

            @Override
            public void onDisconnected(MediaSession session, MediaSession.ControllerInfo controller) {
                Logging.debug(TAG, "Media Session onDisconnect");
                MediaLibrarySession.MediaLibrarySessionCallback.super.onDisconnected(session, controller);
            }

            @Override
            public ListenableFuture<LibraryResult<Void>> onSearch(MediaLibrarySession session, MediaSession.ControllerInfo browser, String query, @Nullable LibraryParams params) {
                Logging.debug(TAG, "Media Session onSearch");
                return MediaLibrarySession.MediaLibrarySessionCallback.super.onSearch(session, browser, query, params);
            }
        };
    }

    private static class CustomMediaFiler implements MediaSession.MediaItemFiller {
        @NonNull
        @Override
        public MediaItem fillInLocalConfiguration(MediaSession session, MediaSession.ControllerInfo controller, MediaItem mediaItem) {
            return new MediaItem.Builder()
                                .setMediaId(mediaItem.mediaId)
                                .setMimeType(null)
                                .setUri(mediaItem.mediaMetadata.mediaUri)
                                .setMediaMetadata(new MediaMetadata.Builder().setTitle("Example").build())
                                .build();
        }
    }
}
