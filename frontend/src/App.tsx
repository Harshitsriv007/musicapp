import { useEffect, useState } from 'react';
import { AccountMenu } from './components/AccountMenu';
import { ClockIcon, DownloadLineIcon, HeartLineIcon, HomeIcon, LibraryIcon, SearchIcon } from './components/Icons';
import { AccountProvider, useAccount } from './hooks/AccountContext';
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
import { SearchView } from './views/SearchView';
import { LikedView } from './views/LikedView';
import { LoginView } from './views/LoginView';
import { NewSongsView } from './views/NewSongsView';

const TABS = [
  { id: 'home', label: 'Home', icon: HomeIcon },
  { id: 'search', label: 'Search', icon: SearchIcon },
  { id: 'library', label: 'All songs', icon: LibraryIcon },
  { id: 'liked', label: 'Liked', icon: HeartLineIcon },
  { id: 'downloads', label: 'Downloads', icon: DownloadLineIcon },
  { id: 'history', label: 'History', icon: ClockIcon },
] as const;

export default function App() {
  return (
    <AccountProvider>
      <AccountScope />
    </AccountProvider>
  );
}

/** Only logged-in listeners see the app; switching account remounts it so every list reloads. */
function AccountScope() {
  const { username } = useAccount();
  return username ? <Shell key={username} /> : <LoginView />;
}

function Shell() {
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

  // "/" opens search from anywhere (unless you are already typing somewhere).
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const typing = e.target instanceof HTMLElement && e.target.closest('input, textarea, [contenteditable="true"]');
      if (e.key === '/' && !typing && !e.metaKey && !e.ctrlKey && !e.altKey) {
        e.preventDefault();
        window.location.hash = 'search';
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  return (
    <LikesProvider>
      <DownloadsProvider>
      <PlayerProvider onPlayRecorded={() => setHistoryVersion((v) => v + 1)}>
        <div className="app">
          <aside className="sidebar">
            <div className="sidebar__top">
              <a className="brand" href="#home" aria-label="Dhun home">
                <img src="/favicon.svg" alt="" width={30} height={30} />
                <span>Dhun</span>
              </a>
              <AccountMenu />
            </div>
            <nav className="nav" aria-label="Sections">
              {TABS.map(({ id, label, icon: Icon }) => (
                <a
                  key={id}
                  href={`#${id}`}
                  className={`nav__item${tab === id ? ' nav__item--active' : ''}`}
                  aria-current={tab === id ? 'page' : undefined}
                >
                  <Icon width={22} height={22} />
                  <span>{label}</span>
                </a>
              ))}
            </nav>
          </aside>
          <main className="content">
            {route.view === 'home' && <HomeView />}
            {route.view === 'new' && <NewSongsView />}
            {route.view === 'folders' && <FoldersView type={route.type} />}
            {route.view === 'folder' && <FolderView key={`${route.filter}/${route.key}`} filter={route.filter} folderKey={route.key} />}
            {route.view === 'search' && <SearchView key={route.q} initialQuery={route.q} />}
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
