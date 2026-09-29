import {Component,Input,Output,EventEmitter} from '@angular/core';
@Component({selector:'zones-panel',standalone:true,template:`<h2>Zonas de monstruos</h2><p class="muted">Elige un territorio según tu nivel. Las criaturas persiguen dentro de su zona y recuperan vida al retirarse. Los niveles son recomendaciones de dificultad.</p>
@for(f of world?.floors;track f.id){<h3>Piso {{f.id}} · {{f.name}}</h3>@for(z of f.monsterZones;track z.id){<article class="hunting-card" [class.hunting-danger]="me?.level<z.minLevel"><span class="eyebrow">{{species(z.speciesId)?.name}} · {{status(f,z)}}</span><h3>{{z.name}}</h3><b>Nv. {{z.minLevel}}–{{z.maxLevel}}</b><p>{{z.population}} criaturas · reaparecen {{z.respawnSeconds}} s tras caer</p>@if(me?.floor===f.id){<small>{{alive(z)}} vivas ahora en el piso</small><button (click)="approach(f,z)" [attr.aria-label]="'Acercarse a '+z.name">Acercarse</button>}@else{<small>Llega a este piso mediante su portal.</small>}</article>}@empty{<p class="muted">Root puede añadir territorios en este piso.</p>}}
`})
export class ZonesPanelComponent {
 @Input() world:any;@Input() me:any;@Input() monsters:any[]=[];@Output() action=new EventEmitter<any>();
 status(f:any,z:any){return this.inside(f,z)?'ESTÁS AQUÍ':this.me?.level<z.minLevel?'NIVEL SUPERIOR AL TUYO':'EXPLORACIÓN';}
 species(id:string){return this.world.monsterSpecies.find((s:any)=>s.id===id);}inside(f:any,z:any){return this.me?.floor===f.id&&Math.hypot(this.me.x-z.x,this.me.z-z.z)<=z.radius;}alive(z:any){return this.monsters.filter(m=>m.zoneId===z.id).length;}
 approach(f:any,z:any){const candidates=[];for(let i=0;i<32;i++){const angle=i*Math.PI/16;const p={x:z.x+Math.cos(angle)*(z.radius-.7),z:z.z+Math.sin(angle)*(z.radius-.7)};if(!f.props.some((v:any)=>Math.hypot(p.x-v.x,p.z-v.z)<v.radius+.6))candidates.push(p);}candidates.sort((a,b)=>Math.hypot(a.x-this.me.x,a.z-this.me.z)-Math.hypot(b.x-this.me.x,b.z-this.me.z));if(candidates.length)this.action.emit({type:'move',...candidates[0]});}
}
