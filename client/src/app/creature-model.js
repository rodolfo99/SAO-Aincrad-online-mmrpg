import * as THREE from 'three';
import {V,tint,random,material,mesh,group,box,ball,cylinder,torus,branch,link,consolidate} from './render-kit.js';

function palette(color){return{skin:material(color||0x788b61,'skin'),shade:material(tint(color||0x788b61,.73),'skin'),light:material(tint(color||0x788b61,1.2),'skin'),mouth:material(0x3f3531,'skin'),ivory:material(0xd6cba8),eye:material(0xe1c071,'plain',{roughness:.22}),pupil:material(0x17262b,'plain',{roughness:.12}),metal:material(0x65747a,'metal'),edge:material(0xb1aaa0,'metal'),leather:material(0x514437,'wood'),wood:material(0x77573a,'wood'),rope:material(0xb2a174,'cloth'),cloth:material(0x655b48,'cloth'),glow:material(0x86d4d7,'plain',{emissive:0x50b5ca,emissiveIntensity:.8,roughness:.22})};}
function eye(g,m,x,y,z,size=.09){
 ball(g,m.shade,x,y,z,size*1.08,size*.56,size*.17,16);ball(g,m.eye,x,y,z+size*.13,size*.78,size*.34,size*.15,16);ball(g,m.pupil,x,y,z+size*.26,size*.22,size*.30,size*.065,12);ball(g,m.ivory,x-size*.19,y+size*.10,z+size*.30,size*.09,size*.08,size*.025,8);
 branch(g,m.skin,[[x-size*.86,y+size*.08,z],[x,y+size*.35,z+size*.24],[x+size*.86,y+size*.08,z]],size*.17,size*.14,12,6);
 branch(g,m.skin,[[x-size*.82,y-size*.08,z],[x,y-size*.30,z+size*.20],[x+size*.82,y-size*.08,z]],size*.085,size*.065,10,6);
}
function ear(g,m,x,y,z,side,size=1){const e=branch(g,m.skin,[[x,y,z],[x+side*.22*size,y+.10*size,z],[x+side*.49*size,y+.21*size,z-.05]],.14*size,.004,12,8);e.scale.z=.46;const inner=branch(g,m.shade,[[x+side*.06,y+.005,z+.035],[x+side*.2*size,y+.09*size,z+.035],[x+side*.4*size,y+.18*size,z]],.075*size,.002,10,7);inner.scale.z=.5;}
function horn(g,m,p,r=.08){return branch(g,m,p,r,.002,16,8);}
function anatomy(g,m,rows,sides=24){
 const c=new THREE.CatmullRomCurve3(rows.map(r=>V(r[1],r[0],r[2])),false,'catmullrom',.35),p=[],uv=[],index=[],steps=(rows.length-1)*4;
 for(let i=0;i<=steps;i++){const v=c.getPoint(i/steps);for(let j=0;j<=sides;j++){const a=j/sides*6.283185; p.push(Math.sin(a)*Math.max(.001,v.x),v.y,Math.cos(a)*Math.max(.001,v.z));uv.push(j/sides,i/steps);if(i&&j){const n=i*(sides+1)+j;index.push(n,n-1,n-sides-2,n,n-sides-2,n-sides-1);}}}
 const geo=new THREE.BufferGeometry();geo.setAttribute('position',new THREE.Float32BufferAttribute(p,3));geo.setAttribute('uv',new THREE.Float32BufferAttribute(uv,2));geo.setIndex(index);geo.computeVertexNormals();return mesh(g,geo,m);
}
function createSlime(color){
 const g=new THREE.Group(),body=group(g),m=palette(color||0x83b55a);g.userData.body=body;g.userData.slime=true;
 const jelly=material(color||0x83b55a,'plain',{roughness:.16,metalness:.08,transparent:true,opacity:.83});
 anatomy(body,jelly,[[.04,.42,.40],[.12,.76,.68],[.45,.83,.73],[.79,.67,.60],[1.04,.40,.38],[1.13,.012,.015]],40);
 ball(body,m.light,-.23,.83,.14,.32,.12,.30,24);const core=ball(body,m.glow,0,.42,-.05,.20,.19,.17,20);core.material.emissiveIntensity=.22;
 for(const x of [-.26,.26])eye(body,m,x,.66,.665,.105);
 branch(body,m.mouth,[[-.14,.41,.716],[0,.37,.75],[.14,.41,.716]],.016,.016,14,7);
 for(let i=0;i<9;i++){const a=i*2.4;ball(body,jelly,Math.sin(a)*.42,.2+(i%3)*.16,Math.cos(a)*.44,.035+(i%2)*.02,.05,.035,10);}
 g.userData.labelHeight=1.55;consolidate(g);return g;
}
function quadruped(kind,color){const boar=kind==='boar',g=new THREE.Group(),body=group(g),m=palette(color||(boar?0x795340:0x8c8270));g.userData.body=body;
 ball(body,m.skin,0,boar?.89:.91,-.10,boar?.54:.34,boar?.59:.40,boar?.84:.78,28);
 ball(body,m.skin,0,boar?1.0:1.08,.45,boar?.47:.31,boar?.52:.43,.45,24);
 const head=group(body,0,boar?1.05:1.31,boar?.66:.60);head.rotation.x=boar?.17:.12;ball(head,m.skin,0,0,0,boar?.38:.29,boar?.40:.32,boar?.42:.34,24);
 ball(head,boar?m.light:m.light,0,-.12,.34,boar?.27:.20,boar?.20:.15,boar?.33:.34,24);ball(head,boar?m.shade:m.mouth,0,-.10,.60,boar?.24:.16,.12,.10,20);
 for(const side of [-1,1]){
  if(boar){ball(head,m.mouth,side*.1,-.10,.69,.044,.047,.013,12);horn(head,m.ivory,[[side*.26,-.25,.38],[side*.41,-.23,.55],[side*.42,.03,.69],[side*.32,.16,.68]],.09);}
  eye(head,m,side*(boar?.27:.18),.085,boar?.30:.25,.052);
  branch(head,m.shade,[[side*.11,.14,boar?.34:.26],[side*.22,.17,boar?.30:.23],[side*.29,.10,.19]],.037,.012,8,7);
  const ears=group(head,side*(boar?.27:.20),.22,-.04);ears.rotation.z=-side*.24;horn(ears,m.skin,[[0,0,0],[side*.04,.18,0],[side*.12,boar?.31:.37,-.04]],boar?.145:.12).scale.z=.42;horn(ears,m.shade,[[0,.04,.045],[side*.035,.17,.035],[side*.095,boar?.26:.32,-.005]],.070).scale.z=.45;
 }
 branch(head,m.mouth,[[-.15,-.23,.53],[0,-.24,.64],[.15,-.23,.53]],.012,.012,12,6);
 const legs=[];for(const side of [-1,1])for(const front of [true,false]){
  const leg=group(body,side*(boar?.36:.23),.66,front?.48:-.56),lower=group(leg,0,-.30,front?.03:-.08);
  ball(leg,m.skin,0,-.08,0,boar?.17:.12,.27,.18,16);ball(lower,m.shade,0,-.10,.04,boar?.11:.075,.24,.10,16);ball(lower,boar?m.mouth:m.skin,0,-.27,.10,boar?.15:.12,.10,.2,16);
  for(let j=0;j<(boar?2:3);j++){const dx=(j-(boar?.5:1))*.06;ball(lower,m.mouth,dx,-.28,.22,.04,.065,.055,10);if(!boar)horn(lower,m.ivory,[[dx,-.28,.23],[dx,-.29,.29],[dx,-.31,.31]],.016);}
  legs.push({joint:leg,knee:lower,phase:(side===1?0:Math.PI)+(front?0:Math.PI)});
 }
 const tail=group(body,0,1,-.78);branch(tail,boar?m.shade:m.skin,[[0,0,0],[.05,.16,-.26],[.11,boar?.25:.37,-.47],[boar?.16:.08,boar?.17:.43,-.64]],boar?.033:.105,.004,20,8);
 // Directional mane and cheek tufts rather than a faceted coat.
 const rng=random(boar?55:78);for(let i=0;i<(boar?36:23);i++){const z=-.7+rng()*1.06,x=(rng()-.5)*.4,y=boar?1.35:1.21;horn(body,boar?m.shade:m.light,[[x,y,z],[x*.9,y+.08,z-.08],[x*.8,y+(boar?.14:.11),z-.19]],.024);}
 if(boar)for(let i=0;i<4;i++){const shard=mesh(body,new THREE.OctahedronGeometry(.15,0),m.glow,0,1.48,-.57+i*.28);shard.scale.set(.7,1.5,1);}
 g.userData.creatureRig={legs,arms:[],head,tail,quadruped:true};g.userData.labelHeight=boar?2:2.05;consolidate(g);return g;
}
function humanoid(kind,color){
 const troll=kind==='troll',goblin=kind==='goblin',guardian=kind==='guardian',g=new THREE.Group(),body=group(g),m=palette(color||(troll?0x858877:goblin?0x759451:guardian?0x577985:0x71835a));g.userData.body=body;
 if(guardian)m.metal.color.set(color||0x577985);
 const width=troll?.67:guardian?.53:goblin?.34:.5,skin=guardian?m.metal:m.skin;
 anatomy(body,skin,[[1.0,width*.68,.24],[1.16,width*.87,.30],[1.45,width,.34],[1.9,width*1.14,.39],[2.15,width*.93,.30],[2.25,.21,.19]],28);
 ball(body,skin,0,2.24,0,.22,.22,.22,20);const head=group(body,0,2.58,.05);
 ball(head,skin,0,0,0,goblin?.34:.32,troll?.36:.35,.29,28);ball(head,skin,0,-.19,.11,troll?.35:.28,.22,.25,24);
 if(guardian){
  ball(head,m.metal,0,.08,0,.36,.34,.32,24);box(head,m.mouth,0,.015,.285,.48,.065,.06,.025);for(const side of [-1,1]){box(head,m.glow,side*.13,.02,.322,.17,.028,.012);horn(head,m.edge,[[side*.29,.20,0],[side*.46,.46,-.06],[side*.42,.67,-.12]],.075);}box(head,m.edge,0,.16,.30,.05,.34,.055,.014);
 }else{
  ball(head,m.shade,0,-.14,.28,.21,.066,.055,20);branch(head,m.mouth,[[-.20,-.17,.30],[0,-.21,.34],[.20,-.17,.30]],.014,.014,18,7);
  // Broad muzzle, nostrils, eyelids, brows and curved lower tusks.
  ball(head,m.skin,0,-.015,.285,goblin?.12:.14,.13,goblin?.24:.13,20);for(const side of [-1,1]){ball(head,m.shade,side*.072,-.075,goblin?.46:.38,.037,.026,.027,10);eye(head,m,side*.157,.09,.247,goblin?.085:.073);branch(head,m.shade,[[side*.05,.12,.285],[side*.16,.18,.29],[side*.27,.18,.23]],.048,.021,10,8);ear(head,m,side*.29,.04,0,side,goblin?1.15:.62);horn(head,m.ivory,[[side*.19,-.22,.31],[side*.22,-.10,.36],[side*.20,troll?.13:.035,.40]],troll?.068:.042);}
  for(const side of [-1,1])ball(head,m.skin,side*.205,-.03,.215,.095,.055,.042,16);
  for(let i=0;i<3;i++)branch(head,m.shade,[[-.18,.23+i*.032,.22],[-.02,.25+i*.035,.29],[.16,.23+i*.032,.22]],.005,.005,12,5);
 }
 const legs=[],arms=[];for(const side of [-1,1]){
  const leg=group(body,side*width*.58,1.05,0),knee=group(leg,0,-.46,.025);ball(leg,skin,0,-.20,0,troll?.26:.18,.33,.23,20);ball(knee,skin,0,-.20,.005,troll?.19:.13,.29,.15,20);ball(knee,guardian?m.metal:m.leather,0,-.45,.13,troll?.25:.18,.12,.32,20);
  if(!goblin){anatomy(knee,guardian?m.metal:m.leather,[[-.43,.18,.17],[-.3,.18,.15],[-.05,.20,.17],[.02,.12,.13]],16);box(knee,m.edge,0,-.2,.17,.09,.3,.04,.02);}
  legs.push({joint:leg,knee,phase:side===1?0:Math.PI});
  const arm=group(body,side*(width+.11),2.08,0);arm.rotation.z=-side*.12;ball(arm,skin,0,-.17,0,troll?.24:.17,.31,.20,20);const elbow=group(arm,0,-.48,0);ball(elbow,skin,0,-.16,.02,troll?.21:.135,.26,.16,20);ball(elbow,skin,0,-.40,.04,troll?.21:.14,.17,.13,20);
  for(let j=0;j<3;j++)branch(elbow,skin,[[(j-1)*.066,-.41,.12],[(j-1)*.066,-.54,.12],[(j-1)*.066,-.56,.065]],.034,.023,8,7);
  cylinder(elbow,m.leather,0,-.25,0,troll?.22:.151,troll?.23:.16,.19,18);torus(elbow,m.edge,0,-.30,0,troll?.228:.16,.018,true);arms.push({joint:arm,elbow,side});
  if(!goblin){const armor=ball(arm,guardian?m.edge:m.metal,side*.035,.02,-.015,troll?.32:.27,.18,.32,20);for(let i=0;i<3;i++)ball(arm,m.edge,side*(.11+i*.06),.13-i*.025,.21,.022,.022,.022,8);if(!guardian)horn(arm,m.ivory,[[side*.08,.14,-.03],[side*.16,.38,-.08],[side*.18,.43,-.11]],.065);}
 }
 // Belts, overlapping skirt panels and armour follow the torso's curved silhouette.
 anatomy(body,m.leather,[[.99,width*.77,.27],[1.12,width*.85,.31],[1.18,width*.89,.32]],28);box(body,m.edge,0,1.095,.322,.19,.16,.06,.022);
 for(let j=0;j<7;j++){const a=(j-3)*.42,x=Math.sin(a)*width*.87,z=Math.cos(a)*.34;const plate=box(body,guardian?m.metal:m.cloth,x,.88,z,.20,.35,.06,.024);plate.rotation.y=a;plate.rotation.z=-Math.sin(a)*.10;}
 if(guardian||kind==='orc'){
  ball(body,m.metal,0,1.74,.29,width*.91,.38,.18,24);for(const side of [-1,1]){branch(body,m.edge,[[side*.06,2.03,.37],[side*.28,1.93,.46],[side*width*.82,1.70,.37]],.026,.018,15,7);box(body,m.leather,side*width*.57,1.48,.34,.06,.34,.06);}
  if(guardian){const core=mesh(body,new THREE.OctahedronGeometry(.17,1),m.glow,0,1.79,.485);core.scale.y=1.3;torus(body,m.edge,0,1.79,.49,.23,.035);for(const side of [-1,1])box(body,m.glow,side*.27,1.53,.425,.04,.20,.025,.01);}
 }else if(goblin){const strap=box(body,m.leather,0,1.73,.35,.14,.90,.055,.02);strap.rotation.z=-.48;ball(body,m.leather,-.30,1.20,.24,.14,.20,.12,16);for(let i=0;i<9;i++)horn(head,m.shade,[[(i-4)*.035,.26,-.04],[(i-4)*.04,.46,-.06],[(i-4)*.03,.57,-.12]],.029);}
 else{
  const moss=material(0x637654,'skin');for(let j=0;j<10;j++){const a=j*2.4,x=Math.sin(a)*.51,y=1.35+(j%4)*.2,z=-.26;const rock=mesh(body,new THREE.IcosahedronGeometry(.22,1),j%3?m.shade:moss,x,y,z);rock.scale.set(1.2,.8,.72);}for(let j=0;j<6;j++)ball(head,m.shade,Math.sin(j*2.4)*.22,.30,-.05+Math.cos(j*2.4)*.16,.09,.07,.08,12);
 }
 const weapon=group(arms[1].elbow,0,-.43,.08);
 if(troll){branch(weapon,m.wood,[[0,0,0],[0,.0,.65],[.04,.03,1.43]],.08,.16,16,10);for(const z of [1.02,1.31]){const band=torus(weapon,m.metal,.025,.02,z,.17,.035);band.rotation.y=0;}for(const side of [-1,1])horn(weapon,m.ivory,[[side*.12,.02,1.15],[side*.28,.05,1.20],[side*.35,.08,1.27]],.05);}
 else if(kind==='orc'){link(weapon,m.wood,[0,0,-.13],[0,0,1.18],.06);const shape=new THREE.Shape();shape.moveTo(-.06,.65);shape.lineTo(-.40,.52);shape.quadraticCurveTo(-.65,.97,-.34,1.18);shape.lineTo(-.05,1.02);shape.closePath();const blade=mesh(weapon,new THREE.ExtrudeGeometry(shape,{depth:.06,bevelEnabled:true,bevelSize:.025,bevelThickness:.02,bevelSegments:1,steps:1}),m.metal);blade.rotation.x=Math.PI/2;blade.position.y=.03;}
 else{const long=guardian?1.27:.58;box(weapon,m.leather,0,0,.14,.075,.075,.28,.02);box(weapon,m.edge,0,0,.30,guardian?.48:.24,.05,.07,.02);const shape=new THREE.Shape();shape.moveTo(-.07,.33);shape.lineTo(-.055,long);shape.lineTo(0,long+.19);shape.lineTo(.055,long);shape.lineTo(.07,.33);shape.closePath();const blade=mesh(weapon,new THREE.ExtrudeGeometry(shape,{depth:.026,bevelEnabled:true,bevelSize:.008,bevelThickness:.008,bevelSegments:1,steps:1}),m.edge);blade.rotation.x=Math.PI/2;blade.position.y=.014;}
 const shield=group(arms[0].elbow,-.08,-.17,.18);if(goblin||guardian){const r=guardian?.36:.28;const disk=cylinder(shield,guardian?m.metal:m.wood,0,0,0,r,r,.09,24);disk.rotation.x=Math.PI/2;torus(shield,m.edge,0,0,.06,r,.025);ball(shield,m.edge,0,0,.07,.09,.09,.06,14);}
 g.userData.creatureRig={legs,arms,head,quadruped:false};g.userData.swordArm=arms[1].joint;
 if(goblin)g.scale.setScalar(.68);else if(troll)g.scale.set(1.28,1.30,1.25);else if(guardian)g.scale.setScalar(1.20);
 g.userData.labelHeight=3.2;consolidate(g);return g;
}
function dummy(p){const g=new THREE.Group(),body=group(g),m=palette(p.ally?0x86a681:0xc2a26c);g.userData.body=body;
 cylinder(g,m.wood,0,.075,0,.56,.64,.15,20);link(body,m.wood,[0,.1,0],[0,2.15,0],.075);link(body,m.wood,[-.89,1.53,0],[.89,1.53,0],.073);
 anatomy(body,p.defense?m.metal:m.skin,[[.90,.26,.18],[1.1,.40,.28],[1.50,.42,.29],[1.7,.30,.22]],24);ball(body,m.skin,0,2.02,0,.28,.31,.26,24);
 for(const x of [-.095,.095]){branch(body,m.mouth,[[x-.03,2.09,.25],[x+.03,2.03,.26]],.012,.012,4,5);branch(body,m.mouth,[[x+.03,2.09,.25],[x-.03,2.03,.26]],.012,.012,4,5);}
 branch(body,m.mouth,[[-.095,1.9,.235],[0,1.86,.26],[.095,1.9,.235]],.009,.009,10,5);
 for(const y of [1.02,1.60]){const b=torus(body,m.rope,0,y,0,.38,.023,true);b.scale.y=.72;}
 for(let i=0;i<20;i++){const a=i/20*6.28;branch(body,m.rope,[[Math.sin(a)*.24,1.0,Math.cos(a)*.18],[Math.sin(a)*.32,.88,Math.cos(a)*.24],[Math.sin(a)*.34,.79,Math.cos(a)*.26]],.008,.002,5,5);}
 if(p.defense)for(const side of [-1,1])ball(body,m.metal,side*.48,1.58,0,.22,.14,.25,16);
 const target=material(p.ally?0xb5d98f:0xa76549,'cloth');torus(body,target,0,1.34,.30,.22,.029);if(p.ally){box(body,m.ivory,0,1.34,.33,.065,.29,.035);box(body,m.ivory,0,1.34,.33,.29,.065,.035);}else ball(body,target,0,1.34,.315,.06,.06,.018,12);
 g.userData.labelHeight=2.55;consolidate(g);return g;
}
export function createCreature(p){switch(p.model||p.kind){case 'dummy':return dummy(p);case 'slime':return createSlime(p.color);case 'dog':case 'boar':return quadruped(p.model||p.kind,p.color);case 'orc':case 'goblin':case 'troll':return humanoid(p.model||p.kind,p.color);default:return humanoid('guardian',p.color);}}
export const ELEMENT_COLORS={fire:0xf3a154,ice:0xb2e8f1,water:0x66c9ce,earth:0xb5a17b,light:0xddecae,shadow:0xb08fda,physical:0xe9d3a2};
export function createSummon(p){const g=new THREE.Group(),body=group(g),color=ELEMENT_COLORS[p.element]||ELEMENT_COLORS.shadow,m=palette(color);g.userData.body=body;const crystal=material(color,'plain',{emissive:color,emissiveIntensity:.75,metalness:.2,roughness:.2});
 const core=mesh(body,new THREE.OctahedronGeometry(.39,0),crystal,0,1.12,0);core.scale.y=1.5;for(const side of [-1,1]){const a=mesh(body,new THREE.OctahedronGeometry(.17,0),m.metal,side*.48,1,0);a.rotation.z=-side*.35;ball(body,m.glow,side*.095,1.20,.30,.045,.029,.028,12);}for(let i=0;i<3;i++){const r=torus(body,crystal,0,.74+i*.36,0,.47,.017,true);r.rotation.z=(i-1)*.25;}
 g.userData.labelHeight=2.15;g.userData.summon=true;consolidate(g);return g;
}
export function animateCreature(g,time,moving=false,attack=0){const body=g.userData.body;if(!body)return;const rig=g.userData.creatureRig;
 if(g.userData.slime){const pulse=Math.sin(time*(moving?9:3));body.scale.set(1+pulse*.05,1-pulse*.09,1+pulse*.05);body.position.y=Math.max(0,pulse)*(moving?.14:.025);return;}
 if(g.userData.summon){body.position.y=Math.sin(time*2)*.12;body.rotation.y=Math.sin(time*.9)*.25;return;}
 if(!rig){body.rotation.z=Math.sin(attack*Math.PI)*.09;return;}
 body.position.y=moving?Math.sin(time*(rig.quadruped?20:16))*.022:Math.sin(time*2)*.009;
 rig.legs.forEach(({joint,knee,phase})=>{const cycle=Math.sin(time*(rig.quadruped?10:8)+phase);joint.rotation.x=moving?cycle*.38:0;knee.rotation.x=moving?Math.max(0,-cycle)*.47:0;});
 rig.arms.forEach(({joint,elbow,side})=>{joint.rotation.x=moving?-Math.sin(time*8+(side===1?0:Math.PI))*.25:Math.sin(time*2)*.025;elbow.rotation.x=-.1;if(side===1&&attack>0){joint.rotation.x=-Math.sin(attack*Math.PI)*1.65;elbow.rotation.x=-Math.sin(attack*Math.PI)*.45;}});
 rig.head.rotation.y=Math.sin(time*1.15)*.035;if(rig.tail)rig.tail.rotation.y=Math.sin(time*(moving?8:3))*.22;
}
