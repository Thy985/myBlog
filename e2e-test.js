const { chromium } = require('playwright');

const API_URL = 'http://localhost:8080/api';
const ADMIN_USER = 'admin';
const ADMIN_PASS = '147258369Thy@';

let authToken = '';

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function apiRequest(method, endpoint, data = null) {
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json',
      'satoken': authToken
    }
  };
  if (data) {
    options.body = JSON.stringify(data);
  }
  const response = await fetch(`${API_URL}${endpoint}`, options);
  return response.json();
}

async function login() {
  console.log('\n=== 1. 登录测试 ===');
  const result = await apiRequest('POST', '/auth/login', {
    username: ADMIN_USER,
    password: ADMIN_PASS
  });
  if (result.code === 200 && result.data && result.data.token) {
    authToken = result.data.token;
    console.log('✅ 登录成功');
    return true;
  }
  console.log('❌ 登录失败:', result.message);
  return false;
}

async function testBlogPublish(title, message) {
  console.log(`\n=== 发布博客: ${title} ===`);
  const result = await apiRequest('POST', '/agent/chat', {
    sessionId: 'e2e-test-session',
    message: message
  });

  if (result.code === 200 && result.data) {
    const response = result.data.response || '';
    console.log('   响应:', response.substring(0, 200));
    return { success: response.includes('成功'), response };
  }
  console.log('❌ 发布失败:', result.message);
  return { success: false };
}

async function run() {
  console.log('╔══════════════════════════════════════════════════════════════╗');
  console.log('║        智能体博客发布功能 E2E 测试                       ║');
  console.log('╚══════════════════════════════════════════════════════════════╝');

  const results = [];

  try {
    if (!await login()) throw new Error('登录失败');

    // Test cases
    const testCases = [
      {
        title: 'Claude源码泄露',
        message: '发一个关于Claude源码泄露的博文'
      },
      {
        title: '量子计算',
        message: '写一篇关于量子计算的科普文章'
      },
      {
        title: '人工智能伦理',
        message: '请帮我发布一篇博客，标题是"人工智能伦理思考"，内容关于AI发展中的道德困境'
      },
      {
        title: '深度学习入门',
        message: '发表一篇文章《深度学习入门指南》，介绍机器学习基础知识'
      }
    ];

    for (const tc of testCases) {
      const result = await testBlogPublish(tc.title, tc.message);
      results.push({ title: tc.title, ...result });
      await sleep(2000);
    }

  } catch (error) {
    console.error('\n❌ 测试异常:', error.message);
  }

  // Summary
  console.log('\n╔══════════════════════════════════════════════════════════════╗');
  console.log('║                        测试结果汇总                         ║');
  console.log('╚══════════════════════════════════════════════════════════════╝');
  results.forEach(r => {
    console.log(`   ${r.success ? '✅' : '❌'} ${r.title}`);
  });
  const passed = results.filter(r => r.success).length;
  console.log(`\n   通过: ${passed}/${results.length}`);
  if (passed === results.length) {
    console.log('   🎉 所有博客发布测试通过！');
  }
}

run().catch(console.error);
