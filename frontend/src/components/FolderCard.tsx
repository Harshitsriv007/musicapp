import type { Folder, FolderFilter } from '../api/types';
import { href } from '../lib/routes';
import { Artwork } from './Artwork';

const HUES = [18, 330, 265, 200, 150, 45, 300];

/** A folder tile: year folders get a big coloured number, artists a round photo, movies a poster. */
export function FolderCard({ folder, filter }: { folder: Folder; filter: FolderFilter }) {
  const count = `${folder.songCount} ${folder.songCount === 1 ? 'song' : 'songs'}`;
  const hue = HUES[Math.abs(hash(folder.key)) % HUES.length];
  return (
    <a
      className={`folder-card folder-card--${filter}`}
      href={href({ view: 'folder', filter, key: folder.key })}
      aria-label={`${folder.title}, ${count}`}
    >
      <div className="folder-card__cover">
        {filter === 'year' ? (
          <div className="year-tile" style={{ ['--hue' as string]: hue }}>
            <span>{folder.title}</span>
          </div>
        ) : (
          <Artwork src={folder.imageUrl} size={150} />
        )}
      </div>
      <div className="folder-card__title">{folder.title}</div>
      <div className="folder-card__sub">{count}</div>
    </a>
  );
}

function hash(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
  return h;
}
