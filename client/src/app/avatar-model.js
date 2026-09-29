import * as THREE from 'three';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';
import { armorFits, weaponFits } from './equipment.mjs';

// Original, articulated meshes. No remote assets, billboard faces or generated portraits
// are used as 3D models. Coordinates are metres in the existing game scale, facing +Z.
const V = (x, y, z) => new THREE.Vector3(x, y, z);
const tint = (color, factor) => new THREE.Color(color).multiplyScalar(factor);
const material = (color, roughness = .7, metalness = 0, extra = {}) =>
  new THREE.MeshStandardMaterial({ color, roughness, metalness, ...extra });

// Subtle surface relief evaluated by the GPU, with no downloaded texture dependency.
function surface(mat, kind) {
  const frequency=kind==='cloth'?210:kind==='skin'?730:kind==='metal'?360:280;
  const relief=kind==='skin'?.000018:kind==='cloth'?.00010:.00005;
  mat.onBeforeCompile=shader=>{
    shader.vertexShader=shader.vertexShader.replace('#include <common>','#include <common>\nvarying vec3 avatarSurface;')
      .replace('#include <begin_vertex>','#include <begin_vertex>\navatarSurface=position;');
    shader.fragmentShader=shader.fragmentShader.replace('#include <common>',`#include <common>
varying vec3 avatarSurface;
float avatarNoise(vec3 p){return fract(sin(dot(p,vec3(12.9898,78.233,45.164)))*43758.5453);}`)
      .replace('#include <color_fragment>',`#include <color_fragment>
float grain=avatarNoise(floor(avatarSurface*${frequency.toFixed(1)}));
float weave=sin(avatarSurface.x*1300.0)*sin(avatarSurface.y*1300.0);
diffuseColor.rgb*=1.0+${kind==='cloth'?'0.025*weave+0.018*(grain-.5)':'0.015*(grain-.5)'};`)
      .replace('#include <roughnessmap_fragment>','#include <roughnessmap_fragment>\nroughnessFactor=clamp(roughnessFactor+(grain-.5)*.05,.06,1.0);')
      .replace('#include <normal_fragment_maps>',`#include <normal_fragment_maps>
float h=${relief.toFixed(7)}*${kind==='cloth'?'weave':'grain'};
vec3 sx=dFdx(-vViewPosition),sy=dFdy(-vViewPosition);
vec3 rx=cross(sy,normal),ry=cross(normal,sx);
float determinant=dot(sx,rx);
normal=normalize(abs(determinant)*normal-sign(determinant)*(dFdx(h)*rx+dFdy(h)*ry));`);
  };
  mat.customProgramCacheKey=()=> 'avatar-surface-'+kind;
}

function smoothSeam(geo, segments) {
  const normals=geo.attributes.normal;
  for(let i=0;i<normals.count;i+=segments+1){const j=i+segments;if(j>=normals.count)break;const n=V(normals.getX(i)+normals.getX(j),normals.getY(i)+normals.getY(j),normals.getZ(i)+normals.getZ(j)).normalize();normals.setXYZ(i,n.x,n.y,n.z);normals.setXYZ(j,n.x,n.y,n.z);}
}

function mesh(parent, geometry, mat, x = 0, y = 0, z = 0) {
  const m = new THREE.Mesh(geometry, mat);
  m.position.set(x, y, z); m.castShadow = true; m.receiveShadow = true;
  parent.add(m); return m;
}
function ellipsoid(parent, mat, x, y, z, sx, sy, sz, segments = 20) {
  const m = mesh(parent, new THREE.SphereGeometry(1, segments, Math.max(10, segments / 2)), mat, x, y, z);
  m.scale.set(sx, sy, sz); return m;
}
function group(parent, x = 0, y = 0, z = 0) {
  const g = new THREE.Group(); g.position.set(x, y, z); parent.add(g); return g;
}
function box(parent, mat, x, y, z, sx, sy, sz) {
  return mesh(parent, new THREE.BoxGeometry(sx, sy, sz), mat, x, y, z);
}
function tube(parent, mat, points, radius = .01, segments = 20, sides = 6) {
  return mesh(parent, new THREE.TubeGeometry(new THREE.CatmullRomCurve3(points.map(p => V(...p))), segments, radius, sides, false), mat);
}

// A continuous elliptical surface, used for anatomy, fitted clothing and curved armour.
// Profile rows: height, half width, half depth, depth offset. Open ends overlap joints.
function loft(parent, mat, rows, segments = 24, folds = 0) {
  if(rows.length>2){
    const curve=new THREE.CatmullRomCurve3(rows.map(r=>V(r[1],r[0],r[2])),false,'catmullrom',.3);
    const sampled=[];
    for(let i=0;i<=(rows.length-1)*3;i++){
      const t=i/((rows.length-1)*3),v=curve.getPoint(t),j=Math.min(rows.length-2,Math.floor(t*(rows.length-1))),f=t*(rows.length-1)-j;
      sampled.push([v.y,Math.max(.001,v.x),Math.max(.001,v.z),THREE.MathUtils.lerp(rows[j][3]||0,rows[j+1][3]||0,f)]);
    }rows=sampled;
  }
  const points = [], uv = [], indices = [];
  for (let r = 0; r < rows.length; r++) {
    const [y, rx, rz, cz = 0] = rows[r];
    for (let s = 0; s <= segments; s++) {
      const a = s / segments * Math.PI * 2;
      const ripple = 1 + folds * Math.cos(a * 8 + r * .45);
      points.push(Math.sin(a) * rx * ripple, y, Math.cos(a) * rz * ripple + cz);
      uv.push(s / segments, r / (rows.length - 1));
      if (r && s) { const i = r * (segments + 1) + s; indices.push(i, i - 1, i - segments - 2, i, i - segments - 2, i - segments - 1); }
    }
  }
  const geo = new THREE.BufferGeometry(); geo.setAttribute('position', new THREE.Float32BufferAttribute(points, 3));
  geo.setAttribute('uv', new THREE.Float32BufferAttribute(uv, 2)); geo.setIndex(indices); geo.computeVertexNormals();smoothSeam(geo,segments);
  return mesh(parent, geo, mat);
}

