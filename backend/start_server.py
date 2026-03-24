#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Myblog 后端服务器启动脚本
功能：
1. 从 .env 文件加载环境变量
2. 创建必要的目录（日志目录、上传目录）
3. 启动 Spring Boot 应用
"""

import os
import sys
import subprocess
import signal
import time
from pathlib import Path
from datetime import datetime


def load_env_file(env_file_path):
    """从 .env 文件加载环境变量"""
    env_vars = {}
    
    if not os.path.exists(env_file_path):
        print(f"[警告] .env 文件不存在: {env_file_path}")
        return env_vars
    
    print(f"[信息] 正在加载环境变量: {env_file_path}")
    
    with open(env_file_path, 'r', encoding='utf-8') as f:
        for line_num, line in enumerate(f, 1):
            line = line.strip()
            
            # 跳过空行和注释
            if not line or line.startswith('#'):
                continue
            
            # 解析 KEY=VALUE
            if '=' in line:
                key, value = line.split('=', 1)
                key = key.strip()
                value = value.strip()
                
                # 移除引号
                if value.startswith('"') and value.endswith('"'):
                    value = value[1:-1]
                elif value.startswith("'") and value.endswith("'"):
                    value = value[1:-1]
                
                env_vars[key] = value
                print(f"[信息] 加载环境变量: {key}=***")
    
    return env_vars


def create_directories(env_vars):
    """创建必要的目录"""
    directories = []
    
    # 日志目录
    log_file = env_vars.get('LOG_FILE_PATH', '/www/wwwroot/myblog/logs/backend.log')
    log_dir = os.path.dirname(log_file)
    if log_dir:
        directories.append(log_dir)
    
    # 上传目录
    upload_path = env_vars.get('FILE_UPLOAD_PATH', '/www/wwwroot/myblog/uploads')
    if upload_path:
        directories.append(upload_path)
    
    # 创建目录
    for dir_path in directories:
        try:
            Path(dir_path).mkdir(parents=True, exist_ok=True)
            print(f"[信息] 创建目录: {dir_path}")
        except Exception as e:
            print(f"[警告] 无法创建目录 {dir_path}: {e}")


def setup_logging(env_vars):
    """设置日志相关配置"""
    # 确保日志目录存在
    log_file = env_vars.get('LOG_FILE_PATH', '/www/wwwroot/myblog/logs/backend.log')
    if log_file:
        log_dir = os.path.dirname(log_file)
        if log_dir:
            try:
                Path(log_dir).mkdir(parents=True, exist_ok=True)
                print(f"[信息] 日志目录已准备: {log_dir}")
            except Exception as e:
                print(f"[错误] 无法创建日志目录: {e}")


def start_server(jar_path, env_vars, java_opts=None):
    """启动 Spring Boot 服务器"""
    
    if not os.path.exists(jar_path):
        print(f"[错误] JAR 文件不存在: {jar_path}")
        sys.exit(1)
    
    # 设置环境变量
    env = os.environ.copy()
    env.update(env_vars)
    
    # 默认 JVM 参数
    if java_opts is None:
        java_opts = [
            '-Xms512m',
            '-Xmx1024m',
            '-XX:+UseG1GC',
            '-XX:MaxGCPauseMillis=200',
            '-Djava.awt.headless=true',
            '-Dfile.encoding=UTF-8',
            '-Dsun.jnu.encoding=UTF-8',
        ]
    
    # 构建启动命令
    cmd = ['java'] + java_opts + ['-jar', jar_path]
    
    print(f"[信息] 启动命令: {' '.join(cmd)}")
    print(f"[信息] 启动时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)
    
    # 启动进程
    process = None
    try:
        process = subprocess.Popen(
            cmd,
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            encoding='utf-8',
            errors='replace',
            bufsize=1
        )
        
        # 实时输出日志
        print("[信息] 服务器启动中，按 Ctrl+C 停止...")
        print("-" * 60)
        
        for line in iter(process.stdout.readline, ''):
            if line:
                print(line, end='')
        
        process.wait()
        
    except KeyboardInterrupt:
        print("\n[信息] 收到停止信号，正在关闭服务器...")
        if process:
            process.terminate()
            try:
                process.wait(timeout=10)
                print("[信息] 服务器已正常关闭")
            except subprocess.TimeoutExpired:
                process.kill()
                print("[警告] 服务器被强制终止")
    except Exception as e:
        print(f"[错误] 启动失败: {e}")
        if process:
            process.terminate()
        sys.exit(1)


def main():
    """主函数"""
    # 获取脚本所在目录
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # 配置文件路径
    env_file = os.path.join(script_dir, '.env')
    jar_file = os.path.join(script_dir, 'target', 'backend-0.0.1-SNAPSHOT.jar')
    
    # 检查命令行参数
    if len(sys.argv) > 1:
        jar_file = sys.argv[1]
    
    print("=" * 60)
    print("  Myblog 后端服务器启动脚本")
    print("=" * 60)
    
    # 加载环境变量
    env_vars = load_env_file(env_file)
    
    # 创建必要目录
    create_directories(env_vars)
    
    # 设置日志
    setup_logging(env_vars)
    
    # 启动服务器
    start_server(jar_file, env_vars)


if __name__ == '__main__':
    main()
