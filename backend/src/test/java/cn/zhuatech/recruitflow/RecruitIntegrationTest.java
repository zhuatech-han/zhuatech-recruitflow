// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.recruitflow;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 实际HTTP/JPA招聘闭环与越权、陈旧版本、名额、预约和资料处置回归。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@SpringBootTest
@AutoConfigureMockMvc
class RecruitIntegrationTest {
  static final String PASSWORD = "Aa9" + UUID.randomUUID();

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add(
        "spring.datasource.url",
        () -> "jdbc:h2:mem:recruit;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    r.add("spring.datasource.username", () -> "sa");
    r.add("spring.datasource.password", () -> "");
    r.add("recruitflow.admin-password", () -> PASSWORD);
  }

  @Autowired MockMvc mvc;
  final JsonMapper json = JsonMapper.builder().build();
  MockHttpSession admin, recruiter, manager, interviewer, other;
  long jid, rid, mid, iid;
  String suffix;

  @BeforeEach
  void setup() throws Exception {
    suffix = UUID.randomUUID().toString().substring(0, 8);
    admin = login("admin");
    var roles = getAs(admin, "/admin/roles", 200);
    long rr = role(roles, "Recruiter"),
        mr = role(roles, "Hiring manager"),
        ir = role(roles, "Interviewer");
    rid = user("hr" + suffix, rr, 1);
    mid = user("mgr" + suffix, mr, 1);
    iid = user("iv" + suffix, ir, 1);
    user("other" + suffix, ir, 1);
    recruiter = login("hr" + suffix);
    manager = login("mgr" + suffix);
    interviewer = login("iv" + suffix);
    other = login("other" + suffix);
    jid =
        postAs(
                recruiter,
                "/jobs",
                m(
                    "title",
                    "TEST engineer",
                    "location",
                    "TEST office",
                    "employmentType",
                    "FULL_TIME",
                    "description",
                    "TEST responsibilities",
                    "departmentId",
                    1,
                    "ownerId",
                    rid,
                    "managerId",
                    mid,
                    "headcount",
                    1),
                200)
            .get("id")
            .asLong();
    jobAction(recruiter, "submit", 200);
    jobAction(manager, "approve", 200);
  }

  long role(JsonNode rows, String name) {
    for (var r : rows) if (r.get("name").asString().contains(name)) return r.get("id").asLong();
    throw new AssertionError(name);
  }

  Map<String, Object> m(Object... pairs) {
    var result = new LinkedHashMap<String, Object>();
    for (int n = 0; n < pairs.length; n += 2) result.put(pairs[n].toString(), pairs[n + 1]);
    return result;
  }

  JsonNode call(MockHttpSession session, String path, String method, Object body, int expected)
      throws Exception {
    var b =
        switch (method) {
          case "POST" -> post("/api" + path);
          case "PUT" -> put("/api" + path);
          case "DELETE" -> delete("/api" + path);
          default -> get("/api" + path);
        };
    b.session(session);
    if (!method.equals("GET")) b.with(csrf());
    if (body != null) b.contentType("application/json").content(json.writeValueAsString(body));
    var response = mvc.perform(b).andReturn().getResponse();
    assertEquals(expected, response.getStatus(), method + path + response.getContentAsString());
    return response.getContentAsString().isEmpty()
        ? json.readTree("{}")
        : json.readTree(response.getContentAsString());
  }

  JsonNode postAs(MockHttpSession s, String path, Object data, int code) throws Exception {
    return call(s, path, "POST", data, code);
  }

  JsonNode getAs(MockHttpSession s, String path, int code) throws Exception {
    return call(s, path, "GET", null, code);
  }

