import {test} from 'node:test';
import assert from 'node:assert/strict';
import * as THREE from 'three';
import {createCreature,animateCreature,createSummon} from '../src/app/creature-model.js';
import {createResource,createTree,createBuilding} from '../src/app/environment-model.js';
import {createSpell,animateEffect} from '../src/app/combat-effects.js';
import {WorldRenderer} from '../src/app/world-renderer.js';
const dispose=o=>WorldRenderer.prototype.disposeObject(o);
function finite(o){let triangles=0;o.traverse(m=>{if(!m.geometry)return;for(const key of ['position','normal']){const a=m.geometry.attributes[key];if(a)assert.ok(Array.from(a.array).every(Number.isFinite),key);}triangles+=(m.geometry.index?.count||m.geometry.attributes.position.count)/3*(m.isInstancedMesh?m.count:1);});return triangles;}
test('Bestiario: geometría acotada, colores root y articulaciones finitas en reposo, carrera y golpe',()=>{
 for(const model of ['slime','dog','boar','orc','goblin','troll','guardian','dummy']){
  const g=createCreature({model,color:'#8866cc'}),triangles=finite(g);assert.ok(triangles<50000,model+' '+triangles);for(const pose of [[0,false,0],[.37,true,0],[.61,true,.5]]){animateCreature(g,...pose);g.updateMatrixWorld(true);const b=new THREE.Box3().setFromObject(g);assert.ok(b.min.y>-.30&&b.max.y<5,model+' altura');assert.ok(Number.isFinite(b.min.x));}
  if(model!=='dummy'){let configured=false;g.traverse(o=>{if(o.material?.color?.getHexString()==='8866cc')configured=true;});assert.ok(configured,model+' color root');}dispose(g);
 }
});
test('Recursos conservan raíz seleccionable y base visible al agotarse; árboles distinguen pino y roble',()=>{
 for(const kind of ['tree','rock']){const {mesh,form}=createResource({id:kind==='tree'?'pine-tree':'iron-vein',kind,color:'#aabc72'});mesh.userData.nodeId='n1';form.visible=false;assert.ok(mesh.children.some(c=>c!==form&&c.visible));form.visible=true;assert.ok(finite(mesh)>0);dispose(mesh);}
 const pine=createTree({pine:true,seed:19}),oak=createTree({pine:false,seed:19});assert.notDeepEqual(new THREE.Box3().setFromObject(pine).getSize(new THREE.Vector3()),new THREE.Box3().setFromObject(oak).getSize(new THREE.Vector3()));dispose(pine);dispose(oak);
});
test('Efectos acotados y liberación del más antiguo bajo ráfagas; invocaciones de los cuatro elementos',()=>{
 const helper=Object.create(WorldRenderer.prototype);Object.assign(helper,{scene:new THREE.Scene(),effects:[],time:0});let disposed=0;
 for(let i=0;i<55;i++){const fx=createSpell(['fire','ice','water','earth','light','shadow'][i%6],3);fx.children.find(m=>m.geometry)?.geometry.addEventListener('dispose',()=>disposed++);helper.addEffect(fx,'spell',1);animateEffect(helper.effects.at(-1),.35);assert.ok(finite(fx)>0);}
 assert.equal(helper.effects.length,48);assert.equal(helper.scene.children.length,48);assert.equal(disposed,7);dispose(helper.scene);
 for(const element of ['fire','ice','water','earth']){const summon=createSummon({element});animateCreature(summon,.5);assert.ok(finite(summon)<15000);dispose(summon);}
});
