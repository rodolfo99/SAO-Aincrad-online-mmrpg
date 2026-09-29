import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
@Component({selector:'skills-panel',standalone:true,imports:[FormsModule],template:`
<h2>Habilidades activas</h2><p class="muted">Aprende habilidades en Atributos y talentos. Selecciona una criatura o jugador con un clic; las mejoras individuales usan al aliado seleccionado o a ti. Las invocaciones atacan criaturas.</p><h3>{{me?.specialization?.name}}</h3><p class="muted">{{me?.specialization?.description}}</p>
<div class="skill-bindings">@for(slot of [0,1,2,3];track slot){<label>Tecla {{slot+1}}<select [ngModel]="me?.skills?.slots[slot]" (ngModelChange)="send('bindSkill',{slot,skillId:$event})">@for(s of learned();track s.id){<option [value]="s.id">{{s.name}}</option>}@if(!learned().length){<option value="">Aprende un talento</option>}</select></label>}</div>
@for(s of learned();track s.id){<article class="skill-card" [attr.data-element]="s.element"><div><span class="eyebrow gold">{{element(s.element)}} · {{kind(s.kind)}}</span><h3>{{s.name}}</h3><p>{{s.description}}</p><small>Alcance {{s.range}} · radio {{s.radius}} · duración {{s.duration}} s · recarga {{s.cooldown}} s</small></div><button class="primary" [disabled]="cooldown(s)>0||me?.hp<=0" (click)="send('castSkill',{skillId:s.id})">{{cooldown(s)>0?cooldown(s).toFixed(1)+' s':'Usar'}}</button></article>}@empty{<p class="muted">Aún no has aprendido habilidades activas. Tus acciones Q y T siguen disponibles.</p>}
@if(me?.effects?.length){<h3>Efectos actuales</h3>@for(e of me.effects;track e){<p class="active-effect">{{e}}</p>}}
`})
export class SkillsPanelComponent{
 @Input() me:any;@Input() catalog:any;@Output() action=new EventEmitter<any>();
 learned(){return this.catalog?.skills.filter((s:any)=>this.me?.skills?.learned.includes(s.id))||[];}cooldown(s:any){return this.me?.skills?.cooldowns[s.id]||0;}send(type:string,extra:any){this.action.emit({type,...extra});}
 element(s:string){return ({physical:'Físico',fire:'Fuego',ice:'Hielo',water:'Agua',earth:'Tierra',light:'Luz',shadow:'Sombra',specialization:'Elemental'} as any)[s]||s;}kind(s:string){return ({damage:'Daño',curse:'Maldición',buff:'Mejora',heal:'Curación',summon:'Invocación',taunt:'Provocación'} as any)[s]||s;}
}
