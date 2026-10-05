// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** 口令、字节上限和附件说明摘要的边界检查。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
class RecruitPolicyTest {
  @Test
  void weakPasswordsAreRejected() {
    for (var s : new String[] {"", "password", "aaaaaaaaaaaa1", "AAAAAAAAAAAA1", "Aa123"})
      assertThrows(Problem.class, () -> AdminService.validatePassword(s));
  }

  @Test
  void unicodePasswordsRespectBcryptByteLimit() {
    assertThrows(Problem.class, () -> AdminService.validatePassword("Aa9" + "中".repeat(24)));
    assertDoesNotThrow(() -> AdminService.validatePassword("Aa9" + "中".repeat(20)));
  }

  @Test
  void requiredTextRejectsBlankAndOverflow() {
    assertThrows(Problem.class, () -> AdminService.text(" ", 10));
    assertThrows(Problem.class, () -> AdminService.text("x".repeat(11), 10));
    assertEquals("A", AdminService.text(" A ", 10));
  }

  @Test
  void sha256IsStableAndSensitiveToContent() {
    var bytes = "TEST privacy notice".getBytes(StandardCharsets.UTF_8);
    assertEquals(64, RecruitService.digest(bytes).length());
    assertEquals(RecruitService.digest(bytes), RecruitService.digest(bytes));
    assertNotEquals(
        RecruitService.digest(bytes),
        RecruitService.digest("Changed".getBytes(StandardCharsets.UTF_8)));
  }
}
