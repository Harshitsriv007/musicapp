import { describe, expect, it } from 'vitest';
import { formatDuration, timeAgo } from './format';

describe('formatDuration', () => {
  it.each([
    [0, '0:00'],
    [5, '0:05'],
    [65, '1:05'],
    [268.7, '4:28'],
    [null, '0:00'],
    [NaN, '0:00'],
  ])('%s -> %s', (input, expected) => expect(formatDuration(input as number | null)).toBe(expected));
});

describe('timeAgo', () => {
  const now = new Date('2026-01-02T12:00:00Z');
  it.each([
    ['2026-01-02T11:59:30Z', 'just now'],
    ['2026-01-02T11:50:00Z', '10 min ago'],
    ['2026-01-02T09:00:00Z', '3 h ago'],
    ['2026-01-01T11:00:00Z', 'yesterday'],
    ['2025-12-28T12:00:00Z', '5 days ago'],
  ])('%s -> %s', (iso, expected) => expect(timeAgo(iso, now)).toBe(expected));
});
