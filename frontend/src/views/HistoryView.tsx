import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { HistoryEntry } from '../api/types';
import { SongList } from '../components/SongList';
import { timeAgo } from '../lib/format';

export function HistoryView({ version }: { version: number }) {
  const [entries, setEntries] = useState<HistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api
      .history()
      .then((res) => !cancelled && (setEntries(res.content), setError(null)))
      .catch(() => !cancelled && setError('Could not load history'))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [version]);

  const clear = async () => {
    if (!window.confirm('Clear your listening history?')) return;
    await api.clearHistory();
    setEntries([]);
  };

  return (
    <section aria-labelledby="history-heading">
      <div className="view-head">
        <h1 id="history-heading">History</h1>
        {entries.length > 0 && (
          <button type="button" className="btn btn--ghost" onClick={() => void clear()}>
            Clear
          </button>
        )}
      </div>
      <SongList
        songs={entries.map((e) => e.song)}
        rowKey={(_, i) => entries[i].id}
        meta={(_, i) => timeAgo(entries[i].playedAt)}
        loading={loading}
        error={error}
        empty="Songs you play will show up here."
      />
    </section>
  );
}
