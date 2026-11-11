import { api } from '../api/client';

type Listener = (data: unknown) => void;

/**
 * One shared Server-Sent Events connection for the whole app (new songs, download progress).
 * Opened when the first subscriber arrives, closed when the last one leaves.
 */
let source: EventSource | null = null;
let registered = new Set<string>();
let live = false;
const listeners = new Map<string, Set<Listener>>();
const liveListeners = new Set<(live: boolean) => void>();

function setLive(value: boolean) {
  live = value;
  liveListeners.forEach((fn) => fn(value));
}

function register(type: string) {
  if (!source || registered.has(type)) return;
  registered.add(type);
  source.addEventListener(type, (event) => {
    let data: unknown;
    try {
      data = JSON.parse((event as MessageEvent<string>).data);
    } catch {
      return;
    }
    listeners.get(type)?.forEach((fn) => fn(data));
  });
}

function totalListeners() {
  let n = 0;
  listeners.forEach((set) => (n += set.size));
  return n;
}

export function subscribe<T>(type: string, fn: (data: T) => void): () => void {
  const set = listeners.get(type) ?? new Set<Listener>();
  set.add(fn as Listener);
  listeners.set(type, set);
  if (!source && typeof EventSource !== 'undefined') {
    source = new EventSource(api.songStreamUrl());
    registered = new Set();
    source.onopen = () => setLive(true);
    source.onerror = () => setLive(false); // the browser reconnects automatically
    listeners.forEach((_, t) => register(t));
  }
  register(type);
  return () => {
    set.delete(fn as Listener);
    if (totalListeners() === 0 && source) {
      source.close();
      source = null;
      setLive(false);
    }
  };
}

export function onLiveChange(fn: (live: boolean) => void): () => void {
  liveListeners.add(fn);
  fn(live);
  return () => liveListeners.delete(fn);
}
