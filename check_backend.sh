#!/bin/bash
ps aux | grep java
echo "=== 环境变量 ==="
env | grep -i "cors\|spring"
echo "=== 检查后端进程 ==="
ps -ef | grep java
