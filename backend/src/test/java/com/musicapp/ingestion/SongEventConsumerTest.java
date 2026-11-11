package com.musicapp.ingestion;

import static com.musicapp.TestFixtures.event;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.musicapp.service.SongService;
import com.musicapp.service.SongUpdatesBroadcaster;
import com.musicapp.web.dto.SongDto;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SongEventConsumerTest {

    private final SongService songService = mock(SongService.class);
    private final SongUpdatesBroadcaster broadcaster = mock(SongUpdatesBroadcaster.class);
    private final SongEventConsumer consumer = new SongEventConsumer(songService, broadcaster);

    @Test
    void broadcastsNewlyCreatedSongs() {
        SongDto dto = new SongDto(1L, "Kesariya", "Arijit", null, null, null, null, "s", 1, null, "audius", null, false, "NONE");
        when(songService.upsert(any())).thenReturn(Optional.of(dto));

        consumer.onSongEvent(event("1", "Kesariya"));

        verify(broadcaster).songAdded(dto);
    }

    @Test
    void doesNotBroadcastUpdatesOfExistingSongs() {
        when(songService.upsert(any())).thenReturn(Optional.empty());

        consumer.onSongEvent(event("1", "Kesariya"));

        verify(broadcaster, never()).songAdded(any());
    }

    @Test
    void rejectsInvalidEventsWithoutTouchingTheDatabase() {
        SongEvent invalid = new SongEvent("audius", "1", " ", null, null, null, null, "s", null, null);

        assertThatThrownBy(() -> consumer.onSongEvent(invalid)).isInstanceOf(InvalidSongEventException.class);
        verifyNoInteractions(songService, broadcaster);
    }
}
