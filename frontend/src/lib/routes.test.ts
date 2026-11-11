import { describe, expect, it } from 'vitest';
import { href, parseHash, tabOf, type Route } from './routes';

describe('routes', () => {
  it.each<[string, Route]>([
    ['', { view: 'home' }],
    ['#home', { view: 'home' }],
    ['#new', { view: 'new' }],
    ['#years', { view: 'folders', type: 'years' }],
    ['#artists', { view: 'folders', type: 'artists' }],
    ['#movies', { view: 'folders', type: 'movies' }],
    ['#year/2024', { view: 'folder', filter: 'year', key: '2024' }],
    ['#artist/Arijit%20Singh', { view: 'folder', filter: 'artist', key: 'Arijit Singh' }],
    ['#movie/Brahmastra%20(OST)', { view: 'folder', filter: 'movie', key: 'Brahmastra (OST)' }],
    ['#liked', { view: 'liked' }],
    ['#nonsense', { view: 'home' }],
    ['#genre/pop', { view: 'home' }],
  ])('%s', (hash, route) => expect(parseHash(hash)).toEqual(route));

  it('round-trips folder links, including special characters', () => {
    const route: Route = { view: 'folder', filter: 'artist', key: 'A.R. Rahman & Friends / 100%' };
    expect(parseHash(href(route))).toEqual(route);
  });

  it('highlights Home for every browse screen', () => {
    expect(tabOf({ view: 'folder', filter: 'year', key: '2024' })).toBe('home');
    expect(tabOf({ view: 'new' })).toBe('home');
    expect(tabOf({ view: 'downloads' })).toBe('downloads');
  });
});
