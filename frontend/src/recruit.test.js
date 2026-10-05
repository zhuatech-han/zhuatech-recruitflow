// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import test from "node:test";
import assert from "node:assert/strict";
import { filtered, isoLocal } from "./recruit.js";
test("stage and job filters do not match hidden object keys or other jobs", () => {
  const rows = [
    { id: 1, jobId: 2, stage: "NEW", name: "TEST Alex" },
    { id: 2, jobId: 3, stage: "HIRED", name: "TEST Alex" },
  ];
  assert.equal(filtered(rows, "alex", "NEW", "2").length, 1);
  assert.equal(filtered(rows, "alex", "HIRED", "2").length, 0);
  assert.equal(filtered(rows, "name").length, 0);
});
test("local date conversion rejects invalid input and preserves offsets", () => {
  assert.equal(isoLocal("invalid"), null);
  assert.equal(
    isoLocal("2026-10-05T10:00:00+08:00"),
    "2026-10-05T02:00:00.000Z",
  );
});
