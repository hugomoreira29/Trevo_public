import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import {
  getCurrentUserProfile,
  updateCurrentUserProfile,
} from "../services/userService";
import LoadingState from "../components/LoadingState";
import Alert from "../components/Alert";

const initialForm = {
  fullName: "",
  email: "",
  phone: "",
  currentPassword: "",
  newPassword: "",
};

function getErrorMessage(error, fallback) {
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

export default function Profile() {
  const { t } = useTranslation("profile");
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    let active = true;

    async function loadProfile() {
      try {
        const profile = await getCurrentUserProfile();

        if (active) {
          setForm({
            fullName: profile.fullName || "",
            email: profile.email || "",
            phone: profile.phone || "",
            currentPassword: "",
            newPassword: "",
          });
          setError("");
        }
      } catch (loadError) {
        if (active) {
          setError(getErrorMessage(loadError, t("loadProfileFailed")));

          if (loadError.response?.status === 401) {
            logout();
            navigate("/login", { replace: true });
          }
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    loadProfile();

    return () => {
      active = false;
    };
  }, [logout, navigate, t]);

  function handleChange(event) {
    setForm((prev) => ({
      ...prev,
      [event.target.name]: event.target.value,
    }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!form.fullName.trim() || !form.email.trim()) {
      setError(t("fillNameEmail"));
      return;
    }

    if (
      (form.currentPassword && !form.newPassword) ||
      (!form.currentPassword && form.newPassword)
    ) {
      setError(t("fillPasswords"));
      return;
    }

    if (form.newPassword && form.newPassword.length < 8) {
      setError(t("newPasswordTooShort"));
      return;
    }

    const payload = {
      fullName: form.fullName.trim(),
      email: form.email.trim(),
      phone: form.phone.trim() || null,
    };

    if (form.currentPassword || form.newPassword) {
      payload.currentPassword = form.currentPassword;
      payload.newPassword = form.newPassword;
    }

    try {
      setIsSubmitting(true);
      const updatedProfile = await updateCurrentUserProfile(payload);

      setForm({
        fullName: updatedProfile.fullName || "",
        email: updatedProfile.email || "",
        phone: updatedProfile.phone || "",
        currentPassword: "",
        newPassword: "",
      });
      setSuccess(t("profileUpdated"));
    } catch (updateError) {
      setError(getErrorMessage(updateError, t("updateProfileFailed")));

      if (updateError.response?.status === 401) {
        logout();
        navigate("/login", { replace: true });
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="page-shell-narrow">
      <h1 className="h-page">{t("profile")}</h1>
      <p className="mt-2 text-ink-500">Atualiza os teus dados pessoais e palavra-passe.</p>

      <div className="mt-8">
        {isLoading && <LoadingState label={t("loadingProfile")} />}
        {error && <Alert variant="error" className="mb-4">{error}</Alert>}
        {success && <Alert variant="success" className="mb-4">{success}</Alert>}

        {!isLoading && (
          <form onSubmit={handleSubmit} className="card card-pad p-6 flex flex-col gap-5">
            <section>
              <h2 className="text-base font-semibold mb-4">Dados pessoais</h2>
              <div className="grid sm:grid-cols-2 gap-4">
                <div className="field">
                  <label className="field-label" htmlFor="p-fullName">{t("name")}</label>
                  <input id="p-fullName" className="input" type="text" name="fullName" value={form.fullName} onChange={handleChange} />
                </div>
                <div className="field">
                  <label className="field-label" htmlFor="p-email">Email</label>
                  <input id="p-email" className="input" type="email" name="email" value={form.email} onChange={handleChange} />
                </div>
                <div className="field sm:col-span-2">
                  <label className="field-label" htmlFor="p-phone">{t("phone")}</label>
                  <input id="p-phone" className="input" type="text" name="phone" value={form.phone} onChange={handleChange} />
                </div>
              </div>
            </section>

            <hr className="border-ink-100" />

            <section>
              <h2 className="text-base font-semibold mb-1">Alterar palavra-passe</h2>
              <p className="text-xs text-ink-500 mb-4">Deixa em branco para manter a atual.</p>
              <div className="grid sm:grid-cols-2 gap-4">
                <div className="field">
                  <label className="field-label" htmlFor="p-curPwd">{t("currentPassword", { ns: "auth" })}</label>
                  <input id="p-curPwd" className="input" type="password" name="currentPassword" value={form.currentPassword} onChange={handleChange} autoComplete="current-password" />
                </div>
                <div className="field">
                  <label className="field-label" htmlFor="p-newPwd">{t("newPassword", { ns: "auth" })}</label>
                  <input id="p-newPwd" className="input" type="password" name="newPassword" value={form.newPassword} onChange={handleChange} autoComplete="new-password" />
                </div>
              </div>
            </section>

            <div className="flex justify-end">
              <button type="submit" disabled={isSubmitting} className="btn-primary">
                {isSubmitting ? t("saving") : t("saveChanges")}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
