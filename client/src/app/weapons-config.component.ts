import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CharacterEditorComponent } from './character-editor.component';
@Component({selector:'weapons-config',standalone:true,imports:[FormsModule,CharacterEditorComponent],template:`
@if(catalog?.weaponSets){<section class="editor-card weapons-config" id="weapons-config">
<span class="eyebrow gold">PRINCIPAL · SECUNDARIA · NIVEL</span><h2>Armería del mundo</h2>
<p class="muted">Báculos de una o dos manos, varas para magos y sanadores, escudos y objetos de apoyo. Se guarda y aplica con el mundo.</p>
<div class="toolbar-actions"><button class="primary" (click)="add(false)">+ Añadir armas</button><button (click)="add(true)">Duplicar armas</button><button (click)="remove()">Eliminar armas</button></div>
@if(message){<p class="error" role="alert">{{message}}</p>}
<label>Conjunto de armas<select aria-label="Conjunto de armas a editar" [(ngModel)]="index">@for(s of catalog.weaponSets;track s.id;let i=$index){<option [ngValue]="i">{{s.name}} · {{className(s.classId)}}</option>}</select></label>
@if(set();as s){<div class="field-row"><label>Nombre<input [(ngModel)]="s.name" maxlength="80"></label><label>Clase<select [(ngModel)]="s.classId">@for(c of catalog.classes;track c.id){<option [value]="c.id">{{c.name}}</option>}</select></label><label>Nivel mínimo<input type="number" min="1" max="1000" [(ngModel)]="s.minLevel"></label></div>
<label>Especialidades (vacío: todas)<select multiple [(ngModel)]="s.specializationIds">@for(p of catalog.specializations;track p.id){@if(p.classId===s.classId){<option [value]="p.id">{{p.name}}</option>}}</select></label><label class="check"><input type="checkbox" [(ngModel)]="s.autoEquip"> Elegir automáticamente al alcanzar este nivel</label>
<p class="muted">Cada clase requiere un conjunto automático de nivel 1. Solo uno automático por clase y nivel. Los jugadores también pueden elegir un conjunto desbloqueado.</p>
<div class="weapon-editor-grid">
@for(slot of slots;track slot.key){<section class="weapon-slot"><h3>{{slot.name}}</h3>
@if(s[slot.key];as item){<label>Nombre del objeto<input [(ngModel)]="item.name" maxlength="80"></label><div class="field-row"><label>Tipo<select [(ngModel)]="item.kind" (ngModelChange)="kindChanged(slot.key)">@for(k of kinds(slot.key);track k.id){<option [value]="k.id">{{k.name}}</option>}</select></label><label>Manos<select [(ngModel)]="item.hands" (ngModelChange)="handsChanged()"><option [ngValue]="1" [disabled]="item.kind==='greatsword'">Una</option>@if(slot.key==='mainHand'&&(item.kind==='staff'||item.kind==='greatsword')){<option [ngValue]="2">Dos</option>}</select></label></div>
<div class="field-row"><label>Material<input type="color" [(ngModel)]="item.primary"></label><label>Detalle<input type="color" [(ngModel)]="item.secondary"></label><label>Cristal<input type="color" [(ngModel)]="item.glow"></label></div>
@for(id of keys(item);track id){<div class="attribute-row"><label>{{definition(id)?.name||id}}</label><input type="number" [min]="definition(id)?.min" [max]="definition(id)?.max" [step]="definition(id)?.step" [(ngModel)]="item.attributes[id]" [attr.aria-label]="slot.name+' '+id"><button (click)="drop(item,id)" [attr.aria-label]="'Quitar '+slot.name+' '+id">×</button></div>}
<div class="field-row"><select [(ngModel)]="selectedAttribute" aria-label="Atributo de arma">@for(d of catalog.attributeDefinitions;track d.id){<option [value]="d.id">{{d.name}}</option>}</select><button (click)="addAttribute(item)">Añadir atributo</button></div>
@if(slot.key==='offHand'){<button (click)="s.offHand=null">Dejar secundaria libre</button>}
}@else{<p>{{s.mainHand.hands===2?'Ocupada por el arma de dos manos.':'Sin objeto secundario.'}}</p><button (click)="addOffHand()" [disabled]="s.mainHand.hands===2">+ Añadir secundaria</button>}
</section>}
</div><div class="weapon-preview"><character-editor [weapons]="s" [appearance]="{gender:'female',face:'calm',hairColor:'#74503a',skinColor:'#dfb18b',classId:s.classId,race:catalog.races[0].id,specializationId:s.specializationIds?.[0]||''}" [catalog]="catalog" [level]="s.minLevel" [weaponSetId]="s.id" /></div>}
</section>}`})
export class WeaponsConfigComponent {
 @Input() catalog:any;index=0;selectedAttribute='attack';message='';
 slots=[{key:'mainHand',name:'Mano principal'},{key:'offHand',name:'Mano secundaria'}];
 types=[{id:'sword',name:'Espada'},{id:'dagger',name:'Daga'},{id:'greatsword',name:'Mandoble (2 manos)'},{id:'staff',name:'Báculo'},{id:'wand',name:'Vara'},{id:'shield',name:'Escudo'},{id:'orb',name:'Orbe'},{id:'tome',name:'Libro de apoyo'}];
 set(){return this.catalog?.weaponSets[this.index];} className(id:string){return this.catalog.classes.find((c:any)=>c.id===id)?.name;}
 kinds(slot:string){return this.types.filter(k=>slot==='mainHand'?!['shield','orb','tome'].includes(k.id):k.id!=='greatsword');}
 definition(id:string){return this.catalog.attributeDefinitions.find((d:any)=>d.id===id);}keys(item:any){return Object.keys(item.attributes);}
 drop(item:any,id:string){delete item.attributes[id];}addAttribute(item:any){const d=this.definition(this.selectedAttribute);if(d)item.attributes[d.id]=Math.max(d.min,Math.min(d.max,0));}
 kindChanged(slot:string){const i=this.set()[slot];i.hands=i.kind==='greatsword'?2:1;this.handsChanged();}
 handsChanged(){if(this.set().mainHand.hands===2)this.set().offHand=null;}
 addOffHand(){this.set().offHand={name:'Vara de apoyo',kind:'wand',hands:1,attributes:{},primary:'#eee4ba',secondary:'#709987',glow:'#b7efc9'};}
 add(copy:boolean){const s=structuredClone(this.set());s.id='weapons-'+crypto.randomUUID().slice(0,8);s.name=copy?s.name+' (copia)':'Nuevo conjunto de armas';s.autoEquip=false;this.catalog.weaponSets.push(s);this.index=this.catalog.weaponSets.length-1;this.message='';}
 remove(){if(this.set().autoEquip&&this.set().minLevel===1){this.message='Conserva el conjunto automático inicial de cada clase.';return;}this.catalog.weaponSets.splice(this.index,1);this.index=0;this.message='';}
}
