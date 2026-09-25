FROM node:20-alpine AS construction
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Le bundle statique est servi par nginx, qui relaie /api vers le backend.
FROM nginx:1.27-alpine
COPY --from=construction /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
