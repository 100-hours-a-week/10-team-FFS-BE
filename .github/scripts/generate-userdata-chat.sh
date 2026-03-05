#!/bin/bash
set -e

# 인자 받기
ENVIRONMENT=$1
IMAGE_URI=$2
AWS_REGION=$3

# 현재 스크립트 디렉토리
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATES_DIR="${SCRIPT_DIR}/../templates"

# 템플릿 파일 base64 인코딩
COMPOSE_B64=$(base64 -w 0 "${TEMPLATES_DIR}/docker-compose.chat.yml")
ENV_B64=$(base64 -w 0 "${TEMPLATES_DIR}/.env.chat.template")

# User Data 스크립트 생성
cat > user-data-chat.sh <<'EOF'
#!/bin/bash
set -e

# 로깅 설정
exec > >(tee /var/log/user-data.log)
exec 2>&1

echo "=== Chat Server User Data Script Started ==="

# 환경 변수
export AWS_REGION="{{AWS_REGION}}"
export IMAGE_URI="{{IMAGE_URI}}"
export ENVIRONMENT="{{ENVIRONMENT}}"

COMPOSE_B64="{{COMPOSE_B64}}"
ENV_B64="{{ENV_B64}}"

# 필수 도구 설치
echo "Installing required tools..."

apt update -y
apt install -y ca-certificates curl gnupg jq gettext-base awscli

install -m 0755 -d /etc/apt/keyrings

curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | gpg --dearmor -o /etc/apt/keyrings/docker.gpg

chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt update -y
apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

systemctl enable docker
systemctl start docker

usermod -a -G docker ubuntu

echo "Docker version:"
docker --version
docker compose version

# Install node_exporter
echo "Installing node_exporter..."
NODE_EXPORTER_VERSION="1.7.0"
curl -fsSL "https://github.com/prometheus/node_exporter/releases/download/v${NODE_EXPORTER_VERSION}/node_exporter-${NODE_EXPORTER_VERSION}.linux-amd64.tar.gz" -o /tmp/node_exporter.tar.gz
tar xzf /tmp/node_exporter.tar.gz -C /tmp/
mv /tmp/node_exporter-${NODE_EXPORTER_VERSION}.linux-amd64/node_exporter /usr/local/bin/
rm -rf /tmp/node_exporter*
useradd -rs /bin/false node_exporter || true

cat > /etc/systemd/system/node_exporter.service <<'SYSTEMD_EOF'
[Unit]
Description=Node Exporter
After=network.target

[Service]
User=node_exporter
ExecStart=/usr/local/bin/node_exporter
Restart=always

[Install]
WantedBy=multi-user.target
SYSTEMD_EOF

systemctl daemon-reload
systemctl enable node_exporter
systemctl start node_exporter

# Run cAdvisor
echo "Starting cAdvisor..."
docker run -d \
  --name cadvisor \
  --restart unless-stopped \
  --volume /:/rootfs:ro \
  --volume /var/run:/var/run:ro \
  --volume /sys:/sys:ro \
  --volume /var/lib/docker/:/var/lib/docker:ro \
  --publish 8888:8080 \
  gcr.io/cadvisor/cadvisor:v0.47.2

# 작업 디렉토리
APP_DIR="/home/ubuntu/app"
mkdir -p ${APP_DIR}
cd ${APP_DIR}

# ECR 로그인
echo "Logging into ECR..."
aws ecr get-login-password --region ${AWS_REGION} | \
  docker login --username AWS --password-stdin $(echo ${IMAGE_URI} | cut -d'/' -f1)

# SSM에서 환경 변수 가져오기
echo "Fetching configuration from Parameter Store..."

get_param() {
  aws ssm get-parameter --name "$1" --query "Parameter.Value" --output text --region ${AWS_REGION} 2>/dev/null || echo ""
}

