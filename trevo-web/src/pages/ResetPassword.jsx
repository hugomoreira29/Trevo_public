import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { confirmPasswordReset, validatePasswordResetToken } from "../lib/authApi";
import Alert from "../components/Alert";
import LoadingState from "../components/LoadingState";

export default function ResetPassword() {
  const { t } = useTranslation("auth");
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [tokenStatus, setTokenStatus] = useState("checking");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    let ignore = false;

    async function checkToken() {
      if (!token) {
        setTokenStatus("invalid");
        setError(t("missingResetToken"));
        return;
      }

      try {
        await validatePasswordResetToken(token);
        if (!ignore) {
          setTokenStatus("valid");
        }
      } catch (validationError) {
        if (!ignore) {
          setTokenStatus("invalid");
          setError(validationError.message);
        }
      }
    }

    checkToken();

    return () => {
      ignore = true;
    };
  }, [token, t]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    if (newPassword.length < 8) {
      setError(t("passwordTooShort"));
      return;
    }

    if (newPassword !== confirmPassword) {
      setError(t("passwordsMismatch"));
      return;
    }

    try {
      setIsSubmitting(true);
      await confirmPasswordReset(token, newPassword);
      navigate("/login", {
        replace: true,
        state: { message: t("passwordReset") },
      });
    } catch (submitError) {
      setError(submitError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="min-h-[calc(100vh-64px)] flex items-center justify-center px-6 py-12 bg-ink-50/30">
      <div className="card card-pad w-full max-w-md p-8">
        <h1 className="text-2xl font-semibold tracking-tight mb-1.5">{t("resetPasswordTitle")}</h1>

        {tokenStatus === "checking" && <LoadingState label={t("validatingLink")} />}

        {error && <Alert variant="error" className="mb-4">{error}</Alert>}

        {tokenStatus === "valid" && (
          <form onSubmit={handleSubmit} className="flex flex-col gap-4 mt-2">
            <div className="field">
              <label className="field-label" htmlFor="newPassword">{t("newPassword")}</label>
              <input
                id="newPassword"
                className="input"
                type="password"
                name="newPassword"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                autoComplete="new-password"
              />
            </div>

            <div className="field">
              <label className="field-label" htmlFor="confirmPassword">{t("confirmPassword")}</label>
              <input
                id="confirmPassword"
                className="input"
                type="password"
                name="confirmPassword"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                autoComplete="new-password"
              />
            </div>

            <button type="submit" disabled={isSubmitting} className="btn-primary w-full mt-2 py-3">
              {isSubmitting ? t("saving", { ns: "common" }) : t("resetPasswordSubmit")}
            </button>
          </form>
        )}

        {tokenStatus === "invalid" && (
          <Link to="/forgot-password" className="mt-4 inline-block text-sm text-clover-700 hover:text-clover-800 font-medium">
            {t("requestNewLink")}
          </Link>
        )}
      </div>
    </div>
  );
}
