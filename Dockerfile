# Builds the server and both web apps once, then assembles three small images from the results:
#   docker build --target server -t portfolio-server .
#   docker build --target client -t portfolio-client .
#   docker build --target admin  -t portfolio-admin  .
# The build stage always runs on the build machine's own architecture (the jar and the wasm bundles are
# architecture-independent), so multi-arch builds never compile under emulation.

FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip libatomic1 \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /src
COPY . .
# The Kotlin Toolchain provisions its own JRE and Node; keep its shared cache between builds.
RUN --mount=type=cache,target=/root/.cache/JetBrains/Kotlin \
    ./kotlin package -m server -f executable-jar \
    && ./kotlin build -m clientWebApp -m adminWebApp -p wasmJs -v release \
    && mkdir -p /out/client /out/admin \
    && cp build/tasks/_server_executableJarJvm/server-jvm-executable.jar /out/server.jar \
    && cp -r build/tasks/_clientWebApp_buildWasmJsAppWasmJsRelease/. /out/client/ \
    && cp -r build/tasks/_adminWebApp_buildWasmJsAppWasmJsRelease/. /out/admin/

FROM eclipse-temurin:25-jre AS server
RUN useradd --system --uid 10001 --home-dir /data app \
    && mkdir -p /data \
    && chown app:app /data
COPY --from=build /out/server.jar /app/server.jar
USER app
WORKDIR /data
ENV PORTFOLIO_DB=/data/portfolio.db \
    PORTFOLIO_IMAGES=/data/images \
    PORTFOLIO_PORT=8090 \
    JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"
VOLUME /data
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/server.jar"]

FROM nginx:1-alpine AS client
COPY deploy/nginx/client.conf /etc/nginx/conf.d/default.conf
COPY --from=build /out/client /usr/share/nginx/html
EXPOSE 80

FROM nginx:1-alpine AS admin
COPY deploy/nginx/admin.conf /etc/nginx/conf.d/default.conf
COPY --from=build /out/admin /usr/share/nginx/html
EXPOSE 80
