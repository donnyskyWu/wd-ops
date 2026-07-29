import baseConfig from './playwright.config'

export default {
  ...baseConfig,
  webServer: undefined,
  timeout: 90_000,
  use: {
    ...baseConfig.use,
    baseURL: 'http://localhost:5777',
    screenshot: 'on',
  },
}
