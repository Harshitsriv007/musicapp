import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { EyeIcon, EyeOffIcon, HeartLineIcon, DownloadLineIcon, ClockIcon } from '../components/Icons';
import { useAccount } from '../hooks/AccountContext';

type Mode = 'login' | 'signup';

const FEATURES = [
  { icon: HeartLineIcon, text: 'Liked songs saved to your account' },
  { icon: ClockIcon, text: 'Listening history on every device' },
  { icon: DownloadLineIcon, text: 'Downloads that play instantly' },
];

/** The first screen: log in or create an account. Nothing else is shown until you do. */
export function LoginView() {
  const { logIn, signUp } = useAccount();
  const [mode, setMode] = useState<Mode>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const userRef = useRef<HTMLInputElement>(null);
  const hintId = useId();
  const errorId = useId();
  const signingUp = mode === 'signup';

  useEffect(() => userRef.current?.focus(), [mode]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await (signingUp ? signUp : logIn)(username.trim(), password);
    } catch (err) {
      setError(
        err instanceof ApiError && err.status < 500
          ? err.message
          : err instanceof ApiError
            ? 'Something went wrong on our side. Please try again.'
            : 'Can’t reach the Dhun server. Check your connection and try again.',
      );
      setBusy(false);
    }
  };

  const switchMode = () => {
    setMode(signingUp ? 'login' : 'signup');
    setError(null);
  };

  return (
    <div className="auth">
      <aside className="auth__hero" aria-hidden="true">
        <div className="auth__hero-inner">
          <div className="brand brand--light">
            <img src="/favicon.svg" alt="" width={36} height={36} />
            <span>Dhun</span>
          </div>
          <h2 className="auth__tagline">Your Bollywood music, everywhere you listen.</h2>
          <ul className="auth__features">
            {FEATURES.map(({ icon: Icon, text }) => (
              <li key={text}>
                <span className="auth__feature-icon"><Icon width={18} height={18} /></span>
                {text}
              </li>
            ))}
          </ul>
        </div>
      </aside>

      <main className="auth__panel">
        <form className="auth__form" onSubmit={(e) => void submit(e)} aria-describedby={error ? errorId : undefined}>
          <div className="brand auth__mobile-brand">
            <img src="/favicon.svg" alt="" width={32} height={32} />
            <span>Dhun</span>
          </div>
          <h1>{signingUp ? 'Create your account' : 'Welcome back'}</h1>
          <p className="auth__lead">
            {signingUp ? 'Sign up to keep your music in sync on all your devices.' : 'Log in to continue listening.'}
          </p>

          <label className="field">
            <span className="field__label">Username</span>
            <input
              ref={userRef}
              className="input"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              required
              minLength={3}
              maxLength={32}
              aria-describedby={signingUp ? hintId : undefined}
            />
          </label>

          <label className="field">
            <span className="field__label">Password</span>
            <span className="input-wrap">
              <input
                className="input"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete={signingUp ? 'new-password' : 'current-password'}
                required
                minLength={signingUp ? 8 : undefined}
              />
              <button
                type="button"
                className="input-wrap__toggle"
                onClick={() => setShowPassword((s) => !s)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOffIcon width={18} height={18} /> : <EyeIcon width={18} height={18} />}
              </button>
            </span>
          </label>

          {signingUp && (
            <p className="field__hint" id={hintId}>
              Username: 3–32 letters, digits, “.”, “_” or “-”. Password: at least 8 characters.
            </p>
          )}
          {error && (
            <p className="form-error" role="alert" id={errorId}>
              {error}
            </p>
          )}

          <button type="submit" className="btn btn--lg btn--block" disabled={busy}>
            {busy ? <span className="spinner spinner--light" style={{ width: 16, height: 16 }} /> : null}
            {busy ? 'Please wait…' : signingUp ? 'Create account' : 'Log in'}
          </button>

          <p className="auth__switch">
            {signingUp ? 'Already have an account?' : 'New to Dhun?'}{' '}
            <button type="button" className="link-btn" onClick={switchMode}>
              {signingUp ? 'Log in' : 'Create an account'}
            </button>
          </p>
        </form>
      </main>
    </div>
  );
}
