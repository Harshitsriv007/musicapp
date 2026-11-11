import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Folder, FolderType } from '../api/types';
import { Artwork } from '../components/Artwork';
import { FolderCard } from '../components/FolderCard';
import { useDownloads } from '../hooks/DownloadsContext';
import { useNewSongs } from '../hooks/useNewSongs';
import { subscribe } from '../lib/liveEvents';
import { FILTER_OF, href } from '../lib/routes';
import { usePlayer } from '../player/PlayerContext';

const SHELF = 12;
const SECTIONS: { type: FolderType; title: string; empty: string }[] = [
  { type: 'decades', title: 'Songs by decade', empty: 'No release dates yet.' },
  { type: 'artists', title: 'Artists', empty: 'No artists yet.' },
  {
    type: 'movies',
    title: 'Movies & albums',
    empty: 'No movie info yet – songs titled like “Song (From “Movie”)” will appear here.',
  },
];

/** Home screen: New songs plus folders by year, artist and movie. */
export function HomeView() {
  const { songs: newSongs, loading } = useNewSongs();
  const player = usePlayer();
  const { withStatus } = useDownloads();
  const [folders, setFolders] = useState<Partial<Record<FolderType, Folder[]>>>({});

  const load = useCallback(() => {
    SECTIONS.forEach(({ type }) =>
      api
        .browse(type)
        .then((list) => setFolders((prev) => ({ ...prev, [type]: list })))
        .catch(() => setFolders((prev) => ({ ...prev, [type]: prev[type] ?? [] }))),
    );
  }, []);

  useEffect(load, [load]);

  // A song arriving live can create a new folder or change counts.
  useEffect(() => {
    let timer: ReturnType<typeof setTimeout> | undefined;
    const off = subscribe('song-added', () => {
      clearTimeout(timer);
      timer = setTimeout(load, 1500);
    });
    return () => {
      clearTimeout(timer);
      off();
    };
  }, [load]);

  const queue = newSongs.map(withStatus);

  return (
    <div className="home">
      <section aria-labelledby="home-new">
        <div className="shelf-head">
          <h2 id="home-new">New songs</h2>
          <a href={href({ view: 'new' })}>See all</a>
        </div>
        <div className="shelf" role="list">
          {loading && newSongs.length === 0 && <div className="shelf__empty">Loading…</div>}
          {newSongs.slice(0, SHELF).map((song) => (
            <div role="listitem" key={song.id}>
              <button
                type="button"
                className={`song-card${player.current?.id === song.id ? ' song-card--active' : ''}`}
                aria-label={`Play ${song.title}${song.artist ? ` by ${song.artist}` : ''}`}
                onClick={() => player.playSong(withStatus(song), queue)}
              >
                <Artwork src={song.imageUrl} size={150} />
                <span className="folder-card__title">{song.title}</span>
                <span className="folder-card__sub">{song.artist}</span>
              </button>
            </div>
          ))}
        </div>
      </section>

      {SECTIONS.map(({ type, title, empty }) => {
        const list = folders[type];
        return (
          <section key={type} aria-labelledby={`home-${type}`}>
            <div className="shelf-head">
              <h2 id={`home-${type}`}>{title}</h2>
              {list && list.length > 0 && <a href={href({ view: 'folders', type })}>See all</a>}
            </div>
            <div className="shelf" role="list">
              {!list && <div className="shelf__empty">Loading…</div>}
              {list && list.length === 0 && <div className="shelf__empty">{empty}</div>}
              {list?.slice(0, SHELF).map((f) => (
                <div role="listitem" key={f.key}>
                  <FolderCard folder={f} filter={FILTER_OF[type]} />
                </div>
              ))}
            </div>
          </section>
        );
      })}
    </div>
  );
}
