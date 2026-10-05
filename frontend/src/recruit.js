// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
/** 双语状态，所有状态来自数据库。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const names = {
  DRAFT: ["草稿", "Draft"],
  PENDING: ["待审批", "Pending review"],
  OPEN: ["招聘中", "Open"],
  PAUSED: ["暂停投递", "Paused"],
  CLOSED: ["已关闭", "Closed"],
  NEW: ["待初筛", "New"],
  SCREENING: ["初筛中", "Screening"],
  INTERVIEW: ["面试中", "Interview"],
  OFFER: ["录用审批", "Offer"],
  ACCEPTED: ["已接受", "Accepted"],
  HIRED: ["已入职", "Joined"],
  REJECTED: ["已拒绝", "Rejected"],
  WITHDRAWN: ["已撤回", "Withdrawn"],
  SCHEDULED: ["待面试", "Scheduled"],
  COMPLETED: ["已反馈", "Completed"],
  CANCELLED: ["已取消", "Cancelled"],
  APPROVED: ["已批准", "Approved"],
  ISSUED: ["已送达", "Delivered"],
  DECLINED: ["已婉拒", "Declined"],
  REVOKED: ["已撤销", "Revoked"],
  EXPIRED: ["已过期", "Expired"],
  POSITIVE: ["建议继续", "Proceed"],
  CONCERNS: ["有待核实", "Concerns"],
  NEUTRAL: ["中立", "Neutral"],
  NO_SHOW: ["未出席", "No show"],
  MONTHLY: ["月薪", "Monthly"],
  ANNUAL: ["年薪", "Annual"],
  PUBLIC: ["在线投递", "Online application"],
};
/** 页面反馈说明下一步操作，不返回数据库详情。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const errors = {
  STALE_VERSION: [
    "记录已更新，请刷新后重新核对",
    "Record changed; refresh and review",
  ],
  SELF_APPROVAL: [
    "送审人和审批人需要不同账号",
    "An independent reviewer is required",
  ],
  HEADCOUNT_FULL: [
    "岗位名额已用完，先核对或释放其他录用建议",
    "No available headcount; review existing offers",
  ],
  INTERVIEW_CONFLICT: [
    "面试官或候选人的时段有冲突",
    "Interviewer or applicant has a conflicting appointment",
  ],
  PRIVACY_CONFIGURATION_REQUIRED: [
    "先填写至少50字的资料用途说明及联系人",
    "Configure a privacy notice (50+ characters) and contact first",
  ],
  RATE_LIMITED: [
    "提交过于频繁，请稍后重试",
    "Too many submissions; retry later",
  ],
  INVALID_RESUME: [
    "仅支持带完整文件尾的PDF，最多3MB",
    "PDF only, max 3MB, complete file required",
  ],
  PROFILE_FROZEN: [
    "已进入面试，基本资料和简历已冻结",
    "Profile and resume are frozen after screening",
  ],
  ACTIVE_ASSIGNMENT: [
    "账号仍承担招聘或面试任务，请先交接",
    "Transfer active assignments before changing this account",
  ],
  UNAUTHENTICATED: ["会话已结束，请重新登录", "Session ended; sign in again"],
  NETWORK_ERROR: [
    "连接失败或超时，请检查后重试",
    "Connection failed or timed out",
  ],
  FORBIDDEN: ["当前账号无权限", "Permission denied"],
  OUT_OF_SCOPE: ["不在你的资料范围内", "Outside your data scope"],
  CONFLICT: [
    "记录已存在或有引用，操作未保存",
    "Duplicate or referenced record; no changes saved",
  ],
  WEAK_PASSWORD: [
    "密码需12–72字节，含大小写和数字",
    "Password: 12–72 bytes, upper/lower case and digits",
  ],
  LAST_ADMIN: [
    "必须保留一个有效管理员",
    "An enabled administrator must remain",
  ],
  INVALID_INPUT: [
    "请检查必填项、格式和长度",
    "Check required fields, format and length",
  ],
  LOGIN_FAILED: ["账号或密码不正确", "Incorrect username or password"],
  TERMINAL_ONLY: [
    "只有已结束申请可以匿名化",
    "Only finished applications can be redacted",
  ],
  INTERVIEW_REQUIRED: [
    "先完成面试并录入反馈",
    "Complete all interviews and record feedback first",
  ],
  NOT_EXPIRED: ["尚未到录用建议有效期", "Offer has not expired"],
  CURRENCY_LOCKED: [
    "录用建议已存在，币种不能再变更",
    "Currency is locked after the first offer",
  ],
  INVALID_STATE: [
    "当前状态不支持此操作，请刷新核对",
    "Action unavailable; refresh and review",
  ],
  HISTORY_PROTECTED: [
    "流程记录不能删除，请使用关闭、撤回或匿名化",
    "History is protected; close, withdraw or redact",
  ],
  INVALID_JOIN_DATE: [
    "入职日期应不早于约定日期且不晚于今天",
    "Joining date must be between the agreed date and today",
  ],
  INTAKE_DISABLED: ["当前未开放投递", "Applications are currently closed"],
  NOTICE_CONFIRMATION_REQUIRED: [
    "资料用途说明已更新，请重新阅读确认",
    "Privacy notice changed; reread and confirm",
  ],
  ASSIGNED_INTERVIEWER_ONLY: [
    "仅指定面试官可以提交反馈",
    "Only the assigned interviewer can submit feedback",
  ],
  INVALID_INTERVIEW_TIME: [
    "时间需有先后，最长4小时，最多回溯30分钟",
    "Check times: up to 4 hours, at most 30 minutes backdated",
  ],
};
/** 返回经过资料范围和状态筛选的招聘队列。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function filtered(rows, query, state = "", jobId = "") {
  return rows.filter(
    (x) =>
      (!state || x.stage === state || x.status === state) &&
      (!jobId || x.jobId === Number(jobId)) &&
      Object.values(x).some(
        (v) =>
          typeof v !== "object" &&
          String(v ?? "")
            .toLowerCase()
            .includes(query.toLowerCase()),
      ),
  );
}
/** 本地表单时间转换为UTC存储；无效时间由表单和服务端拒绝。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function isoLocal(value) {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d.toISOString();
}

Object.assign(names, {
  FULL_TIME: ["全职", "Full time"],
  PART_TIME: ["兼职", "Part time"],
  CONTRACT: ["合同制", "Contract"],
  MANUAL: ["招聘录入", "Recruiter entry"],
  REFERRAL: ["员工推荐", "Referral"],
  JOB_BOARD: ["外部渠道", "External job board"],
});
Object.assign(errors, {
  INVALID_EMAIL: ["邮箱格式不正确", "Invalid email format"],
  INVALID_TEAM_MEMBER: [
    "人员需启用、同部门且有对应权限",
    "Choose an enabled team member in the same department with the required permission",
  ],
  INDEPENDENT_MANAGER_REQUIRED: [
    "招聘人员与审批负责人必须不同",
    "Recruiter and hiring manager must be different people",
  ],
  INVALID_HEADCOUNT: ["招聘人数应为1至1000", "Headcount must be 1–1000"],
  INVALID_SALARY: [
    "薪资需正数且最多两位小数",
    "Salary must be positive with at most 2 decimals",
  ],
  INVALID_OFFER: [
    "检查薪资周期、入职日期和有效期",
    "Check pay period, start date and deadline",
  ],
  DESIGNATED_MANAGER_ONLY: [
    "仅指定负责人可以审批此岗位",
    "Only the designated hiring manager can review this job",
  ],
  APPLICATION_FINISHED: ["申请已结束，不能继续推进", "Application is finished"],
  SCREEN_FIRST: ["先开始初筛，再安排面试", "Start screening before scheduling"],
  INTERVIEW_NOT_FINISHED: [
    "面试尚未结束或已反馈",
    "Interview has not ended or already has feedback",
  ],
  INTERVIEW_LIMIT: ["面试记录最多20条", "Maximum 20 interview records"],
  JOB_NOT_ACTIVE: ["岗位需招聘中或已暂停", "Opening must be open or paused"],
  JOB_NOT_OPEN: [
    "岗位当前不接受公开投递",
    "Opening is not accepting online applications",
  ],
  ACTIVE_APPLICATIONS: [
    "还有在途申请，请先完成或结束流程",
    "Resolve active applications before closing",
  ],
  DRAFT_ONLY: ["仅草稿可以编辑或删除", "Only drafts can be edited or deleted"],
  NOT_FOUND: ["记录不存在或已删除", "Record does not exist"],
  BUILTIN_RESOURCE: [
    "系统基础目录不能删除",
    "Built-in resources cannot be deleted",
  ],
  LOGIN_THROTTLED: [
    "登录尝试过多，请稍后重试",
    "Too many sign-in attempts; retry later",
  ],
  OLD_PASSWORD_INVALID: ["当前密码不正确", "Current password is incorrect"],
  INVALID_DICTIONARY: [
    "选择已启用的招聘字典项",
    "Select an enabled recruitment dictionary item",
  ],
  OFFER_NOT_ACCEPTED: [
    "先登记候选人接受录用建议",
    "Record offer acceptance before joining",
  ],
  INVALID_USERNAME: [
    "账号需3–60位字母、数字或_.-",
    "Username: 3–60 letters, digits, or _.-",
  ],
  RESOURCE_LIMIT: [
    "记录量超过学习版列表上限，请联系扩展",
    "Record limit exceeded; contact us for scaling",
  ],
});

errors.UPLOAD_TOO_LARGE = [
  "上传文件超过3MB上限，请缩小后重试",
  "File exceeds the 3MB limit; reduce it and retry",
];

errors.SOURCE_FROZEN = [
  "在线申请的来源与确认记录不可改写",
  "Online source and consent reference cannot be changed",
];
