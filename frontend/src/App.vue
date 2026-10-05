<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, reactive, computed, onMounted, nextTick } from "vue";
import {
  LayoutDashboard,
  BriefcaseBusiness,
  Users,
  CalendarDays,
  ChartNoAxesColumn,
  Settings,
  History,
  Plus,
  Search,
  ArrowLeft,
  LogOut,
  RefreshCw,
  ChevronRight,
  X,
  Download,
  Menu,
  Globe,
} from "@lucide/vue";
import { api, resetCsrf, upload, applyPublic, downloadReport } from "./api.js";
import { forms } from "./schema.js";
import { names, errors, filtered, isoLocal } from "./recruit.js";
import { money, pageRows } from "./format.js";
const lang = ref(localStorage.getItem("recruitflow-language") || "zh");
const t = (zh, en) => (lang.value === "en" ? en : zh);
const label = (v) => names[v]?.[lang.value === "en" ? 1 : 0] || v;
const eventLabel = (action) => {
  const events = {
    JOB_SAVED: ["岗位已保存", "Job saved"],
    JOB_SUBMIT: ["岗位已送审", "Opening submitted"],
    JOB_APPROVE: ["岗位已批准", "Opening approved"],
    APPLICANT_CREATED: ["申请已录入", "Application created"],
    PROFILE_UPDATED: ["资料已更新", "Profile updated"],
    APPLICANT_SCREEN: ["开始初筛", "Screening started"],
    APPLICANT_ASSIGN: ["申请已交接", "Application transferred"],
    APPLICANT_WITHDRAW: ["申请已撤回", "Application withdrawn"],
    APPLICANT_REJECT: ["申请已拒绝", "Application rejected"],
    APPLICANT_HIRE: ["已登记入职", "Joining recorded"],
    PUBLIC_APPLICATION: ["在线投递已收到", "Online application received"],
    INTERVIEW_SCHEDULED: ["面试已安排", "Interview scheduled"],
    INTERVIEW_COMPLETED: ["面试反馈已记录", "Interview feedback recorded"],
    INTERVIEW_FEEDBACK: ["面试反馈已记录", "Interview feedback recorded"],
    INTERVIEW_CANCELLED: ["面试已取消", "Interview cancelled"],
    OFFER_DRAFT_SAVED: ["录用建议已保存", "Offer proposal saved"],
    OFFER_DRAFT_DELETED: ["建议草稿已删除", "Offer draft deleted"],
    OFFER_SUBMIT: ["建议已送审", "Offer submitted"],
    OFFER_APPROVE: ["建议已批准", "Offer approved"],
    OFFER_RETURN: ["建议已退回", "Offer returned"],
    OFFER_ISSUE: ["送达已登记", "Delivery recorded"],
    OFFER_ACCEPT: ["接受已登记", "Acceptance recorded"],
    OFFER_DECLINE: ["婉拒已登记", "Decline recorded"],
    OFFER_REVOKE: ["建议已撤销", "Offer revoked"],
    OFFER_EXPIRE: ["已释放过期建议", "Expired offer released"],
    NOTE_ADDED: ["跟进已追加", "Note added"],
    RESUME_UPDATED: ["简历已保存", "Resume saved"],
    PERSONAL_DATA_REDACTED: ["个人资料已匿名化", "Personal data redacted"],
  };
  return events[action]?.[lang.value === "en" ? 1 : 0] || action;
};
const errorText = (e) =>
  errors[e]?.[lang.value === "en" ? 1 : 0] ||
  t(
    "操作未完成，请检查当前记录与权限：",
    "Operation failed; review record and permissions: ",
  ) + e;
const me = ref(null),
  view = ref("dashboard"),
  busy = ref(false),
  error = ref(""),
  notice = ref(""),
  mobileNav = ref(false),
  publicMode = ref(location.pathname === "/careers"),
  publicData = ref(null),
  publicJob = ref(null),
  publicFile = ref(null),
  submitted = ref(false);
const credentials = reactive({ username: "", password: "" }),
  application = reactive({
    name: "",
    email: "",
    phone: "",
    introduction: "",
    consent: false,
  });
const jobs = ref([]),
  apps = ref([]),
  calendar = ref([]),
  reports = ref([]),
  audits = ref([]),
  catalog = ref({
    people: [],
    departments: [],
    dictionaries: [],
    currency: "CNY",
  }),
  detail = ref(null),
  selectedJob = ref(null);
const adminType = ref("users"),
  adminData = reactive({
    users: [],
    roles: [],
    permissions: [],
    menus: [],
    departments: [],
    dictionaries: [],
    settings: [],
  });
const search = ref(""),
  state = ref(""),
  jobFilter = ref(""),
  page = ref(1),
  descending = ref(true);
const modal = ref(null),
  form = reactive({}),
  dialog = ref(null),
  priorFocus = ref(null);
const can = (p) => me.value?.permissions?.includes(p);
const roleScope = () => me.value?.scope;
const icons = {
  dashboard: LayoutDashboard,
  jobs: BriefcaseBusiness,
  applicants: Users,
  interviews: CalendarDays,
  reports: ChartNoAxesColumn,
  admin: Settings,
  audit: History,
};
const menus = computed(() => me.value?.menus || []);
const activeA = computed(() => detail.value?.applicant);
const activeJob = computed(() => detail.value?.job || selectedJob.value);
const isManager = (j) =>
  can("approve") && (roleScope() === "ALL" || me.value?.id === j?.managerId);
const finished = (a) => ["HIRED", "REJECTED", "WITHDRAWN"].includes(a?.stage);
const workflowVisible = computed(
  () =>
    (!finished(activeA.value) &&
      (can("recruit") || isManager(activeJob.value))) ||
    (can("privacy") && finished(activeA.value) && !activeA.value?.redactedAt),
);
const stages = [
  "NEW",
  "SCREENING",
  "INTERVIEW",
  "OFFER",
  "ACCEPTED",
  "HIRED",
  "REJECTED",
  "WITHDRAWN",
];
const people = (permission, dept) =>
  catalog.value.people.filter(
    (x) =>
      x.permissions.includes(permission) &&
      (!dept || x.departmentId === Number(dept)),
  );
const person = (id) =>
  catalog.value.people.find((x) => x.id === id)?.name || "#" + id;
const scopeName = (v) =>
  ({
    ALL: t("全部", "All"),
    DEPARTMENT: t("本部门", "Department"),
    ASSIGNED: t("本人任务", "Assigned tasks"),
  })[v] || v;
const parameterName = (v) =>
  ({
    companyName: t("招聘团队名称", "Hiring team name"),
    currency: t("薪资币种", "Salary currency"),
    publicIntake: t("开放在线投递", "Enable online applications"),
    privacyNotice: t("申请资料用途说明", "Application privacy notice"),
    privacyContact: t("资料处理联系人", "Privacy contact"),
  })[v] || v;
const permissionName = (v) => {
  const n = adminData.permissions.find((p) => p.code === v)?.name;
  return n?.split(" / ")[lang.value === "en" ? 1 : 0] || n || v;
};
const jobTitle = (id) => jobs.value.find((x) => x.id === id)?.title || "#" + id;
const appName = (id) => apps.value.find((x) => x.id === id)?.name || "#" + id;
const date = (x) =>
  x
    ? new Date(x).toLocaleString(lang.value === "en" ? "en-GB" : "zh-CN", {
        dateStyle: "medium",
        timeStyle: "short",
      })
    : "—";