  MockHttpSession login(String username) throws Exception {
    var result =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(m("username", username, "password", PASSWORD))))
            .andReturn();
    assertEquals(200, result.getResponse().getStatus());
    return (MockHttpSession) result.getRequest().getSession(false);
  }

  long user(String username, long role, long dept) throws Exception {
    return postAs(
            admin,
            "/admin/users",
            m(
                "username",
                username,
                "displayName",
                "TEST " + username,
                "password",
                PASSWORD,
                "roleId",
                role,
                "departmentId",
                dept,
                "enabled",
                true),
            200)
        .get("id")
        .asLong();
  }

  long jobVersion() throws Exception {
    for (var j : getAs(recruiter, "/jobs", 200))
      if (j.get("id").asLong() == jid) return j.get("revision").asLong();
    throw new AssertionError();
  }

  void jobAction(MockHttpSession s, String action, int code) throws Exception {
    postAs(
        s,
        "/jobs/" + jid + "/actions/" + action,
        m("revision", jobVersion(), "note", "TEST job action"),
        code);
  }

  long applicant() throws Exception {
    return postAs(
            recruiter,
            "/applicants",
            m(
                "jobId",
                jid,
                "name",
                "TEST candidate",
                "email",
                UUID.randomUUID() + "@example.invalid",
                "phone",
                "",
                "introduction",
                "TEST experience",
                "source",
                "MANUAL",
                "consentReference",
                "TEST permission reference"),
            200)
        .get("id")
        .asLong();
  }

  long version(long aid) throws Exception {
    return getAs(recruiter, "/applicants/" + aid, 200).get("applicant").get("revision").asLong();
  }

  JsonNode command(MockHttpSession s, long aid, String action, int code) throws Exception {
    return postAs(
        s,
        "/applicants/" + aid + "/actions/" + action,
        m("revision", version(aid), "note", "TEST action"),
        code);
  }

  long interview(long aid, boolean past) throws Exception {
    command(recruiter, aid, "screen", 200);
    var start = Instant.now().plusSeconds(past ? -1200 : 3600);
    return postAs(
            recruiter,
            "/applicants/" + aid + "/interviews",
            m(
                "revision",
                version(aid),
                "interviewerId",
                iid,
                "title",
                "TEST technical interview",
                "location",
                "TEST office",
                "startsAt",
                start.toString(),
                "endsAt",
                start.plusSeconds(600).toString()),
            200)
        .get("id")
        .asLong();
  }

  long offer(long aid) throws Exception {
    long id = interview(aid, true);
    postAs(
        interviewer,
        "/interviews/" + id + "/feedback",
        m(
            "revision",
            version(aid),
            "recommendation",
            "POSITIVE",
            "feedback",
            "TEST human feedback"),
        200);
    return postAs(
            recruiter,
            "/applicants/" + aid + "/offers",
            m(
                "revision",
                version(aid),
                "salary",
                "10000.00",
                "payPeriod",
                "MONTHLY",
                "startDate",
                LocalDate.now(ZoneOffset.UTC).toString(),
                "expiresAt",
                Instant.now().plusSeconds(3600).toString(),
                "terms",
                "TEST proposal terms"),
            200)
        .get("id")
        .asLong();
  }

  void offerAct(MockHttpSession s, long aid, long oid, String action, int code) throws Exception {
    postAs(
        s,
        "/offers/" + oid + "/actions/" + action,
        m("revision", version(aid), "note", "TEST external reference"),
        code);
  }

  @Test
  void completeHiringFlow() throws Exception {
    long aid = applicant(), oid = offer(aid);
    offerAct(recruiter, aid, oid, "submit", 200);
    offerAct(manager, aid, oid, "approve", 200);
    offerAct(recruiter, aid, oid, "issue", 200);
    offerAct(recruiter, aid, oid, "accept", 200);
    var a =
        postAs(
            manager,
            "/applicants/" + aid + "/actions/hire",
            m(
                "revision",
                version(aid),
                "note",
                "TEST actual joining",
                "employeeCode",
                "TEST-" + suffix,
                "joinedDate",
                LocalDate.now(ZoneOffset.UTC).toString()),
            200);
    assertEquals("HIRED", a.get("stage").asString());
    jobAction(manager, "close", 200);
    assertTrue(getAs(manager, "/reports", 200).toString().contains("HIRED"));
  }

  @Test
  void unassignedInterviewersCannotSeeOrMutateApplicants() throws Exception {
    long aid = applicant();
    getAs(other, "/applicants/" + aid, 403);
    command(other, aid, "screen", 403);
    getAs(other, "/admin/users", 403);
    getAs(other, "/reports", 403);
  }

  @Test
  void onlyActualInterviewerCanSubmitAndNotBeforeEnd() throws Exception {
    long aid = applicant(), id = interview(aid, false);
    postAs(
        interviewer,
        "/interviews/" + id + "/feedback",
        m("revision", version(aid), "recommendation", "NEUTRAL", "feedback", "TEST"),
        409);
    postAs(
        admin,
        "/interviews/" + id + "/feedback",
        m("revision", version(aid), "recommendation", "NEUTRAL", "feedback", "TEST"),
        403);
    assertEquals(0, getAs(interviewer, "/applicants/" + aid, 200).get("offers").size());
  }

  @Test
  void repeatedCommandsCannotAdvanceWithStaleVersion() throws Exception {
    long aid = applicant(), v = version(aid);
    command(recruiter, aid, "screen", 200);
    postAs(
        recruiter,
        "/applicants/" + aid + "/actions/withdraw",
        m("revision", v, "note", "TEST"),
        409);
  }

  @Test
  void calendarConflictsAreRejectedAndCancellationReleasesSlot() throws Exception {
    long aid = applicant(), id = interview(aid, false), a2 = applicant();
    command(recruiter, a2, "screen", 200);
    var i = getAs(recruiter, "/interviews", 200);
    JsonNode first = null;
    for (var x : i) if (x.get("id").asLong() == id) first = x;
    var data =
        m(
            "revision",
            version(a2),
            "interviewerId",
            iid,
            "title",
            "TEST",
            "location",
            "TEST",
            "startsAt",
            first.get("startsAt").asString(),
            "endsAt",
            first.get("endsAt").asString());
    postAs(recruiter, "/applicants/" + a2 + "/interviews", data, 409);
    postAs(
        recruiter,
        "/interviews/" + id + "/cancel",
        m("revision", version(aid), "note", "TEST cancellation"),
        200);
    postAs(recruiter, "/applicants/" + a2 + "/interviews", data, 200);
  }

  @Test
  void interviewIsRequiredBeforeOfferAndApplicationBlocksClosing() throws Exception {
    long aid = applicant();
    postAs(
        recruiter,
        "/applicants/" + aid + "/offers",
        m(
            "revision",
            version(aid),
            "salary",
            "100",
            "payPeriod",
            "MONTHLY",
            "startDate",
            LocalDate.now(ZoneOffset.UTC).toString(),
            "expiresAt",
            Instant.now().plusSeconds(1000).toString(),
            "terms",
            "TEST"),
        409);
    jobAction(manager, "close", 409);
  }

  @Test
  void pdfIsScopedAndDownloadedByApplicantId() throws Exception {
    long aid = applicant();
    var f =
        new MockMultipartFile(
            "file",
            "../../TEST.pdf",
            "application/pdf",
            "%PDF-1.7\nTEST fixture\n%%EOF".getBytes());
    var response =
        mvc.perform(
                multipart("/api/applicants/" + aid + "/resume")
                    .file(f)
                    .param("revision", "" + version(aid))
                    .session(recruiter)
                    .with(csrf()))
            .andReturn()
            .getResponse();
    assertEquals(200, response.getStatus());
    var pdf =
        mvc.perform(get("/api/applicants/" + aid + "/resume").session(recruiter))
            .andReturn()
            .getResponse();
    assertEquals(200, pdf.getStatus());
    assertEquals(
        "attachment; filename=resume-" + aid + ".pdf", pdf.getHeader("Content-Disposition"));
    assertArrayEquals(f.getBytes(), pdf.getContentAsByteArray());
    getAs(other, "/applicants/" + aid + "/resume", 403);
  }

  @Test
  void invalidResumeDoesNotUpdateRevision() throws Exception {
    long aid = applicant(), v = version(aid);
    var f =
        new MockMultipartFile(
            "file", "TEST.html", "application/pdf", "<script>bad</script>".getBytes());
    assertEquals(
        400,
        mvc.perform(
                multipart("/api/applicants/" + aid + "/resume")
                    .file(f)
                    .param("revision", "" + v)
                    .session(recruiter)
                    .with(csrf()))
            .andReturn()
            .getResponse()
            .getStatus());
    assertEquals(v, version(aid));
  }

  @Test
  void terminatedApplicationCanBeRedactedOnlyByPrivacyRole() throws Exception {
    long aid = applicant();
    command(recruiter, aid, "withdraw", 200);
    postAs(
        recruiter,
        "/applicants/" + aid + "/redact",
        m("revision", version(aid), "note", "TEST privacy instruction"),
        403);
    postAs(
        manager,
        "/applicants/" + aid + "/redact",
        m("revision", version(aid), "note", "TEST privacy instruction"),
        200);
    var a = getAs(recruiter, "/applicants/" + aid, 200).get("applicant");
    assertTrue(a.get("email").asString().endsWith("@example.invalid"));
    assertEquals("", a.get("introduction").asString());
    assertFalse(a.get("redactedAt").isNull());
  }

  @Test
  void departmentScopeIsEnforced() throws Exception {
    var dept =
        postAs(admin, "/admin/departments", m("name", "TEST other dept"), 200).get("id").asLong();
    long rr = role(getAs(admin, "/admin/roles", 200), "Recruiter");
    user("dept" + suffix, rr, dept);
    var session = login("dept" + suffix);
    long aid = applicant();
    getAs(session, "/applicants/" + aid, 403);
    for (var row : getAs(session, "/jobs", 200)) assertNotEquals(jid, row.get("id").asLong());
  }

  @Test
  void csrfAndLastAdminProtection() throws Exception {
    assertEquals(
        403,
        mvc.perform(post("/api/jobs").session(admin).contentType("application/json").content("{}"))
            .andReturn()
            .getResponse()
            .getStatus());
    call(admin, "/admin/users/1", "DELETE", null, 409);
  }

  @Test
  void publicIntakeRequiresConfigurationAndIsEmptyByDefault() throws Exception {
    var r = mvc.perform(get("/api/public/jobs")).andReturn().getResponse();
    assertEquals(200, r.getStatus());
    assertFalse(json.readTree(r.getContentAsString()).get("enabled").asBoolean());
    for (var s : getAs(admin, "/admin/settings", 200))
      if (s.get("code").asString().equals("publicIntake"))
        call(admin, "/admin/settings/" + s.get("id").asLong(), "PUT", m("value", "true"), 400);
  }

  @Test
  void simultaneousOfferApprovalsCannotOverbook() throws Exception {
    long a1 = applicant(), o1 = offer(a1);
    offerAct(recruiter, a1, o1, "submit", 200);
    iid = user("ivtwo" + suffix, role(getAs(admin, "/admin/roles", 200), "Interviewer"), 1);
    interviewer = login("ivtwo" + suffix);
    long a2 = applicant(), o2 = offer(a2);
    offerAct(recruiter, a2, o2, "submit", 200);
    var body1 =
        json.writeValueAsString(m("revision", version(a1), "note", "TEST concurrent approval"));
    var body2 =
        json.writeValueAsString(m("revision", version(a2), "note", "TEST concurrent approval"));
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var tasks =
          List.<java.util.concurrent.Callable<Integer>>of(
              () ->
                  mvc.perform(
                          post("/api/offers/" + o1 + "/actions/approve")
                              .session(manager)
                              .with(csrf())
                              .contentType("application/json")
                              .content(body1))
                      .andReturn()
                      .getResponse()
                      .getStatus(),
              () ->
                  mvc.perform(
                          post("/api/offers/" + o2 + "/actions/approve")
                              .session(manager)
                              .with(csrf())
                              .contentType("application/json")
                              .content(body2))
                      .andReturn()
                      .getResponse()
                      .getStatus());
      var codes = new ArrayList<Integer>();
      for (var f : pool.invokeAll(tasks)) codes.add(f.get());
      Collections.sort(codes);
      assertEquals(List.of(200, 409), codes);
    }
  }

  @Test
  void jobSubmitterCannotApproveUsingAllScope() throws Exception {
    var draft =
        postAs(
            recruiter,
            "/jobs",
            m(
                "title",
                "TEST self review",
                "location",
                "TEST",
                "employmentType",
                "FULL_TIME",
                "description",
                "TEST requirements",
                "departmentId",
                1,
                "ownerId",
                rid,
                "managerId",
                mid,
                "headcount",
                1),
            200);
    long id = draft.get("id").asLong();
    var pending =
        postAs(
            admin,
            "/jobs/" + id + "/actions/submit",
            m("revision", draft.get("revision").asLong(), "note", "TEST admin submits"),
            200);
    postAs(
        admin,
        "/jobs/" + id + "/actions/approve",
        m("revision", pending.get("revision").asLong(), "note", "TEST self review"),
        403);
  }

  @Test
  @org.springframework.transaction.annotation.Transactional
  void publicApplicationCanBeCorrectedWithoutChangingConsent() throws Exception {
    var settings = getAs(admin, "/admin/settings", 200);
    for (var row : settings) {
      String code = row.get("code").asString();
      if (code.equals("privacyNotice") || code.equals("privacyContact"))
        call(
            admin,
            "/admin/settings/" + row.get("id").asLong(),
            "PUT",
            m(
                "value",
                code.equals("privacyNotice")
                    ? "TEST synthetic applicant privacy notice for workflow verification only. No real personal data is processed."
                    : "TEST local administrator"),
            200);
    }
    for (var row : settings)
      if (row.get("code").asString().equals("publicIntake"))
        call(admin, "/admin/settings/" + row.get("id").asLong(), "PUT", m("value", "true"), 200);
    var config = getAs(new MockHttpSession(), "/public/jobs", 200);
    String email = "public" + suffix + "@example.invalid";
    var data =
        new MockMultipartFile(
            "data",
            "data.json",
            "application/json",
            json.writeValueAsBytes(
                m(
                    "jobId",
                    jid,
                    "name",
                    "TEST Public",
                    "email",
                    email,
                    "phone",
                    "",
                    "introduction",
                    "TEST public experience",
                    "consent",
                    true,
                    "noticeHash",
                    config.get("noticeHash").asString())));
    assertEquals(
        200,
        mvc.perform(multipart("/api/public/applications").file(data).with(csrf()))
            .andReturn()
            .getResponse()
            .getStatus());
    JsonNode a = null;
    for (var x : getAs(recruiter, "/applicants", 200))
      if (x.get("email").asString().equals(email)) a = x;
    assertNotNull(a);
    var body =
        m(
            "revision",
            a.get("revision").asLong(),
            "jobId",
            jid,
            "name",
            "TEST Public corrected",
            "email",
            email,
            "phone",
            "",
            "introduction",
            "TEST corrected experience",
            "source",
            "PUBLIC",
            "consentReference",
            a.get("consentReference").asString());
    var changed = call(recruiter, "/applicants/" + a.get("id").asLong(), "PUT", body, 200);
    assertEquals("PUBLIC", changed.get("source").asString());
    assertEquals(a.get("consentHash").asString(), changed.get("consentHash").asString());
    body.put("revision", changed.get("revision").asLong());
    body.put("source", "MANUAL");
    call(recruiter, "/applicants/" + a.get("id").asLong(), "PUT", body, 400);
  }
}
