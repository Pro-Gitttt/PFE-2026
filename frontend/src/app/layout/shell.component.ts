import { Component, signal, inject } from '@angular/core';
import {
  RouterOutlet,
  RouterLink,
  RouterLinkActive,
  Router,
  NavigationEnd,
} from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter, map } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { AuthService } from '../core/services/auth.service';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
})
export class ShellComponent {

  private router = inject(Router);
  private sanitizer = inject(DomSanitizer);
  public auth = inject(AuthService);

  readonly navItems: NavItem[] = [
    { path: '/dashboard', label: 'Dashboard', icon: 'grid' },
    { path: '/projects', label: 'Projets', icon: 'folder' },
    { path: '/pipelines', label: 'Pipelines', icon: 'activity' },
    { path: '/security', label: 'Sécurité', icon: 'shield' },
    { path: '/notifications', label: 'Notifications', icon: 'bell' },
  ];

  sidebarOpen = signal(true);

  private routeTitle$ = this.router.events.pipe(
    filter(e => e instanceof NavigationEnd),
    map(() => {
      const seg = this.router.url.split('/').pop() ?? '';
      return {
        dashboard: 'Tableau de bord',
        projects: 'Projets',
        pipelines: 'Pipelines',
        security: 'Sécurité',
        notifications: 'Notifications',
      }[seg] ?? 'DevSecOps STB';
    })
  );

  readonly pageTitle = toSignal(this.routeTitle$, {
    initialValue: 'Tableau de bord',
  });

  toggleSidebar() {
    this.sidebarOpen.update(v => !v);
  }

  get initials(): string {
    const user = this.auth.currentUser?.();
    return (user?.username || '?').slice(0, 2).toUpperCase();
  }

  getIcon(name: string): SafeHtml {
    const icons: Record<string, string> = {
      grid: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <rect x="3" y="3" width="7" height="7"/>
        <rect x="14" y="3" width="7" height="7"/>
        <rect x="14" y="14" width="7" height="7"/>
        <rect x="3" y="14" width="7" height="7"/>
      </svg>`,
    };

    return this.sanitizer.bypassSecurityTrustHtml(icons[name] || '');
  }
}