/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        paper: '#F6F5F0',
        surface: '#FFFFFF',
        border: '#E4E1D6',
        ink: {
          DEFAULT: '#16221F',
          soft: '#5C6B65',
          faint: '#8C9A93',
        },
        brand: {
          DEFAULT: '#163832',
          light: '#1F4D45',
          dark: '#0E2622',
        },
        accent: {
          DEFAULT: '#B8863B',
          light: '#D6A968',
          soft: '#F3E6D0',
        },
        success: {
          DEFAULT: '#3D7A5C',
          soft: '#DFEEE5',
        },
        warning: {
          DEFAULT: '#C08A2E',
          soft: '#F6E9D2',
        },
        danger: {
          DEFAULT: '#B3413A',
          soft: '#F5DFDD',
        },
        info: {
          DEFAULT: '#2E5F63',
          soft: '#DEEBEC',
        },
      },
      fontFamily: {
        display: ['"Space Grotesk"', 'system-ui', 'sans-serif'],
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'monospace'],
      },
      boxShadow: {
        card: '0 1px 2px rgba(22, 34, 31, 0.04), 0 4px 16px rgba(22, 34, 31, 0.06)',
      },
      borderRadius: {
        xl: '0.875rem',
      },
    },
  },
  plugins: [],
}
