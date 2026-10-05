#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""独立空库HTTP业务验收，只生成TEST资料；拒绝在已有业务数据库运行。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
import argparse, json, urllib.request, urllib.error, http.cookiejar, secrets, datetime, hashlib, concurrent.futures, os
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--base',default='http://127.0.0.1:8114');p.add_argument('--env',default='.env');p.add_argument('--output',default='output/recruitflow-quality-state.json');args=p.parse_args()
env=dict(line.split('=',1) for line in Path(args.env).read_text().splitlines() if '=' in line and not line.startswith('#'))
checks=0

def check(condition,label):
    global checks
    assert condition,label
    checks+=1

class Client:
    def __init__(self):self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()));self.csrf=None
    def request(self,path,method='GET',data=None,expect=200,raw=False,csrf=True,content=None):
        if method!='GET' and csrf and self.csrf is None:self.csrf=self.request('/api/auth/csrf')
        headers={}
        if method!='GET' and csrf:headers[self.csrf['header']]=self.csrf['token']
        if content:body=data;headers['Content-Type']=content
        elif data is not None:body=json.dumps(data).encode();headers['Content-Type']='application/json'
        else:body=None
        req=urllib.request.Request(args.base+path,data=body,method=method,headers=headers)
        try:r=self.opener.open(req,timeout=30)
        except urllib.error.HTTPError as e:r=e
        b=r.read();check(r.status==expect,f'{method} {path}: expected {expect}, actual {r.status}; {b[:400] if r.status!=expect else ""}')
        return (b,dict(r.headers)) if raw else json.loads(b or '{}')
    def login(self,user,pwd):self.request('/api/auth/login','POST',{'username':user,'password':pwd});self.csrf=None;return self
    def multi(self,path,data,file=None,expect=200):
        boundary='----Zhua'+secrets.token_hex(12);parts=[]
        if data is not None:parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="data"\r\nContent-Type: application/json\r\n\r\n'.encode()+json.dumps(data).encode()+b'\r\n')
        if file is not None:parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="TEST.pdf"\r\nContent-Type: application/pdf\r\n\r\n'.encode()+file+b'\r\n')
        parts.append(f'--{boundary}--\r\n'.encode());return self.request(path,'POST',b''.join(parts),expect,content='multipart/form-data; boundary='+boundary)

admin=Client().login(env.get('ADMIN_USERNAME','admin'),env['ADMIN_PASSWORD'])
check(admin.request('/actuator/health')['status']=='UP','health')
check(admin.request('/api/jobs')==[] and admin.request('/api/applicants')==[],'fresh business database required')
check(not Client().request('/api/public/jobs')['enabled'],'public intake initially closed')
roles=admin.request('/api/admin/roles');pwd='Aa9'+secrets.token_hex(20);suffix=secrets.token_hex(3)
def role(name):return next(x['id'] for x in roles if name in x['name'])
users={};sessions={}
for key,roleName in [('hr','Recruiter'),('manager','Hiring manager'),('interviewer','Interviewer'),('other','Interviewer')]:
    user=admin.request('/api/admin/users','POST',{'username':key+suffix,'displayName':'TEST '+key,'password':pwd,'roleId':role(roleName),'departmentId':1,'enabled':True});users[key]=user['id'];sessions[key]=Client().login(key+suffix,pwd)
hr=sessions['hr'];manager=sessions['manager'];iv=sessions['interviewer'];other=sessions['other']
check('passwordHash' not in json.dumps(admin.request('/api/admin/users')),'password hash is not exposed')
other.request('/api/admin/users',expect=403);Client().request('/api/jobs',expect=401)
admin.request('/api/jobs','POST',{},403,csrf=False)
admin.request('/api/admin/users/1','DELETE',expect=409)
settings=admin.request('/api/admin/settings');settingIds={s['code']:s['id'] for s in settings}
admin.request('/api/admin/settings/'+str(settingIds['publicIntake']),'PUT',{'value':'true'},400)
notice='TEST environment only. Application data is used solely to verify recruitment workflows. No real applicants, actual hiring decisions, emails or employment contracts are processed. Contact the local test administrator for removal.'
for code,value in [('companyName','TEST Recruitment Team'),('privacyNotice',notice),('privacyContact','TEST local administrator'),('publicIntake','true')]:admin.request('/api/admin/settings/'+str(settingIds[code]),'PUT',{'value':value})

