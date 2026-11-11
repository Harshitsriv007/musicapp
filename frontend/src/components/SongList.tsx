import type { ReactNode } from 'react';
import type { Song } from '../api/types';
import { formatDuration } from '../lib/format';
import { useDownloads } from '../hooks/DownloadsContext';
import { usePlayer } from '../player/PlayerContext';
import { Artwork } from './Artwork';
import { DownloadButton } from './DownloadButton';
import { LikeButton } from './LikeButton';

interface Props {
  songs: Song[];
  loading?: boolean;
  error?: string | null;
  empty?: ReactNode;
  /** Optional extra text per row, e.g. "played 5 min ago". */
  meta?: (song: Song, index: number) => ReactNode;
  badge?: (song: Song) => ReactNode;
  /** Key per row – history can contain the same song twice. */
  rowKey?: (song: Song, index: number) => string | number;
}

export function SongList({ songs, loading, error, empty, meta, badge, rowKey }: Props) {
  const player = usePlayer();
  const { withStatus } = useDownloads();

  if (loading && songs.length === 0) {
    return (
      <ul className="song-list" aria-busy="true" aria-label="Loading songs">
        {Array.from({ length: 6 }, (_, i) => (
          <li key={i} className="song-row song-row--skeleton" />
        ))}
      </ul>
    );
  }
  if (error && songs.length === 0) return <p className="state state--error" role="alert">{error}</p>;
  if (songs.length === 0) return <div className="state">{empty ?? 'No songs yet.'}</div>;

  // The queue never contains duplicates, so "next" behaves predictably from history lists.
  // Each song carries its latest download status so downloaded songs play from our own DB.
  const queue = songs.filter((s, i) => songs.findIndex((o) => o.id === s.id) === i).map(withStatus);

  return (
    <ul className="song-list">
      {songs.map((song, i) => {
        const active = player.current?.id === song.id;
        return (
          <li key={rowKey ? rowKey(song, i) : song.id}>
            <div
              role="button"
              tabIndex={0}
              className={`song-row${active ? ' song-row--active' : ''}`}
              aria-current={active ? 'true' : undefined}
              aria-label={`Play ${song.title}${song.artist ? ` by ${song.artist}` : ''}`}
              onClick={() => (active ? player.togglePlay() : player.playSong(withStatus(song), queue))}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  if (active) player.togglePlay();
                  else player.playSong(withStatus(song), queue);
                }
              }}
            >
              <div className="song-row__art">
                <Artwork src={song.imageUrl} size={48} />
                {active && player.isPlaying && (
                  <span className="eq" aria-hidden="true"><i /><i /><i /></span>
                )}
              </div>
              <div className="song-row__text">
                <span className="song-row__title">
                  {song.title}
                  {badge?.(song)}
                </span>
                <span className="song-row__sub">
                  {song.artist ?? 'Unknown artist'}
                  {meta && <> · {meta(song, i)}</>}
                </span>
              </div>
              <span className="song-row__duration">{formatDuration(song.durationSeconds)}</span>
              <LikeButton song={song} />
              <DownloadButton song={song} />
            </div>
          </li>
        );
      })}
    </ul>
  );
}
