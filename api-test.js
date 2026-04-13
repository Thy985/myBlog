/**
 * 后端 API 全面接口测试
 */
const API_URL = 'http://localhost:8080/api';
const ADMIN_USER = 'admin';
const ADMIN_PASS = '147258369Thy@';

let authToken = '';
let testArticleId = null;
let testCategoryId = null;
let testTagId = null;
let testCommentId = null;

const results = [];

function pass(name) {
  console.log(`  ✅ ${name}`);
  results.push({ name, status: 'PASS' });
}

function fail(name, reason) {
  console.log(`  ❌ ${name}: ${reason}`);
  results.push({ name, status: 'FAIL', reason });
}

function skip(name, reason) {
  console.log(`  ⚠️  ${name}: ${reason}`);
  results.push({ name, status: 'SKIP', reason });
}

async function req(method, endpoint, data = null) {
  const opts = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(authToken ? { satoken: authToken } : {})
    }
  };
  if (data) opts.body = JSON.stringify(data);
  try {
    const res = await fetch(`${API_URL}${endpoint}`, opts);
    const json = await res.json();
    return json;
  } catch (e) {
    return { code: -1, message: e.message };
  }
}

// ============================================================
// 健康检查
// ============================================================
async function testHealth() {
  console.log('\n📦 【健康检查】');
  const r = await req('GET', '/health');
  if (r.code === 200 || r.status === 'UP') {
    pass('GET /health');
  } else {
    fail('GET /health', JSON.stringify(r));
  }
}

// ============================================================
// 认证模块
// ============================================================
async function testAuth() {
  console.log('\n📦 【认证模块】');

  const loginRes = await req('POST', '/auth/login', { username: ADMIN_USER, password: ADMIN_PASS });
  if (loginRes.code === 200 && loginRes.data?.token) {
    authToken = loginRes.data.token;
    pass('POST /auth/login - 登录成功');
  } else {
    fail('POST /auth/login', loginRes.message);
    throw new Error('登录失败，终止测试');
  }

  const wrongLogin = await req('POST', '/auth/login', { username: ADMIN_USER, password: 'wrongpassword' });
  if (wrongLogin.code !== 200) {
    pass('POST /auth/login - 错误密码拒绝');
  } else {
    fail('POST /auth/login - 错误密码拒绝', '应拒绝但成功了');
  }

  const meRes = await req('GET', '/user/profile');
  if (meRes.code === 200 && meRes.data) {
    pass('GET /user/profile');
  } else {
    fail('GET /user/profile', meRes.message);
  }
}

// ============================================================
// 分类模块
// ============================================================
async function testCategories() {
  console.log('\n📦 【分类模块】');

  const listRes = await req('GET', '/category/list');
  if (listRes.code === 200 && Array.isArray(listRes.data)) {
    pass(`GET /category/list (${listRes.data.length} 条)`);
    if (listRes.data.length > 0) testCategoryId = listRes.data[0].id;
  } else {
    fail('GET /category/list', listRes.message);
  }

  const catName = 'API测试分类_' + Date.now();
  const createRes = await req('POST', '/category', {
    name: catName,
    description: '自动测试创建',
    sortOrder: 99
  });
  if (createRes.code === 200) {
    pass('POST /category - 创建分类');
    // id 回填由 MyBatis-Flex 主键配置决定，用名字从列表查
    const listAfter = await req('GET', '/category/list');
    if (listAfter.data) {
      const found = listAfter.data.find(c => c.name === catName || c.categoryName === catName);
      if (found?.id) testCategoryId = found.id;
    }
  } else {
    fail('POST /category', createRes.message);
  }

  if (testCategoryId) {
    const getRes = await req('GET', `/category/${testCategoryId}`);
    if (getRes.code === 200) {
      pass('GET /category/:id - 获取分类详情');
    } else {
      fail('GET /category/:id', getRes.message);
    }

    const updateRes = await req('PUT', `/category/${testCategoryId}`, {
      name: 'API测试分类_已更新',
      description: '已更新',
      sortOrder: 99
    });
    if (updateRes.code === 200) {
      pass('PUT /category/:id - 更新分类');
    } else {
      fail('PUT /category/:id', updateRes.message);
    }
  }
}

