import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {LogInStatus} from "../models/login-status";

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private http: HttpClient) { }

  logIn(email: string, password: string): Observable<LogInStatus> {
    return this.http.post<LogInStatus>('http://localhost:3000/public/login', {email: email, password: password});
  }
}
