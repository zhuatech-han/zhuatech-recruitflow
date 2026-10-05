# 部署、备份与恢复

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2

复制 `.env.example` 私下填写强密码，或运行 `python3 scripts/init-env.py` 生成0600私有文件。不得上传.env。运行 `docker compose config --quiet` 和 `docker compose up -d --build --wait`；MySQL健康后启动后端，后端健康后启动Nginx。默认127.0.0.1:8114，可通过WEB_PORT覆盖。数据库无宿主机公开端口。后端与Nginx非root运行。

开发HTTP设COOKIE_SECURE=false；面向公网需部署受信HTTPS证书和反向代理，并设COOKIE_SECURE=true。代理必须覆盖客户端伪造的转发头，不能透传任意X-Forwarded-Proto。当前Nginx覆盖转发来源，直接TLS终结可按实际环境配置；外部TLS代理链路必须一致配置，否则Secure Cookie登录失败。域名、证书、邮件、隐私说明由部署方管理。

外部MySQL使用 `jdbc:mariadb://...?...&sslMode=verify-full` 和受信CA，不使用孤立Compose网络的trust模式连接公网。限制数据库账户、管理后台和备份读取范围，建立实际资料保留与处置流程。

备份包括简历和招聘资料，应保存在受限位置，不能提交Git。下面只在自己的部署资源运行：

```sh
umask 077
docker compose exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --no-tablespaces zhuatech_recruitflow' > recruitflow-private-backup.sql
```

恢复到独立新Compose项目与端口，避免覆盖原库。先启动目标mysql，然后：

```sh
docker compose -p recruitflow-restore exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" zhuatech_recruitflow' < recruitflow-private-backup.sql
docker compose -p recruitflow-restore up -d backend frontend --wait
```

上述目标项目需预先配置不同WEB_PORT，原应用版本必须匹配备份结构。验证版本、账号不重置、岗位/申请/附件摘要、权限与业务流程后才能决定实际恢复。不要将带个人资料的备份用于公开试用。`docker compose down`停止服务保留卷；`down -v`删除全部项目数据库，只能用于明确可丢弃的独立测试项目。
