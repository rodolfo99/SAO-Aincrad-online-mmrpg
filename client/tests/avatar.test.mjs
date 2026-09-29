import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import * as THREE from 'three';
import {createAvatar,animateAvatar,npcAppearance} from '../src/app/avatar-model.js';
import {WorldRenderer} from '../src/app/world-renderer.js';
const catalog=JSON.parse(readFileSync(new URL('../../world/world.json',import.meta.url))).characterOptions;
const dispose=a=>WorldRenderer.prototype.disposeObject(a);
const appearance=(race,gender='male',classId='warrior',specializationId='swordsman')=>({race:race.id,gender,classId,specializationId,face:'kind',skinColor:race.skinColors[0],hairColor:race.hairColors[0]});
test('Cinco razas y ambos géneros: mallas finitas, animación articulada y presupuesto de geometría',()=>{
 for(const race of catalog.races)for(const gender of ['male','female']){
  const avatar=createAvatar('#326c65',appearance(race,gender),catalog,10);let triangles=0;
  avatar.traverse(m=>{if(!m.geometry)return;for(const key of ['position','normal']){const a=m.geometry.attributes[key];if(a)assert.ok(Array.from(a.array).every(Number.isFinite),race.id+' '+key);}triangles+=(m.geometry.index?.count||m.geometry.attributes.position.count)/3;});
  assert.ok(triangles<90000,'Presupuesto de triángulos: '+triangles);
  for(const [t,walk,hit] of [[0,false,0],[.6,true,0],[1.1,true,.7]]){animateAvatar(avatar,t,walk,hit);const b=new THREE.Box3().setFromObject(avatar);assert.ok(Number.isFinite(b.min.x)&&Number.isFinite(b.max.y));assert.ok(b.max.y>1.7&&b.max.y<3.5);}
  assert.equal(avatar.userData.avatarRig.legs.length,2);assert.equal(avatar.userData.avatarRig.arms.length,2);dispose(avatar);
 }
});
test('Los 30 conjuntos respetan las armas explícitas y las ranuras de dos manos',()=>{
 for(const set of catalog.weaponSets){const look=appearance(catalog.races[0],'female',set.classId,set.specializationIds[0]||catalog.specializations.find(s=>s.classId===set.classId).id);const a=createAvatar('#326c65',look,catalog,set.minLevel,set.id,{weapons:set});const names=[];a.traverse(n=>{if(n.name.startsWith('weapon-'))names.push(n.name)});assert.equal(names.length,set.offHand?2:1);assert.ok(names.includes('weapon-'+set.mainHand.kind));if(set.offHand)assert.ok(names.includes('weapon-'+set.offHand.kind));assert.equal(a.userData.avatarRig.twoHands,set.mainHand.hands===2);dispose(a);}
});
test('La vista root utiliza el conjunto concreto editado y Lyra tiene identidad propia',()=>{
 const outfit={...catalog.equipmentSets[0],primary:'#ff0066',metal:'#1166ee',style:'plate',cape:false,helmet:true};const a=createAvatar('#326c65',appearance(catalog.races[0]),catalog,1,'',{outfit,weapons:{mainHand:null,offHand:null}});assert.ok(a.userData.avatarMaterials.some(m=>m.color.getHexString()==='ff0066'));assert.equal(a.userData.body.userData.cloak,undefined);assert.equal(a.getObjectByName('weapon-sword'),undefined);dispose(a);
 const lyra=npcAppearance({role:'guide'});assert.equal(lyra.appearance.gender,'female');assert.equal(lyra.appearance.hairStyle,'bob');assert.equal(lyra.weapons.mainHand.kind,'tome');
});