// A tapered, flattened lock of hair / curved horn, including a closed pointed tip.
function lock(parent, mat, points, width, thickness = .018, segments = 18) {
  const curve = new THREE.CatmullRomCurve3(points.map(p => V(...p)));
  const frames = curve.computeFrenetFrames(segments, false), positions = [], uvs = [], indices = [], sides = 10;
  for (let i = 0; i <= segments; i++) {
    const t = i / segments, p = curve.getPointAt(t);
    const taper = Math.max(.012, Math.pow(1 - t, .65) * (.6 + .65 * Math.sin(Math.PI * t)));
    for (let j = 0; j <= sides; j++) {
      const a = j / sides * Math.PI * 2;
      const v = p.clone().addScaledVector(frames.normals[i], Math.cos(a) * width * taper)
        .addScaledVector(frames.binormals[i], Math.sin(a) * thickness * taper);
      positions.push(v.x, v.y, v.z); uvs.push(j / sides, t);
      if (i && j) { const n = i * (sides + 1) + j; indices.push(n, n - 1, n - sides - 2, n, n - sides - 2, n - sides - 1); }
    }
  }
  const geo = new THREE.BufferGeometry(); geo.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
  geo.setAttribute('uv', new THREE.Float32BufferAttribute(uvs, 2)); geo.setIndex(indices); geo.computeVertexNormals();
  return mesh(parent, geo, mat);
}

// Consolidate only direct mesh children; articulated joint boundaries remain intact.
// One material draw per joint instead of one draw for each rivet, hair lock or finger.
function consolidate(parent) {
  for (const c of [...parent.children]) if (c.isGroup) consolidate(c);
  const batches = new Map();
  for (const c of [...parent.children]) if (c.isMesh && !Array.isArray(c.material)) {
    const key = c.material.uuid + ':' + Object.keys(c.geometry.attributes).sort().join(',');
    if (!batches.has(key)) batches.set(key, []); batches.get(key).push(c);
  }
  for (const list of batches.values()) if (list.length > 1) {
    const transformed = list.map(m => { m.updateMatrix(); const g = m.geometry.clone(); g.applyMatrix4(m.matrix); if(!g.index)return g;const expanded=g.toNonIndexed();g.dispose();return expanded; });
    const merged = mergeGeometries(transformed, false);
    transformed.forEach(g => g.dispose());
    if (merged) {
      mesh(parent, merged, list[0].material);
      list.forEach(m => { parent.remove(m); m.geometry.dispose(); });
    }
  }
}

export function avatarEnvironment(renderer) {
  const room = new RoomEnvironment(), generator = new THREE.PMREMGenerator(renderer);
  const target = generator.fromScene(room, .06);
  room.dispose(); generator.dispose(); return target;
}

