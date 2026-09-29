import * as THREE from 'three';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';
import { RoundedBoxGeometry } from 'three/addons/geometries/RoundedBoxGeometry.js';

// Small original modelling primitives, shared by scenery and creatures.
export const V=(x,y,z)=>new THREE.Vector3(x,y,z);
export const tint=(c,n)=>new THREE.Color(c).multiplyScalar(n);
export function random(seed=1){let s=seed>>>0;return()=>{s=(1664525*s+1013904223)>>>0;return s/4294967296;};}
export function material(color,kind='stone',extra={}){
  const m=new THREE.MeshStandardMaterial({color,roughness:kind==='metal'?.38:kind==='skin'?.69:.88,metalness:kind==='metal'?.7:0,...extra});
  if(kind==='plain')return m;
  m.onBeforeCompile=s=>{
    s.vertexShader=s.vertexShader.replace('#include <common>','#include <common>\nvarying vec3 detailPosition;').replace('#include <begin_vertex>','#include <begin_vertex>\ndetailPosition=position;');
    s.fragmentShader=s.fragmentShader.replace('#include <common>',`#include <common>
varying vec3 detailPosition;
float grainHash(vec3 p){return fract(sin(dot(p,vec3(12.9898,78.233,45.164)))*43758.5453);}
float grainNoise(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(mix(grainHash(i),grainHash(i+vec3(1,0,0)),f.x),mix(grainHash(i+vec3(0,1,0)),grainHash(i+vec3(1,1,0)),f.x),f.y),mix(mix(grainHash(i+vec3(0,0,1)),grainHash(i+vec3(1,0,1)),f.x),mix(grainHash(i+vec3(0,1,1)),grainHash(i+vec3(1,1,1)),f.x),f.y),f.z);}`)
      .replace('#include <color_fragment>',`#include <color_fragment>
float grains=grainNoise(detailPosition*${kind==='wood'?'vec3(15.0,1.3,15.0)':kind==='cloth'?'90.0':kind==='skin'?'55.0':'17.0'});
float fineGrain=grainNoise(detailPosition*120.0);
diffuseColor.rgb*=1.0+${kind==='wood'?'0.24':kind==='stone'?'0.20':'0.06'}*(grains-.5)+.025*(fineGrain-.5);`)
      .replace('#include <roughnessmap_fragment>','#include <roughnessmap_fragment>\nroughnessFactor=clamp(roughnessFactor+(grains-.5)*.12,.12,1.0);');
  };
  m.customProgramCacheKey=()=>`world-surface-${kind}`;
  return m;
}
export function mesh(g,geo,mat,x=0,y=0,z=0){const m=new THREE.Mesh(geo,mat);m.position.set(x,y,z);m.castShadow=true;m.receiveShadow=true;g.add(m);return m;}
export function group(g,x=0,y=0,z=0){const a=new THREE.Group();a.position.set(x,y,z);g.add(a);return a;}
export function box(g,m,x,y,z,w,h,d,bevel=0){return mesh(g,bevel?new RoundedBoxGeometry(w,h,d,1,Math.min(bevel,w/3,h/3,d/3)):new THREE.BoxGeometry(w,h,d),m,x,y,z);}
export function ball(g,m,x,y,z,w,h=w,d=w,segments=16){const a=mesh(g,new THREE.SphereGeometry(1,segments,Math.max(8,Math.floor(segments*.65))),m,x,y,z);a.scale.set(w,h,d);return a;}
export function cylinder(g,m,x,y,z,top,bottom,height,sides=16){return mesh(g,new THREE.CylinderGeometry(top,bottom,height,sides),m,x,y,z);}
export function torus(g,m,x,y,z,r,thick=.03,horizontal=false){const a=mesh(g,new THREE.TorusGeometry(r,thick,6,40),m,x,y,z);if(horizontal)a.rotation.x=Math.PI/2;return a;}
export function branch(g,m,points,start=.1,end=.005,steps=14,sides=8){
 const curve=new THREE.CatmullRomCurve3(points.map(p=>V(...p))),frames=curve.computeFrenetFrames(steps,false),positions=[],uv=[],index=[];
 for(let i=0;i<=steps;i++){const p=curve.getPointAt(i/steps),r=THREE.MathUtils.lerp(start,end,i/steps);for(let j=0;j<=sides;j++){const a=j/sides*Math.PI*2,v=p.clone().addScaledVector(frames.normals[i],Math.cos(a)*r).addScaledVector(frames.binormals[i],Math.sin(a)*r);positions.push(v.x,v.y,v.z);uv.push(j/sides,i/steps);if(i&&j){const n=i*(sides+1)+j;index.push(n,n-1,n-sides-2,n,n-sides-2,n-sides-1);}}}
 const geo=new THREE.BufferGeometry();geo.setAttribute('position',new THREE.Float32BufferAttribute(positions,3));geo.setAttribute('uv',new THREE.Float32BufferAttribute(uv,2));geo.setIndex(index);geo.computeVertexNormals();return mesh(g,geo,m);
}
export function link(g,m,a,b,width=.1){const p=V(...a),q=V(...b),obj=cylinder(g,m,0,0,0,width,width,p.distanceTo(q),10);obj.position.copy(p).add(q).multiplyScalar(.5);obj.quaternion.setFromUnitVectors(V(0,1,0),q.sub(p).normalize());return obj;}

