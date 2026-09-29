import test from 'node:test';import assert from 'node:assert/strict';
import {movement,pointerNdc,isTyping,approachAngle} from '../src/app/input.mjs';
test('W and Up move away from a camera at positive Z',()=>{assert.deepEqual(movement(new Set(['KeyW'])),{dx:0,dz:-1});assert.deepEqual(movement(new Set(['ArrowUp'])),{dx:0,dz:-1});});
test('diagonals have the same speed and opposed keys cancel',()=>{let v=movement(new Set(['KeyW','KeyD']));assert.ok(Math.abs(Math.hypot(v.dx,v.dz)-1)<1e-10);assert.deepEqual(movement(new Set(['KeyW','KeyS'])),{dx:0,dz:0});});
test('forward follows rotated camera',()=>{let v=movement(new Set(['KeyW']),Math.PI/2);assert.ok(v.dx<-.999);assert.ok(Math.abs(v.dz)<1e-10);});
test('picking uses canvas rectangle after layout shifts',()=>{assert.deepEqual(pointerNdc(700,350,{left:200,top:100,width:1000,height:500}),{x:0,y:0});});
test('focused text fields suppress world shortcuts',()=>{assert.equal(isTyping({closest:()=>({})}),true);assert.equal(isTyping({closest:()=>null}),false);});
test('heading takes shortest path at wrap boundary',()=>{const next=approachAngle(3.13,-3.13,.5);assert.ok(Math.abs(next-Math.PI)<.001);});
