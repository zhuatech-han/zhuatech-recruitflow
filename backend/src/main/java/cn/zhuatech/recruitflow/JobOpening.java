// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import jakarta.persistence.*;
import java.time.*;

/** 招聘岗位及已批准的公开描述，冻结归属和目标人数。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "job_opening")
public class JobOpening {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "code", nullable = false, length = 60)
  public String code;

  @Column(name = "title", nullable = false, length = 160)
  public String title;

  @Column(name = "location", nullable = false, length = 160)
  public String location;

  @Column(name = "employment_type", nullable = false, length = 60)
  public String employmentType;

  @Column(name = "description", nullable = false, length = 6000)
  public String description;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "owner_id", nullable = false)
  public Long ownerId;

  @Column(name = "manager_id", nullable = false)
  public Long managerId;

  @Column(name = "submitted_by_id")
  public Long submittedById;

  @Column(name = "headcount", nullable = false)
  public int headcount;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "revision", nullable = false)
  public long revision;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "published_at", nullable = true)
  public Instant publishedAt;
}
