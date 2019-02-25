import {Component, OnInit} from '@angular/core';
import {ToastController} from "@ionic/angular";
import {AuthService} from "../../services/auth.service";
import {UserService} from "../../services/user.service";

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
})
export class HomePage implements OnInit{

  constructor(private toastController: ToastController,
              private authService: AuthService,
              private userService: UserService) {}

  public username: string = 'martin@testus.com';
  public password: string = 'testpass';
  public isConnected: boolean = false;


  logIn(event) {
    this.authService.logIn(this.username, this.password).subscribe(status => {
      // succeeded
      localStorage.setItem('userToken', status.token);
      this.toastController.create({
        message: 'Successfully logged in',
        duration: 3000
      }).then(value => {
        value.present();
      });
      this.isConnected = true;
    }, error => {
      console.log(error);
    }, () => {
      console.log('The login process is complete!');
    })

  }

  getInfo(event) {
    this.userService.getPersonalInfo().subscribe(user => {
      console.log(user);
    }, error => {
      console.log(error);
    });
  }

  ngOnInit(): void {
  }
}
