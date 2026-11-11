package com.musicapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.musicapp.web.dto.SongDto;
import org.junit.jupiter.api.Test;

class SongUpdatesBroadcasterTest {

    @Test
    void tracksSubscribersAndSurvivesBroadcastWithoutOpenConnection() {
        var broadcaster = new SongUpdatesBroadcaster();
        broadcaster.subscribe();
        broadcaster.subscribe();
        assertThat(broadcaster.subscriberCount()).isEqualTo(2);

        // Emitters not bound to an HTTP response buffer events instead of failing.
        broadcaster.songAdded(new SongDto(1L, "A", null, null, null, null, null, "s", null, null, "audius", null, false, "NONE", null));
        broadcaster.heartbeat();

        assertThat(broadcaster.subscriberCount()).isEqualTo(2);
    }
}
