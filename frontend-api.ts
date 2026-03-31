import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios';

/**
 * 建议在前端项目里通过环境变量覆盖
 * 例如 Vite: VITE_API_BASE_URL=http://localhost:8088/aihub-api
 */
export const DEFAULT_API_BASE_URL = 'http://localhost:8088/aihub-api';

export type RoleCode = 'super_admin' | 'admin' | 'user';
export type ResourceTypeCode = 'app' | 'knowledge';
export type ResourceTypeEnumValue = 'APP' | 'KNOWLEDGE';

export interface R<T> {
  code: number;
  message: string;
  data: T;
}

export type AnyObject = Record<string, any>;

export interface ApiClientOptions {
  baseURL?: string;
  timeout?: number;
  getToken?: () => string | null | undefined;
}

export interface CaptchaResponse {
  captchaId: string;
  imageBase64: string;
  expireIn: number;
}

export interface LoginRequest {
  username: string;
  password: string;
  captchaId: string;
  captchaCode: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  username: string;
  nickname?: string;
  roleCode: RoleCode;
}

export interface RegisterUserRequest {
  username: string;
  password: string;
  nickname?: string;
  email?: string;
  phone?: string;
}

export interface UpdateUserRequest {
  nickname?: string;
  email?: string;
  phone?: string;
  avatar?: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

export interface UserInfoVO {
  id: number;
  username: string;
  nickname?: string;
  avatar?: string;
  email?: string;
  phone?: string;
  roleCode: RoleCode;
  roleName?: string;
  status: number;
  lastLoginAt?: string;
  createdAt: string;
}

export interface CreateAppRequest {
  appName: string;
  appCode: string;
  appType: string;
  difyApiKey: string;
  difyAppId?: string;
  difyBaseUrl?: string;
  description?: string;
  icon?: string;
  sort?: number;
}

export interface AppConfigVO {
  id: number;
  appName: string;
  appCode: string;
  appType: string;
  description?: string;
  icon?: string;
  sort: number;
  enabled: boolean;
  createdAt: string;
}

export interface CreateKnowledgeBaseRequest {
  kbName: string;
  kbCode: string;
  difyApiKey: string;
  difyDatasetId?: string;
  difyBaseUrl?: string;
  description?: string;
  icon?: string;
  indexingTechnique?: string;
  embeddingModel?: string;
  sort?: number;
}

export interface KnowledgeBaseVO {
  id: number;
  kbName: string;
  kbCode: string;
  description?: string;
  icon?: string;
  documentCount: number;
  wordCount: number;
  indexingTechnique?: string;
  embeddingModel?: string;
  sort: number;
  enabled: boolean;
  createdAt: string;
}

export interface GrantRequest {
  userId: number;
  resourceType: ResourceTypeEnumValue;
  resourceIds: number[];
}

export interface ChatRequest {
  query: string;
  conversationId?: string;
  inputs?: Record<string, any>;
}

export interface SseEventPayload {
  event: string;
  data: any;
}

export interface StreamChatHandlers {
  onOpen?: () => void;
  onMessage?: (data: any) => void;
  onMessageEnd?: (data: any) => void;
  onMessageReplace?: (data: any) => void;
  onPing?: (data: any) => void;
  onError?: (error: any) => void;
  onRawEvent?: (event: SseEventPayload) => void;
  onClose?: () => void;
}

export interface StreamChatOptions {
  signal?: AbortSignal;
  handlers?: StreamChatHandlers;
}

let tokenProvider: (() => string | null | undefined) | null = null;

export function createApiClient(options: ApiClientOptions = {}): AxiosInstance {
  tokenProvider = options.getToken ?? null;

  const client = axios.create({
    baseURL: options.baseURL ?? DEFAULT_API_BASE_URL,
    timeout: options.timeout ?? 30000,
  });

  client.interceptors.request.use((config) => {
    const token = tokenProvider?.();
    if (token) {
      config.headers = config.headers ?? {};
      if (!('Authorization' in config.headers)) {
        (config.headers as AnyObject).Authorization = `Bearer ${token}`;
      }
    }
    return config;
  });

  return client;
}

export const apiClient = createApiClient();

export function setStaticToken(token?: string | null): void {
  tokenProvider = () => token ?? null;
}

async function unwrap<T>(promise: Promise<AxiosResponse<R<T>>>): Promise<T> {
  const resp = await promise;
  const body = resp.data;
  if (body.code !== 200) {
    throw new Error(body.message || `API Error: ${body.code}`);
  }
  return body.data;
}

function toFormData(data: Record<string, any>): FormData {
  const form = new FormData();
  Object.entries(data).forEach(([key, value]) => {
    if (value === undefined || value === null) return;
    form.append(key, value as any);
  });
  return form;
}

export const authApi = {
  getCaptchaImage(config?: AxiosRequestConfig) {
    return unwrap<CaptchaResponse>(apiClient.get('/auth/captcha/image', config));
  },

  login(payload: LoginRequest, config?: AxiosRequestConfig) {
    return unwrap<LoginResponse>(apiClient.post('/auth/login', payload, config));
  },

  logout(config?: AxiosRequestConfig) {
    return unwrap<void>(apiClient.post('/auth/logout', undefined, config));
  },
};

export const userApi = {
  me(config?: AxiosRequestConfig) {
    return unwrap<UserInfoVO>(apiClient.get('/users/me', config));
  },

  updateMe(payload: UpdateUserRequest, config?: AxiosRequestConfig) {
    return unwrap<UserInfoVO>(apiClient.put('/users/me', payload, config));
  },

  changePassword(payload: ChangePasswordRequest, config?: AxiosRequestConfig) {
    return unwrap<void>(apiClient.put('/users/me/password', payload, config));
  },

  list(params?: { keyword?: string; page?: number; size?: number }, config?: AxiosRequestConfig) {
    return unwrap<UserInfoVO[]>(apiClient.get('/users/list', { ...config, params }));
  },

  create(payload: RegisterUserRequest, config?: AxiosRequestConfig) {
    return unwrap<UserInfoVO>(apiClient.post('/users/create', payload, config));
  },

  toggleStatus(id: number, status: 0 | 1, config?: AxiosRequestConfig) {
    return unwrap<void>(apiClient.put(`/users/${id}/status`, undefined, { ...config, params: { status } }));
  },

  changeRole(id: number, roleId: number, config?: AxiosRequestConfig) {
    return unwrap<void>(apiClient.put(`/users/${id}/role`, undefined, { ...config, params: { roleId } }));
  },

  delete(id: number, config?: AxiosRequestConfig) {
    return unwrap<void>(apiClient.delete(`/users/${id}`, config));
  },
};

export const appApi = {
  list(config?: AxiosRequestConfig) {
    return unwrap<AppConfigVO[]>(apiClient.get('/apps/list', config));
  },

  detail(id: number, config?: AxiosRequestConfig) {
    return unwrap<AppConfigVO>(apiClient.get(`/apps/${id}`, config));
  },

  create(payload: CreateAppRequest, config?: AxiosRequestConfig) {
    return unwrap<AppConfigVO>(apiClient.post('/apps/create', payload, config));
  },

  update(id: number, payload: CreateAppRequest, config?: AxiosRequestConfig) {
    return unwrap<AppConfigVO>(apiClient.put(`/apps/${id}`, payload, config));
  },

  delete(id: number, config?: AxiosRequestConfig) {
    return unwrap<void>(apiClient.delete(`/apps/${id}`, config));
  },

  getParameters(id: number, config?: AxiosRequestConfig) {
    return unwrap<AnyObject>(apiClient.get(`/apps/${id}/parameters`, config));
  },
};

export const resourceAuthApi = {
  grant(payload: GrantRequest, config?: AxiosRequestConfig) {
    return unwrap<void>(apiClient.post('/resource-auth/grant', payload, config));
  },

  revoke(userId: number, resourceType: ResourceTypeEnumValue, config?: AxiosRequestConfig) {
    return unwrap<void>(apiClient.post('/resource-auth/revoke', undefined, { ...config, params: { userId, resourceType } }));
  },

  getUserAuthorizations(userId: number, config?: AxiosRequestConfig) {
    return unwrap<{ app: number[]; knowledge: number[] }>(apiClient.get(`/resource-auth/user/${userId}`, config));
  },
};

export const chatApi = {
  stopGeneration(appId: number, taskId: string, config?: AxiosRequestConfig) {
    return unwrap<void>(apiClient.post('/chat/stop', undefined, { ...config, params: { appId, taskId } }));
  },

  feedback(appId: number, messageId: string, rating: string, config?: AxiosRequestConfig) {
    return unwrap<void>(apiClient.post('/chat/feedback', undefined, { ...config, params: { appId, messageId, rating } }));
  },

  suggested(appId: number, messageId: string, config?: AxiosRequestConfig) {
    return unwrap<AnyObject>(apiClient.get('/chat/suggested', { ...config, params: { appId, messageId } }));
  },

  audioToText(appId: number, file: File | Blob, config?: AxiosRequestConfig) {
    const form = toFormData({ appId, file });
    return unwrap<AnyObject>(apiClient.post('/chat/audio-to-text', form, {
      ...config,
      headers: {
        ...(config?.headers ?? {}),
        'Content-Type': 'multipart/form-data',
      },
    }));
  },

  /**
   * POST + SSE 流式聊天
   * 注意：该接口不是标准 EventSource GET，因此使用 fetch + ReadableStream 解析。
   */
  async streamChat(appId: number, payload: ChatRequest, options: StreamChatOptions = {}): Promise<void> {
    const token = tokenProvider?.() ?? null;
    const baseURL = (apiClient.defaults.baseURL || DEFAULT_API_BASE_URL).replace(/\/$/, '');

    const response = await fetch(`${baseURL}/chat/stream?appId=${encodeURIComponent(String(appId))}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(payload),
      signal: options.signal,
    });

    if (!response.ok) {
      const msg = `stream request failed: ${response.status} ${response.statusText}`;
      options.handlers?.onError?.(new Error(msg));
      throw new Error(msg);
    }

    options.handlers?.onOpen?.();

    const reader = response.body?.getReader();
    if (!reader) {
      options.handlers?.onClose?.();
      return;
    }

    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const events = buffer.split('\n\n');
      buffer = events.pop() ?? '';

      for (const block of events) {
        const event = parseSseBlock(block);
        if (!event) continue;

        options.handlers?.onRawEvent?.(event);

        switch (event.event) {
          case 'message':
            options.handlers?.onMessage?.(event.data);
            break;
          case 'message_end':
            options.handlers?.onMessageEnd?.(event.data);
            break;
          case 'message_replace':
            options.handlers?.onMessageReplace?.(event.data);
            break;
          case 'ping':
            options.handlers?.onPing?.(event.data);
            break;
          case 'error':
            options.handlers?.onError?.(event.data);
            break;
          default:
            break;
        }
      }
    }

    options.handlers?.onClose?.();
  },
};

function parseSseBlock(block: string): SseEventPayload | null {
  const lines = block.split('\n').map((line) => line.trim());
  if (lines.length === 0) return null;

  let eventName = 'message';
  const dataLines: string[] = [];

  for (const line of lines) {
    if (!line || line.startsWith(':')) continue;
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim() || eventName;
      continue;
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim());
      continue;
    }
  }

  if (dataLines.length === 0) return null;

  const raw = dataLines.join('\n');
  const data = safeJsonParse(raw);
  return { event: eventName, data };
}

function safeJsonParse(raw: string): any {
  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
}

export const conversationApi = {
  list(params: { appId: number; page?: number; limit?: number }, config?: AxiosRequestConfig) {
    return unwrap<AnyObject>(apiClient.get('/conversations', { ...config, params }));
  },

  messages(
    conversationId: string,
    params: { appId: number; firstId?: string; limit?: number },
    config?: AxiosRequestConfig,
  ) {
    return unwrap<AnyObject>(apiClient.get(`/conversations/${conversationId}/messages`, { ...config, params }));
  },

  rename(
    conversationId: string,
    params: { appId: number; name?: string; autoGenerate?: boolean },
    config?: AxiosRequestConfig,
  ) {
    return unwrap<AnyObject>(apiClient.put(`/conversations/${conversationId}/name`, undefined, { ...config, params }));
  },

  delete(conversationId: string, appId: number, config?: AxiosRequestConfig) {
    return unwrap<void>(apiClient.delete(`/conversations/${conversationId}`, { ...config, params: { appId } }));
  },
};

export const knowledgeApi = {
  list(config?: AxiosRequestConfig) {
    return unwrap<KnowledgeBaseVO[]>(apiClient.get('/knowledge/list', config));
  },

  detail(id: number, config?: AxiosRequestConfig) {
    return unwrap<KnowledgeBaseVO>(apiClient.get(`/knowledge/${id}`, config));
  },

  create(payload: CreateKnowledgeBaseRequest, config?: AxiosRequestConfig) {
    return unwrap<KnowledgeBaseVO>(apiClient.post('/knowledge/create', payload, config));
  },

  update(id: number, payload: CreateKnowledgeBaseRequest, config?: AxiosRequestConfig) {
    return unwrap<KnowledgeBaseVO>(apiClient.put(`/knowledge/${id}`, payload, config));
  },

  delete(id: number, config?: AxiosRequestConfig) {
    return unwrap<void>(apiClient.delete(`/knowledge/${id}`, config));
  },

  listDocuments(id: number, params?: { keyword?: string; page?: number; limit?: number }, config?: AxiosRequestConfig) {
    return unwrap<AnyObject>(apiClient.get(`/knowledge/${id}/documents`, { ...config, params }));
  },

  createDocumentByText(id: number, payload: AnyObject, config?: AxiosRequestConfig) {
    return unwrap<AnyObject>(apiClient.post(`/knowledge/${id}/documents/text`, payload, config));
  },

  createDocumentByFile(id: number, file: File | Blob, processRule?: string, config?: AxiosRequestConfig) {
    const form = toFormData({ file, processRule });
    return unwrap<AnyObject>(apiClient.post(`/knowledge/${id}/documents/file`, form, {
      ...config,
      headers: {
        ...(config?.headers ?? {}),
        'Content-Type': 'multipart/form-data',
      },
    }));
  },

  updateDocumentByText(id: number, docId: string, payload: AnyObject, config?: AxiosRequestConfig) {
    return unwrap<AnyObject>(apiClient.put(`/knowledge/${id}/documents/${docId}/text`, payload, config));
  },

  updateDocumentByFile(id: number, docId: string, file: File | Blob, processRule?: string, config?: AxiosRequestConfig) {
    const form = toFormData({ file, processRule });
    return unwrap<AnyObject>(apiClient.put(`/knowledge/${id}/documents/${docId}/file`, form, {
      ...config,
      headers: {
        ...(config?.headers ?? {}),
        'Content-Type': 'multipart/form-data',
      },
    }));
  },

  deleteDocument(id: number, docId: string, config?: AxiosRequestConfig) {
    return unwrap<AnyObject>(apiClient.delete(`/knowledge/${id}/documents/${docId}`, config));
  },

  indexingStatus(id: number, batch: string, config?: AxiosRequestConfig) {
    return unwrap<AnyObject>(apiClient.get(`/knowledge/${id}/documents/indexing-status`, { ...config, params: { batch } }));
  },

  listSegments(id: number, docId: string, config?: AxiosRequestConfig) {
    return unwrap<AnyObject>(apiClient.get(`/knowledge/${id}/documents/${docId}/segments`, config));
  },

  retrieve(id: number, payload: AnyObject, config?: AxiosRequestConfig) {
    return unwrap<AnyObject>(apiClient.post(`/knowledge/${id}/retrieve`, payload, config));
  },
};

export const fileApi = {
  upload(appId: number, file: File | Blob, config?: AxiosRequestConfig) {
    const form = toFormData({ appId, file });
    return unwrap<AnyObject>(apiClient.post('/files/upload', form, {
      ...config,
      headers: {
        ...(config?.headers ?? {}),
        'Content-Type': 'multipart/form-data',
      },
    }));
  },
};

export const systemApi = {
  health(config?: AxiosRequestConfig) {
    return unwrap<{ status: 'UP'; timestamp: string }>(apiClient.get('/system/health', config));
  },
};

export const api = {
  auth: authApi,
  user: userApi,
  app: appApi,
  resourceAuth: resourceAuthApi,
  chat: chatApi,
  conversation: conversationApi,
  knowledge: knowledgeApi,
  file: fileApi,
  system: systemApi,
};

export default api;
