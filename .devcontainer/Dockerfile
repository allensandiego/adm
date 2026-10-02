FROM mcr.microsoft.com/devcontainers/base:resolute

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		ca-certificates \
		curl \
		openjdk-21-jdk \
		postgresql-18 \
		postgresql-client-18 \
	&& rm -rf /var/lib/apt/lists/*

USER vscode

RUN curl -fsSL https://opencode.ai/v2/install | bash

ENV PATH="/home/vscode/.opencode/bin:${PATH}"