function face(head, mats, look, race) {
  const female = look.gender === 'female', bold = look.face === 'bold', kind = look.face === 'kind';
  const drake = race?.model === 'draconian';
  const rows = [
    [-.195, .018, .038, .022], [-.181, .059, .082, .025], [-.153, female ? .096 : .109, .111, .012],
    [-.113, .127, .130, 0], [-.067, .145, .138, -.002], [-.017, .151, .140, -.004],
    [.030, .148, .142, -.006], [.070, .151, .151, -.012], [.116, .148, .152, -.015],
    [.159, .129, .134, -.022], [.192, .089, .098, -.025], [.211, .012, .025, -.025]
  ];
  // Subdivide the profile and integrate cheekbones, eye sockets and nose bridge.
  const smooth = [];
  for (let i = 0; i < rows.length - 1; i++) for (let j = 0; j < 1; j++) smooth.push(rows[i]);
  smooth.push(rows.at(-1));
  const headMesh = loft(head, mats.skinFace, smooth, 56), p = headMesh.geometry.attributes.position, colors = [];
  const color = new THREE.Color(look.skinColor || '#dfb18b'), warm = color.clone().lerp(new THREE.Color('#a7483e'), .17);
  for (let i = 0; i < p.count; i++) {
    const x = p.getX(i), y = p.getY(i), z = p.getZ(i), front = Math.max(0, z / .15);
    const gauss = (cx, cy, sx, sy) => Math.exp(-(((x - cx) / sx) ** 2) - ((y - cy) / sy) ** 2);
    const nose = (.039 * gauss(0, -.033, .023, .078) + .026 * gauss(0, -.065, .031, .026)) * front ** 6;
    const cheeks = .012 * (gauss(-.084, -.064, .05, .045) + gauss(.084, -.064, .05, .045));
    const sockets = .010 * (gauss(-.063, .018, .047, .028) + gauss(.063, .018, .047, .028));
    p.setZ(i, z + (nose + cheeks - sockets) * front);
    const blush = Math.min(1, (gauss(-.095, -.06, .034, .028) + gauss(.095, -.06, .034, .028)) * front);
    const c = color.clone().lerp(warm, blush).multiplyScalar(1 - .04 * Math.max(0, -y / .2)); colors.push(c.r, c.g, c.b);
  }
  headMesh.geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3)); headMesh.geometry.computeVertexNormals();smoothSeam(headMesh.geometry,56);
  const detail = group(head); detail.name = 'facial-details';
  for (const side of [-1, 1]) {
    const eye = group(detail, side * .062, .012, .131); eye.rotation.y = side * .23;
    ellipsoid(eye, mats.white, 0, 0, 0, .035, female ? .0140 : .0120, .014, 20);
    ellipsoid(eye, mats.iris, 0, 0, .0125, .013, .0133, .0035, 20);
    ellipsoid(eye, mats.pupil, 0, 0, .0153, drake ? .0028 : .0057, .007, .0018, 14);
    ellipsoid(eye, mats.eyeGlint, -.004, .0047, .0174, .0023, .0023, .001, 8);
    const eyeHeight = female ? .0138 : .0118;
    tube(eye, mats.lash, [[-.039, .001, 0], [-.021, eyeHeight, .010], [.010, eyeHeight * .91, .014], [.036, .0015, .002]], female ? .0026 : .0021, 14);
    tube(eye, mats.lipSkin, [[-.038, 0, 0], [-.015, -eyeHeight * .65, .011], [.015, -eyeHeight * .62, .011], [.037, .001, .001]], .0026, 12);
    tube(detail, mats.hair, [[side * .028, bold ? .048 : .052, .141], [side * .062, bold ? .059 : .061, .139], [side * .105, .048, .113]], bold ? .0055 : .0032, 12);
    ellipsoid(detail, mats.shadowSkin, side * .015, -.079, .174, .004, .0016, .002, 12);
    if (race?.ears === 'pointed' || race?.ears === 'horned') {
      const ear = group(head, side * .134, -.002, -.015);
      lock(ear, mats.skin, [[0, -.035, 0], [side * .036, .014, .006], [side * .13, .071, -.043]], .037, .019, 12);
      lock(ear, mats.lipSkin, [[side * .011, -.022, .017], [side * .043, .016, .021], [side * .113, .062, -.025]], .019, .004, 10);
    } else {
      ellipsoid(head, mats.skin, side * .148, -.02, -.006, .028, .051, .026);
      ellipsoid(head, mats.lipSkin, side * .162, -.017, .012, .012, .029, .010, 16);
    }
  }
  // Small lip volumes and the mouth seam follow the chin instead of a painted square.
  const smile = kind ? .003 : .001;
  tube(detail, mats.lips, [[-.032, -.121 + smile, .131], [-.013, -.119, .143], [0, -.120, .147], [.013, -.119, .143], [.032, -.121 + smile, .131]], .0028, 18);
  tube(detail, mats.lipSkin, [[-.03, -.124 + smile, .131], [0, -.131, .144], [.03, -.124 + smile, .131]], .0035, 16);
  tube(detail, mats.mouth, [[-.031, -.122 + smile, .134], [0, -.123, .148], [.031, -.122 + smile, .134]], .0017, 16);
  return detail;
}

function hair(head, mats, look, race) {
  const female = look.gender === 'female';
  const long = race?.ears === 'pointed' || (female && look.hairStyle !== 'bob');
  const scalp = new THREE.SphereGeometry(1, 32, 18), p = scalp.attributes.position;
  // Variable hairline: forehead open, sides and nape covered.
  for (let i = 0; i < p.count; i++) {
    const x = p.getX(i), y = p.getY(i), z = p.getZ(i), a = Math.atan2(x, z);
    const theta = Math.acos(THREE.MathUtils.clamp(y, -1, 1)) / Math.PI;
    const end = 1.29 + .92 * (1 - Math.max(0, Math.cos(a))) + .035 * Math.sin(a * 5);
    const t = theta * end;
    p.setXYZ(i, Math.sin(t) * Math.sin(a) * .172, Math.cos(t) * .222 + .007, Math.sin(t) * Math.cos(a) * .176 - .012);
  }
  // SphereGeometry omits one triangle per bottom pole cell. The deformed cap has an
  // open hairline instead of a pole, so both triangles must be restored there.
  const capIndices=[];
  for(let r=1;r<=18;r++)for(let c=1;c<=32;c++){const i=r*33+c;capIndices.push(i,i-34,i-1,i,i-33,i-34);}
  scalp.setIndex(capIndices);scalp.computeVertexNormals();smoothSeam(scalp,32);mesh(head, scalp, mats.hair);
  // Combed locks flow from a side part across the forehead; fine ridges catch light.
  for (let i = 0; i < 11; i++) {
    const t = i / 10, side = i < 4 ? 1 : -1, spread = i < 4 ? i / 3 : (i - 4) / 6;
    const pts = [[.045 - t * .014, .231 - spread * .014, .015 + spread * .025],
      [side * (.067 + spread * .025), .207 - spread * .025, .114 + spread * .017],
      [side * (.117 + spread * .012), .103 - spread * .048, .158 - spread * .005],
      [side * (.135 - spread * .055), .025 - spread * .014, .132 + spread * .022]];
    lock(head, i % 3 === 0 ? mats.hairLight : mats.hair, pts, .022 + spread * .008, .011, 18);
    if (i % 2 === 0) tube(head, mats.hairLight, pts.map(([x,y,z]) => [x + .003,y + .002,z + .008]), .0012, 20, 4);
  }
  for (let i = 0; i < 14; i++) {
    const a = .9 + i / 13 * (Math.PI * 2 - 1.8), x = Math.sin(a), z = Math.cos(a);
    const endY = long ? -.50 - .12 * Math.sin(i * 2.1) : female ? -.20 : -.15;
    const pts = [[x * .065, .220, z * .068 - .019], [x * .172, .075, z * .184 - .020],
      [x * .170, -.14, z * .180 - .019], [x * (.166 + (long ? .045 : .01)), endY, z * .192 - .043]];
    lock(head, i % 4 === 0 ? mats.hairLight : mats.hair, pts, long ? .037 : .028, .018, 18);
  }
  if (race?.beard && !female) {
    for (let i = -3; i <= 3; i++) {
      const x = i * .033;
      lock(head, i % 2 ? mats.hairLight : mats.hair, [[x, -.104, .109], [x * .96, -.20, .145], [x * .72, -.31 - .035 * (3 - Math.abs(i)), .11]], .032, .021, 18);
    }
    for (const s of [-1, 1]) lock(head, mats.hair, [[0, -.095, .168], [s * .053, -.10, .155], [s * .10, -.157, .126]], .015, .012);
    for (const s of [-1, 1]) ellipsoid(head, mats.gold, s * .05, -.313, .122, .025, .02, .022, 12);
  }
}

