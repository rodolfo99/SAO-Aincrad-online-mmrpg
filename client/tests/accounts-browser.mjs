import {chromium} from 'playwright';
import assert from 'node:assert/strict';
import {randomBytes} from 'node:crypto';
import {readFile,writeFile,mkdir} from 'node:fs/promises';

const url=process.env.AINCRAD_URL||'http://127.0.0.1:8080',rootPassword=process.env.AINCRAD_ADMIN_PASSWORD;
if(!rootPassword)throw Error('Usa un servidor aislado y AINCRAD_ADMIN_PASSWORD.');
const legacy=process.env.AINCRAD_LEGACY_FIXTURE?JSON.parse(await readFile(process.env.AINCRAD_LEGACY_FIXTURE,'utf8')):null;
const username='aventurero_'+randomBytes(5).toString('hex'),password=randomBytes(24).toString('base64url');
const newPassword=randomBytes(24).toString('base64url'),resetPassword=randomBytes(24).toString('base64url');
const out=new URL('../../docs/screenshots/',import.meta.url).pathname;await mkdir(out,{recursive:true});
const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader','--ignore-gpu-blocklist']});
const ctx=await browser.newContext({viewport:{width:1365,height:960}});let page=await ctx.newPage(),state;
const errors=[],checks=[],done=text=>{checks.push(text);console.log(text);};
const bind=page=>{page.setDefaultTimeout(60000);page.on('pageerror',e=>errors.push(e.message));page.on('console',m=>{if(m.type()==='error'&&!m.text().startsWith('WebSocket connection'))errors.push(m.text());});page.on('websocket',ws=>ws.on('framereceived',({payload})=>{try{const message=JSON.parse(String(payload));if(message.type==='state')state=message;}catch{}}));};bind(page);
const me=()=>state?.players.find(p=>p.id===state.you);
const wait=async(fn,message)=>{const end=Date.now()+60000;while(Date.now()<end){if(fn())return;await new Promise(r=>setTimeout(r,100));}throw Error(message);};
async function login(page,password){await page.getByLabel('Usuario',{exact:true}).fill(username);await page.getByLabel('Contraseña',{exact:true}).fill(password);await page.getByRole('button',{name:/Entrar a mi cuenta/}).click();await page.locator('.account-status').getByText(username,{exact:true}).waitFor();}
if(legacy)await page.addInitScript(token=>{try{if(!sessionStorage.getItem('legacy-test-seeded')){localStorage.setItem('aincrad-token-v1',token);sessionStorage.setItem('legacy-test-seeded','1');}}catch{}},legacy.token);
try{
  await page.goto(url);await page.getByRole('button',{name:/Entrar a mi cuenta/}).waitFor();
  assert.equal(await page.getByRole('button',{name:/Entrar al mundo/}).count(),0);assert.equal(await page.locator('character-editor').count(),0);
  await page.screenshot({path:out+'33-inicio-sesion.png'});
  await page.getByRole('button',{name:'Crear cuenta',exact:true}).click();await page.getByLabel('Correo electrónico',{exact:true}).fill(username+'@example.test');await page.getByLabel('Usuario',{exact:true}).fill(username);await page.getByLabel('Contraseña',{exact:true}).fill(password);await page.getByLabel('Repetir contraseña',{exact:true}).fill('No-coincide-123');await page.getByRole('button',{name:/Registrarme/}).click();await page.getByText('Las contraseñas no coinciden',{exact:true}).waitFor();
  await page.getByLabel('Contraseña',{exact:true}).fill(password);await page.getByLabel('Repetir contraseña',{exact:true}).fill(password);await page.getByRole('button',{name:/Registrarme/}).click();await page.getByText('Cuenta creada. Inicia sesión para jugar.',{exact:true}).waitFor();
  assert.equal((await ctx.cookies()).filter(c=>c.name==='aincrad_player').length,0);assert.equal(await page.getByRole('button',{name:/Entrar al mundo/}).count(),0);
  done('La portada bloquea el juego anónimo; registro con confirmación de contraseña y acceso separado mediante inicio de sesión');

  await login(page,password);const cookie=(await ctx.cookies()).find(c=>c.name==='aincrad_player');assert.ok(cookie?.httpOnly);assert.equal(cookie.sameSite,'Strict');assert.equal(cookie.path,'/');assert.ok(!(await page.evaluate(()=>document.cookie)).includes('aincrad_player'));
  if(legacy){
    await page.getByRole('button',{name:'Vincular personaje anterior',exact:true}).click();await page.getByText('Personaje vinculado a tu cuenta. Ya puedes continuar.',{exact:true}).waitFor();
    await page.screenshot({path:out+'34-personaje-vinculado.png'});await page.getByRole('button',{name:/Continuar aventura/}).click();await wait(()=>me()?.id===legacy.id,'No recupera el personaje anterior');assert.equal(me().xp,300);assert.equal(me().col,234);assert.equal(me().potions,8);
    assert.equal(await page.evaluate(()=>localStorage.getItem('aincrad-token-v1')),null);
    await page.getByRole('button',{name:'Salir del mundo',exact:true}).click();await page.getByRole('button',{name:'Crear otro personaje',exact:true}).click();
    done('Una partida creada por el JAR anterior se vincula tras iniciar sesión, conserva EXP/col/pociones y deja de usar la clave del navegador');
  }
  await page.getByRole('button',{name:'Chica',exact:true}).click();await page.getByLabel('TU NOMBRE DE AVENTURERO').fill('Guardiana');await page.getByRole('button',{name:/Entrar al mundo/}).click();await wait(()=>me()?.name==='Guardiana','No crea el personaje de la cuenta');const id=me().id;
  await page.locator('#world-canvas').focus();const z=me().z;await page.keyboard.down('KeyW');await page.waitForTimeout(400);await page.keyboard.up('KeyW');await wait(()=>me().z<z-.5,'El personaje autenticado no se mueve');
  await page.getByRole('button',{name:'Cerrar sesión',exact:true}).click();await page.getByRole('button',{name:/Entrar a mi cuenta/}).waitFor();
  assert.equal((await ctx.cookies()).filter(c=>c.name==='aincrad_player').length,0);
  done('Creación de otro personaje, movimiento real y cierre de sesión desde el juego; se elimina la sesión del navegador');

  await page.goto('about:blank');const second=await browser.newContext({viewport:{width:1365,height:960}});page=await second.newPage();bind(page);state=null;await page.goto(url);await login(page,password);
  assert.equal(await page.evaluate(()=>localStorage.length),0);await page.getByLabel('Tus personajes',{exact:true}).selectOption(id);await page.screenshot({path:out+'35-personajes-cuenta.png'});await page.getByRole('button',{name:/Continuar aventura/}).click();await wait(()=>me()?.id===id,'No recupera el personaje en otro navegador');
  await page.getByRole('button',{name:'Salir del mundo',exact:true}).click();await page.getByRole('button',{name:'Cambiar mi contraseña',exact:true}).click();await page.getByLabel('Contraseña actual',{exact:true}).fill(password);await page.getByLabel('Nueva contraseña',{exact:true}).fill(newPassword);await page.getByLabel('Repetir nueva contraseña',{exact:true}).fill(newPassword);await page.getByRole('button',{name:'Guardar contraseña',exact:true}).click();await page.getByText('Contraseña actualizada. Inicia sesión de nuevo.',{exact:true}).waitFor();
  await login(page,newPassword);done('Un navegador sin almacenamiento previo recupera los personajes con usuario/contraseña; el cambio de contraseña exige volver a iniciar sesión');

  await page.goto(url+'/admin');await page.getByLabel('Contraseña',{exact:true}).fill(rootPassword);await page.getByRole('button',{name:/Entrar como root/}).click();await page.getByRole('heading',{name:'Usuarios registrados',exact:true}).waitFor();
  await page.getByLabel('Cuenta a recuperar',{exact:true}).selectOption({label:username});await page.getByLabel('Nueva contraseña del jugador',{exact:true}).fill(resetPassword);await page.getByRole('button',{name:'Restablecer contraseña del jugador',exact:true}).click();await page.getByText('Contraseña actualizada. Se cerraron las sesiones de esa cuenta.',{exact:false}).waitFor();await page.locator('.accounts-admin').scrollIntoViewIfNeeded();await page.screenshot({path:out+'36-cuentas-root.png'});
  await page.goto(url);await page.getByRole('button',{name:/Entrar a mi cuenta/}).waitFor();await login(page,resetPassword);assert.equal(await page.locator('#owned-character option').count(),legacy?2:1);
  done('Root administra las cuentas y restablece contraseñas; la sesión anterior queda invalidada y los personajes se conservan');

  if(process.env.AINCRAD_SMTP_INBOX){
    async function mail(subject){let found;const end=Date.now()+15000;while(Date.now()<end){try{found=JSON.parse(await readFile(process.env.AINCRAD_SMTP_INBOX,'utf8')).find(m=>m.to===username+'@example.test'&&m.subject.includes(subject));}catch{}if(found)return found;await new Promise(r=>setTimeout(r,100));}throw Error('No llegó el correo al receptor local');}
    await page.getByRole('button',{name:'Cerrar sesión',exact:true}).click();await page.getByRole('button',{name:'Olvidé mi contraseña',exact:true}).click();await page.getByLabel('Correo electrónico',{exact:true}).fill(username+'@example.test');await page.getByRole('button',{name:/Enviar enlace de recuperación/}).click();await page.getByText(/Si el correo corresponde a una cuenta/).waitFor();await page.screenshot({path:out+'38-recuperacion-correo.png'});
    const message=await mail('Restablecer'),link=message.text.split(/\s+/).find(x=>x.startsWith(url+'/#reset-password='));assert.ok(link);await page.goto(link);await page.getByRole('heading',{name:'Nueva contraseña',exact:true}).waitFor();assert.equal(new URL(page.url()).hash,'');assert.equal(await page.locator('meta[name="referrer"]').getAttribute('content'),'no-referrer');await page.screenshot({path:out+'39-nueva-contrasena.png'});
    const recovered=randomBytes(24).toString('base64url');await page.getByLabel('Nueva contraseña',{exact:true}).fill(recovered);await page.getByLabel('Repetir contraseña',{exact:true}).fill(recovered);await page.getByRole('button',{name:/^Restablecer contraseña/}).click();await page.getByText('Contraseña restablecida. Inicia sesión con tu nueva contraseña.',{exact:true}).waitFor();await login(page,recovered);assert.equal(await page.locator('#owned-character option').count(),legacy?2:1);
    done('Recuperación por correo con SMTP local real, enlace sin filtrarlo en URL/referrer y nueva contraseña que conserva los personajes');
    await page.goto(url+'/admin');await page.getByLabel('Correo de prueba',{exact:true}).fill(username+'@example.test');await page.getByRole('button',{name:'Enviar correo de prueba',exact:true}).click();await page.getByText(/Correo aceptado por la utilidad de envío/).waitFor();await mail('Prueba');await page.locator('.mail-admin').scrollIntoViewIfNeeded();await page.screenshot({path:out+'40-correo-root.png'});await page.goto(url);await page.locator('.account-status').waitFor();
    done('Root consulta el correo integrado y envía una prueba al receptor local sin exponer credenciales ni enlaces privados');
  }
  await page.getByRole('button',{name:'Cerrar sesión',exact:true}).click();await page.getByRole('button',{name:/Entrar a mi cuenta/}).waitFor();await page.setViewportSize({width:390,height:844});await page.getByRole('button',{name:'Crear cuenta',exact:true}).click();
  assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await page.getByRole('button',{name:/Registrarme/}).scrollIntoViewIfNeeded();await page.screenshot({path:out+'37-registro-estrecho.png'});done('El formulario de registro permanece accesible a 390 px de ancho');
  assert.deepEqual(errors,[]);await writeFile(new URL('../../docs/accounts-browser-result.json',import.meta.url),JSON.stringify({date:new Date().toISOString(),browser:browser.version(),legacyReleaseFixture:!!legacy,localSmtpFixture:!!process.env.AINCRAD_SMTP_INBOX,checks,errors},null,2)+'\n');
}catch(error){await page.screenshot({path:out+'failure-accounts.png'}).catch(()=>{});console.error({error:error.message,errors,position:me()&&{name:me().name,x:me().x,z:me().z}});throw error;}finally{await browser.close();}
