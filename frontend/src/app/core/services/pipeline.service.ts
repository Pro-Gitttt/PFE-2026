import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

import {
  Pipeline,
  PipelineExecution,
  TriggerExecutionRequest
} from '../models/pipeline.model';

@Injectable({ providedIn: 'root' })
export class PipelineService {

  private base = `${environment.apiPipeline}`;

  constructor(private http: HttpClient) {}

  // ================= PIPELINES =================
  getByProject(projectId: number): Observable<Pipeline[]> {
    return this.http.get<Pipeline[]>(
      `${this.base}/projects/${projectId}/pipelines`
    );
  }

  getById(id: number): Observable<Pipeline> {
    return this.http.get<Pipeline>(
      `${this.base}/pipelines/${id}`
    );
  }

  // ================= EXECUTIONS =================
  getExecutions(pipelineId: number): Observable<PipelineExecution[]> {
    return this.http.get<PipelineExecution[]>(
      `${this.base}/pipelines/${pipelineId}/executions`
    );
  }

  trigger(
    pipelineId: number,
    req: TriggerExecutionRequest
  ): Observable<PipelineExecution> {
    return this.http.post<PipelineExecution>(
      `${this.base}/pipelines/${pipelineId}/executions`,
      req
    );
  }
}