import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AccountProvider } from '../hooks/AccountContext';
import { getSession, setSession } from '../lib/session';
import { mockFetch } from '../test/fakes';
import { LoginView } from './LoginView';

afterEach(() => {
  vi.unstubAllGlobals();
  setSession(null);
});

const renderLogin = () =>
  render(
    <AccountProvider>
      <LoginView />
    </AccountProvider>,
  );

describe('LoginView', () => {
  it('logs in', async () => {
    const fetchMock = mockFetch({ 'POST /api/auth/login': { token: 'tok', username: 'riya' } });
    renderLogin();

    await userEvent.type(screen.getByLabelText('Username'), 'riya');
    await userEvent.type(screen.getByLabelText('Password'), 'secret-123');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(getSession()).toEqual({ token: 'tok', username: 'riya' });
    expect(fetchMock.mock.calls[0][1]!.body).toBe('{"username":"riya","password":"secret-123"}');
  });

  it('creates an account and shows server errors', async () => {
    mockFetch({
      'POST /api/auth/signup': new Response(JSON.stringify({ detail: 'That username is already taken' }), { status: 409 }),
    });
    renderLogin();

    await userEvent.click(screen.getByRole('button', { name: 'Create an account' }));
    expect(screen.getByRole('heading', { name: 'Create your account' })).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Username'), 'riya');
    await userEvent.type(screen.getByLabelText('Password'), 'secret-123');
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('That username is already taken');
    expect(getSession()).toBeNull();
  });

  it('says so when the server cannot be reached', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new TypeError('Failed to fetch'))));
    renderLogin();

    await userEvent.type(screen.getByLabelText('Username'), 'riya');
    await userEvent.type(screen.getByLabelText('Password'), 'secret-123');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Can’t reach the Dhun server');
  });

  it('can show the password', async () => {
    renderLogin();
    const password = screen.getByLabelText('Password');
    expect(password).toHaveAttribute('type', 'password');
    await userEvent.click(screen.getByRole('button', { name: 'Show password' }));
    expect(password).toHaveAttribute('type', 'text');
  });

  it('resets a forgotten password and logs in', async () => {
    const fetchMock = mockFetch({ 'POST /api/auth/reset-password': { token: 'tok2', username: 'riya' } });
    renderLogin();

    await userEvent.click(screen.getByRole('button', { name: 'Forgot password?' }));
    expect(screen.getByRole('heading', { name: 'Reset your password' })).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Username'), 'riya');
    await userEvent.type(screen.getByLabelText('New password'), 'new-pass-1');
    await userEvent.type(screen.getByLabelText('Confirm new password'), 'new-pass-2');
    await userEvent.click(screen.getByRole('button', { name: 'Save new password' }));

    expect(screen.getByRole('alert')).toHaveTextContent('The two passwords don’t match.');
    expect(fetchMock).not.toHaveBeenCalled();

    await userEvent.clear(screen.getByLabelText('Confirm new password'));
    await userEvent.type(screen.getByLabelText('Confirm new password'), 'new-pass-1');
    await userEvent.click(screen.getByRole('button', { name: 'Save new password' }));

    expect(getSession()).toEqual({ token: 'tok2', username: 'riya' });
    expect(fetchMock.mock.calls[0][1]!.body).toBe('{"username":"riya","newPassword":"new-pass-1"}');
  });
});
