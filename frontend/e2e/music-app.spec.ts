import { expect, test } from '@playwright/test';
import { fakeBackend, song } from './fakeBackend';

const catalogue = [song(3, 'Chaleya'), song(2, 'Kesariya'), song(1, 'Tum Hi Ho')];

test.describe('Listener journeys', () => {
  test('new songs list loads and plays a song in the mini player', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'New songs' })).toBeVisible();
    await expect(page.getByRole('button', { name: /^Play .* by / })).toHaveCount(3);

    await page.getByRole('button', { name: 'Play Kesariya by Arijit Singh' }).click();

    const mini = page.getByRole('contentinfo', { name: 'Mini player' });
    await expect(mini.getByTestId('now-playing-title')).toHaveText('Kesariya');
    await expect(mini.getByRole('button', { name: 'Pause', exact: true })).toBeVisible();

    await mini.getByRole('button', { name: 'Pause', exact: true }).click();
    await expect(mini.getByRole('button', { name: 'Play', exact: true })).toBeVisible();

    await mini.getByRole('button', { name: 'Next', exact: true }).click();
    await expect(mini.getByTestId('now-playing-title')).toHaveText('Tum Hi Ho');
  });

  test('previous and stop from the full player', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/');
    await page.getByRole('button', { name: 'Play Kesariya by Arijit Singh' }).click();

    await page.getByRole('button', { name: 'Open full player' }).click();
    const full = page.getByRole('region', { name: 'Now playing' });
    await expect(full.getByRole('heading', { name: 'Kesariya' })).toBeVisible();

    await full.getByRole('button', { name: 'Previous', exact: true }).click();
    await expect(full.getByRole('heading', { name: 'Chaleya' })).toBeVisible();

    await full.getByRole('button', { name: 'Stop', exact: true }).click();
    await expect(full.getByRole('button', { name: 'Play', exact: true })).toBeVisible();
  });

  test('liked songs list follows the heart button', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/');

    await page.getByRole('button', { name: 'Like Chaleya' }).click();
    await page.getByRole('button', { name: 'Like Tum Hi Ho' }).click();
    await page.getByRole('link', { name: 'Liked' }).click();

    await expect(page.getByRole('heading', { name: 'Liked songs' })).toBeVisible();
    await expect(page.getByRole('button', { name: /^Play / })).toHaveText([/Tum Hi Ho/, /Chaleya/]);

    await page.getByRole('button', { name: 'Unlike Tum Hi Ho' }).click();
    await expect(page.getByRole('button', { name: /^Play / })).toHaveText([/Chaleya/]);
  });

  test('history shows what was played, most recent first', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/');
    await page.getByRole('button', { name: 'Play Tum Hi Ho by Arijit Singh' }).click();
    await page.getByRole('button', { name: 'Play Chaleya by Arijit Singh' }).click();

    await page.getByRole('link', { name: 'History' }).click();

    await expect(page.getByRole('button', { name: /^Play / })).toHaveText([/Chaleya/, /Tum Hi Ho/]);
    page.once('dialog', (d) => d.accept());
    await page.getByRole('button', { name: 'Clear' }).click();
    await expect(page.getByText('Songs you play will show up here.')).toBeVisible();
  });

  test('search in all songs', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/#library');

    await page.getByRole('searchbox', { name: 'Search songs' }).fill('kesar');

    await expect(page.getByRole('button', { name: /^Play / })).toHaveText([/Kesariya/]);
  });

  test('new songs appear without reloading the page', async ({ page }) => {
    const backend = await fakeBackend(page, catalogue);
    await page.goto('/');
    await expect(page.getByRole('button', { name: /^Play / })).toHaveCount(3);

    backend.addSong(song(9, 'Heeriye', 'Arijit Singh'));
    // The app re-fetches whenever it becomes visible again (e.g. phone unlocked / tab focused).
    await page.evaluate(() => document.dispatchEvent(new Event('visibilitychange')));

    await expect(page.getByRole('button', { name: 'Play Heeriye by Arijit Singh' })).toBeVisible();
  });

  test('installable as an app (PWA manifest)', async ({ page, request }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/');
    const href = await page.locator('link[rel="manifest"]').getAttribute('href');
    expect(href).toBeTruthy();

    const manifest = await (await request.get(href!)).json();
    expect(manifest.display).toBe('standalone');
    expect(manifest.icons.map((i: { sizes: string }) => i.sizes)).toEqual(expect.arrayContaining(['192x192', '512x512']));
  });
});
