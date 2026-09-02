<template>
  <div class="docs-page">
    <div class="docs-shell container">
      <aside class="docs-sidebar" aria-label="开发文档目录">
        <div v-for="group in menuGroups" :key="group.title" class="menu-group">
          <p class="menu-group-title">{{ group.title }}</p>
          <button
            v-for="item in group.items"
            :key="item.key"
            class="doc-menu"
            :class="{ active: currentSection === item.key }"
            type="button"
            @click="selectSection(item.key)"
          >
            {{ item.label }}
          </button>
        </div>
      </aside>

      <section class="docs-main">
        <article v-if="currentSection === 'overview'" class="markdown">
          <h1>TEN API 接口开放平台</h1>
          <p class="lead">
            一个面向接口提供者和调用者的 API 管理平台，覆盖接口发布、开发者鉴权、配额扣减与调用记录。
          </p>
          <h2>项目定位</h2>
          <p>
            平台通过统一网关接收调用请求，并使用 <code>AccessKey</code> 标识调用方、使用
            <code>SecretKey</code> 完成签名校验。管理端负责用户、接口和调用额度的维护。
          </p>
          <blockquote>
            <p>首页只负责展示项目名称和接口示例。实现细节、接入方式与运维说明统一放在开发文档中。</p>
          </blockquote>
          <h2>模块组成</h2>
          <table>
            <thead>
              <tr><th>模块</th><th>职责</th></tr>
            </thead>
            <tbody>
              <tr><td><code>ten-api-gateway</code></td><td>请求鉴权、路由、限流与调用扣减</td></tr>
              <tr><td><code>ten-api-backend</code></td><td>用户、接口和管理能力</td></tr>
              <tr><td><code>ten-api-interface</code></td><td>提供实际可调用的示例接口</td></tr>
              <tr><td><code>api-client-sdk</code></td><td>封装签名与请求发送逻辑</td></tr>
            </tbody>
          </table>
        </article>

        <article v-else-if="currentSection === 'quickstart'" class="markdown">
          <h1>快速开始</h1>
          <p class="lead">使用 Docker Compose 启动平台并验证服务健康状态。</p>
          <h2>环境要求</h2>
          <ul>
            <li>Docker Engine 24 或更高版本</li>
            <li>Docker Compose v2</li>
            <li>4 核 CPU、8 GB 内存的 Linux 服务器</li>
          </ul>
          <h2>启动服务</h2>
          <pre><code>cp .env.example .env
docker compose --env-file .env up -d --build
docker compose ps</code></pre>
          <p>全部服务显示 <code>healthy</code> 后，通过服务器 HTTP 地址访问前端。</p>
        </article>

        <article v-else-if="currentSection === 'request'" class="markdown">
          <h1>接口调用</h1>
          <p class="lead">所有开放接口统一通过 Gateway 访问，业务服务不直接暴露到公网。</p>
          <h2>请求地址</h2>
          <pre><code>POST /api/name/object
Content-Type: application/json

{
  "username": "ten-api"
}</code></pre>
          <h2>响应约定</h2>
          <table>
            <thead><tr><th>字段</th><th>说明</th></tr></thead>
            <tbody>
              <tr><td><code>code</code></td><td>业务状态码，成功为 0</td></tr>
              <tr><td><code>data</code></td><td>接口响应数据</td></tr>
              <tr><td><code>message</code></td><td>状态说明</td></tr>
            </tbody>
          </table>
        </article>

        <article v-else-if="currentSection === 'auth'" class="markdown">
          <h1>签名鉴权</h1>
          <p class="lead">开放接口使用 AK/SK 签名，平台管理请求使用 Session 登录态。</p>
          <h2>签名请求头</h2>
          <table>
            <thead><tr><th>请求头</th><th>说明</th></tr></thead>
            <tbody>
              <tr><td><code>accessKey</code></td><td>调用方标识</td></tr>
              <tr><td><code>nonce</code></td><td>至少 5 位的随机数</td></tr>
              <tr><td><code>timestamp</code></td><td>当前秒级时间戳</td></tr>
              <tr><td><code>sign</code></td><td>使用 SecretKey 生成的请求签名</td></tr>
            </tbody>
          </table>
          <h2>校验顺序</h2>
          <ol>
            <li>校验 accessKey 与时间戳有效性。</li>
            <li>使用 Redis SETNX 和 TTL 拦截重放请求。</li>
            <li>重新计算签名并进行安全比较。</li>
            <li>校验通过后执行接口调用和原子额度扣减。</li>
          </ol>
        </article>

        <article v-else-if="currentSection === 'sdk'" class="markdown">
          <h1>Java SDK</h1>
          <p class="lead">SDK 负责生成签名请求头并发送 HTTP 请求。</p>
          <h2>安装依赖</h2>
          <pre><code>&lt;dependency&gt;
  &lt;groupId&gt;com.ten&lt;/groupId&gt;
  &lt;artifactId&gt;api-client-sdk&lt;/artifactId&gt;
  &lt;version&gt;0.0.1-SNAPSHOT&lt;/version&gt;
