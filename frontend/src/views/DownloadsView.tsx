import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Song } from '../api/types';
import { SongList } from '../components/SongList';
import { useDownloads } from '../hooks/DownloadsContext';

export function DownloadsView() {
  const { version } = useDownloads();
  const [songs, setSongs] = useState<Song[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api
      .songs('', 0, 100, true)
      .then((res) => {
        if (cancelled) return;
        setSongs(res.content);
        setError(null);
      })
      .catch(() => !cancelled && setError('Could not load downloads'))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [version]);

  return (
    <section aria-labelledby="downloads-heading">
      <div className="view-head">
        <div>
          <h1 id="downloads-heading">Downloads</h1>
          <p className="view-head__sub">Stored on our server – plays instantly, no buffering</p>
        </div>
      </div>
      <SongList
        songs={songs}
        loading={loading}
        error={error}
        empty="Tap the download arrow on any song (or “Download all” on Liked) to keep it here."
      />
    </section>
  );
}
