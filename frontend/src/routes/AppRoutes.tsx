import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { RoleRoute } from './RoleRoute';
import { AppLayout } from '../components/layout/AppLayout';
import { LoginPage } from '../features/auth/LoginPage';
import { RegisterPage } from '../features/auth/RegisterPage';
import { StudentProfilePage } from '../features/student/profile/StudentProfilePage';
import { DrivesBrowsePage } from '../features/student/drives/DrivesBrowsePage';
import { DriveDetailPage } from '../features/student/drives/DriveDetailPage';
import { MyApplicationsPage } from '../features/student/applications/MyApplicationsPage';
import { StudentDashboardPage } from '../features/student/dashboard/StudentDashboardPage';
import { NotificationsPage } from '../features/notifications/NotificationsPage';
import { AdminDashboardPage } from '../features/admin/dashboard/AdminDashboardPage';
import { AdminDrivesPage } from '../features/admin/drives/AdminDrivesPage';
import { DriveApplicantsPage } from '../features/admin/applicants/DriveApplicantsPage';
import { AdminStudentsPage } from '../features/admin/students/AdminStudentsPage';
import { AdminNotificationsPage } from '../features/admin/notifications/AdminNotificationsPage';

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route element={<RoleRoute role="STUDENT" />}>
              <Route path="/dashboard" element={<StudentDashboardPage />} />
              <Route path="/profile" element={<StudentProfilePage />} />
              <Route path="/drives" element={<DrivesBrowsePage />} />
              <Route path="/drives/:id" element={<DriveDetailPage />} />
              <Route path="/applications" element={<MyApplicationsPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
            </Route>
            <Route element={<RoleRoute role="ADMIN" />}>
              <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
              <Route path="/admin/drives" element={<AdminDrivesPage />} />
              <Route path="/admin/drives/:driveId/applicants" element={<DriveApplicantsPage />} />
              <Route path="/admin/students" element={<AdminStudentsPage />} />
              <Route path="/admin/notifications" element={<AdminNotificationsPage />} />
            </Route>
          </Route>
        </Route>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
