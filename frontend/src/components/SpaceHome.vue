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
  }
})
</script>