function draconicFeatures(body, head, mats, race) {
  if (race?.horns) for (const side of [-1, 1]) {
    lock(head, mats.horn, [[side * .118,.14,-.04],[side * .22,.26,-.10],[side * .26,.30,-.18],[side * .19,.38,-.22]], .053, .046, 24);
    for (let i = 0; i < 4; i++) ellipsoid(head, mats.scale, side * (.117 - i * .017), .07 - i * .034, .096 + i * .015, .021, .028, .01, 12);
  }
  if (race?.tail) {
    const tail = group(body, 0, 1.16, -.15); tail.name = 'dragon-tail';
    lock(tail, mats.skin, [[0,0,0],[.05,-.28,-.38],[.27,-.68,-.76],[.48,-.61,-1.08]], .095, .077, 28);
    for (let i = 0; i < 5; i++) {
      const t = i / 4;
      lock(tail, mats.horn, [[t * .34, -.09 - t * .48, -.19 - t * .69],[t * .34, .016 - t * .48, -.24 - t * .69]], .023, .019, 8);
    }
    body.userData.tail = tail;
  }
  if (race?.wings) for (const s of [-1, 1]) {
    const wing = group(body, s * .21, 1.79, -.16); wing.rotation.y = s * -.2;
    const pts = [[0,0,0],[s*.22,.46,-.17],[s*.58,.32,-.36],[s*.44,-.06,-.32],[s*.25,-.38,-.20],[s*.03,-.19,-.03]];
    const positions = [], indices = [];
    pts.forEach(p => positions.push(...p)); positions.push(s * .18, .005, -.26);
    for (let i = 0; i < pts.length; i++) indices.push(6, i, (i + 1) % pts.length);
    const geo = new THREE.BufferGeometry(); geo.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3)); geo.setIndex(indices); geo.computeVertexNormals();
    mesh(wing, geo, mats.membrane);
    for (const i of [1,2,3,4]) tube(wing, mats.scale, [pts[0], [s*.18,.06,-.21], pts[i]], i === 1 ? .023 : .012, 12);
    tube(wing, mats.scale, [...pts,pts[0]], .012, 24);
  }
}

function cloak(body, mats, robe) {
  const cloth = group(body); cloth.name = 'cloak';
  const columns = 18, rows = 18, vertices = [], uvs = [], indices = [];
  const bottom = robe ? .29 : .57;
  const point = (u, t) => {
    const width = .27 + .12 * t, x = (u * 2 - 1) * width;
    return [x, 1.94 - t * (1.94 - bottom) + Math.abs(u - .5) * .09 * t,
      -.16 - .20 * t - .035 * Math.cos(u * Math.PI * 8) * Math.sin(t * Math.PI * .65)];
  };
  for (let r = 0; r <= rows; r++) for (let c = 0; c <= columns; c++) {
    vertices.push(...point(c / columns, r / rows)); uvs.push(c / columns, r / rows);
    if (c && r) { const i = r * (columns + 1) + c; indices.push(i,i-columns-2,i-1,i,i-columns-1,i-columns-2); }
  }
  const geo = new THREE.BufferGeometry(); geo.setAttribute('position',new THREE.Float32BufferAttribute(vertices,3)); geo.setAttribute('uv',new THREE.Float32BufferAttribute(uvs,2)); geo.setIndex(indices); geo.computeVertexNormals();
  mesh(cloth,geo,mats.cloak);
  for (const u of [0,1]) tube(cloth,mats.trim,Array.from({length:10},(_,i)=>point(u,i/9)),.007,24);
  tube(cloth,mats.trim,Array.from({length:15},(_,i)=>point(i/14,1)),.007,24);
  body.userData.cloak = cloth;
}

function pendant(parent, mats, x, y, z, scale = 1) {
  const g = group(parent,x,y,z); g.scale.setScalar(scale);
  const ring = mesh(g,new THREE.TorusGeometry(.037,.005,6,24),mats.gold);
  mesh(g,new THREE.OctahedronGeometry(.021),mats.gem,0,0,.006).scale.set(.8,1.3,.45);
  return ring;
}

