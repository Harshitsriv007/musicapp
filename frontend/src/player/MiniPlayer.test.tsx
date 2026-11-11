import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useEffect } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DownloadsProvider } from '../hooks/DownloadsContext';
import { LikesProvider } from '../hooks/LikesContext';
import { FakeAudio, mockFetch, song } from '../test/fakes';
import { MiniPlayer } from './MiniPlayer';
import { PlayerProvider, usePlayer, type PlayerApi } from './PlayerContext';

const queue = [song(1, 'Tum Hi Ho'), song(2, 'Kesariya')];

function renderMiniPlayer() {
  const audio = new FakeAudio();
  const ref: { player?: PlayerApi } = {};
  function Capture() {
    const p = usePlayer();
    useEffect(() => {
      ref.player = p;
    });
    return null;
  }
  render(
    <LikesProvider>
      <DownloadsProvider>
      <PlayerProvider audioFactory={() => audio.asElement()}>
        <Capture />
        <MiniPlayer />
      </PlayerProvider>
      </DownloadsProvider>
    </LikesProvider>,
  );
  return { audio, ref };
}

beforeEach(() => mockFetch({ 'POST /api/history': { id: 1 } }));
afterEach(() => vi.unstubAllGlobals());

describe('MiniPlayer', () => {
  it('is hidden until something plays', () => {
    renderMiniPlayer();
    expect(screen.queryByRole('contentinfo', { name: 'Mini player' })).not.toBeInTheDocument();
  });

  it('shows song name, artist and artwork with working controls', async () => {
    const user = userEvent.setup();
    const { ref, audio } = renderMiniPlayer();
    act(() => ref.player!.playSong(queue[0], queue));

    expect(screen.getByTestId('now-playing-title')).toHaveTextContent('Tum Hi Ho');
    expect(screen.getAllByText('Arijit Singh')[0]).toBeInTheDocument();
    expect(document.querySelector('.mini-player img')).toHaveAttribute('src', 'https://img.test/1.jpg');

    await user.click(screen.getByRole('button', { name: 'Pause' }));
    expect(audio.pause).toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Play' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByTestId('now-playing-title')).toHaveTextContent('Kesariya');
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: 'Previous' }));
    expect(screen.getByTestId('now-playing-title')).toHaveTextContent('Tum Hi Ho');

    await user.click(screen.getByRole('button', { name: 'Stop' }));
    expect(screen.getByRole('button', { name: 'Play' })).toBeInTheDocument();
  });

  it('opens the full player with a seek bar', async () => {
    const user = userEvent.setup();
    const { ref } = renderMiniPlayer();
    act(() => ref.player!.playSong(queue[0], queue));

    await user.click(screen.getByRole('button', { name: 'Open full player' }));

    expect(screen.getByRole('region', { name: 'Now playing' })).toBeInTheDocument();
    expect(screen.getByRole('slider', { name: 'Seek' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Close player' }));
    expect(screen.queryByRole('region', { name: 'Now playing' })).not.toBeInTheDocument();
  });

  it('offers a pop-out window only where Document Picture-in-Picture exists', () => {
    const { ref } = renderMiniPlayer();
    act(() => ref.player!.playSong(queue[0], queue));
    expect(screen.queryByRole('button', { name: 'Pop out mini player' })).not.toBeInTheDocument();
  });
});