def newJob(title,heads=1):
    return hr.request('/api/jobs','POST',{'title':title,'location':'TEST Shanghai / Remote','employmentType':'FULL_TIME','description':'TEST role only. Coordinate project delivery, confirm requirements and document milestones. This is a functional verification record, not an actual advertised vacancy.','departmentId':1,'ownerId':users['hr'],'managerId':users['manager'],'headcount':heads})
def jget(jid):return next(x for x in hr.request('/api/jobs') if x['id']==jid)
def jact(c,jid,action,expect=200,**extra):return c.request(f'/api/jobs/{jid}/actions/{action}','POST',{'revision':jget(jid)['revision'],'note':'TEST job workflow',**extra},expect)
def anew(jid,name):return hr.request('/api/applicants','POST',{'jobId':jid,'name':'TEST '+name,'email':name.lower()+suffix+'@example.invalid','phone':'','introduction':'TEST experience for functional verification; no real personal record.','source':'MANUAL','consentReference':'TEST consent record '+name})
def detail(aid):return hr.request(f'/api/applicants/{aid}')
def rev(aid):return detail(aid)['applicant']['revision']
def aact(c,aid,action,expect=200,**extra):return c.request(f'/api/applicants/{aid}/actions/{action}','POST',{'revision':rev(aid),'note':'TEST external evidence '+action,**extra},expect)
def interview(aid,offset=-1200):
    aact(hr,aid,'screen');start=datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(seconds=offset)
    return hr.request(f'/api/applicants/{aid}/interviews','POST',{'revision':rev(aid),'interviewerId':users['interviewer'],'title':'TEST delivery skills review','location':'TEST meeting room','startsAt':start.isoformat(),'endsAt':(start+datetime.timedelta(seconds=120)).isoformat()})
def offer(aid,offset):
    i=interview(aid,offset);iv.request(f'/api/interviews/{i["id"]}/feedback','POST',{'revision':rev(aid),'recommendation':'POSITIVE','feedback':'TEST feedback: workflow verified, human review recorded. No real candidate evaluation.'})
    return hr.request(f'/api/applicants/{aid}/offers','POST',{'revision':rev(aid),'salary':'12000.00','payPeriod':'MONTHLY','startDate':datetime.datetime.now(datetime.timezone.utc).date().isoformat(),'expiresAt':(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=7)).isoformat(),'terms':'TEST proposed role and monthly compensation, not a binding employment offer.'})
def oact(c,aid,oid,action,expect=200):return c.request(f'/api/offers/{oid}/actions/{action}','POST',{'revision':rev(aid),'note':'TEST external evidence '+action},expect)

job=newJob('TEST 项目交付工程师 / Delivery engineer',1);jid=job['id'];jact(hr,jid,'submit');jact(hr,jid,'approve',403);jact(manager,jid,'approve')
a=anew(jid,'Alex');aid=a['id'];other.request(f'/api/applicants/{aid}',expect=403)
# Valid minimal one-page PDF, including xref and trailer; generated only from synthetic TEST text.
def fixture():
    objects=[b'<< /Type /Catalog /Pages 2 0 R >>',b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>',b'<< /Length 55 >>\nstream\nBT /F1 14 Tf 50 790 Td (TEST RecruitFlow resume) Tj ET\nendstream',b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>'];body=b'%PDF-1.4\n';offsets=[0]
    for n,obj in enumerate(objects,1):offsets.append(len(body));body+=f'{n} 0 obj\n'.encode()+obj+b'\nendobj\n'
    xref=len(body);body+=b'xref\n0 6\n0000000000 65535 f \n'+b''.join(f'{o:010} 00000 n \n'.encode() for o in offsets[1:])+f'trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n'.encode();return body
