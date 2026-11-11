import { expect, test } from '@playwright/test';
import { fakeBackend, song } from './fakeBackend';

const catalogue = [
  song(3, 'Chaleya', 'Arijit Singh', '2023-08-14', 'Jawan'),
  song(2, 'Kesariya', 'Arijit Singh', '2022-07-17', 'Brahmastra'),
  song(1, 'Tum Hi Ho', 'Arijit Singh', '2013-04-01', 'Aashiqui 2'),
  song(6, 'Lag Ja Gale', 'Lata Mangeshkar', '1964-01-01', 'Woh Kaun Thi'),
  song(4, 'Deva Deva', 'Arijit Singh', '2022-08-08', 'Brahmastra'),
  song(5, 'Tum Kya Mile', 'Shreya Ghoshal', '2023-06-28'),
];
const NEW_SONGS = 6;

test.describe('Listener journeys', () => {
  test('new songs list loads and plays a song in the mini player', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/#new');

    await expect(page.getByRole('heading', { name: 'New songs' })).toBeVisible();
    await expect(page.getByRole('button', { name: /^Play .* by / })).toHaveCount(NEW_SONGS);

    await page.getByRole('button', { name: 'Play Kesariya by Arijit Singh' }).click();

    const mini = page.getByRole('contentinfo', { name: 'Mini player' });
    await expect(mini.getByTestId('now-playing-title')).toHaveText('Kesariya');
    await expect(mini.getByRole('button', { name: 'Pause', exact: true })).toBeVisible();

    await mini.getByRole('button', { name: 'Pause', exact: true }).click();
    await expect(mini.getByRole('button', { name: 'Play', exact: true })).toBeVisible();

    await mini.getByRole('button', { name: 'Next', exact: true }).click();
    await expect(mini.getByTestId('now-playing-title')).toHaveText('Tum Hi Ho');
    await expect(mini.getByRole('button', { name: 'Pause', exact: true })).toBeVisible();
  });

  test('previous and stop from the full player', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/#new');
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
    await page.goto('/#new');

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
    await page.goto('/#new');
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
    await page.goto('/#new');
    await expect(page.getByRole('button', { name: /^Play / })).toHaveCount(NEW_SONGS);

    backend.addSong(song(9, 'Heeriye', 'Arijit Singh'));
    // The app re-fetches whenever it becomes visible again (e.g. phone unlocked / tab focused).
    await page.evaluate(() => document.dispatchEvent(new Event('visibilitychange')));

    await expect(page.getByRole('button', { name: 'Play Heeriye by Arijit Singh' })).toBeVisible();
  });

  test('download all liked songs, then play them from our own server', async ({ page }) => {
    const backend = await fakeBackend(page, catalogue);
    await page.goto('/#new');
    await page.getByRole('button', { name: 'Like Kesariya' }).click();
    await page.getByRole('button', { name: 'Like Chaleya' }).click();
    await page.getByRole('link', { name: 'Liked' }).click();

    await page.getByRole('button', { name: 'Download all (2)' }).click();

    await expect(page.getByRole('button', { name: 'All downloaded' })).toBeDisabled();
    await page.getByRole('link', { name: 'Downloads' }).click();
    await expect(page.getByRole('button', { name: /^Play / })).toHaveText([/Chaleya/, /Kesariya/]);

    await page.getByRole('button', { name: 'Play Kesariya by Arijit Singh' }).click();
    await expect(page.getByTestId('now-playing-title')).toHaveText('Kesariya');
    await expect.poll(() => backend.audioServedFromDb).toContain(2);

    await page.getByRole('button', { name: 'Remove download of Chaleya' }).click();
    await expect(page.getByRole('button', { name: /^Play / })).toHaveText([/Kesariya/]);
  });

  test('download a single song from All songs', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/#library');

    await page.getByRole('button', { name: 'Download Tum Hi Ho' }).click();

    await expect(page.getByRole('button', { name: 'Remove download of Tum Hi Ho' })).toBeVisible();
    await expect(page.getByRole('button', { name: `Download all (${NEW_SONGS - 1})` })).toBeVisible();
  });

  test('home shows new songs and folders by decade, artist and movie', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'New songs' })).toBeVisible();
    const decades = page.getByRole('region', { name: 'Songs by decade' });
    await expect(decades.getByRole('listitem')).toHaveText([/Latest/, /20s/, /10s/, /00s/, /90s/, /80s/, /70s/, /60s/, /50s/]);
    await expect(decades.getByRole('link', { name: '20s, 4 songs' })).toBeVisible();
    await expect(decades.getByRole('link', { name: '90s, 0 songs' })).toBeVisible();
    await expect(page.getByRole('region', { name: 'Artists' }).getByRole('link', { name: 'Arijit Singh, 4 songs' })).toBeVisible();
    await expect(page.getByRole('region', { name: 'Movies & albums' }).getByRole('link', { name: 'Brahmastra, 2 songs' })).toBeVisible();

    await page.getByRole('region', { name: 'New songs' }).getByRole('button', { name: 'Play Kesariya by Arijit Singh' }).click();
    await expect(page.getByTestId('now-playing-title')).toHaveText('Kesariya');
  });

  test('open a movie folder, like all and download all', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/');
    await page.getByRole('link', { name: 'Brahmastra, 2 songs' }).click();

    await expect(page.getByRole('heading', { name: 'Brahmastra' })).toBeVisible();
    await expect(page.getByRole('button', { name: /^Play .* by / })).toHaveText([/Kesariya/, /Deva Deva/]);

    await page.getByRole('button', { name: 'Like all (2)' }).click();
    await expect(page.getByRole('button', { name: 'Unlike all' })).toBeVisible();
    await page.getByRole('button', { name: 'Download all (2)' }).click();
    await expect(page.getByRole('button', { name: 'All downloaded' })).toBeDisabled();

    await page.getByRole('link', { name: 'Liked' }).click();
    await expect(page.getByRole('button', { name: /^Play / })).toHaveCount(2);
    await page.getByRole('link', { name: 'Downloads' }).click();
    await expect(page.getByRole('button', { name: /^Play / })).toHaveCount(2);
  });

  test('decade and artist folders, with per-song like and download', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/');
    await page.getByRole('region', { name: 'Songs by decade' }).getByRole('link', { name: 'See all' }).click();
    await page.getByRole('link', { name: '60s, 1 song' }).click();
    await expect(page.getByRole('heading', { name: '60s' })).toBeVisible();
    await expect(page.getByRole('button', { name: /^Play .* by / })).toHaveText([/Lag Ja Gale/]);

    await page.getByRole('link', { name: '← All decades' }).click();
    await page.getByRole('link', { name: '20s, 4 songs' }).click();
    await expect(page.getByRole('heading', { name: '20s' })).toBeVisible();
    await page.getByRole('button', { name: 'Like Tum Kya Mile' }).click();
    await page.getByRole('button', { name: 'Download Chaleya' }).click();
    await expect(page.getByRole('button', { name: 'Unlike Tum Kya Mile' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Remove download of Chaleya' })).toBeVisible();

    await page.goto('/#artists');
    await page.getByRole('searchbox', { name: 'Filter Artists' }).fill('shreya');
    await page.getByRole('link', { name: 'Shreya Ghoshal, 1 song' }).click();
    await expect(page.getByRole('button', { name: /^Play .* by / })).toHaveText([/Tum Kya Mile/]);
    await expect(page.getByRole('button', { name: 'Unlike all' })).toBeVisible(); // its only song is liked
  });

  test('search by song name, artist and movie', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/');
    await page.getByRole('link', { name: 'Search' }).click();
    const box = page.getByRole('searchbox', { name: 'Search songs, artists or movies' });

    await box.fill('kesar');
    await expect(page.getByRole('region', { name: 'Songs' }).getByRole('button', { name: /^Play / })).toHaveText([/Kesariya/]);

    await box.fill('shreya');
    await expect(page.getByRole('region', { name: 'Artists' }).getByRole('link', { name: 'Shreya Ghoshal, 1 song' })).toBeVisible();

    await box.fill('brahmastra');
    const songs = page.getByRole('region', { name: 'Songs' });
    await expect(songs.getByRole('button', { name: /^Play / })).toHaveText([/Kesariya/, /Deva Deva/]);
    await songs.getByRole('button', { name: 'Like Deva Deva' }).click();
    await expect(songs.getByRole('button', { name: 'Unlike Deva Deva' })).toBeVisible();
    await expect(page).toHaveURL(/#search\/brahmastra$/);

    await page.getByRole('region', { name: 'Movies & albums' }).getByRole('link', { name: 'Brahmastra, 2 songs' }).click();
    await expect(page.getByRole('heading', { name: 'Brahmastra' })).toBeVisible();
    await page.goBack();
    await expect(box).toHaveValue('brahmastra');
  });

  test('press / to search from anywhere', async ({ page }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/#liked');
    await page.keyboard.press('/');
    await expect(page.getByRole('searchbox', { name: 'Search songs, artists or movies' })).toBeFocused();
  });

  test('installable as an app (PWA manifest)', async ({ page, request }) => {
    await fakeBackend(page, catalogue);
    await page.goto('/#new');
    const href = await page.locator('link[rel="manifest"]').getAttribute('href');
    expect(href).toBeTruthy();

    const manifest = await (await request.get(href!)).json();
    expect(manifest.display).toBe('standalone');
    expect(manifest.icons.map((i: { sizes: string }) => i.sizes)).toEqual(expect.arrayContaining(['192x192', '512x512']));
  });
});
