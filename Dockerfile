FROM node:22.22.0-bookworm-slim AS client-build
WORKDIR /build/client
COPY client/package*.json ./
RUN npm ci --no-audit --no-fund
COPY client/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS server-build
WORKDIR /build
COPY server/pom.xml server/pom.xml
RUN mvn -B -f server/pom.xml dependency:go-offline
COPY server/src server/src
COPY world world
RUN mvn -B -f server/pom.xml verify

FROM eclipse-temurin:17-jre-jammy
RUN groupadd -g 10001 aincrad && useradd -u 10001 -g aincrad -m aincrad
WORKDIR /app
COPY --from=server-build /build/server/target/aincrad-server-0.2.0.jar /app/server.jar
COPY --from=client-build /build/client/dist/aincrad/browser /app/client/dist/aincrad/browser
COPY world/world.json /app/defaults/world.json
COPY scripts/container-entrypoint.sh /app/entrypoint.sh
RUN mkdir -p /app/data /app/world && chown -R aincrad:aincrad /app/data /app/world && chmod +x /app/entrypoint.sh
USER aincrad
ENV BIND_ADDRESS=0.0.0.0 PORT=8080 DATA_DIR=/app/data WORLD_FILE=/app/world/world.json
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