// ============================================================
// 标签模块
// ============================================================
async function testTags() {
  console.log('\n📦 【标签模块】');

  const listRes = await req('GET', '/tag/list');
  if (listRes.code === 200 && Array.isArray(listRes.data)) {
    pass(`GET /tag/list (${listRes.data.length} 条)`);
  } else {
    fail('GET /tag/list', listRes.message);
  }

  // Tag create uses @RequestParam (not @RequestBody)
  const tagName = 'API测试标签_' + Date.now();
  const tagCreateRes = await fetch(`${API_URL}/tag?name=${encodeURIComponent(tagName)}&color=%23FF6B6B`, {
    method: 'POST',
    headers: { satoken: authToken }
  }).then(r => r.json()).catch(e => ({ code: -1, message: e.message }));
  if (tagCreateRes.code === 200) {
    pass('POST /tag - 创建标签');
    // 从列表找到刚创建的标签获取 id
    const tagList = await req('GET', '/tag/list');
    if (tagList.data) {
      const found = tagList.data.find(t => t.name === tagName || t.tagName === tagName);
      if (found?.id) testTagId = found.id;
    }
  } else {
    fail('POST /tag', tagCreateRes.message);
  }

  const hotRes = await req('GET', '/tag/hot');
  if (hotRes.code === 200) {
    pass('GET /tag/hot - 热门标签');
  } else {
    fail('GET /tag/hot', hotRes.message);
  }
}

// ============================================================
// 文章模块
// ============================================================
async function testArticles() {
  console.log('\n📦 【文章模块】');

  const listRes = await req('GET', '/article/list?page=1&pageSize=10');
  if (listRes.code === 200 && listRes.data) {
    const count = listRes.data.records?.length ?? 0;
    pass(`GET /article/list (${count} 条)`);
  } else {
    fail('GET /article/list', listRes.message);
  }

  const createRes = await req('POST', '/article', {
    title: 'API测试文章_' + Date.now(),
    content: '# 测试文章\n\n这是由 API 接口测试自动创建的测试文章，内容用于验证接口功能是否正常。',
    summary: 'API 测试自动生成',
    categoryId: testCategoryId || 2,
    tagIds: testTagId ? [testTagId] : [],
    status: 0,
    commentStatus: 1
  });
  if (createRes.code === 200 && createRes.data?.id) {
    testArticleId = createRes.data.id;
    pass('POST /article - 创建文章');
  } else {
    fail('POST /article', createRes.message);
  }

  if (testArticleId) {
    const detailRes = await req('GET', `/article/${testArticleId}`);
    if (detailRes.code === 200 && detailRes.data) {
      pass('GET /article/:id - 文章详情');
    } else {
      fail('GET /article/:id', detailRes.message);
    }

    const updateRes = await req('PUT', `/article/${testArticleId}`, {
      title: 'API测试文章_已更新',
      content: '# 更新后的测试文章\n内容已更新。',
      summary: '已更新的摘要',
      categoryId: testCategoryId || 2,
    });
    if (updateRes.code === 200) {
      pass('PUT /article/:id - 更新文章');
    } else {
      fail('PUT /article/:id', updateRes.message);
    }

    const publishRes = await req('POST', `/article/${testArticleId}/publish`);
    if (publishRes.code === 200) {
      pass('POST /article/:id/publish - 发布文章');
    } else {
      fail('POST /article/:id/publish', publishRes.message);
    }

    const statsRes = await req('GET', `/article/${testArticleId}/stats`);
    if (statsRes.code === 200) {
      pass('GET /article/:id/stats - 文章统计');
    } else {
      fail('GET /article/:id/stats', statsRes.message);
    }
  }

  const searchRes = await req('GET', '/article/search?keyword=测试&page=1&pageSize=5');
  if (searchRes.code === 200) {
    pass('GET /article/search - 文章搜索');
  } else {
    fail('GET /article/search', searchRes.message);
  }

  const hotRes = await req('GET', '/article/hot');
  if (hotRes.code === 200) {
    pass('GET /article/hot - 热门文章');
  } else {
    fail('GET /article/hot', hotRes.message);
  }

  const archiveRes = await req('GET', '/article/archive');
  if (archiveRes.code === 200) {
    pass('GET /article/archive - 文章归档');
  } else {
    fail('GET /article/archive', archiveRes.message);
  }
}

