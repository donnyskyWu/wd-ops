/**
 * Ops API client — thin wrapper over Football requestClient.
 * Reuses accessStore Bearer token (see #/api/request.ts) and adds
 * X-Tenant-Id required by Gateway / oa-server for /admin-api/oa/**.
 *
 * Hand-maintained (not copied from ops-platform-ui-vue). mount-ops-all.py
 * must preserve / recreate this file after remount.
 */
import type { AxiosRequestConfig } from '@vben/request';

import { useAccessStore } from '@vben/stores';

import { requestClient } from '#/api/request';

/** Legacy-compatible alias matching ops-platform-ui-vue request helper shape. */
export interface OpsRequestConfig {
  url: string;
  params?: Record<string, unknown>;
  data?: unknown;
  timeout?: number;
}

/** oa-server DevAuthFilter requires X-Tenant-Id (distinct from Football tenant-id header). */
function resolveOpsTenantId(): string {
  try {
    const accessStore = useAccessStore();
    const tenantId = accessStore.tenantId;
    if (tenantId != null && String(tenantId) !== '') {
      return String(tenantId);
    }
  } catch {
    // Pinia may not be ready during module init; oa-server dev default.
  }
  return '1';
}

/** Strip accidental /admin-api prefix — baseURL already includes it. */
function normalizeOpsUrl(url: string): string {
  if (url.startsWith('/admin-api/')) {
    return url.slice('/admin-api'.length);
  }
  if (url.startsWith('admin-api/')) {
    return `/${url.slice('admin-api'.length)}`;
  }
  return url;
}

function withOpsHeaders(config: AxiosRequestConfig = {}): AxiosRequestConfig {
  const headers = {
    ...(config.headers as Record<string, string> | undefined),
    'X-Tenant-Id': resolveOpsTenantId(),
  };
  return { ...config, headers };
}

function toConfig(
  urlOrConfig: OpsRequestConfig | string,
  params?: Record<string, unknown>,
): OpsRequestConfig {
  if (typeof urlOrConfig === 'string') {
    return { url: urlOrConfig, params };
  }
  return urlOrConfig;
}

export const opsRequest = {
  get<T>(
    urlOrConfig: OpsRequestConfig | string,
    params?: Record<string, unknown>,
    config?: AxiosRequestConfig,
  ): Promise<T> {
    const req = toConfig(urlOrConfig, params);
    const axiosConfig: AxiosRequestConfig = {
      ...(config ?? {}),
      ...(req.params ? { params: req.params } : {}),
      ...(req.timeout ? { timeout: req.timeout } : {}),
    };
    return requestClient.get<T>(normalizeOpsUrl(req.url), withOpsHeaders(axiosConfig));
  },
  post<T>(
    urlOrConfig: OpsRequestConfig | string,
    data?: unknown,
    config?: AxiosRequestConfig,
  ): Promise<T> {
    const req = typeof urlOrConfig === 'string' ? { url: urlOrConfig } : urlOrConfig;
    const axiosConfig = withOpsHeaders({
      ...(config ?? {}),
      ...(req.params ? { params: req.params } : {}),
      ...(req.timeout ? { timeout: req.timeout } : {}),
    });
    return requestClient.post<T>(
      normalizeOpsUrl(req.url),
      data ?? req.data,
      axiosConfig,
    );
  },
  put<T>(
    urlOrConfig: OpsRequestConfig | string,
    data?: unknown,
    config?: AxiosRequestConfig,
  ): Promise<T> {
    const req = typeof urlOrConfig === 'string' ? { url: urlOrConfig } : urlOrConfig;
    const axiosConfig = withOpsHeaders({
      ...(config ?? {}),
      ...(req.params ? { params: req.params } : {}),
      ...(req.timeout ? { timeout: req.timeout } : {}),
    });
    return requestClient.put<T>(normalizeOpsUrl(req.url), data ?? req.data, axiosConfig);
  },
  delete<T>(
    urlOrConfig: OpsRequestConfig | string,
    params?: Record<string, unknown>,
    config?: AxiosRequestConfig,
  ): Promise<T> {
    const req = toConfig(urlOrConfig, params);
    const axiosConfig = withOpsHeaders({
      ...(config ?? {}),
      ...(req.params ? { params: req.params } : {}),
      ...(req.timeout ? { timeout: req.timeout } : {}),
    });
    return requestClient.delete<T>(normalizeOpsUrl(req.url), axiosConfig);
  },
};

/** Legacy-compatible alias matching ops-platform-ui-vue `export const request`. */
export const request = {
  get<T>(configOrUrl: OpsRequestConfig | string, params?: Record<string, unknown>): Promise<T> {
    return opsRequest.get<T>(configOrUrl, params);
  },
  post<T>(configOrUrl: OpsRequestConfig | string, data?: unknown): Promise<T> {
    if (typeof configOrUrl === 'string') {
      return opsRequest.post<T>(configOrUrl, data);
    }
    return opsRequest.post<T>(configOrUrl, configOrUrl.data);
  },
  put<T>(configOrUrl: OpsRequestConfig | string, data?: unknown): Promise<T> {
    if (typeof configOrUrl === 'string') {
      return opsRequest.put<T>(configOrUrl, data);
    }
    return opsRequest.put<T>(configOrUrl, configOrUrl.data);
  },
  delete<T>(configOrUrl: OpsRequestConfig | string, params?: Record<string, unknown>): Promise<T> {
    return opsRequest.delete<T>(configOrUrl, params);
  },
};

/**
 * Axios-like default for `import service from './client'`
 * (ops football-user.ts rewritten from `@/utils/request` default export).
 */
const service = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return requestClient.get<T>(normalizeOpsUrl(url), withOpsHeaders(config ?? {}));
  },
  post<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return requestClient.post<T>(normalizeOpsUrl(url), data, withOpsHeaders(config ?? {}));
  },
  put<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return requestClient.put<T>(normalizeOpsUrl(url), data, withOpsHeaders(config ?? {}));
  },
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return requestClient.delete<T>(normalizeOpsUrl(url), withOpsHeaders(config ?? {}));
  },
};

export default service;
