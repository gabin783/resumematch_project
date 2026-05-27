/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    // src 폴더 안의 components 폴더에 있는 모든 js, jsx 파일에 Tailwind를 적용하겠다는 뜻입니다!
    "./src/components/**/*.{js,jsx,ts,tsx}",
    
    // 만약 나중에 components 말고 src 바로 아래에 있는 App.js 등에서도 
    // Tailwind를 쓰고 싶다면 아래 줄도 같이 남겨두는 것이 제일 안전합니다.
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}