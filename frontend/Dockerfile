# ---- Build stage ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json .
RUN npm install
COPY public ./public
COPY src ./src
RUN npm run build

# ---- Serve stage ----
FROM node:20-alpine
RUN npm install -g serve
COPY --from=build /app/build /app/build
EXPOSE 3000
CMD ["serve", "-s", "/app/build", "-l", "3000"]
