import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import auth from "./locales/pt-PT/auth.json";
import browse from "./locales/pt-PT/browse.json";
import checkout from "./locales/pt-PT/checkout.json";
import common from "./locales/pt-PT/common.json";
import orders from "./locales/pt-PT/orders.json";
import profile from "./locales/pt-PT/profile.json";
import reviews from "./locales/pt-PT/reviews.json";
import shop from "./locales/pt-PT/shop.json";

i18n.use(initReactI18next).init({
  lng: "pt-PT",
  fallbackLng: "pt-PT",
  ns: ["common", "auth", "browse", "checkout", "orders", "shop", "profile", "reviews"],
  defaultNS: "common",
  resources: {
    "pt-PT": {
      auth,
      browse,
      checkout,
      common,
      orders,
      profile,
      reviews,
      shop,
    },
  },
  interpolation: {
    escapeValue: false,
  },
});

export default i18n;
