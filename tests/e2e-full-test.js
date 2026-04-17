const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:5173';
const API_URL = 'http://localhost:8080/api';
const ADMIN_USER = 'admin';
const ADMIN_PASS = '147258369Thy@';
const TEST_ARTICLE_ID = 1;
let authToken = '';
let browser, page;

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function apiRequest(method, endpoint, data = null) {
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(authToken ? { Authorization: `Bearer ${authToken}`, satoken: authToken } : {}),
    }
  };
  if (data) {
    options.body = JSON.stringify(data);
  }
  const response = await fetch(`${API_URL}${endpoint}`, options);
  return response.json();
}

async function login() {
  console.log('\n🔐 登录测试');
  const result = await apiRequest('POST', '/auth/login', {
    username: ADMIN_USER,
    password: ADMIN_PASS
  });
  if (result.code === 200 && result.data && result.data.token) {
    authToken = result.data.token;
    console.log('   ✅ 登录成功');
    return true;
  }
  console.log('   ❌ 登录失败:', result.message);
  return false;
}

async function testFrontendAccess() {
  console.log('\n🌐 前端页面访问测试');
  try {
    await page.goto(BASE_URL, { waitUntil: 'networkidle', timeout: 30000 });
    const title = await page.title();
    console.log(`   ✅ 首页加载成功 - 标题: "${title}"`);
    return true;
  } catch (error) {
    console.log(`   ❌ 首页加载失败: ${error.message}`);
    return false;
  }
}

async function testFrontendElements() {
  console.log('\n🎨 前端元素渲染测试');
  const checks = [];

  try {
    // 检查导航栏
    const header = await page.$('header');
    checks.push({ name: '导航栏', pass: !!header });

    // 检查文章列表 - 使用 data-testid 等待异步组件加载
    await page.waitForSelector('[data-testid="article-card"]', { timeout: 20000 }).catch(() => null);
    await page.waitForTimeout(3000); // 额外等待确保所有卡片渲染
    const articles = await page.$$('[data-testid="article-card"]');
    checks.push({ name: '文章卡片', pass: articles.length > 0, count: articles.length });

    // 检查分类侧边栏
    const categorySection = await page.$('text=文章分类');
    checks.push({ name: '分类列表', pass: !!categorySection });

    // 检查标签侧边栏
    const tagSection = await page.$('text=热门标签');
    checks.push({ name: '标签列表', pass: !!tagSection });

    // 检查页脚
    const footer = await page.$('footer');
    checks.push({ name: '页脚', pass: !!footer });

  } catch (error) {
    console.log(`   ❌ 元素检查出错: ${error.message}`);
  }

  checks.forEach(c => {
    if (c.count !== undefined) {
      console.log(`   ${c.pass ? '✅' : '❌'} ${c.name} (${c.count} 个)`);
    } else {
      console.log(`   ${c.pass ? '✅' : '❌'} ${c.name}`);
    }
  });

  return checks.every(c => c.pass);
}

async function testArticleListAPI() {
  console.log('\n📝 文章列表 API 测试');
  try {
    const res = await apiRequest('GET', '/article/list?current=1&size=10');
    const pass = res.code === 200 && res.data && res.data.list;
    console.log(`   ${pass ? '✅' : '❌'} GET /article/list - ${pass ? '返回 ' + res.data.list.length + ' 篇文章' : res.message}`);
    if (pass) {
      const article = res.data.list[0];
      console.log(`   📄 最新文章: "${article.title}" (ID: ${article.id})`);
      console.log(`   🖼️  图片: ${article.titleImage || article.thumbnail || '无'}`);
      return article.id;
    }
    return null;
  } catch (error) {
    console.log(`   ❌ 文章列表 API 失败: ${error.message}`);
    return null;
  }
}

async function testArticleDetailAPI(articleId) {
  console.log('\n📄 文章详情 API 测试');
  if (!articleId) {
    console.log('   ⏭️  跳过（无文章ID）');
    return false;
  }
  try {
    const res = await apiRequest('GET', `/article/${articleId}`);
    const pass = res.code === 200 && res.data;
    console.log(`   ${pass ? '✅' : '❌'} GET /article/${articleId}`);
    if (pass) {
      console.log(`   📄 标题: "${res.data.title}"`);
      console.log(`   👤 作者: ${res.data.authorName || res.data.createBy || '未知'}`);
      console.log(`   🏷️  分类: ${res.data.categoryName || '无'}`);
      console.log(`   🖼️  封面图: ${res.data.titleImage || '无'}`);
    }
    return pass;
  } catch (error) {
    console.log(`   ❌ 文章详情 API 失败: ${error.message}`);
    return false;
  }
}

