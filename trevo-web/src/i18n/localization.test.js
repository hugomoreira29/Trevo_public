import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const files = [
  "src/pages/Admin.jsx",
  "src/pages/Cart.jsx",
  "src/pages/Checkout.jsx",
  "src/pages/Dashboard.jsx",
  "src/pages/ForgotPassword.jsx",
  "src/pages/Home.jsx",
  "src/pages/Login.jsx",
  "src/pages/OrderHistory.jsx",
  "src/pages/ProductDetail.jsx",
  "src/pages/ProductSearch.jsx",
  "src/pages/Profile.jsx",
  "src/pages/ProSubscription.jsx",
  "src/pages/PublicShopProfile.jsx",
  "src/pages/ReceiptView.jsx",
  "src/pages/Register.jsx",
  "src/pages/ResetPassword.jsx",
  "src/pages/ShopDashboard.jsx",
  "src/pages/StripePaymentForm.jsx",
  "src/pages/Unauthorized.jsx",
  "src/components/ReviewForm.jsx",
  "src/components/ReviewsSection.jsx",
];

const forbiddenVisibleEnglish = [
  />\s*Login\s*</,
  />\s*Register\s*</,
  />\s*Logout\s*</,
  />\s*Dashboard\s*</,
  />\s*Checkout\s*</,
  />\s*Cart\s*</,
  />\s*Search\s*</,
  />\s*Profile\s*</,
  />\s*Reviews\s*</,
  />\s*Review\s*</,
  /"Order #/,
  />\s*Order #/,
  /"Tracking:/,
  />\s*Tracking:/,
  />\s*Virtual Try-On\s*</,
  />\s*See It On Yourself\s*</,
  />\s*Generate Try-On\s*</,
  /"Slim"/,
  /"Athletic"/,
  /"Plus Size"/,
  />\s*Shop Owner\s*</,
  />\s*Buyer\s*</,
  /placeholder="Password"/,
  /aria-label="Close"/,
  /aria-label="Search"/,
  /aria-label="Cart"/,
  /defaultValue\s*:/,
  /["']Não foi possível/,
  /["']A confirmar pagamento\.\.\./,
  /["']Confirmar pagamento Stripe/,
];

describe("Portuguese localization", () => {
  it("does not keep known English UI strings hardcoded in migrated pages", () => {
    const offenders = [];

    files.forEach((file) => {
      const source = readFileSync(resolve(process.cwd(), file), "utf8");

      forbiddenVisibleEnglish.forEach((pattern) => {
        if (pattern.test(source)) {
          offenders.push(`${file}: ${pattern}`);
        }
      });
    });

    expect(offenders).toEqual([]);
  });
});
