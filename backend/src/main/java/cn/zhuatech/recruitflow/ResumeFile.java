// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.*;

/** 受申请访问权限保护的简历原文件，二进制不进入JSON响应。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "resume_file")
public class ResumeFile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "applicant_id", nullable = false)
  public Long applicantId;

  @Column(name = "filename", nullable = false, length = 160)
  public String filename;

  @Column(name = "digest", nullable = false, length = 64)
  public String digest;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @JsonIgnore
  @Lob
  @Column(name = "content", nullable = false, columnDefinition = "MEDIUMBLOB", length = 3145728)
  public byte[] content;
}
