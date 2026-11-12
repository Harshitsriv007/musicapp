import { afterEach, describe, expect, it, vi } from 'vitest';
import { getSession, sessionExpired, setSession } from '../lib/session';

const LATER = new Date(Date.now() + 4 * 3600_000).toISOString();
import { mockFetch, song } from '../test/fakes';
import { api, ApiError, audioUrl } from './client';

afterEach(() => {
  vi.unstubAllGlobals();
  setSession(null);
});

const headersOf = (init: RequestInit | undefined) => init!.headers as Record<string, string>;

describe('api client', () => {
  it('sends a stable per-device X-User-Id header', async () => {
    const fetchMock = mockFetch({ 'GET /api/songs/new': [song(1, 'Kesariya')] });

    const first = await api.newSongs();
    await api.newSongs();

    expect(first[0].title).toBe('Kesariya');
    const ids = fetchMock.mock.calls.map(([, init]) => (init!.headers as Record<string, string>)['X-User-Id']);
    expect(ids[0]).toMatch(/^[A-Za-z0-9_-]{1,64}$/);
    expect(ids[1]).toBe(ids[0]);
  });

  it('encodes search queries', async () => {
    const fetchMock = mockFetch({ 'GET /api/songs': { content: [], page: 0, size: 30, totalElements: 0, totalPages: 0 } });

    await api.songs('tum hi ho');

    expect(String(fetchMock.mock.calls[0][0])).toBe('/api/songs?page=0&size=30&q=tum%20hi%20ho');
  });

  it('uses PUT / DELETE for like and unlike', async () => {
    const fetchMock = mockFetch();

    await api.like(5);
    await api.unlike(5);

    expect(fetchMock.mock.calls.map(([url, init]) => `${init!.method} ${url}`)).toEqual([
      'PUT /api/likes/5',
      'DELETE /api/likes/5',
    ]);
  });

  it('plays downloaded songs from our own API and others from the source', () => {
    expect(audioUrl(song(4, 'A', { downloadStatus: 'DOWNLOADED' }))).toBe('/api/songs/4/audio');
    expect(audioUrl(song(5, 'B'))).toBe('https://stream.test/5');
  });

  it('requests downloads in bulk and filters downloaded songs', async () => {
    const fetchMock = mockFetch({
      'POST /api/downloads': [{ songId: 1, downloadStatus: 'PENDING' }],
      'GET /api/songs': { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 },
    });

    await api.download([1, 2]);
    await api.songs('', 0, 100, { downloaded: true });

    expect(fetchMock.mock.calls[0][1]!.body).toBe('{"songIds":[1,2]}');
    expect(String(fetchMock.mock.calls[1][0])).toBe('/api/songs?page=0&size=100&downloaded=true');
  });

  it('lists the songs of a folder and likes a whole folder', async () => {
    const fetchMock = mockFetch({ 'GET /api/songs': { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 } });

    await api.songs('', 0, 100, { artist: 'Arijit Singh' });
    await api.likeBatch([1, 2], true);

    expect(String(fetchMock.mock.calls[0][0])).toBe('/api/songs?page=0&size=100&artist=Arijit%20Singh');
    expect(fetchMock.mock.calls[1][0]).toBe('/api/likes/batch');
    expect(fetchMock.mock.calls[1][1]!.body).toBe('{"songIds":[1,2],"liked":true}');
  });

  it('throws ApiError on HTTP errors', async () => {
    mockFetch({ 'PUT /api/likes/9': new Response(null, { status: 404 }) });

    await expect(api.like(9)).rejects.toBeInstanceOf(ApiError);
  });

  it('sends the login token so likes and history follow the account', async () => {
    const fetchMock = mockFetch({ 'POST /api/auth/login': { token: 'tok', username: 'riya', expiresAt: LATER } });

    const res = await api.logIn('riya', 'secret-123');
    setSession(res);
    await api.like(1);

    expect(headersOf(fetchMock.mock.calls[0][1]).Authorization).toBeUndefined();
    expect(headersOf(fetchMock.mock.calls[1][1]).Authorization).toBe('Bearer tok');
    expect(headersOf(fetchMock.mock.calls[1][1])['X-User-Id']).toBeTruthy();
  });

  it('forgets an expired session', async () => {
    setSession({ token: 'old', username: 'riya', expiresAt: LATER });
    mockFetch({ 'GET /api/likes': new Response(null, { status: 401 }) });

    await expect(api.liked()).rejects.toBeInstanceOf(ApiError);
    expect(getSession()).toBeNull();
    expect(sessionExpired()).toBe(true);
  });

  it('stops sending a token once the session has run out', async () => {
    setSession({ token: 'tok', username: 'riya', expiresAt: new Date(Date.now() - 1000).toISOString() });
    const fetchMock = mockFetch({ 'GET /api/songs/new?limit=50': [] });

    await api.newSongs();

    expect(getSession()).toBeNull();
    expect(headersOf(fetchMock.mock.calls[0][1]).Authorization).toBeUndefined();
  });

  it('surfaces the server error message', async () => {
    mockFetch({
      'POST /api/auth/signup': new Response(JSON.stringify({ detail: 'That username is already taken' }), { status: 409 }),
    });

    await expect(api.signUp('riya', 'secret-123')).rejects.toThrow('That username is already taken');
  });
});