get_secure_param() {
  aws ssm get-parameter --name "$1" --with-decryption --query "Parameter.Value" --output text --region ${AWS_REGION} 2>/dev/null || echo ""
}

# 환경 변수 설정
export SPRING_PROFILE=$(get_param "/klosetlab/${ENVIRONMENT}/spring/profile")
export SERVER_PORT=$(get_param "/klosetlab/${ENVIRONMENT}/spring/server/chat-port")

export JWT_SECRET=$(get_secure_param "/klosetlab/${ENVIRONMENT}/spring/jwt/secret")

export AWS_S3_BUCKET=$(get_param "/klosetlab/${ENVIRONMENT}/spring/aws/s3/bucket")

export MONGODB_HOST=$(get_param "/klosetlab/${ENVIRONMENT}/spring/mongodb/host")
export MONGODB_PORT=$(get_param "/klosetlab/${ENVIRONMENT}/spring/mongodb/port")
export MONGODB_DATABASE=$(get_param "/klosetlab/${ENVIRONMENT}/spring/mongodb/database")
export MONGODB_USERNAME=$(get_secure_param "/klosetlab/${ENVIRONMENT}/spring/mongodb/username")
export MONGODB_PASSWORD=$(get_secure_param "/klosetlab/${ENVIRONMENT}/spring/mongodb/password")

export REDIS_HOST=$(get_param "/klosetlab/${ENVIRONMENT}/spring/redis-main/host")
export REDIS_PORT=$(get_param "/klosetlab/${ENVIRONMENT}/spring/redis-main/port")
export REDIS_PASSWORD=$(get_secure_param "/klosetlab/${ENVIRONMENT}/spring/redis-main/password")

export MYSQL_HOST=$(get_param "/klosetlab/${ENVIRONMENT}/spring/mysql/host")
export MYSQL_PORT=$(get_param "/klosetlab/${ENVIRONMENT}/spring/mysql/port")
export MYSQL_DATABASE=$(get_param "/klosetlab/${ENVIRONMENT}/spring/mysql/database")
export MYSQL_USERNAME=$(get_secure_param "/klosetlab/${ENVIRONMENT}/spring/mysql/username")
export MYSQL_PASSWORD=$(get_secure_param "/klosetlab/${ENVIRONMENT}/spring/mysql/password")

echo "Restoring docker-compose.chat.yml..."
echo "${COMPOSE_B64}" | base64 -d > docker-compose.yml

echo "Restoring .env.chat.template..."
echo "${ENV_B64}" | base64 -d > .env.template

echo "Generating .env..."
envsubst < .env.template > .env

chmod 600 .env
chmod 644 docker-compose.yml

echo "✅ Configuration files created"
ls -lh docker-compose.yml .env

# 배포
echo "Deploying chat application..."

echo "Pulling Docker image..."
docker compose pull

echo "Starting containers..."
docker compose up -d --remove-orphans

echo "Waiting for container..."

for i in {1..30}; do
  if docker compose ps | grep "Up"; then
    echo "Container running"
    exit 0
  fi
  sleep 10
done

echo "Deployment failed"
docker compose logs --tail=50
exit 1
EOF

sed -i "s|{{COMPOSE_B64}}|${COMPOSE_B64}|g" user-data-chat.sh
sed -i "s|{{ENV_B64}}|${ENV_B64}|g" user-data-chat.sh
sed -i "s|{{IMAGE_URI}}|${IMAGE_URI}|g" user-data-chat.sh
sed -i "s|{{AWS_REGION}}|${AWS_REGION}|g" user-data-chat.sh
sed -i "s|{{ENVIRONMENT}}|${ENVIRONMENT}|g" user-data-chat.sh

# Base64 인코딩
USER_DATA_BASE64=$(base64 -w 0 user-data-chat.sh)
echo "user_data_base64=${USER_DATA_BASE64}" >> $GITHUB_OUTPUT

echo "✅ Chat User Data script created and encoded"