export type SeverityLevel = 'LOW'|'MEDIUM'|'HIGH'|'CRITICAL';
export type ScanType      = 'SAST'|'SCA'|'SECRET';
export interface Vulnerability { id: number; type: ScanType; severity: SeverityLevel; description: string; filePath: string; cve: string; }
export interface SecurityScan  { id: number; projectId: number; executionId: number; score: number; blocked: boolean; createdAt: string; vulnerabilities: Vulnerability[]; }