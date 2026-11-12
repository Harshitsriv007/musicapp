import { useEffect, useRef, useState } from 'react';
import { useAccount } from '../hooks/AccountContext';
import { Avatar } from './Avatar';
import { ChevronIcon, LogOutIcon } from './Icons';

/** The logged-in account: avatar + name; opens a small menu with "Log out". */
export function AccountMenu() {
  const { username, logOut } = useAccount();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: PointerEvent) => !ref.current?.contains(e.target as Node) && setOpen(false);
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('pointerdown', onDown);
    window.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('pointerdown', onDown);
      window.removeEventListener('keydown', onKey);
    };
  }, [open]);

  if (!username) return null;
  return (
    <div className="account" ref={ref}>
      <button
        type="button"
        className="account__btn"
        aria-label={`Account: ${username}`}
        aria-expanded={open}
        aria-haspopup="true"
        onClick={() => setOpen((o) => !o)}
      >
        <Avatar name={username} />
        <span className="account__name">{username}</span>
        <ChevronIcon className="account__chevron" width={16} height={16} />
      </button>
      {open && (
        <div className="account__menu">
          <div className="account__info">
            <Avatar name={username} size={40} />
            <div>
              <p className="account__menu-name">{username}</p>
              <p className="account__menu-sub">Likes &amp; history sync to every device</p>
            </div>
          </div>
          <button type="button" className="account__item" onClick={() => void logOut()}>
            <LogOutIcon width={18} height={18} /> Log out
          </button>
        </div>
      )}
    </div>
  );
}
