import {floorLayout} from './floor-layout.mjs';
import * as THREE from 'three';
import { V, tint, random, material, mesh, group, box, ball, cylinder, torus, branch, link, consolidate, bakeScenery } from './render-kit.js';

export const GRAPHICS={
 low:{name:'Ligera',pixelRatio:1,shadows:false,shadowSize:1024,leaves:38,grass:1000,particles:40},
 balanced:{name:'Equilibrada',pixelRatio:1.25,shadows:true,shadowSize:1536,leaves:72,grass:3200,particles:90},
 high:{name:'Alta',pixelRatio:1.75,shadows:true,shadowSize:2048,leaves:120,grass:6000,particles:160}
};
export function readGraphics(){try{const v=localStorage.getItem('aincrad-graphics-v1');return GRAPHICS[v]?v:'balanced';}catch{return 'balanced';}}
function palette(crystal=false){return{
 bark:material(crystal?0x6b7470:0x61503c,'wood'),wood:material(0x73513a,'wood'),cutWood:material(0xb59c71,'wood'),
 stone:material(crystal?0x70888c:0x90928a),darkStone:material(crystal?0x47576b:0x656963),lightStone:material(0xc0baaa),
 plaster:material(0xd0c4a3),tile:material(0x9e5236),tileDark:material(0x744936),metal:material(0x6a7980,'metal'),gold:material(0xb99a55,'metal'),
 linen:material(0xc9bd99,'cloth'),leaves:material(crystal?0x527c88:0x4d6b37,'plain',{side:THREE.DoubleSide}),
 glow:material(0x9ae3e1,'plain',{emissive:0x5bd8e4,emissiveIntensity:.7,roughness:.22,metalness:.2}),
 window:material(0xf1cf8c,'plain',{emissive:0xffb856,emissiveIntensity:.4,roughness:.3}),
 grass:material(crystal?0x608c8d:0x607a39,'plain',{side:THREE.DoubleSide})
};}

function sprayGeometry(){
 const p=[],uv=[],ix=[];for(let j=0;j<6;j++){const a=j*2.399,r=.24+(j%3)*.12,cx=Math.cos(a)*r,cz=Math.sin(a)*r,y=(j%2)*.14;
  const points=[[0,0,-.26],[-.15,.025,-.06],[-.12,.03,.16],[0,.065,.38],[.12,.03,.16],[.15,.025,-.06],[0,.09,.03]];
  for(const [x,dy,z]of points){p.push(cx+x*Math.cos(a)-z*Math.sin(a),y+dy,cz+x*Math.sin(a)+z*Math.cos(a));uv.push(x+.5,z+.5);}for(let k=0;k<6;k++)ix.push(j*7+6,j*7+k,j*7+(k+1)%6);
 }
 const geo=new THREE.BufferGeometry();geo.setAttribute('position',new THREE.Float32BufferAttribute(p,3));geo.setAttribute('uv',new THREE.Float32BufferAttribute(uv,2));geo.setIndex(ix);geo.computeVertexNormals();return geo;
}
function crystalShard(g,m,x,y,z,scale=1,angle=0){
 const points=[[0,-.7],[.27,-.4],[.29,.53],[0,1]].map(p=>new THREE.Vector2(...p));const o=mesh(g,new THREE.LatheGeometry(points,6),m,x,y,z);o.scale.setScalar(scale);o.rotation.z=angle;return o;
}
function stone(g,p,x,y,z,sx,sy,sz,seed=1){
 const geo=new THREE.IcosahedronGeometry(1,2),pos=geo.attributes.position;
 for(let i=0;i<pos.count;i++){const x=pos.getX(i),y=pos.getY(i),z=pos.getZ(i),n=1+.11*Math.sin(x*7+seed)*Math.sin(z*5-y*4);pos.setXYZ(i,x*n,y*n,z*n);}geo.computeVertexNormals();const m=mesh(g,geo,p,x,y,z);m.scale.set(sx,sy,sz);return m;
}

