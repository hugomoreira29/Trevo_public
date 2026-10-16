/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Brand — clover green
        clover: {
          50:  "#F1FAF3",
          100: "#DBF3E1",
          200: "#B8E7C4",
          300: "#85D49D",
          400: "#4CB673",
          500: "#2A9D54", // primary brand
          600: "#1F7E41",
          700: "#196533",
          800: "#114C26",
          900: "#0A3019",
        },
        // Neutrals — ink scale
        ink: {
          50:  "#F7F6F9",
          100: "#ECECEF",
          200: "#D8D5DE",
          300: "#B5B1BF",
          400: "#8E889A",
          500: "#6B6478",
          600: "#4A4458",
          700: "#2D2738",
          800: "#1A1623",
          900: "#08060D", // primary text
        },
      },
      fontFamily: {
        sans: [
          "Geist",
          "ui-sans-serif",
          "system-ui",
          "-apple-system",
          "BlinkMacSystemFont",
          "Segoe UI",
          "sans-serif",
        ],
        mono: ["Geist Mono", "ui-monospace", "SFMono-Regular", "Menlo", "monospace"],
      },
      borderRadius: {
        sm: "4px",
        DEFAULT: "4px",
        md: "8px",
        lg: "12px",
        xl: "16px",
        "2xl": "24px",
      },
      boxShadow: {
        xs: "0 1px 2px rgba(8, 6, 13, 0.06)",
        sm: "0 2px 4px rgba(8, 6, 13, 0.06), 0 1px 2px rgba(8, 6, 13, 0.04)",
        md: "0 8px 16px -4px rgba(8, 6, 13, 0.08), 0 4px 8px -2px rgba(8, 6, 13, 0.04)",
        lg: "0 16px 32px -8px rgba(8, 6, 13, 0.12), 0 8px 16px -4px rgba(8, 6, 13, 0.06)",
        xl: "0 24px 48px -12px rgba(8, 6, 13, 0.18)",
        clover: "0 12px 32px -8px rgba(42, 157, 84, 0.35)",
      },
      letterSpacing: {
        tightest: "-0.03em",
        tighter: "-0.02em",
      },
    },
  },
  plugins: [],
}
