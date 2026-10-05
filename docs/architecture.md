# 架构与状态约束

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2

浏览器 → Nginx8080 → Spring Boot8080 → MySQL8.4。Vue 使用同源 HttpOnly/SameSite 会话、内存CSRF令牌。登录旋转会话，账号启用状态和当前散列、角色范围在服务端每次重新核对。密码更新使旧会话失效。所有SQL查询使用参数绑定。

AdminService负责管理目录；RecruitService负责岗位、候选人、面试与录用事务。写事务先锁基础部门1，使用READ_COMMITTED获取最新业务版本，避免并发超人数和时间双约。申请与岗位revision在命令中必填，错误版本返回409，前端保留失败表单。该串行策略面向中小团队，不能宣称分布式或海量吞吐。列表上限10000，客户端分页不等于服务器分页。

岗位：DRAFT → PENDING → OPEN；退回DRAFT，OPEN ↔ PAUSED，所有申请结束后CLOSED。必须由非岗位招聘人员且非送审人批准。已经发布的职责、人数与部门冻结。

申请：NEW → SCREENING → INTERVIEW → OFFER → ACCEPTED → HIRED。人工拒绝/撤回终止在途记录并取消预约、撤销有效建议；不能恢复终态。录用建议退回、婉拒、撤销或到期释放返回INTERVIEW。

录用建议：DRAFT → PENDING → APPROVED → ISSUED → ACCEPTED；REJECTED、DECLINED、REVOKED、EXPIRED结束建议。创建人与审批人分离，批准前锁内检查岗位headcount，所有已批准/送达/接受（含已入职）的建议占用名额。草稿只在面试反馈齐全时建立；同申请不能有两个在途建议。

面试官ASSIGNED范围依靠其实际面试记录判定申请可见。取消的安排仍保留历史归属；普通面试官只返回本人的记录、必要联系资料和简历，不能获得其他人的薪资、内部跟进或资料取得凭据。部门范围严格对岗位部门校验，管理员ALL范围不免除指定面试官或独立审批规则。

匿名化在一个事务内更新所有关联个人字段并删简历；审计不储存完整载荷。附件只做3MiB和PDF头尾签名检查，使用安全自动文件名、权限附件下载与nosniff，不在页面内执行PDF，也未提供病毒检测。
