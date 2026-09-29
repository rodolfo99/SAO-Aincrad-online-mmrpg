import {AccountPanelComponent} from './account-panel.component';
import { floorLayout } from './floor-layout.mjs';
import { ZonesPanelComponent } from './zones-panel.component';
import { TrainingPanelComponent } from './training-panel.component';
import { SkillsPanelComponent } from './skills-panel.component';
import { weaponFits } from './equipment.mjs';
import { CraftingPanelComponent } from './crafting-panel.component';
import { Component, AfterViewInit, OnDestroy, ViewChild, ElementRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { GameService } from './game.service';
import { readGraphics } from './environment-model.js';
import { WorldRenderer } from './world-renderer.js';
import { CharacterEditorComponent } from './character-editor.component';
import { ProgressionEditorComponent } from './progression-editor.component';
import { AdminComponent } from './admin.component';
@Component({selector:'app-root',standalone:true,imports:[AccountPanelComponent,ZonesPanelComponent,TrainingPanelComponent,SkillsPanelComponent,CraftingPanelComponent,FormsModule,AdminComponent,CharacterEditorComponent,ProgressionEditorComponent],templateUrl:'./app.component.html'})
export class AppComponent implements AfterViewInit,OnDestroy {
  game=inject(GameService);adminPage=location.pathname.startsWith('/admin');entered=signal(false);panel=signal('');renderError=signal('');name='Explorador';message='';npcMessage='';chatOpen=signal(true);renderer?:WorldRenderer;
  appearance:any={gender:'male',face:'calm',hairColor:'#342f32',skinColor:'#dfb18b',classId:'warrior',race:'human',specializationId:'swordsman'};editAppearance:any;editName='';editAttributes:any={};editTalents:any={};
  @ViewChild('worldCanvas') canvas?:ElementRef<HTMLCanvasElement>;
  hasProfile(){return !!this.game.selectedCharacterId;}
  async enter(){if(!this.game.account())return;this.game.appearance=this.appearance;this.entered.set(true);await this.game.connect(this.name);}
  ngAfterViewInit(){if(!this.adminPage){void this.game.loadSession().catch(()=>{this.game.sessionReady.set(true);this.game.error.set('No se pudo comprobar la sesión. Inicia el servidor y recarga.');});void this.game.loadWorld().catch(()=>this.game.error.set('Inicia el servidor para cargar las opciones del personaje.'));}if(this.canvas)this.renderer=new WorldRenderer(this.canvas.nativeElement,this.game,(m:string)=>this.renderError.set(m));}
  xpPercent(){const p=this.game.me(),r=this.game.world()?.characterOptions?.progression;if(!p||!r)return 0;return p.level>=r.maxLevel?100:100*(p.xp%r.xpPerLevel)/r.xpPerLevel;}
  floor(){return this.game.world()?.floors.find((f:any)=>f.id===this.game.state()?.floor);}
  target(){const list=this.game.me()?.targetKind==='player'?this.game.state()?.players:this.game.state()?.monsters;return list?.find((m:any)=>m.id===this.game.me()?.target);}
  sendChat(){if(this.message.trim()){this.game.send({type:'chat',text:this.message});this.message='';}}
  askNpc(){const d=this.game.dialogue();if(!d||!this.npcMessage.trim())return;this.game.send({type:'npcChat',npcId:d.npcId,text:this.npcMessage});this.game.dialogue.update((x:any)=>({...x,waiting:true}));this.npcMessage='';}
  action(type:string,extra={}){this.game.send({type,...extra});}
  saveProgression(){this.game.send({type:'progression',attributes:this.editAttributes,talents:this.editTalents});}
  toggle(panel:string){if(panel==='progression'&&this.game.me()){this.editAttributes=structuredClone(this.game.me().attributeRanks);this.editTalents=structuredClone(this.game.me().talentRanks);}if(panel==='character'&&this.game.me()){this.editAppearance=structuredClone(this.game.me().appearance);this.editName=this.game.me().name;}this.panel.set(this.panel()===panel?'':panel);this.renderer?.stop();}
  outfitStats(){const attrs=this.game.me()?.outfit?.attributes||{};return Object.entries(attrs).map(([id,value])=>({name:this.game.world()?.characterOptions?.attributeDefinitions.find((a:any)=>a.id===id)?.name||id,value}));}
  availableWeapons(){const me=this.game.me();return this.game.world()?.characterOptions?.weaponSets.filter((w:any)=>weaponFits(this.game.world().characterOptions,w,me?.appearance))||[];}
  itemStats(item:any){return Object.entries(item?.attributes||{}).map(([id,value])=>`${this.game.world()?.characterOptions?.attributeDefinitions.find((a:any)=>a.id===id)?.name||id}: ${value}`).join(' · ');}
  otherPlayers(){return this.game.state()?.players.filter((p:any)=>p.id!==this.game.id)||[];}
  distance(p:any){return Math.hypot(p.x-this.game.me().x,p.z-this.game.me().z).toFixed(1);}
  activeZone(){const p=this.game.me();return p?this.floor()?.monsterZones?.find((z:any)=>Math.hypot(p.x-z.x,p.z-z.z)<=z.radius):null;}
  dummies(){return this.game.state()?.monsters.filter((m:any)=>m.training)||[];}
  skill(id:string){return this.game.world()?.characterOptions?.skills.find((s:any)=>s.id===id);}
  castSlot(index:number){const id=this.game.me()?.skills?.slots[index];if(id)this.action('castSkill',{skillId:id});}
  saveCharacter(){this.game.send({type:'profile',name:this.editName,appearance:this.editAppearance});}
  newCharacter(){this.game.selectedCharacterId='';this.name='Explorador';}
  exit(){this.game.disconnect();this.entered.set(false);this.game.state.set(null);void this.game.loadSession().catch(()=>{});}
  async logout(){try{await this.game.logout();this.entered.set(false);this.panel.set('');}catch(e:any){this.game.error.set(e.message);}}
  ngOnDestroy(){this.renderer?.dispose();this.game.disconnect();}
  graphics=readGraphics();
  setGraphics(value:string){this.graphics=value;this.renderer?.setQuality(value);}
  layout(){return floorLayout(this.floor());}
  Math=Math;
}
