// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 招聘业务事务、实时范围、流程与录用名额保护；不自动评价或选择候选人。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Service
@Transactional
public class RecruitService {
  final Store db;
  final AccessService access;
  final Clock clock;

  public RecruitService(Store db, AccessService access, Clock clock) {
    this.db = db;
    this.access = access;
    this.clock = clock;
  }

  /** 岗位草稿输入。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record JobInput(
      String title,
      String location,
      String employmentType,
      String description,
      Long departmentId,
      Long ownerId,
      Long managerId,
      Integer headcount,
      Long revision) {}

  /** 申请基本资料；只收集招聘必要联系资料和文字经历。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record ApplicantInput(
      Long jobId,
      String name,
      String email,
      String phone,
      String introduction,
      String source,
      String consentReference,
      Long revision) {}

  /** 原子命令携带当前版本，拒绝陈旧重复写入。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record Command(
      Long revision, String note, Long ownerId, String employeeCode, LocalDate joinedDate) {}

  /** 单名面试官预约输入，时间是UTC偏移ISO时间。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record InterviewInput(
      Long interviewerId,
      String title,
      String location,
      Instant startsAt,
      Instant endsAt,
      Long revision) {}

  /** 本人不可覆写的人工反馈输入。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record Feedback(Long revision, String recommendation, String feedback) {}

  /** 录用建议的金额、有效期与条款；不构成系统电子合同。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record OfferInput(
      BigDecimal salary,
      String payPeriod,
      LocalDate startDate,
      Instant expiresAt,
      String terms,
      Long revision) {}

  /** 匿名投递输入；必须阅读当前已配置的说明并确认。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public record PublicInput(
      Long jobId,
      String name,
      String email,
      String phone,
      String introduction,
      Boolean consent,
      String noticeHash) {}

  private void lock() {
    db.lock(Department.class, 1L);
  }

  private String text(String value, int max) {
    return AdminService.text(value, max);
  }

  private String optional(String value, int max) {
    if (value == null) return "";
    if (value.length() > max) throw new Problem(400, "INVALID_INPUT");
    return value.trim();
  }

  private Instant now() {
    return clock.instant();
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }

  private void version(long actual, Long supplied) {
    if (supplied == null || actual != supplied) throw new Problem(409, "STALE_VERSION");
  }

  private boolean full() {
    var p = access.role().permissions;
    return p.contains("recruit") || p.contains("approve");
  }

  private boolean terminal(Applicant a) {
    return Set.of("HIRED", "REJECTED", "WITHDRAWN").contains(a.stage);
  }

  private void active(Applicant a) {
    if (terminal(a) || a.redactedAt != null) throw new Problem(409, "APPLICATION_FINISHED");
  }

  private void requireTeam(Long id, Long dept, String permission) {
    if (id == null) throw new Problem(400, "INVALID_TEAM_MEMBER");
    var a = db.get(Account.class, id);
    if (!a.enabled
        || !a.departmentId.equals(dept)
        || !db.get(AccessRole.class, a.roleId).permissions.contains(permission))
      throw new Problem(400, "INVALID_TEAM_MEMBER");
  }

  private List<Applicant> jobApps(Long id) {
    return db.query(Applicant.class, "from Applicant where jobId=?1 order by id", id);
  }

  private List<Interview> interviews(Long id) {
    return db.query(Interview.class, "from Interview where applicantId=?1 order by id", id);
  }

  private List<Offer> offers(Long id) {
    return db.query(Offer.class, "from Offer where applicantId=?1 order by version", id);
  }

  private boolean ownInterview(Long id) {
    return interviews(id).stream().anyMatch(i -> i.interviewerId.equals(access.current().id));
  }

  private boolean visibleJob(JobOpening j) {
    var a = access.current();
    var r = access.role();
    return r.scope.equals("ALL")
        || r.scope.equals("DEPARTMENT") && a.departmentId.equals(j.departmentId)
        || r.scope.equals("ASSIGNED")
            && (a.id.equals(j.ownerId)
                || a.id.equals(j.managerId)
                || jobApps(j.id).stream()
                    .anyMatch(x -> a.id.equals(x.ownerId) || ownInterview(x.id)));
  }

  private boolean visible(Applicant a) {
    var j = db.get(JobOpening.class, a.jobId);
    var u = access.current();
    var r = access.role();
    return r.scope.equals("ALL")
        || r.scope.equals("DEPARTMENT") && u.departmentId.equals(j.departmentId)
        || r.scope.equals("ASSIGNED")
            && (u.id.equals(a.ownerId) || u.id.equals(j.managerId) || ownInterview(a.id));
  }

  private JobOpening job(Long id) {
    access.require("read");
    var j = db.get(JobOpening.class, id);
    if (!visibleJob(j)) throw new Problem(403, "OUT_OF_SCOPE");
    return j;
  }

  private Applicant applicant(Long id) {
    access.require("read");
    var a = db.get(Applicant.class, id);
    if (!visible(a)) throw new Problem(403, "OUT_OF_SCOPE");
    return a;
  }

  private void manager(JobOpening j) {
    access.require("approve");
    if (!access.role().scope.equals("ALL") && !access.current().id.equals(j.managerId))
      throw new Problem(403, "DESIGNATED_MANAGER_ONLY");
  }

  private void event(JobOpening j, Applicant a, String action, String note, boolean anonymous) {
    var e = new RecruitEvent();
    e.jobId = j.id;
    e.applicantId = a == null ? null : a.id;
    e.action = action;
    e.actor = anonymous ? "PUBLIC" : access.current().username;
    e.note = optional(note, 1000);
    e.createdAt = now();
    db.save(e);
    if (!anonymous) access.audit(action, a == null ? j.id : a.id, j.departmentId);
  }

  private String setting(String code) {
    return db.query(SystemSetting.class, "from SystemSetting where code=?1", code).getFirst().value;
  }

  /** SHA-256固定资料摘要，用于确认投递说明版本和附件完整性。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static String digest(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String email(String value) {
    var e = text(value, 254).toLowerCase(Locale.ROOT);
    if (!e.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new Problem(400, "INVALID_EMAIL");
    return e;
  }

  private void dictionary(String type, String code) {
    if (db.query(
            DictionaryEntry.class,
            "from DictionaryEntry where type=?1 and code=?2 and enabled=true",
            type,
            code)
        .isEmpty()) throw new Problem(400, "INVALID_DICTIONARY");
  }

  /** 按数据库实时范围返回基础目录和可指派人员，不暴露密码散列。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> catalog() {
    access.require("read");
    var people =
        db.all(Account.class).stream()
            .filter(
                a ->
                    a.enabled
                        && (access.role().scope.equals("ALL")
                            || a.departmentId.equals(access.current().departmentId)))
            .filter(a -> full() || a.id.equals(access.current().id))
            .map(
                a ->
                    Map.of(
                        "id",
                        a.id,
                        "name",
                        a.displayName,
                        "departmentId",
                        a.departmentId,
                        "permissions",
                        db.get(AccessRole.class, a.roleId).permissions))
            .toList();
    return Map.of(
        "people",
        people,
        "departments",
        db.all(Department.class).stream().filter(d -> access.visible(d.id)).toList(),
        "dictionaries",
        db.all(DictionaryEntry.class).stream().filter(d -> d.enabled).toList(),
        "currency",
        setting("currency"));
  }

  /** 岗位列表和权限相同的统计所用资料。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public List<JobOpening> jobs() {
    access.require("read");
    return db.all(JobOpening.class).stream().filter(this::visibleJob).toList();
  }

  /** 新建或修改尚未送审岗位；发布后必须另建岗位避免改写历史。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public JobOpening saveJob(Long id, JobInput v) {
    access.require("recruit");
    lock();
    var j = id == null ? new JobOpening() : job(id);
    if (id != null) {
      version(j.revision, v.revision);
      if (!j.status.equals("DRAFT")) throw new Problem(409, "DRAFT_ONLY");
    }
    access.department(v.departmentId);
    db.get(Department.class, v.departmentId);
    requireTeam(v.ownerId, v.departmentId, "recruit");
    requireTeam(v.managerId, v.departmentId, "approve");
    if (v.ownerId.equals(v.managerId)) throw new Problem(400, "INDEPENDENT_MANAGER_REQUIRED");
    if (v.headcount == null || v.headcount < 1 || v.headcount > 1000)
      throw new Problem(400, "INVALID_HEADCOUNT");
    dictionary("employment", v.employmentType);
    j.title = text(v.title, 160);
    j.location = text(v.location, 160);
    j.employmentType = v.employmentType;
    j.description = text(v.description, 6000);
    j.departmentId = v.departmentId;
    j.ownerId = v.ownerId;
    j.managerId = v.managerId;
    j.headcount = v.headcount;
    j.revision++;
    if (id == null) {
      j.code = "JOB-" + UUID.randomUUID();
      j.status = "DRAFT";
      j.createdAt = now();
      db.save(j);
    }
    event(j, null, "JOB_SAVED", "", false);
    return j;
  }

  /** 仅删除从未送审且没有历史或申请引用的草稿岗位。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void deleteJob(Long id, Long revision) {
    access.require("recruit");
    lock();
    var j = job(id);
    version(j.revision, revision);
    if (!j.status.equals("DRAFT")
        || !jobApps(id).isEmpty()
        || db.query(
                    RecruitEvent.class,
                    "from RecruitEvent where jobId=?1 and action<>'JOB_SAVED'",
                    id)
                .size()
            > 0) throw new Problem(409, "HISTORY_PROTECTED");
    for (var e : db.query(RecruitEvent.class, "from RecruitEvent where jobId=?1", id)) db.delete(e);
    access.audit("JOB_DRAFT_DELETE", id, j.departmentId);
    db.delete(j);
  }

  /** 岗位送审、独立发布、暂停、恢复、关闭与责任交接。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public JobOpening jobCommand(Long id, String action, Command c) {
    lock();
    var j = job(id);
    version(j.revision, c.revision);
    var note = text(c.note, 1000);
    switch (action) {
      case "submit" -> {
        access.require("recruit");
        if (!j.status.equals("DRAFT")) throw new Problem(409, "INVALID_STATE");
        j.status = "PENDING";
        j.submittedById = access.current().id;
      }
      case "approve", "return" -> {
        manager(j);
        if (!j.status.equals("PENDING")) throw new Problem(409, "INVALID_STATE");
        if ((access.current().id.equals(j.ownerId) || access.current().id.equals(j.submittedById)))
          throw new Problem(403, "SELF_APPROVAL");
        j.status = action.equals("approve") ? "OPEN" : "DRAFT";
        if (j.status.equals("OPEN")) j.publishedAt = now();
      }
      case "pause", "resume" -> {
        access.require("recruit");
        if (!j.status.equals(action.equals("pause") ? "OPEN" : "PAUSED"))
          throw new Problem(409, "INVALID_STATE");
        j.status = action.equals("pause") ? "PAUSED" : "OPEN";
      }
      case "close" -> {
        manager(j);
        if (!Set.of("OPEN", "PAUSED").contains(j.status)
            || jobApps(id).stream().anyMatch(a -> !terminal(a)))
          throw new Problem(409, "ACTIVE_APPLICATIONS");
        j.status = "CLOSED";
      }
      case "assign" -> {
        manager(j);
        if (j.status.equals("CLOSED")) throw new Problem(409, "INVALID_STATE");
        requireTeam(c.ownerId, j.departmentId, "recruit");
        if (c.ownerId.equals(j.managerId)) throw new Problem(400, "INDEPENDENT_MANAGER_REQUIRED");
        j.ownerId = c.ownerId;
      }
      default -> throw new Problem(404, "NOT_FOUND");
    }
    j.revision++;
    event(j, null, "JOB_" + action.toUpperCase(Locale.ROOT), note, false);
    return j;
  }

  private void fields(Applicant a, String name, String mail, String phone, String intro) {
    a.name = text(name, 160);
    a.email = email(mail);
    a.phone = optional(phone, 80);
    a.introduction = text(intro, 4000);
  }

  private Applicant create(
      JobOpening j,
      String name,
      String mail,
      String phone,
      String intro,
      String source,
      String consentRef,
      String consentHash) {
    var a = new Applicant();
    a.reference = "APP-" + UUID.randomUUID();
    a.jobId = j.id;
    a.ownerId = j.ownerId;
    fields(a, name, mail, phone, intro);
    a.source = source;
    a.consentReference = consentRef;
    a.consentHash = consentHash;
    a.consentAt = now();
    a.stage = "NEW";
    a.createdAt = now();
    return db.save(a);
  }

  /** 招聘人员录入、修改或删除未开始评审的申请，记录合法取得资料的外部凭据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Applicant saveApplicant(Long id, ApplicantInput v) {
    access.require("recruit");
    lock();
    var j = job(v.jobId);
    if (!Set.of("OPEN", "PAUSED").contains(j.status)) throw new Problem(409, "JOB_NOT_ACTIVE");
    if (id == null) {
      dictionary("source", v.source);
      String ref = text(v.consentReference, 1000);
      var a = create(j, v.name, v.email, v.phone, v.introduction, v.source, ref, "");
      a.ownerId = access.current().id;
      event(j, a, "APPLICANT_CREATED", "", false);
      return a;
    }
    var a = applicant(id);
    version(a.revision, v.revision);
    if (!a.jobId.equals(j.id)
        || !Set.of("NEW", "SCREENING").contains(a.stage)
        || a.redactedAt != null) throw new Problem(409, "PROFILE_FROZEN");
    fields(a, v.name, v.email, v.phone, v.introduction);
    if (a.source.equals("PUBLIC")) {
      if (!Objects.equals(v.source, a.source)
          || !Objects.equals(v.consentReference, a.consentReference))
        throw new Problem(400, "SOURCE_FROZEN");
    } else {
      dictionary("source", v.source);
      a.source = v.source;
      a.consentReference = text(v.consentReference, 1000);
    }
    a.revision++;
    event(j, a, "PROFILE_UPDATED", "", false);
    return a;
  }

  /** 未评审的人工录入记录可删除；公开申请和已有流程记录保留历史。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void deleteApplicant(Long id, Long revision) {
    access.require("recruit");
    lock();
    var a = applicant(id);
    version(a.revision, revision);
    if (!a.stage.equals("NEW")
        || a.source.equals("PUBLIC")
        || db.query(
                    RecruitEvent.class,
                    "from RecruitEvent where applicantId=?1 and action not in ('APPLICANT_CREATED','PROFILE_UPDATED')",
                    id)
                .size()
            > 0) throw new Problem(409, "HISTORY_PROTECTED");
    for (var f : db.query(ResumeFile.class, "from ResumeFile where applicantId=?1", id))
      db.delete(f);
    for (var e : db.query(RecruitEvent.class, "from RecruitEvent where applicantId=?1", id))
      db.delete(e);
    access.audit("APPLICANT_DRAFT_DELETE", id, db.get(JobOpening.class, a.jobId).departmentId);
    db.delete(a);
  }

  /** 按申请数据范围返回列表；面试岗位不返回授权凭据、其他内部备注或薪资。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public List<Object> applicants() {
    access.require("read");
    return db.all(Applicant.class).stream().filter(this::visible).map(this::viewApplicant).toList();
  }

  private Object viewApplicant(Applicant a) {
    if (full()) return a;
    var m = new LinkedHashMap<String, Object>();
    m.put("id", a.id);
    m.put("reference", a.reference);
    m.put("jobId", a.jobId);
    m.put("name", a.name);
    m.put("email", a.email);
    m.put("phone", a.phone);
    m.put("introduction", a.introduction);
    m.put("stage", a.stage);
    m.put("revision", a.revision);
    m.put("createdAt", a.createdAt);
    m.put("redactedAt", a.redactedAt);
    return m;
  }

  /** 明细按岗位范围过滤，二进制简历走独立权限下载接口。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> detail(Long id) {
    var a = applicant(id);
    var f = db.query(ResumeFile.class, "from ResumeFile where applicantId=?1", id);
    return Map.of(
        "applicant",
        viewApplicant(a),
        "job",
        job(a.jobId),
        "interviews",
        interviews(id).stream()
            .filter(i -> full() || i.interviewerId.equals(access.current().id))
            .toList(),
        "offers",
        full() ? offers(id) : List.of(),
        "notes",
        full()
            ? db.query(
                ApplicantNote.class, "from ApplicantNote where applicantId=?1 order by id", id)
            : List.of(),
        "events",
        full()
            ? db.query(RecruitEvent.class, "from RecruitEvent where applicantId=?1 order by id", id)
            : List.of(),
        "resume",
        f.isEmpty()
            ? Map.of()
            : Map.of(
                "id",
                f.getFirst().id,
                "filename",
                f.getFirst().filename,
                "digest",
                f.getFirst().digest));
  }

  /** 人工初筛、撤回、拒绝、指派及实际入职登记；不从反馈自动推断决定。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Applicant applicantCommand(Long id, String action, Command c) {
    lock();
    var a = applicant(id);
    version(a.revision, c.revision);
    active(a);
    var j = job(a.jobId);
    var note = text(c.note, 1000);
    switch (action) {
      case "screen" -> {
        access.require("recruit");
        if (!a.stage.equals("NEW")) throw new Problem(409, "INVALID_STATE");
        a.stage = "SCREENING";
      }
      case "assign" -> {
        access.require("recruit");
        requireTeam(c.ownerId, j.departmentId, "recruit");
        a.ownerId = c.ownerId;
      }
      case "reject", "withdraw" -> {
        if (action.equals("reject")) manager(j);
        else access.require("recruit");
        for (var i : interviews(id)) if (i.status.equals("SCHEDULED")) i.status = "CANCELLED";
        for (var o : offers(id))
          if (Set.of("DRAFT", "PENDING", "APPROVED", "ISSUED", "ACCEPTED").contains(o.status))
            o.status = "REVOKED";
        a.stage = action.equals("reject") ? "REJECTED" : "WITHDRAWN";
      }
      case "hire" -> {
        manager(j);
        if (!a.stage.equals("ACCEPTED")) throw new Problem(409, "OFFER_NOT_ACCEPTED");
        var o =
            offers(id).stream().filter(x -> x.status.equals("ACCEPTED")).findFirst().orElseThrow();
        if (c.joinedDate == null
            || c.joinedDate.isBefore(o.startDate)
            || c.joinedDate.isAfter(today())) throw new Problem(400, "INVALID_JOIN_DATE");
        a.employeeCode = text(c.employeeCode, 80);
        a.joinedDate = c.joinedDate;
        a.stage = "HIRED";
      }
      default -> throw new Problem(404, "NOT_FOUND");
    }
    a.revision++;
    event(j, a, "APPLICANT_" + action.toUpperCase(Locale.ROOT), note, false);
    return a;
  }

  /** 内部跟进备注；提交后只能经正式资料匿名化移除个人内容。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public ApplicantNote note(Long id, Command c) {
    access.require("recruit");
    lock();
    var a = applicant(id);
    version(a.revision, c.revision);
    active(a);
    var n = new ApplicantNote();
    n.applicantId = id;
    n.actorId = access.current().id;
    n.note = text(c.note, 4000);
    n.createdAt = now();
    db.save(n);
    a.revision++;
    event(job(a.jobId), a, "NOTE_ADDED", "", false);
    return n;
  }

  /** 预约同部门面试官，锁内检查本人和候选人的重叠时段；最多回溯30分钟登记。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Interview schedule(Long id, InterviewInput v) {
    access.require("recruit");
    lock();
    var a = applicant(id);
    version(a.revision, v.revision);
    active(a);
    if (!Set.of("SCREENING", "INTERVIEW").contains(a.stage)) throw new Problem(409, "SCREEN_FIRST");
    var j = job(a.jobId);
    requireTeam(v.interviewerId, j.departmentId, "interview");
    if (v.startsAt == null
        || v.endsAt == null
        || !v.endsAt.isAfter(v.startsAt)
        || v.startsAt.isBefore(now().minusSeconds(1800))
        || v.startsAt.isAfter(now().plusSeconds(366 * 86400L))
        || Duration.between(v.startsAt, v.endsAt).toSeconds() > 14400)
      throw new Problem(400, "INVALID_INTERVIEW_TIME");
    if (interviews(id).size() >= 20) throw new Problem(413, "INTERVIEW_LIMIT");
    boolean overlap =
        db.all(Interview.class).stream()
            .filter(i -> !i.status.equals("CANCELLED"))
            .anyMatch(
                i ->
                    (i.interviewerId.equals(v.interviewerId) || i.applicantId.equals(id))
                        && i.startsAt.isBefore(v.endsAt)
                        && v.startsAt.isBefore(i.endsAt));
    if (overlap) throw new Problem(409, "INTERVIEW_CONFLICT");
    var i = new Interview();
    i.applicantId = id;
    i.interviewerId = v.interviewerId;
    i.title = text(v.title, 160);
    i.location = text(v.location, 300);
    i.startsAt = v.startsAt;
    i.endsAt = v.endsAt;
    i.status = "SCHEDULED";
    i.recommendation = "";
    i.feedback = "";
    i.creatorId = access.current().id;
    db.save(i);
    a.stage = "INTERVIEW";
    a.revision++;
    event(j, a, "INTERVIEW_SCHEDULED", "", false);
    return i;
  }

  /** 日程只显示有权访问的预约，普通面试官只看到本人任务。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public List<Interview> calendar() {
    access.require("read");
    return db.all(Interview.class).stream()
        .filter(
            i ->
                visible(db.get(Applicant.class, i.applicantId))
                    && (full() || i.interviewerId.equals(access.current().id)))
        .toList();
  }

  /** 只有指定面试官在结束时刻后记录反馈，不得重写已完成面试。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Interview feedback(Long id, Feedback v) {
    access.require("interview");
    lock();
    var i = db.get(Interview.class, id);
    var a = applicant(i.applicantId);
    version(a.revision, v.revision);
    active(a);
    if (!i.interviewerId.equals(access.current().id))
      throw new Problem(403, "ASSIGNED_INTERVIEWER_ONLY");
    if (!i.status.equals("SCHEDULED") || now().isBefore(i.endsAt))
      throw new Problem(409, "INTERVIEW_NOT_FINISHED");
    if (v.recommendation == null
        || !Set.of("POSITIVE", "CONCERNS", "NEUTRAL", "NO_SHOW").contains(v.recommendation))
      throw new Problem(400, "INVALID_RECOMMENDATION");
    i.feedback = text(v.feedback, 4000);
    i.recommendation = v.recommendation;
    i.status = "COMPLETED";
    i.completedAt = now();
    a.revision++;
    event(job(a.jobId), a, "INTERVIEW_COMPLETED", "", false);
    return i;
  }

  /** 招聘人员取消未完成日程并保留理由，释放时段但不删除历史。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Interview cancelInterview(Long id, Command c) {
    access.require("recruit");
    lock();
    var i = db.get(Interview.class, id);
    var a = applicant(i.applicantId);
    version(a.revision, c.revision);
    active(a);
    if (!i.status.equals("SCHEDULED")) throw new Problem(409, "INVALID_STATE");
    i.status = "CANCELLED";
    a.revision++;
    event(job(a.jobId), a, "INTERVIEW_CANCELLED", text(c.note, 1000), false);
    return i;
  }

  /** 新建、修改或删除未送审录用版本；已完成面试才允许提出人工录用建议。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Offer saveOffer(Long applicantId, Long offerId, OfferInput v) {
    access.require("recruit");
    lock();
    var a = applicant(applicantId);
    version(a.revision, v.revision);
    active(a);
    var prior = offers(a.id);
    var o = offerId == null ? new Offer() : db.get(Offer.class, offerId);
    if (offerId == null) {
      if (!a.stage.equals("INTERVIEW")
          || prior.stream()
              .anyMatch(
                  x ->
                      Set.of("DRAFT", "PENDING", "APPROVED", "ISSUED", "ACCEPTED")
                          .contains(x.status))
          || interviews(a.id).stream()
              .noneMatch(i -> i.status.equals("COMPLETED") && !i.recommendation.equals("NO_SHOW"))
          || interviews(a.id).stream().anyMatch(i -> i.status.equals("SCHEDULED")))
        throw new Problem(409, "INTERVIEW_REQUIRED");
      o.applicantId = a.id;
      o.version = prior.size() + 1;
      o.creatorId = access.current().id;
      o.createdAt = now();
      o.status = "DRAFT";
      o.reviewNote = "";
      o.deliveryReference = "";
      o.responseReference = "";
    } else if (!o.applicantId.equals(a.id) || !o.status.equals("DRAFT"))
      throw new Problem(409, "DRAFT_ONLY");
    if (v.salary == null
        || v.salary.signum() <= 0
        || v.salary.scale() > 2
        || v.salary.compareTo(new BigDecimal("999999999999.99")) > 0)
      throw new Problem(400, "INVALID_SALARY");
    if (v.payPeriod == null
        || !Set.of("MONTHLY", "ANNUAL").contains(v.payPeriod)
        || v.startDate == null
        || v.startDate.isBefore(today())
        || v.startDate.isAfter(today().plusDays(366))
        || v.expiresAt == null
        || !v.expiresAt.isAfter(now())
        || v.expiresAt.isAfter(now().plusSeconds(90 * 86400L)))
      throw new Problem(400, "INVALID_OFFER");
    o.salary = v.salary;
    o.currency = setting("currency");
    o.payPeriod = v.payPeriod;
    o.startDate = v.startDate;
    o.expiresAt = v.expiresAt;
    o.terms = text(v.terms, 4000);
    if (offerId == null) db.save(o);
    a.stage = "OFFER";
    a.revision++;
    event(job(a.jobId), a, "OFFER_DRAFT_SAVED", "", false);
    return o;
  }

  /** 删除未送审的建议版本并将申请返回面试阶段，已送审建议不物理删除。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void deleteOffer(Long id, Command c) {
    access.require("recruit");
    lock();
    var o = db.get(Offer.class, id);
    var a = applicant(o.applicantId);
    version(a.revision, c.revision);
    active(a);
    if (!o.status.equals("DRAFT")) throw new Problem(409, "DRAFT_ONLY");
    db.delete(o);
    a.stage = "INTERVIEW";
    a.revision++;
    event(job(a.jobId), a, "OFFER_DRAFT_DELETED", text(c.note, 1000), false);
  }

  private long reserved(JobOpening j) {
    return db.all(Offer.class).stream()
        .filter(
            o ->
                Set.of("APPROVED", "ISSUED", "ACCEPTED").contains(o.status)
                    && db.get(Applicant.class, o.applicantId).jobId.equals(j.id))
        .count();
  }

  /** 独立审批检查名额；发送和接受只登记外部凭据，不发信、不签署、不转账。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Offer offerCommand(Long id, String action, Command c) {
    lock();
    var o = db.get(Offer.class, id);
    var a = applicant(o.applicantId);
    version(a.revision, c.revision);
    active(a);
    var j = job(a.jobId);
    var note = text(c.note, 1000);
    switch (action) {
      case "submit" -> {
        access.require("recruit");
        if (!o.status.equals("DRAFT") || !now().isBefore(o.expiresAt))
          throw new Problem(409, "INVALID_STATE");
        o.status = "PENDING";
        o.submittedById = access.current().id;
      }
      case "approve", "return" -> {
        manager(j);
        if (!o.status.equals("PENDING") || !now().isBefore(o.expiresAt))
          throw new Problem(409, "INVALID_STATE");
        if (o.creatorId.equals(access.current().id)
            || Objects.equals(o.submittedById, access.current().id))
          throw new Problem(403, "SELF_APPROVAL");
        if (action.equals("approve")) {
          if (reserved(j) >= j.headcount) throw new Problem(409, "HEADCOUNT_FULL");
          o.status = "APPROVED";
        } else {
          o.status = "REJECTED";
          a.stage = "INTERVIEW";
        }
        o.reviewerId = access.current().id;
        o.reviewNote = note;
      }
      case "issue" -> {
        access.require("recruit");
        if (!o.status.equals("APPROVED") || !now().isBefore(o.expiresAt))
          throw new Problem(409, "INVALID_STATE");
        o.status = "ISSUED";
        o.deliveryReference = text(c.note, 300);
      }
      case "accept", "decline" -> {
        access.require("recruit");
        if (!o.status.equals("ISSUED") || !now().isBefore(o.expiresAt))
          throw new Problem(409, "INVALID_STATE");
        o.status = action.equals("accept") ? "ACCEPTED" : "DECLINED";
        o.responseReference = text(c.note, 300);
        a.stage = action.equals("accept") ? "ACCEPTED" : "INTERVIEW";
      }
      case "revoke" -> {
        manager(j);
        if (!Set.of("DRAFT", "PENDING", "APPROVED", "ISSUED", "ACCEPTED").contains(o.status))
          throw new Problem(409, "INVALID_STATE");
        o.status = "REVOKED";
        o.responseReference = text(c.note, 300);
        a.stage = "INTERVIEW";
      }
      case "expire" -> {
        access.require("recruit");
        if (!Set.of("DRAFT", "PENDING", "APPROVED", "ISSUED").contains(o.status)
            || now().isBefore(o.expiresAt)) throw new Problem(409, "NOT_EXPIRED");
        o.status = "EXPIRED";
        a.stage = "INTERVIEW";
      }
      default -> throw new Problem(404, "NOT_FOUND");
    }
    a.revision++;
    event(j, a, "OFFER_" + action.toUpperCase(Locale.ROOT), note, false);
    return o;
  }

  private byte[] pdf(MultipartFile file) {
    try {
      if (file == null || file.isEmpty() || file.getSize() > 3145728)
        throw new Problem(400, "INVALID_RESUME");
      var b = file.getBytes();
      var start = new String(b, 0, Math.min(b.length, 8), StandardCharsets.US_ASCII);
      var tail =
          new String(
              b,
              Math.max(0, b.length - 1024),
              Math.min(1024, b.length),
              StandardCharsets.ISO_8859_1);
      if (!start.startsWith("%PDF-") || !tail.contains("%%EOF"))
        throw new Problem(400, "INVALID_RESUME");
      return b;
    } catch (java.io.IOException e) {
      throw new Problem(400, "INVALID_RESUME");
    }
  }

  private ResumeFile savePdf(Applicant a, MultipartFile file) {
    var bytes = pdf(file);
    var rows = db.query(ResumeFile.class, "from ResumeFile where applicantId=?1", a.id);
    var f = rows.isEmpty() ? new ResumeFile() : rows.getFirst();
    f.applicantId = a.id;
    f.filename = "resume-" + a.id + ".pdf";
    f.digest = digest(bytes);
    f.createdAt = now();
    f.content = bytes;
    if (rows.isEmpty()) db.save(f);
    return f;
  }

  /** 初筛结束前可更新当前PDF简历；文件只下载不执行，原始路径与名字不信任。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> upload(Long id, Long revision, MultipartFile file) {
    access.require("recruit");
    lock();
    var a = applicant(id);
    version(a.revision, revision);
    active(a);
    if (!Set.of("NEW", "SCREENING").contains(a.stage)) throw new Problem(409, "PROFILE_FROZEN");
    var f = savePdf(a, file);
    a.revision++;
    event(job(a.jobId), a, "RESUME_UPDATED", "", false);
    return Map.of("id", f.id, "digest", f.digest);
  }

  /** 简历下载重复验证当前申请权限和资料状态。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public ResumeFile resume(Long id) {
    var a = applicant(id);
    var f =
        db.query(ResumeFile.class, "from ResumeFile where applicantId=?1", id).stream()
            .findFirst()
            .orElseThrow(() -> new Problem(404, "NOT_FOUND"));
    if (a.redactedAt != null) throw new Problem(404, "NOT_FOUND");
    return f;
  }

  /** 匿名化已结束申请的个人文本与文件，保留不可再执行的状态、人员和时间元数据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Applicant redact(Long id, Command c) {
    access.require("privacy");
    lock();
    var a = applicant(id);
    version(a.revision, c.revision);
    if (!terminal(a) || a.redactedAt != null) throw new Problem(409, "TERMINAL_ONLY");
    var j = job(a.jobId);
    String reason = text(c.note, 1000);
    a.name = "已匿名化 / Redacted";
    a.email = UUID.randomUUID() + "@example.invalid";
    a.phone = "";
    a.introduction = "";
    a.consentReference = "";
    a.consentHash = "";
    a.employeeCode = null;
    a.redactedAt = now();
    for (var n : db.query(ApplicantNote.class, "from ApplicantNote where applicantId=?1", id))
      n.note = "";
    for (var i : interviews(id)) {
      i.feedback = "";
      i.location = "";
      i.title = "已匿名化 / Redacted";
    }
    for (var o : offers(id)) {
      o.salary = BigDecimal.ZERO;
      o.terms = "";
      o.reviewNote = "";
      o.deliveryReference = "";
      o.responseReference = "";
    }
    for (var e : db.query(RecruitEvent.class, "from RecruitEvent where applicantId=?1", id))
      e.note = "";
    for (var f : db.query(ResumeFile.class, "from ResumeFile where applicantId=?1", id))
      db.delete(f);
    a.revision++;
    event(
        j,
        a,
        "PERSONAL_DATA_REDACTED",
        "已依据内部处置记录匿名化 / Redacted under internal instruction",
        false);
    return a;
  }

  /** 公开岗位只包含批准职位和投递说明，不返回账号、申请、薪酬或部门权限配置。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> publicJobs() {
    boolean enabled =
        setting("publicIntake").equals("true")
            && setting("privacyNotice").length() >= 50
            && !setting("privacyContact").isBlank();
    var list =
        enabled
            ? db.all(JobOpening.class).stream()
                .filter(j -> j.status.equals("OPEN"))
                .map(
                    j ->
                        Map.of(
                            "id",
                            j.id,
                            "code",
                            j.code,
                            "title",
                            j.title,
                            "location",
                            j.location,
                            "employmentType",
                            j.employmentType,
                            "description",
                            j.description,
                            "headcount",
                            j.headcount))
                .toList()
            : List.of();
    return Map.of(
        "companyName",
        setting("companyName"),
        "enabled",
        enabled,
        "jobs",
        list,
        "notice",
        enabled ? setting("privacyNotice") : "",
        "contact",
        enabled ? setting("privacyContact") : "",
        "noticeHash",
        enabled ? digest(setting("privacyNotice").getBytes(StandardCharsets.UTF_8)) : "");
  }

  /** 匿名投递同岗位同邮箱去重且不泄露原申请是否存在，不允许借重复投递覆写资料。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Boolean> apply(PublicInput v, MultipartFile file) {
    lock();
    var config = publicJobs();
    if (!Boolean.TRUE.equals(config.get("enabled"))) throw new Problem(409, "INTAKE_DISABLED");
    if (!Boolean.TRUE.equals(v.consent) || !config.get("noticeHash").equals(v.noticeHash))
      throw new Problem(400, "NOTICE_CONFIRMATION_REQUIRED");
    var j = db.get(JobOpening.class, v.jobId);
    if (!j.status.equals("OPEN")) throw new Problem(409, "JOB_NOT_OPEN");
    var normalized = email(v.email);
    text(v.name, 160);
    text(v.introduction, 4000);
    optional(v.phone, 80);
    if (file != null && !file.isEmpty()) pdf(file);
    if (!db.query(Applicant.class, "from Applicant where jobId=?1 and email=?2", j.id, normalized)
        .isEmpty()) return Map.of("ok", true);
    var a =
        create(
            j,
            v.name,
            normalized,
            v.phone,
            v.introduction,
            "PUBLIC",
            "公开投递确认 / Public application consent",
            v.noticeHash);
    if (file != null && !file.isEmpty()) savePdf(a, file);
    event(j, a, "PUBLIC_APPLICATION", "", true);
    return Map.of("ok", true);
  }

  /** 汇总采用当前数据范围和状态，薪酬不混入招聘统计，不宣称真实经营效果。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public List<Map<String, Object>> report() {
    access.require("report");
    return jobs().stream()
        .map(
            j -> {
              var apps = jobApps(j.id).stream().filter(this::visible).toList();
              var stages = new LinkedHashMap<String, Long>();
              for (var stage :
                  List.of(
                      "NEW",
                      "SCREENING",
                      "INTERVIEW",
                      "OFFER",
                      "ACCEPTED",
                      "HIRED",
                      "REJECTED",
                      "WITHDRAWN"))
                stages.put(stage, apps.stream().filter(a -> a.stage.equals(stage)).count());
              return Map.<String, Object>of(
                  "jobId",
                  j.id,
                  "title",
                  j.title,
                  "code",
                  j.code,
                  "status",
                  j.status,
                  "headcount",
                  j.headcount,
                  "applications",
                  apps.size(),
                  "stages",
                  stages);
            })
        .toList();
  }

  /** 审计按部门权限过滤，不返回请求载荷、口令或简历。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public List<AuditEvent> audits() {
    access.require("audit");
    if (access.role().scope.equals("ASSIGNED")) throw new Problem(403, "FORBIDDEN");
    return db.all(AuditEvent.class).stream().filter(e -> access.visible(e.departmentId)).toList();
  }
}
