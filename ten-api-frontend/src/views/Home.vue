<template>
  <div class="home-page">
    <section class="home-intro container">
      <h1>TEN API 接口开放平台</h1>
      <p>
        稳定、可管理的 API 服务，统一处理接口发布、签名鉴权、调用配额和使用记录。
      </p>
    </section>

    <section class="api-section container">
      <div class="section-heading">
        <div>
          <h2>接口示例</h2>
          <p>仅作内容展示，不提供详情或调用入口。</p>
        </div>
        <span class="availability">{{ featuredInterfaces.length }} 条展示</span>
      </div>

      <div class="api-table-wrap">
        <table class="api-table">
          <thead>
            <tr>
              <th class="method-column">方法</th>
              <th class="path-column">请求路径</th>
              <th>接口说明</th>
              <th class="status-column">状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in featuredInterfaces" :key="item.path">
              <td><MethodTag :method="item.method" /></td>
              <td class="api-path">{{ item.path }}</td>
              <td>
                <div class="api-name">{{ item.name }}</div>
                <div class="api-description">{{ item.description }}</div>
              </td>
              <td>
                <span class="status" :class="{ restricted: item.restricted }">
                  {{ item.restricted ? '受限' : '在线' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import MethodTag from '@/components/MethodTag.vue'

const featuredInterfaces = [
  {
    method: 'GET',
    path: '/api/name/ga',
    name: '查询名称',
    description: '通过查询参数返回名称结果',
  },
  {
    method: 'POST',
    path: '/api/name/object',
    name: '名称对象处理',
    description: '提交 JSON 对象并返回处理结果',
  },
  {
    method: 'POST',
    path: '/api/name/path/{name}',
    name: '路径参数示例',
    description: '通过路径变量传递调用参数',
  },
  {
    method: 'PUT',
    path: '/api/interfaceInfo/update',
    name: '接口配置更新',
    description: '管理员维护接口元数据和发布状态',
    restricted: true,
  },
]
</script>

<style scoped>
.home-intro {
  min-height: 330px;
  padding-top: 88px;
  padding-bottom: 64px;
  border-bottom: 1px solid #e2e6ea;
}

.home-intro h1 {
  max-width: 780px;
  color: #111827;
  font-size: clamp(38px, 5vw, 64px);
  font-weight: 760;
  line-height: 1.12;
}

.home-intro p {
  max-width: 700px;
  margin-top: 22px;
  color: #667085;
  font-size: 17px;
  line-height: 1.8;
}

.api-section {
  padding-top: 52px;
  padding-bottom: 88px;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.section-heading h2 {
  color: #111827;
  font-size: 24px;
  font-weight: 720;
}

.section-heading p {
  margin-top: 4px;
  color: #667085;
  font-size: 14px;
}

.availability {
  color: #16734a;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  white-space: nowrap;
}

.api-table-wrap {
  overflow-x: auto;
  border-top: 1px solid #cfd5dc;
  border-bottom: 1px solid #e2e6ea;
}

.api-table {
  width: 100%;
  min-width: 720px;
  border-collapse: collapse;
  table-layout: fixed;
}

.api-table th,
.api-table td {
  padding: 17px 14px;
  border-bottom: 1px solid #e2e6ea;
  text-align: left;
  vertical-align: middle;
}

.api-table tr:last-child td {
  border-bottom: 0;
}

.api-table th {
  color: #7b8492;
  font-size: 12px;
  font-weight: 600;
}

.method-column {
  width: 86px;
}

.path-column {
  width: 290px;
}

.status-column {
  width: 90px;
}

.api-path {
  color: #252d3a;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  word-break: break-all;
}

.api-name {
  color: #111827;
  font-weight: 620;
}

.api-description {
  margin-top: 2px;
  color: #667085;
  font-size: 13px;
}

.status {
  color: #16734a;
  font-size: 13px;
}

.status.restricted {
  color: #9a6700;
}

@media (max-width: 820px) {
  .home-intro {
    min-height: 290px;
    padding-top: 62px;
    padding-bottom: 46px;
  }
}

@media (max-width: 560px) {
  .home-intro h1 {
    font-size: 37px;
  }

  .home-intro p {
    font-size: 15px;
  }

  .section-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .api-table-wrap {
    overflow: visible;
  }

  .api-table {
    min-width: 0;
  }

  .api-table thead {
    display: none;
  }

  .api-table tbody,
  .api-table tr {
    display: block;
  }

  .api-table tr {
    display: grid;
    grid-template-columns: 58px minmax(0, 1fr) 42px;
    gap: 7px 10px;
    padding: 18px 2px;
    border-bottom: 1px solid #e2e6ea;
  }

  .api-table td {
    padding: 0;
    border: 0;
  }

  .api-table td:nth-child(1) {
    grid-row: 1 / 3;
  }

  .api-table td:nth-child(2) {
    grid-column: 2;
  }

  .api-table td:nth-child(3) {
    grid-column: 2 / 4;
  }

  .api-table td:nth-child(4) {
    grid-column: 3;
    grid-row: 1;
    text-align: right;
  }
}
</style>
