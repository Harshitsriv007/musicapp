import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { LikedEntry } from '../api/types';
import { DownloadAllButton } from '../components/DownloadButton';
import { SongList } from '../components/SongList';
import { useLikes } from '../hooks/LikesContext';

export function LikedView() {
  const { version } = useLikes();
  const [entries, setEntries] = useState<LikedEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api
      .liked()
      .then((res) => !cancelled && (setEntries(res.content), setError(null)))
      .catch(() => !cancelled && setError('Could not load liked songs'))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [version]);

  return (
    <section aria-labelledby="liked-heading">
      <div className="view-head">
        <div>
          <h1 id="liked-heading">Liked songs</h1>
          <p className="view-head__sub">{entries.length} {entries.length === 1 ? 'song' : 'songs'}</p>
        </div>
        <DownloadAllButton songs={entries.map((e) => e.song)} />
      </div>
      <SongList
        songs={entries.map((e) => e.song)}
        loading={loading}
        error={error}
        empty="Tap the heart on any song to save it here."
      />
    </section>
  );
}