pdf=fixture();out=Path(args.output).parent;out.mkdir(parents=True,exist_ok=True);(out/'TEST-resume.pdf').write_bytes(pdf)
hr.multi(f'/api/applicants/{aid}/resume?revision={rev(aid)}',None,b'<html>invalid</html>',400)
hr.multi(f'/api/applicants/{aid}/resume?revision={rev(aid)}',None,pdf)
b,h=hr.request(f'/api/applicants/{aid}/resume',raw=True);check(b==pdf and h.get('X-Content-Type-Options')=='nosniff','PDF attachment persistence and nosniff');other.request(f'/api/applicants/{aid}/resume',expect=403)
old=rev(aid);aact(hr,aid,'screen');hr.request(f'/api/applicants/{aid}/actions/withdraw','POST',{'revision':old,'note':'TEST stale'},409)
# This application is already screened; complete manually without screening twice.
start=datetime.datetime.now(datetime.timezone.utc)-datetime.timedelta(seconds=1500)
i=hr.request(f'/api/applicants/{aid}/interviews','POST',{'revision':rev(aid),'interviewerId':users['interviewer'],'title':'TEST project delivery interview','location':'TEST meeting room','startsAt':start.isoformat(),'endsAt':(start+datetime.timedelta(seconds=120)).isoformat()})
view=iv.request(f'/api/applicants/{aid}');check(view['offers']==[] and view['notes']==[] and 'consentReference' not in view['applicant'],'interviewer least-privilege fields')
admin.request(f'/api/interviews/{i["id"]}/feedback','POST',{'revision':rev(aid),'recommendation':'POSITIVE','feedback':'TEST'},403)
iv.request(f'/api/interviews/{i["id"]}/feedback','POST',{'revision':rev(aid),'recommendation':'POSITIVE','feedback':'TEST completed human interview feedback'})
iv.request(f'/api/interviews/{i["id"]}/feedback','POST',{'revision':rev(aid),'recommendation':'POSITIVE','feedback':'TEST duplicate'},409)
hr.multi(f'/api/applicants/{aid}/resume?revision={rev(aid)}',None,pdf,409)
o=hr.request(f'/api/applicants/{aid}/offers','POST',{'revision':rev(aid),'salary':'12000.00','payPeriod':'MONTHLY','startDate':datetime.datetime.now(datetime.timezone.utc).date().isoformat(),'expiresAt':(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=7)).isoformat(),'terms':'TEST proposal, no actual employment offer'});oid=o['id'];oact(hr,aid,oid,'submit');oact(hr,aid,oid,'approve',403);oact(manager,aid,oid,'approve')
bapp=anew(jid,'Blair');bid=bapp['id'];bo=offer(bid,-1200);oact(hr,bid,bo['id'],'submit');oact(manager,bid,bo['id'],'approve',409)
oact(manager,aid,oid,'revoke');oact(manager,bid,bo['id'],'approve');oact(manager,bid,bo['id'],'revoke')
# New version after revocation; cannot reuse published version.
o2=hr.request(f'/api/applicants/{aid}/offers','POST',{'revision':rev(aid),'salary':'12500.00','payPeriod':'MONTHLY','startDate':datetime.datetime.now(datetime.timezone.utc).date().isoformat(),'expiresAt':(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=7)).isoformat(),'terms':'TEST revised proposal'});check(o2['version']==2,'offer version history');oid=o2['id'];oact(hr,aid,oid,'submit');oact(manager,aid,oid,'approve');oact(hr,aid,oid,'issue');oact(hr,aid,oid,'accept')
aact(manager,aid,'hire',employeeCode='TEST-'+suffix,joinedDate=datetime.datetime.now(datetime.timezone.utc).date().isoformat());jact(manager,jid,'close',409);aact(hr,bid,'withdraw');jact(manager,jid,'close')
# Closed app cannot resume; redaction removes files and all personal content.
aact(hr,bid,'screen',409);manager.request(f'/api/applicants/{bid}/redact','POST',{'revision':rev(bid),'note':'TEST authorized cleanup'});check(detail(bid)['applicant']['introduction']=='','redaction contents removed')
admin.request('/api/admin/settings/'+str(settingIds['currency']),'PUT',{'value':'USD'},409)
report=manager.request('/api/reports');row=next(x for x in report if x['jobId']==jid);check(row['stages']['HIRED']==1 and row['stages']['WITHDRAWN']==1,'funnel totals correct')
csv,headers=manager.request('/api/reports.csv',raw=True);check(csv.startswith(b'\xef\xbb\xbf') and b'Alex' not in csv and headers.get('Content-Disposition','').endswith('recruitflow-funnel.csv'),'CSV contains only aggregate data')
# Second active job has separate capacity and public intake.
demo=newJob('TEST 客户实施顾问 / Implementation consultant',3);did=demo['id'];jact(hr,did,'submit');jact(manager,did,'approve');public=Client();config=public.request('/api/public/jobs');check(all('ownerId' not in j for j in config['jobs']),'public jobs do not expose internal staff')
data={'jobId':did,'name':'TEST Casey','email':'casey'+suffix+'@example.invalid','phone':'','introduction':'TEST public application from synthetic record','consent':True,'noticeHash':config['noticeHash']}
public.multi('/api/public/applications',dict(data,noticeHash='old'),None,400);public.multi('/api/public/applications',data,pdf);public.multi('/api/public/applications',dict(data,name='TEST attempted overwrite'),None)
records=hr.request('/api/applicants');cases=[x for x in records if x['email']==data['email']];check(len(cases)==1 and cases[0]['name']=='TEST Casey','duplicate public intake never overwrites')
jact(hr,did,'pause');public.multi('/api/public/applications',dict(data,email='paused@example.invalid'),None,409);jact(hr,did,'resume');public.multi('/api/public/applications',data,None);public.multi('/api/public/applications',data,None,429)
# Future interview cannot be completed early and cancellation frees its exact slot.
c=anew(did,'Drew');cid=c['id'];ci=interview(cid,3600);iv.request(f'/api/interviews/{ci["id"]}/feedback','POST',{'revision':rev(cid),'recommendation':'NEUTRAL','feedback':'TEST early'},409)
d=anew(did,'Erin');eid=d['id'];aact(hr,eid,'screen');payload={'revision':rev(eid),'interviewerId':users['interviewer'],'title':'TEST conflict','location':'TEST room','startsAt':ci['startsAt'],'endsAt':ci['endsAt']};hr.request(f'/api/applicants/{eid}/interviews','POST',payload,409);hr.request(f'/api/interviews/{ci["id"]}/cancel','POST',{'revision':rev(cid),'note':'TEST reschedule'});hr.request(f'/api/applicants/{eid}/interviews','POST',payload)
# Different application commands racing with one revision commit only once.
old=rev(cases[0]['id']);path=f'/api/applicants/{cases[0]["id"]}/actions/screen';body={'revision':old,'note':'TEST simultaneous screening'}
def race():
    # A separate authorized session avoids cookie jar thread races.
    client=Client().login('hr'+suffix,pwd)
    try:client.request(path,'POST',body);return 200
    except AssertionError as e:
        if 'actual 409' in str(e):return 409
        raise
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:codes=sorted(pool.map(lambda _:race(),range(2)))
check(codes==[200,409],'simultaneous revision protection')
# Cross-department read denial; active account cannot be disabled before transfer.
dep=admin.request('/api/admin/departments','POST',{'name':'TEST separate department'});external=admin.request('/api/admin/users','POST',{'username':'dept'+suffix,'displayName':'TEST other department','password':pwd,'roleId':role('Recruiter'),'departmentId':dep['id'],'enabled':True});outsider=Client().login('dept'+suffix,pwd);outsider.request(f'/api/applicants/{aid}',expect=403)
account=next(x for x in admin.request('/api/admin/users') if x['id']==users['hr']);admin.request('/api/admin/users/'+str(account['id']),'PUT',dict(account,enabled=False),409)
# Unassigned account reset revokes the old session immediately.
account=next(x for x in admin.request('/api/admin/users') if x['id']==users['other']);admin.request('/api/admin/users/'+str(account['id']),'PUT',dict(account,password='Aa9'+secrets.token_hex(20)));other.request('/api/jobs',expect=401)
state={'base':args.base,'suffix':suffix,'usernames':{k:k+suffix for k in users},'password':pwd,'users':users,'closedJob':jid,'demoJob':did,'hiredApplicant':aid,'publicApplicant':cases[0]['id'],'screenedApplicant':cid,'scheduledApplicant':eid,'resumeSha256':hashlib.sha256(pdf).hexdigest(),'checks':checks}
fd=os.open(args.output,os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600)
with os.fdopen(fd,'w')as f:json.dump(state,f)
print(f'PASS: {checks} HTTP assertions on fresh MySQL workflow, permissions, public intake, files, revisions and reports; private QA state saved')