export function createTree({crystal=false,pine=false,color=null,seed=1,leaves=72,p=null}={}){
 p=p||palette(crystal);const g=new THREE.Group(),rng=random(seed);
 branch(g,p.bark,[[0,0,0],[.13,1.5,.07],[-.05,3.1,-.05],[.16,4.7,0]],.42,.07,24,12);
 for(let j=0;j<5;j++){const a=j*1.256+.2;branch(g,p.bark,[[0,.28,0],[Math.sin(a)*.45,.13,Math.cos(a)*.45],[Math.sin(a)*.64,.02,Math.cos(a)*.64]],.13,.025,8,7);}
 const leafMat=color?material(color,'plain',{side:THREE.DoubleSide}):p.leaves;
 const sprays=new THREE.InstancedMesh(p.leafGeo||sprayGeometry(),leafMat,leaves),dummy=new THREE.Object3D();
 for(let j=0;j<(pine?10:7);j++){const a=j*2.399,h=pine?1.9+j*.25:2.2+(j%3)*.7,r=pine?1.5-(h-2)*.35:1.3+(j%2)*.3;branch(g,p.bark,[[0,h-.4,0],[Math.sin(a)*r*.5,h+.15,Math.cos(a)*r*.5],[Math.sin(a)*r,h+.7,Math.cos(a)*r]],.13,.012,12,8);}
 for(let i=0;i<leaves;i++){
  const a=rng()*Math.PI*2,u=rng(),r=Math.sqrt(rng());let y,reach;
  if(pine){y=1.9+u*3.8;reach=(5.9-y)*.48*r;}else{y=3+u*2.8;reach=Math.sin(u*Math.PI)*1.95*r+.1;}
  dummy.position.set(Math.sin(a)*reach,y,Math.cos(a)*reach);dummy.rotation.set((rng()-.5)*1.4,a,(rng()-.5)*1.2);dummy.scale.setScalar((pine?.68:.95)+rng()*.45);dummy.updateMatrix();sprays.setMatrixAt(i,dummy.matrix);sprays.setColorAt(i,new THREE.Color().setRGB(.75+rng()*.35,.85+rng()*.3,.7+rng()*.3));
 }
 sprays.castShadow=true;sprays.receiveShadow=true;g.add(sprays);return g;
}
function barrel(g,p,x,z,scale=1){const b=group(g,x,0,z);b.scale.setScalar(scale);const rows=[[0,.28],[.08,.32],[.4,.38],[.73,.32],[.78,.29]].map(([y,r])=>new THREE.Vector2(r,y));mesh(b,new THREE.LatheGeometry(rows,16),p.wood);for(const y of [.12,.64])torus(b,p.metal,0,y,0,.34,.028,true);cylinder(b,p.cutWood,0,.78,0,.29,.29,.035,16);for(let j=0;j<8;j++){const a=j*Math.PI/4;link(b,p.darkStone,[Math.sin(a)*.324,.15,Math.cos(a)*.324],[Math.sin(a)*.34,.63,Math.cos(a)*.34],.008);}return b;}
function crate(g,p,x,y,z,s=.6){box(g,p.wood,x,y,z,s,s,s,.025);for(const dx of [-.24,.24])box(g,p.cutWood,x+dx*s/.6,y,z+s*.51,.06,s,.025);for(const dy of [-.24,.24])box(g,p.cutWood,x,y+dy*s/.6,z+s*.51,s,.06,.025);const slat=box(g,p.cutWood,x,y,z+s*.535,.06,s*1.12,.025);slat.rotation.z=-.65;}
function lantern(g,p,x,y,z){box(g,p.metal,x,y,z,.22,.36,.2,.025);box(g,p.window,x,y,z+.11,.15,.26,.02);cylinder(g,p.metal,x,y+.25,z,.035,.19,.12,6);torus(g,p.metal,x,y+.36,z,.065,.012);}

