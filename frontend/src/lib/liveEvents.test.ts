import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { onLiveChange, subscribe } from './liveEvents';

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  listeners = new Map<string, (e: MessageEvent) => void>();
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  closed = false;
  constructor(public url: string) {
    FakeEventSource.instances.push(this);
  }
  addEventListener(type: string, fn: (e: MessageEvent) => void) {
    this.listeners.set(type, fn);
  }
  close() {
    this.closed = true;
  }
}

beforeEach(() => {
  FakeEventSource.instances = [];
  vi.stubGlobal('EventSource', FakeEventSource);
});
afterEach(() => vi.unstubAllGlobals());

describe('liveEvents', () => {
  it('shares one connection between subscribers and closes it when the last one leaves', () => {
    const added = vi.fn();
    const updated = vi.fn();
    const live = vi.fn();
    const offLive = onLiveChange(live);

    const off1 = subscribe('song-added', added);
    const off2 = subscribe('song-updated', updated);
    expect(FakeEventSource.instances).toHaveLength(1);
    const es = FakeEventSource.instances[0];

    es.onopen?.();
    es.listeners.get('song-updated')!(new MessageEvent('song-updated', { data: '{"id":1}' }));
    es.listeners.get('song-added')!(new MessageEvent('song-added', { data: 'not json' }));

    expect(live).toHaveBeenLastCalledWith(true);
    expect(updated).toHaveBeenCalledWith({ id: 1 });
    expect(added).not.toHaveBeenCalled();

    off1();
    expect(es.closed).toBe(false);
    off2();
    expect(es.closed).toBe(true);
    expect(live).toHaveBeenLastCalledWith(false);
    offLive();
  });
});
