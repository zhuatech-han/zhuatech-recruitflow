[中文](README.md) | [English](README.en.md)

# ZhiHua RecruitFlow · Recruitment Collaboration and Applicant Tracking

**ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)** · [Official website](https://www.zhuatech.cn/).

Version 1.0.0 uses Java 21, Spring Boot, Vue 3 and MySQL to connect approved openings, applications, interviews, human feedback, independent offer-proposal approval and actual onboarding records. Chinese/English UI supports HR, hiring managers, interviewers and software implementers. Recruitment decisions remain human decisions.

**Public source for learning / non-commercial edition.** Own-source individual learning, technical research and non-commercial exchange are allowed under the existing [LICENSE](LICENSE); prior written company authorization is required for commercial use, paid deployment/customer delivery, SaaS and resale. This is not an OSI open-source license. Third-party components retain their own terms and the source is provided as-is.

## Scenarios and workflow

For teams keeping vacancies, lawful application records, appointments and offer proposals in a private recruitment workspace. Department and assigned-task scope protects information; this is a small/medium-team single-instance system, without distributed/high-volume claims.

1. Administrator creates departments and individual recruiter, hiring-manager and interviewer accounts.
2. Recruiter drafts responsibilities, headcount, location and ownership, then submits to a distinct designated manager. Approved descriptions/headcount/department freeze.
3. Recruiter records lawfully obtained applications with acquisition/consent references, or enables `/careers` for public applications and optional PDF resumes.
4. Screen and schedule interviews; the system checks candidate/interviewer time overlaps.
5. Only the assigned interviewer records human feedback after the appointment ends; completed feedback cannot be overwritten.
6. Recruiter drafts a proposal after attended feedback is complete; the manager independently approves within remaining headcount.
7. Recruiter records delivery/acceptance/decline completed outside the system with actual reference text. The manager records actual start date and unique employee identifier.
8. Ended applications remain in aggregate statistics; authorized privacy staff can anonymize personal fields in the current database.

## Implemented modules and limits

| Module | Implemented behavior |
| --- | --- |
| Workspace | Scoped jobs, applicant stages, proposal progress and upcoming/recent interviews |
| Openings | Draft CRUD, submission, independent approval, return, pause/resume, assignment and closure |
| Applications | Manual/public entry, same-job email duplicate protection, early edits, lawful acquisition records, notes and history |
| PDF resume | MySQL BLOB persistence, 3 MiB limit, header/trailer checks, scoped download and freeze after review begins |
| Interviews | Candidate/interviewer conflict checks, cancellation, assigned feedback and no-show records |
| Proposals | Versions, salary currency/pay period, expiry, terms, independent approval, headcount occupancy/release |
| Onboarding | External response reference, employee code and actual joined date within permitted date bounds |
| Privacy disposal | Ended-application anonymization of contact data, resume, personal text, proposal salary and external references |
| Reports | Eight-stage per-opening funnel and CSV without applicant personal fields |
| Administration | Users/roles, registered permission names, menus, departments, dictionaries, settings and last-admin protection |
| Common | Search/filter/sort, ten-row pages, audit, sessions, CSRF and health |

Not implemented: automatic email/SMS, applicant accounts/progress portal, recruitment-site synchronization, resume parsing/AI ranking, e-signing, background checks, payroll, HRIS synchronization, multitenant SaaS, scheduled expiry/disposal or virus scanning. Delivery/response is evidence registration, not email transmission or contract execution. Expired proposals release capacity only when the explicit expiry action is performed; salary is proposed information, not a payment.

No external key is needed to start. External MySQL and HTTPS use your own configuration/certificates; email, recruitment-site and HR integrations require separate development. Data collection basis, retention and backups require operator decisions; the project offers no legal/security certification or promised candidate outcome.

## Actual running screenshots

These existing current screens use TEST openings, accounts and fictional applicant records in an isolated database. Initial installation creates none of those business fixtures.

| Employee login | Scoped workspace |
| --- | --- |
| ![Login](docs/screenshots/login.jpg) | ![Recruitment workspace](docs/screenshots/workspace.jpg) |
| Applicant workflow details | Public vacancies and intake |
| ![Applicant detail](docs/screenshots/applicant.jpg) | ![Public careers](docs/screenshots/careers.jpg) |
| Account administration | Recruitment funnel |
| ![Accounts](docs/screenshots/accounts.jpg) | ![Funnel reports](docs/screenshots/reports.jpg) |
| Roles and permissions | Parameters and public intake configuration |
| ![Role scopes](docs/screenshots/roles.jpg) | ![Intake settings](docs/screenshots/settings.jpg) |

| Mobile submission confirmation | Assigned interviewer information |
| --- | --- |
| ![Mobile submission](docs/screenshots/mobile.jpg) | ![Interviewer view](docs/screenshots/interviewer.jpg) |

Recruiter/manager/interviewer business views perform execution, independent approval and personal tasks. Administration manages accounts/configuration. Interviewers only receive assigned applicants and their own interview records; internal notes, proposal salaries, other interview feedback and acquisition evidence are excluded. Public pages return approved unpaused vacancies, without applicant records or internal staff directories.

## Environment and first startup

Java **21**, Maven **3.9**, Node.js **24.19.0+**, MySQL **8.4**, Docker Engine/Desktop and Compose v2. Pinned build versions: Spring Boot **4.0.7**, Vue **3.5.40**, Vite **8.1.5**, MariaDB Java Client **3.5.10** connecting to MySQL via `jdbc:mariadb://`.

From the repository root:

```sh
python3 scripts/init-env.py
# Read initial ADMIN_USERNAME/ADMIN_PASSWORD privately in .env; never publish it.
docker compose config --quiet
docker compose up -d --build --wait
```

Employee entry: `http://localhost:8114/`; careers: `http://localhost:8114/careers`; health: `http://localhost:8114/actuator/health`. Initial username defaults to `admin`, with an independently generated `.env` password and no public universal credential. The script writes mode 0600 and refuses existing files. Restart never resets an existing password.

Only first-empty-database initialization creates an administrator, four roles, permissions/menus, recruiting department, dictionaries and parameters. It creates no openings, candidates, salaries or onboarding records. Choose a different private `WEB_PORT` if occupied; leave other projects running. Gateway defaults to localhost, with no MySQL/backend host port mappings.

| Configuration | Purpose |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | Independent strong database-administration password |
| `DATABASE_PASSWORD` | Strong application database password |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | First account and 12–72-byte uppercase/lowercase/digit password |
| `WEB_PORT` / `BIND_ADDRESS` | Shared employee/public entry, default 8114 / 127.0.0.1 |
| `COOKIE_SECURE` | False for local HTTP, true for trusted HTTPS |
| `DATABASE_URL` / `DATABASE_USER` | Optional external MySQL; verify TLS/CA outside the isolated network |

For separate development, start your isolated MySQL and safely inject its reachable URL/account/password plus initial administrator variables. Compose's private `mysql` hostname is not a host-accessible database endpoint.

```sh
docker compose up -d mysql --wait
# Privately configure DATABASE_URL/USER/PASSWORD and ADMIN_USERNAME/PASSWORD.
mvn -f backend/pom.xml spring-boot:run
cd frontend
npm ci --no-audit --no-fund
npm run dev
```

Vite localhost 5173 proxies backend 8080; deployed Nginx uses same-origin `/api` and SPA fallback. Never put credentials or actual applicant records into frontend files.

## Architecture, directories and database initialization

```text
backend/src/main/java/cn/zhuatech/recruitflow/  Accounts/scopes, administration and recruitment transactions
backend/src/main/resources/db/migration/      Versioned Flyway SQL
backend/src/test/                             Rule and HTTP/JPA integration tests
frontend/src/                                Bilingual business views and forms
frontend/public/brand/                       Official logo
frontend/public/third-party/                 Dependency notices
docs/                                        Manual/API/architecture/deployment/security and images
scripts/                                     Initialization, acceptance and release checks
compose.yaml                                 MySQL, Spring Boot and Vue Nginx
```

Spring Security sessions/CSRF and live account/role checks protect reads/writes; JPA persists to MySQL. Flyway `V1__recruit_schema.sql` initializes sixteen application tables plus history, including users/roles, openings, applicants, interviews, proposals, resumes and follow-up/events. Foreign keys protect history; same-job email and employee codes are unique. Resume `MEDIUMBLOB` data and hashes must be included in backups. JPA validates schema instead of automatically rebuilding it.

Writes lock the base department and validate revisions, headcount and schedules atomically. Lists cap at 10,000 authorized records, then use frontend filtering/pagination; this is not server pagination. Hidden menus never bypass API permissions. Add new migrations for upgrades without modifying applied SQL; see [database](docs/database.md) and [architecture](docs/architecture.md).

Openings: `DRAFT → PENDING → OPEN`, with return, pause/resume and closure after all applicants end. Applicants: `NEW → SCREENING → INTERVIEW → OFFER → ACCEPTED → HIRED`; rejection/withdrawal is terminal and cancels pending appointments/proposals. Proposal approval occupies headcount through acceptance and hire; rejection/revocation/decline/explicit expiry releases eligible unused capacity. Proposal creator and approver must differ.

## Public intake and information boundaries

Set deployment-owned `privacyNotice` (at least 50 characters) and `privacyContact` before `publicIntake=true`. Public intake requires reading confirmation and current notice hash; phone/PDF are optional. Same-job email duplicates return uniform acknowledgement without overwriting or disclosing an existing application. Email syntax checks do not verify address ownership; no applicant login, verification code or CAPTCHA is implemented. Limits are five submissions/session/minute and 100/process/minute, rather than large-scale abuse protection.

Manual records require acquisition/authorization evidence. Early NEW/SCREENING records can be edited; interview-stage review freezes profile/resume. Appointments last at most four hours, allow only bounded historical scheduling and future windows, and cap at 20 records. Only assigned interviewers can give feedback after the end; no-show is not attended feedback. A proposal needs attended completed feedback and no pending appointment. Actual onboarding cannot precede the agreed date or exceed current UTC date.

Anonymization of ended applications irreversibly removes personal fields/resume in the current database, retaining job/state/time/operator metadata. Backups, emails and downloads need separate handling. It is not automatic retention management or a compliance certificate. Use the Chinese [manual](docs/manual.md) and [API](docs/api.md) for operational details.

## Deployment, upgrades and backup

Use trusted HTTPS, `COOKIE_SECURE=true`, least-privilege accounts and controlled public/admin/backup access. Configure proxy Host/protocol consistently and overwrite spoofable forwarding headers. External MySQL must use `sslMode=verify-full`, trusted CA and matching identity, rather than the isolated Compose trust setting. Backend/Nginx containers run as non-root.

Back up MySQL consistently, including resumes and Flyway history, to a private mode-0600 directory outside Git. The [deployment guide](docs/deployment.md) contains scoped dump/import commands. Restore first into a different Compose project, unused web port and fresh volume, matching the application/migration version. Start MySQL, import, then start backend/frontend; verify original passwords, role scopes, jobs/applications and resume hashes. New initial-password variables do not replace restored accounts.

Upgrade only after independent recovery/migration validation. `down` retains volumes; `down -v` deletes that project's entire database and is only for explicitly disposable tests. Do not clean other projects or overwrite production during a rehearsal. Default deployment is single-instance without production/high-availability certification.

## Tests and release checks

```sh
mvn -B -f backend/pom.xml spotless:check test package
cd frontend
npm ci --no-audit --no-fund
npm run format:check
npm run lint
npm test
npm run build
cd ..
python3 scripts/release-check.py
git diff --check
docker compose config --quiet
docker compose build
```

Nineteen backend cases (four unit/fifteen HTTP/JPA integration) and five frontend cases cover policy, authorization, schedules, PDF scope, version conflicts, anonymization, CSRF, administrator protection, filtering and local-time/UTC handling. Docker Maven builds run full tests. H2 does not prove MySQL deployment. The [testing guide](docs/testing.md) requires separate actual database, browser, restart and independent recovery acceptance.

`scripts/smoke-test.py --base http://127.0.0.1:8114` rejects existing business data, creates only TEST/example.invalid fixtures and private random test credentials, and must run exclusively on a fresh disposable instance. Set `--env /path/to/private-test.env --output /path/to/private-test/state.json` for matching private configuration and generated fixture/state storage; these paths are placeholders to replace. Never target real candidate records. Remove only its own disposable resources afterward.

## Troubleshooting, security and feedback

For unreachable pages check unused port and three healthy services. For startup failure inspect scoped MySQL/migration/strong-password diagnostics rather than deleting existing volumes. For missing approvals check designation, role/department and independent submitter. Resolve overlapping appointments by deliberate cancellation/rescheduling. Reload and review stale versions; do not blindly replay. Public vacancies require configured notice/contact, enabled intake and an approved OPEN job.

BCrypt cost 12, 12–72-byte passwords, HttpOnly/SameSite Strict 30-minute cookies, login-session rotation, CSRF including anonymous applications, bounded username/IP login limiting, live account changes and active-task/last-admin protection are implemented. PDF checks cover 3 MiB size and header/trailer, not full parsing or malware scanning; downloads are scoped attachments with nosniff/no-cache. Audit/aggregate CSV does not intentionally include personal resume contents; formula prefixes are neutralized.

The operator determines lawful collection, retention, backup disposal and hiring decisions. No actual email, signing, payroll, automated ranking or candidate eligibility outcome is performed by this source. Issues/contributions should include version, safe reproduction and small verified changes; preserve attribution and compatible third-party terms. Report vulnerabilities privately without actual resumes, cookies, database secrets or exploit payloads. See [security](docs/security.md) and [third-party declarations](docs/third-party.md).

## Contact ZhiHua Technology

**ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)**. Commercial authorization, customization, private deployment and system integration:

- Website: [https://www.zhuatech.cn/](https://www.zhuatech.cn/)
- Email: [han@zhuatech.cn](mailto:han@zhuatech.cn)
- Email: [jack@zhuatech.cn](mailto:jack@zhuatech.cn)
- WhatsApp: [+86 17521234993](https://wa.me/8617521234993)
