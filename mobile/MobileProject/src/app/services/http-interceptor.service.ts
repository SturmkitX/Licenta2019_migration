import { Injectable } from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from "@angular/common/http";
import {Observable} from "rxjs";

@Injectable()
export class HttpInterceptorService implements HttpInterceptor{

  constructor() { }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (localStorage.getItem('userToken')) {
      req = req.clone({
        setHeaders: {'Authorization': 'Bearer ' + localStorage.getItem('userToken')}
      });
    }
    return next.handle(req);
  }
}
