import type { FolderFilter, FolderType } from '../api/types';

/** Screens, addressed by the URL hash so the back button and shared links work. */
export type Route =
  | { view: 'home' }
  | { view: 'new' }
  | { view: 'folders'; type: FolderType }
  | { view: 'folder'; filter: FolderFilter; key: string }
  | { view: 'search'; q: string }
  | { view: 'library' }
  | { view: 'liked' }
  | { view: 'downloads' }
  | { view: 'history' };

const FOLDER_TYPES: FolderType[] = ['decades', 'artists', 'movies'];
const FILTERS: FolderFilter[] = ['decade', 'artist', 'movie'];
const SIMPLE = ['home', 'new', 'library', 'liked', 'downloads', 'history'] as const;

export const FILTER_OF: Record<FolderType, FolderFilter> = { decades: 'decade', artists: 'artist', movies: 'movie' };
export const TYPE_OF: Record<FolderFilter, FolderType> = { decade: 'decades', artist: 'artists', movie: 'movies' };

/** Display name of a folder key: decade '1990' → '90s', 'latest' → 'Latest'; others unchanged. */
export function folderTitle(filter: FolderFilter, key: string): string {
  if (filter !== 'decade') return key;
  if (key === 'latest') return 'Latest';
  return /^\d{4}$/.test(key) ? `${key.slice(2)}s` : key;
}

export function parseHash(hash: string): Route {
  const raw = hash.replace(/^#\/?/, '');
  if (raw === 'search' || raw.startsWith('search/')) {
    let q = raw.slice('search/'.length);
    try {
      q = decodeURIComponent(q);
    } catch {
      /* keep raw */
    }
    return { view: 'search', q: raw === 'search' ? '' : q };
  }
  const slash = raw.indexOf('/');
  if (slash > 0) {
    const filter = raw.slice(0, slash) as FolderFilter;
    let key = raw.slice(slash + 1);
    try {
      key = decodeURIComponent(key);
    } catch {
      /* keep raw */
    }
    if (FILTERS.includes(filter) && key) return { view: 'folder', filter, key };
  }
  if (FOLDER_TYPES.includes(raw as FolderType)) return { view: 'folders', type: raw as FolderType };
  if ((SIMPLE as readonly string[]).includes(raw)) return { view: raw } as Route;
  return { view: 'home' };
}

export function href(route: Route): string {
  switch (route.view) {
    case 'folders':
      return `#${route.type}`;
    case 'folder':
      return `#${route.filter}/${encodeURIComponent(route.key)}`;
    case 'search':
      return route.q ? `#search/${encodeURIComponent(route.q)}` : '#search';
    default:
      return `#${route.view}`;
  }
}

/** Which top-level tab is highlighted for a route. */
export function tabOf(route: Route): 'home' | 'search' | 'library' | 'liked' | 'downloads' | 'history' {
  switch (route.view) {
    case 'search':
    case 'library':
    case 'liked':
    case 'downloads':
    case 'history':
      return route.view;
    default:
      return 'home';
  }
}
