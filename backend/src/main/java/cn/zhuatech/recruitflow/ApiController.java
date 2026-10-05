// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 招聘、面试、录用和管理接口；权限与事务由业务服务统一检查。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@RestController
@RequestMapping("/api")
public class ApiController {
  final RecruitService business;
  final AdminService admin;

  public ApiController(RecruitService business, AdminService admin) {
    this.business = business;
    this.admin = admin;
  }

  /** 岗位列表。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/jobs")
  public Object jobs() {
    return business.jobs();
  }

  /** 新增岗位草稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/jobs")
  public Object newJob(@RequestBody RecruitService.JobInput v) {
    return business.saveJob(null, v);
  }

  /** 修改岗位草稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/jobs/{id}")
  public Object editJob(@PathVariable Long id, @RequestBody RecruitService.JobInput v) {
    return business.saveJob(id, v);
  }

  /** 删除没有流程历史的岗位草稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/jobs/{id}")
  public Object deleteJob(@PathVariable Long id, @RequestParam Long revision) {
    business.deleteJob(id, revision);
    return Map.of("ok", true);
  }

  /** 送审、发布和停止招聘等岗位命令。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/jobs/{id}/actions/{action}")
  public Object jobAction(
      @PathVariable Long id, @PathVariable String action, @RequestBody RecruitService.Command c) {
    return business.jobCommand(id, action, c);
  }

  /** 申请列表，面试官不返回薪资或授权凭据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/applicants")
  public Object applicants() {
    return business.applicants();
  }

  /** 申请详情及按身份过滤的面试、录用建议、备注和历史。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/applicants/{id}")
  public Object detail(@PathVariable Long id) {
    return business.detail(id);
  }

  /** 人工录入申请。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/applicants")
  public Object newApplicant(@RequestBody RecruitService.ApplicantInput v) {
    return business.saveApplicant(null, v);
  }

  /** 修改初筛前的资料。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/applicants/{id}")
  public Object editApplicant(@PathVariable Long id, @RequestBody RecruitService.ApplicantInput v) {
    return business.saveApplicant(id, v);
  }

  /** 删除没有流程历史的人工申请草稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/applicants/{id}")
  public Object deleteApplicant(@PathVariable Long id, @RequestParam Long revision) {
    business.deleteApplicant(id, revision);
    return Map.of("ok", true);
  }

  /** 初筛、撤回、拒绝、交接或入职登记。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/applicants/{id}/actions/{action}")
  public Object applicantAction(
      @PathVariable Long id, @PathVariable String action, @RequestBody RecruitService.Command c) {
    return business.applicantCommand(id, action, c);
  }

  /** 追加不可覆写的跟进备注。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/applicants/{id}/notes")
  public Object note(@PathVariable Long id, @RequestBody RecruitService.Command c) {
    return business.note(id, c);
  }

  /** 安排面试并验证人员时段冲突。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/applicants/{id}/interviews")
  public Object schedule(@PathVariable Long id, @RequestBody RecruitService.InterviewInput v) {
    return business.schedule(id, v);
  }

  /** 当前账号可见日程。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/interviews")
  public Object calendar() {
    return business.calendar();
  }

  /** 指定面试官在结束后记录人工反馈。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/interviews/{id}/feedback")
  public Object feedback(@PathVariable Long id, @RequestBody RecruitService.Feedback v) {
    return business.feedback(id, v);
  }

  /** 取消尚未完成的预约，保留历史。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/interviews/{id}/cancel")
  public Object cancel(@PathVariable Long id, @RequestBody RecruitService.Command c) {
    return business.cancelInterview(id, c);
  }

  /** 新增录用建议草稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/applicants/{id}/offers")
  public Object offer(@PathVariable Long id, @RequestBody RecruitService.OfferInput v) {
    return business.saveOffer(id, null, v);
  }

  /** 修改尚未送审的录用建议。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/applicants/{id}/offers/{offerId}")
  public Object editOffer(
      @PathVariable Long id, @PathVariable Long offerId, @RequestBody RecruitService.OfferInput v) {
    return business.saveOffer(id, offerId, v);
  }

  /** 删除尚未送审的录用建议。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/offers/{id}/delete")
  public Object deleteOffer(@PathVariable Long id, @RequestBody RecruitService.Command c) {
    business.deleteOffer(id, c);
    return Map.of("ok", true);
  }

  /** 独立审批、送达记录、外部答复记录和名额释放。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/offers/{id}/actions/{action}")
  public Object offerAction(
      @PathVariable Long id, @PathVariable String action, @RequestBody RecruitService.Command c) {
    return business.offerCommand(id, action, c);
  }

  /** 上传受限PDF附件并保存在数据库，冻结评审中的简历。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/applicants/{id}/resume")
  public Object upload(
      @PathVariable Long id,
      @RequestParam Long revision,
      @RequestParam("file") MultipartFile file) {
    return business.upload(id, revision, file);
  }

  /** PDF仅按附件下载，使用安全自动文件名和独立数据权限。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/applicants/{id}/resume")
  public ResponseEntity<byte[]> resume(@PathVariable Long id) {
    var f = business.resume(id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=" + f.filename)
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "private, no-store")
        .body(f.content);
  }

  /** 已结束申请的个人资料匿名化，保留不含个人内容的流程统计。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/applicants/{id}/redact")
  public Object redact(@PathVariable Long id, @RequestBody RecruitService.Command c) {
    return business.redact(id, c);
  }

  /** 部门、岗位人员和招聘字典。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/catalog")
  public Object catalog() {
    return business.catalog();
  }

  /** 当前数据范围内的岗位漏斗统计。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/reports")
  public Object report() {
    return business.report();
  }

  /** CSV仅导出漏斗统计，文本中和公式前缀，不导出候选人个人资料。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/reports.csv")
  public ResponseEntity<String> csv() {
    var b =
        new StringBuilder(
            "\ufeffJob,Title,Status,Headcount,Applications,NEW,SCREENING,INTERVIEW,OFFER,ACCEPTED,HIRED,REJECTED,WITHDRAWN\r\n");
    for (var row : business.report()) {
      for (var v :
          List.of(
              row.get("code").toString(),
              row.get("title").toString(),
              row.get("status").toString())) {
        b.append(cell(v)).append(',');
      }
      b.append(row.get("headcount")).append(',').append(row.get("applications"));
      for (var stage :
          List.of(
              "NEW",
              "SCREENING",
              "INTERVIEW",
              "OFFER",
              "ACCEPTED",
              "HIRED",
              "REJECTED",
              "WITHDRAWN")) b.append(',').append(((Map<?, ?>) row.get("stages")).get(stage));
      b.append("\r\n");
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .header("Content-Disposition", "attachment; filename=recruitflow-funnel.csv")
        .header("Cache-Control", "private, no-store")
        .body(b.toString());
  }

  private String cell(String s) {
    if (s.matches("^[\\s]*[=+\\-@].*")) s = "'" + s;
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }

  /** 元数据审计，不保存简历或完整业务输入。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/audit")
  public Object audit() {
    return business.audits();
  }

  /** 管理资源列表。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/{type}")
  public Object list(@PathVariable String type) {
    return admin.list(type);
  }

  /** 新增管理资源。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/admin/{type}")
  public Object add(@PathVariable String type, @RequestBody AdminService.Input v) {
    return admin.save(type, null, v);
  }

  /** 修改管理资源。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/admin/{type}/{id}")
  public Object update(
      @PathVariable String type, @PathVariable Long id, @RequestBody AdminService.Input v) {
    return admin.save(type, id, v);
  }

  /** 删除未被引用的管理资源。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/admin/{type}/{id}")
  public Object remove(@PathVariable String type, @PathVariable Long id) {
    admin.delete(type, id);
    return Map.of("ok", true);
  }
}
