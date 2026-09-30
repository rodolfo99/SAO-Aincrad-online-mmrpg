/** Offline transfer of this repository's original art. The desktop runtime is Java only. */
import {readFile,writeFile,mkdir,rm} from 'node:fs/promises';
import {resolve,dirname} from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
import {createHash} from 'node:crypto';
import {gzipSync} from 'node:zlib';

const root=resolve(dirname(fileURLToPath(import.meta.url)),'../..');
const source=resolve(root,'client/src/app'),temporary=resolve(root,'client-java/target/model-export');
await mkdir(temporary,{recursive:true});
const threeURL=pathToFileURL(resolve(root,'client/node_modules/three/build/three.module.js')).href;
const addonsURL=pathToFileURL(resolve(root,'client/node_modules/three/examples/jsm')).href+'/';
for(const name of ['avatar-model.js','render-kit.js','environment-model.js','creature-model.js','equipment.mjs','floor-layout.mjs']){
  let text=await readFile(resolve(source,name),'utf8');
  text=text.replaceAll("from 'three'",`from '${threeURL}'`).replaceAll("from 'three/addons/",`from '${addonsURL}`);
  if(name==='avatar-model.js')text+='\nexport { weapon as exportWeapon };\n';
  await writeFile(resolve(temporary,name),text);
}
await writeFile(resolve(temporary,'package.json'),'{"type":"module"}');
const THREE=await import(threeURL);
const {mergeGeometries}=await import(addonsURL+'utils/BufferGeometryUtils.js');
const {createAvatar,npcAppearance,exportWeapon}=await import(pathToFileURL(resolve(temporary,'avatar-model.js')));
const {createTree,createBuilding,createResource,createPortal}=await import(pathToFileURL(resolve(temporary,'environment-model.js')));
const {createCreature,createSummon}=await import(pathToFileURL(resolve(temporary,'creature-model.js')));
const world=JSON.parse(await readFile(resolve(root,'world/world.json'),'utf8')),catalog=world.characterOptions;
const geometries=[],materials=[],models={},geoIds=new Map(),matIds=new Map();
const roles='skin skinFace lipSkin lips shadowSkin mouth white iris pupil eyeGlint lash hair hairLight cloth cloak linen leather trousers metal gold trim gem horn scale membrane'.split(' ');
const rgb=c=>[c.r,c.g,c.b].map(n=>+n.toFixed(7));
const bytes=(values,integer=false)=>{const a=integer?Uint32Array.from(values):Float32Array.from(values);return Buffer.from(a.buffer).toString('base64');};
function geometry(g){
  const spec={};
  for(const k of ['position','normal','uv','color'])if(g.attributes[k])spec[k]=bytes(g.attributes[k].array);
  if(g.index)spec.index=bytes(g.index.array,true);
  const json=JSON.stringify(spec),key=createHash('sha256').update(json).digest('hex');
  if(!geoIds.has(key)){geoIds.set(key,geometries.length);geometries.push(spec);}return geoIds.get(key);
}
function material(m,instanceColor){
  const color=m.color?.clone()||new THREE.Color('#76d1cd');if(instanceColor)color.multiply(instanceColor);
  const key=m.customProgramCacheKey?.()||'';
  const spec={color:rgb(color),roughness:m.roughness??.35,metalness:m.metalness??0,opacity:m.opacity??.3,
    transparent:!!m.transparent,doubleSide:m.side===THREE.DoubleSide,vertexColors:!!m.vertexColors,
    emissive:rgb((m.emissive||new THREE.Color(0)).clone().multiplyScalar(m.emissiveIntensity??1)),
    role:m.userData?.role||'',surface:key.startsWith('world-surface-')?key.slice(14):key.startsWith('avatar-surface-')?key.slice(15):''};
  const json=JSON.stringify(spec);if(!matIds.has(json)){matIds.set(json,materials.length);materials.push(spec);}return matIds.get(json);
}
function serialize(o,rig){
  const n={p:o.position.toArray(),q:o.quaternion.toArray(),s:o.scale.toArray(),c:[]};
  if(rig.has(o))n.r=rig.get(o);
  if(o.name)n.name=o.name;
  if(o.isMesh){
    if(Array.isArray(o.material))throw Error('A multi-material mesh requires an explicit split');
    n.g=geometry(o.geometry);n.m=material(o.material);n.shadow=[!!o.castShadow,!!o.receiveShadow];
    if(o.isInstancedMesh){
      const matrix=new THREE.Matrix4(),color=new THREE.Color(),parts=[];
      for(let i=0;i<o.count;i++){o.getMatrixAt(i,matrix);if(o.instanceColor)o.getColorAt(i,color);else color.set(0xffffff);
        const g=(o.geometry.index?o.geometry.toNonIndexed():o.geometry.clone()).applyMatrix4(matrix),colors=[];
        for(let j=0;j<g.attributes.position.count;j++)colors.push(color.r,color.g,color.b);
        g.setAttribute('color',new THREE.Float32BufferAttribute(colors,3));parts.push(g);}
      const merged=mergeGeometries(parts),paint=o.material.clone();paint.vertexColors=true;n.g=geometry(merged);n.m=material(paint);parts.forEach(g=>g.dispose());merged.dispose();paint.dispose();
    }
  }
  for(const child of o.children)n.c.push(serialize(child,rig));return n;
}
function store(key,root){
  const rig=new Map(),r=root.userData.avatarRig||root.userData.creatureRig;
  if(root.userData.body)rig.set(root.userData.body,'body');
  if(r){if(r.head)rig.set(r.head,'head');if(r.tail)rig.set(r.tail,'tail');
    (r.legs||[]).forEach((leg,i)=>{rig.set(leg.joint||leg,`leg${i}`);if(leg.knee||leg.userData?.shin)rig.set(leg.knee||leg.userData.shin,`knee${i}`);});
    (r.arms||[]).forEach((arm,i)=>{rig.set(arm.joint||arm,`arm${i}`);if(arm.elbow||arm.userData?.forearm)rig.set(arm.elbow||arm.userData.forearm,`elbow${i}`);if(arm.userData?.hand)rig.set(arm.userData.hand,`hand${i}`);});
  }
  if(root.userData.body?.userData.cloak)rig.set(root.userData.body.userData.cloak,'cloak');
  if(root.userData.body?.userData.tail)rig.set(root.userData.body.userData.tail,'tail');
  if(root.userData.avatarMaterials)root.userData.avatarMaterials.forEach((m,i)=>m.userData.role=roles[i]);
  models[key]={node:serialize(root,rig),height:root.userData.labelHeight||2.8,slime:!!root.userData.slime,quadruped:!!r?.quadruped};
  root.traverse(o=>{o.geometry?.dispose();if(o.material&&!Array.isArray(o.material))o.material.dispose();});
}
// All face/race/gender and armour combinations; colors are parameters, never baked player data.
for(const race of catalog.races)for(const gender of catalog.genders)for(const face of catalog.faces)
  for(const style of ['leather','mail','plate','tunic'])for(const helmet of [false,true])for(const cape of [false,true]){
    const look={race:race.id,gender:gender.id,face:face.id,classId:'warrior',specializationId:'swordsman',skinColor:'#ffffff',hairColor:'#ffffff'};
    const outfit={style,helmet,cape,primary:'#ffffff',metal:'#ffffff',accent:'#ffffff'};
    store(`avatar/${race.id}/${gender.id}/${face.id}/${style}/${+helmet}/${+cape}`,createAvatar('#ffffff',look,catalog,1,'',{outfit,weapons:{mainHand:null,offHand:null}}));
  }
