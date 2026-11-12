import { WifiOffIcon } from './Icons';

/**
 * Shown when the server could not be reached. Never shows an empty list instead: an
 * unreachable server must not look like "there are no songs".
 */
export function ErrorState({ message, onRetry, compact }: { message: string; onRetry?: () => void; compact?: boolean }) {
  return (
    <div className={`error-state${compact ? ' error-state--compact' : ''}`} role="alert">
      {!compact && <WifiOffIcon width={28} height={28} />}
      <div>
        <p className="error-state__title">{message}</p>
        {!compact && <p className="error-state__text">Check your internet connection and that the Dhun server is running.</p>}
      </div>
      <button type="button" className="btn btn--ghost btn--sm" onClick={onRetry ?? (() => window.location.reload())}>
        Try again
      </button>
    </div>
  );
}
