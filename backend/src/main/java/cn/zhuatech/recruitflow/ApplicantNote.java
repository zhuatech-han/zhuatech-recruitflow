// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import jakarta.persistence.*;
import java.time.*;

/** 内部跟进备注，面试官只能查看本人面试反馈。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "applicant_note")
public class ApplicantNote {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "applicant_id", nullable = false)
  public Long applicantId;

  @Column(name = "actor_id", nullable = false)
  public Long actorId;

  @Column(name = "note", nullable = false, length = 4000)
  public String note;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
