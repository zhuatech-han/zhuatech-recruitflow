# 测试与部署验收

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2

后端共19项测试（4单元、15 HTTP/JPA集成），前端5项测试。后端：`mvn -B -f backend/pom.xml spotless:check test package`。RecruitPolicyTest验证口令字节边界、必填文本和摘要；RecruitIntegrationTest以实际HTTP、安全过滤链、Flyway和JPA执行完整招聘到入职，覆盖部门/任务权限、预约冲突、本人反馈与时间、版本冲突、PDF类型和按申请下载、匿名化、CSRF、最后管理员和默认关闭公开投递。H2测试不是MySQL部署证明。

前端：npm ci、format:check、lint、test、build。纯函数检查筛选、分页、币种与本地时间转UTC；页面验收另外在实际运行容器中执行。

发布检查：`python3 scripts/release-check.py` 验证署名、许可、README图路径、两张原始二维码摘要和已知秘密格式；不是完整安全审计。`git diff --check`；`docker compose config --quiet`；Docker构建完整执行后端测试和前端检查。

独立MySQL验收：使用新项目名recruitflow-check、新数据库卷，确认健康和FlywayV1，首次业务为空。生成仅TEST账号和example.invalid申请，验证新建岗位、送审发布、人工/公开投递、PDF存取、面试反馈、独立建议审批、超人数拒绝、送达/接受记录、入职、关闭、统计导出、撤回与个人资料匿名化；另外验证无权调用、跨部门、重复、CSRF、陈旧版本、相邻/重叠预约、暂停公开投递、公开限流、账号任务保护与旧会话失效。

浏览器验收：登录、菜单、表单失败保留、筛选/排序/分页、真实附件上传下载、面试官资料最小化、实际CSV下载、双语和390px布局；截图采集实际页面。备份到0600私有文件并在另一新卷恢复，核对账号不重置、迁移不重复、业务与附件摘要持久化。仅清理自己的测试容器、网络与卷，不删除其他项目资源。

空库 smoke 验收命令：`python3 scripts/smoke-test.py --base http://127.0.0.1:8114`。脚本拒绝已有业务库，生成TEST资料、随机测试口令、PDF与0600私有验收状态，输出181项断言结果。只在可丢弃的独立库运行。
