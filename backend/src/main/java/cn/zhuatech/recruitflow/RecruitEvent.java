// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import jakarta.persistence.*;
import java.time.*;

/** 岗位与申请状态事件；与业务同事务保存，匿名化后保留非个人元数据。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "recruit_event")
public class RecruitEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "job_id", nullable = false)
  public Long jobId;

  @Column(name = "applicant_id", nullable = true)
  public Long applicantId;

  @Column(name = "action", nullable = false, length = 60)
  public String action;

  @Column(name = "actor", nullable = false, length = 60)
  public String actor;

  @Column(name = "note", nullable = false, length = 1000)
  public String note;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
