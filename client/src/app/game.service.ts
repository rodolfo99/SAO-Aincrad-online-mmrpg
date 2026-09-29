import { Injectable, signal } from '@angular/core';
@Injectable({providedIn:'root'})
export class GameService {
  account=signal<any>(null);sessionReady=signal(false);registrationEnabled=signal(true);recoveryEnabled=signal(false);resetToken=signal(this.takeResetToken());selectedCharacterId='';
  private takeResetToken(){const token=new URLSearchParams(location.hash.slice(1)).get('reset-password');if(token!==null){history.replaceState(null,'',location.pathname+location.search);return token||'invalid';}return '';}
  constructor(){window.addEventListener('hashchange',()=>{const token=this.takeResetToken();if(token){this.resetToken.set(token);this.forgetSession();}});}
  state=signal<any>(null); world=signal<any>(null); connection=signal('offline'); error=signal(''); notice=signal(''); dialogue=signal<any>(null); chat=signal<any[]>([]); name='Explorador';appearance:any=null;
  private socket?:WebSocket; private heartbeat?:ReturnType<typeof setInterval>; private timeout?:ReturnType<typeof setTimeout>; private retry?:ReturnType<typeof setTimeout>; private attempts=0; private desired=false;
  onEvent:(e:any)=>void=()=>{}; id='';
  async playerRequest(path:string,body?:any){
    const response=await fetch('/api/player/'+path,{method:body===undefined?'GET':'POST',credentials:'same-origin',headers:{'Content-Type':'application/json','X-Aincrad-Player':'1'},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(15000)});
    const data=await response.json();if(!response.ok)throw new Error(data.error||'No se pudo completar la solicitud');return data;
  }
  async loadSession(){
    const data=await this.playerRequest('session');this.registrationEnabled.set(data.registrationEnabled);this.recoveryEnabled.set(data.recoveryEnabled);this.sessionReady.set(true);
    if(data.authenticated&&!this.resetToken()){this.account.set(data);if(!data.characters.some((p:any)=>p.id===this.selectedCharacterId))this.selectedCharacterId=data.characters[0]?.id||'';}
    else this.forgetSession();return data.authenticated&&!this.resetToken();
  }
  async login(username:string,password:string){
    const data=await this.playerRequest('login',{username,password});this.disconnect();this.state.set(null);this.selectedCharacterId=data.characters[0]?.id||'';this.account.set(data);this.error.set('');this.notice.set('');
  }
  async logout(){await this.playerRequest('logout',{});this.forgetSession();this.error.set('');this.notice.set('');}
  forgetSession(){this.disconnect();this.account.set(null);this.selectedCharacterId='';this.id='';this.state.set(null);this.dialogue.set(null);this.chat.set([]);}
  async loadWorld(){const r=await fetch('/api/world',{signal:AbortSignal.timeout(8000)});if(!r.ok)throw new Error('No se pudo cargar el mundo');this.world.set(await r.json());}
  async connect(name=this.name){
    if(!this.account()){this.error.set('Inicia sesión para entrar al mundo.');return;}
    this.name=name;this.desired=true;this.error.set('');this.connection.set('connecting');clearTimeout(this.retry);
    try {const selected=this.selectedCharacterId;if(!await this.loadSession()){this.error.set('Tu sesión ha finalizado. Inicia sesión de nuevo.');return;}this.selectedCharacterId=selected;await this.loadWorld();}catch(e){this.error.set('El servidor no responde. Comprueba que esté iniciado.');this.connection.set('offline');return;}
    const ws=new WebSocket(`${location.protocol==='https:'?'wss:':'ws:'}//${location.host}/ws`);this.socket=ws;
    this.timeout=setTimeout(()=>{if(this.connection()!=='online'){this.error.set('Tiempo de conexión agotado.');ws.close();}},8000);
    ws.onopen=()=>ws.send(JSON.stringify({type:'join',name:this.name,characterId:this.selectedCharacterId,appearance:this.appearance}));
    ws.onmessage=event=>{
      if(this.socket!==ws)return;const m=JSON.parse(event.data);
      if(m.type==='welcome'){clearTimeout(this.timeout);this.id=m.id;this.selectedCharacterId=m.id;this.account.update((a:any)=>a?{...a,characters:[...a.characters.filter((p:any)=>p.id!==m.id),{id:m.id,name:m.player.name,level:m.player.level,floor:m.player.floor,online:true}]}:a);this.connection.set('online');this.attempts=0;clearInterval(this.heartbeat);this.heartbeat=setInterval(()=>this.send({type:'ping'}),5000);}
      if(m.type==='state')this.state.set(m);
      if(m.type==='dialogue')this.dialogue.set(m);
      if(m.type==='npcReply'&&this.dialogue()?.npcId===m.npcId){this.dialogue.update((d:any)=>({...d,reply:m.text,fallback:m.fallback,reason:m.reason,waiting:false}));}
      if(m.type==='chat'&&m.floor===this.state()?.floor)this.chat.update(lines=>[...lines,m].slice(-30));
      if(m.type==='notice'){this.notice.set(m.text);setTimeout(()=>{if(this.notice()===m.text)this.notice.set('');},7000);}
      if(m.type==='error'){if(this.dialogue())this.dialogue.update((d:any)=>({...d,waiting:false}));this.error.set(m.text);if(this.connection()!=='online'){this.desired=false;ws.close();}else setTimeout(()=>this.error.set(''),4000);}
      this.onEvent(m);
    };
    ws.onerror=()=>this.error.set('No se pudo conectar al servidor de juego.');
    ws.onclose=event=>{if(this.socket!==ws)return;if(event.code===4401){this.forgetSession();this.error.set('Tu sesión ha finalizado. Inicia sesión de nuevo.');return;}clearTimeout(this.timeout);clearInterval(this.heartbeat);this.connection.set('offline');this.dialogue.set(null);if(this.desired&&this.attempts<5){this.attempts++;this.retry=setTimeout(()=>void this.connect(),Math.min(1000*this.attempts,5000));}};
  }
  send(data:any){if(this.socket?.readyState===WebSocket.OPEN)this.socket.send(JSON.stringify(data));}
  me(){return this.state()?.players?.find((p:any)=>p.id===this.id);}
  disconnect(){this.desired=false;clearTimeout(this.retry);clearTimeout(this.timeout);clearInterval(this.heartbeat);const socket=this.socket;this.socket=undefined;socket?.close();this.connection.set('offline');}
}
