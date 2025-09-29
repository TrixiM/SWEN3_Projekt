# build Angular
FROM node:20-alpine as build
WORKDIR /app
COPY paperless-ui/package*.json ./
RUN npm install
COPY paperless-ui/ .
RUN npm run build -- --configuration production

# serve with nginx
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /app/dist/paperless-ui/browser /usr/share/nginx/html
