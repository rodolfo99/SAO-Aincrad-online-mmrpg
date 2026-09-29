import { floorLayout } from './floor-layout.mjs';
import { BestiaryConfigComponent } from './bestiary-config.component';
import { TrainingConfigComponent } from './training-config.component';
import { SkillsConfigComponent } from './skills-config.component';
import { weaponFits } from './equipment.mjs';
import { GatheringConfigComponent } from './gathering-config.component';
import { CraftingConfigComponent } from './crafting-config.component';
import { WeaponsConfigComponent } from './weapons-config.component';
import { RacesConfigComponent } from './races-config.component';
import { PvpConfigComponent } from './pvp-config.component';
import { Component, signal, OnInit } from '@angular/core';
import { ProgressionEditorComponent } from './progression-editor.component';
import { ProgressionConfigComponent } from './progression-config.component';
import { FormsModule } from '@angular/forms';
import { CharacterEditorComponent } from './character-editor.component';
@Component({selector:'admin-panel',standalone:true,imports:[BestiaryConfigComponent,TrainingConfigComponent,SkillsConfigComponent,GatheringConfigComponent,CraftingConfigComponent,WeaponsConfigComponent,RacesConfigComponent,PvpConfigComponent,FormsModule,CharacterEditorComponent,ProgressionEditorComponent,ProgressionConfigComponent],templateUrl:'./admin.component.html'})
export class AdminComponent implements OnInit {
  mail=signal<any>(null);mailRecipient='';accounts=signal<any[]>([]);resetAccountId='';resetAccountPassword='';characterAccountId='';
  Math=Math;pardon=false;craftingRules:any=null;
  authenticated=signal(false);busy=signal(false);message=signal('');error=signal('');password='';newPassword='';draft:any=null;selected=0;raw='';advanced=false;health:any=null;pending=signal(false);ai:any=null;aiReply=signal('');models=signal<string[]>([]);testNpc='lyra';testText='¿Qué puedo hacer en este piso?';characters=signal<any[]>([]);character:any=null;characterCatalog:any=null;gearIndex=0;statToAdd='health';
  async request(path:string,method='GET',body?:any){const r=await fetch('/api/admin/'+path,{method,credentials:'same-origin',headers:{'Content-Type':'application/json','X-Aincrad-Admin':'1'},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(path==='ai/test'?((this.ai?.timeoutSeconds||30)+5)*1000:15000)});const data=await r.json();if(!r.ok){if(r.status===403&&path!=='login')this.authenticated.set(false);throw new Error(data.error||'No se pudo completar la operación');}return data;}
  async run(fn:()=>Promise<void>){this.busy.set(true);this.error.set('');this.message.set('');try{await fn();}catch(e:any){this.error.set(e.message||'El servidor no responde');}finally{this.busy.set(false);}}
  async ngOnInit(){await this.run(async()=>{const s=await this.request('status');if(s.authenticated)await this.load();this.authenticated.set(s.authenticated);});}
  async login(){await this.run(async()=>{await this.request('login','POST',{username:'root',password:this.password});this.password='';await this.load();this.authenticated.set(true);});}
  async testMail(){await this.run(async()=>{const r=await this.request('mail/test','POST',{email:this.mailRecipient});this.message.set(r.message);});}
  async load(){this.mail.set(await this.request('mail'));this.draft=await this.request('world');this.ai=await this.request('ai');await this.loadCharacters();this.raw=JSON.stringify(this.draft,null,2);this.selected=Math.min(this.selected,this.draft.floors.length-1);this.health=await(await fetch('/api/health')).json();}
  dimensions(){return floorLayout(this.floor());}
  floor(){return this.draft?.floors[this.selected];}
  async save(){await this.run(async()=>{if(this.advanced)this.draft=JSON.parse(this.raw);const r=await this.request('world','PUT',this.draft);this.pending.set(true);this.message.set(r.message);this.raw=JSON.stringify(this.draft,null,2);});}
  async apply(){await this.run(async()=>{const r=await this.request('reload','POST');this.pending.set(false);this.message.set(r.message);await this.load();});}
  async logout(){await this.request('logout','POST');this.authenticated.set(false);this.draft=null;}
  async changePassword(){await this.run(async()=>{const r=await this.request('password','POST',{password:this.newPassword});this.newPassword='';this.authenticated.set(false);this.message.set(r.message);});}
  addFloor(){const f=structuredClone(this.floor());const id=Math.max(...this.draft.floors.map((x:any)=>x.id))+1;f.id=id;f.name='Nuevo piso '+id;f.npcs.forEach((n:any,i:number)=>n.id=`npc-${id}-${i}`);f.monsters.forEach((n:any,i:number)=>n.id=`mob-${id}-${i}`);f.resources?.forEach((n:any,i:number)=>n.id=`resource-${id}-${i}`);f.buildings?.forEach((n:any,i:number)=>n.id=`building-${id}-${i}`);f.training?.dummies?.forEach((n:any,i:number)=>n.id=`dummy-${id}-${i}`);f.monsterZones?.forEach((n:any,i:number)=>n.id=`zone-${id}-${i}`);f.portal.to=this.draft.floors[0].id;f.portal.requiresBoss=false;this.draft.floors.push(f);this.selected=this.draft.floors.length-1;}
  addNpc(){this.floor().npcs.push({id:'npc-'+crypto.randomUUID().slice(0,8),name:'Nuevo habitante',role:'guide',x:3,z:10});}
  addMonster(){this.floor().monsters.push({id:'mob-'+crypto.randomUUID().slice(0,8),name:'Nuevo jabalí',kind:this.draft.monsterSpecies[0].id,level:1,x:10,z:0});}
  gearKeys(){return Object.keys(this.gear()?.attributes||{});}
  attribute(id:string){return this.draft?.characterOptions?.attributeDefinitions.find((a:any)=>a.id===id);}
  addGear(copy=false){const g=structuredClone(this.gear());if(!g)return;g.id='set-'+crypto.randomUUID().slice(0,8);g.name=copy?g.name+' (copia)':'Nuevo conjunto';g.minLevel=Math.max(...this.draft.characterOptions.equipmentSets.filter((v:any)=>v.classId===g.classId&&v.gender===g.gender).map((v:any)=>v.minLevel))+5;if(!copy){g.attributes={};g.helmet=false;g.cape=false;g.style='tunic';}this.draft.characterOptions.equipmentSets.push(g);this.gearIndex=this.draft.characterOptions.equipmentSets.length-1;}
  deleteGear(){if(this.gear()?.minLevel===1){this.error.set('Cada clase y género necesita un conjunto de nivel 1. Conserva el inicial.');return;}this.draft.characterOptions.equipmentSets.splice(this.gearIndex,1);this.gearIndex=0;}
  addAttribute(){const a=this.attribute(this.statToAdd);if(a)this.gear().attributes[a.id]=Math.max(a.min,Math.min(a.max,0));}
  removeAttribute(id:string){delete this.gear().attributes[id];}
  newAttribute(){this.draft.characterOptions.attributeDefinitions.push({id:'attr-'+crypto.randomUUID().slice(0,8),name:'Nuevo atributo',effect:'none',min:0,max:100,step:1});}
  gear(){return this.draft?.characterOptions?.equipmentSets?.[this.gearIndex];}
  async loadCharacters(){this.accounts.set(await this.request('accounts'));this.characters.set(await this.request('characters'));const world=await(await fetch('/api/world')).json();this.characterCatalog=world.characterOptions;this.craftingRules=world.crafting;}
  characterLevel(){const r=this.characterCatalog?.progression;return r?Math.min(r.maxLevel,1+Math.floor(this.character.xp/r.xpPerLevel)):1;}
  selectCharacter(p:any){this.character=structuredClone(p);this.characterAccountId=p.accountId||'';this.pardon=false;}
  resetCharacterTalents(){if(this.character)this.character.talentRanks={};}
  async saveCharacter(){await this.run(async()=>{const p=this.character;this.character=await this.request('characters/'+encodeURIComponent(p.id),'PATCH',{name:p.name,appearance:p.appearance,xp:p.xp,col:p.col,potions:p.potions,attributeRanks:p.attributeRanks,talentRanks:p.talentRanks});await this.loadCharacters();this.message.set('Personaje actualizado. Se guardó una copia anterior y el cambio es visible en el juego.');});}
  accountName(id:string){return this.accounts().find(a=>a.id===id)?.username||'Sin vincular';}
  async resetPlayerPassword(){await this.run(async()=>{try{const r=await this.request('accounts/'+encodeURIComponent(this.resetAccountId)+'/password','POST',{password:this.resetAccountPassword});this.message.set(r.message);}finally{this.resetAccountPassword='';}});}
  async assignCharacterAccount(){if(!this.character)return;await this.run(async()=>{await this.request('characters/'+encodeURIComponent(this.character.id)+'/account','PATCH',{accountId:this.characterAccountId});this.character.accountId=this.characterAccountId;await this.loadCharacters();this.message.set('Personaje vinculado a la cuenta. Se conservó una copia previa.');});}
  async refreshCharacters(){await this.run(async()=>await this.loadCharacters());}
  characterWeapons(){return this.characterCatalog?.weaponSets?.filter((w:any)=>weaponFits(this.characterCatalog,w,this.character?.appearance)&&w.minLevel<=this.characterLevel())||[];}
  async saveWeapons(){await this.run(async()=>{this.character=await this.request('characters/'+encodeURIComponent(this.character.id)+'/weapons','PATCH',{weaponSetId:this.character.weaponSetId});await this.loadCharacters();this.message.set('Armas del personaje guardadas.');});}
  async saveCitizenship(){await this.run(async()=>{this.character=await this.request('characters/'+encodeURIComponent(this.character.id)+'/citizenship','PATCH',{citizenship:this.character.pvp.citizenship,pardon:this.pardon});this.pardon=false;await this.loadCharacters();this.message.set('Ciudadanía actualizada con respaldo. El historial de bajas se conserva.');});}
  async saveCrafting(){await this.run(async()=>{this.character=await this.request('characters/'+encodeURIComponent(this.character.id)+'/crafting','PATCH',{professionXp:this.character.crafting.professionXp,materials:this.character.crafting.materials});await this.loadCharacters();this.message.set('Progreso de oficios y materiales guardados.');});}
  addBuilding(){this.floor().buildings.push({id:'building-'+crypto.randomUUID().slice(0,8),kind:'smithy',name:'Nuevo taller',x:-20,z:15,rotation:0});}
  async saveAi(){await this.run(async()=>{const r=await this.request('ai','PUT',this.ai);this.message.set(r.message);});}
  async listModels(){await this.run(async()=>{const r=await this.request('ai/models','POST',this.ai);this.models.set((r.models||[]).map((m:any)=>m.name));this.message.set(`${this.models().length} modelos disponibles en Ollama.`);});}
  async testAi(){await this.run(async()=>{const r=await this.request('ai/test','POST',{config:this.ai,npcId:this.testNpc,text:this.testText});this.aiReply.set(r.text+(r.fallback?' [Respaldo: '+r.reason+']':''));});}
  allNpcs(){return this.draft?.floors.flatMap((f:any)=>f.npcs)||[];}
  addBinding(){const n=this.allNpcs().find((n:any)=>!this.ai.bindings.some((b:any)=>b.npcId===n.id));if(!n){this.error.set('Todos los NPC ya están vinculados. Añade y aplica otro NPC en el mundo.');return;}this.ai.bindings.push({npcId:n.id,enabled:true,model:'',systemPrompt:'Interpreta a '+n.name+' en español, en dos o tres frases.',fallback:'Ahora estoy ocupado. Hablemos más tarde.'});}
  switchEditor(){if(!this.advanced)this.raw=JSON.stringify(this.draft,null,2);else{try{this.draft=JSON.parse(this.raw);}catch{this.error.set('Corrige el JSON antes de volver al formulario.');return;}}this.advanced=!this.advanced;}
}
