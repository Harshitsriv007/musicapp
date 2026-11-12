import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AccountProvider } from '../hooks/AccountContext';
import { getSession, setSession } from '../lib/session';

const LATER = new Date(Date.now() + 4 * 3600_000).toISOString();
import { mockFetch } from '../test/fakes';
import { AccountMenu } from './AccountMenu';

afterEach(() => {
  vi.unstubAllGlobals();
  setSession(null);
});

describe('AccountMenu', () => {
  it('shows the account and logs out', async () => {
    setSession({ token: 'tok', username: 'riya', expiresAt: LATER });
    const fetchMock = mockFetch();
    const { container } = render(
      <AccountProvider>
        <AccountMenu />
      </AccountProvider>,
    );

    await userEvent.click(screen.getByRole('button', { name: 'Account: riya' }));
    await userEvent.click(screen.getByRole('button', { name: 'Log out' }));

    expect(getSession()).toBeNull();
    expect(fetchMock.mock.calls[0][0]).toBe('/api/auth/logout');
    expect(container).toBeEmptyDOMElement();
  });
});
