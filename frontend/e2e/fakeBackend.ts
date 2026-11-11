import type { Page, Route } from '@playwright/test';

export interface FakeSong {
  id: number;
  title: string;
  artist: string;
  imageUrl: string | null;
  streamUrl: string;
  durationSeconds: number;
  releaseDate: string;
  source: string;
  album: null;
  movie: string | null;
  genre: string;
  addedAt: string;
  liked: boolean;
  downloadStatus: 'NONE' | 'DOWNLOADED';
}

export const song = (id: number, title: string, artist = 'Arijit Singh', releaseDate = '2024-01-01', movie: string | null = null): FakeSong => ({
  id,
  title,
  artist,
  imageUrl: null,
  streamUrl: `/media/${id}.wav`,
  durationSeconds: 2,
  releaseDate,
  source: 'audius',
  album: null,
  movie,
  genre: 'Bollywood',
  addedAt: new Date().toISOString(),
  liked: false,
  downloadStatus: 'NONE',
});

/** 2 seconds of silence – a real, playable audio file for the browser. */
function silentWav(seconds = 2, rate = 8000): Buffer {
  const samples = seconds * rate;
  const b = Buffer.alloc(44 + samples);
  b.write('RIFF', 0); b.writeUInt32LE(36 + samples, 4); b.write('WAVE', 8);
  b.write('fmt ', 12); b.writeUInt32LE(16, 16); b.writeUInt16LE(1, 20); b.writeUInt16LE(1, 22);
  b.writeUInt32LE(rate, 24); b.writeUInt32LE(rate, 28); b.writeUInt16LE(1, 32); b.writeUInt16LE(8, 34);
  b.write('data', 36); b.writeUInt32LE(samples, 40); b.fill(128, 44);
  return b;
}

/** In-memory stand-in for the Spring Boot API, so browser tests are deterministic. */
export async function fakeBackend(page: Page, initial: FakeSong[]) {
  const songs = [...initial];
  const liked = new Map<number, string>();
  const history: { id: number; songId: number; playedAt: string }[] = [];
  const downloaded = new Set<number>();
  const audioServedFromDb: number[] = [];
  const view = (s: FakeSong) => ({ ...s, liked: liked.has(s.id), downloadStatus: downloaded.has(s.id) ? 'DOWNLOADED' : 'NONE' });
  const json = (route: Route, body: unknown, status = 200) =>
    route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
  const pageOf = <T>(content: T[]) => ({ content, page: 0, size: content.length, totalElements: content.length, totalPages: 1 });

  await page.route('**/media/*.wav', (route) => route.fulfill({ contentType: 'audio/wav', body: silentWav() }));
  await page.route('**/api/songs/stream', (route) => route.fulfill({ status: 200, contentType: 'text/event-stream', body: 'event: connected\ndata: ok\n\n' }));
  await page.route('**/api/**', async (route) => {
    const req = route.request();
    const url = new URL(req.url());
    const path = url.pathname;
    const method = req.method();

    if (method === 'GET' && path === '/api/songs/new') return json(route, songs.map(view));
    if (method === 'GET' && path === '/api/songs') {
      const q = (url.searchParams.get('q') ?? '').toLowerCase();
      const onlyDownloaded = url.searchParams.get('downloaded') === 'true';
      const year = url.searchParams.get('year');
      const artist = url.searchParams.get('artist');
      const movie = url.searchParams.get('movie');
      return json(route, pageOf(songs
        .filter((s) => s.title.toLowerCase().includes(q) || s.artist.toLowerCase().includes(q))
        .filter((s) => !onlyDownloaded || downloaded.has(s.id))
        .filter((s) => !year || s.releaseDate.startsWith(year))
        .filter((s) => !artist || s.artist === artist)
        .filter((s) => !movie || s.movie === movie)
        .map(view)));
    }
    const browseMatch = path.match(/^\/api\/browse\/(years|artists|movies)$/);
    if (method === 'GET' && browseMatch) {
      const keyOf = (s: FakeSong) =>
        browseMatch[1] === 'years' ? s.releaseDate.slice(0, 4) : browseMatch[1] === 'artists' ? s.artist : s.movie;
      const counts = new Map<string, number>();
      songs.forEach((s) => { const k = keyOf(s); if (k) counts.set(k, (counts.get(k) ?? 0) + 1); });
      const folders = [...counts.entries()]
        .map(([key, songCount]) => ({ key, title: key, songCount, imageUrl: null }))
        .sort((a, b) => (browseMatch[1] === 'years' ? b.key.localeCompare(a.key) : b.songCount - a.songCount || a.key.localeCompare(b.key)));
      return json(route, folders);
    }
    if (method === 'POST' && path === '/api/likes/batch') {
      const { songIds, liked: like } = req.postDataJSON() as { songIds: number[]; liked: boolean };
      songIds.forEach((id) => (like ? liked.set(id, new Date().toISOString()) : liked.delete(id)));
      return route.fulfill({ status: 204 });
    }
    if (method === 'POST' && path === '/api/downloads') {
      const { songIds } = req.postDataJSON() as { songIds: number[] };
      songIds.forEach((id) => downloaded.add(id)); // instant "download" in the fake
      return json(route, songIds.map((songId) => ({ songId, downloadStatus: 'DOWNLOADED' })), 202);
    }
    const downloadMatch = path.match(/^\/api\/songs\/(\d+)\/download$/);
    if (downloadMatch && method === 'DELETE') { downloaded.delete(Number(downloadMatch[1])); return route.fulfill({ status: 204 }); }
    const audioMatch = path.match(/^\/api\/songs\/(\d+)\/audio$/);
    if (audioMatch && method === 'GET' && downloaded.has(Number(audioMatch[1]))) {
      audioServedFromDb.push(Number(audioMatch[1]));
      return route.fulfill({ contentType: 'audio/wav', body: silentWav() });
    }
    const likeMatch = path.match(/^\/api\/likes\/(\d+)$/);
    if (likeMatch && method === 'PUT') { liked.set(Number(likeMatch[1]), new Date().toISOString()); return route.fulfill({ status: 204 }); }
    if (likeMatch && method === 'DELETE') { liked.delete(Number(likeMatch[1])); return route.fulfill({ status: 204 }); }
    if (method === 'GET' && path === '/api/likes') {
      const entries = [...liked.entries()].reverse().map(([id, likedAt]) => ({ song: view(songs.find((s) => s.id === id)!), likedAt }));
      return json(route, pageOf(entries));
    }
    if (method === 'POST' && path === '/api/history') {
      const { songId } = req.postDataJSON() as { songId: number };
      const entry = { id: history.length + 1, songId, playedAt: new Date().toISOString() };
      history.unshift(entry);
      return json(route, { ...entry, song: view(songs.find((s) => s.id === songId)!) }, 201);
    }
    if (method === 'GET' && path === '/api/history') {
      return json(route, pageOf(history.map((h) => ({ id: h.id, playedAt: h.playedAt, song: view(songs.find((s) => s.id === h.songId)!) }))));
    }
    if (method === 'DELETE' && path === '/api/history') { history.length = 0; return route.fulfill({ status: 204 }); }
    if (method === 'POST' && path === '/api/admin/ingestion/run') return json(route, { status: 'started' }, 202);
    return route.fulfill({ status: 404 });
  });

  return {
    /** Simulates the cron job + Kafka consumer adding a song (picked up by the REST poll / refresh). */
    addSong: (s: FakeSong) => songs.unshift(s),
    history,
    audioServedFromDb,
  };
}
