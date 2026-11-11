import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Folder, FolderType } from '../api/types';
import { Artwork } from '../components/Artwork';
import { ErrorState } from '../components/ErrorState';
import { useAccount } from '../hooks/AccountContext';
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
  const { songs: newSongs, loading, error: newError, refresh } = useNewSongs();
  const player = usePlayer();
  const { withStatus } = useDownloads();
  const { username } = useAccount();
  const [folders, setFolders] = useState<Partial<Record<FolderType, Folder[]>>>({});
  const [failed, setFailed] = useState<Partial<Record<FolderType, boolean>>>({});

  const load = useCallback(() => {
    SECTIONS.forEach(({ type }) =>
      api
        .browse(type)
        .then((list) => {
          setFolders((prev) => ({ ...prev, [type]: list }));
          setFailed((prev) => ({ ...prev, [type]: false }));
        })
        // Keep what we had; an unreachable server must not look like "no artists".
        .catch(() => setFailed((prev) => ({ ...prev, [type]: true }))),
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

  const retryAll = () => {
    void refresh();
    load();
  };

  return (
    <div className="home">
      <header className="home__hero">
        <p className="home__eyebrow">{greeting()}</p>
        <h1>{username ? `Welcome back, ${username}` : 'Welcome to Dhun'}</h1>
        <p className="home__lead">
          Fresh Bollywood releases, your favourite artists and every decade in one place.
        </p>
      </header>
      <section aria-labelledby="home-new">
        <div className="shelf-head">
          <h2 id="home-new">New songs</h2>
          <a href={href({ view: 'new' })}>See all</a>
        </div>
        <div className="shelf" role="list">
          {loading && newSongs.length === 0 && <ShelfSkeleton />}
          {!loading && newSongs.length === 0 && newError && (
            <div className="shelf__empty"><ErrorState compact message="Couldn’t load new songs" onRetry={retryAll} /></div>
          )}
          {!loading && newSongs.length === 0 && !newError && (
            <div className="shelf__empty">No songs yet – the first fetch runs a few seconds after the server starts.</div>
          )}
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
              {!list && !failed[type] && <ShelfSkeleton round={type === 'artists'} />}
              {!list && failed[type] && (
                <div className="shelf__empty"><ErrorState compact message={`Couldn’t load ${title.toLowerCase()}`} onRetry={retryAll} /></div>
              )}
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

function ShelfSkeleton({ round }: { round?: boolean }) {
  return (
    <>
      {Array.from({ length: 6 }, (_, i) => (
        <div key={i} className="card-skeleton" aria-hidden="true">
          <div className={`card-skeleton__cover${round ? ' card-skeleton__cover--round' : ''}`} />
          <div className="card-skeleton__line" />
          <div className="card-skeleton__line card-skeleton__line--short" />
        </div>
      ))}
    </>
  );
}

function greeting(now = new Date()): string {
  const h = now.getHours();
  return h < 5 ? 'Good night' : h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening';
}
