export function movement(keys, yaw = 0) {
  let x = (keys.has('KeyD') || keys.has('ArrowRight') ? 1 : 0) - (keys.has('KeyA') || keys.has('ArrowLeft') ? 1 : 0);
  let z = (keys.has('KeyS') || keys.has('ArrowDown') ? 1 : 0) - (keys.has('KeyW') || keys.has('ArrowUp') ? 1 : 0);
  const len = Math.max(1, Math.hypot(x,z)); x/=len; z/=len;
  return {dx:x*Math.cos(yaw)+z*Math.sin(yaw), dz:z*Math.cos(yaw)-x*Math.sin(yaw)};
}
export function pointerNdc(clientX, clientY, rect) {return {x:(clientX-rect.left)/rect.width*2-1,y:-(clientY-rect.top)/rect.height*2+1};}
export function isTyping(target) {return !!target?.closest?.('input, textarea, select, [contenteditable="true"]');}
export function approachAngle(current,target,alpha){return current+Math.atan2(Math.sin(target-current),Math.cos(target-current))*alpha;}
