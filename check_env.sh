#!/bin/bash
echo "=== 检查Java进程的环境变量 ==="
cat /proc/$(ps aux | grep "backend-0.0.1-SNAPSHOT.jar" | grep -v grep | awk '{print $2}')/environ | tr '\0' '\n' | grep -i mail