function equipment(body, mats, look, outfit, rig) {
  const heavy = outfit.style === 'plate', mail = outfit.style === 'mail', robe = outfit.style === 'tunic';
  const female = look.gender === 'female', width = female ? .91 : 1;
  loft(body,mats.linen,[[1.20,.20*width,.133],[1.37,.18*width,.126],[1.57,.223*width,.139],[1.78,.279*width,.163],[1.91,.251*width,.135],[1.97,.104,.097]],32,.025);
  loft(body,heavy || mail ? mats.metal : mats.leather,[[1.32,.200*width,.149],[1.44,.210*width,.157],[1.65,.256*width,.181],[1.80,.290*width,.183],[1.88,.280*width,.160]],32,.004);
  // Cream inset, split tabard, raised seams and cross-body strap recall the portraits.
  const front = group(body);
  const inset = [[-.08,1.895,.159],[-.075,1.71,.184],[-.065,1.49,.165],[-.083,1.35,.148],[.083,1.35,.148],[.065,1.49,.165],[.075,1.71,.184],[.08,1.895,.159]];
  const geo = new THREE.BufferGeometry();geo.setAttribute('position',new THREE.Float32BufferAttribute(inset.flat(),3));geo.setIndex([0,1,7,1,6,7,1,2,6,2,5,6,2,3,5,3,4,5]);geo.computeVertexNormals();mesh(front,geo,mats.linen);
  for(const s of [-1,1]) {
    tube(front,mats.trim,[[s*.083,1.895,.164],[s*.08,1.71,.191],[s*.07,1.49,.171],[s*.087,1.35,.154]],.005,22);
    const hem=robe?.30:heavy?.93:.79;
    const point=(u,t)=>{
      const angle=s*(.10+u*(Math.PI-.20)),r=.21+t*.08;
      return [Math.sin(angle)*r,1.325-t*(1.325-hem)+Math.cos(angle*2)*t*.024,
        Math.cos(angle)*(.155+t*.03)+Math.sin(u*Math.PI*4)*.008*t];
    };
    const positions=[],indices=[],uvs=[];
    for(let r=0;r<=14;r++)for(let c=0;c<=18;c++){
      positions.push(...point(c/18,r/14));uvs.push(c/18,r/14);
      if(r&&c){const i=r*19+c;if(s===1)indices.push(i,i-20,i-1,i,i-19,i-20);else indices.push(i,i-1,i-20,i,i-20,i-19);}
    }
    const geo=new THREE.BufferGeometry();geo.setAttribute('position',new THREE.Float32BufferAttribute(positions,3));geo.setAttribute('uv',new THREE.Float32BufferAttribute(uvs,2));geo.setIndex(indices);geo.computeVertexNormals();mesh(body,geo,heavy?mats.metal:mats.cloak);
    for(const u of [0,1])tube(body,mats.trim,Array.from({length:10},(_,i)=>point(u,i/9)),.0045,18);
    tube(body,mats.trim,Array.from({length:15},(_,i)=>point(i/14,1)),.005,24);
  }
  loft(body,mats.leather,[[1.295,.214*width,.150],[1.38,.207*width,.15]],32);
  for(const y of [1.303,1.370]) tube(body,mats.trim,[[-.19*width,y,.075],[-.12*width,y,.140],[0,y,.157],[.12*width,y,.140],[.19*width,y,.075]],.0045,22);
  const buckle=mesh(body,new THREE.TorusGeometry(.035,.007,5,4),mats.gold,0,1.336,.162);buckle.rotation.z=Math.PI/4;buckle.scale.x=1.4;
  const strap=tube(body,mats.leather,[[-.205*width,1.88,.127],[-.15,1.73,.182],[.035,1.51,.191],[.15,1.32,.151]],.021,28,10);strap.scale.z=1.005;
  pendant(body,mats,-.19*width,1.83,.167,.85);
  pendant(body,mats,0,1.70,.19,.7);
  // Pouch and visible fastener.
  ellipsoid(body,mats.leather,-.245*width,1.23,.012,.062,.087,.07);
  ellipsoid(body,mats.gold,-.249*width,1.252,.08,.008,.009,.004,8);
  loft(body,mats.linen,[[1.92,.096,.099],[2.012,.078,.082]],24);
  for(const s of [-1,1])tube(body,mats.trim,[[s*.079,2.012,.030],[s*.079,1.968,.083],[s*.12,1.91,.113]],.005,12);
  if(outfit.cape)cloak(body,mats,robe);
  for(const joint of rig.arms) {
    const s=joint.userData.side;
    ellipsoid(joint,heavy||mail?mats.metal:mats.cloth,s*.008,-.021,0,heavy?.17:.127,.098,.140);
    for(const y of [-.01,-.063])tube(joint,mats.gold,[[-.12,y,.04],[0,y+.058,.163],[.12,y,.04]],heavy?.008:.0045,18);
    if(heavy)for(const y of [-.05,-.11])ellipsoid(joint,mats.metal,s*.03,y,-.003,.175,.08,.151);
    const fore=joint.userData.forearm;
    loft(fore,heavy||mail?mats.metal:mats.leather,[[-.345,.063,.075],[-.28,.068,.08],[-.14,.084,.093],[-.085,.088,.09]],20);
    for(const y of [-.33,-.1])loft(fore,mats.gold,[[y,.072,.083],[y+.012,.073,.084]],20);
    tube(fore,mats.trim,[[0,-.31,.08],[s*.032,-.235,.086],[0,-.12,.094]],.004,14);
  }
  for(const leg of rig.legs) {
    const shin=leg.userData.shin;
    if(heavy||mail) {
      ellipsoid(shin,mats.metal,0,.012,.064,.091,.084,.053);
      loft(shin,mats.metal,[[-.43,.063,.075,.025],[-.30,.09,.098,.014],[-.08,.087,.084,.015]],20);
      tube(shin,mats.gold,[[0,-.4,.102],[0,-.25,.124],[0,-.05,.10]],.006,16);
    }
  }
  if(outfit.helmet) {
    const head=rig.head;
    // Open-face helmet preserves facial customization, with a fitted brow and cheek plates.
    const helm=mesh(head,new THREE.SphereGeometry(1,28,14,0,Math.PI*2,0,Math.PI*.46),mats.metal,0,.045,-.026);helm.scale.set(.174,.22,.177);
    tube(head,mats.gold,[[-.158,.083,.045],[-.10,.094,.14],[0,.107,.164],[.10,.094,.14],[.158,.083,.045]],.009,24);
    for(const s of [-1,1])lock(head,mats.metal,[[s*.151,.08,.027],[s*.159,-.035,.066],[s*.123,-.103,.085]],.031,.02,16);
    pendant(head,mats,0,.133,.151,.67);
  }
}

