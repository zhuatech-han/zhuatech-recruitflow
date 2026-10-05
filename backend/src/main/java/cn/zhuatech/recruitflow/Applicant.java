// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import jakarta.persistence.*;
import java.time.*;

/** 单岗位申请档案；不同岗位独立保存，限制部门和执行人访问。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "applicant")
public class Applicant {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "reference", nullable = false, length = 60)
  public String reference;

  @Column(name = "job_id", nullable = false)
  public Long jobId;

  @Column(name = "owner_id", nullable = false)
  public Long ownerId;

  @Column(name = "name", nullable = false, length = 160)
  public String name;

  @Column(name = "email", nullable = false, length = 254)
  public String email;

  @Column(name = "phone", nullable = false, length = 80)
  public String phone;

  @Column(name = "introduction", nullable = false, length = 4000)
  public String introduction;

  @Column(name = "source", nullable = false, length = 60)
  public String source;

  @Column(name = "consent_reference", nullable = false, length = 1000)
  public String consentReference;

  @Column(name = "consent_hash", nullable = false, length = 64)
  public String consentHash;

  @Column(name = "consent_at", nullable = false)
  public Instant consentAt;

  @Column(name = "stage", nullable = false, length = 20)
  public String stage;

  @Column(name = "revision", nullable = false)
  public long revision;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "employee_code", nullable = true, length = 80)
  public String employeeCode;

  @Column(name = "joined_date", nullable = true)
  public LocalDate joinedDate;

  @Column(name = "redacted_at", nullable = true)
  public Instant redactedAt;
}
