# Replicate IDM-VTON Integration

Integrate Replicate's IDM-VTON model into the existing virtual try-on backend so that try-on image generation actually works end-to-end, and wire the frontend to call the real backend endpoint.

## Approach

Modify `HttpAiImageGenerationClient` to call Replicate's prediction API directly instead of delegating to an external middleware service. The existing `AiImageGenerationClient` interface, `TryOnService`, `TryOnController`, and database schema remain unchanged.

## Backend

### `HttpAiImageGenerationClient` rewrite

Replace the current generic REST logic with Replicate-specific calls:

1. **Create prediction** — `POST https://api.replicate.com/v1/predictions`
   - Model: `cuuupid/idm-vton` (uses `POST /v1/models/cuuupid/idm-vton/predictions` by default — always uses latest version; optionally pin a version hash via `REPLICATE_MODEL_VERSION`)
   - Input fields:
     - `garm_img` (string, required) — first product image URL (from `product.getImages()`)
     - `human_img` (string, required) — model reference photo URL (mapped from body type identifier)
     - `category` (string) — garment category: `"upper_body"`, `"lower_body"`, or `"dresses"`. Default `"upper_body"` for MVP.
     - `garment_des` (string) — garment description, use `product.getName()`
   - Auth header: `Authorization: Bearer ${REPLICATE_API_TOKEN}`

2. **Poll for completion** — `GET https://api.replicate.com/v1/predictions/{id}`
   - Poll every 2 seconds
   - Timeout after 60 seconds
   - Terminal states: `succeeded` (return output URL), `failed`/`canceled` (throw 502)

3. **Return** the output image URL string (first element of the `output` array).

### Personalized try-on

For `generatePersonalizedTryOn`, the user uploads 3 photos. Replicate needs URLs, not raw bytes. Upload the first user photo to Cloudinary as a temporary image (using the existing `CloudinaryService`), then pass that URL as `human_img` along with the product image as `garm_img`.

### Model photo mapping

A static map from body type identifier to a public reference photo URL:

| Identifier | Description |
|------------|-------------|
| `slim`     | Slim body type reference photo |
| `athletic` | Athletic body type reference photo |
| `regular`  | Regular body type reference photo |
| `plusSize`  | Plus-size body type reference photo |

These URLs are configured via `app.ai.try-on.model-photos.*` properties so they can be changed without code changes. Sensible defaults point to placeholder images initially — the team provides real model photos separately.

### New configuration properties

```properties
# Required
replicate.api-token=${REPLICATE_API_TOKEN:}

# Optional (defaults provided)
replicate.model-version=${REPLICATE_MODEL_VERSION:}
replicate.poll-interval-ms=${REPLICATE_POLL_INTERVAL_MS:2000}
replicate.timeout-ms=${REPLICATE_TIMEOUT_MS:60000}

# Model reference photo URLs (body types)
app.ai.try-on.model-photos.slim=${AI_TRYON_MODEL_PHOTO_SLIM:}
app.ai.try-on.model-photos.athletic=${AI_TRYON_MODEL_PHOTO_ATHLETIC:}
app.ai.try-on.model-photos.regular=${AI_TRYON_MODEL_PHOTO_REGULAR:}
app.ai.try-on.model-photos.plusSize=${AI_TRYON_MODEL_PHOTO_PLUS_SIZE:}
```

Note: `plusSize` uses camelCase to match the frontend `BODY_TYPES` value exactly (Spring relaxed binding applies to property names but not Map keys).

### Error handling

- Missing `REPLICATE_API_TOKEN` — throw 502 "AI image generation API is not configured" (same as current behavior for missing base URL)
- Replicate returns non-2xx — throw 502 "AI image generation API request failed"
- Poll timeout — throw 502 "AI image generation timed out"
- Prediction fails — throw 502 with Replicate's error message
- No product images — throw 400 "Product has no images"

## Frontend

### `tryOnService.js`

Add functions:

```js
export async function generateTryOn(productId, modelIdentifier) {
  const { data } = await apiClient.post("/buyer/try-on", { productId, modelIdentifier });
  return data;
}
```

### `ProductDetail.jsx`

Replace the stub `handleGenerateTryOn`:

- Call `generateTryOn(productId, selectedBodyType)` 
- Show loading spinner during the request (~10-30s)
- On success: display the returned `imageUrl`
- On error: show error message in the modal
- Update `tryOnUsed` state on success (for free tier tracking)

### "See it on yourself" flow

Replace the stub `handleSeeItSubmit`:

- Call `generatePersonalizedTryOn(productId, photos)` via multipart POST
- Same loading/error handling pattern

## What stays unchanged

- `AiImageGenerationClient` interface
- `TryOnService` (usage tracking, caching, Pro gating)
- `TryOnController` (endpoints, validation)
- Database schema (`tryon_images`, `tryon_usage` tables)
- Body type radio buttons in the UI

## Testing

- Unit tests for the new Replicate client (mock RestClient responses for create/poll)
- Existing `TryOnServiceTest` continues to work (it mocks `AiImageGenerationClient`)
- Manual E2E test with a real Replicate API token