&lt;/dependency&gt;</code></pre>
          <h2>创建客户端</h2>
          <pre><code>ApiClient client = new ApiClient(accessKey, secretKey);
String result = client.getNameByPOSTJson(user);</code></pre>
        </article>

        <article v-else-if="currentSection === 'architecture'" class="markdown">
          <h1>系统架构</h1>
          <p class="lead">管理请求和开放接口调用通过同一个网关进入不同的服务边界。</p>
          <div class="inline-flow">
            Browser / SDK → Nginx → Gateway :8090 → Backend :7529 / Interface :8123
          </div>
          <h2>服务协作</h2>
          <ul>
            <li>Gateway 使用 WebFlux 响应式模型处理入口流量。</li>
            <li>Backend 通过 Dubbo 提供用户鉴权和额度扣减能力。</li>
            <li>Zookeeper 负责 Dubbo 服务发现，Redis 负责会话和防重放。</li>
            <li>MySQL 保存用户、接口和调用配额等持久化数据。</li>
          </ul>
        </article>

        <article v-else-if="currentSection === 'capabilities'" class="markdown">
          <h1>核心能力</h1>
          <p class="lead">平台能力以接口生命周期和调用链路为中心组织。</p>
          <h2>接口管理</h2>
          <p>管理员维护接口元数据、请求参数和上线状态；接口市场只展示已上线接口。</p>
          <h2>调用鉴权</h2>
          <p>网关校验签名、时间戳和 nonce，并在请求通过后执行原子额度扣减。</p>
          <h2>服务治理</h2>
          <p>网关提供限流、超时和熔断配置，Dubbo 调用在隔离线程池中执行。</p>
        </article>

        <article v-else-if="currentSection === 'stack'" class="markdown">
          <h1>技术栈</h1>
          <p class="lead">技术选型围绕 Java 微服务、统一网关和容器化部署展开。</p>
          <table>
            <thead><tr><th>层级</th><th>技术</th></tr></thead>
            <tbody>
              <tr><td>前端</td><td>Vue 3、Vite、Element Plus、Pinia</td></tr>
              <tr><td>应用服务</td><td>Java 8、Spring Boot 2.7、MyBatis-Plus</td></tr>
              <tr><td>网关与 RPC</td><td>Spring Cloud Gateway、Dubbo 3</td></tr>
              <tr><td>基础设施</td><td>MySQL 8、Redis 6、Zookeeper 3.7</td></tr>
              <tr><td>部署</td><td>Docker、Docker Compose、Nginx</td></tr>
            </tbody>
          </table>
        </article>

        <article v-else-if="currentSection === 'deployment'" class="markdown">
          <h1>Docker 部署</h1>
          <p class="lead">生产环境使用 Compose 编排前端、Java 服务和基础设施。</p>
          <h2>配置原则</h2>
          <ul>
            <li>密码和连接信息由 <code>.env</code> 注入，不写入仓库。</li>
            <li>所有服务配置健康检查和 CPU、内存限制。</li>
            <li>MySQL、Redis 和 Zookeeper 仅在容器网络内访问。</li>
            <li>Nginx 对外监听 80 端口，并将 <code>/api</code> 转发到 Gateway。</li>
          </ul>
          <h2>更新应用</h2>
          <pre><code>git pull
mvn clean package -DskipTests
docker compose --env-file .env up -d --build</code></pre>
        </article>

        <article v-else class="markdown">
          <h1>调用统计</h1>
          <p class="lead">统计页面属于登录后的工作台，开发文档仅说明指标口径。</p>
          <h2>指标定义</h2>
          <table>
            <thead><tr><th>指标</th><th>口径</th></tr></thead>
            <tbody>
              <tr><td>总调用次数</td><td>历史成功扣减的调用次数</td></tr>
              <tr><td>剩余次数</td><td>用户在指定接口上的可用额度</td></tr>
              <tr><td>失败请求</td><td>鉴权、限流或业务处理失败的请求</td></tr>
            </tbody>
          </table>
          <blockquote>
            <p>展示数据应来自真实统计接口，首页不放置模拟数据和趋势图。</p>
          </blockquote>
        </article>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const menuGroups = [
  {
    title: '开始',
    items: [
      { key: 'overview', label: '项目说明' },
      { key: 'quickstart', label: '快速开始' },
    ],
  },
  {
    title: '接入',
    items: [
      { key: 'request', label: '接口调用' },
      { key: 'auth', label: '签名鉴权' },
      { key: 'sdk', label: 'Java SDK' },
    ],
  },
  {
    title: '实现',
    items: [
      { key: 'architecture', label: '系统架构' },
      { key: 'capabilities', label: '核心能力' },
      { key: 'stack', label: '技术栈' },
    ],
  },
  {
    title: '运维',
    items: [
      { key: 'deployment', label: 'Docker 部署' },
      { key: 'metrics', label: '调用统计' },
    ],
  },
]