function weapon(hand, item, side, mats) {
  if(!item)return null;
  const root=group(hand,0,-.07,.028);root.name='weapon-'+item.kind;
  const steel=material(item.primary,.27,.82),gold=material(item.secondary,.32,.70),wood=material('#473127',.68),
    glow=material(item.glow,.17,.22,{emissive:item.glow,emissiveIntensity:.25});
  const kind=item.kind;
  if(['sword','dagger','greatsword'].includes(kind)) {
    const length=kind==='dagger'?.54:kind==='greatsword'?1.24:.95,w=kind==='greatsword'?.076:.045;
    // Diamond-section blade: metallic edge and a raised central fuller.
    const verts=[-w,0,.12,0,.017,.12,w,0,.12,0,-.017,.12,-w*.65,0,length,0,.009,length,w*.65,0,length,0,-.009,length,0,0,length+.16];
    const geo=new THREE.BufferGeometry();geo.setAttribute('position',new THREE.Float32BufferAttribute(verts,3));geo.setIndex([0,4,5,0,5,1,1,5,6,1,6,2,2,6,7,2,7,3,3,7,4,3,4,0,4,8,5,5,8,6,6,8,7,7,8,4]);geo.computeVertexNormals();mesh(root,geo,steel);
    tube(root,gold,[[-.16,-.012,.083],[-.08,.018,.026],[0,.023,.012],[.08,.018,.026],[.16,-.012,.083]],.012,20);
    const grip=mesh(root,new THREE.CylinderGeometry(.023,.027,item.hands===2?.31:.20,12),wood,0,0,-.085);grip.rotation.x=Math.PI/2;
    for(let i=0;i<5;i++){const ring=mesh(root,new THREE.TorusGeometry(.025,.0025,4,12),gold,0,0,-.17+i*.038);}
    ellipsoid(root,gold,0,0,item.hands===2?-.25:-.204,.034,.034,.04,16);
    // Carry diagonally at rest, tip clear of the ground. Attack poses pivot at the shoulder.
    root.rotation.x=.56;root.rotation.y=side*.20;
  } else if(kind==='staff'||kind==='wand') {
    const length=kind==='wand'?.52:item.hands===2?1.80:1.47;
    mesh(root,new THREE.CylinderGeometry(.020,.026,length,16),wood,0,length*.24,.02);
    for(const y of [-length*.23,length*.65,length*.73])mesh(root,new THREE.CylinderGeometry(.03,.03,.035,16),gold,0,y,.02);
    const crown=length*.77;
    for(const s of [-1,1])tube(root,gold,[[0,crown-.12,.02],[s*.105,crown,.02],[s*.076,crown+.18,.02],[0,crown+.245,.02]],.01,18);
    const crystal=mesh(root,new THREE.OctahedronGeometry(kind==='wand'?.065:.095),glow,0,crown+.105,.02);crystal.scale.y=1.55;
    root.rotation.z=-side*.09;root.rotation.x=.50;
  } else if(kind==='shield') {
    const shield=group(root,0,.15,.095);shield.rotation.y=side*.18;
    ellipsoid(shield,gold,0,0,0,.245,.34,.06,28);
    ellipsoid(shield,steel,0,0,.046,.220,.314,.045,28);
    tube(shield,gold,[[0,.264,.07],[0,0,.096],[0,-.26,.07]],.009,20);
    for(const s of [-1,1])tube(shield,gold,[[0,.18,.086],[s*.135,.05,.092],[0,-.18,.086]],.006,16);
    ellipsoid(shield,gold,0,0,.10,.045,.055,.02,16);
  } else if(kind==='orb') {
    ellipsoid(root,glow,0,.05,.07,.135,.135,.135,28);
    for(const a of [0,Math.PI/2]){const ring=mesh(root,new THREE.TorusGeometry(.152,.006,6,36),gold,0,.05,.07);ring.rotation.y=a;ring.rotation.x=.3;}
  } else if(kind==='tome') {
    const book=group(root,0,.05,.09);book.rotation.x=-.3;
    box(book,mats.linen,0,0,0,.19,.27,.07);
    for(const z of [-.046,.046])box(book,steel,0,0,z,.207,.292,.016);
    tube(book,gold,[[-.087,.13,.057],[.087,.13,.057],[.087,-.13,.057],[-.087,-.13,.057],[-.087,.13,.057]],.005,24);
    mesh(book,new THREE.OctahedronGeometry(.035),glow,0,0,.067).scale.z=.45;
  }
  return root;
}

