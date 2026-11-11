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
  genre: string;
  addedAt: string;
  liked: boolean;
}

export const song = (id: number, title: string, artist = 'Arijit Singh'): FakeSong => ({
  id,
  title,
  artist,
  imageUrl: null,
  streamUrl: `/media/${id}.wav`,
  durationSeconds: 2,
  releaseDate: '2024-01-01',
  source: 'audius',
  album: null,
  genre: 'Bollywood',
  addedAt: new Date().toISOString(),
  liked: false,
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
  const view = (s: FakeSong) => ({ ...s, liked: liked.has(s.id) });
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
      return json(route, pageOf(songs.filter((s) => s.title.toLowerCase().includes(q) || s.artist.toLowerCase().includes(q)).map(view)));
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
  };
}
