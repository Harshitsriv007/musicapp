import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { FolderFilter, Song } from '../api/types';
import { Artwork } from '../components/Artwork';
import { DownloadAllButton } from '../components/DownloadButton';
import { PlayIcon } from '../components/Icons';
import { LikeAllButton } from '../components/LikeAllButton';
import { SongList } from '../components/SongList';
import { useDownloads } from '../hooks/DownloadsContext';
import { folderTitle, href, TYPE_OF } from '../lib/routes';
import { usePlayer } from '../player/PlayerContext';

const PAGE = 100;
const MAX_SONGS = 1000;
const KIND: Record<FolderFilter, string> = { decade: 'Decade', artist: 'Artist', movie: 'Movie / album' };

/** "1964 (orig.)" when MusicBrainz knows the original release, else the upload year. */
function releaseYear(song: Song) {
  if (song.originalReleaseDate) return `${song.originalReleaseDate.slice(0, 4)} (orig.)`;
  return song.releaseDate?.slice(0, 4) ?? 'year unknown';
}

/** One folder: its songs with like / download per song, plus Play all, Like all and Download all. */
export function FolderView({ filter, folderKey }: { filter: FolderFilter; folderKey: string }) {
  const [songs, setSongs] = useState<Song[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const player = usePlayer();
  const { withStatus } = useDownloads();

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setSongs([]);
    (async () => {
      const all: Song[] = [];
      for (let page = 0; all.length < MAX_SONGS; page++) {
        const res = await api.songs('', page, PAGE, { [filter]: folderKey });
        all.push(...res.content);
        if (page + 1 >= res.totalPages) break;
      }
      return all;
    })()
      .then((all) => {
        if (cancelled) return;
        setSongs(all);
        setError(null);
      })
      .catch(() => !cancelled && setError('Could not load this folder'))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [filter, folderKey]);

  const cover = songs.find((s) => s.imageUrl)?.imageUrl ?? null;

  return (
    <section aria-labelledby="folder-heading">
      <a className="back-link" href={href({ view: 'folders', type: TYPE_OF[filter] })}>
        ← All {TYPE_OF[filter]}
      </a>
      <div className="folder-head">
        <Artwork src={cover} size={132} />
        <div className="folder-head__text">
          <p className="folder-head__kind">
            {filter === 'decade' && folderKey === 'latest' ? 'Released in the last 12 months' : KIND[filter]}
          </p>
          <h1 id="folder-heading">{folderTitle(filter, folderKey)}</h1>
          <p className="view-head__sub">{loading ? 'Loading…' : `${songs.length} ${songs.length === 1 ? 'song' : 'songs'}`}</p>
          <div className="folder-head__actions">
            <button
              type="button"
              className="btn"
              disabled={songs.length === 0}
              onClick={() => player.playSong(withStatus(songs[0]), songs.map(withStatus))}
            >
              <PlayIcon width={16} height={16} /> Play all
            </button>
            <LikeAllButton songs={songs} />
            <DownloadAllButton songs={songs} />
          </div>
        </div>
      </div>
      <SongList
        songs={songs}
        loading={loading}
        error={error}
        empty="This folder is empty."
        meta={filter === 'decade' ? releaseYear : undefined}
      />
    </section>
  );
}
