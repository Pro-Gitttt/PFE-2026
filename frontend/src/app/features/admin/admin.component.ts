import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface AdminUser { id:number; username:string; email:string; role:string; enabled:boolean; createdAt:string; }

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
})
export class AdminComponent implements OnInit {

  private http = inject(HttpClient);
  private base = `${environment.apiAuth}/admin`;

  users    = signal<AdminUser[]>([]);
  loading  = signal(true);
  error    = signal('');
  updating = signal<number | null>(null);

  readonly roles = ['ADMIN', 'DEV', 'DEVOPS', 'AUDITOR'];

  ngOnInit(): void { this.loadUsers(); }

  loadUsers(): void {
    this.loading.set(true);
    this.http.get<AdminUser[]>(`${this.base}/users`).subscribe({
      next: data => { this.users.set(data); this.loading.set(false); },
      error: e   => { this.error.set(e?.error?.message ?? 'Erreur chargement'); this.loading.set(false); },
    });
  }

  toggleEnabled(user: AdminUser): void {
    this.updating.set(user.id);
    this.http.put<AdminUser>(`${this.base}/users/${user.id}/enabled?enabled=${!user.enabled}`, {}).subscribe({
      next: updated => { this.users.update(list => list.map(u => u.id === updated.id ? updated : u)); this.updating.set(null); },
      error: () => this.updating.set(null),
    });
  }

  changeRole(user: AdminUser, newRole: string): void {
    this.updating.set(user.id);
    this.http.put<AdminUser>(`${this.base}/users/${user.id}/role?role=${newRole}`, {}).subscribe({
      next: updated => { this.users.update(list => list.map(u => u.id === updated.id ? updated : u)); this.updating.set(null); },
      error: () => this.updating.set(null),
    });
  }

  deleteUser(user: AdminUser): void {
    if (!confirm(`Supprimer "${user.username}" ?`)) return;
    this.updating.set(user.id);
    this.http.delete(`${this.base}/users/${user.id}`).subscribe({
      next:  () => { this.users.update(list => list.filter(u => u.id !== user.id)); this.updating.set(null); },
      error: () => this.updating.set(null),
    });
  }

  roleColor(role: string): string { return ({ADMIN:'#7c3aed',DEV:'#2563eb',DEVOPS:'#059669',AUDITOR:'#d97706'})[role] ?? '#6b7280'; }
  roleIcon(role: string):  string { return ({ADMIN:'👑',DEV:'💻',DEVOPS:'🚀',AUDITOR:'🔍'})[role] ?? '👤'; }

  get totalUsers()    { return this.users().length; }
  get activeUsers()   { return this.users().filter(u => u.enabled).length; }
  get disabledUsers() { return this.users().filter(u => !u.enabled).length; }
  get adminCount()    { return this.users().filter(u => u.role === 'ADMIN').length; }
}