export function createBuilding(kind='house',name='',{crystal=false,p=null}={}){
 p=p||palette(crystal);const g=new THREE.Group();g.userData.name=name;
 // Ground silhouette stays inside the server's 3.1 m collision circle.
 box(g,p.darkStone,0,.17,0,4.5,.34,3.7,.1);box(g,p.plaster,0,1.73,0,4.3,3.15,3.5,.045);
 for(let row=0;row<2;row++)for(let i=0;i<9;i++)box(g,i%3?p.stone:p.lightStone,-2.01+i*.50+(row%2)*.04,.34+row*.27,1.77,.47,.245,.12,.025);
 for(const x of [-2.13,0,2.13])for(const z of [-1.78,1.78])box(g,p.wood,x,1.8,z,.16,3.45,.18,.02);
 for(const y of [.78,2.75,3.27])for(const z of [-1.79,1.79])box(g,p.wood,0,y,z,4.4,.14,.16,.015);
 for(const x of [-2.18,2.18])for(const y of [.78,2.75,3.27])box(g,p.wood,x,y,0,.13,.14,3.6,.012);
 for(const x of [-1.45,1.45])link(g,p.wood,[x-.45,.88,1.84],[x+.45,2.65,1.84],.065);
 // Gable roof, overlapping clay courses, individual ridge caps and carved fascias.
 for(const side of [-1,1]){const roof=box(g,p.tileDark,side*1.24,4.02,0,2.98,.14,4.4);roof.rotation.z=-side*.54;
  for(let row=0;row<7;row++)for(let col=0;col<14;col++){const x=side*(.15+row*.37),y=4.77-row*.223,z=-2.08+col*.32+(row%2)*.03;const t=box(g,(row+col)%5?p.tile:p.tileDark,x,y,z,.45,.09,.35,.025);t.rotation.z=-side*.54;}
  link(g,p.wood,[0,4.85,2.22],[side*2.57,3.3,2.22],.105);link(g,p.wood,[0,4.85,-2.22],[side*2.57,3.3,-2.22],.105);
 }
 for(let i=0;i<15;i++)cylinder(g,p.tileDark,0,4.83,-2.12+i*.3,.12,.12,.32,10).rotation.x=Math.PI/2;
 // Filled triangular gables with a small circular attic vent.
 const tri=new THREE.Shape();tri.moveTo(-2.12,3.25);tri.lineTo(2.12,3.25);tri.lineTo(0,4.63);tri.closePath();
 for(const z of [-1.76,1.76]){const panel=mesh(g,new THREE.ShapeGeometry(tri),p.plaster,0,0,z);if(z<0){panel.rotation.y=Math.PI;}}
 torus(g,p.wood,0,3.83,1.81,.29,.07);cylinder(g,p.darkStone,0,3.83,1.80,.25,.25,.035,24).rotation.x=Math.PI/2;
 box(g,p.wood,0,1.1,1.85,.98,2.15,.15,.04);for(let i=0;i<5;i++)box(g,p.cutWood,-.37+i*.185,1.05,1.95,.16,1.99,.035,.01);for(const y of [.4,1.7])box(g,p.metal,0,y,1.99,.91,.085,.04);ball(g,p.gold,.31,1.06,2.04,.055,.055,.055,10);
 for(const x of [-1.36,1.36]){box(g,p.wood,x,2,1.88,.81,1.08,.16,.035);box(g,p.window,x,2,1.98,.64,.89,.025);for(const dx of [-.17,.17])box(g,p.wood,x+dx,2,2.01,.03,.92,.035);box(g,p.wood,x,2,2.02,.64,.035,.03);box(g,p.wood,x,1.41,1.94,.99,.12,.36,.025);for(const side of [-1,1]){const shutter=box(g,p.wood,x+side*.52,2,1.83,.22,1,.07,.012);shutter.rotation.y=side*.35;}}
 box(g,p.stone,1.32,4.36,-.62,.69,2.15,.65,.03);for(let y=3.5;y<5.4;y+=.24)for(const x of [1.1,1.5])box(g,p.lightStone,x,y,-.28,.32,.18,.06,.01);box(g,p.darkStone,1.32,5.46,-.62,.88,.2,.85,.04);
 lantern(g,p,-.67,2.36,2.08);
 if(kind!=='house'){
  const cloth=material(kind==='smithy'?0x45595e:kind==='tailor'?0x726684:0x64835f,'cloth');
  for(const side of [-1,1])cylinder(g,p.wood,side*1.9,1.24,2.65,.065,.085,2.48,12);
  for(let i=0;i<9;i++){const awning=box(g,i%2?cloth:p.linen,-1.78+i*.445,2.67,2.2,.45,.065,1.4);awning.rotation.x=.19;box(g,i%2?cloth:p.linen,-1.78+i*.445,2.47,2.88,.45,.22,.045);}
  if(kind==='smithy'){
   box(g,p.darkStone,-1.1,.55,2.12,.94,1.06,.82,.065);box(g,p.metal,-1.1,1.15,2.12,1.03,.18,.84,.03);box(g,material(0xf8a344,'plain',{emissive:0xff651c,emissiveIntensity:2}),-1.1,1.25,2.12,.62,.06,.5);
   for(let i=0;i<9;i++)stone(g,p.darkStone,-1.35+(i%3)*.25,1.29,1.96+Math.floor(i/3)*.17,.10,.07,.09,i);
   cylinder(g,p.wood,.9,.25,2.24,.3,.39,.5);box(g,p.metal,.9,.57,2.24,.27,.4,.3,.04);box(g,p.metal,.9,.8,2.24,.77,.17,.4,.045);branch(g,p.metal,[[1.17,.8,2.24],[1.37,.8,2.24],[1.52,.79,2.24]],.15,.02,6,8);
   link(g,p.wood,[.78,.87,2.22],[1.05,1.08,2.22],.035);box(g,p.metal,1.07,1.09,2.22,.22,.14,.13,.02);barrel(g,p,-1.72,1.6,.55);
  }else if(kind==='tailor'){
   box(g,p.wood,-.45,.84,2.38,1.72,.13,.69,.03);for(const x of [-1.13,.22])for(const z of [2.12,2.61])box(g,p.wood,x,.43,z,.09,.85,.09);
   for(let i=0;i<4;i++){const m=material([0xb07569,0x777694,0x8c9c6a,0xc5b487][i],'cloth');cylinder(g,m,-1+i*.38,1.02,2.37,.105,.105,.57,16).rotation.x=Math.PI/2;torus(g,p.linen,-1+i*.38,1.02,2.665,.08,.012);}
   const stand=group(g,1.15,0,2.13);cylinder(stand,p.wood,0,.73,0,.025,.025,1.4);cylinder(stand,p.wood,0,.1,0,.25,.28,.1);ball(stand,p.linen,0,1.35,0,.28,.4,.2);cylinder(stand,p.linen,0,1.64,0,.1,.14,.16);link(stand,p.wood,[-.32,1.52,0],[.32,1.52,0],.06);
  }else{for(const x of [-1.1,1.1])crate(g,p,x,.36,2.23,.69);barrel(g,p,1.75,1.4,.75);for(let j=0;j<14;j++)ball(g,material(j%2?0xb77a3c:0x94623d,'plain'),-.95+(j%4)*.13,.77+Math.floor(j/8)*.1,2.05+Math.floor(j/4)%2*.18,.095,.10,.10,8);box(g,p.wood,-.8,1.55,1.93,1.6,.10,.43);for(let j=0;j<4;j++){cylinder(g,p.glow,-1.36+j*.33,1.77,1.92,.07,.1,.32,10);cylinder(g,p.gold,-1.36+j*.33,1.95,1.92,.042,.042,.08,8);}}
  link(g,p.metal,[-2,3.18,1.85],[-2,3.18,2.83],.04);box(g,p.wood,-2,2.98,2.75,.64,.47,.09,.03);
  // Readable physical workshop emblems: crossed tools, needle, and trade diamond.
  if(kind==='smithy'){link(g,p.gold,[-2.2,2.83,2.82],[-1.83,3.12,2.82],.022);box(g,p.gold,-1.83,3.12,2.83,.2,.08,.035);}else if(kind==='tailor'){link(g,p.gold,[-2.17,2.81,2.82],[-1.88,3.13,2.82],.018);torus(g,p.gold,-1.91,3.1,2.82,.025,.012);}else{const gem=box(g,p.gold,-2,2.98,2.82,.18,.18,.03);gem.rotation.z=Math.PI/4;}
 }
 consolidate(g);return g;
}

