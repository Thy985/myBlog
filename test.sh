#!/bin/bash
curl -s http://localhost:8080/api/verification/send-code -H "Content-Type: application/json" -d "{\"email\":\"3881736197@qq.com\",\"type\":\"register\"}"
