import {registerAndLogin} from './player-account.mjs';
// Run only against an isolated server: root disables the boss requirement in
// that copy to exercise the real distant portals without changing their positions.
import {chromium} from 'playwright';
import assert from 'node:assert/strict';
import {mkdir, writeFile} from 'node:fs/promises';
import {floorLayout} from '../src/app/floor-layout.mjs';

const url = process.env.AINCRAD_URL || 'http://127.0.0.1:8080';
const password = process.env.AINCRAD_ADMIN_PASSWORD;
if (!password) throw Error('Usa un servidor aislado y AINCRAD_ADMIN_PASSWORD.');
const out = new URL('../../docs/screenshots/', import.meta.url).pathname;
await mkdir(out, {recursive: true});
const world = await (await fetch(url + '/api/world')).json();
assert.deepEqual(world.floors.map(f => f.radius), [92, 84]);
assert.equal(world.floors.flatMap(f => f.resources).length, 22);
const checks = [], navigation = [];
const done = text => { checks.push(text); console.log(text); };

// Flood the server's actual generated obstacle field at one-metre resolution.
// Clearance .9 m is stricter than the server's .4 m collision margin.
for (const floor of world.floors) {
  const radius = Math.ceil(floor.radius), side = radius * 2 + 1;
  const index = (x, z) => (z + radius) * side + x + radius;
  const free = new Uint8Array(side * side), visited = new Uint8Array(side * side);
  for (let z = -radius; z <= radius; z++) for (let x = -radius; x <= radius; x++) {
    free[index(x,z)] = Math.hypot(x,z) < floor.radius-1 && !floor.props.some(p => Math.hypot(x-p.x,z-p.z) < p.radius+.9) ? 1 : 0;
  }
  const start = [Math.round(floor.spawn.x), Math.round(floor.spawn.z)], queue = [start];
  assert.equal(free[index(...start)], 1); visited[index(...start)] = 1;
  for (let i = 0; i < queue.length; i++) {
    const [x,z] = queue[i];
    for (const [dx,dz] of [[1,0],[-1,0],[0,1],[0,-1]]) {
      const nx=x+dx,nz=z+dz,id=index(nx,nz);
      if (Math.abs(nx)>radius || Math.abs(nz)>radius || !free[id] || visited[id]) continue;
      visited[id]=1; queue.push([nx,nz]);
    }
  }
  const reachable = (p, distance) => queue.some(([x,z]) => Math.hypot(x-p.x,z-p.z) <= distance);
  assert.ok(reachable(floor.portal,1), 'Portal inaccesible en piso '+floor.id);
  for (const npc of floor.npcs) assert.ok(reachable(npc,1), 'NPC inaccesible: '+npc.id);
  for (const zone of floor.monsterZones) assert.ok(reachable(zone,zone.radius-2), 'Zona aislada: '+zone.id);
  for (const node of floor.resources) assert.ok(reachable(node,2.5), 'Recurso inaccesible: '+node.id);
  for (let z = Math.min(floor.spawn.z,floor.portal.z); z <= Math.max(floor.spawn.z,floor.portal.z); z++) {
    assert.equal(free[index(0,Math.round(z))],1,'Sendero principal bloqueado');
  }
  navigation.push({floor:floor.id, radius:floor.radius, area:floorLayout(floor).area,
    generatedTrees:floor.props.filter(p=>['tree','crystal'].includes(p.kind)).length,
    reachableGridCells:queue.length, zones:floor.monsterZones.length, resources:floor.resources.length});
}
done('Los dos pisos tienen rutas desde la aparición hasta portales, NPC, cinco zonas y 22 recursos; el sendero central está libre');

const browser = await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH,
  args:['--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader','--ignore-gpu-blocklist']});
