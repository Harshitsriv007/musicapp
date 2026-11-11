import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { Artwork } from '../components/Artwork';
import { ChevronIcon, PopOutIcon } from '../components/Icons';
import { LikeButton } from '../components/LikeButton';
import { formatDuration } from '../lib/format';
import { usePlayer } from './PlayerContext';
import { PlayerControls, SeekBar } from './PlayerControls';
import { usePictureInPicture } from './usePictureInPicture';

/** Compact "now playing" card – rendered inside the pop-out window. */
export function CompactPlayer() {
  const { current } = usePlayer();
  if (!current) return <div className="compact compact--empty">Nothing playing</div>;
  return (
    <div className="compact">
      <Artwork src={current.imageUrl} size={84} alt={current.title} />
      <div className="compact__body">
        <div className="compact__title" title={current.title}>{current.title}</div>
        <div className="compact__artist">{current.artist}</div>
        <PlayerControls size="sm" />
      </div>
    </div>
  );
}

export function MiniPlayer() {
  const p = usePlayer();
  const pip = usePictureInPicture();
  const [expanded, setExpanded] = useState(false);

  useEffect(() => {
    if (!expanded) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setExpanded(false);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [expanded]);

  if (!p.current) return null;
  const song = p.current;

  return (
    <>
      {expanded && (
        <section className="full-player" aria-label="Now playing">
          <button type="button" className="icon-btn full-player__close" aria-label="Close player" onClick={() => setExpanded(false)}>
            <ChevronIcon width={28} height={28} />
          </button>
          <div className="full-player__art">
            <Artwork src={song.imageUrl} size={300} alt={song.title} />
          </div>
          <div className="full-player__meta">
            <div>
              <h2>{song.title}</h2>
              <p>{song.artist}</p>
            </div>
            <LikeButton song={song} size={26} />
          </div>
          <SeekBar />
          <div className="full-player__times">
            <span>{formatDuration(p.currentTime)}</span>
            <span>{formatDuration(p.duration)}</span>
          </div>
          <PlayerControls size="lg" />
          {p.error && <p className="state--error" role="alert">{p.error}</p>}
        </section>
      )}

      <footer className="mini-player" aria-label="Mini player">
        <div className="mini-player__progress" style={{ width: p.duration ? `${(p.currentTime / p.duration) * 100}%` : '0%' }} />
        <button type="button" className="mini-player__info" onClick={() => setExpanded(true)} aria-label="Open full player">
          <Artwork src={song.imageUrl} size={44} alt="" />
          <span className="mini-player__text">
            <span className="mini-player__title" data-testid="now-playing-title">{song.title}</span>
            <span className="mini-player__artist">{p.error ?? song.artist}</span>
          </span>
        </button>
        <LikeButton song={song} />
        <PlayerControls size="md" />
        {pip.supported && (
          <button
            type="button"
            className="icon-btn hide-mobile"
            aria-label={pip.pipWindow ? 'Close pop-out player' : 'Pop out mini player'}
            title="Pop out – keeps a small player on top while you work"
            onClick={() => (pip.pipWindow ? pip.close() : void pip.open())}
          >
            <PopOutIcon />
          </button>
        )}
      </footer>

      {pip.pipWindow && createPortal(<CompactPlayer />, pip.pipWindow.document.body)}
    </>
  );
}
