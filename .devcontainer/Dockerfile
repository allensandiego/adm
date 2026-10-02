FROM mcr.microsoft.com/devcontainers/base:resolute

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		ca-certificates \
		curl \
		openjdk-21-jdk \
		postgresql-18 \
		postgresql-client-18 \
	&& rm -rf /var/lib/apt/lists/*

# Development-only credential; matches the datasource default in application.properties.
RUN service postgresql start \
	&& su postgres -c "psql -c \"ALTER ROLE postgres WITH PASSWORD 'postgres';\"" \
	&& service postgresql stop

USER vscode

RUN curl -fsSL https://opencode.ai/install | bash

ENV PATH="/home/vscode/.opencode/bin:${PATH}"
