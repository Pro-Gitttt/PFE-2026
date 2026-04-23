export type VcsType = 'GITHUB'|'GITLAB'|'BITBUCKET';
export interface Project              { id: number; name: string; repositoryUrl: string; branch: string; owner: string; vcsType: VcsType; createdAt: string; }
export interface CreateProjectRequest { name: string; repositoryUrl: string; branch: string; owner: string; vcsType: VcsType; }