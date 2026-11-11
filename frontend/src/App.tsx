import { useEffect, useState } from 'react';
import { DownloadsProvider } from './hooks/DownloadsContext';
import { LikesProvider } from './hooks/LikesContext';
import { MiniPlayer } from './player/MiniPlayer';
import { PlayerProvider } from './player/PlayerContext';
import { DownloadsView } from './views/DownloadsView';
import { HistoryView } from './views/HistoryView';
import { LibraryView } from './views/LibraryView';
import { LikedView } from './views/LikedView';
import { NewSongsView } from './views/NewSongsView';

const TABS = [
  { id: 'new', label: 'New' },
  { id: 'library', label: 'All songs' },
  { id: 'liked', label: 'Liked' },
  { id: 'downloads', label: 'Downloads' },
  { id: 'history', label: 'History' },
] as const;
type TabId = (typeof TABS)[number]['id'];

const tabFromHash = (): TabId => {
  const h = window.location.hash.replace('#', '');
  return (TABS.find((t) => t.id === h)?.id ?? 'new') as TabId;
};

export default function App() {
  const [tab, setTab] = useState<TabId>(tabFromHash);
  const [historyVersion, setHistoryVersion] = useState(0);

  useEffect(() => {
    const onHash = () => setTab(tabFromHash());
    window.addEventListener('hashchange', onHash);
    return () => window.removeEventListener('hashchange', onHash);
  }, []);

  return (
    <LikesProvider>
      <DownloadsProvider>
      <PlayerProvider onPlayRecorded={() => setHistoryVersion((v) => v + 1)}>
        <div className="app">
          <header className="topbar">
            <a className="brand" href="#new" aria-label="Dhun home">
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
            {tab === 'new' && <NewSongsView />}
            {tab === 'library' && <LibraryView />}
            {tab === 'liked' && <LikedView />}
            {tab === 'downloads' && <DownloadsView />}
            {tab === 'history' && <HistoryView version={historyVersion} />}
          </main>
          <MiniPlayer />
        </div>
      </PlayerProvider>
      </DownloadsProvider>
    </LikesProvider>
  );
}
