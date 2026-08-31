<template>
  <div class="home-page">
    <!-- Hero -->
    <section class="hero">
      <div class="container">
        <h1 class="hero-title">
          API <span class="gradient">接口开放平台</span>
        </h1>
        <p class="hero-subtitle">
          基于 Spring Boot + Dubbo 微服务架构，提供接口管理、签名鉴权、调用统计等完整功能
        </p>
        <div class="hero-actions">
          <router-link to="/market" class="btn btn-primary">浏览接口 →</router-link>
          <a href="#architecture" class="btn btn-ghost">开发文档</a>
        </div>
      </div>
    </section>

    <!-- Architecture -->
    <section id="architecture" class="section">
      <div class="container">
        <h2 class="section-title">微服务调用链路</h2>
        <div class="arch-card">
          <div class="arch-flow">
            <div class="arch-node">
              <div class="arch-node-icon">💻</div>
              <div class="arch-node-name">客户端 / SDK</div>
            </div>
            <div class="arch-arrow">→</div>
            <div class="arch-node">
              <div class="arch-node-icon">🌐</div>
              <div class="arch-node-name">Gateway</div>
              <div class="arch-node-port">:8090</div>
            </div>
            <div class="arch-arrow">→</div>
            <div class="arch-node">
              <div class="arch-node-icon">⚙️</div>
              <div class="arch-node-name">Interface</div>
              <div class="arch-node-port">:8123</div>
            </div>
          </div>
          <div class="arch-bottom">
            <span>Backend<code>:7529</code> Dubbo Provider</span>
            <span class="dot">·</span>
            <span>Zookeeper<code>:2181</code></span>
            <span class="dot">·</span>
            <span>MySQL + Redis</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Core Features -->
    <section class="section">
      <div class="container">
        <h2 class="section-title">核心功能</h2>
        <div class="feature-grid">
          <div class="feature-card" v-for="f in features" :key="f.title">
            <div class="feature-icon">{{ f.icon }}</div>
            <h3 class="feature-title">{{ f.title }}</h3>
            <p class="feature-desc" v-html="f.desc"></p>
          </div>
        </div>
      </div>
    </section>

    <!-- Tech Stack -->
    <section class="section">
      <div class="container">
        <h2 class="section-title">技术栈</h2>
        <div class="tech-grid">
          <div class="tech-card" v-for="t in techStack" :key="t">
            <span class="tech-name">{{ t }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Stats -->
    <section class="section">
      <div class="container">
        <h2 class="section-title">数据统计</h2>
        <div class="stat-grid">
          <div class="stat-card" v-for="s in stats" :key="s.label">
            <div class="stat-value">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
        </div>
        <div class="chart-card">
          <div class="chart-header">
            <span class="chart-title">近 12 个月调用趋势</span>
            <span class="chart-unit">单位：次</span>
          </div>
          <div class="chart-body">
            <div class="chart-bar" v-for="(b, i) in bars" :key="i">
              <div class="bar-fill" :style="{ height: b.height + '%' }"></div>
              <div class="bar-label">{{ b.label }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
const features = [
  {
    icon: '🔐',
    title: '签名鉴权',
    desc: '基于 <code>AK/SK</code> 的 <code>HMAC-SHA256</code> 签名，支持 <code>nonce</code> 防重放和时间戳校验',
  },
  {
    icon: '📊',
    title: '调用统计',
    desc: '接口调用次数自动扣减，支持用户维度和接口维度的用量统计',
  },
  {
    icon: '🌐',
    title: '网关路由',
    desc: '<code>Spring Cloud Gateway</code> 统一入口，路由分发到后端管理和接口服务',
  },
  {
    icon: '📦',
    title: 'SDK 支持',
    desc: 'Java SDK 封装签名逻辑，开发者只需调用方法即可完成请求',
  },
  {
    icon: '🔗',
    title: 'Dubbo RPC',
    desc: '网关通过 <code>Dubbo</code> 调用后端服务，获取用户信息并扣减调用次数',
  },
  {
    icon: '🐳',
    title: 'Docker 部署',
    desc: '完整的 <code>docker-compose</code> 编排，一键启动全部服务和中间件',
  },
]

const techStack = [
  'Java 17',
  'Spring Boot 2.7.x',
  'Spring Cloud Gateway',
  'Dubbo 3.0.9',
  'Zookeeper 3.7.2',
  'MySQL 8.0',
  'Redis 6.x',
  'Docker Compose',
]

const stats = [
  { label: '总调用次数', value: '26,181' },
  { label: '今日调用', value: '1,247' },
  { label: '活跃用户', value: '38' },
  { label: '接口数量', value: '5' },
]

const bars = [
  { label: '1月', height: 42 },
  { label: '2月', height: 55 },
  { label: '3月', height: 48 },
  { label: '4月', height: 67 },
  { label: '5月', height: 60 },
  { label: '6月', height: 75 },
  { label: '7月', height: 70 },
  { label: '8月', height: 82 },
  { label: '9月', height: 65 },
  { label: '10月', height: 88 },
  { label: '11月', height: 78 },
  { label: '12月', height: 95 },
]
</script>

<style scoped>
.home-page {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Helvetica Neue', Arial, sans-serif;
  color: #1f2937;
  scroll-behavior: smooth;
}

code {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  background: #f3f4f6;
  color: #1f2937;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 0.9em;
}

/* ===== Hero ===== */
.hero {
  padding: 96px 0 72px;
  text-align: center;
}

.hero-title {
  font-size: 52px;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.5px;
  color: #1f2937;
}

.hero-title .gradient {
  background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
}

.hero-subtitle {
  margin: 20px auto 0;
  max-width: 680px;
  font-size: 17px;
  line-height: 1.7;
  color: #6b7280;
}

.hero-actions {
  margin-top: 36px;
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 46px;
  padding: 0 28px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.btn-primary {
  background: #111827;
  color: #ffffff;
}

.btn-primary:hover {
  background: #000000;
  color: #ffffff;
}

.btn-ghost {
  background: #ffffff;
  color: #1f2937;
  border-color: #e5e7eb;
}

.btn-ghost:hover {
  border-color: #1f2937;
  color: #1f2937;
}

/* ===== Section ===== */
.section {
  padding: 56px 0;
}

.section-title {
  font-size: 26px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 28px;
}

/* ===== Architecture ===== */
.arch-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 40px 32px;
}

.arch-flow {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.arch-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 150px;
  padding: 24px 20px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.arch-node-icon {
  font-size: 30px;
}

.arch-node-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.arch-node-port {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  color: #2563eb;
}

.arch-arrow {
  font-size: 22px;
  color: #9ca3af;
  font-weight: 600;
}

.arch-bottom {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 14px;
  color: #6b7280;
}

.arch-bottom code {
  margin: 0 2px;
}

.arch-bottom .dot {
  color: #d1d5db;
}

/* ===== Features ===== */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 28px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.feature-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.08);
}

.feature-icon {
  font-size: 32px;
  margin-bottom: 14px;
}

.feature-title {
  font-size: 17px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 10px;
}

.feature-desc {
  font-size: 14px;
  line-height: 1.7;
  color: #6b7280;
}

/* ===== Tech Stack ===== */
.tech-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.tech-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 22px 20px;
  text-align: center;
  transition: border-color 0.2s ease;
}

