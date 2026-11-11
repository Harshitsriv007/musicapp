import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DownloadsProvider } from '../hooks/DownloadsContext';
import { LikesProvider } from '../hooks/LikesContext';
import { PlayerProvider } from '../player/PlayerContext';
import { FakeAudio, mockFetch, song } from '../test/fakes';
import { SearchView } from './SearchView';

afterEach(() => vi.unstubAllGlobals());

const result = {
  query: 'brahma',
  songs: [song(1, 'Kesariya (From "Brahmastra")', { movie: 'Brahmastra' })],
  artists: [],
  movies: [{ key: 'Brahmastra', title: 'Brahmastra', songCount: 2, imageUrl: null }],
};

function renderSearch(initialQuery = '') {
  render(
    <LikesProvider>
      <DownloadsProvider>
        <PlayerProvider audioFactory={() => new FakeAudio().asElement()}>
          <SearchView initialQuery={initialQuery} />
        </PlayerProvider>
      </DownloadsProvider>
    </LikesProvider>,
  );
}

describe('SearchView', () => {
  it('searches as you type and groups the results', async () => {
    const fetchMock = mockFetch({ 'GET /api/search': result });
    renderSearch();
    const box = screen.getByRole('searchbox', { name: 'Search songs, artists or movies' });
    expect(box).toHaveFocus();

    await userEvent.type(box, 'brahma');

    const movies = await screen.findByRole('region', { name: 'Movies & albums' });
    expect(within(movies).getByRole('link', { name: 'Brahmastra, 2 songs' })).toHaveAttribute('href', '#movie/Brahmastra');
    const songs = screen.getByRole('region', { name: 'Songs' });
    expect(within(songs).getByRole('button', { name: /Play Kesariya/ })).toBeInTheDocument();
    expect(within(songs).getByRole('button', { name: /Like Kesariya/ })).toBeInTheDocument();
    expect(within(songs).getByRole('button', { name: /Download Kesariya/ })).toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'Artists' })).not.toBeInTheDocument();
    // debounced: one request for the whole word, not one per letter
    expect(fetchMock.mock.calls.filter(([u]) => String(u).startsWith('/api/search'))).toHaveLength(1);
    expect(String(fetchMock.mock.calls[0][0])).toBe('/api/search?q=brahma');
    expect(window.location.hash).toBe('#search/brahma');
  });

  it('shows the movie next to the artist only when the song has one', async () => {
    mockFetch({
      'GET /api/search': {
        query: 'a',
        songs: [song(1, 'Kesariya', { movie: 'Brahmastra' }), song(2, 'Lofi Mix', { artist: 'NFThunk' })],
        artists: [],
        movies: [],
      },
    });
    renderSearch('a');

    expect(await screen.findByText('Arijit Singh · Brahmastra')).toBeInTheDocument();
    expect(screen.getByText('NFThunk')).toBeInTheDocument();
  });

  it('runs the query from the URL and says when nothing matches', async () => {
    mockFetch({ 'GET /api/search': { query: 'zzz', songs: [], artists: [], movies: [] } });
    renderSearch('zzz');

    expect(await screen.findByText('No songs, artists or movies match “zzz”.')).toBeInTheDocument();
  });

  it('shows a hint while empty and does not call the API', () => {
    const fetchMock = mockFetch();
    renderSearch();

    expect(screen.getByText(/Find a song by its name/)).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('reports errors', async () => {
    mockFetch({ 'GET /api/search': new Response(null, { status: 500 }) });
    renderSearch('x');

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Search failed'));
  });
});