function ground(g,p,f,rng,quality){
 const crystal=f.biome==='crystal',layout=floorLayout(f),{road,plaza}=layout;
 cylinder(g,p.darkStone,0,-3.1,0,f.radius-.3,f.radius-4,6,96);cylinder(g,p.stone,0,-10,0,f.radius-4,7,9,64);
 const cliffs=Math.ceil(70*f.radius/46);for(let i=0;i<cliffs;i++){const a=i/cliffs*Math.PI*2,r=f.radius-1.4;stone(g,i%3?p.stone:p.darkStone,Math.sin(a)*r,-1.6-(i%3)*.4,Math.cos(a)*r,1.8,2.8,1.9,i);}
 const mat=material(crystal?0x688c86:0x879756,'stone'),old=mat.onBeforeCompile;mat.onBeforeCompile=s=>{old(s);s.fragmentShader=s.fragmentShader.replace('#include <color_fragment>',`#include <color_fragment>\nfloat soilBlend=grainNoise(detailPosition*.26);diffuseColor.rgb=mix(diffuseColor.rgb*vec3(.78,.85,.74),diffuseColor.rgb*vec3(1.13,1.09,.91),smoothstep(.22,.78,soilBlend));`);};mat.customProgramCacheKey=()=>`terrain-${crystal}`;
 const land=mesh(g,new THREE.CircleGeometry(f.radius,128),mat,0,.002,0);land.rotation.x=-Math.PI/2;land.castShadow=false;
 // Continuous walkable level; colour and pebbles give relief without fictitious slopes.
 const path=material(crystal?0x949f95:0xb8ad90);box(g,path,road.x,.012,(road.from+road.to)/2,road.width,.022,road.to-road.from);cylinder(g,path,plaza.x,.013,plaza.z,plaza.radius,plaza.radius,.025,96);
 const paving=[p.lightStone,material(crystal?0xabb9b2:0xb5b09b),material(crystal?0x9ea9a4:0xc2bca5)];
 for(let z=road.from+.55;z<road.to-.55;z+=1.12)for(let x=-2.2;x<2.4;x+=1.1){const slab=box(g,paving[Math.floor(rng()*3)],x+(Math.round(z/1.12)%2)*.12,.028,z,1.055,.045,1.055,.035);slab.rotation.y=(rng()-.5)*.025;}
 for(let j=0;j<8;j++)for(let i=0;i<42;i++){const a=i/42*Math.PI*2,r=4.1+j*.57;if(Math.abs(plaza.x+Math.sin(a)*r)<2.9)continue;const tile=box(g,paving[(i+j)%3],plaza.x+Math.sin(a)*r,.026,plaza.z+Math.cos(a)*r,.52,.044,.65,.035);tile.rotation.y=a;}
 const blade=new THREE.BufferGeometry();blade.setAttribute('position',new THREE.Float32BufferAttribute([-.08,0,0,.08,0,0,.04,.32,.025,0,.62,.1,-.08,0,0,.04,.32,.025],3));blade.setAttribute('uv',new THREE.Float32BufferAttribute([0,0,1,0,.8,.5,.5,1,0,0,.8,.5],2));blade.computeVertexNormals();
 const grassCount=Math.min(24000,Math.round(quality.grass*layout.densityScale));const grasses=new THREE.InstancedMesh(blade,p.grass,grassCount),d=new THREE.Object3D();let n=0;
 for(let i=0;i<grassCount*5&&n<grassCount;i++){
  const x=(rng()*2-1)*(f.radius-.5),z=(rng()*2-1)*(f.radius-.5);if(Math.hypot(x,z)>f.radius-1||Math.abs(x)<3.1||Math.hypot(x-plaza.x,z-plaza.z)<9.2||f.training&&Math.hypot(x-f.training.x,z-f.training.z)<f.training.radius+.2||f.props?.some(o=>['house','smithy','shop','tailor'].includes(o.kind)&&Math.hypot(x-o.x,z-o.z)<3.3))continue;
  d.position.set(x,.012,z);d.rotation.y=rng()*Math.PI*2;d.scale.setScalar(.5+rng()*.75);d.updateMatrix();grasses.setMatrixAt(n,d.matrix);grasses.setColorAt(n++,new THREE.Color().setRGB(.77+rng()*.45,.83+rng()*.34,.69+rng()*.35));
 }
 grasses.count=n;grasses.castShadow=false;grasses.receiveShadow=true;g.add(grasses);
 for(let i=0;i<100;i++){const a=rng()*6.28,r=9+rng()*(f.radius-11),x=Math.sin(a)*r,z=Math.cos(a)*r;if(Math.abs(x)<3.3||Math.hypot(x-plaza.x,z-plaza.z)<9||f.training&&Math.hypot(x-f.training.x,z-f.training.z)<f.training.radius)continue;stone(g,p.stone,x,.03,z,.09+rng()*.15,.06,.1+rng()*.12,i);}
}
function castle(g,p,z){const c=group(g,-28,27,z);for(let i=0;i<7;i++){const r=25-i*3.15,y=i*4.6;cylinder(c,p.stone,0,y,0,r,r+1.1,1.3,72);cylinder(c,p.lightStone,0,y+.72,0,r+.15,r+.15,.16,72);cylinder(c,p.plaster,0,y+2.3,0,r*.72,r*.83,3.3,64);
 for(let j=0;j<26;j++){const a=j/26*Math.PI*2;box(c,p.lightStone,Math.sin(a)*r*.98,y+1.18,Math.cos(a)*r*.98,.8,.85,.9);if(j%3===0){cylinder(c,p.lightStone,Math.sin(a)*r*.77,y+2.7,Math.cos(a)*r*.77,.6,.8,3.8,12);cylinder(c,p.tileDark,Math.sin(a)*r*.77,y+5,Math.cos(a)*r*.77,0,.87,1.8,12);}}
 }cylinder(c,p.lightStone,0,37,0,1.2,2.1,10,16);cylinder(c,p.tileDark,0,46,0,0,2.8,11,16);return c;}

