# 数据库与升级

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2

数据库默认 `zhuatech_recruitflow`。建表与迁移：`backend/src/main/resources/db/migration/V1__recruit_schema.sql`。MySQL8.4首次启动由Flyway执行，Hibernate validate检查实体。H2仅用于快速集成测试；完整部署必须另验MySQL。

| 表 | 内容 |
|---|---|
| department / account | 部门、账号，BCrypt12散列不返回接口 |
| access_role / permission / role_permission | 实时权限与ALL/DEPARTMENT/ASSIGNED范围 |
| nav_menu / dictionary_entry / system_setting | 业务菜单、招聘字典、公开投递参数 |
| audit_event | 操作者、动作、对象、部门、时间元数据 |
| job_opening | 岗位归属、招聘人数、审批状态和版本 |
| applicant | 岗位申请、负责人员、联系资料、阶段与实际入职 |
| interview | 预约、指定面试官、反馈，时间先后约束 |
| recruit_offer | 录用建议版本、金额币种、条款、期限与审批 |
| applicant_note / recruit_event | 跟进与状态历史 |
| resume_file | 当前PDF的MEDIUMBLOB、SHA256、安全文件名 |

同岗位邮箱、员工编号、申请录用版本、当前简历有唯一约束；关联人员、岗位、申请均有外键。删除被引用账号、角色或部门被拒绝；账号有在途任务时，不能改为无权角色、停用或调部门，先交接任务。保留一个启用的ALL管理员。首次初始化只有真实目录，不创建业务样例。

升级：备份 → 在独立卷恢复并验收 → 增加V2等新迁移 → 构建并启动 → 检查Flyway版本及健康 → 验证登录与核心流程。禁止改写已经执行的V1；DDL失败应读日志定位，不通过删原业务卷处理。基础部门1承担事务锁，不可删除。
