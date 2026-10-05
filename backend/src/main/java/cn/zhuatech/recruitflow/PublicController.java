// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 公开职位及确认资料用途的匿名投递；基础会话和全局限流，不发送通知。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@RestController
@RequestMapping("/api/public")
public class PublicController {
  final RecruitService business;
  final Clock clock;
  long window;
  int count;

  public PublicController(RecruitService business, Clock clock) {
    this.business = business;
    this.clock = clock;
  }

  /** 仅返回已批准职位和部署方配置的说明。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/jobs")
  public Object jobs() {
    return business.publicJobs();
  }

  /**
   * Multipart资料和可选PDF，以当前说明摘要确认，响应不泄漏是否已有同邮箱申请。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。
   */
  @PostMapping("/applications")
  public Object apply(
      @RequestPart("data") RecruitService.PublicInput v,
      @RequestPart(value = "file", required = false) MultipartFile file,
      HttpSession session) {
    limit(session);
    return business.apply(v, file);
  }

  private synchronized void limit(HttpSession session) {
    long minute = clock.millis() / 60000;
    if (window != minute) {
      window = minute;
      count = 0;
    }
    var previous = (Long) session.getAttribute("intakeMinute");
    int n =
        previous != null && previous == minute ? (Integer) session.getAttribute("intakeCount") : 0;
    if (count >= 100 || n >= 5) throw new Problem(429, "RATE_LIMITED");
    count++;
    session.setAttribute("intakeMinute", minute);
    session.setAttribute("intakeCount", n + 1);
  }
}