async function testCategoryAPI() {
  console.log('\n📂 分类 API 测试');
  try {
    const res = await apiRequest('GET', '/category/list');
    const pass = res.code === 200 && Array.isArray(res.data);
    console.log(`   ${pass ? '✅' : '❌'} GET /category/list - ${pass ? res.data.length + ' 个分类' : res.message}`);
    if (pass && res.data.length > 0) {
      console.log(`   📂 示例: "${res.data[0].name}" (${res.data[0].articleCount || 0} 篇文章)`);
    }
    return pass;
  } catch (error) {
    console.log(`   ❌ 分类 API 失败: ${error.message}`);
    return false;
  }
}

async function testTagAPI() {
  console.log('\n🏷️ 标签 API 测试');
  try {
    const res = await apiRequest('GET', '/tag/list');
    const pass = res.code === 200 && Array.isArray(res.data);
    console.log(`   ${pass ? '✅' : '❌'} GET /tag/list - ${pass ? res.data.length + ' 个标签' : res.message}`);
    if (pass && res.data.length > 0) {
      console.log(`   🏷️ 示例: "${res.data[0].name}"`);
    }
    return pass;
  } catch (error) {
    console.log(`   ❌ 标签 API 失败: ${error.message}`);
    return false;
  }
}

async function testCommentAPI() {
  console.log('\n💬 评论 API 测试');
  try {
    // 获取评论列表 - 需要传入 articleId
    const listRes = await apiRequest('GET', `/comment/list?articleId=${TEST_ARTICLE_ID}&current=1&size=5`);
    const listPass = listRes.code === 200;
    console.log(`   ${listPass ? '✅' : '❌'} GET /comment/list?articleId=${TEST_ARTICLE_ID}`);

    // 发布评论
    const publishRes = await apiRequest('POST', '/comment', {
      articleId: TEST_ARTICLE_ID,
      content: 'E2E 测试评论',
      rootId: null
    });
    const publishPass = publishRes.code === 200;
    console.log(`   ${publishPass ? '✅' : '❌'} POST /comment (发布评论)`);

    return listPass && publishPass;
  } catch (error) {
    console.log(`   ❌ 评论 API 失败: ${error.message}`);
    return false;
  }
}

async function testBlogSettingsAPI() {
  console.log('\n⚙️ 博客设置 API 测试');
  try {
    const res = await apiRequest('GET', '/blog/setting/detail');
    const pass = res.code === 200 && res.data;
    console.log(`   ${pass ? '✅' : '❌'} GET /blog/setting/detail`);
    if (pass) {
      console.log(`   🏠 博客名称: ${res.data.blogName || '未设置'}`);
      console.log(`   👤 博主昵称: ${res.data.nickname || '未设置'}`);
      console.log(`   📝 座右铭: ${res.data.motto || '未设置'}`);
    }
    return pass;
  } catch (error) {
    console.log(`   ❌ 博客设置 API 失败: ${error.message}`);
    return false;
  }
}

async function testImageUpload() {
  console.log('\n🖼️ 图片上传测试');
  try {
    // 创建一个简单的测试图片 (1x1 红色 PNG)
    const pngData = Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==',
      'base64'
    );

    const formData = new FormData();
    const blob = new Blob([pngData], { type: 'image/png' });
    formData.append('file', blob, 'test.png');

    const response = await fetch(`${API_URL}/file/upload/image`, {
      method: 'POST',
      headers: {
        ...(authToken ? { Authorization: `Bearer ${authToken}`, satoken: authToken } : {}),
      },
      body: formData,
    });

    const res = await response.json();
    const pass = res.code === 200;
    console.log(`   ${pass ? '✅' : '❌'} POST /upload/image - ${pass ? res.data || '上传成功' : res.message}`);
    return pass;
  } catch (error) {
    console.log(`   ❌ 图片上传失败: ${error.message}`);
    return false;
  }
}

async function testAgentBlogPublish() {
  console.log('\n🤖 智能体博客发布测试');
  try {
    const res = await apiRequest('POST', '/agent/chat', {
      sessionId: 'e2e-full-test-' + Date.now(),
      message: '发一篇关于 Typescript 泛型入门的博客'
    });

    const pass = res.code === 200 && res.data && res.data.response;
    if (pass) {
      console.log(`   ✅ 智能体响应成功`);
      console.log(`   📝 ${res.data.response.substring(0, 150)}...`);
    } else {
      console.log(`   ❌ 智能体响应失败: ${res.message}`);
    }
    return pass;
  } catch (error) {
    console.log(`   ❌ 智能体测试失败: ${error.message}`);
    return false;
  }
}

