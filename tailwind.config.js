/** @type {import('tailwindcss').Config} */
import scrollbarHide from 'tailwind-scrollbar-hide';

export default {
    content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
    theme: {
        extend: {
            keyframes: {
                "pulse-dot": {
                    "0%, 80%, 100%": { transform: "scale(0)", opacity: 0.3 },
                    "40%": { transform: "scale(1)", opacity: 1 },
                },
            },
            animation: {
                "pulse-dot-0": "pulse-dot 1.4s infinite ease-in-out 0s",
                "pulse-dot-1": "pulse-dot 1.4s infinite ease-in-out 0.2s",
                "pulse-dot-2": "pulse-dot 1.4s infinite ease-in-out 0.4s",
            },
        },
    },
    plugins: [scrollbarHide],
};
