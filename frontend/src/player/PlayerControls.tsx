import { NextIcon, PauseIcon, PlayIcon, PrevIcon, StopIcon } from '../components/Icons';
import { usePlayer } from './PlayerContext';

/** Play / pause / next / previous / stop – shared by the mini player, the full player and the pop-out window. */
export function PlayerControls({ size = 'md', showStop = true }: { size?: 'sm' | 'md' | 'lg'; showStop?: boolean }) {
  const p = usePlayer();
  const icon = size === 'lg' ? 30 : size === 'sm' ? 18 : 22;
  return (
    <div className={`controls controls--${size}`} role="group" aria-label="Playback controls">
      <button type="button" className="icon-btn" aria-label="Previous" onClick={p.previous} disabled={!p.current}>
        <PrevIcon width={icon} height={icon} />
      </button>
      <button
        type="button"
        className="icon-btn play-btn"
        aria-label={p.isPlaying ? 'Pause' : 'Play'}
        onClick={p.togglePlay}
        disabled={!p.current}
      >
        {p.isPlaying ? <PauseIcon width={icon} height={icon} /> : <PlayIcon width={icon} height={icon} />}
      </button>
      <button type="button" className="icon-btn" aria-label="Next" onClick={p.next} disabled={!p.hasNext}>
        <NextIcon width={icon} height={icon} />
      </button>
      {showStop && (
        <button type="button" className="icon-btn" aria-label="Stop" onClick={p.stop} disabled={!p.current}>
          <StopIcon width={icon * 0.85} height={icon * 0.85} />
        </button>
      )}
    </div>
  );
}

export function SeekBar() {
  const p = usePlayer();
  const max = p.duration > 0 ? p.duration : 0;
  return (
    <input
      className="seek"
      type="range"
      min={0}
      max={max || 1}
      step={1}
      value={Math.min(p.currentTime, max || 1)}
      onChange={(e) => p.seek(Number(e.target.value))}
      disabled={!p.current || !max}
      aria-label="Seek"
      style={{ ['--progress' as string]: max ? `${(p.currentTime / max) * 100}%` : '0%' }}
    />
  );
}
