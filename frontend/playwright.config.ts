import { defineConfig, devices } from '@playwright/test';

/**
 * Browser acceptance tests. By default they run against `vite preview` with the backend
 * API mocked at the network layer (hermetic). Set E2E_BASE_URL=http://localhost:3000 to point
 * them at the full docker-compose stack instead (API mocks stay in place for determinism).
 */
const external = process.env.E2E_BASE_URL;

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: external ?? 'http://localhost:4173',
    trace: 'retain-on-failure',
    serviceWorkers: 'block',
  },
  projects: [
    { name: 'desktop-chrome', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile-chrome', use: { ...devices['Pixel 7'] } },
  ],
  webServer: external
    ? undefined
    : { command: 'npm run build && npm run preview -- --port 4173 --strictPort', port: 4173, reuseExistingServer: true, timeout: 120_000 },
});
