// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

/** 版本化录用建议，独立审批后记录外部发出、接受和实际入职。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "recruit_offer")
public class Offer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "applicant_id", nullable = false)
  public Long applicantId;

  @Column(name = "version", nullable = false)
  public int version;

  @Column(name = "salary", nullable = false, precision = 18, scale = 2)
  public BigDecimal salary;

  @Column(name = "currency", nullable = false, length = 3)
  public String currency;

  @Column(name = "pay_period", nullable = false, length = 20)
  public String payPeriod;

  @Column(name = "start_date", nullable = false)
  public LocalDate startDate;

  @Column(name = "expires_at", nullable = false)
  public Instant expiresAt;

  @Column(name = "terms", nullable = false, length = 4000)
  public String terms;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "creator_id", nullable = false)
  public Long creatorId;

  @Column(name = "submitted_by_id", nullable = true)
  public Long submittedById;

  @Column(name = "reviewer_id", nullable = true)
  public Long reviewerId;

  @Column(name = "review_note", nullable = false, length = 1000)
  public String reviewNote;

  @Column(name = "delivery_reference", nullable = false, length = 300)
  public String deliveryReference;

  @Column(name = "response_reference", nullable = false, length = 300)
  public String responseReference;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
