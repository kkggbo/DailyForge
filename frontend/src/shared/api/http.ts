export type RequestMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export type QueryValue =
  | string
  | number
  | boolean
  | null
  | undefined
  | Array<string | number | boolean>;

export type QueryParams = Record<string, QueryValue>;

type RequestOptions = {
  method?: RequestMethod;
  body?: unknown;
  accessToken?: string;
  query?: QueryParams;
};

type ErrorPayload = {
  code?: string;
  message?: string;
};

type SuccessPayload<T> = {
  code: string;
  message: string;
  data: T;
};

export class ApiRequestError extends Error {
  code?: string;
  status: number;

  constructor(message: string, options: { code?: string; status: number }) {
    super(message);
    this.name = "ApiRequestError";
    this.code = options.code;
    this.status = options.status;
  }
}

export async function request<T>(path: string, options: RequestOptions = {}) {
  const response = await fetch(buildApiUrl(path, options.query), {
    method: options.method ?? "GET",
    headers: {
      "Content-Type": "application/json",
      ...(options.accessToken
        ? {
            Authorization: `Bearer ${options.accessToken}`
          }
        : {})
    },
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  });

  if (!response.ok) {
    const errorBody = (await tryParseJson(response)) as ErrorPayload | null;
    throw new ApiRequestError(
      errorBody?.message ?? `请求失败: ${response.status}`,
      {
        code: errorBody?.code,
        status: response.status
      }
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const payload = (await response.json()) as SuccessPayload<T>;
  return payload.data;
}

function buildApiUrl(path: string, query?: QueryParams) {
  const url = new URL(`/api${path}`, window.location.origin);

  if (query) {
    for (const [key, rawValue] of Object.entries(query)) {
      if (rawValue === null || rawValue === undefined) {
        continue;
      }

      if (Array.isArray(rawValue)) {
        for (const item of rawValue) {
          url.searchParams.append(key, String(item));
        }
        continue;
      }

      url.searchParams.set(key, String(rawValue));
    }
  }

  return `${url.pathname}${url.search}`;
}

async function tryParseJson(response: Response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}