for(const kind of ['sword','greatsword','dagger','staff','wand','shield','orb','tome'])for(const hands of [1,2])for(const side of [-1,1]){
  const hand=new THREE.Group(),item={kind,hands,primary:'#ccdde1',secondary:'#cca252',glow:'#68e0dc'};
  const linen=new THREE.MeshStandardMaterial({color:'#d9d0b9'});exportWeapon(hand,item,side,{linen});
  hand.traverse(o=>{if(!o.material?.color)return;const hex=o.material.color.getHexString();o.material.userData.role=hex==='ccdde1'?'weaponPrimary':hex==='cca252'?'weaponSecondary':hex==='68e0dc'?'weaponGlow':'';});
  store(`weapon/${kind}/${hands}/${side}`,hand);
}
for(const role of ['guide','smith','tailor','shopkeeper']){const npc={role},a=npcAppearance(npc);store(`npc/${role}`,createAvatar(a.outfit.primary,a.appearance,catalog,1,'',a));}
for(const s of world.monsterSpecies){const root=createCreature(s),base=new THREE.Color(s.color);root.traverse(o=>{if(!o.material?.color)return;for(const [role,factor] of [['creatureSkin',1],['creatureShade',.73],['creatureLight',1.2]])if(o.material.color.equals(base.clone().multiplyScalar(factor)))o.material.userData.role=role;});store(`creature/${s.model}`,root);}
for(const ally of [false,true])for(const defense of [false,true])store(`dummy/${+ally}/${+defense}`,createCreature({model:'dummy',ally,defense,color:'#bda875'}));
for(const element of ['fire','ice','water','earth','light','shadow','physical'])store(`summon/${element}`,createSummon({element}));
for(const crystal of [false,true])for(let i=0;i<6;i++)store(`tree/${+crystal}/${i}`,createTree({crystal,seed:world.seed+i*37,leaves:120}));
for(const crystal of [false,true])for(const kind of ['house','smithy','tailor','shop'])store(`building/${+crystal}/${kind}`,createBuilding(kind,'',{crystal}));
for(const r of world.gathering.resources){const root=createResource(r,{leaves:120,seed:world.seed}).mesh,color=new THREE.Color(r.color);root.traverse(o=>{if(o.material?.color?.equals(color))o.material.userData.role='resourceTint';});store(`resource/${r.id}`,root);}
store('portal',createPortal());
const pack={format:1,source:'Original procedural art from client/src/app; offline export, no JavaScript at runtime.',models,materials,geometries};
const output=resolve(root,'client-java/src/main/resources/art/models.json.gz');await mkdir(dirname(output),{recursive:true});
const compressed=gzipSync(JSON.stringify(pack),{level:9});await writeFile(output,compressed);
const manifest={format:1,models:Object.keys(models).length,geometries:geometries.length,materials:materials.length,bytes:compressed.length,sha256:createHash('sha256').update(compressed).digest('hex'),sourceCommit:'9e893fe',sources:{}};
for(const name of ['avatar-model.js','render-kit.js','environment-model.js','creature-model.js','equipment.mjs'])manifest.sources[name]=createHash('sha256').update(await readFile(resolve(source,name))).digest('hex');
await writeFile(resolve(dirname(output),'manifest.json'),JSON.stringify(manifest,null,2)+'\n');await rm(temporary,{recursive:true,force:true});console.log(JSON.stringify(manifest));
