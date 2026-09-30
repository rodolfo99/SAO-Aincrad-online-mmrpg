/** Opt-in integration QA on an isolated server; requires the existing Angular dev dependencies. */
import {chromium} from '../../client/node_modules/playwright/index.mjs';
import {spawn} from 'node:child_process';
import {mkdir,readFile,writeFile} from 'node:fs/promises';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../..');
const url=process.env.AINCRAD_URL,user=process.env.SAO_SMOKE_USER,password=process.env.SAO_SMOKE_PASSWORD;
assert.ok(url&&user&&password,'Define AINCRAD_URL, SAO_SMOKE_USER y SAO_SMOKE_PASSWORD para una cuenta de prueba Java en un servidor aislado.');
const out=resolve(process.env.SAO_QA_OUT||'/tmp/sao-mixed-clients');await mkdir(out,{recursive:true});
const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader','--ignore-gpu-blocklist']});
const context=await browser.newContext({viewport:{width:1280,height:820}}),page=await context.newPage();page.setDefaultTimeout(60000);
let state,welcome,javaStart,javaMoved=false,nativeChat=false;const errors=[];
page.on('pageerror',e=>errors.push(e.message));page.on('websocket',ws=>ws.on('framereceived',({payload})=>{try{const m=JSON.parse(String(payload));if(m.type==='welcome')welcome=m;if(m.type==='chat'&&m.text==='Java y Angular comparten Aincrad ⚔')nativeChat=true;if(m.type==='state'){state=m;const native=m.players.find(p=>p.name==='JavaVisual');if(native){if(javaStart===undefined)javaStart=native.z;else if(Math.abs(native.z-javaStart)>.3)javaMoved=true;}}}catch{}}));
const wait=async(fn,message)=>{const end=Date.now()+90000;while(Date.now()<end){if(fn())return;await new Promise(r=>setTimeout(r,100));}throw Error(message);};
let native;
try{
  const angularUser='web_java_'+Date.now().toString(36),angularPassword='MixedClientLocalTest2026';
  const registered=await context.request.post(url+'/api/player/register',{headers:{Origin:url,'X-Aincrad-Player':'1'},data:{username:angularUser,email:angularUser+'@example.test',password:angularPassword,confirmPassword:angularPassword}});assert.equal(registered.status(),201,await registered.text());
  await page.goto(url);await page.getByLabel('Usuario',{exact:true}).fill(angularUser);await page.getByLabel('Contraseña',{exact:true}).fill(angularPassword);await page.getByRole('button',{name:/Entrar a mi cuenta/}).click();await page.getByLabel('TU NOMBRE DE AVENTURERO').fill('AngularMixed');await page.getByRole('button',{name:/Entrar al mundo/}).click();await wait(()=>state?.players.some(p=>p.name==='AngularMixed'),'Angular no entra al mundo');
  native=spawn('xvfb-run',['-a','-s','-screen 0 1600x1000x24','java','-Xmx1536m','-Dprism.forceGPU=true','-Dprism.order=es2','-cp','client-java/target/classes:client-java/target/lib/*','dev.aincrad.client.Launcher','--server='+url,'--smoke='+out],{cwd:root,env:{...process.env,LIBGL_ALWAYS_SOFTWARE:'1',SAO_SMOKE_MIXED:'1'},stdio:['ignore','pipe','pipe']});let log='';native.stdout.on('data',b=>log+=b);native.stderr.on('data',b=>log+=b);const ended=new Promise((res,rej)=>{native.on('error',rej);native.on('exit',code=>code===0?res():rej(Error('Java terminó con '+code)));});
  await wait(()=>state?.players.some(p=>p.name==='JavaVisual'),'Angular no recibe al personaje Java');
  await page.getByLabel('Mensaje de chat').fill('Angular responde a Java ✓');await page.getByLabel('Enviar mensaje').click();await wait(()=>nativeChat&&javaMoved,'Angular no recibe el chat o movimiento Java');
  const own=state.players.find(p=>p.id===welcome.id),before=own.z;await page.locator('#world-canvas').focus();await page.keyboard.down('KeyW');await page.waitForTimeout(450);await page.keyboard.up('KeyW');await wait(()=>state.players.find(p=>p.id===welcome.id).z<before-.3,'No funciona el movimiento Angular');
  await page.screenshot({path:resolve(out,'07-angular-with-java.png')});await ended;await writeFile(resolve(out,'native.log'),log);
  const result=JSON.parse(await readFile(resolve(out,'visual-result.json'),'utf8'));assert.ok(result.connected&&result.presentedFrames>0&&result.sharedPlayers);assert.ok(result.chat.some(m=>m.text==='Angular responde a Java ✓'));assert.deepEqual(errors,[]);
  const report={server:url,angularConnected:true,javaConnected:result.connected,sharedFloor:result.floor,nativeVisibleInAngular:true,nativeMovementVisibleInAngular:javaMoved,angularMovement:true,javaChatVisibleInAngular:nativeChat,angularChatVisibleInJava:true,openGLFrames:result.presentedFrames,browserErrors:errors};await writeFile(resolve(out,'mixed-result.json'),JSON.stringify(report,null,2)+'\n');console.log(JSON.stringify(report));
}finally{if(native&&native.exitCode===null)native.kill('SIGTERM');await browser.close();}
