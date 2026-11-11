import type { FolderFilter, FolderType } from '../api/types';

/** Screens, addressed by the URL hash so the back button and shared links work. */
export type Route =
  | { view: 'home' }
  | { view: 'new' }
  | { view: 'folders'; type: FolderType }
  | { view: 'folder'; filter: FolderFilter; key: string }
  | { view: 'library' }
  | { view: 'liked' }
  | { view: 'downloads' }
  | { view: 'history' };

const FOLDER_TYPES: FolderType[] = ['years', 'artists', 'movies'];
const FILTERS: FolderFilter[] = ['year', 'artist', 'movie'];
const SIMPLE = ['home', 'new', 'library', 'liked', 'downloads', 'history'] as const;

export const FILTER_OF: Record<FolderType, FolderFilter> = { years: 'year', artists: 'artist', movies: 'movie' };
export const TYPE_OF: Record<FolderFilter, FolderType> = { year: 'years', artist: 'artists', movie: 'movies' };

export function parseHash(hash: string): Route {
  const raw = hash.replace(/^#\/?/, '');
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
    default:
      return `#${route.view}`;
  }
}

/** Which top-level tab is highlighted for a route. */
export function tabOf(route: Route): 'home' | 'library' | 'liked' | 'downloads' | 'history' {
  switch (route.view) {
    case 'library':
    case 'liked':
    case 'downloads':
    case 'history':
      return route.view;
    default:
      return 'home';
  }
}
