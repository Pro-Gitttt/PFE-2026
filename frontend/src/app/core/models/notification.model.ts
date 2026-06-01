export type EventType =
  | 'PIPELINE_SUCCESS'
  | 'PIPELINE_FAILED'
  | 'DEPLOYMENT_FAILED'
  | 'SECURITY_BLOCKED'
  | 'SECURITY_WARNING'
  | 'UPDATE_PROJECT'
  | string;

export type NChannel = 'EMAIL' | 'SLACK';
export type NStatus  = 'PENDING' | 'SENT' | 'FAILED';

export interface NotificationItem {
  id:                  number;
  eventType:           EventType;
  channel:             NChannel;
  status:              NStatus;
  recipient:           string;
  message:             string;
  projectId:           number;
  pipelineExecutionId: number;
  createdAt:           string | null;
  sentAt:              string | null;
}
