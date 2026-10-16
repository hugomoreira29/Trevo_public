import { useCallback, useEffect, useRef, useState } from "react";
import { Bell, Check } from "lucide-react";
import { useAuth } from "../hooks/useAuth";
import { normalizeRole, ROLES } from "../lib/roles";
import {
  getNotifications,
  markNotificationAsRead,
} from "../services/notificationService";

const POLLING_INTERVAL_MS = 60000;
const PAGE_SIZE = 20;

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

function notificationsFromResponse(response) {
  if (Array.isArray(response)) return response;
  return response?.notifications || [];
}

function formatDate(value) {
  if (!value) return "";
  return new Intl.DateTimeFormat("pt-PT", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function NotificationIndicator() {
  const { isAuthenticated, role } = useAuth();
  const isShopOwner =
    isAuthenticated && normalizeRole(role) === ROLES.SHOP_OWNER;
  const containerRef = useRef(null);
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [error, setError] = useState("");
  const [readingId, setReadingId] = useState(null);

  const loadNotifications = useCallback(async () => {
    try {
      const response = await getNotifications({ page: 0, size: PAGE_SIZE });
      setNotifications(notificationsFromResponse(response));
      setUnreadCount(response?.unreadCount || 0);
      setError("");
    } catch (loadError) {
      setError(getErrorMessage(loadError, "Não foi possível carregar notificações."));
    }
  }, []);

  useEffect(() => {
    if (!isShopOwner) return undefined;

    loadNotifications();
    const intervalId = window.setInterval(loadNotifications, POLLING_INTERVAL_MS);
    return () => window.clearInterval(intervalId);
  }, [isShopOwner, loadNotifications]);

  useEffect(() => {
    if (!isOpen) return undefined;

    function handlePointerDown(event) {
      if (!containerRef.current?.contains(event.target)) {
        setIsOpen(false);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, [isOpen]);

  if (!isShopOwner) return null;

  async function handleMarkAsRead(notification) {
    if (notification.read) return;

    setReadingId(notification.id);
    setError("");

    try {
      const updated = await markNotificationAsRead(notification.id);
      const next = {
        ...notification,
        ...(updated || {}),
        read: updated?.read ?? true,
      };

      setNotifications((current) =>
        current.map((c) => (c.id === notification.id ? next : c))
      );
      setUnreadCount((c) => Math.max(c - 1, 0));
    } catch (readError) {
      setError(getErrorMessage(readError, "Não foi possível marcar como lida."));
    } finally {
      setReadingId(null);
    }
  }

  return (
    <div className="fixed right-4 top-20 z-40" ref={containerRef}>
      <button
        type="button"
        aria-label="Notificações"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((v) => !v)}
        className="relative w-10 h-10 inline-flex items-center justify-center rounded-full border border-ink-200 bg-white hover:bg-ink-50 shadow-sm transition-colors"
      >
        <Bell className="w-[18px] h-[18px] text-ink-800" strokeWidth={1.7} />
        {unreadCount > 0 && (
          <span className="absolute -right-0.5 -top-0.5 min-w-[18px] h-[18px] px-1 rounded-full bg-clover-500 text-[10px] font-bold leading-[18px] text-white text-center tabular">
            {unreadCount}
          </span>
        )}
      </button>

      <div aria-live="polite">
        {isOpen && (
          <section className="mt-2 w-80 rounded-md border border-ink-200 bg-white shadow-lg overflow-hidden">
            <div className="flex items-center justify-between gap-4 px-4 py-3 border-b border-ink-100">
              <h2 className="font-semibold text-sm">Notificações</h2>
              <span className="text-xs text-ink-500">
                {unreadCount} por ler
              </span>
            </div>

            {error && (
              <p className="px-4 py-3 text-sm text-red-700 bg-red-50 border-b border-red-100" role="alert">
                {error}
              </p>
            )}

            {notifications.length === 0 && !error && (
              <p className="px-4 py-8 text-center text-sm text-ink-500">
                Sem notificações.
              </p>
            )}

            {notifications.length > 0 && (
              <ul className="max-h-96 overflow-y-auto divide-y divide-ink-100">
                {notifications.map((notification) => (
                  <li
                    key={notification.id}
                    className={
                      "px-4 py-3 " +
                      (notification.read ? "bg-white" : "bg-clover-50/40")
                    }
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0 flex-1">
                        <p className="font-semibold text-sm flex items-center gap-1.5">
                          {!notification.read && (
                            <span className="w-1.5 h-1.5 rounded-full bg-clover-500 shrink-0" />
                          )}
                          {notification.title}
                        </p>
                        <p className="mt-1 text-sm text-ink-700 leading-snug">
                          {notification.message}
                        </p>
                        {notification.createdAt && (
                          <p className="mt-2 text-[11px] text-ink-500 tabular">
                            {formatDate(notification.createdAt)}
                          </p>
                        )}
                      </div>
                    </div>

                    {!notification.read && (
                      <button
                        type="button"
                        onClick={() => handleMarkAsRead(notification)}
                        disabled={readingId === notification.id}
                        className="mt-2 inline-flex items-center gap-1 text-xs text-clover-700 hover:text-clover-800 font-medium disabled:opacity-50"
                      >
                        <Check className="w-3.5 h-3.5" strokeWidth={2} />
                        {readingId === notification.id
                          ? "A marcar..."
                          : "Marcar como lida"}
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>
        )}
      </div>
    </div>
  );
}