// ============================================================
// 评论模块
// ============================================================
async function testComments() {
  console.log('\n📦 【评论模块】');

  if (!testArticleId) {
    skip('评论模块', '无文章ID');
    return;
  }

  const createRes = await req('POST', '/comment', {
    articleId: testArticleId,
    content: 'API 接口测试自动发布的评论'
  });
  if (createRes.code === 200 && createRes.data?.id) {
    testCommentId = createRes.data.id;
    pass('POST /comment - 发布评论');
  } else {
    fail('POST /comment', createRes.message);
  }

  const listRes = await req('GET', `/comment/list?articleId=${testArticleId}`);
  if (listRes.code === 200) {
    pass('GET /comment/list - 获取评论列表');
  } else {
    fail('GET /comment/list', listRes.message);
  }
}

// ============================================================
// 仪表盘模块
// ============================================================
async function testDashboard() {
  console.log('\n📦 【仪表盘模块】');

  const overviewRes = await req('GET', '/admin/dashboard/stats/overview');
  if (overviewRes.code === 200 && overviewRes.data) {
    pass('GET /admin/dashboard/stats/overview - 总览统计');
  } else {
    fail('GET /admin/dashboard/stats/overview', overviewRes.message);
  }

  const hotArticlesRes = await req('GET', '/admin/dashboard/stats/hot-articles');
  if (hotArticlesRes.code === 200) {
    pass('GET /admin/dashboard/stats/hot-articles - 热门文章');
  } else {
    fail('GET /admin/dashboard/stats/hot-articles', hotArticlesRes.message);
  }

  const categoryDistRes = await req('GET', '/admin/dashboard/stats/category-distribution');
  if (categoryDistRes.code === 200) {
    pass('GET /admin/dashboard/stats/category-distribution - 分类分布');
  } else {
    fail('GET /admin/dashboard/stats/category-distribution', categoryDistRes.message);
  }
}

// ============================================================
// 用户 API Key 模块
// ============================================================
async function testUserApiKey() {
  console.log('\n📦 【用户 API Key 模块】');

  const getRes = await req('GET', '/user/apikey');
  if (getRes.code === 200) {
    pass('GET /user/apikey - 获取 API Key 信息');
  } else {
    fail('GET /user/apikey', getRes.message);
  }

  const usageRes = await req('GET', '/user/apikey/usage');
  if (usageRes.code === 200) {
    pass('GET /user/apikey/usage - API Key 使用统计');
  } else {
    fail('GET /user/apikey/usage', usageRes.message);
  }

  const modelsRes = await req('GET', '/user/apikey/models?provider=deepseek');
  if (modelsRes.code === 200) {
    pass('GET /user/apikey/models?provider=deepseek - 可用模型列表');
  } else {
    fail('GET /user/apikey/models', modelsRes.message);
  }
}

// ============================================================
// 博客设置模块
// ============================================================
async function testBlogSettings() {
  console.log('\n📦 【博客设置模块】');

  const getRes = await req('GET', '/blog/setting/detail');
  if (getRes.code === 200) {
    pass('GET /blog/setting/detail - 获取博客设置');
  } else {
    fail('GET /blog/setting/detail', getRes.message);
  }
}