export function createAvatar(color, appearance={}, catalog=null, level=1, weaponSetId='', resolved=null) {
  const look=appearance||{},female=look.gender==='female';
  const race=catalog?.races?.find(r=>r.id===look.race);
  const outfit=resolved?.outfit||catalog?.equipmentSets?.filter(o=>armorFits(catalog,o,look)&&o.minLevel<=level).sort((a,b)=>b.minLevel-a.minLevel)[0]||{primary:color,metal:'#d0d7d2',accent:'#bc995b',style:'leather',cape:true};
  const eligible=catalog?.weaponSets?.filter(w=>weaponFits(catalog,w,look)&&w.minLevel<=level)||[];
  const weapons=resolved?.weapons||eligible.find(w=>w.id===weaponSetId)||eligible.filter(w=>w.autoEquip).sort((a,b)=>b.minLevel-a.minLevel)[0]||{mainHand:{kind:'sword',hands:1,primary:'#d6e3e3',secondary:'#bca26c',glow:'#6cab99'}};
  const skin=look.skinColor||'#dfb18b',hairColor=look.hairColor||'#342f32';
  const mats={
    skin:material(tint(skin,.83),.59,0),skinFace:material('#dedede',.58,0,{vertexColors:true}),
    lipSkin:material(new THREE.Color(skin).lerp(new THREE.Color('#a8534b'),.24),.62),
    lips:material(new THREE.Color(skin).lerp(new THREE.Color('#9e4d43'),.62).multiplyScalar(.72),.51),
    shadowSkin:material(tint(skin,.36),.85),mouth:material(tint(skin,.37),.8),
    white:material('#ede6d5',.25),iris:material(race?.eyeColor||'#43604b',.28),pupil:material('#11181a',.2),
    eyeGlint:material('#fff8e0',.05,0,{emissive:'#fff8e0',emissiveIntensity:.4}),lash:material(tint(hairColor,.4),.8),
    hair:material(tint(hairColor,.75),.52,.02),hairLight:material(new THREE.Color(hairColor).lerp(new THREE.Color('#d7b28a'),.10).multiplyScalar(.8),.49,.02),
    cloth:material(outfit.primary,.91),cloak:material(outfit.primary,.87,0,{side:THREE.DoubleSide}),
    linen:material('#d9d0b9',.93),leather:material(look.specializationId==='assassin'?'#292e35':'#514137',.7),
    trousers:material(look.classId==='healer'?'#8b8677':'#30373b',.96),
    metal:material(outfit.metal||'#c6d3d0',.3,.78),gold:material(outfit.accent||'#c6a365',.34,.68),
    trim:material(outfit.accent||'#c6a365',.72,.15),gem:material(race?.eyeColor||'#4f9c9a',.17,.2),
    horn:material('#a49171',.52),scale:material(tint(skin,.76),.48,.05),
    membrane:material(new THREE.Color(skin).lerp(new THREE.Color('#c9ac7c'),.47),.85,0,{side:THREE.DoubleSide})
  };
  for(const k of ['skin','skinFace','lipSkin'])surface(mats[k],'skin');
  for(const k of ['cloth','cloak','linen','trousers'])surface(mats[k],'cloth');
  surface(mats.leather,'leather');surface(mats.metal,'metal');
  const root=new THREE.Group(),body=group(root),rig={arms:[],legs:[],head:null};
  root.name='avatar';root.userData.body=body;root.userData.avatarRig=rig;
  const head=group(body,0,2.20,0);rig.head=head;
  loft(body,mats.skin,[[1.94,.082,.076],[2.06,.074,.074],[2.10,.074,.075]],24);
  const details=face(head,mats,look,race);hair(head,mats,look,race);
  root.userData.faceDetails=details;
  const width=female?.91:1,hip=female?.128:.14;
  for(const s of [-1,1]) {
    const leg=group(body,s*hip,1.245,0);leg.userData.side=s;rig.legs.push(leg);
    loft(leg,mats.trousers,[[0,.112,.121],[-.10,.125,.129],[-.27,.104,.114],[-.47,.085,.092],[-.57,.076,.083]],24,.017);
    const shin=group(leg,0,-.57,0);leg.userData.shin=shin;
    ellipsoid(shin,mats.trousers,0,0,0,.076,.082,.084);
    loft(shin,mats.leather,[[-.57,.06,.076],[-.39,.072,.078],[-.25,.087,.085],[-.09,.083,.085],[0,.075,.082]],24,.012);
    ellipsoid(shin,mats.leather,0,-.572,.054,.080,.078,.159,24);
    ellipsoid(shin,mats.trousers,0,-.622,.059,.082,.027,.157,20);
    for(const y of [-.18,-.36])loft(shin,mats.trim,[[y,.09,.09],[y+.018,.09,.09]],20);
    const arm=group(body,s*.31*width,1.875,0);arm.userData.side=s;arm.rotation.z=s*.095;rig.arms.push(arm);
    loft(arm,mats.linen,[[-.37,.073,.078],[-.27,.081,.092],[-.09,.101,.112],[.015,.091,.096]],24,.045);
    const fore=group(arm,0,-.36,0);arm.userData.forearm=fore;fore.rotation.x=-.13;
    ellipsoid(fore,mats.linen,0,0,0,.075,.078,.083);
    loft(fore,mats.linen,[[-.36,.050,.052],[-.27,.063,.065],[-.10,.081,.089],[0,.073,.078]],24,.04);
    const hand=group(fore,0,-.373,0);arm.userData.hand=hand;
    ellipsoid(hand,mats.skin,0,-.061,0,.052,.077,.026);
    const held=s===1?weapons.mainHand:weapons.offHand;
    for(let f=0;f<4;f++) {
      const x=(f-1.5)*.023;
      if(held)tube(hand,mats.skin,[[x,-.083,.004],[x,-.12,.024],[x,-.10,.050]],.010,10,8);
      else ellipsoid(hand,mats.skin,x,-.116+(Math.abs(f-1.5)-.5)*.012,.003,.012,.041,.014,12);
    }
    const thumb=ellipsoid(hand,mats.skin,-s*.047,-.063,.015,.017,.042,.018,14);thumb.rotation.z=s*.42;
    arm.userData.weapon=weapon(hand,s===1?weapons.mainHand:weapons.offHand,s,mats);
  }
  equipment(body,mats,look,outfit,rig);draconicFeatures(body,head,mats,race);
  if(race?.ears==='cat'){for(const s of [-1,1])lock(head,mats.hair,[[s*.12,.14,0],[s*.13,.33,-.01]],.07,.038,12);if(!race.tail){const tail=group(body,0,1.15,-.15);lock(tail,mats.hair,[[0,0,0],[0,-.36,-.38],[.22,-.52,-.63],[.34,-.3,-.66]],.037,.035,22);body.userData.tail=tail;}}
  rig.twoHands=weapons.mainHand?.hands===2;rig.staff=['staff','wand'].includes(weapons.mainHand?.kind);
  root.userData.limbs=[rig.legs[0],rig.arms[0],rig.legs[1],rig.arms[1]];root.userData.swordArm=rig.arms[1];
  if(race)root.scale.set(race.scale*(race.width||1),race.scale,race.scale);
  root.userData.labelHeight=2.75;
  consolidate(root);
  // Dispose unused palette entries as well as active meshes when the avatar is replaced.
  root.userData.avatarMaterials=Object.values(mats);
  animateAvatar(root,0,false,0);
  return root;
}

