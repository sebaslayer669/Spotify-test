import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/lists': 'http://localhost:8080',
      '/spotify': 'http://localhost:8080'
    }
  }
});