.tech-card:hover {
  border-color: #2563eb;
}

.tech-name {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

/* ===== Stats ===== */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 28px 24px;
}

.stat-value {
  font-size: 32px;
  font-weight: 800;
  color: #1f2937;
  line-height: 1.1;
}

.stat-label {
  margin-top: 8px;
  font-size: 14px;
  color: #6b7280;
}

.chart-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 28px 32px;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.chart-unit {
  font-size: 13px;
  color: #9ca3af;
}

.chart-body {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  height: 220px;
  padding-top: 12px;
}

.chart-bar {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  height: 100%;
  justify-content: flex-end;
}

.bar-fill {
  width: 100%;
  max-width: 38px;
  background: linear-gradient(180deg, #60a5fa 0%, #2563eb 100%);
  border-radius: 6px 6px 0 0;
  transition: opacity 0.2s ease;
}

.chart-bar:hover .bar-fill {
  opacity: 0.85;
}

.bar-label {
  font-size: 12px;
  color: #9ca3af;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .hero-title {
    font-size: 38px;
  }
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .tech-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .hero {
    padding: 64px 0 48px;
  }
  .hero-title {
    font-size: 30px;
  }
  .hero-subtitle {
    font-size: 15px;
  }
  .feature-grid,
  .tech-grid,
  .stat-grid {
    grid-template-columns: 1fr;
  }
  .arch-node {
    min-width: 120px;
  }
  .arch-arrow {
    transform: rotate(90deg);
  }
}
</style>
