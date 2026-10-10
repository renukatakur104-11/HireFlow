export const API = {
  auth: {
    register: '/auth/register',
    login: '/auth/login',
    refresh: '/auth/refresh',
    logout: '/auth/logout',
    me: '/auth/me',
  },
  students: { me: '/students/me', resume: '/students/me/resume' },
  skills: '/skills',
  drives: {
    list: '/drives',
    detail: (id: number) => `/drives/${id}`,
    apply: (id: number) => `/drives/${id}/apply`,
  },
  applications: {
    my: '/applications/my',
    withdraw: (id: number) => `/applications/${id}/withdraw`,
  },
  notifications: {
    list: '/notifications',
    unreadCount: '/notifications/unread-count',
    markRead: (id: number) => `/notifications/${id}/read`,
    markAllRead: '/notifications/read-all',
  },
  admin: {
    drives: {
      list: '/admin/drives',
      create: '/admin/drives',
      detail: (id: number) => `/admin/drives/${id}`,
      status: (id: number) => `/admin/drives/${id}/status`,
      applicants: (id: number) => `/admin/drives/${id}/applicants`,
    },
    applications: {
      status: (id: number) => `/admin/applications/${id}/status`,
      bulkStatus: '/admin/applications/bulk-status',
      history: (id: number) => `/admin/applications/${id}/history`,
    },
    students: {
      list: '/admin/students',
      detail: (id: number) => `/admin/students/${id}`,
      resume: (id: number) => `/admin/students/${id}/resume`,
      export: '/admin/students/export',
      import: '/admin/students/import',
      template: '/admin/students/template',
    },
    stats: {
      kpi: '/admin/stats/kpi',
      funnel: '/admin/stats/funnel',
      branchPlacements: '/admin/stats/branch-placements',
    },
    notifications: { previewCount: '/admin/notifications/preview-count', broadcast: '/admin/notifications/broadcast' },
    ai: { analyzeJd: '/admin/ai/analyze-jd', matchCandidates: '/admin/ai/match-candidates' },
  },
};
