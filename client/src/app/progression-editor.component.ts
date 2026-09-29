import { Component, Input } from '@angular/core';
@Component({selector:'progression-editor',standalone:true,template:`
@if(rules()){
<div class="point-summary"><div><strong>{{attributeLeft()}}</strong><span>PUNTOS DE ATRIBUTO</span></div><div><strong>{{talentLeft()}}</strong><span>PUNTOS DE TALENTO</span></div><div><strong>{{level}}</strong><span>NIVEL ACTUAL</span></div></div>
<p class="muted">Cada nivel concede {{rules().attributePointsPerLevel}} puntos de atributo y {{rules().talentPointsPerLevel}} de talento. Guarda tu elección en el refugio.</p>
<h3>Atributos del personaje</h3>
@for(a of rules().attributes;track a.id){<div class="rank-row"><div><b>{{a.name}}</b><small>{{a.description}}</small><small>{{effects(a.effects)}}</small></div><div class="rank-controls"><button type="button" [disabled]="(!admin&&!rules().allowRespec)||!attributes[a.id]" (click)="decrement(attributes,a.id)" [attr.aria-label]="'Reducir '+a.name">−</button><strong>{{attributes[a.id]||0}} / {{a.maxRank}}</strong><button type="button" [disabled]="attributeLeft()<1||(attributes[a.id]||0)>=a.maxRank" (click)="increment(attributes,a.id)" [attr.aria-label]="'Aumentar '+a.name">+</button></div></div>}
<h3>Árbol de {{className()}}</h3><p class="muted">Los enlaces requieren al menos un rango del talento anterior. Puedes combinar ramas si tienes puntos.</p>
<div class="talent-tree">@for(tier of tiers();track tier){<div class="talent-tier"><span class="tier-label">NIVEL {{tier}}</span><div class="talent-nodes">@for(t of nodes(tier);track t.id){<article class="talent-node" [class.learned]="talents[t.id]>0" [class.locked]="!eligible(t)"><span class="eyebrow gold">{{t.branch}}</span><h4>{{t.name}}</h4><p>{{t.description}}</p><small>{{effects(t.effects)}}</small>@if(t.requires.length){<div class="talent-link">↳ Desde {{parents(t)}}</div>}<div class="rank-controls"><button type="button" [disabled]="(!admin&&!rules().allowRespec)||!talents[t.id]" (click)="decrement(talents,t.id)" [attr.aria-label]="'Reducir '+t.name">−</button><strong>{{talents[t.id]||0}} / {{t.maxRank}}</strong><button type="button" [disabled]="!eligible(t)||talentLeft()<t.cost||(talents[t.id]||0)>=t.maxRank" (click)="increment(talents,t.id)" [attr.aria-label]="'Aprender '+t.name">+</button></div><small>{{t.cost}} punto(s) por rango</small></article>}</div></div>}</div>
@if(admin||rules().allowRespec){<button type="button" (click)="reset()">Restablecer reparto</button>}@else{<p class="muted">Root ha desactivado la redistribución.</p>}
}`})
export class ProgressionEditorComponent {
 @Input() catalog:any;@Input() classId='warrior';@Input() specializationId='';@Input() level=1;@Input() attributes:any={};@Input() talents:any={};@Input() admin=false;
 rules(){return this.catalog?.progression;}
 attributeLeft(){return (this.level-1)*(this.rules()?.attributePointsPerLevel||0)-Object.values(this.attributes).reduce((sum:number,n:any)=>sum+Number(n),0);}
 talentLeft(){return (this.level-1)*(this.rules()?.talentPointsPerLevel||0)-(this.rules()?.talents||[]).reduce((sum:number,t:any)=>sum+(this.talents[t.id]||0)*t.cost,0);}
 className(){return this.catalog?.classes.find((c:any)=>c.id===this.classId)?.name||this.classId;}
 tiers(){return [...new Set<number>((this.rules()?.talents||[]).filter((t:any)=>t.classId===this.classId&&(!t.specializationIds?.length||t.specializationIds.includes(this.specializationId))).map((t:any)=>t.minLevel))].sort((a,b)=>a-b);}
 nodes(tier:number){return this.rules().talents.filter((t:any)=>t.classId===this.classId&&(!t.specializationIds?.length||t.specializationIds.includes(this.specializationId))&&t.minLevel===tier);}
 parents(t:any){return t.requires.map((id:string)=>this.rules().talents.find((p:any)=>p.id===id)?.name||id).join(' + ');}
 eligible(t:any){return this.level>=t.minLevel&&t.requires.every((id:string)=>(this.talents[id]||0)>0);}
 effects(e:any){const labels:any={maxHp:'PV',damage:'daño',defense:'defensa',speed:'velocidad',criticalChance:'% crítico',healing:'curación',skillPower:'potencia',cooldownReduction:'s recuperación',none:'descriptivo'};return Object.entries(e).map(([k,v])=>`${Number(v)>=0?'+':''}${v} ${labels[k]||k}`).join(' · ');}
 increment(map:any,id:string){map[id]=(map[id]||0)+1;}
 decrement(map:any,id:string){if(map[id]>1)map[id]--;else delete map[id];if(map===this.talents){let changed=true;while(changed){changed=false;for(const t of this.rules().talents)if(this.talents[t.id]&&t.requires.some((p:string)=>!this.talents[p])){delete this.talents[t.id];changed=true;}}}}
 reset(){for(const id of Object.keys(this.attributes))delete this.attributes[id];for(const id of Object.keys(this.talents))delete this.talents[id];}
}
