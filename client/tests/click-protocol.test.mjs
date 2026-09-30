import test from 'node:test';
import assert from 'node:assert/strict';
import * as THREE from 'three';
import {WorldRenderer} from '../src/app/world-renderer.js';

function renderer(){
  const sent=[];
  const view=Object.create(WorldRenderer.prototype);
  Object.assign(view,{
    game:{id:'self',connection:()=> 'online',send:message=>sent.push(message)},
    scene:{},canvas:{getBoundingClientRect:()=>({left:100,top:50,width:800,height:600})},
    camera:{},plane:{},entities:new Map(),resourceMeshes:new Map(),time:4,
    marker:{position:new THREE.Vector3(),visible:false},keys:new Set(['KeyW']),
    ray:{setFromCamera:()=>{},intersectObjects:()=>[],ray:{intersectPlane:(_plane,point)=>point.set(12.25,0,-7.75)}}
  });
  return {view,sent};
}

test('ground click sends only the existing move intent and does not change player position',()=>{
  const {view,sent}=renderer();const player={x:0,z:16};view.game.me=()=>player;
  view.click({clientX:500,clientY:350});
  assert.deepEqual(sent,[{type:'move',x:12.25,z:-7.75}]);
  assert.deepEqual(player,{x:0,z:16});assert.equal(view.marker.visible,true);
});

test('stop clears WASD and sends the existing stop command',()=>{
  const {view,sent}=renderer();view.stop();
  assert.equal(view.keys.size,0);assert.deepEqual(sent,[{type:'stop'}]);
});

test('target and resource picking retain priority over ground movement',()=>{
  for(const kind of ['monster','resource']){
    const {view,sent}=renderer();const object=new THREE.Group();
    if(kind==='monster'){object.userData.entityId='boar-1';view.entities.set('boar-1',{kind,mesh:object});}
    else{object.userData.nodeId='vein-1';view.resourceMeshes.set('vein-1',{mesh:object});}
    view.ray.intersectObjects=objects=>objects.includes(object)?[{object}]:[];
    view.click({clientX:500,clientY:350});
    assert.deepEqual(sent,[kind==='monster'?{type:'target',id:'boar-1',kind:'monster'}:{type:'gather',nodeId:'vein-1'}]);
  }
});

test('offline clicks do not submit a move',()=>{
  const {view,sent}=renderer();view.game.connection=()=> 'offline';view.click({clientX:500,clientY:350});assert.deepEqual(sent,[]);
});
