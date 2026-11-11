import { useEffect, useState } from 'react';
import { DownloadsProvider } from './hooks/DownloadsContext';
import { LikesProvider } from './hooks/LikesContext';
import { MiniPlayer } from './player/MiniPlayer';
import { PlayerProvider } from './player/PlayerContext';
import { DownloadsView } from './views/DownloadsView';
import { parseHash, tabOf, type Route } from './lib/routes';
import { FoldersView } from './views/FoldersView';
import { FolderView } from './views/FolderView';
import { HistoryView } from './views/HistoryView';
import { HomeView } from './views/HomeView';
import { LibraryView } from './views/LibraryView';
import { LikedView } from './views/LikedView';
import { NewSongsView } from './views/NewSongsView';

const TABS = [
  { id: 'home', label: 'Home' },
  { id: 'library', label: 'All songs' },
  { id: 'liked', label: 'Liked' },
  { id: 'downloads', label: 'Downloads' },
  { id: 'history', label: 'History' },
] as const;

export default function App() {
  const [route, setRoute] = useState<Route>(() => parseHash(window.location.hash));
  const [historyVersion, setHistoryVersion] = useState(0);
  const tab = tabOf(route);

  useEffect(() => {
    const onHash = () => {
      setRoute(parseHash(window.location.hash));
      window.scrollTo({ top: 0 });
    };
    window.addEventListener('hashchange', onHash);
    return () => window.removeEventListener('hashchange', onHash);
  }, []);

  return (
    <LikesProvider>
      <DownloadsProvider>
      <PlayerProvider onPlayRecorded={() => setHistoryVersion((v) => v + 1)}>
        <div className="app">
          <header className="topbar">
            <a className="brand" href="#home" aria-label="Dhun home">
              <img src="/favicon.svg" alt="" width={28} height={28} />
              <span>Dhun</span>
            </a>
            <nav className="tabs" aria-label="Sections">
              {TABS.map((t) => (
                <a
                  key={t.id}
                  href={`#${t.id}`}
                  className={`tab${tab === t.id ? ' tab--active' : ''}`}
                  aria-current={tab === t.id ? 'page' : undefined}
                >
                  {t.label}
                </a>
              ))}
            </nav>
          </header>
          <main className="content">
            {route.view === 'home' && <HomeView />}
            {route.view === 'new' && <NewSongsView />}
            {route.view === 'folders' && <FoldersView type={route.type} />}
            {route.view === 'folder' && <FolderView key={`${route.filter}/${route.key}`} filter={route.filter} folderKey={route.key} />}
            {route.view === 'library' && <LibraryView />}
            {route.view === 'liked' && <LikedView />}
            {route.view === 'downloads' && <DownloadsView />}
            {route.view === 'history' && <HistoryView version={historyVersion} />}
          </main>
          <MiniPlayer />
        </div>
      </PlayerProvider>
      </DownloadsProvider>
    </LikesProvider>
  );
}
