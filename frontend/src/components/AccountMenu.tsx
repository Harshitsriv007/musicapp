import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { useAccount } from '../hooks/AccountContext';
import { UserIcon } from './Icons';

type Mode = 'login' | 'signup';

/** Top-bar account button: log in to see the same likes and history on every device. */
export function AccountMenu() {
  const { username, logOut } = useAccount();
  const [open, setOpen] = useState(false);

  return (
    <>
      <button
        type="button"
        className="account-btn"
        onClick={() => setOpen(true)}
        aria-haspopup="dialog"
        aria-label={username ? `Account: ${username}` : 'Log in'}
      >
        <UserIcon width={18} height={18} />
        <span className="account-btn__label">{username ?? 'Log in'}</span>
      </button>
      {open && (
        <div className="modal" onClick={(e) => e.target === e.currentTarget && setOpen(false)}>
          {username ? (
            <AccountPanel username={username} onClose={() => setOpen(false)} onLogOut={() => void logOut().finally(() => setOpen(false))} />
          ) : (
            <AuthForm onClose={() => setOpen(false)} />
          )}
        </div>
      )}
    </>
  );
}

function useEscape(onClose: () => void) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
}

function AccountPanel({ username, onClose, onLogOut }: { username: string; onClose: () => void; onLogOut: () => void }) {
  const titleId = useId();
  useEscape(onClose);
  return (
    <div className="modal__card" role="dialog" aria-modal="true" aria-labelledby={titleId}>
      <h2 id={titleId}>{username}</h2>
      <p className="modal__text">Your liked songs and history are saved to your account, so they show up on any device you log in on.</p>
      <div className="modal__actions">
        <button type="button" className="btn btn--ghost" onClick={onClose}>Close</button>
        <button type="button" className="btn" onClick={onLogOut} autoFocus>Log out</button>
      </div>
    </div>
  );
}

function AuthForm({ onClose }: { onClose: () => void }) {
  const { logIn, signUp } = useAccount();
  const [mode, setMode] = useState<Mode>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const titleId = useId();
  const userRef = useRef<HTMLInputElement>(null);
  useEscape(onClose);

  useEffect(() => userRef.current?.focus(), []);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await (mode === 'login' ? logIn : signUp)(username.trim(), password);
      onClose();
    } catch (err) {
      setError(err instanceof ApiError && err.status !== 500 ? err.message : 'Something went wrong, please try again.');
      setBusy(false);
    }
  };

  const signingUp = mode === 'signup';
  return (
    <form className="modal__card" role="dialog" aria-modal="true" aria-labelledby={titleId} onSubmit={(e) => void submit(e)}>
      <h2 id={titleId}>{signingUp ? 'Create account' : 'Log in'}</h2>
      <p className="modal__text">
        Keep your liked songs and history on every device. What you liked and played here is moved into your account.
      </p>
      <label className="field">
        <span>Username</span>
        <input
          ref={userRef}
          className="search"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
          autoCapitalize="none"
          spellCheck={false}
          required
          minLength={3}
          maxLength={32}
        />
      </label>
      <label className="field">
        <span>Password</span>
        <input
          className="search"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete={signingUp ? 'new-password' : 'current-password'}
          required
          minLength={signingUp ? 8 : undefined}
        />
      </label>
      {signingUp && <p className="modal__hint">3–32 letters, digits, “.”, “_” or “-”. Password at least 8 characters.</p>}
      {error && <p className="modal__error" role="alert">{error}</p>}
      <div className="modal__actions">
        <button
          type="button"
          className="btn btn--ghost"
          onClick={() => {
            setMode(signingUp ? 'login' : 'signup');
            setError(null);
          }}
        >
          {signingUp ? 'I have an account' : 'Create account'}
        </button>
        <button type="submit" className="btn" disabled={busy}>
          {busy ? 'Please wait…' : signingUp ? 'Sign up' : 'Log in'}
        </button>
      </div>
    </form>
  );
}
