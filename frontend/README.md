# Finly Frontend

React single-page application built with Vite, React Router, and Tailwind CSS. See the [project root README](../README.md) for complete database/backend configuration, local startup, API overview, and contribution instructions.

## Development

```sh
npm ci
npm run dev
```

The Vite development server proxies `/api` requests to `http://localhost:8080` (configured in `vite.config.js`). `VITE_API_BASE_URL` is optional and defaults to the same-origin proxy setup.

## Build and lint

```sh
npm run build
npm run lint
```
