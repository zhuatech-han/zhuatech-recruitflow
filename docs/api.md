# 招聘接口

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2

同源 `/api`，JSON与HttpOnly会话；除健康、CSRF、登录、公开岗位与投递外须认证。先GET `/auth/csrf`，非GET请求带响应header指定的token。登录成功后重新取得CSRF。无认证401、无权403、坏参数400、状态/版本/重复409。

| 路径 | 方法及用途 |
|---|---|
| /auth/login, /auth/logout, /auth/password | POST登录、退出、凭旧密码更新（旧会话失效） |
| /auth/me, /auth/csrf | GET当前权限与菜单、CSRF |
| /jobs | GET列表、POST草稿 |
| /jobs/{id} | PUT草稿、DELETE?revision=当前版本 |
| /jobs/{id}/actions/{action} | POST submit/approve/return/pause/resume/assign/close |
| /applicants | GET范围内申请、POST人工录入 |
| /applicants/{id} | GET明细、PUT初筛资料、DELETE?revision=版本 |
| /applicants/{id}/actions/{action} | POST screen/assign/reject/withdraw/hire |
| /applicants/{id}/notes, /interviews, /offers | POST跟进、预约、录用建议 |
| /applicants/{id}/offers/{offerId} | PUT建议草稿 |
| /offers/{id}/actions/{action} | POST submit/approve/return/issue/accept/decline/revoke/expire |
| /offers/{id}/delete | POST删除草稿建议 |
| /interviews | GET权限过滤日程 |
| /interviews/{id}/feedback, /cancel | POST本人反馈、招聘人员取消 |
| /applicants/{id}/resume | POST multipart file + query revision；GET权限附件下载 |
| /applicants/{id}/redact | POST已结束申请匿名化 |
| /catalog, /reports, /reports.csv, /audit | GET目录、统计、CSV、审计 |
| /admin/{type}, /admin/{type}/{id} | GET/POST目录、PUT/DELETE项；type=users/roles/permissions/menus/departments/dictionaries/settings |
| /public/jobs | GET已批准公开岗位及部署方notice/contact/noticeHash |
| /public/applications | POST multipart：data为application/json，file可选PDF；仍需CSRF |

命令正文 `{ "revision": 当前版本, "note": "实际记录说明" }`，交接追加ownerId，入职追加employeeCode/joinedDate。申请变更使用申请revision；岗位命令用岗位revision。金额使用十进制字符串或JSON数值，时间ISO UTC偏移，日期YYYY-MM-DD。建议薪资使用2位精度正金额，入职日期当天至366日后，期限未来90日内。

公开data包含jobId/name/email/phone/introduction/consent=true/noticeHash。返回统一 `{ "ok": true }`，不返回申请编号。不能凭邮箱查询他人申请。对照Java记录定义与操作页面构造字段；未知字段不会绕过服务端状态权限。业务事件不可编辑。
