import { useEffect, useRef, useState } from 'react';
import { api } from '../api/client';
import type { SearchResult } from '../api/types';
import { FolderCard } from '../components/FolderCard';
import { SongList } from '../components/SongList';
import { href } from '../lib/routes';

const DEBOUNCE_MS = 250;

/**
 * One search box for songs (by name, artist or movie), artist folders and movie folders.
 * The query lives in the URL (#search/kesariya) so results can be shared and the back button works.
 */
export function SearchView({ initialQuery }: { initialQuery: string }) {
  const [query, setQuery] = useState(initialQuery);
  const [result, setResult] = useState<SearchResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const input = useRef<HTMLInputElement>(null);

  useEffect(() => input.current?.focus(), []);

  useEffect(() => {
    const q = query.trim();
    // keep the URL in step without adding a history entry per keystroke
    const target = href({ view: 'search', q });
    if (window.location.hash !== target) window.history.replaceState(null, '', target);

    if (!q) {
      setResult(null);
      setLoading(false);
      return;
    }
    const controller = new AbortController();
    setLoading(true);
    const timer = setTimeout(() => {
      api
        .search(q, { signal: controller.signal })
        .then((r) => {
          setResult(r);
          setError(null);
        })
        .catch((e: unknown) => {
          if (!(e instanceof DOMException && e.name === 'AbortError')) setError('Search failed');
        })
        .finally(() => !controller.signal.aborted && setLoading(false));
    }, DEBOUNCE_MS);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [query]);

  const nothing = result && result.songs.length === 0 && result.artists.length === 0 && result.movies.length === 0;

  return (
    <section aria-labelledby="search-heading">
      <div className="view-head">
        <h1 id="search-heading">Search</h1>
      </div>
      <input
        ref={input}
        className="search search--big"
        type="search"
        placeholder="Songs, artists or movies"
        aria-label="Search songs, artists or movies"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        autoComplete="off"
        enterKeyHint="search"
      />

      {!query.trim() && <p className="state">Find a song by its name, an artist, or a movie like “Brahmastra”.</p>}
      {error && <p className="state state--error" role="alert">{error}</p>}
      {nothing && !loading && <p className="state">No songs, artists or movies match “{result.query}”.</p>}

      {result && result.artists.length > 0 && (
        <section className="search-group" aria-labelledby="search-artists">
          <h2 id="search-artists">Artists</h2>
          <div className="shelf" role="list">
            {result.artists.map((f) => (
              <div role="listitem" key={f.key}>
                <FolderCard folder={f} filter="artist" />
              </div>
            ))}
          </div>
        </section>
      )}

      {result && result.movies.length > 0 && (
        <section className="search-group" aria-labelledby="search-movies">
          <h2 id="search-movies">Movies & albums</h2>
          <div className="shelf" role="list">
            {result.movies.map((f) => (
              <div role="listitem" key={f.key}>
                <FolderCard folder={f} filter="movie" />
              </div>
            ))}
          </div>
        </section>
      )}

      {result && result.songs.length > 0 && (
        <section className="search-group" aria-labelledby="search-songs">
          <h2 id="search-songs">Songs</h2>
          <SongList songs={result.songs} meta={(s) => s.movie ?? null} />
        </section>
      )}
      {loading && !result && <SongList songs={[]} loading />}
    </section>
  );
}
