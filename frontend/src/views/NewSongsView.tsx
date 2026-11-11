import { useState } from 'react';
import { api } from '../api/client';
import { SongList } from '../components/SongList';
import { useNewSongs } from '../hooks/useNewSongs';

export function NewSongsView() {
  const { songs, loading, error, freshIds, live, refresh } = useNewSongs();
  const [checking, setChecking] = useState(false);

  const checkNow = async () => {
    setChecking(true);
    try {
      await api.triggerIngestion();
      // New songs arrive through the live feed; also re-poll shortly in case SSE is blocked.
      setTimeout(() => void refresh(), 8000);
    } finally {
      setTimeout(() => setChecking(false), 3000);
    }
  };

  return (
    <section aria-labelledby="new-heading">
      <div className="view-head">
        <div>
          <h1 id="new-heading">New songs</h1>
          <p className="view-head__sub">
            <span className={`live-dot${live ? ' live-dot--on' : ''}`} aria-hidden="true" />
            {live ? 'Live – updates automatically' : 'Auto-refreshing'}
          </p>
        </div>
        <button type="button" className="btn" onClick={() => void checkNow()} disabled={checking}>
          {checking ? 'Checking…' : 'Check now'}
        </button>
      </div>
      <SongList
        songs={songs}
        loading={loading}
        error={error}
        badge={(s) => freshIds.has(s.id) && <span className="badge">NEW</span>}
        empty={<>No songs yet. The first fetch runs a few seconds after the server starts.</>}
      />
    </section>
  );
}
