export type PipelineStatus =
  'CREATED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';

export interface Stage {
  id: number;
  name: string;
  orderIndex: number;
  type: string;
}

export interface Pipeline {
  id: number;
  name: string;
  projectId: number;
  status: PipelineStatus;
  createdAt: string;
  stages: Stage[];
}

export interface StageExecution {
  stageId: number;
  stageName: string;
  stageType: string;
  status: PipelineStatus;
  startTime: string;
  endTime: string;
}

export interface PipelineExecution {
  id: number;
  pipelineId: number;
  commitHash: string;
  status: PipelineStatus;
  startTime: string;
  endTime: string;
  stages: StageExecution[];
}

/** used by /projects/{id}/pipelines POST */
export interface CreatePipelineRequest {
  name: string;
}
export interface TriggerExecutionRequest {
  commitHash: string;
  userId: number;
}