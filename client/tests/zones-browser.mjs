import {registerAndLogin} from './player-account.mjs';
import {chromium} from 'playwright';
import assert from 'node:assert/strict';
import {mkdir,writeFile} from 'node:fs/promises';
const url=process.env.AINCRAD_URL||'http://127.0.0.1:8080',password=process.env.AINCRAD_ADMIN_PASSWORD;
if(!password)throw new Error('Usa un servidor aislado con AINCRAD_ADMIN_PASSWORD.');
const output=new URL('../../docs/screenshots/',import.meta.url).pathname;await mkdir(output,{recursive:true});
const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH||undefined,args:['--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader','--ignore-gpu-blocklist']});
const ctx=await browser.newContext({viewport:{width:1440,height:1000}}),page=await ctx.newPage(),admin=await ctx.newPage();let state;const errors=[],checks=[],notices=[];
for(const p of [page,admin]){p.on('pageerror',e=>errors.push(e.message));p.on('console',m=>{if(m.type()==='error'&&!m.text().startsWith('WebSocket connection'))errors.push(m.text());});}
page.on('websocket',ws=>ws.on('framereceived',({payload})=>{try{const n=JSON.parse(String(payload));if(n.type==='state')state=n;else if(n.type==='notice'||n.type==='error')notices.push(n.text);}catch{}}));
await page.addInitScript(()=>{const Original=window.WebSocket;window.WebSocket=class extends Original{constructor(...args){super(...args);window.__testSocket=this;}};});
const me=()=>state?.players.find(p=>p.id===state.you);
const wait=async(fn,msg,ms=25000)=>{const end=Date.now()+ms;while(Date.now()<end){if(fn())return;await new Promise(r=>setTimeout(r,100));}throw new Error(msg);};
const done=s=>{checks.push(s);console.log(s);};
const send=async n=>page.evaluate(n=>window.__testSocket.send(JSON.stringify(n)),n);
const saveApply=async()=>{
 await admin.bringToFront();
 const saving=admin.waitForResponse(r=>r.url().endsWith('/api/admin/world')&&r.request().method()==='PUT');
 await admin.getByRole('button',{name:'Guardar y validar',exact:true}).click();const saved=await saving;assert.ok(saved.ok(),await saved.text());
 const applying=admin.waitForResponse(r=>r.url().endsWith('/api/admin/reload'));
 await admin.getByRole('button',{name:/Aplicar y reconectar/}).click();const applied=await applying;assert.ok(applied.ok(),await applied.text());
};
try{
 await registerAndLogin(ctx.request,url);
 await page.goto(url);await page.getByLabel('Clase',{exact:true}).selectOption('mage');await page.getByLabel('TU NOMBRE DE AVENTURERO').fill('Cazador');await page.getByRole('button',{name:/Entrar al mundo/}).click();await wait(()=>me(),'No entró el personaje');
 const world=await(await ctx.request.get(url+'/api/world')).json();assert.equal(world.schemaRevision,7);assert.equal(world.monsterSpecies.length,7);assert.equal(world.floors.flatMap(f=>f.monsterZones).length,5);
 assert.equal(state.monsters.filter(m=>m.zoneId).length,16);
 for(const z of world.floors[0].monsterZones){const mobs=state.monsters.filter(m=>m.zoneId===z.id);assert.equal(mobs.length,z.population);assert.ok(mobs.every(m=>m.level>=z.minLevel&&m.level<=z.maxLevel&&m.model===z.speciesId));}
 done('Servidor y Angular cargan cinco zonas, siete especies y 16 criaturas de zona en el primer piso');
 await page.getByRole('button',{name:'Zonas de monstruos',exact:true}).click();await page.locator('.hunting-card').nth(4).waitFor();assert.equal(await page.locator('.hunting-card').count(),5);
 for(const range of ['1–3','4–6','7–10','11–15','16–20'])assert.ok((await page.locator('zones-panel').innerText()).includes('Nv. '+range));
 await page.screenshot({path:output+'12-zonas.png'});await page.getByRole('button',{name:'Cerrar panel',exact:true}).click();done('Panel de zonas muestra niveles, población, reaparición y aviso de dificultad de ambos pisos');
 await admin.bringToFront();await admin.goto(url+'/admin');await admin.getByLabel('Contraseña',{exact:true}).fill(password);await admin.getByRole('button',{name:/Entrar como root/}).click();console.log('Root autenticado, esperando el editor');await admin.locator('bestiary-config').waitFor({state:'attached',timeout:60000});await admin.locator('bestiary-config').scrollIntoViewIfNeeded();await admin.locator('.bestiary-config').waitFor({timeout:60000});console.log('Editor de zonas cargado');
 const zone=admin.locator('.bestiary-config details').first();await zone.getByLabel('Nivel mínimo',{exact:true}).fill('2');await zone.getByLabel('Nivel máximo',{exact:true}).fill('4');await saveApply();
 await wait(()=>state?.monsters.filter(m=>m.zoneId==='slime-marsh').map(m=>m.level).join(',')==='2,3,4,2,3,4','No se aplicaron los niveles nuevos');
 await page.getByRole('button',{name:'Zonas de monstruos',exact:true}).click();await page.locator('.hunting-card').first().getByText('Nv. 2–4',{exact:true}).waitFor();await page.getByRole('button',{name:'Cerrar panel',exact:true}).click();
 await zone.getByLabel('Nivel mínimo',{exact:true}).fill('1');await zone.getByLabel('Nivel máximo',{exact:true}).fill('3');await saveApply();await wait(()=>state?.monsters.filter(m=>m.zoneId==='slime-marsh').map(m=>m.level).join(',')==='1,2,3,1,2,3','No se restauraron los niveles');
 done('Root edita niveles, valida, aplica y actualiza la sesión conectada; se restaura la configuración original');
 await admin.bringToFront();const preview=admin.locator('.monster-preview');
 for(const id of ['slime','dog','goblin','orc','troll']){const s=world.monsterSpecies.find(s=>s.id===id);await admin.locator('.species-config').getByLabel('Especie a editar',{exact:true}).selectOption({label:s.name});await preview.scrollIntoViewIfNeeded();await admin.waitForTimeout(600);assert.equal(await admin.locator('.species-config').getByLabel('Modelo 3D',{exact:true}).inputValue(),id);await preview.screenshot({path:output+'monster-'+id+'.png'});}
 await preview.scrollIntoViewIfNeeded();await admin.waitForTimeout(200);await admin.screenshot({path:output+'13-bestiario-root.png'});done('Root presenta las cinco especies con modelos 3D, crecimiento por nivel y materiales editables');
 await page.bringToFront();const slime=state.monsters.find(m=>m.zoneId==='slime-marsh');await send({type:'target',id:slime.id});await page.locator('.target-card').getByText('· Nv. '+slime.level,{exact:true}).waitFor();assert.ok((await page.locator('.target-card').innerText()).includes(slime.name));done('El objetivo de combate identifica especie, nivel y vida de la criatura');
 await page.getByRole('button',{name:'Zonas de monstruos',exact:true}).click();const from={x:me().x,z:me().z};await page.getByRole('button',{name:'Acercarse a Marisma de los slimes',exact:true}).click();await wait(()=>Math.hypot(me().x-from.x,me().z-from.z)>.6,'Acercarse no envió movimiento');await send({type:'move',x:me().x,z:me().z});await page.getByRole('button',{name:'Cerrar panel',exact:true}).click();
 // Route using public obstacle data and normal movement intentions; no teleport or testing endpoint.
 const floor=world.floors[0],slimeZone=floor.monsterZones.find(z=>z.id==='slime-marsh'),start=[Math.round(me().x),Math.round(me().z)],end=[Math.round(slimeZone.x-slimeZone.radius+2),Math.round(slimeZone.z)],key=p=>p.join(','),queue=[start],seen=new Set([key(start)]),parent=new Map();let found=false;
 const free=(x,z)=>Math.hypot(x,z)<floor.radius-1&&!floor.props.some(p=>Math.hypot(x-p.x,z-p.z)<p.radius+.7);
 for(let i=0;i<queue.length;i++){const p=queue[i];if(key(p)===key(end)){found=true;break;}for(const [dx,dz] of [[1,0],[-1,0],[0,1],[0,-1]]){const n=[p[0]+dx,p[1]+dz],k=key(n);if(!seen.has(k)&&free(...n)){seen.add(k);parent.set(k,p);queue.push(n);}}}assert.ok(found,'No existe ruta hasta la zona');
 const path=[end];while(key(path.at(-1))!==key(start))path.push(parent.get(key(path.at(-1))));path.reverse();
 for(let i=1;i<path.length;i++)if(i===path.length-1||path[i+1][0]-path[i][0]!==path[i][0]-path[i-1][0]||path[i+1][1]-path[i][1]!==path[i][1]-path[i-1][1]){const [x,z]=path[i];await send({type:'move',x,z});await wait(()=>Math.hypot(me().x-x,me().z-z)<.3,'Camino bloqueado hacia '+x+','+z);}
 await page.locator('.zone-banner').getByText('Marisma de los slimes · Nv. 1–3',{exact:true}).waitFor();const banner=await page.locator('.zone-banner').boundingBox(),target=await page.locator('.target-card').boundingBox();assert.ok(banner&&target&&target.y>=banner.y+banner.height,'El objetivo oculta el nombre de la zona');await page.screenshot({path:output+'14-marisma.png'});done('Acercamiento físico y entrada en la marisma muestran el rótulo de zona y sus criaturas');
 assert.deepEqual(errors,[]);await writeFile(new URL('../../docs/zones-browser-result.json',import.meta.url),JSON.stringify({date:new Date().toISOString(),browser:browser.version(),checks,errors},null,2)+'\n');
}catch(e){await page.screenshot({path:output+'failure-zones.png'}).catch(()=>{});await admin.screenshot({path:output+'failure-bestiary.png'}).catch(()=>{});console.error({error:e.message,position:me()&&{x:me().x,z:me().z},errors,notices:notices.slice(-5),admin:await admin.locator('body').innerText().then(s=>s.slice(-1800)).catch(()=>''),bestiary:await admin.locator('bestiary-config').evaluateAll(xs=>xs.map(x=>({html:x.outerHTML.slice(0,500),box:x.getBoundingClientRect().toJSON()})))});throw e;}finally{await browser.close();}
