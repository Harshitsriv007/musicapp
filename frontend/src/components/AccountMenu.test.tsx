import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AccountProvider } from '../hooks/AccountContext';
import { getSession, setSession } from '../lib/session';
import { mockFetch } from '../test/fakes';
import { AccountMenu } from './AccountMenu';

afterEach(() => {
  vi.unstubAllGlobals();
  setSession(null);
});

const renderMenu = () =>
  render(
    <AccountProvider>
      <AccountMenu />
    </AccountProvider>,
  );

describe('AccountMenu', () => {
  it('logs in and shows the username', async () => {
    const fetchMock = mockFetch({ 'POST /api/auth/login': { token: 'tok', username: 'riya' } });
    renderMenu();

    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
    await userEvent.type(screen.getByLabelText('Username'), 'riya');
    await userEvent.type(screen.getByLabelText('Password'), 'secret-123');
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Log in' }));

    expect(await screen.findByRole('button', { name: 'Account: riya' })).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(getSession()).toEqual({ token: 'tok', username: 'riya' });
    expect(fetchMock.mock.calls[0][1]!.body).toBe('{"username":"riya","password":"secret-123"}');
  });

  it('signs up and shows server errors', async () => {
    mockFetch({
      'POST /api/auth/signup': new Response(JSON.stringify({ detail: 'That username is already taken' }), { status: 409 }),
    });
    renderMenu();

    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));
    await userEvent.type(screen.getByLabelText('Username'), 'riya');
    await userEvent.type(screen.getByLabelText('Password'), 'secret-123');
    await userEvent.click(screen.getByRole('button', { name: 'Sign up' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('That username is already taken');
    expect(getSession()).toBeNull();
  });

  it('logs out', async () => {
    setSession({ token: 'tok', username: 'riya' });
    const fetchMock = mockFetch();
    renderMenu();

    await userEvent.click(screen.getByRole('button', { name: 'Account: riya' }));
    await userEvent.click(screen.getByRole('button', { name: 'Log out' }));

    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument();
    expect(getSession()).toBeNull();
    expect(fetchMock.mock.calls[0][0]).toBe('/api/auth/logout');
  });
});
