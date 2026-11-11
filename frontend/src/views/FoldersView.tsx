import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Folder, FolderType } from '../api/types';
import { FolderCard } from '../components/FolderCard';
import { FILTER_OF, href } from '../lib/routes';

const TITLES: Record<FolderType, string> = { decades: 'Songs by decade', artists: 'Artists', movies: 'Movies & albums' };

/** All folders of one kind (every year / artist / movie), with a filter box. */
export function FoldersView({ type }: { type: FolderType }) {
  const [folders, setFolders] = useState<Folder[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState('');

  useEffect(() => {
    let cancelled = false;
    setFolders(null);
    api
      .browse(type)
      .then((list) => !cancelled && setFolders(list))
      .catch(() => !cancelled && setError('Could not load folders'));
    return () => {
      cancelled = true;
    };
  }, [type]);

  const shown = (folders ?? []).filter((f) => f.title.toLowerCase().includes(filter.trim().toLowerCase()));

  return (
    <section aria-labelledby="folders-heading">
      <a className="back-link" href={href({ view: 'home' })}>← Home</a>
      <div className="view-head">
        <div>
          <h1 id="folders-heading">{TITLES[type]}</h1>
          {folders && <p className="view-head__sub">{folders.length} folders</p>}
        </div>
      </div>
      {type !== 'decades' && (
        <input
          className="search"
          type="search"
          placeholder={`Find ${type === 'artists' ? 'an artist' : 'a movie'}`}
          aria-label={`Filter ${TITLES[type]}`}
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
      )}
      {error && <p className="state state--error" role="alert">{error}</p>}
      {!folders && !error && <p className="state">Loading…</p>}
      {folders && shown.length === 0 && <p className="state">No folders here yet.</p>}
      <div className="folder-grid">
        {shown.map((f) => (
          <FolderCard key={f.key} folder={f} filter={FILTER_OF[type]} />
        ))}
      </div>
    </section>
  );
}