export function animateAvatar(root,time,moving=false,attack=0) {
  const rig=root.userData.avatarRig;if(!rig)return;
  const body=root.userData.body,phase=time*9.2;
  body.position.y=moving?Math.abs(Math.sin(phase))*.018:Math.sin(time*1.8)*.004;
  body.rotation.y=moving?Math.sin(phase)*.025:0;
  rig.head.rotation.y=Math.sin(time*.7)*.025;rig.head.rotation.z=Math.sin(time*.9)*.009;
  rig.legs.forEach((leg,i)=>{const cycle=phase+i*Math.PI;leg.rotation.x=moving?Math.sin(cycle)*.40:0;leg.userData.shin.rotation.x=moving?Math.max(0,-Math.sin(cycle))*.54:0;});
  rig.arms.forEach((arm,i)=>{
    arm.rotation.x=moving?-Math.sin(phase+i*Math.PI)*.28:-.065;
    arm.rotation.z=arm.userData.side*(.095+Math.sin(time*1.8)*.008);
    arm.userData.forearm.rotation.set(-.13-(moving?Math.max(0,Math.sin(phase+i*Math.PI))*.17:0),0,0);
  });
  if(rig.twoHands&&!rig.staff){rig.arms[1].rotation.z=-.35;rig.arms[1].rotation.x=-.38;rig.arms[1].userData.forearm.rotation.x=-.65;}
  if(attack>0){rig.arms[1].rotation.x=-1.4+Math.sin(attack*Math.PI)*.85;rig.arms[1].userData.forearm.rotation.x=-.28;body.rotation.y=-Math.sin(attack*Math.PI)*.20;}
  if(rig.twoHands&&rig.arms[1].userData.weapon){
    // Two-bone reach towards the actual weapon grip, in the body's local space.
    // This is visual articulation only; hit ranges remain server-authoritative.
    const arm=rig.arms[0],weapon=rig.arms[1].userData.weapon;
    root.updateWorldMatrix(true,true);
    const target=body.worldToLocal(weapon.localToWorld(rig.staff?V(0,.34,.02):V(0,0,-.20))).add(V(0,.065,-.012));
    const axis=target.clone().sub(arm.position),d=Math.min(.725,Math.max(.05,axis.length()));axis.normalize();
    const a=(.36**2-.373**2+d*d)/(2*d),h=Math.sqrt(Math.max(0,.36**2-a*a));
    const bend=V(-1,-.1,-.2).addScaledVector(axis,-V(-1,-.1,-.2).dot(axis)).normalize();
    const elbow=axis.clone().multiplyScalar(a).addScaledVector(bend,h);
    arm.quaternion.setFromUnitVectors(V(0,-1,0),elbow.clone().normalize());
    const lower=axis.clone().multiplyScalar(d).sub(elbow).normalize();
    arm.userData.forearm.quaternion.setFromUnitVectors(V(0,-1,0),lower).premultiply(arm.quaternion.clone().invert());
  }
  if(body.userData.cloak)body.userData.cloak.rotation.x=Math.sin(time*2.2)*.013+(moving?.06:0);
  if(body.userData.tail)body.userData.tail.rotation.y=Math.sin(time*(moving?4:1.2))*.15;
}

// Named NPC appearances match their existing portraits while preserving IDs and roles.
export function npcAppearance(npc) {
  if(npc.role==='guide')return {appearance:{gender:'female',face:'kind',race:'human',hairColor:'#653e2b',skinColor:'#dfb18b',hairStyle:'bob',classId:'warrior'},outfit:{primary:'#326c65',metal:'#d7d6c6',accent:'#b8995a',style:'mail',cape:true},weapons:{mainHand:{kind:'tome',primary:'#635041',secondary:'#b8995a',glow:'#4c9682'}}};
  if(npc.role==='smith')return {appearance:{gender:'male',face:'bold',race:'dwarf',hairColor:'#713c25',skinColor:'#c9936b',classId:'warrior'},outfit:{primary:'#655246',metal:'#9b9e96',accent:'#a78753',style:'leather',cape:false},weapons:{mainHand:null}};
  return {appearance:{gender:'female',face:'kind',race:'human',hairColor:npc.role==='tailor'?'#78412c':'#342f32',skinColor:'#bb835d',classId:'healer'},outfit:{primary:npc.role==='tailor'?'#7d667c':'#698172',metal:'#cfcab2',accent:'#bca26c',style:'tunic',cape:false},weapons:{mainHand:null}};
}