// ============================================================
// AI 模块 (V2)
// ============================================================
async function testAI() {
  console.log('\n📦 【AI 模块 (V2)】');

  const chatRes = await req('POST', '/v2/ai/chat', {
    message: '用一句话描述什么是人工智能',
    sessionId: 'ai-test-' + Date.now()
  });
  if (chatRes.code === 200 && chatRes.data?.content) {
    const preview = chatRes.data.content.substring(0, 80).replace(/\n/g, ' ');
    pass(`POST /v2/ai/chat ("${preview}...")`);
  } else {
    fail('POST /v2/ai/chat', chatRes.message);
  }
}

// ============================================================
// 智能体模块
// ============================================================
async function testAgent() {
  console.log('\n📦 【智能体模块】');

  const sessionRes = await req('POST', '/agent/session/start');
  let sessionId = 'test-session-' + Date.now();
  if (sessionRes.code === 200 && sessionRes.data?.sessionId) {
    sessionId = sessionRes.data.sessionId;
    pass('POST /agent/session/start - 创建会话');
  } else {
    fail('POST /agent/session/start', sessionRes.message);
  }

  const chatRes = await req('POST', '/agent/chat', {
    sessionId,
    message: '你好，你有什么能力？'
  });
  if (chatRes.code === 200 && chatRes.data?.response) {
    const preview = chatRes.data.response.substring(0, 80).replace(/\n/g, ' ');
    pass(`POST /agent/chat - 普通对话 ("${preview}...")`);
  } else {
    fail('POST /agent/chat', chatRes.message);
  }
}

// ============================================================
// 清理测试数据
// ============================================================
async function cleanup() {
  console.log('\n📦 【清理测试数据】');

  if (testCommentId) {
    const r = await req('DELETE', `/comment/${testCommentId}`);
    r.code === 200 ? pass('DELETE /comment/:id') : skip('DELETE /comment/:id', r.message);
  }

  if (testArticleId) {
    const r = await req('DELETE', `/article/${testArticleId}`);
    r.code === 200 ? pass('DELETE /article/:id') : skip('DELETE /article/:id', r.message);
  }

  if (testTagId) {
    const r = await req('DELETE', `/tag/${testTagId}`);
    r.code === 200 ? pass('DELETE /tag/:id') : skip('DELETE /tag/:id', r.message);
  }

  if (testCategoryId) {
    const r = await req('DELETE', `/category/${testCategoryId}`);
    r.code === 200 ? pass('DELETE /category/:id') : skip('DELETE /category/:id', r.message);
  }
}

// ============================================================
// 主入口
// ============================================================
async function run() {
  console.log('╔══════════════════════════════════════════════════════════════╗');
  console.log('║              后端 API 全面接口测试                          ║');
  console.log('╚══════════════════════════════════════════════════════════════╝');
  console.log(`  目标: ${API_URL}\n`);

  try {
    await testHealth();
    await testAuth();
    await testCategories();
    await testTags();
    await testArticles();
    await testComments();
    await testDashboard();
    await testUserApiKey();
    await testBlogSettings();
    await testAI();
    await testAgent();
    await cleanup();
  } catch (e) {
    console.error('\n❌ 测试意外终止:', e.message);
  }

  const passed = results.filter(r => r.status === 'PASS').length;
  const failed = results.filter(r => r.status === 'FAIL');
  const skipped = results.filter(r => r.status === 'SKIP').length;
  const total = results.length;

  console.log('\n╔══════════════════════════════════════════════════════════════╗');
  console.log('║                        测试结果汇总                         ║');
  console.log('╚══════════════════════════════════════════════════════════════╝');
  console.log(`  ✅ 通过: ${passed}  ❌ 失败: ${failed.length}  ⚠️  跳过: ${skipped}  📊 总计: ${total}`);

  if (failed.length > 0) {
    console.log('\n  失败详情:');
    failed.forEach(f => console.log(`    ❌ ${f.name}: ${f.reason}`));
  }

  const denominator = total - skipped;
  const rate = denominator > 0 ? Math.round((passed / denominator) * 100) : 0;
  console.log(`\n  通过率: ${rate}% ${rate >= 90 ? '🎉' : rate >= 70 ? '📈' : '⚠️'}`);
}

run().catch(console.error);
