import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { EyeIcon, EyeOffIcon, HeartLineIcon, DownloadLineIcon, ClockIcon } from '../components/Icons';
import { useAccount } from '../hooks/AccountContext';

type Mode = 'login' | 'signup' | 'reset';

const TEXT: Record<Mode, { title: string; lead: string; submit: string }> = {
  login: { title: 'Welcome back', lead: 'Log in to continue listening.', submit: 'Log in' },
  signup: { title: 'Create your account', lead: 'Sign up to keep your music in sync on all your devices.', submit: 'Create account' },
  reset: { title: 'Reset your password', lead: 'Choose a new password for your account. You’ll be logged in straight away.', submit: 'Save new password' },
};


const FEATURES = [
  { icon: HeartLineIcon, text: 'Liked songs saved to your account' },
  { icon: ClockIcon, text: 'Listening history on every device' },
  { icon: DownloadLineIcon, text: 'Downloads that play instantly' },
];

/** The first screen: log in or create an account. Nothing else is shown until you do. */
export function LoginView() {
  const { logIn, signUp, resetPassword, sessionExpired } = useAccount();
  const [mode, setMode] = useState<Mode>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const userRef = useRef<HTMLInputElement>(null);
  const hintId = useId();
  const errorId = useId();
  const choosingPassword = mode !== 'login'; // sign-up and reset both set a new password
  const text = TEXT[mode];

  useEffect(() => userRef.current?.focus(), [mode]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (mode === 'reset' && password !== confirm) {
      setError('The two passwords don’t match.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const action = mode === 'login' ? logIn : mode === 'signup' ? signUp : resetPassword;
      await action(username.trim(), password);
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

  const go = (next: Mode) => {
    setMode(next);
    setPassword('');
    setConfirm('');
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
          <h1>{text.title}</h1>
          <p className="auth__lead">{text.lead}</p>
          {sessionExpired && mode === 'login' && !error && (
            <p className="form-notice" role="status">
              Your session has expired. Please log in again.
            </p>
          )}

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
              aria-describedby={mode === 'signup' ? hintId : undefined}
            />
          </label>

          <div className="field">
            <label className="field__label" htmlFor="auth-password">
              {mode === 'reset' ? 'New password' : 'Password'}
            </label>
            <span className="input-wrap">
              <input
                id="auth-password"
                className="input"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete={choosingPassword ? 'new-password' : 'current-password'}
                required
                minLength={choosingPassword ? 8 : undefined}
                aria-describedby={choosingPassword ? hintId : undefined}
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
            {mode === 'login' && (
              <button type="button" className="link-btn field__aside" onClick={() => go('reset')}>
                Forgot password?
              </button>
            )}
          </div>

          {mode === 'reset' && (
            <label className="field">
              <span className="field__label">Confirm new password</span>
              <input
                className="input"
                type={showPassword ? 'text' : 'password'}
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                autoComplete="new-password"
                required
                minLength={8}
              />
            </label>
          )}

          {choosingPassword && (
            <p className="field__hint" id={hintId}>
              {mode === 'signup' ? 'Username: 3–32 letters, digits, “.”, “_” or “-”. ' : ''}Password: at least 8 characters.
              {mode === 'reset' ? ' You’ll be logged out on your other devices.' : ''}
            </p>
          )}
          {error && (
            <p className="form-error" role="alert" id={errorId}>
              {error}
            </p>
          )}

          <button type="submit" className="btn btn--lg btn--block" disabled={busy}>
            {busy ? <span className="spinner spinner--light" style={{ width: 16, height: 16 }} /> : null}
            {busy ? 'Please wait…' : text.submit}
          </button>

          <p className="auth__switch">
            {mode === 'login' ? (
              <>
                New to Dhun?{' '}
                <button type="button" className="link-btn" onClick={() => go('signup')}>Create an account</button>
              </>
            ) : (
              <>
                {mode === 'signup' ? 'Already have an account?' : 'Remembered it?'}{' '}
                <button type="button" className="link-btn" onClick={() => go('login')}>Log in</button>
              </>
            )}
          </p>
        </form>
      </main>
    </div>
  );
}