const sectionKeys = new Set(menuGroups.flatMap((group) => group.items.map((item) => item.key)))
const currentSection = computed(() => {
  const section = String(route.params.section || 'overview')
  return sectionKeys.has(section) ? section : 'overview'
})

const selectSection = (section) => {
  router.push({ name: 'Docs', params: { section } })
}
</script>

<style scoped>
.docs-shell {
  min-height: calc(100vh - 64px);
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 68px;
}

.docs-sidebar {
  position: sticky;
  top: 64px;
  height: calc(100vh - 64px);
  overflow-y: auto;
  padding: 38px 22px 48px 0;
  border-right: 1px solid #e2e6ea;
}

.menu-group + .menu-group {
  margin-top: 26px;
}

.menu-group-title {
  margin-bottom: 8px;
  color: #929aa6;
  font-size: 11px;
  font-weight: 700;
}

.doc-menu {
  width: 100%;
  min-height: 34px;
  margin: 1px 0;
  padding: 6px 9px;
  border: 0;
  border-left: 2px solid transparent;
  background: transparent;
  color: #596273;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  text-align: left;
}

.doc-menu:hover {
  color: #111827;
}

.doc-menu.active {
  border-left-color: #2563eb;
  background: #f5f7fa;
  color: #111827;
  font-weight: 650;
}

.docs-main {
  min-width: 0;
  padding: 52px 0 96px;
}

.markdown {
  max-width: 780px;
}

.markdown h1 {
  margin-bottom: 12px;
  color: #111827;
  font-size: 36px;
  font-weight: 750;
  line-height: 1.25;
}

.markdown h2 {
  margin: 44px 0 14px;
  padding-bottom: 9px;
  border-bottom: 1px solid #e2e6ea;
  color: #111827;
  font-size: 23px;
  font-weight: 700;
  line-height: 1.35;
}

.markdown p {
  margin: 12px 0;
  color: #3f4856;
}

.markdown .lead {
  margin: 0 0 30px;
  color: #667085;
  font-size: 16px;
}

.markdown ul,
.markdown ol {
  margin: 12px 0;
  padding-left: 24px;
  color: #3f4856;
}

.markdown li {
  margin: 6px 0;
}

.markdown code {
  padding: 2px 5px;
  border-radius: 3px;
  background: #f0f2f5;
  color: #9d174d;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 0.9em;
}

.markdown pre {
  margin: 18px 0;
  overflow-x: auto;
  border: 1px solid #2b3544;
  border-radius: 6px;
  background: #18202d;
  color: #e5eaf0;
}

.markdown pre code {
  display: block;
  min-width: max-content;
  padding: 18px 20px;
  background: transparent;
  color: inherit;
  font-size: 13px;
  line-height: 1.65;
}

.markdown blockquote {
  margin: 20px 0;
  padding: 8px 18px;
  border-left: 3px solid #2563eb;
  background: #f7f9fc;
}

.markdown table {
  width: 100%;
  margin: 18px 0;
  border-collapse: collapse;
  font-size: 14px;
}

.markdown th,
.markdown td {
  padding: 11px 12px;
  border: 1px solid #e2e6ea;
  text-align: left;
}

.markdown th {
  background: #f7f8fa;
  font-weight: 650;
}

.inline-flow {
  margin: 18px 0;
  padding: 15px 0;
  overflow-x: auto;
  border-top: 1px solid #e2e6ea;
  border-bottom: 1px solid #e2e6ea;
  color: #343d4b;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  white-space: nowrap;
}

@media (max-width: 820px) {
  .docs-shell {
    display: block;
  }

  .docs-sidebar {
    position: sticky;
    top: 64px;
    z-index: 10;
    height: auto;
    margin: 0 -24px;
    padding: 10px 24px;
    overflow-x: auto;
    border-right: 0;
    border-bottom: 1px solid #e2e6ea;
    background: rgba(255, 255, 255, 0.98);
    white-space: nowrap;
  }

  .menu-group,
  .menu-group + .menu-group {
    display: inline-flex;
    margin: 0 16px 0 0;
  }

  .menu-group-title {
    display: none;
  }

  .doc-menu {
    width: auto;
    margin: 0 2px;
    border: 0;
    border-bottom: 2px solid transparent;
  }

  .doc-menu.active {
    border-bottom-color: #2563eb;
    background: transparent;
  }

  .docs-main {
    padding: 38px 0 72px;
  }
}

@media (max-width: 560px) {
  .markdown h1 {
    font-size: 30px;
  }
}
</style>