const ctx=await browser.newContext({viewport:{width:1365,height:960}}),page=await ctx.newPage();
page.setDefaultTimeout(60000);
const errors=[],events=[]; let state;
page.on('pageerror',e=>errors.push(e.message));
page.on('console',m=>{if(m.type()==='error'&&!m.text().startsWith('WebSocket connection')) errors.push(m.text());});
page.on('websocket',ws=>ws.on('framereceived',({payload})=>{try {const e=JSON.parse(String(payload));if(e.type==='state')state=e;else events.push(e);}catch{}}));
await page.addInitScript(()=>{const Original=window.WebSocket;window.WebSocket=class extends Original{constructor(...args){super(...args);window.__testSocket=this;}};});
const me=()=>state?.players.find(p=>p.id===state.you);
const send=e=>page.evaluate(e=>window.__testSocket.send(JSON.stringify(e)),e);
const wait=async(fn,message,ms=60000)=>{const end=Date.now()+ms;while(Date.now()<end){if(fn())return;await new Promise(r=>setTimeout(r,100));}throw Error(message);};
async function checkMap(floor) {
  await page.waitForFunction(({diameter})=>document.querySelector('.coords')?.textContent.includes('DIÁMETRO '+diameter+' m'),floorLayout(floor));
  const actual=await page.locator('.minimap svg').getAttribute('viewBox');
  assert.equal(actual,floorLayout(floor).viewBox);
  assert.ok((await page.locator('.minimap path').getAttribute('transform')).includes(`translate(${floor.portal.x},${floor.portal.z})`));
  const [x,z,width,height]=actual.split(' ').map(Number);
  for(const p of [floor.spawn,floor.portal,...floor.monsterZones]) {
    const margin=p.radius||4;
    assert.ok(p.x-margin>=x && p.x+margin<=x+width && p.z-margin>=z && p.z+margin<=z+height,'Marcador recortado');
  }
}
try {
  await registerAndLogin(ctx.request,url);
  await page.goto(url+'/admin');
  await page.getByLabel('Contraseña',{exact:true}).fill(password);
  await page.getByRole('button',{name:/Entrar como root/}).click();
  await page.getByRole('heading',{name:'Construye el siguiente nivel.'}).waitFor();
  assert.equal(await page.getByLabel('Radio (35–100)',{exact:true}).inputValue(),'92');
  await page.getByText('Diámetro: 184 m', {exact:false}).waitFor();
  await page.locator('.floor-list button').nth(1).click();
  await page.getByText('Diámetro: 168 m', {exact:false}).waitFor();
  assert.equal(await page.getByLabel('Radio (35–100)',{exact:true}).inputValue(),'84');
  await page.screenshot({path:out+'29-pisos-root.png'});
  await page.locator('.floor-list button').first().click();
  await page.getByText('Diámetro: 184 m', {exact:false}).waitFor();
  await page.getByLabel('Requiere derrotar a un Centinela',{exact:true}).uncheck();
  const save=page.waitForResponse(r=>r.url().endsWith('/api/admin/world')&&r.request().method()==='PUT');
  await page.getByRole('button',{name:'Guardar y validar',exact:true}).click();
  assert.ok((await save).ok());
  const apply=page.waitForResponse(r=>r.url().endsWith('/api/admin/reload'));
  await page.getByRole('button',{name:'Aplicar y reconectar →',exact:true}).click();
  assert.ok((await apply).ok());
  done('Root muestra 184/168 m de diámetro, valida y aplica el mundo ampliado; los portales conservan sus coordenadas');

  await page.goto(url);
  await page.getByLabel('TU NOMBRE DE AVENTURERO').fill('Explorador');
  await page.getByRole('button',{name:/Entrar al mundo/}).click();
  await wait(()=>me(),'No entra al mundo');
  const identity=me().id;
  await checkMap(world.floors[0]);
  await send({type:'move',x:0,z:-52});
  await wait(()=>me().z < -51.7,'No sale del límite antiguo de 46 m');
  await page.waitForTimeout(900);
  await page.screenshot({path:out+'30-praderas-ampliadas.png'});
  const beforeErrors=events.length;
  await send({type:'move',x:0,z:-94});
  await wait(()=>events.slice(beforeErrors).some(e=>e.type==='error'&&e.text==='Destino bloqueado'),'Aceptó destino fuera del nuevo piso');
  assert.ok(me().z>=-53);
  done('Movimiento real hasta Z=-52, fuera del piso antiguo; el servidor sigue rechazando destinos fuera del radio nuevo');

  await send({type:'move',x:world.floors[0].portal.x,z:world.floors[0].portal.z});
  await wait(()=>Math.hypot(me().x,me().z+76)<.4,'No alcanza el portal norte real');
  await page.waitForTimeout(500);
  await page.screenshot({path:out+'31-portal-norte-ampliado.png'});
  await send({type:'portal'});
  await wait(()=>state.floor===2,'No atraviesa el portal norte');
  assert.equal(me().id,identity); assert.ok(Math.abs(me().z-58)<.5);
  await checkMap(world.floors[1]);
  await page.waitForTimeout(900);
  await page.screenshot({path:out+'32-bosque-ampliado.png'});
  done('Portal norte en Z=-76 y llegada al bosque en Z=58, con personaje conservado y minimapa completo en ambos pisos');

  await send({type:'move',x:0,z:63});
  await wait(()=>Math.abs(me().z-63)<.4,'No alcanza el portal sur real');
  await page.waitForTimeout(2200);
  await send({type:'portal'});
  await wait(()=>state.floor===1,'No regresa al primer piso');
  assert.equal(me().id,identity);
  assert.ok(Math.hypot(me().x-world.floors[0].spawn.x,me().z-world.floors[0].spawn.z)<.5);
  await checkMap(world.floors[0]);
  done('El portal de regreso en Z=63 devuelve al refugio con la misma identidad');
  assert.deepEqual(errors,[]);
  await writeFile(new URL('../../docs/expanded-floors-result.json',import.meta.url),JSON.stringify({date:new Date().toISOString(),browser:browser.version(),checks,navigation,errors,
    testWorldOverride:'Sólo requiresBoss=false en el portal 1; posiciones, obstáculos y criaturas originales conservados.'},null,2)+'\n');
} catch(e) {
  await page.screenshot({path:out+'failure-expanded-floors.png'}).catch(()=>{});
  console.error({error:e.message,errors,position:me()&&{x:me().x,z:me().z,floor:state.floor,hp:me().hp},events:events.slice(-5)});
  throw e;
} finally {await browser.close();}
