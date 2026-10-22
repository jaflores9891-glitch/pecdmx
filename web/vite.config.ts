import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    // jsdom es un navegador "de mentira" para que React pueda pintar dentro de los tests.
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
  },
})
