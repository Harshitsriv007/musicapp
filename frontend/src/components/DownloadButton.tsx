import type { Song } from '../api/types';
import { useDownloads } from '../hooks/DownloadsContext';
import { DownloadedIcon, DownloadIcon } from './Icons';

export function DownloadButton({ song, size = 20 }: { song: Song; size?: number }) {
  const { statusOf, download, remove } = useDownloads();
  const status = statusOf(song);
  // The provider's terms (e.g. Jamendo) forbid storing this track.
  if (song.downloadAllowed === false && status === 'NONE') return null;

  if (status === 'PENDING' || status === 'DOWNLOADING') {
    return (
      <span className="icon-btn dl-btn" role="status" aria-label={`Downloading ${song.title}`} title="Downloading…">
        <span className="spinner" style={{ width: size - 4, height: size - 4 }} />
      </span>
    );
  }
  if (status === 'DOWNLOADED') {
    return (
      <button
        type="button"
        className="icon-btn dl-btn dl-btn--done"
        aria-label={`Remove download of ${song.title}`}
        title="Downloaded – plays from our server. Click to remove."
        onClick={(e) => {
          e.stopPropagation();
          void remove(song);
        }}
      >
        <DownloadedIcon width={size} height={size} />
      </button>
    );
  }
  const failed = status === 'FAILED';
  return (
    <button
      type="button"
      className={`icon-btn dl-btn${failed ? ' dl-btn--failed' : ''}`}
      aria-label={failed ? `Retry download of ${song.title}` : `Download ${song.title}`}
      title={failed ? 'Download failed – retry' : 'Download – play later without buffering'}
      onClick={(e) => {
        e.stopPropagation();
        void download([song]);
      }}
    >
      <DownloadIcon width={size} height={size} />
    </button>
  );
}

/** "Download all" for a list of songs (Liked, All songs). */
export function DownloadAllButton({ songs }: { songs: Song[] }) {
  const { statusOf, download } = useDownloads();
  const remaining = songs.filter((s) => s.downloadAllowed !== false && ['NONE', 'FAILED'].includes(statusOf(s)));
  const busy = songs.some((s) => ['PENDING', 'DOWNLOADING'].includes(statusOf(s)));
  if (songs.length === 0) return null;
  return (
    <button type="button" className="btn btn--ghost" disabled={remaining.length === 0} onClick={() => void download(remaining)}>
      <DownloadIcon width={16} height={16} />
      {remaining.length === 0 ? (busy ? 'Downloading…' : 'All downloaded') : `Download all (${remaining.length})`}
    </button>
  );
}
