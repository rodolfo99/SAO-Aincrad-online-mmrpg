import {Component, effect, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {GameService} from './game.service';

@Component({selector:'account-panel',standalone:true,imports:[FormsModule],templateUrl:'./account-panel.component.html'})
export class AccountPanelComponent {
  game=inject(GameService);mode=this.game.resetToken()?'reset':'login';username='';email='';password='';confirmPassword='';currentPassword='';
  changing=false;editingEmail=false;busy=signal(false);error=signal('');message=signal('');legacy=signal(this.legacyToken());
  constructor(){effect(()=>{if(this.game.resetToken()){this.mode='reset';this.password='';this.confirmPassword='';this.error.set('');this.message.set('');}});}
  private legacyToken(){try{return localStorage.getItem('aincrad-token-v1')||'';}catch{return '';}}
  switchMode(mode:string){this.mode=mode;this.game.resetToken.set('');this.password='';this.confirmPassword='';this.error.set('');this.message.set('');}
  async submit(){
    if(this.busy())return;this.busy.set(true);this.error.set('');this.message.set('');
    try{
      if(this.mode==='register'){
        if(this.password!==this.confirmPassword)throw Error('Las contraseñas no coinciden');
        const result=await this.game.playerRequest('register',{username:this.username,email:this.email,password:this.password,confirmPassword:this.confirmPassword});
        this.username=result.username;this.mode='login';this.message.set(result.message);
      }else if(this.mode==='forgot'){
        const result=await this.game.playerRequest('forgot-password',{email:this.email});this.message.set(result.message);
      }else if(this.mode==='reset'){
        if(this.password!==this.confirmPassword)throw Error('Las contraseñas no coinciden');
        const result=await this.game.playerRequest('reset-password',{token:this.game.resetToken(),password:this.password,confirmPassword:this.confirmPassword});
        this.game.resetToken.set('');this.game.forgetSession();this.mode='login';this.message.set(result.message);
      }else await this.game.login(this.username,this.password);
    }catch(e:any){this.error.set(e.message);}finally{this.password='';this.confirmPassword='';this.busy.set(false);}
  }
  async logout(){
    if(this.busy())return;this.busy.set(true);this.error.set('');
    try{await this.game.logout();this.changing=false;this.currentPassword='';this.password='';this.confirmPassword='';this.message.set('Sesión cerrada.');}
    catch(e:any){this.error.set(e.message);}finally{this.busy.set(false);}
  }
  async claim(){
    if(this.busy())return;this.busy.set(true);this.error.set('');
    try{const character=await this.game.playerRequest('claim-character',{legacyToken:this.legacy()});await this.game.loadSession();this.game.selectedCharacterId=character.id;localStorage.removeItem('aincrad-token-v1');this.legacy.set('');this.message.set('Personaje vinculado a tu cuenta. Ya puedes continuar.');}
    catch(e:any){this.error.set(e.message);}finally{this.busy.set(false);}
  }
  async changePassword(){
    if(this.busy())return;this.busy.set(true);this.error.set('');
    try{const result=await this.game.playerRequest('password',{currentPassword:this.currentPassword,password:this.password,confirmPassword:this.confirmPassword});this.game.forgetSession();this.changing=false;this.message.set(result.message);}
    catch(e:any){this.error.set(e.message);}finally{this.currentPassword='';this.password='';this.confirmPassword='';this.busy.set(false);}
  }
  async changeEmail(){
    if(this.busy())return;this.busy.set(true);this.error.set('');
    try{const result=await this.game.playerRequest('email',{currentPassword:this.currentPassword,email:this.email});this.game.forgetSession();this.editingEmail=false;this.message.set(result.message);}
    catch(e:any){this.error.set(e.message);}finally{this.currentPassword='';this.busy.set(false);}
  }
}
