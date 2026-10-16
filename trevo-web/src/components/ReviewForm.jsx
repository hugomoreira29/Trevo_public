import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Star } from "lucide-react";
import { createReview } from "../services/reviewService";
import Alert from "./Alert";

function getErrorMessage(error, fallback, t) {
  if (error.response?.status === 409) {
    return t("alreadyReviewed");
  }

  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

export default function ReviewForm({ shopId }) {
  const { t } = useTranslation("reviews");
  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [comment, setComment] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");
  const [hasSubmitted, setHasSubmitted] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();

    if (rating === 0 || isSubmitting || hasSubmitted) return;

    setSuccess("");
    setError("");
    setIsSubmitting(true);

    try {
      await createReview(shopId, {
        rating,
        comment: comment.trim() ? comment.trim() : null,
      });
      setSuccess(t("reviewSent"));
      setHasSubmitted(true);
      setComment("");
      setRating(0);
    } catch (submitError) {
      setError(getErrorMessage(submitError, t("submitFailed"), t));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section>
      <h2 className="text-lg font-semibold tracking-tight">{t("reviewShop")}</h2>

      {hasSubmitted ? (
        <Alert variant="success" className="mt-4">{success}</Alert>
      ) : (
        <form className="mt-4 flex flex-col gap-4" onSubmit={handleSubmit}>
          <div>
            <p className="field-label">{t("rating")}</p>
            <div className="mt-2 flex gap-1" aria-label={t("stars", { rating: t("rating") })}>
              {[1, 2, 3, 4, 5].map((star) => {
                const filled = (hoverRating || rating) >= star;
                return (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    onMouseEnter={() => setHoverRating(star)}
                    onMouseLeave={() => setHoverRating(0)}
                    aria-label={t(star === 1 ? "starOne" : "starMany", { count: star })}
                    aria-pressed={rating >= star}
                    className="p-1 -m-1 transition-transform hover:scale-110"
                  >
                    <Star
                      className={
                        "w-7 h-7 " +
                        (filled ? "text-amber-500 fill-amber-400" : "text-ink-300")
                      }
                      strokeWidth={1.5}
                    />
                  </button>
                );
              })}
            </div>
          </div>

          <label className="field">
            <span className="field-label">{t("optionalComment")}</span>
            <textarea
              value={comment}
              onChange={(event) => setComment(event.target.value)}
              rows={4}
              className="input resize-y"
            />
          </label>

          <div>
            <button
              type="submit"
              disabled={rating === 0 || isSubmitting}
              className="btn-primary"
            >
              {isSubmitting ? t("submitting") : t("submitReview")}
            </button>
          </div>

          {error && <Alert variant="error">{error}</Alert>}
        </form>
      )}
    </section>
  );
}
