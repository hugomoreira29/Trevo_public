import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { normalizeRole } from "../lib/roles";

export default function RoleRoute({ allowedRoles }) {
  const { isAuthenticated, role } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const normalizedRole = normalizeRole(role);
  const normalizedAllowedRoles = allowedRoles
    .map((allowedRole) => {
      const normalizedAllowedRole = normalizeRole(allowedRole);

      if (!normalizedAllowedRole) {
        console.warn(`Unsupported role passed to RoleRoute: ${allowedRole}`);
      }

      return normalizedAllowedRole;
    })
    .filter(Boolean);

  if (!normalizedAllowedRoles.includes(normalizedRole)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
}