export function createPortal(){const p=palette(true),g=new THREE.Group();cylinder(g,p.darkStone,0,.11,0,2.65,2.9,.22,64);cylinder(g,p.lightStone,0,.23,0,2.45,2.62,.08,64);for(const r of [1.8,2.3])torus(g,p.gold,0,.28,0,r,.023,true);
 // Open, elevated arch leaves the original portal approach unobstructed.
 const arch=group(g,0,2.3,-.5);torus(arch,p.stone,0,0,0,1.8,.25);torus(arch,p.gold,0,0,.24,1.78,.04);torus(arch,p.glow,0,0,.28,1.59,.04);
 for(let i=0;i<12;i++){const a=i/12*Math.PI*2,b=box(arch,p.gold,Math.sin(a)*1.8,Math.cos(a)*1.8,.255,.15,.28,.07,.025);b.rotation.z=-a;}
 for(const side of [-1,1]){cylinder(g,p.stone,side*1.48,.9,-.5,.24,.38,1.8,12);crystalShard(g,p.glow,side*2.12,.9,-.45,.53,-side*.18);}
 const core=group(g,0,2.3,-.45);crystalShard(core,p.glow,0,0,0,.7);const orbit=torus(core,p.gold,0,0,0,.78,.025);orbit.rotation.y=.5;
 const veil=new THREE.ShaderMaterial({transparent:true,depthWrite:false,side:THREE.DoubleSide,uniforms:{time:{value:0}},vertexShader:'varying vec2 st;void main(){st=uv;gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.0);}',fragmentShader:'varying vec2 st;uniform float time;void main(){vec2 p=st*2.-1.;float r=length(p);float a=atan(p.y,p.x);float v=.5+.5*sin(a*5.+r*19.-time*1.4);float fade=(1.-smoothstep(.5,1.,r));gl_FragColor=vec4(mix(vec3(.14,.49,.52),vec3(.61,.91,.85),v),fade*(.17+v*.19));}'});
 const window=mesh(g,new THREE.CircleGeometry(1.55,64),veil,0,2.3,-.5);window.castShadow=false;window.receiveShadow=false;
 g.userData.portalCore=core;g.userData.portalVeil=veil;return g;
}
export function createResource(r,{leaves=72,seed=1}={}){const p=palette(),g=new THREE.Group(),form=new THREE.Group();g.add(form);
 if(r.kind==='tree'){
  const tree=createTree({crystal:r.id==='ancient-tree',pine:r.id==='pine-tree',color:r.color,seed,leaves});form.add(tree);cylinder(g,p.bark,0,.15,0,.31,.43,.3,18);cylinder(g,p.cutWood,0,.307,0,.29,.29,.012,24);for(const radius of [.1,.19,.26])torus(g,p.wood,0,.32,0,radius,.005,true);
 }else{const ore=material(r.color,'metal',{emissive:r.color,emissiveIntensity:r.id.includes('crystal')?.28:.035});stone(form,p.darkStone,0,.43,0,.84,.68,.67,seed);stone(form,p.stone,-.52,.25,.17,.41,.37,.43,seed+1);for(let j=0;j<8;j++){const a=j*2.4;crystalShard(form,ore,Math.sin(a)*.55,.55+(j%3)*.15,Math.cos(a)*.47,.36+(j%3)*.11,(j%2?1:-1)*.28);}for(let j=0;j<6;j++)stone(g,p.stone,Math.sin(j)*.62,.065,Math.cos(j)*.62,.12,.09,.1,j);}
 consolidate(form);return {mesh:g,form};}

