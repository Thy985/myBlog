#!/bin/bash
echo "========== 禁用有问题的仓库 =========="
mv /etc/yum.repos.d/gitlab-ce.repo /etc/yum.repos.d/gitlab-ce.repo.bak 2>/dev/null || true

echo "========== 清理缓存 =========="
yum clean all

echo "========== 安装 JDK 17 =========="
yum install -y java-17-openjdk java-17-openjdk-devel

echo "========== 安装 Maven =========="
yum install -y maven

echo "========== 安装 Node.js 20.x =========="
curl -fsSL https://rpm.nodesource.com/setup_20.x | bash -
yum install -y nodejs

echo "========== 验证安装 =========="
java -version 2>&1 || echo "Java not found"
mvn -v || echo "Maven not found"
node -v || echo "Node not found"
npm -v || echo "NPM not found"
