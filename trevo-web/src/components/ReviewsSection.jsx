import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Star, MessageSquare, RefreshCw } from "lucide-react";
import { useAuth } from "../hooks/useAuth";
import { normalizeRole, ROLES } from "../lib/roles";
import { getShopReviews, respondToReview } from "../services/reviewService";
import Alert from "./Alert";
import LoadingState from "./LoadingState";

const PAGE_SIZE = 5;

function getErrorMessage(error, fallback, t) {
  if (error.response?.status === 409) return t("reviewAlreadyAnswered");
  return (
    error.response?.data?.message ||
    error.response?.data?.detail ||
    fallback
  );
}

function formatDate(value, t) {
  if (!value) return t("dateUnavailable");
  return new Intl.DateTimeFormat("pt-PT", { dateStyle: "short" }).format(new Date(value));
}

function Stars({ rating, size = "sm" }) {
  const { t } = useTranslation("reviews");
  const rounded = Math.round(Number(rating || 0));
  const cls = size === "lg" ? "w-5 h-5" : "w-4 h-4";

  return (
    <span aria-label={t("stars", { rating: Number(rating || 0).toFixed(1) })} className="inline-flex gap-0.5">
      {[1, 2, 3, 4, 5].map((star) => (
        <Star
          key={star}
          className={cls + " " + (star <= rounded ? "text-amber-500 fill-amber-400" : "text-ink-300")}
          strokeWidth={1.5}
        />
      ))}
    </span>
  );
}

function getResponseText(response) {
  if (!response) return "";
  return response.response || response.text || response.content || "";
}

export default function ReviewsSection({ shopId }) {
  const { t } = useTranslation("reviews");
  const { isAuthenticated, role } = useAuth();
  const [reviews, setReviews] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [averageRating, setAverageRating] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [activeResponseReviewId, setActiveResponseReviewId] = useState(null);
  const [responseText, setResponseText] = useState("");
  const [responseError, setResponseError] = useState("");
  const [isSubmittingResponse, setIsSubmittingResponse] = useState(false);

  const canRespond =
    isAuthenticated && normalizeRole(role) === ROLES.SHOP_OWNER;

  const loadReviews = useCallback(
    async (nextPage = 0, mode = "replace", options = {}) => {
      const { isActive = () => true } = options;

      setIsLoading(true);
      setError("");

      try {
        const response = await getShopReviews(shopId, { page: nextPage, size: PAGE_SIZE });

        if (isActive()) {
          setReviews((current) =>
            mode === "append"
              ? [...current, ...(response.reviews || [])]
              : response.reviews || []
          );
          setAverageRating(response.averageRating ?? null);
          setTotalPages(response.totalPages || 0);
          setPage(response.page ?? nextPage);
        }
      } catch (loadError) {
        if (isActive()) setError(getErrorMessage(loadError, t("loadFailed"), t));
      } finally {
        if (isActive()) setIsLoading(false);
      }
    },
    [shopId, t]
  );

  useEffect(() => {
    let active = true;
    loadReviews(0, "replace", { isActive: () => active });
    return () => {
      active = false;
    };
  }, [loadReviews]);

  function openResponseForm(reviewId) {
    setActiveResponseReviewId(reviewId);
    setResponseText("");
    setResponseError("");
  }

  function closeResponseForm() {
    setActiveResponseReviewId(null);
    setResponseText("");
    setResponseError("");
  }

  async function handleResponseSubmit(event, reviewId) {
    event.preventDefault();

    const trimmedResponse = responseText.trim();
    if (!trimmedResponse) return;

    setIsSubmittingResponse(true);
    setResponseError("");

    try {
      const reviewResponse = await respondToReview(reviewId, trimmedResponse);

      setReviews((current) =>
        current.map((review) =>
          review.id === reviewId ? { ...review, response: reviewResponse } : review
        )
      );
      closeResponseForm();
    } catch (submitError) {
      setResponseError(getErrorMessage(submitError, t("responseFailed"), t));
    } finally {
      setIsSubmittingResponse(false);
    }
  }

  const hasMore = page < totalPages - 1;
  const isEmpty = !isLoading && !error && averageRating == null && reviews.length === 0;

  return (
    <section>
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-lg font-semibold tracking-tight">{t("reviews")}</h2>
        {averageRating != null && (
          <div className="flex items-center gap-2 text-sm">
            <Stars rating={averageRating} />
            <span className="tabular font-semibold">{Number(averageRating).toFixed(1)}</span>
          </div>
        )}
      </div>

      {isLoading && <LoadingState label={t("loading")} />}

      {error && (
        <div className="mt-3 space-y-2">
          <Alert variant="error">{error}</Alert>
          {reviews.length === 0 && (
            <button type="button" onClick={() => loadReviews(0, "replace")} className="btn-secondary">
              <RefreshCw className="w-4 h-4" strokeWidth={1.7} />
              {t("retry")}
            </button>
          )}
        </div>
      )}

      {isEmpty && <p className="mt-3 text-sm text-ink-500">{t("empty")}</p>}

      {reviews.length > 0 && (
        <ul className="mt-4 flex flex-col gap-4">
          {reviews.map((review) => (
            <li key={review.id} className="card card-pad p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="font-semibold">{review.buyerName}</p>
                <Stars rating={review.rating} />
              </div>
              <p className="text-xs text-ink-500 mt-0.5">{formatDate(review.createdAt, t)}</p>
              {review.comment && (
                <p className="mt-3 text-sm leading-relaxed text-ink-700">{review.comment}</p>
              )}
              {review.response && (
                <div className="mt-3 border-l-2 border-clover-400 bg-clover-50/50 pl-3 py-2 rounded-r-sm">
                  <p className="text-xs font-semibold text-clover-700 flex items-center gap-1.5">
                    <MessageSquare className="w-3 h-3" strokeWidth={1.8} />
                    {t("shopResponse")}
                  </p>
                  <p className="mt-1 text-sm text-ink-800">{getResponseText(review.response)}</p>
                </div>
              )}
              {canRespond && !review.response && (
                <div className="mt-3">
                  {activeResponseReviewId === review.id ? (
                    <form onSubmit={(event) => handleResponseSubmit(event, review.id)} className="flex flex-col gap-2">
                      <label className="sr-only" htmlFor={`review-response-${review.id}`}>
                        {t("response")}
                      </label>
                      <textarea
                        id={`review-response-${review.id}`}
                        value={responseText}
                        onInput={(event) => setResponseText(event.target.value)}
                        className="input resize-y"
                        rows={3}
                      />
                      {responseError && <Alert variant="error">{responseError}</Alert>}
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="submit"
                          disabled={!responseText.trim() || isSubmittingResponse}
                          className="btn-primary"
                        >
                          {t("sendResponse")}
                        </button>
                        <button
                          type="button"
                          onClick={closeResponseForm}
                          disabled={isSubmittingResponse}
                          className="btn-secondary"
                        >
                          {t("cancel")}
                        </button>
                      </div>
                    </form>
                  ) : (
                    <button
                      type="button"
                      onClick={() => openResponseForm(review.id)}
                      className="btn-secondary"
                    >
                      <MessageSquare className="w-4 h-4" strokeWidth={1.7} />
                      {t("reply")}
                    </button>
                  )}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      {hasMore && !error && (
        <button
          type="button"
          onClick={() => loadReviews(page + 1, "append")}
          disabled={isLoading}
          className="btn-secondary mt-4"
        >
          {t("loadMore")}
        </button>
      )}
    </section>
  );
}