function zoneDetail(g,p,z,species,rng){
 // Low ground decals only: zone dressing never introduces unmodelled obstacles.
 const materialId=species?.model||z.speciesId,center=group(g,z.x,0,z.z);
 if(materialId==='slime'){
  const wet=material(0x456e59,'plain',{roughness:.2,metalness:.18,transparent:true,opacity:.45});for(let i=0;i<4;i++){const puddle=mesh(center,new THREE.CircleGeometry(1.05+rng(),28),wet,Math.sin(i*2.4)*z.radius*.4,.035,Math.cos(i*2.4)*z.radius*.4);puddle.rotation.x=-Math.PI/2;puddle.scale.y=.65;}
 }else{
  const patch=mesh(center,new THREE.CircleGeometry(z.radius*.88,64),material(materialId==='troll'?0x788080:materialId==='dog'?0x8d855f:0x867b65,'plain',{transparent:true,opacity:.21,depthWrite:false}),0,.016,0);patch.rotation.x=-Math.PI/2;
  if(materialId==='goblin'||materialId==='orc')for(let j=0;j<6;j++){const a=j*Math.PI/3;box(center,p.darkStone,Math.sin(a)*1.05,.03,Math.cos(a)*1.05,.32,.055,.27,.025);}
 }
}
function particles(count,crystal,radius){const rng=random(121),positions=[],phases=[];for(let i=0;i<count;i++){const a=rng()*Math.PI*2,r=Math.sqrt(rng())*(radius-2);positions.push(Math.sin(a)*r,.3+rng()*7,Math.cos(a)*r);phases.push(rng()*6.28);}const geo=new THREE.BufferGeometry();geo.setAttribute('position',new THREE.Float32BufferAttribute(positions,3));geo.setAttribute('phase',new THREE.Float32BufferAttribute(phases,1));const mat=new THREE.ShaderMaterial({transparent:true,depthWrite:false,uniforms:{time:{value:0},color:{value:new THREE.Color(crystal?0xa4e4f1:0xe5cc8a)}},vertexShader:'attribute float phase;uniform float time;varying float a;void main(){vec3 p=position;p.x+=sin(time*.3+phase)*.35;p.y+=sin(time*.6+phase)*.22;vec4 mv=modelViewMatrix*vec4(p,1.);a=.25+.4*pow(sin(time*.8+phase),2.);gl_PointSize=clamp(35./-mv.z,1.,4.);gl_Position=projectionMatrix*mv;}',fragmentShader:'uniform vec3 color;varying float a;void main(){float r=length(gl_PointCoord-.5)*2.;gl_FragColor=vec4(color,a*(1.-smoothstep(.2,1.,r)));}'});const obj=new THREE.Points(geo,mat);obj.frustumCulled=false;return obj;}

