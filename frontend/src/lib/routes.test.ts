import { describe, expect, it } from 'vitest';
import { folderTitle, href, parseHash, tabOf, type Route } from './routes';

describe('routes', () => {
  it.each<[string, Route]>([
    ['', { view: 'home' }],
    ['#home', { view: 'home' }],
    ['#new', { view: 'new' }],
    ['#decades', { view: 'folders', type: 'decades' }],
    ['#years', { view: 'home' }],
    ['#artists', { view: 'folders', type: 'artists' }],
    ['#movies', { view: 'folders', type: 'movies' }],
    ['#decade/1990', { view: 'folder', filter: 'decade', key: '1990' }],
    ['#decade/latest', { view: 'folder', filter: 'decade', key: 'latest' }],
    ['#artist/Arijit%20Singh', { view: 'folder', filter: 'artist', key: 'Arijit Singh' }],
    ['#movie/Brahmastra%20(OST)', { view: 'folder', filter: 'movie', key: 'Brahmastra (OST)' }],
    ['#liked', { view: 'liked' }],
    ['#search', { view: 'search', q: '' }],
    ['#search/tum%20hi%20ho', { view: 'search', q: 'tum hi ho' }],
    ['#nonsense', { view: 'home' }],
    ['#genre/pop', { view: 'home' }],
  ])('%s', (hash, route) => expect(parseHash(hash)).toEqual(route));

  it('round-trips search links', () => {
    expect(parseHash(href({ view: 'search', q: '100% love / ishq' }))).toEqual({ view: 'search', q: '100% love / ishq' });
    expect(href({ view: 'search', q: '' })).toBe('#search');
  });

  it('round-trips folder links, including special characters', () => {
    const route: Route = { view: 'folder', filter: 'artist', key: 'A.R. Rahman & Friends / 100%' };
    expect(parseHash(href(route))).toEqual(route);
  });

  it('highlights Home for every browse screen', () => {
    expect(tabOf({ view: 'folder', filter: 'decade', key: '1990' })).toBe('home');
    expect(tabOf({ view: 'new' })).toBe('home');
    expect(tabOf({ view: 'downloads' })).toBe('downloads');
    expect(tabOf({ view: 'search', q: 'x' })).toBe('search');
  });

  it.each([
    ['1950', '50s'],
    ['1990', '90s'],
    ['2000', '00s'],
    ['2020', '20s'],
    ['latest', 'Latest'],
  ])('decade %s is shown as %s', (key, title) => expect(folderTitle('decade', key)).toBe(title));

  it('keeps artist and movie names as they are', () => {
    expect(folderTitle('artist', 'Arijit Singh')).toBe('Arijit Singh');
  });
});
