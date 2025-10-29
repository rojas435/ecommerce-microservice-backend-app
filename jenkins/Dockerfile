# Dockerfile personalizado para Jenkins con herramientas preinstaladas
FROM jenkins/jenkins:lts-jdk17

# Cambiar a usuario root para instalar dependencias
USER root

# Actualizar repositorios e instalar herramientas necesarias
RUN apt-get update && apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    git \
    maven \
    unzip \
    vim \
    && rm -rf /var/lib/apt/lists/*

# Instalar Docker CLI (para ejecutar docker dentro de Jenkins)
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
RUN echo \
    "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian \
    $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
RUN apt-get update && apt-get install -y docker-ce-cli && rm -rf /var/lib/apt/lists/*

# Instalar kubectl (para deployar a Kubernetes)
RUN curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
RUN install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Instalar Helm (para Kubernetes package management)
RUN curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Instalar GitHub CLI (para Release Notes automáticas)
RUN curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
RUN chmod go+r /usr/share/keyrings/githubcli-archive-keyring.gpg
RUN echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | tee /etc/apt/sources.list.d/github-cli.list > /dev/null
RUN apt-get update && apt-get install -y gh && rm -rf /var/lib/apt/lists/*

# Copiar lista de plugins de Jenkins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt

# Instalar plugins de Jenkins
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Copiar configuraciones iniciales de Jenkins (si existen)
COPY jenkins_config/ /var/jenkins_home/

# Volver a usuario jenkins
USER jenkins

# Exponer puertos
EXPOSE 8080 50000

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -f http://localhost:8080/login || exit 1