export function createScenery(f,world,quality='balanced'){
 const layout=floorLayout(f),q=GRAPHICS[quality]||GRAPHICS.balanced,crystal=f.biome==='crystal',p=palette(crystal),rng=random(world.seed+f.id),g=new THREE.Group();p.leafGeo=sprayGeometry();ground(g,p,f,rng,q);
 for(let i=0;i<(f.props||[]).length;i++){
  const prop=f.props[i];if(prop.kind==='resource')continue;let obj;
  if(['house','smithy','tailor','shop'].includes(prop.kind))obj=createBuilding(prop.kind,'',{p});
  else if(prop.kind==='crystal'){obj=new THREE.Group();for(let j=0;j<6;j++)crystalShard(obj,p.glow,Math.sin(j*2.4)*.42,.7+(j%2)*.65,Math.cos(j*2.4)*.4,.75+(j%3)*.35,(j%2?1:-1)*.18);stone(obj,p.darkStone,0,.12,0,.63,.2,.62,i);}
  else obj=createTree({crystal,seed:world.seed+i*37,leaves:q.leaves,p});
  obj.position.set(prop.x,0,prop.z);obj.scale.setScalar(prop.scale??1);obj.rotation.y=prop.rotation||0;g.add(obj);
 }
 for(const z of f.monsterZones||[])zoneDetail(g,p,z,world.monsterSpecies?.find(s=>s.id===z.speciesId),rng);
 if(f.training){const t=f.training,arena=group(g,t.x,0,t.z);cylinder(arena,p.cutWood,0,.018,0,t.radius,t.radius,.035,80);for(let r=1;r<t.radius;r+=.85)torus(arena,p.linen,0,.048,0,r,.015,true);torus(arena,p.stone,0,.05,0,t.radius,.07,true);for(const a of [0,Math.PI/2,Math.PI,Math.PI*1.5])link(arena,p.linen,[Math.sin(a)*.5,.054,Math.cos(a)*.5],[Math.sin(a)*(t.radius-.4),.054,Math.cos(a)*(t.radius-.4)],.015);}
 castle(g,p,layout.castleZ);bakeScenery(g);
 const motes=particles(Math.min(600,Math.round(q.particles*layout.densityScale)),crystal,f.radius);g.add(motes);g.userData.motes=motes;
 // Layered transparent horizon mist, using geometry rather than image downloads.
 const mistMat=material(crystal?0xa3c4d0:0xd2ddca,'plain',{transparent:true,opacity:.16,depthWrite:false});const clouds=new THREE.Group();for(let i=0;i<14;i++){const a=i*2.4;for(let j=0;j<3;j++){const cloud=ball(clouds,mistMat,Math.cos(a)*layout.cloudRadius+j*5,2+i%4*3,Math.sin(a)*layout.cloudRadius,7,1.5,3.5,16);cloud.castShadow=false;cloud.receiveShadow=false;}}consolidate(clouds);g.add(clouds);
 return g;
}
export function animateScenery(g,portal,time){if(g?.userData.motes)g.userData.motes.material.uniforms.time.value=time;if(portal?.userData.portalCore){portal.userData.portalCore.rotation.y=time*.48;portal.userData.portalCore.position.y=2.3+Math.sin(time*1.7)*.10;portal.userData.portalVeil.uniforms.time.value=time;}}
