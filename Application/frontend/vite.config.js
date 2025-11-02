import { fileURLToPath, URL } from 'node:url'
import path from 'node:path'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const SRC_DIR = fileURLToPath(new URL('./src', import.meta.url));

// https://vite.dev/config/
export default defineConfig({
    plugins: [
        vue(),
    ],
    appType: 'custom',
    build: {
        outDir: 'dist',
        emptyOutDir: true,
        rollupOptions: {
            input: fileURLToPath(new URL('./src/main.js', import.meta.url)),
            output: {
                entryFileNames: 'assets/main.js',
                chunkFileNames: 'assets/[name].js',
                assetFileNames: assetInfo => {
                    const originals = assetInfo.originalFileNames || [];
                    const first = originals[0];

                    if (first)
                    {
                        let rel = path.relative(SRC_DIR, first).replace(/\\/g, '/');

                        if (rel && !rel.startsWith('..'))
                        {
                            rel = rel.replace(/^assets\//, '');

                            return `assets/${rel}`;
                        }
                    }

                    return 'assets/[name][extname]';
                }
            }
        }
    },
    resolve: {
        alias: {
            '@': SRC_DIR
        },
    },
})
