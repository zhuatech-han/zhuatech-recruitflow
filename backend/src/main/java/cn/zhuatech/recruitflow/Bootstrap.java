// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 首次空库初始化招聘岗位权限和配置，不填候选人、薪资或已录用业务。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Component
public class Bootstrap implements ApplicationRunner {
  final Store db;
  final BCryptPasswordEncoder encoder;

  @Value("${recruitflow.admin-username}")
  String username;

  @Value("${recruitflow.admin-password}")
  String password;

  public Bootstrap(Store db, BCryptPasswordEncoder encoder) {
    this.db = db;
    this.encoder = encoder;
  }

  /** 仅初始化空账号库，重启不重置口令。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!db.all(Account.class).isEmpty()) return;
    AdminService.validatePassword(password);
    if (!username.matches("[a-zA-Z0-9_.-]{3,60}"))
      throw new IllegalArgumentException("INVALID_ADMIN_USERNAME");
    var d = new Department();
    d.name = "招聘运营 / Recruitment";
    db.save(d);
    var all = new HashSet<String>();
    for (var v :
        new String[][] {
          {"dashboard", "工作台 / Workspace"},
          {"read", "招聘资料 / Recruitment records"},
          {"recruit", "招聘执行 / Recruiting"},
          {"approve", "岗位与录用审批 / Hiring approval"},
          {"interview", "本人面试 / Assigned interviews"},
          {"report", "招聘统计 / Reports"},
          {"privacy", "个人资料匿名化 / Redact personal data"},
          {"admin", "账号与设置 / Administration"},
          {"audit", "操作记录 / Audit"}
        }) {
      var p = new Permission();
      p.code = v[0];
      p.name = v[1];
      db.save(p);
      all.add(v[0]);
    }
    var admin = role("管理员 / Administrator", "ALL", all);
    role("招聘人员 / Recruiter", "DEPARTMENT", Set.of("dashboard", "read", "recruit", "report"));
    role(
        "招聘负责人 / Hiring manager",
        "DEPARTMENT",
        Set.of("dashboard", "read", "approve", "report", "privacy"));
    role("面试官 / Interviewer", "ASSIGNED", Set.of("dashboard", "read", "interview"));
    var a = new Account();
    a.username = username;
    a.displayName = "管理员 / Administrator";
    a.passwordHash = encoder.encode(password);
    a.roleId = admin.id;
    a.departmentId = d.id;
    a.enabled = true;
    db.save(a);
    int pos = 0;
    for (var v :
        new String[][] {
          {"dashboard", "工作台", "Workspace", "dashboard"},
          {"jobs", "招聘岗位", "Job openings", "read"},
          {"applicants", "候选人", "Applicants", "read"},
          {"interviews", "面试日程", "Interviews", "read"},
          {"reports", "招聘统计", "Hiring reports", "report"},
          {"admin", "账号与设置", "Administration", "admin"},
          {"audit", "操作记录", "Audit", "audit"}
        }) {
      var m = new NavMenu();
      m.code = v[0];
      m.name = v[1];
      m.nameEn = v[2];
      m.permissionCode = v[3];
      m.position = pos++;
      m.enabled = true;
      db.save(m);
    }
    for (var v :
        new String[][] {
          {"companyName", "招聘团队 / Hiring team"},
          {"currency", "CNY"},
          {"publicIntake", "false"},
          {"privacyNotice", ""},
          {"privacyContact", ""}
        }) {
      var s = new SystemSetting();
      s.code = v[0];
      s.value = v[1];
      db.save(s);
    }
    for (var v :
        new String[][] {
          {"employment", "FULL_TIME", "全职", "Full time"},
          {"employment", "PART_TIME", "兼职", "Part time"},
          {"employment", "CONTRACT", "合同制", "Contract"},
          {"source", "REFERRAL", "员工推荐", "Referral"},
          {"source", "MANUAL", "招聘录入", "Recruiter entry"},
          {"source", "JOB_BOARD", "外部渠道人工登记", "Job board (manual)"}
        }) {
      var e = new DictionaryEntry();
      e.type = v[0];
      e.code = v[1];
      e.name = v[2];
      e.nameEn = v[3];
      e.enabled = true;
      db.save(e);
    }
  }

  private AccessRole role(String name, String scope, Set<String> permissions) {
    var r = new AccessRole();
    r.name = name;
    r.scope = scope;
    r.permissions = new HashSet<>(permissions);
    return db.save(r);
  }
}
