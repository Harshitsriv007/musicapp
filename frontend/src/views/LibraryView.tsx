import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Song } from '../api/types';
import { DownloadAllButton } from '../components/DownloadButton';
import { SongList } from '../components/SongList';

const PAGE_SIZE = 30;

export function LibraryView() {
  const [query, setQuery] = useState('');
  const [debounced, setDebounced] = useState('');
  const [songs, setSongs] = useState<Song[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const t = setTimeout(() => setDebounced(query.trim()), 300);
    return () => clearTimeout(t);
  }, [query]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    api
      .songs(debounced, page, PAGE_SIZE)
      .then((res) => {
        if (cancelled) return;
        setSongs((prev) => (page === 0 ? res.content : [...prev, ...res.content]));
        setHasMore(res.page + 1 < res.totalPages);
        setError(null);
      })
      .catch(() => !cancelled && setError('Could not load songs'))
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [debounced, page]);

  useEffect(() => setPage(0), [debounced]);

  return (
    <section aria-labelledby="library-heading">
      <div className="view-head">
        <h1 id="library-heading">All songs</h1>
        <DownloadAllButton songs={songs} />
      </div>
      <input
        className="search"
        type="search"
        placeholder="Search songs, artists, albums"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        aria-label="Search songs"
      />
      <SongList songs={songs} loading={loading} error={error} empty={debounced ? `No songs match “${debounced}”.` : undefined} />
      {hasMore && (
        <button type="button" className="btn btn--block" onClick={() => setPage((p) => p + 1)} disabled={loading}>
          {loading ? 'Loading…' : 'Load more'}
        </button>
      )}
    </section>
  );
}
