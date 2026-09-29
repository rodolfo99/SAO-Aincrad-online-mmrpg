import { Component, Input, AfterViewInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AvatarPreview } from './world-renderer.js';
@Component({selector:'character-editor',standalone:true,imports:[FormsModule],template:`
<div class="character-editor">
<div class="avatar-stage"><canvas #preview tabindex="0" aria-label="Vista previa tridimensional de tu personaje. Arrastra o usa las flechas para girar."></canvas><div class="avatar-view-controls"><button type="button" [class.selected]="view==='body'" (click)="view='body'" aria-label="Ver cuerpo entero">Cuerpo</button><button type="button" [class.selected]="view==='face'" (click)="view='face'" aria-label="Acercar al rostro">Rostro</button></div><span>ARRASTRA PARA GIRAR · 3D</span></div>
<div class="appearance-controls">
<div class="gender-options">@for(g of catalog?.genders;track g.id){<button type="button" [class.selected]="appearance.gender===g.id" (click)="appearance.gender=g.id">{{g.name}}</button>}</div>
<div class="field-row"><label>Rostro<select aria-label="Rostro" [(ngModel)]="appearance.face">@for(f of catalog?.faces;track f.id){<option [value]="f.id">{{f.name}}</option>}</select></label><label>Raza<select aria-label="Raza" [(ngModel)]="appearance.race" (ngModelChange)="chooseRace()">@for(r of catalog?.races;track r.id){<option [value]="r.id">{{r.name}}</option>}</select></label></div>
<label>Cabello<div class="swatches">@for(c of (race()?.hairColors||catalog?.hairColors);track c){<button type="button" [style.background]="c" [class.selected]="appearance.hairColor===c" [attr.aria-label]="'Cabello '+c" (click)="appearance.hairColor=c"></button>}<input type="color" [(ngModel)]="appearance.hairColor" aria-label="Color de pelo personalizado"></div></label>
<label>Piel<div class="swatches">@for(c of (race()?.skinColors||catalog?.skinColors);track c){<button type="button" [style.background]="c" [class.selected]="appearance.skinColor===c" [attr.aria-label]="'Piel '+c" (click)="appearance.skinColor=c"></button>}<input type="color" [(ngModel)]="appearance.skinColor" aria-label="Color de piel personalizado"></div></label>
<label>Clase<select aria-label="Clase" [(ngModel)]="appearance.classId" (ngModelChange)="chooseClass()">@for(c of catalog?.classes;track c.id){<option [value]="c.id">{{c.name}}</option>}</select></label>
<label>Especialidad<select aria-label="Especialidad" [(ngModel)]="appearance.specializationId">@for(s of specialties();track s.id){<option [value]="s.id">{{s.name}}</option>}</select></label><p class="class-description">{{specialty()?.description}}</p>
@for(c of catalog?.classes;track c.id){@if(c.id===appearance.classId){<p class="class-description">{{c.description}}</p><div class="class-stats"><span><b>{{c.hp}}</b> VIDA BASE</span><span><b>{{c.damage}}</b> DAÑO BASE</span><span><b>{{c.speed}}</b> VELOCIDAD</span></div>}}
</div>
@if(race();as r){<article class="race-detail">@if(r.portrait){<img [src]="r.portrait" [alt]="r.name" loading="lazy">}<div><b>{{r.name}}</b><p>{{r.description}}</p><small>{{racialEffects()}}</small><small><b>T · {{r.ability?.name}}</b> · {{r.ability?.cooldown}} s</small></div></article>}
</div>`})
export class CharacterEditorComponent implements AfterViewInit,OnDestroy {
 @Input() outfit:any;@Input() weapons:any;@Input() level=1;@Input() weaponSetId="";@Input() appearance:any;@Input() catalog:any;@ViewChild('preview') canvas!:ElementRef<HTMLCanvasElement>;preview?:AvatarPreview;view='body';
 ngAfterViewInit(){this.preview=new AvatarPreview(this.canvas.nativeElement,()=>this.appearance,()=>this.catalog,()=>this.level,()=>this.weaponSetId,()=>this.view,()=>this.outfit||this.weapons?{outfit:this.outfit,weapons:this.weapons}:null);}
 specialties(){return this.catalog?.specializations?.filter((s:any)=>s.classId===this.appearance.classId)||[];}
 specialty(){return this.specialties().find((s:any)=>s.id===this.appearance.specializationId)||this.specialties()[0];}
 chooseClass(){this.appearance.specializationId=this.specialties()[0]?.id;}
 race(){return this.catalog?.races.find((r:any)=>r.id===this.appearance.race);}
 chooseRace(){const r=this.race();if(r){this.appearance.skinColor=r.skinColors?.[0]||this.appearance.skinColor;this.appearance.hairColor=r.hairColors?.[0]||this.appearance.hairColor;}}
 racialEffects(){const labels:any={maxHp:'PV',damage:'daño',defense:'defensa',speed:'velocidad',criticalChance:'crítico %',healing:'curación',skillPower:'potencia',cooldownReduction:'recarga'};return Object.entries(this.race()?.effects||{}).filter(([k,v])=>v!==0).map(([k,v])=>`${Number(v)>0?'+':''}${v} ${labels[k]||k}`).join(' · ')||'Sin modificación de estadísticas base';}
 ngOnDestroy(){this.preview?.dispose();}
}
