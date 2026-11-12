# Developer / CI toolbox: Node 22 + Playwright browsers (from the base image), JDK 21, make, git.
FROM mcr.microsoft.com/playwright:v1.65.0-noble

RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-21-jdk-headless make git curl unzip mysql-client \
    && rm -rf /var/lib/apt/lists/*

ENV IN_CONTAINER=1 \
    MAVEN_OPTS="-Xmx1g"

WORKDIR /workspace
CMD ["bash"]