const today = () => new Date().toISOString().slice(0, 10);
const localTime = (addMinutes = 0) => {
  const d = new Date(Date.now() + addMinutes * 60000);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
};
/** 单次提交保留失败表单，实时版本由服务端检查。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function run(fn) {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  notice.value = "";
  try {
    await fn();
  } catch (e) {
    error.value = errorText(e.message);
    if (e.message === "UNAUTHENTICATED") {
      me.value = null;
      resetCsrf();
    }
  } finally {
    busy.value = false;
  }
}
async function load() {
  if (!me.value) return;
  const values = can("read")
    ? await Promise.all([
        api("/jobs"),
        api("/applicants"),
        api("/interviews"),
        api("/catalog"),
      ])
    : [
        [],
        [],
        [],
        { people: [], departments: [], dictionaries: [], currency: "CNY" },
      ];
  [jobs.value, apps.value, calendar.value, catalog.value] = values;
  if (can("report")) reports.value = await api("/reports");
  if (can("audit")) audits.value = await api("/audit");
  if (can("admin"))
    for (const k of Object.keys(adminData))
      adminData[k] = await api("/admin/" + k);
  if (detail.value)
    detail.value = await api("/applicants/" + detail.value.applicant.id);
  if (selectedJob.value)
    selectedJob.value =
      jobs.value.find((x) => x.id === selectedJob.value.id) || null;
}
async function signIn() {
  await run(async () => {
    await api("/auth/login", "POST", credentials);
    resetCsrf();
    credentials.password = "";
    me.value = await api("/auth/me");
    publicMode.value = false;
    view.value = menus.value[0]?.code || "dashboard";
    await load();
  });
}
async function signOut() {
  await run(async () => {
    await api("/auth/logout", "POST");
    resetCsrf();
    me.value = null;
    detail.value = null;
    selectedJob.value = null;
  });
}
function changeLanguage() {
  lang.value = lang.value === "zh" ? "en" : "zh";
  localStorage.setItem("recruitflow-language", lang.value);
  document.documentElement.lang = lang.value;
}
function navigate(code) {
  view.value = code;
  search.value = "";
  state.value = "";
  jobFilter.value = "";
  page.value = 1;
  detail.value = null;
  selectedJob.value = null;
  mobileNav.value = false;
  error.value = "";
  notice.value = "";
}
async function openApplicant(id) {
  await run(async () => {
    detail.value = await api("/applicants/" + id);
    view.value = "applicants";
  });
}
const sourceRows = computed(() => {
  if (view.value === "jobs") return jobs.value;
  if (view.value === "applicants") return apps.value;
  if (view.value === "interviews") return calendar.value;
  if (view.value === "reports")
    return reports.value.map((x) => ({ ...x, id: x.jobId }));
  if (view.value === "audit") return audits.value;
  if (view.value === "admin") return adminData[adminType.value];
  return [];
});
const filteredRows = computed(() =>
  filtered(sourceRows.value, search.value, state.value, jobFilter.value),
);
const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredRows.value.length / 10)),
);
const rows = computed(() =>
  pageRows(
    filteredRows.value,
    "",
    Math.min(page.value, totalPages.value),
    10,
    descending.value,
  ),
);
const jobApplicants = computed(() =>
  apps.value.filter((a) => a.jobId === selectedJob.value?.id),
);
const upcoming = computed(() =>
  calendar.value
    .filter((x) => x.status === "SCHEDULED")
    .toSorted((a, b) => new Date(a.startsAt) - new Date(b.startsAt))
    .slice(0, 8),
);
/** 模态框用原生焦点管理，Escape关闭并回到触发点。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function openForm(
  kind,
  title,
  fields,
  data = {},
  path = "",
  method = "POST",
) {
  priorFocus.value = document.activeElement;
  modal.value = { kind, title, fields, path, method };
  for (const k of Object.keys(form)) delete form[k];
  Object.assign(form, data);
  error.value = "";
  await nextTick();
  dialog.value.showModal();
  dialog.value.querySelector("input,select,textarea,button")?.focus();
}
function closeForm() {
  if (busy.value) return;
  dialog.value?.close();
  modal.value = null;
  priorFocus.value?.focus();
}
const f = (key, zh, en, type = "text", options = null, optional = false) => ({
  key,
  zh,
  en,
  type,
  options,
  optional,
});
function opts(field) {
  if (field.options === "dict:source" && form.source === "PUBLIC")
    return [{ id: "PUBLIC", name: t("在线投递", "Online application") }];
  if (Array.isArray(field.options))
    return field.options.map((x) => ({ id: x[0], name: t(x[1], x[2]) }));
  if (field.options === "departments") return catalog.value.departments;
  if (field.options?.startsWith("people:"))
    return people(
      field.options.split(":")[1],
      form.departmentId || activeJob.value?.departmentId,
    );
  if (field.options?.startsWith("dict:"))
    return catalog.value.dictionaries
      .filter((x) => x.type === field.options.split(":")[1])
      .map((x) => ({ id: x.code, name: t(x.name, x.nameEn) }));
  return (adminData[field.options] || []).map((x) => ({
    id: field.options === "permissions" ? x.code : x.id,
    name: x.name || x.displayName,
  }));
}
const commandFields = [
  f(
    "note",
    "说明／外部记录编号",
    "Reason / external record reference",
    "textarea",
  ),
];
function jobForm(j = null) {
  openForm(
    "job",
    t(j ? "修改岗位" : "新建招聘岗位", j ? "Edit job" : "New job opening"),
    [
      f("title", "岗位名称", "Job title"),
      f("location", "工作地点", "Location"),
      f(
        "employmentType",
        "用工形式",
        "Employment type",
        "select",
        "dict:employment",
      ),
      f("departmentId", "所属部门", "Department", "select", "departments"),
      f("ownerId", "招聘人员", "Recruiter", "select", "people:recruit"),
      f(
        "managerId",
        "审批负责人",
        "Hiring manager",
        "select",
        "people:approve",
      ),
      f("headcount", "招聘人数", "Headcount", "number"),
      f(
        "description",
        "岗位职责与要求",
        "Responsibilities and requirements",
        "textarea",
      ),
    ],
    j
      ? { ...j }
      : {
          departmentId: catalog.value.departments[0]?.id,
          headcount: 1,
          employmentType: "FULL_TIME",
        },
    "/jobs" + (j ? "/" + j.id : ""),
    j ? "PUT" : "POST",
  );
}
function jobAction(j, action) {
  const fields =
    action === "assign"
      ? [
          ...commandFields,
          f(
            "ownerId",
            "接任招聘人员",
            "New recruiter",
            "select",
            "people:recruit",
          ),
        ]
      : commandFields;
  selectedJob.value = j;
  openForm(
    "command",
    t("岗位操作：", "Job action: ") +
      label(
        {
          submit: "PENDING",
          approve: "OPEN",
          return: "DRAFT",
          pause: "PAUSED",
          resume: "OPEN",
          close: "CLOSED",
          assign: "交接 / Transfer",
        }[action],
      ),
    fields,
    { revision: j.revision },
    `/jobs/${j.id}/actions/${action}`,
  );
}
function applicantForm(a = null) {
  openForm(
    "applicant",
    t(
      a ? "修改申请资料" : "录入候选人",
      a ? "Edit application" : "New applicant",
    ),
    [
      f(
        "jobId",
        "招聘岗位",
        "Job",
        "select",
        jobs.value
          .filter((x) => ["OPEN", "PAUSED"].includes(x.status))
          .map((x) => [x.id, x.title, x.title]),
      ),
      f("name", "姓名", "Name"),
      f("email", "联系邮箱", "Email", "email"),
      f("phone", "联系电话", "Phone", "text", null, true),
      f("introduction", "经历与意向", "Experience and interest", "textarea"),
      f("source", "来源", "Source", "select", "dict:source"),
      f(
        "consentReference",
        "资料授权／合法取得记录",
        "Consent / lawful collection reference",
        "textarea",
      ),
    ],
    a
      ? { ...a }
      : {
          jobId:
            selectedJob.value?.id ||
            jobs.value.find((x) => x.status === "OPEN")?.id,
          source: "MANUAL",
        },
    "/applicants" + (a ? "/" + a.id : ""),
    a ? "PUT" : "POST",
  );
}
function appAction(action) {
  const a = activeA.value;
  let fields = [...commandFields];
  if (action === "hire")
    fields.push(
      f("employeeCode", "正式员工编号", "Employee reference"),
      f("joinedDate", "实际入职日期", "Actual joining date", "date"),
    );
  if (action === "assign")
    fields.push(
      f("ownerId", "接任招聘人员", "New recruiter", "select", "people:recruit"),
    );
  openForm(
    "command",
    t("申请操作：", "Application action: ") +
      {
        screen: t("初筛", "Screen"),
        withdraw: t("撤回", "Withdraw"),
        reject: t("拒绝", "Reject"),
        hire: t("登记入职", "Record joining"),
        assign: t("交接", "Transfer"),
      }[action],
    fields,
    { revision: a.revision, joinedDate: today() },
    `/applicants/${a.id}/actions/${action}`,
  );
}
function interviewForm() {
  const a = activeA.value;
  openForm(
    "interview",
    t("安排面试", "Schedule interview"),
    [
      f("interviewerId", "面试官", "Interviewer", "select", "people:interview"),
      f("title", "面试名称", "Interview title"),
      f("location", "地点／会议链接", "Location / meeting link"),
      f("startsAt", "开始时间", "Start", "datetime-local"),
      f("endsAt", "结束时间", "End", "datetime-local"),
    ],
    { revision: a.revision, startsAt: localTime(30), endsAt: localTime(60) },
    `/applicants/${a.id}/interviews`,
  );
}
function feedbackForm(i) {
  openForm(
    "feedback",
    t("记录面试反馈", "Record interview feedback"),
    [
      f("recommendation", "人工建议", "Recommendation", "select", [
        ["POSITIVE", "建议继续", "Proceed"],
        ["CONCERNS", "有待核实", "Concerns"],
        ["NEUTRAL", "中立", "Neutral"],
        ["NO_SHOW", "未出席", "No show"],
      ]),
      f("feedback", "反馈记录", "Feedback", "textarea"),
    ],
    { revision: activeA.value.revision },
    `/interviews/${i.id}/feedback`,
  );
}
function offerForm(o = null) {
  openForm(
    "offer",
    t(
      o ? "修改录用建议" : "录用建议",
      o ? "Edit offer proposal" : "Offer proposal",
    ),
    [
      f(
        "salary",
        `薪资 (${catalog.value.currency})`,
        `Salary (${catalog.value.currency})`,
        "money",
      ),
      f("payPeriod", "薪资周期", "Pay period", "select", [
        ["MONTHLY", "月薪", "Monthly"],
        ["ANNUAL", "年薪", "Annual"],
      ]),
      f("startDate", "约定入职日期", "Agreed start date", "date"),
      f("expiresAt", "答复有效期", "Response deadline", "datetime-local"),
      f("terms", "岗位与录用条款", "Position and terms", "textarea"),
    ],
    o
      ? {
          ...o,
          revision: activeA.value.revision,
          expiresAt: new Date(
            new Date(o.expiresAt).getTime() -
              new Date(o.expiresAt).getTimezoneOffset() * 60000,
          )
            .toISOString()
            .slice(0, 16),
        }
      : {
          revision: activeA.value.revision,
          payPeriod: "MONTHLY",
          startDate: today(),
          expiresAt: localTime(7 * 1440),
        },
    `/applicants/${activeA.value.id}/offers` + (o ? "/" + o.id : ""),
    o ? "PUT" : "POST",
  );
}
function offerAction(o, action) {
  openForm(
    "command",
    t("录用操作：", "Offer action: ") +
      t(
        {
          submit: "送审",
          approve: "批准",
          return: "退回",
          issue: "记录送达",
          accept: "记录接受",
          decline: "记录婉拒",
          revoke: "撤销",
          expire: "释放过期名额",
          delete: "删除草稿",
        }[action],
        action,
      ),
    commandFields,
    { revision: activeA.value.revision },
    `/offers/${o.id}/${action === "delete" ? "delete" : "actions/" + action}`,
  );
}
function adminForm(row = null) {
  let fields = forms[adminType.value];
  if (adminType.value === "settings" && row)
    fields = [
      f(
        "value",
        parameterName(row.code),
        parameterName(row.code),
        row.code === "publicIntake"
          ? "select"
          : row.code === "privacyNotice"
            ? "textarea"
            : "text",
        row.code === "publicIntake"
          ? [
              ["true", "开启", "Enabled"],
              ["false", "关闭", "Disabled"],
            ]
          : null,
      ),
    ];
  let data = row
    ? { ...row, password: "" }
    : {
        enabled: true,
        scope: "DEPARTMENT",
        permissions: [],
        departmentId: catalog.value.departments[0]?.id,
      };
  openForm(
    "admin",
    t(row ? "修改设置" : "新增记录", row ? "Edit record" : "New record"),
    fields,
    data,
    "/admin/" + adminType.value + (row ? "/" + row.id : ""),
    row ? "PUT" : "POST",
  );
}
function removeForm(path, revision) {
  openForm(
    "delete",
    t("删除草稿／未引用记录", "Delete draft / unreferenced record"),
    [],
    { revision },
    path,
    "DELETE",
  );
}
async function saveForm() {
  await run(async () => {
    const m = modal.value;
    const body = { ...form };
    for (const field of m.fields) {
      if (["number", "select"].includes(field.type) && field.key.endsWith("Id"))
        body[field.key] = body[field.key] ? Number(body[field.key]) : null;
      if (field.type === "datetime-local")
        body[field.key] = isoLocal(body[field.key]);
      if (field.type === "number") body[field.key] = Number(body[field.key]);
    }
    if (m.kind === "delete") {
      await api(
        m.path +
          (body.revision === undefined ? "" : "?revision=" + body.revision),
        "DELETE",
      );
      detail.value = null;
      selectedJob.value = null;
    } else if (m.kind === "password") {
      await api("/auth/password", "POST", body);
      resetCsrf();
      me.value = null;
    } else await api(m.path, m.method, body);
    dialog.value.close();
    modal.value = null;
    await load();
    notice.value = t("已保存", "Saved");
    priorFocus.value?.focus();
  });
}
async function uploadResume(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  await run(async () => {
    await upload(
      `/applicants/${activeA.value.id}/resume?revision=${activeA.value.revision}`,
      file,
    );
    await load();
    notice.value = t("简历已保存", "Resume saved");
  });
  event.target.value = "";
}
async function openCareers() {
  await run(async () => {
    publicData.value = await api("/public/jobs");
    publicMode.value = true;
    publicJob.value = null;
    submitted.value = false;
  });
}
async function sendApplication() {
  await run(async () => {
    await applyPublic(
      {
        ...application,
        jobId: publicJob.value.id,
        noticeHash: publicData.value.noticeHash,
      },
      publicFile.value,
    );
    submitted.value = true;
    publicFile.value = null;
    application.name = "";
    application.email = "";
    application.phone = "";
    application.introduction = "";
    application.consent = false;
  });
}
onMounted(async () => {
  document.documentElement.lang = lang.value;
  try {
    me.value = await api("/auth/me");
    if (me.value) await load();
  } catch (e) {
    if (e.message !== "UNAUTHENTICATED") error.value = errorText(e.message);
  }
  if (publicMode.value) await openCareers();
});
</script>
<template>
  <div v-if="publicMode" class="careers">
    <header class="public-header">
      <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
        ><img src="/brand/logo.jpg" alt="知华科技" /></a
      ><strong>{{ publicData?.companyName || "RecruitFlow" }}</strong>
      <div class="grow"></div>
      <button @click="changeLanguage">
        <Globe :size="16" />{{ t("English", "中文") }}</button
      ><button @click="publicMode = false">
        {{ t("员工登录", "Staff sign in") }}
      </button>
    </header>
    <main class="career-main">
      <div v-if="error" class="alert" role="alert">{{ error }}</div>
      <button
        v-if="publicJob"
        class="link"
        @click="
          publicJob = null;
          submitted = false;
        "
      >
        <ArrowLeft :size="16" />{{ t("全部岗位", "All openings") }}
      </button>
      <template v-if="!publicJob"
        ><p class="eyebrow">{{ t("招聘岗位", "JOB OPENINGS") }}</p>
        <h1>{{ t("加入团队", "Join the team") }}</h1>
        <p v-if="!publicData?.enabled" class="empty">
          {{
            t("当前未开放在线投递", "Online applications are currently closed")
          }}
        </p>
        <p v-else-if="!publicData.jobs.length" class="empty">
          {{ t("暂无开放岗位", "No open positions") }}
        </p>
        <button
          v-for="j in publicData?.jobs"
          :key="j.id"
          class="job-link"
          @click="
            publicJob = j;
            submitted = false;
          "
        >
          <span
            ><strong>{{ j.title }}</strong
            ><small
              >{{ j.location }} · {{ label(j.employmentType) }} ·
              {{ j.headcount }} {{ t("人", "positions") }}</small
            ></span
          ><ChevronRight :size="20" /></button
      ></template>
      <template v-else
        ><p class="eyebrow">{{ publicJob.location }}</p>
        <h1>{{ publicJob.title }}</h1>
        <p class="preserve">{{ publicJob.description }}</p>
        <section v-if="submitted" class="confirmation">
          <h2>{{ t("投递已收到", "Application received") }}</h2>
          <p>
            {{
              t(
                "招聘团队将按资料用途说明处理申请。重复投递不会覆盖已有申请。",
                "The team will handle applications according to the privacy notice. Duplicate submissions do not replace earlier records.",
              )
            }}
          </p>
        </section>
        <form v-else class="application-form" @submit.prevent="sendApplication">
          <h2>{{ t("申请这个岗位", "Apply for this position") }}</h2>
          <div class="form-grid">
            <label
              >{{ t("姓名", "Name")
              }}<input
                v-model="application.name"
                maxlength="160"
                required
                autocomplete="name" /></label
            ><label
              >{{ t("联系邮箱", "Email")
              }}<input
                v-model="application.email"
                type="email"
                maxlength="254"
                required
                autocomplete="email" /></label
            ><label
              >{{ t("联系电话（选填）", "Phone (optional)")
              }}<input
                v-model="application.phone"
                maxlength="80"
                autocomplete="tel" /></label
            ><label
              >{{
                t("PDF简历（选填，最多3MB）", "PDF resume (optional, max 3MB)")
              }}<input
                type="file"
                accept="application/pdf,.pdf"
                @change="
                  publicFile = $event.target.files?.[0] || null
                " /></label
            ><label class="full"
              >{{ t("经历与意向", "Experience and interest")
              }}<textarea
                v-model="application.introduction"
                required
                maxlength="4000"
                rows="5"
              ></textarea>
            </label>
          </div>
          <details open class="privacy">
            <summary>{{ t("资料用途说明", "Privacy notice") }}</summary>
            <p class="preserve">{{ publicData.notice }}</p>
            <p>
              {{ t("资料处理联系：", "Privacy contact: ")
              }}{{ publicData.contact }}
            </p>
          </details>
          <label class="check"
            ><input v-model="application.consent" type="checkbox" required />{{
              t(
                "我已阅读资料用途说明并同意提交申请资料",
                "I have read the privacy notice and agree to submit my application",
              )
            }}</label
          ><button class="primary" :disabled="busy || !application.consent">
            {{
              busy
                ? t("提交中…", "Submitting…")
                : t("提交申请", "Submit application")
            }}
          </button>
        </form></template
      >
    </main>
    <footer>
      RecruitFlow 1.0 ·
      {{
        t("知华科技公开源码学习版", "ZhuaTech non-commercial source edition")
      }}
      ·
      <a href="https://www.zhuatech.cn/">{{
        t("商业咨询", "Commercial enquiries")
      }}</a>
    </footer>
  </div>
  <div v-else-if="!me" class="login-page">
    <div class="login-panel">
      <a
        href="https://www.zhuatech.cn/"
        target="_blank"
        rel="noopener"
        class="brand"
        ><img src="/brand/logo.jpg" alt="知华科技" /><span
          >知华科技<span>RecruitFlow</span></span
        ></a
      >
      <div class="login-heading">
        <p class="eyebrow">{{ t("招聘协作", "RECRUITMENT WORKSPACE") }}</p>
        <h1>{{ t("登录工作台", "Sign in to your workspace") }}</h1>
      </div>
      <form @submit.prevent="signIn">
        <label
          >{{ t("登录账号", "Username")
          }}<input
            v-model="credentials.username"
            required
            autocomplete="username"
            maxlength="60" /></label
        ><label
          >{{ t("密码", "Password")
          }}<input
            v-model="credentials.password"
            type="password"
            required
            autocomplete="current-password"
            maxlength="72"
        /></label>
        <div v-if="error" role="alert" class="alert">{{ error }}</div>
        <button class="primary" :disabled="busy">
          {{ busy ? t("登录中…", "Signing in…") : t("登录", "Sign in") }}
        </button>
      </form>
      <div class="login-links">
        <button class="link" @click="openCareers">
          {{ t("查看公开招聘岗位", "View public job openings")
          }}<ChevronRight :size="16" /></button
        ><button @click="changeLanguage">{{ t("English", "中文") }}</button>
      </div>
      <p class="legal">
        RecruitFlow 1.0 ·
        {{
          t(
            "公开源码学习版，未经书面授权不得商用",
            "Non-commercial source edition; written permission required for commercial use",
          )
        }}<br />上海如静知华信息科技有限公司<br /><a
          href="https://www.zhuatech.cn/"
          >zhuatech.cn</a
        >
        · {{ t("商业咨询微信", "Commercial enquiries WeChat") }} zhuatech /
        zhuatech2
      </p>
    </div>
    <aside class="login-aside">
      <div class="recruit-mark">R<span>/</span></div>
      <p>RecruitFlow</p>
      <h2>{{ t("招聘工作流程", "Recruitment workflow") }}</h2>
      <ol>
        <li>{{ t("岗位审批", "Job approval") }}</li>
        <li>{{ t("投递与面试", "Applications & interviews") }}</li>
        <li>{{ t("录用与入职", "Offer & joining") }}</li>
      </ol>
    </aside>
  </div>
  <div v-else class="shell">
    <aside class="sidebar" :class="{ shown: mobileNav }">
      <a
        href="https://www.zhuatech.cn/"
        target="_blank"
        rel="noopener"
        class="brand"
        ><img src="/brand/logo.jpg" alt="知华科技" /><span
          >RecruitFlow<span>{{
            t("知华招聘协作", "ZhuaTech Recruiting")
          }}</span></span
        ></a
      >
      <nav>
        <button
          v-for="m in menus"
          :key="m.id"
          :class="{ active: view === m.code }"
          @click="navigate(m.code)"
        >
          <component :is="icons[m.code] || Settings" :size="18" />{{
            t(m.name, m.nameEn)
          }}<span
            v-if="
              m.code === 'applicants' &&
              apps.filter((x) => x.stage === 'NEW').length
            "
            class="nav-count"
            >{{ apps.filter((x) => x.stage === "NEW").length }}</span
          >
        </button>
      </nav>
      <div class="sidebar-foot">
        <button @click="openCareers">
          <Globe :size="16" />{{ t("公开招聘页", "Public careers") }}</button
        ><button @click="openForm('about', t('关于系统', 'About'), [])">
          {{ t("关于与商业授权", "About & commercial licence") }}</button
        ><a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
          >知华科技 · zhuatech.cn</a
        >
      </div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <button
          class="mobile-menu"
          :aria-label="t('打开菜单', 'Open menu')"
          @click="mobileNav = !mobileNav"
        >
          <Menu :size="20" /></button
        ><span>{{ t("招聘运营", "RECRUITMENT OPERATIONS") }}</span>
        <div class="grow"></div>
        <button
          class="icon-button"
          :aria-label="t('刷新', 'Refresh')"
          :disabled="busy"
          @click="run(load)"
        >
          <RefreshCw :size="17" /></button
        ><button @click="changeLanguage">{{ t("English", "中文") }}</button
        ><button
          class="account-button"
          @click="
            openForm(
              'password',
              t('修改密码', 'Change password'),
              forms.password,
            )
          "
        >
          {{ me.displayName }}</button
        ><button
          class="icon-button"
          :aria-label="t('退出登录', 'Sign out')"
          @click="signOut"
        >
          <LogOut :size="17" />
        </button>
      </header>
      <main>
        <div v-if="error" class="alert" role="alert">{{ error }}</div>
        <div v-if="notice" class="success" role="status">{{ notice }}</div>
        <template v-if="view === 'dashboard'"
          ><div class="page-heading">
            <div>
              <p class="eyebrow">{{ t("工作台", "WORKSPACE") }}</p>
              <h1>{{ t("招聘进展", "Recruitment overview") }}</h1>
            </div>
            <button v-if="can('recruit')" class="primary" @click="jobForm()">
              <Plus :size="17" />{{ t("新建岗位", "New opening") }}
            </button>
          </div>
          <div class="metrics">
            <button
              @click="
                navigate('jobs');
                state = 'OPEN';
              "
            >
              <span>{{ t("招聘中岗位", "Open positions") }}</span
              ><strong>{{
                jobs.filter((x) => x.status === "OPEN").length
              }}</strong></button
            ><button
              @click="
                navigate('applicants');
                state = 'NEW';
              "
            >
              <span>{{ t("待初筛", "New applications") }}</span
              ><strong>{{
                apps.filter((x) => x.stage === "NEW").length
              }}</strong></button
            ><button
              @click="
                navigate('applicants');
                state = 'OFFER';
              "
            >
              <span>{{ t("录用审批中", "Offer stage") }}</span
              ><strong>{{
                apps.filter((x) => x.stage === "OFFER").length
              }}</strong></button
            ><button
              @click="
                navigate('applicants');
                state = 'HIRED';
              "
            >
              <span>{{ t("已入职", "Joined") }}</span
              ><strong>{{
                apps.filter((x) => x.stage === "HIRED").length
              }}</strong>
            </button>
          </div>
          <div class="dashboard-grid">
            <section class="panel">
              <div class="panel-heading">
                <h2>{{ t("近期面试", "Upcoming interviews") }}</h2>
                <button class="link" @click="navigate('interviews')">
                  {{ t("全部日程", "All interviews")
                  }}<ChevronRight :size="15" />
                </button>
              </div>
              <p v-if="!upcoming.length" class="empty">
                {{ t("暂无待面试安排", "No scheduled interviews") }}
              </p>
              <button
                v-for="i in upcoming"
                :key="i.id"
                class="agenda-row"
                @click="openApplicant(i.applicantId)"
              >
                <div class="date-chip">
                  {{ new Date(i.startsAt).getDate()
                  }}<small>{{
                    new Date(i.startsAt).toLocaleString(
                      lang === "en" ? "en" : "zh",
                      { month: "short" },
                    )
                  }}</small>
                </div>
                <div>
                  <strong>{{ appName(i.applicantId) }}</strong
                  ><span>{{ i.title }} · {{ person(i.interviewerId) }}</span
                  ><small>{{ date(i.startsAt) }}</small>
                </div>
                <ChevronRight :size="17" />
              </button>
            </section>
            <section class="panel">
              <div class="panel-heading">
                <h2>{{ t("待推进岗位", "Open workflow") }}</h2>
              </div>
              <p v-if="!jobs.length" class="empty">
                {{
                  t(
                    "新建岗位并指派招聘人员与审批负责人",
                    "Create an opening and assign a recruiter and independent manager",
                  )
                }}
              </p>
              <button
                v-for="j in jobs
                  .filter((x) => ['OPEN', 'PENDING'].includes(x.status))
                  .slice(0, 7)"
                :key="j.id"
                class="job-summary"
                @click="
                  navigate('jobs');
                  selectedJob = j;
                "
              >
                <div>
                  <strong>{{ j.title }}</strong
                  ><small
                    >{{ j.location }} · {{ j.headcount }}
                    {{ t("人", "positions") }}</small
                  >
                </div>
                <span class="badge" :class="j.status">{{
                  label(j.status)
                }}</span>
              </button>
            </section>
          </div></template
        >
        <template v-else-if="view === 'applicants' && detail"
          ><button class="link back" @click="detail = null">
            <ArrowLeft :size="16" />{{ t("候选人列表", "Applicants") }}
          </button>
          <div class="page-heading">
            <div>
              <p class="eyebrow">{{ detail.job.title }}</p>
              <h1>
                {{ activeA.name
                }}<span class="badge" :class="activeA.stage">{{
                  label(activeA.stage)
                }}</span>
              </h1>
              <p class="muted">
                {{ activeA.reference }} · {{ date(activeA.createdAt) }}
              </p>
            </div>
            <div class="actions">
              <button
                v-if="
                  can('recruit') &&
                  ['NEW', 'SCREENING'].includes(activeA.stage) &&
                  !activeA.redactedAt
                "
                @click="applicantForm(activeA)"
              >
                {{ t("编辑资料", "Edit profile") }}</button
              ><button
                v-if="
                  can('recruit') &&
                  activeA.stage === 'NEW' &&
                  activeA.source !== 'PUBLIC'
                "
                class="danger-text"
                @click="
                  removeForm('/applicants/' + activeA.id, activeA.revision)
                "
              >
                {{ t("删除草稿", "Delete draft") }}
              </button>
            </div>
          </div>
          <div class="pipeline">
            <span
              v-for="s in stages.slice(0, 6)"
              :key="s"
              :class="{
                current: activeA.stage === s,
                passed:
                  stages.indexOf(activeA.stage) > stages.indexOf(s) &&
                  stages.indexOf(activeA.stage) < 6,
              }"
              >{{ label(s) }}</span
            >
          </div>
          <div
            class="detail-grid"
            :class="{ wide: !workflowVisible && !detail.events.length }"
          >
            <div class="detail-primary">
              <section class="panel">
                <div class="panel-heading">
                  <h2>{{ t("申请资料", "Application") }}</h2>
                  <a
                    v-if="detail.resume.id"
                    class="button"
                    :href="'/api/applicants/' + activeA.id + '/resume'"
                    download
                    ><Download :size="15" />{{ t("下载简历", "Resume") }}</a
                  >
                </div>
                <dl class="info-grid">
                  <div>
                    <dt>{{ t("联系邮箱", "Email") }}</dt>
                    <dd>{{ activeA.email }}</dd>
                  </div>
                  <div>
                    <dt>{{ t("联系电话", "Phone") }}</dt>
                    <dd>{{ activeA.phone || "—" }}</dd>
                  </div>
                  <div v-if="activeA.source">
                    <dt>{{ t("来源", "Source") }}</dt>
                    <dd>{{ label(activeA.source) }}</dd>
                  </div>
                  <div v-if="activeA.ownerId">
                    <dt>{{ t("招聘人员", "Recruiter") }}</dt>
                    <dd>{{ person(activeA.ownerId) }}</dd>
                  </div>
                </dl>
                <p class="preserve">{{ activeA.introduction || "—" }}</p>
                <p v-if="activeA.consentReference" class="muted">
                  {{ t("资料记录：", "Collection reference: ")
                  }}{{ activeA.consentReference }}
                </p>
                <label
                  v-if="
                    can('recruit') &&
                    ['NEW', 'SCREENING'].includes(activeA.stage)
                  "
                  class="file-control"
                  >{{
                    t(
                      "上传／更换PDF简历（最多3MB）",
                      "Upload / replace PDF resume (max 3MB)",
                    )
                  }}<input
                    type="file"
                    accept="application/pdf,.pdf"
                    :disabled="busy"
                    @change="uploadResume"
                /></label>
                <p v-if="activeA.joinedDate">
                  {{ t("入职日期：", "Joined: ") }}{{ activeA.joinedDate }} ·
                  {{ activeA.employeeCode }}
                </p>
              </section>
              <section class="panel">
                <div class="panel-heading">
                  <h2>{{ t("面试记录", "Interviews") }}</h2>
                  <button
                    v-if="
                      can('recruit') &&
                      !finished(activeA) &&
                      ['SCREENING', 'INTERVIEW'].includes(activeA.stage)
                    "
                    @click="interviewForm"
                  >
                    <Plus :size="15" />{{ t("安排面试", "Schedule") }}
                  </button>
                </div>
                <p v-if="!detail.interviews.length" class="empty">
                  {{ t("暂无面试记录", "No interviews") }}
                </p>
                <article
                  v-for="i in detail.interviews"
                  :key="i.id"
                  class="record"
                >
                  <div class="record-title">
                    <strong>{{ i.title }}</strong
                    ><span class="badge" :class="i.status">{{
                      label(i.status)
                    }}</span>
                  </div>
                  <p>
                    {{ person(i.interviewerId) }} · {{ date(i.startsAt) }} —
                    {{ date(i.endsAt) }}
                  </p>
                  <p class="muted">{{ i.location }}</p>
                  <p v-if="i.status === 'COMPLETED'" class="preserve">
                    <strong>{{ label(i.recommendation) }}</strong
                    ><br />{{ i.feedback }}
                  </p>
                  <div class="actions">
                    <button
                      v-if="
                        can('interview') &&
                        me.id === i.interviewerId &&
                        i.status === 'SCHEDULED' &&
                        !finished(activeA)
                      "
                      :disabled="new Date(i.endsAt) > new Date()"
                      @click="feedbackForm(i)"
                    >
                      {{ t("记录反馈", "Record feedback") }}</button
                    ><button
                      v-if="
                        can('recruit') &&
                        i.status === 'SCHEDULED' &&
                        !finished(activeA)
                      "
                      @click="
                        openForm(
                          'command',
                          t('取消面试', 'Cancel interview'),
                          commandFields,
                          { revision: activeA.revision },
                          '/interviews/' + i.id + '/cancel',
                        )
                      "
                    >
                      {{ t("取消预约", "Cancel") }}
                    </button>
                  </div>
                </article>
              </section>
              <section v-if="can('recruit') || can('approve')" class="panel">
                <div class="panel-heading">
                  <h2>{{ t("录用建议", "Offer proposals") }}</h2>
                  <button
                    v-if="can('recruit') && activeA.stage === 'INTERVIEW'"
                    @click="offerForm()"
                  >
                    <Plus :size="15" />{{ t("新建建议", "New proposal") }}
                  </button>
                </div>
                <p v-if="!detail.offers.length" class="empty">
                  {{ t("暂无录用建议", "No offer proposals") }}
                </p>
                <article v-for="o in detail.offers" :key="o.id" class="record">
                  <div class="record-title">
                    <strong
                      >{{ t("版本", "Version") }} {{ o.version }} ·
                      {{ money(o.salary, o.currency, lang) }} /
                      {{ label(o.payPeriod) }}</strong
                    ><span class="badge" :class="o.status">{{
                      label(o.status)
                    }}</span>
                  </div>
                  <dl class="info-grid">
                    <div>
                      <dt>{{ t("约定入职", "Agreed start") }}</dt>
                      <dd>{{ o.startDate }}</dd>
                    </div>
                    <div>
                      <dt>{{ t("答复有效期", "Response deadline") }}</dt>
                      <dd>{{ date(o.expiresAt) }}</dd>
                    </div>
                  </dl>
                  <p class="preserve">{{ o.terms }}</p>
                  <p v-if="o.reviewNote" class="muted">
                    {{ t("审批：", "Review: ") }}{{ o.reviewNote }}
                  </p>
                  <p v-if="o.deliveryReference">
                    {{ t("送达记录：", "Delivery: ") }}{{ o.deliveryReference }}
                  </p>
                  <p v-if="o.responseReference">
                    {{ t("答复记录：", "Response: ") }}{{ o.responseReference }}
                  </p>
                  <div v-if="!finished(activeA)" class="actions">
                    <template v-if="can('recruit')"
                      ><button
                        v-if="o.status === 'DRAFT'"
                        @click="offerForm(o)"
                      >
                        {{ t("编辑", "Edit") }}</button
                      ><button
                        v-if="o.status === 'DRAFT'"
                        @click="offerAction(o, 'delete')"
                      >
                        {{ t("删除草稿", "Delete draft") }}</button
                      ><button
                        v-if="o.status === 'DRAFT'"
                        class="primary"
                        @click="offerAction(o, 'submit')"
                      >
                        {{ t("送审", "Submit") }}</button
                      ><button
                        v-if="o.status === 'APPROVED'"
                        @click="offerAction(o, 'issue')"
                      >
                        {{ t("记录送达", "Record delivery") }}</button
                      ><template v-if="o.status === 'ISSUED'"
                        ><button
                          class="primary"
                          @click="offerAction(o, 'accept')"
                        >
                          {{ t("记录接受", "Record acceptance") }}</button
                        ><button @click="offerAction(o, 'decline')">
                          {{ t("记录婉拒", "Record decline") }}
                        </button></template
                      ><button
                        v-if="
                          ['DRAFT', 'PENDING', 'APPROVED', 'ISSUED'].includes(
                            o.status,
                          ) && new Date(o.expiresAt) <= new Date()
                        "
                        @click="offerAction(o, 'expire')"
                      >
                        {{ t("释放过期建议", "Release expired offer") }}
                      </button></template
                    ><template v-if="isManager(activeJob)"
                      ><button
                        v-if="
                          o.status === 'PENDING' &&
                          o.creatorId !== me.id &&
                          o.submittedById !== me.id
                        "
                        class="primary"
                        @click="offerAction(o, 'approve')"
                      >
                        {{ t("批准录用", "Approve offer") }}</button
                      ><button
                        v-if="
                          o.status === 'PENDING' &&
                          o.creatorId !== me.id &&
                          o.submittedById !== me.id
                        "
                        @click="offerAction(o, 'return')"
                      >
                        {{ t("退回", "Return") }}</button
                      ><button
                        v-if="
                          [
                            'DRAFT',
                            'PENDING',
                            'APPROVED',
                            'ISSUED',
                            'ACCEPTED',
                          ].includes(o.status)
                        "
                        @click="offerAction(o, 'revoke')"
                      >
                        {{ t("撤销建议", "Revoke") }}
                      </button></template
                    >
                  </div>
                </article>
              </section>
              <section v-if="can('recruit') || can('approve')" class="panel">
                <div class="panel-heading">
                  <h2>{{ t("内部跟进", "Internal notes") }}</h2>
                  <button
                    v-if="can('recruit') && !finished(activeA)"
                    @click="
                      openForm(
                        'command',
                        t('追加跟进', 'Add note'),
                        commandFields,
                        { revision: activeA.revision },
                        '/applicants/' + activeA.id + '/notes',
                      )
                    "
                  >
                    <Plus :size="15" />{{ t("添加", "Add") }}
                  </button>
                </div>
                <p v-if="!detail.notes.length" class="empty">
                  {{ t("暂无跟进", "No notes") }}
                </p>
                <article v-for="n in detail.notes" :key="n.id" class="record">
                  <small
                    >{{ person(n.actorId) }} · {{ date(n.createdAt) }}</small
                  >
                  <p class="preserve">
                    {{
                      n.note ||
                      t("个人内容已匿名化", "Personal content redacted")
                    }}
                  </p>
                </article>
              </section>
            </div>
            <aside
              v-if="workflowVisible || detail.events.length"
              class="detail-side"
            >
              <section v-if="workflowVisible" class="panel">
                <h2>{{ t("流程操作", "Workflow") }}</h2>
                <div class="vertical-actions">
                  <button
                    v-if="can('recruit') && activeA.stage === 'NEW'"
                    class="primary"
                    @click="appAction('screen')"
                  >
                    {{ t("开始初筛", "Start screening") }}</button
                  ><button
                    v-if="can('recruit') && !finished(activeA)"
                    @click="appAction('assign')"
                  >
                    {{ t("交接申请", "Transfer") }}</button
                  ><button
                    v-if="can('recruit') && !finished(activeA)"
                    @click="appAction('withdraw')"
                  >
                    {{ t("记录申请撤回", "Record withdrawal") }}</button
                  ><button
                    v-if="isManager(activeJob) && !finished(activeA)"
                    class="danger-text"
                    @click="appAction('reject')"
                  >
                    {{ t("拒绝申请", "Reject application") }}</button
                  ><button
                    v-if="isManager(activeJob) && activeA.stage === 'ACCEPTED'"
                    class="primary"
                    @click="appAction('hire')"
                  >
                    {{ t("登记实际入职", "Record actual joining") }}</button
                  ><button
                    v-if="
                      can('privacy') && finished(activeA) && !activeA.redactedAt
                    "
                    class="danger-text"
                    @click="
                      openForm(
                        'redact',
                        t(
                          '匿名化个人资料（不可恢复）',
                          'Redact personal data (irreversible)',
                        ),
                        commandFields,
                        { revision: activeA.revision },
                        '/applicants/' + activeA.id + '/redact',
                      )
                    "
                  >
                    {{ t("匿名化个人资料", "Redact personal data") }}
                  </button>
                </div>
              </section>
              <section v-if="detail.events.length" class="panel">
                <h2>{{ t("流程记录", "History") }}</h2>
                <ol class="timeline">
                  <li v-for="e in [...detail.events].reverse()" :key="e.id">
                    <strong>{{ eventLabel(e.action) }}</strong
                    ><small>{{ e.actor }} · {{ date(e.createdAt) }}</small>
                    <p v-if="e.note">{{ e.note }}</p>
                  </li>
                </ol>
              </section>
            </aside>
          </div>
        </template>
        <template v-else-if="view === 'jobs' && selectedJob"
          ><button class="link back" @click="selectedJob = null">
            <ArrowLeft :size="16" />{{ t("岗位列表", "Job openings") }}
          </button>
          <div class="page-heading">
            <div>
              <p class="eyebrow">{{ selectedJob.code }}</p>
              <h1>
                {{ selectedJob.title
                }}<span class="badge" :class="selectedJob.status">{{
                  label(selectedJob.status)
                }}</span>
              </h1>
              <p class="muted">
                {{ selectedJob.location }} · {{ selectedJob.headcount }}
                {{ t("人", "positions") }} · {{ person(selectedJob.ownerId) }} /
                {{ person(selectedJob.managerId) }}
              </p>
            </div>
            <div class="actions">
              <template v-if="can('recruit')"
                ><button
                  v-if="selectedJob.status === 'DRAFT'"
                  @click="jobForm(selectedJob)"
                >
                  {{ t("编辑", "Edit") }}</button
                ><button
                  v-if="selectedJob.status === 'DRAFT'"
                  class="primary"
                  @click="jobAction(selectedJob, 'submit')"
                >
                  {{ t("送审", "Submit for review") }}</button
                ><button
                  v-if="selectedJob.status === 'OPEN'"
                  @click="jobAction(selectedJob, 'pause')"
                >
                  {{ t("暂停投递", "Pause intake") }}</button
                ><button
                  v-if="selectedJob.status === 'PAUSED'"
                  @click="jobAction(selectedJob, 'resume')"
                >
                  {{ t("恢复招聘", "Resume") }}</button
                ><button
                  v-if="selectedJob.status === 'DRAFT'"
                  class="danger-text"
                  @click="
                    removeForm('/jobs/' + selectedJob.id, selectedJob.revision)
                  "
                >
                  {{ t("删除草稿", "Delete draft") }}</button
                ><button
                  v-if="['OPEN', 'PAUSED'].includes(selectedJob.status)"
                  @click="applicantForm()"
                >
                  <Plus :size="15" />{{ t("录入候选人", "Add applicant") }}
                </button></template
              ><template v-if="isManager(selectedJob)"
                ><button
                  v-if="
                    selectedJob.status === 'PENDING' &&
                    selectedJob.ownerId !== me.id &&
                    selectedJob.submittedById !== me.id
                  "
                  class="primary"
                  @click="jobAction(selectedJob, 'approve')"
                >
                  {{ t("批准发布", "Approve opening") }}</button
                ><button
                  v-if="
                    selectedJob.status === 'PENDING' &&
                    selectedJob.submittedById !== me.id
                  "
                  @click="jobAction(selectedJob, 'return')"
                >
                  {{ t("退回", "Return") }}</button
                ><button
                  v-if="selectedJob.status !== 'CLOSED'"
                  @click="jobAction(selectedJob, 'assign')"
                >
                  {{ t("交接招聘人员", "Transfer recruiter") }}</button
                ><button
                  v-if="['OPEN', 'PAUSED'].includes(selectedJob.status)"
                  @click="jobAction(selectedJob, 'close')"
                >
                  {{ t("关闭岗位", "Close opening") }}
                </button></template
              >
            </div>
          </div>
          <section class="panel">
            <h2>
              {{ t("岗位职责与要求", "Responsibilities and requirements") }}
            </h2>
            <p class="preserve">{{ selectedJob.description }}</p>
          </section>
          <section class="panel">
            <h2>{{ t("申请进度", "Application progress") }}</h2>
            <div class="stage-summary">
              <div v-for="s in stages" :key="s">
                <strong>{{
                  jobApplicants.filter((x) => x.stage === s).length
                }}</strong
                ><span>{{ label(s) }}</span>
              </div>
            </div>
            <p v-if="!jobApplicants.length" class="empty">
              {{ t("暂无申请", "No applicants") }}
            </p>
            <button
              v-for="a in jobApplicants"
              :key="a.id"
              class="job-summary"
              @click="openApplicant(a.id)"
            >
              <div>
                <strong>{{ a.name }}</strong
                ><small>{{ date(a.createdAt) }}</small>
              </div>
              <span class="badge" :class="a.stage">{{ label(a.stage) }}</span>
            </button>
          </section></template
        >
        <template v-else
          ><div class="page-heading">
            <div>
              <p class="eyebrow">RECRUITFLOW</p>
              <h1>
                {{
                  t(
                    menus.find((x) => x.code === view)?.name,
                    menus.find((x) => x.code === view)?.nameEn,
                  )
                }}
              </h1>
            </div>
            <div class="actions">
              <button
                v-if="view === 'jobs' && can('recruit')"
                class="primary"
                @click="jobForm()"
              >
                <Plus :size="17" />{{ t("新建岗位", "New opening") }}</button
              ><button
                v-if="view === 'applicants' && can('recruit')"
                class="primary"
                @click="applicantForm()"
              >
                <Plus :size="17" />{{
                  t("录入候选人", "Add applicant")
                }}</button
              ><button v-if="view === 'reports'" @click="run(downloadReport)">
                <Download :size="16" />{{ t("导出统计", "Export CSV") }}</button
              ><button
                v-if="
                  view === 'admin' &&
                  !['permissions', 'menus', 'settings'].includes(adminType)
                "
                class="primary"
                @click="adminForm()"
              >
                <Plus :size="17" />{{ t("新增", "Add") }}
              </button>
            </div>
          </div>
          <div v-if="view === 'admin'" class="tabs">
            <button
              v-for="(n, k) in {
                users: t('账号', 'Accounts'),
                roles: t('角色', 'Roles'),
                permissions: t('权限', 'Permissions'),
                menus: t('菜单', 'Menus'),
                departments: t('部门', 'Departments'),
                dictionaries: t('字典', 'Dictionaries'),
                settings: t('参数', 'Settings'),
              }"
              :key="k"
              :class="{ active: adminType === k }"
              @click="
                adminType = k;
                page = 1;
                search = '';
              "
            >
              {{ n }}
            </button>
          </div>
          <div class="toolbar">
            <label class="search"
              ><Search :size="17" /><input
                v-model="search"
                :placeholder="t('搜索当前列表', 'Search records')"
                :aria-label="t('搜索', 'Search')"
                @input="page = 1" /></label
            ><select
              v-if="['jobs', 'applicants', 'interviews'].includes(view)"
              v-model="state"
              :aria-label="t('状态筛选', 'Filter status')"
              @change="page = 1"
            >
              <option value="">{{ t("全部状态", "All statuses") }}</option>
              <option
                v-for="s in view === 'jobs'
                  ? ['DRAFT', 'PENDING', 'OPEN', 'PAUSED', 'CLOSED']
                  : view === 'applicants'
                    ? stages
                    : ['SCHEDULED', 'COMPLETED', 'CANCELLED']"
                :key="s"
                :value="s"
              >
                {{ label(s) }}
              </option></select
            ><select
              v-if="view === 'applicants'"
              v-model="jobFilter"
              :aria-label="t('岗位筛选', 'Filter job')"
              @change="page = 1"
            >
              <option value="">{{ t("全部岗位", "All jobs") }}</option>
              <option v-for="j in jobs" :key="j.id" :value="j.id">
                {{ j.title }}
              </option>
            </select>
            <div class="grow"></div>
            <button @click="descending = !descending">
              {{
                descending
                  ? t("最新在前", "Newest first")
                  : t("最早在前", "Oldest first")
              }}</button
            ><span class="muted"
              >{{ filteredRows.length }} {{ t("条", "records") }}</span
            >
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr v-if="view === 'jobs'">
                  <th>{{ t("岗位", "Opening") }}</th>
                  <th>{{ t("地点／类型", "Location / type") }}</th>
                  <th>{{ t("人数", "Headcount") }}</th>
                  <th>{{ t("招聘人员", "Recruiter") }}</th>
                  <th>{{ t("状态", "Status") }}</th>
                  <th></th>
                </tr>
                <tr v-else-if="view === 'applicants'">
                  <th>{{ t("候选人", "Applicant") }}</th>
                  <th>{{ t("申请岗位", "Job") }}</th>
                  <th>{{ t("邮箱", "Email") }}</th>
                  <th>{{ t("申请时间", "Applied") }}</th>
                  <th>{{ t("阶段", "Stage") }}</th>
                  <th></th>
                </tr>
                <tr v-else-if="view === 'interviews'">
                  <th>{{ t("时间", "Time") }}</th>
                  <th>{{ t("候选人／面试", "Applicant / interview") }}</th>
                  <th>{{ t("面试官", "Interviewer") }}</th>
                  <th>{{ t("地点", "Location") }}</th>
                  <th>{{ t("状态", "Status") }}</th>
                  <th></th>
                </tr>
                <tr v-else-if="view === 'reports'">
                  <th>{{ t("岗位", "Opening") }}</th>
                  <th>{{ t("人数／申请", "Headcount / applicants") }}</th>
                  <th v-for="s in stages" :key="s">{{ label(s) }}</th>
                </tr>
                <tr v-else-if="view === 'audit'">
                  <th>{{ t("时间", "Time") }}</th>
                  <th>{{ t("操作者", "Actor") }}</th>
                  <th>{{ t("动作", "Action") }}</th>
                  <th>{{ t("对象", "Object") }}</th>
                </tr>
                <tr v-else>
                  <th>ID</th>
                  <th>{{ t("名称／账号／代码", "Name / username / code") }}</th>
                  <th>{{ t("配置", "Configuration") }}</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in rows" :key="r.id">
                  <template v-if="view === 'jobs'"
                    ><td>
                      <button class="table-link" @click="selectedJob = r">
                        {{ r.title }}</button
                      ><small>{{ r.code }}</small>
                    </td>
                    <td>
                      {{ r.location
                      }}<small>{{ label(r.employmentType) }}</small>
                    </td>
                    <td>{{ r.headcount }}</td>
                    <td>{{ person(r.ownerId) }}</td>
                    <td>
                      <span class="badge" :class="r.status">{{
                        label(r.status)
                      }}</span>
                    </td>
                    <td>
                      <button @click="selectedJob = r">
                        {{ t("查看", "View") }}
                      </button>
                    </td></template
                  ><template v-else-if="view === 'applicants'"
                    ><td>
                      <button class="table-link" @click="openApplicant(r.id)">
                        {{ r.name }}</button
                      ><small>#{{ r.id }}</small>
                    </td>
                    <td>{{ jobTitle(r.jobId) }}</td>
                    <td>{{ r.email }}</td>
                    <td>{{ date(r.createdAt) }}</td>
                    <td>
                      <span class="badge" :class="r.stage">{{
                        label(r.stage)
                      }}</span>
                    </td>
                    <td>
                      <button @click="openApplicant(r.id)">
                        {{ t("查看", "View") }}
                      </button>
                    </td></template
                  ><template v-else-if="view === 'interviews'"
                    ><td>
                      {{ date(r.startsAt) }}<small>{{ date(r.endsAt) }}</small>
                    </td>
                    <td>
                      {{ appName(r.applicantId) }}<small>{{ r.title }}</small>
                    </td>
                    <td>{{ person(r.interviewerId) }}</td>
                    <td>{{ r.location }}</td>
                    <td>
                      <span class="badge" :class="r.status">{{
                        label(r.status)
                      }}</span>
                    </td>
                    <td>
                      <button @click="openApplicant(r.applicantId)">
                        {{ t("查看", "View") }}
                      </button>
                    </td></template
                  ><template v-else-if="view === 'reports'"
                    ><td>
                      <strong>{{ r.title }}</strong
                      ><small>{{ label(r.status) }}</small>
                    </td>
                    <td>{{ r.headcount }} / {{ r.applications }}</td>
                    <td v-for="s in stages" :key="s">
                      {{ r.stages[s] }}
                    </td></template
                  ><template v-else-if="view === 'audit'"
                    ><td>{{ date(r.createdAt) }}</td>
                    <td>{{ r.actor }}</td>
                    <td>{{ r.action }}</td>
                    <td>{{ r.objectId }}</td></template
                  ><template v-else
                    ><td>{{ r.id }}</td>
                    <td>
                      <strong>{{
                        r.displayName || r.name || parameterName(r.code)
                      }}</strong
                      ><small>{{ r.username || r.nameEn || r.code }}</small>
                    </td>
                    <td>
                      <span v-if="r.permissions"
                        >{{ scopeName(r.scope) }} ·
                        {{
                          r.permissions.map(permissionName).join(" · ")
                        }}</span
                      ><span
                        v-else-if="r.value !== undefined"
                        class="setting-preview"
                        >{{
                          r.code === "publicIntake"
                            ? r.value === "true"
                              ? t("开启", "Enabled")
                              : t("关闭", "Disabled")
                            : r.value || "—"
                        }}</span
                      ><span v-else-if="r.roleId">
                        {{
                          adminData.roles.find((x) => x.id === r.roleId)?.name
                        }}
                        ·
                        {{
                          adminData.departments.find(
                            (x) => x.id === r.departmentId,
                          )?.name
                        }}
                        <span
                          class="badge"
                          :class="r.enabled ? 'OPEN' : 'CLOSED'"
                          >{{
                            r.enabled
                              ? t("启用", "Enabled")
                              : t("停用", "Disabled")
                          }}</span
                        > </span
                      ><span v-else
                        >{{ r.scope || r.type || r.permissionCode || "" }}
                        <span
                          v-if="r.enabled !== undefined"
                          class="badge"
                          :class="r.enabled ? 'OPEN' : 'CLOSED'"
                          >{{
                            r.enabled
                              ? t("启用", "Enabled")
                              : t("停用", "Disabled")
                          }}</span
                        ></span
                      >
                    </td>
                    <td class="row-actions">
                      <button @click="adminForm(r)">
                        {{ t("编辑", "Edit") }}</button
                      ><button
                        v-if="
                          !['permissions', 'menus', 'settings'].includes(
                            adminType,
                          )
                        "
                        class="danger-text"
                        @click="removeForm('/admin/' + adminType + '/' + r.id)"
                      >
                        {{ t("删除", "Delete") }}
                      </button>
                    </td></template
                  >
                </tr>
                <tr v-if="!rows.length">
                  <td colspan="12" class="empty">
                    {{ t("暂无匹配记录", "No matching records") }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pagination">
            <span>{{ t("每页10条", "10 per page") }}</span>
            <div class="grow"></div>
            <button :disabled="page <= 1" @click="page--">
              {{ t("上一页", "Previous") }}</button
            ><span>{{ Math.min(page, totalPages) }} / {{ totalPages }}</span
            ><button :disabled="page >= totalPages" @click="page++">
              {{ t("下一页", "Next") }}
            </button>
          </div>
        </template>
      </main>
      <footer>
        RecruitFlow 1.0 ·
        {{ t("公开源码学习版", "Non-commercial source edition") }} ·
        <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener">{{
          t("知华科技 · 商业咨询", "ZhuaTech · Commercial enquiries")
        }}</a>
      </footer>
    </div>
  </div>
  <dialog ref="dialog" @cancel.prevent="closeForm">
    <template v-if="modal"
      ><form @submit.prevent="saveForm">
        <header class="dialog-heading">
          <h2>{{ modal.title }}</h2>
          <button
            type="button"
            :disabled="busy"
            :aria-label="t('关闭', 'Close')"
            @click="closeForm"
          >
            <X :size="18" />
          </button>
        </header>
        <div v-if="modal.kind === 'about'" class="about">
          <img src="/brand/logo.jpg" alt="知华科技" />
          <h3>RecruitFlow 1.0</h3>
          <p>上海如静知华信息科技有限公司</p>
          <p>
            {{
              t(
                "公开源码学习版，未经书面授权不得商用。",
                "Non-commercial source edition. Written permission required for commercial use.",
              )
            }}
          </p>
          <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
            >https://www.zhuatech.cn/</a
          >
          <p>
            {{
              t(
                "商业授权、定制、部署与系统集成咨询微信：",
                "Commercial licensing, customization, deployment and integration — WeChat: ",
              )
            }}zhuatech / zhuatech2
          </p>
        </div>
        <template v-else
          ><p v-if="modal.kind === 'delete'" class="muted">
            {{
              t(
                "删除未引用记录。已有流程历史会被系统拒绝删除。",
                "Delete unreferenced records. Workflow history cannot be deleted.",
              )
            }}
          </p>
          <p v-if="modal.kind === 'redact'" class="alert">
            {{
              t(
                "将移除姓名、联系资料、简历、反馈、录用薪资和内部个人内容。请确认内部资料处置要求；备份需单独处理。",
                "Removes name, contact data, resume, feedback, salary and personal notes. Confirm your internal disposal instruction; backups require separate handling.",
              )
            }}
          </p>
          <div class="form-grid">
            <label
              v-for="field in modal.fields"
              :key="field.key"
              :class="{
                full: ['textarea', 'permissions'].includes(field.type),
                check: field.type === 'checkbox',
              }"
              ><template v-if="field.type !== 'checkbox'">{{
                t(field.zh, field.en)
              }}</template
              ><textarea
                v-if="field.type === 'textarea'"
                v-model="form[field.key]"
                :readonly="
                  modal.kind === 'applicant' &&
                  form.source === 'PUBLIC' &&
                  field.key === 'consentReference'
                "
                :required="!field.optional"
                :maxlength="
                  field.key === 'description' ||
                  modal.path.includes('/settings/')
                    ? 6000
                    : field.key === 'note'
                      ? 1000
                      : 4000
                "
                rows="4"
              ></textarea
              ><select
                v-else-if="field.type === 'select'"
                v-model="form[field.key]"
                :required="!field.optional"
              >
                <option disabled value="">{{ t("请选择", "Select") }}</option>
                <option v-for="o in opts(field)" :key="o.id" :value="o.id">
                  {{ o.name }}
                </option>
              </select>
              <div
                v-else-if="field.type === 'permissions'"
                class="permission-grid"
              >
                <label
                  v-for="p in adminData.permissions"
                  :key="p.code"
                  class="check"
                  ><input
                    v-model="form.permissions"
                    type="checkbox"
                    :value="p.code"
                  />{{ p.name }}</label
                >
              </div>
              <template v-else-if="field.type === 'checkbox'"
                ><input v-model="form[field.key]" type="checkbox" />{{
                  t(field.zh, field.en)
                }}</template
              ><input
                v-else
                v-model="form[field.key]"
                :type="field.type === 'money' ? 'number' : field.type"
                :step="
                  field.type === 'money'
                    ? '0.01'
                    : field.type === 'number'
                      ? '1'
                      : undefined
                "
                :required="!field.optional"
                :maxlength="
                  field.type === 'password'
                    ? 72
                    : field.key === 'email'
                      ? 254
                      : field.key === 'location'
                        ? 300
                        : 160
                "
                :autocomplete="
                  field.type === 'password' ? 'new-password' : 'off'
                "
            /></label>
          </div>
          <div v-if="error" class="alert" role="alert">
            {{ error }}
          </div></template
        >
        <div class="dialog-actions">
          <button type="button" :disabled="busy" @click="closeForm">
            {{ t("关闭", "Close") }}</button
          ><button
            v-if="modal.kind !== 'about'"
            class="primary"
            :disabled="busy"
          >
            {{ busy ? t("保存中…", "Saving…") : t("确认保存", "Save") }}
          </button>
        </div>
      </form></template
    >
  </dialog>
</template>
