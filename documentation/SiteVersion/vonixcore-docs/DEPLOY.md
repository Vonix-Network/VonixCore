# Deployment Guide

This project is a static React application built with Vite.

## 1. Build for Production

Run the build command to generate the static files:

```bash
npm run build
```

This will create a `dist` folder containing:
- `index.html`
- `assets/` (CSS, JS, Images)
- `docs/` (Markdown content)

## 2. Preview Locally

To test the production build locally before deploying:

```bash
npm run preview
```

## 3. Deployment Options

### Option A: Static Web Host (Vercel, Netlify, Cloudflare) - Recommended
1. Push this folder to a GitHub repository.
2. Connect your repository to Vercel/Netlify.
3. **Build Command:** `npm run build`
4. **Output Directory:** `dist`

### Option B: Traditional Web Server (Nginx/Apache)
1. Run `npm run build` locally.
2. Upload the **contents** of the `dist` folder to your server's public web directory (e.g., `/var/www/html`).
3. **Important:** Configure your server to redirect all 404s to `index.html` (for Client-Side Routing).

**Nginx Example:**
```nginx
location / {
  try_files $uri $uri/ /index.html;
}
```

### Option C: GitHub Pages
1. run `npm run build`
2. Push the contents of `dist` to a `gh-pages` branch.
