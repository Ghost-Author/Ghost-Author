<template>
  <section class="space-home">
    <div class="space-home-head">
      <div>
        <h2>Space Home</h2>
        <p class="section-subtitle">从空间首页快速进入常用页面</p>
      </div>
      <button @click="$emit('create')">+ 新建页面</button>
    </div>

    <div class="home-stats">
      <article>
        <strong>{{ stats.total }}</strong>
        <span>总页面</span>
      </article>
      <article>
        <strong>{{ stats.published }}</strong>
        <span>已发布</span>
      </article>
      <article>
        <strong>{{ stats.draft }}</strong>
        <span>草稿</span>
      </article>
      <article>
        <strong>{{ stats.privateCount }}</strong>
        <span>私有页面</span>
      </article>
      <article>
        <strong>{{ stats.archived }}</strong>
        <span>已归档</span>
      </article>
    </div>

    <div class="home-sections">
      <div class="home-card">
        <h3>最近访问</h3>
        <ul>
          <li v-for="doc in recentDocs" :key="`recent-${doc.slug}`" @click="$emit('select', doc.slug)">
            <strong>{{ doc.title }}</strong>
            <span>{{ doc.slug }}</span>
          </li>
          <li v-if="recentDocs.length === 0" class="home-empty">暂无最近访问</li>
        </ul>
      </div>

      <div class="home-card">
        <h3>收藏页面</h3>
        <ul>
          <li v-for="doc in favoriteDocs" :key="`fav-${doc.slug}`" @click="$emit('select', doc.slug)">
            <strong>{{ doc.title }}</strong>
            <span>{{ doc.slug }}</span>
          </li>
          <li v-if="favoriteDocs.length === 0" class="home-empty">暂无收藏页面</li>
        </ul>
      </div>
    </div>

    <div class="home-sections">
      <div class="home-card overdue">
        <h3>逾期页面</h3>
        <ul>
          <li v-for="doc in overdueDocs" :key="`overdue-${doc.slug}`" @click="$emit('select', doc.slug)">
            <strong>{{ doc.title }}</strong>
            <span>截止 {{ doc.dueDate || '-' }} · {{ doc.assignee || '未分配' }}</span>
          </li>
          <li v-if="overdueDocs.length === 0" class="home-empty">暂无逾期页面</li>
        </ul>
      </div>

      <div class="home-card today">
        <h3>今日到期</h3>
        <ul>
          <li v-for="doc in todayDocs" :key="`today-${doc.slug}`" @click="$emit('select', doc.slug)">
            <strong>{{ doc.title }}</strong>
            <span>{{ doc.assignee || '未分配' }} · {{ priorityText(doc.priority) }}</span>
          </li>
          <li v-if="todayDocs.length === 0" class="home-empty">今日没有到期页面</li>
        </ul>
      </div>
    </div>

    <div class="home-card assignee">
      <h3>负责人工作台</h3>
      <ul>
        <li v-for="row in assigneeBoard" :key="row.assignee">
          <strong>{{ row.assignee }}</strong>
          <span>总 {{ row.total }} · 逾期 {{ row.overdue }} · 今日 {{ row.today }}</span>
        </li>
        <li v-if="assigneeBoard.length === 0" class="home-empty">暂无负责人数据</li>
      </ul>
    </div>

    <div class="home-card my-todo">
      <div class="my-todo-head">
        <h3>我的待办</h3>
        <button class="secondary small" @click="$emit('open-my-todo')">切换到待办视图</button>
      </div>
      <ul>
        <li v-for="doc in myTodoDocs" :key="`my-${doc.slug}`" @click="$emit('select', doc.slug)">
          <strong>{{ doc.title }}</strong>
          <span>截止 {{ doc.dueDate || '-' }} · {{ priorityText(doc.priority) }}</span>
        </li>
        <li v-if="myTodoDocs.length === 0" class="home-empty">当前用户暂无待办页面</li>
      </ul>
    </div>
  </section>
</template>

<script setup>
defineProps({
  stats: {
    type: Object,
    default: () => ({ total: 0, published: 0, draft: 0, privateCount: 0, archived: 0 })
  },
  recentDocs: {
    type: Array,
    default: () => []
  },
  favoriteDocs: {
    type: Array,
    default: () => []
  },
  overdueDocs: {
    type: Array,
    default: () => []
  },
  todayDocs: {
    type: Array,
    default: () => []
  },
  assigneeBoard: {
    type: Array,
    default: () => []
  },
  myTodoDocs: {
    type: Array,
    default: () => []
  }
})

function priorityText(priority) {
  if (priority === 'HIGH') {
    return '高优先'
  }
  if (priority === 'LOW') {
    return '低优先'
  }
  return '中优先'
}
</script>
