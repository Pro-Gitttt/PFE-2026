import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SecurityScan } from '../models/security.model';
 
@Injectable({ providedIn: 'root' })
export class SecurityService {
 
  private readonly base = `${environment.apiSecurity}/security`;
 
  constructor(private http: HttpClient) {}
 
  // GET /api/security/scan/project/{projectId}
  getByProject(projectId: number): Observable<SecurityScan[]> {
    return this.http.get<SecurityScan[]>(
      `${this.base}/scan/project/${projectId}`
    ).pipe(catchError(this.handleError));
  }
 
  // GET /api/security/scan/execution/{executionId}
  getByExecution(executionId: number): Observable<SecurityScan[]> {
    return this.http.get<SecurityScan[]>(
      `${this.base}/scan/execution/${executionId}`
    ).pipe(catchError(this.handleError));
  }
 
  private handleError(error: HttpErrorResponse) {
    const msg = error.error?.message ?? `Erreur ${error.status}`;
    console.error('[SecurityService] Error:', error.status, msg);
    return throwError(() => new Error(msg));
  }
}
 