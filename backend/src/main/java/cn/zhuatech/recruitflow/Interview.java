// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import jakarta.persistence.*;
import java.time.*;

/** 单名面试官的预约与不可覆写人工反馈；时间区间为左闭右开。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "interview")
public class Interview {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "applicant_id", nullable = false)
  public Long applicantId;

  @Column(name = "interviewer_id", nullable = false)
  public Long interviewerId;

  @Column(name = "title", nullable = false, length = 160)
  public String title;

  @Column(name = "location", nullable = false, length = 300)
  public String location;

  @Column(name = "starts_at", nullable = false)
  public Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  public Instant endsAt;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "recommendation", nullable = false, length = 30)
  public String recommendation;

  @Column(name = "feedback", nullable = false, length = 4000)
  public String feedback;

  @Column(name = "creator_id", nullable = false)
  public Long creatorId;

  @Column(name = "completed_at", nullable = true)
  public Instant completedAt;
}
