import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {

  const auth = inject(AuthService);
  const platformId = inject(PLATFORM_ID);

  let authReq = req;

  if (isPlatformBrowser(platformId)) {
    const token = auth.token;

    // ❌ NEVER attach token to auth endpoints
    const isAuthCall = req.url.includes('/auth');

    if (token && !isAuthCall) {
      authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
  }

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && isPlatformBrowser(platformId)) {
        auth.logout();
      }
      return throwError(() => err);
    })
  );
};