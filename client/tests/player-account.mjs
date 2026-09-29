// Test fixtures use the same registration/login endpoints as the real client.
import assert from 'node:assert/strict';
import {randomBytes} from 'node:crypto';
export async function registerAndLogin(request,url){
  const username='test_'+randomBytes(6).toString('hex'),password=randomBytes(24).toString('base64url');
  const headers={Origin:url,'X-Aincrad-Player':'1'};
  const registered=await request.post(url+'/api/player/register',{headers,data:{username,email:username+"@example.test",password,confirmPassword:password}});
  assert.equal(registered.status(),201,await registered.text());
  const login=await request.post(url+'/api/player/login',{headers,data:{username,password}});
  assert.ok(login.ok(),await login.text());
}