async function testNavigation() {
  console.log('\n🧭 页面导航测试');
  const navTests = [];

  try {
    // 返回首页
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });
    navTests.push({ name: '首页', pass: true });

    // 检查 URL 结构
    const urls = [
      { name: '文章详情页', url: '/article/detail?articleId=1' },
      { name: '分类列表页', url: '/category/list?id=1' },
      { name: '标签列表页', url: '/tag/list?id=1' },
    ];

    for (const u of urls) {
      try {
        await page.goto(BASE_URL + u.url, { waitUntil: 'networkidle', timeout: 10000 });
        navTests.push({ name: u.name, pass: true });
      } catch {
        navTests.push({ name: u.name, pass: false });
      }
    }
  } catch (error) {
    console.log(`   ❌ 导航测试出错: ${error.message}`);
  }

  navTests.forEach(n => console.log(`   ${n.pass ? '✅' : '❌'} ${n.name}`));
  return navTests.every(n => n.pass);
}

async function run() {
  console.log('╔══════════════════════════════════════════════════════════════════════╗');
  console.log('║           全面 E2E 测试 - 前后端功能检测                              ║');
  console.log('╚══════════════════════════════════════════════════════════════════════╝');

  const results = [];
  let articleId = null;

  try {
    // 启动浏览器
    browser = await chromium.launch({ headless: true });
    page = await browser.newPage();

    // 1. 登录
    if (await login()) {
      results.push({ category: '登录', name: '用户登录', pass: true });
    } else {
      results.push({ category: '登录', name: '用户登录', pass: false });
      throw new Error('登录失败，无法继续测试');
    }

    // 2. 前端页面测试
    const frontendAccess = await testFrontendAccess();
    results.push({ category: '前端', name: '首页访问', pass: frontendAccess });

    if (frontendAccess) {
      const elementsPass = await testFrontendElements();
      results.push({ category: '前端', name: '元素渲染', pass: elementsPass });

      const navPass = await testNavigation();
      results.push({ category: '前端', name: '页面导航', pass: navPass });
    }

    // 3. API 测试
    const categoryPass = await testCategoryAPI();
    results.push({ category: 'API', name: '分类接口', pass: categoryPass });

    const tagPass = await testTagAPI();
    results.push({ category: 'API', name: '标签接口', pass: tagPass });

    const settingsPass = await testBlogSettingsAPI();
    results.push({ category: 'API', name: '博客设置', pass: settingsPass });

    articleId = await testArticleListAPI();
    results.push({ category: 'API', name: '文章列表', pass: !!articleId });

    if (articleId) {
      const detailPass = await testArticleDetailAPI(articleId);
      results.push({ category: 'API', name: '文章详情', pass: detailPass });
    }

    const commentPass = await testCommentAPI();
    results.push({ category: 'API', name: '评论功能', pass: commentPass });

    const uploadPass = await testImageUpload();
    results.push({ category: 'API', name: '图片上传', pass: uploadPass });

    // 4. 智能体测试
    const agentPass = await testAgentBlogPublish();
    results.push({ category: '智能体', name: '博客发布', pass: agentPass });

  } catch (error) {
    console.error('\n❌ 测试异常:', error.message);
  } finally {
    if (browser) await browser.close();
  }

  // 汇总报告
  console.log('\n╔══════════════════════════════════════════════════════════════════════╗');
  console.log('║                          测试结果汇总                                ║');
  console.log('╚══════════════════════════════════════════════════════════════════════╝');

  const categories = [...new Set(results.map(r => r.category))];
  categories.forEach(cat => {
    const catResults = results.filter(r => r.category === cat);
    const catPass = catResults.filter(r => r.pass).length;
    console.log(`\n  【${cat}】 ${catPass}/${catResults.length} 通过`);
    catResults.forEach(r => {
      console.log(`     ${r.pass ? '✅' : '❌'} ${r.name}`);
    });
  });

  const totalPass = results.filter(r => r.pass).length;
  const totalTests = results.length;
  console.log(`\n╔══════════════════════════════════════════════════════════════════════╗`);
  console.log(`║   总计: ${totalPass}/${totalTests} 通过  ${totalPass === totalTests ? '🎉 全部通过!' : '⚠️ 部分失败'}   ║`);
  console.log(`╚══════════════════════════════════════════════════════════════════════╝`);
}

run().catch(console.error);