// Merge within an articulated joint, never across joints or interactive roots.
export function consolidate(g){
 for(const c of [...g.children])if(c.isGroup&&!c.userData.unbatched)consolidate(c);
 const batches=new Map();for(const c of [...g.children])if(c.isMesh&&!c.isInstancedMesh&&!c.userData.unbatched&&!Array.isArray(c.material)){const key=c.material.uuid;if(!batches.has(key))batches.set(key,[]);batches.get(key).push(c);}
 for(const list of batches.values())if(list.length>1){const all=list.map(c=>{c.updateMatrix();let geo=c.geometry.clone();if(geo.index){const expanded=geo.toNonIndexed();geo.dispose();geo=expanded;}geo.applyMatrix4(c.matrix);return geo;});const merged=mergeGeometries(all);all.forEach(a=>a.dispose());if(merged){mesh(g,merged,list[0].material);for(const c of list){g.remove(c);c.geometry.dispose();}}}
}

// Spatial batches keep frustum culling effective. Leaf sprays remain GPU instances.
// Use only on the non-interactive scenery group; resource/node and entity roots stay intact.
export function bakeScenery(root,cellSize=20){
 root.updateMatrixWorld(true);const inverse=root.matrixWorld.clone().invert(),batches=new Map(),originals=new Set();
 root.traverse(o=>{if(!o.isMesh||Array.isArray(o.material))return;const p=o.getWorldPosition(new THREE.Vector3()),cell=`${Math.floor(p.x/cellSize)}:${Math.floor(p.z/cellSize)}`;
 const key=cell+':'+o.material.uuid+(o.isInstancedMesh?':i:'+o.geometry.uuid:':m');if(!batches.has(key))batches.set(key,[]);batches.get(key).push(o);originals.add(o.geometry);});
 const baked=new THREE.Group();
 for(const list of batches.values()){
  const first=list[0];if(first.isInstancedMesh){const count=list.reduce((n,o)=>n+o.count,0),m=new THREE.InstancedMesh(first.geometry.clone(),first.material,count),local=new THREE.Matrix4(),color=new THREE.Color();let i=0;
   for(const o of list){const base=inverse.clone().multiply(o.matrixWorld);for(let j=0;j<o.count;j++){o.getMatrixAt(j,local);m.setMatrixAt(i,base.clone().multiply(local));if(o.instanceColor)o.getColorAt(j,color);else color.set(0xffffff);m.setColorAt(i++,color);}}
   m.castShadow=first.castShadow;m.receiveShadow=first.receiveShadow;m.computeBoundingSphere();baked.add(m);
  }else{const all=list.map(o=>{let geo=o.geometry.clone();if(geo.index){const e=geo.toNonIndexed();geo.dispose();geo=e;}geo.applyMatrix4(inverse.clone().multiply(o.matrixWorld));return geo;});const geo=mergeGeometries(all);all.forEach(g=>g.dispose());if(geo){const m=mesh(baked,geo,first.material);m.castShadow=first.castShadow;m.receiveShadow=first.receiveShadow;}}
 }
 root.clear();root.add(baked);originals.forEach(g=>g.dispose());return root;
}
