import * as THREE from 'three';
import {ELEMENT_COLORS} from './creature-model.js';
import {mesh,box,torus,branch,group,random,consolidate} from './render-kit.js';

const glow=(color,opacity=.8)=>new THREE.MeshBasicMaterial({color,transparent:true,opacity,depthWrite:false,side:THREE.DoubleSide,blending:THREE.AdditiveBlending});
export function createSpell(element='physical',radius=1){
 const g=new THREE.Group(),r=Math.max(.7,Math.min(10,radius||.9)),color=ELEMENT_COLORS[element]||ELEMENT_COLORS.light,mat=glow(color,.65),soft=glow(color,.23);g.userData.element=element;g.userData.radius=r;g.userData.particles=[];
 torus(g,mat,0,.09,0,r,.027,true);torus(g,soft,0,.08,0,r*.88,.018,true);
 // Ground boundary is the authoritative radius, never an expanding damaging area.
 for(let i=0;i<12;i++){const a=i/12*Math.PI*2;const rune=box(g,mat,Math.sin(a)*r*.94,.09,Math.cos(a)*r*.94,.08,.015,.16);rune.rotation.y=a;}
 const rng=random(747),count=element==='earth'?10:16;
 for(let i=0;i<count;i++){
  const a=i/count*Math.PI*2,reach=r*(.2+rng()*.72),p=group(g,Math.sin(a)*reach,.12,Math.cos(a)*reach);p.userData.phase=i/count;p.userData.base=p.position.clone();
  if(element==='fire'){branch(p,mat,[[0,0,0],[.10,.23,0],[-.03,.50,0],[.035,.72,.025]],.10,.002,10,6);branch(p,soft,[[0,0,0],[-.12,.27,.04],[.06,.64,.01]],.16,.002,8,6);}
  else if(element==='ice'||element==='earth'){const geo=element==='ice'?new THREE.OctahedronGeometry(.14):new THREE.IcosahedronGeometry(.19,1);const shard=mesh(p,geo,mat,0,.17,0);shard.scale.y=element==='ice'?3:1.1;shard.rotation.z=(rng()-.5)*.6;}
  else if(element==='light'){box(p,mat,0,.15,0,.04,.25,.04);box(p,mat,0,.15,0,.18,.04,.04);}
  else if(element==='water'){torus(p,mat,0,.14,0,.12,.012);}
  else{const spark=mesh(p,new THREE.OctahedronGeometry(.085),mat,0,.15,0);spark.scale.y=2;}
  g.userData.particles.push(p);
 }
 if(element==='water'||element==='shadow'){for(let j=0;j<2;j++){const points=[];for(let i=0;i<=32;i++){const a=i/32*Math.PI*3+j*Math.PI;points.push([Math.sin(a)*r*.66,.12+i/32*.65,Math.cos(a)*r*.66]);}branch(g,soft,points,.025,.003,40,5);}}
 consolidate(g);return g;
}
export function createImpact(color=ELEMENT_COLORS.physical,critical=false){const g=new THREE.Group(),mat=glow(color,.9);for(let j=0;j<2;j++){const slash=mesh(g,new THREE.RingGeometry(.35,.39,24,1,-.3,Math.PI*1.3),mat);slash.rotation.z=j*1.2;slash.scale.setScalar(j?.72:1);}
 for(let i=0;i<(critical?14:8);i++){const a=i*2.4;const spark=box(g,mat,Math.sin(a)*.25,Math.cos(a)*.25,.05,.025,.14,.025);spark.rotation.z=-a;}consolidate(g);return g;}
export function createBolt(start,end,color){const g=new THREE.Group(),mat=glow(color,.8);const path=new THREE.CatmullRomCurve3([start,start.clone().lerp(end,.5).add(new THREE.Vector3(0,.25,0)),end]);mesh(g,new THREE.TubeGeometry(path,16,.035,6,false),mat);const orb=mesh(g,new THREE.SphereGeometry(.11,12,8),glow(color,1));g.userData.path=path;g.userData.orb=orb;return g;}
export function animateEffect(e,time){
 const t=THREE.MathUtils.clamp((time-e.start)/e.duration,0,1),g=e.mesh;
 if(e.kind==='label'){g.position.y=e.baseY+t*1.3;g.material.opacity=1-Math.pow(t,3);return;}
 const materials=new Set();g.traverse(m=>{if(m.material)materials.add(m.material);});for(const mat of materials){if(mat.userData.baseOpacity===undefined)mat.userData.baseOpacity=mat.opacity;mat.opacity=mat.userData.baseOpacity*(1-t*t);}
 if(e.kind==='spell'){
  for(const p of g.userData.particles){const k=p.userData.phase;p.position.copy(p.userData.base);p.position.y+=(g.userData.element==='earth'?.35:1)*Math.sin(t*Math.PI/2)*(1+k*.5);p.scale.setScalar(Math.max(.02,Math.sin(t*Math.PI)));p.rotation.y=t*(g.userData.element==='water'?3:.8);}
 }else if(e.kind==='bolt'){g.userData.orb.position.copy(g.userData.path.getPoint(t));}
 else if(e.kind==='impact'){g.scale.setScalar(.5+t*.8);g.rotation.z=e.angle+t*.4;}
}
