import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'
import visualizer from 'rollup-plugin-visualizer'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // Phase A：standalone :3000 为 harness / 非 Gate（ADR-049 D6）。
  // 目标态入口 = football-front :5777 + Gateway :48080（见 docs/delivery/FOOTBALL-OPS-BRANCH.md）。
  server: {
    host: '127.0.0.1',
    port: 3000,
    open: true,
    proxy: {
      '/admin-api': {
        target: 'http://localhost:48080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // 浠ｇ爜鍒嗗壊浼樺寲
    rollupOptions: {
      output: {
        // 鎸夋ā鍧楀垎鍓瞔hunk
        manualChunks: {
          // Element Plus UI搴撳崟鐙墦鍖?
          'element-plus': ['element-plus'],
          // ECharts鍥捐〃搴撳崟鐙墦鍖?
          'echarts': ['echarts'],
          // Vue鏍稿績搴?
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          // VueFlow娴佺▼鍥?
          'vue-flow': ['@vue-flow/core', '@vue-flow/additional-components'],
        },
        // 浼樺寲chunk鏂囦欢鍚?
        chunkFileNames: 'js/[name]-[hash].js',
        entryFileNames: 'js/[name]-[hash].js',
        assetFileNames: '[ext]/[name]-[hash].[ext]',
      },
    },
    // 鍚敤Gzip鍘嬬缉
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true, // 鐢熶骇鐜绉婚櫎console
        drop_debugger: true,
      },
    },
    // chunk澶у皬璀﹀憡闃堝€?
    chunkSizeWarningLimit: 1000,
    // 鍚敤CSS浠ｇ爜鍒嗗壊
    cssCodeSplit: true,
    // 鍚敤sourcemap锛堢敓浜х幆澧冨彲鍏抽棴锛?
    sourcemap: false,
  },
  // 渚濊禆棰勬瀯寤轰紭鍖?
  optimizeDeps: {
    include: [
      'vue',
      'vue-router',
      'pinia',
      'element-plus',
      'echarts',
    ],
    exclude: [],
  },
  // 鐢熶骇鐜鎻掍欢
  ...(process.env.NODE_ENV === 'production' ? {
    plugins: [
      visualizer({
        open: true,
        filename: 'dist/stats.html',
        gzipSize: true,
        brotliSize: true,
      }),
    ],
  } : {}),
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
})
