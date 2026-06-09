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
  server: {
    port: 3000,
    open: true,
    proxy: {
      '/admin-api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // 代码分割优化
    rollupOptions: {
      output: {
        // 按模块分割chunk
        manualChunks: {
          // Element Plus UI库单独打包
          'element-plus': ['element-plus'],
          // ECharts图表库单独打包
          'echarts': ['echarts'],
          // Vue核心库
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          // VueFlow流程图
          'vue-flow': ['@vue-flow/core', '@vue-flow/additional-components'],
        },
        // 优化chunk文件名
        chunkFileNames: 'js/[name]-[hash].js',
        entryFileNames: 'js/[name]-[hash].js',
        assetFileNames: '[ext]/[name]-[hash].[ext]',
      },
    },
    // 启用Gzip压缩
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true, // 生产环境移除console
        drop_debugger: true,
      },
    },
    // chunk大小警告阈值
    chunkSizeWarningLimit: 1000,
    // 启用CSS代码分割
    cssCodeSplit: true,
    // 启用sourcemap（生产环境可关闭）
    sourcemap: false,
  },
  // 依赖预构建优化
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
  // 生产环境插件
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
})
