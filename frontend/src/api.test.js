// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import test from "node:test";
import assert from "node:assert/strict";
import { api, resetCsrf } from "./api.js";
test("expired session rejected by CSRF requests sign-in without replaying a write", async () => {
  const original = globalThis.fetch;
  const calls = [];
  globalThis.fetch = async (url, opts) => {
    calls.push([url, opts?.method]);
    if (url === "/api/auth/csrf")
      return new Response(
        JSON.stringify({ header: "X-CSRF-TOKEN", token: "TEST-token" }),
      );
    if (url === "/api/auth/me")
      return new Response('{"code":"UNAUTHENTICATED"}', { status: 401 });
    return new Response('{"code":"FORBIDDEN"}', { status: 403 });
  };
  try {
    resetCsrf();
    await assert.rejects(() => api("/jobs", "POST", { title: "TEST" }), {
      message: "UNAUTHENTICATED",
    });
    assert.equal(
      calls.filter(([url, method]) => url === "/api/jobs" && method === "POST")
        .length,
      1,
    );
    assert.equal(calls.at(-1)[0], "/api/auth/me");
  } finally {
    globalThis.fetch = original;
    resetCsrf();
  }
});
