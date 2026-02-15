<template>
  <div class="doc-list">
    <div class="header">
      <div>
        <h2>Space Pages</h2>
      </div>
      <button @click="$emit('create')">+ 新建</button>
    </div>

    <input
      class="search-input"
      v-model="keyword"
      placeholder="搜索文档"
      @keyup.enter="$emit('search', keyword)"
    />

    <div class="search-actions">
      <button class="search-btn" @click="$emit('search', keyword)">搜索</button>
      <button class="search-btn clear" @click="clearSearch">清空</button>
    </div>

    <div class="status-filters">
      <button
        class="filter-btn"
        :class="{ active: statusFilter === 'ALL' }"
        @click="statusFilter = 'ALL'"
      >
        全部 {{ statusCounts.ALL }}
      </button>
      <button
        class="filter-btn"
        :class="{ active: statusFilter === 'PUBLISHED' }"
        @click="statusFilter = 'PUBLISHED'"
      >
        已发布 {{ statusCounts.PUBLISHED }}
      </button>
      <button
        class="filter-btn"
        :class="{ active: statusFilter === 'DRAFT' }"
        @click="statusFilter = 'DRAFT'"
      >
        草稿 {{ statusCounts.DRAFT }}
      </button>
    </div>

    <div class="quick-zones">
      <div class="quick-zone">
        <h4>⭐ 收藏</h4>
        <ul class="quick-list">
          <li
            v-for="doc in favoriteDocs"
            :key="`fav-${doc.slug}`"
            :class="{ active: activeSlug === doc.slug }"
            @click="$emit('select', doc.slug)"
          >
            <span>{{ doc.title }}</span>
          </li>
          <li class="quick-empty" v-if="favoriteDocs.length === 0">还没有收藏页面</li>
        </ul>
      </div>

      <div class="quick-zone">
        <h4>🕘 最近访问</h4>
        <ul class="quick-list">
          <li
            v-for="doc in recentDocs"
            :key="`recent-${doc.slug}`"
            :class="{ active: activeSlug === doc.slug }"
            @click="$emit('select', doc.slug)"
          >
            <span>{{ doc.title }}</span>
          </li>
          <li class="quick-empty" v-if="recentDocs.length === 0">还没有访问记录</li>
        </ul>
      </div>
    </div>

    <div class="tree-nav">
      <div
        v-for="group in groupedDocs"
        :key="group.name"
        class="tree-group"
      >
        <button class="tree-group-head" @click="toggleGroup(group.name)">
          <span>{{ opened[group.name] ? '▾' : '▸' }} {{ group.name }}</span>
          <em>{{ group.items.length }}</em>
        </button>

        <ul class="doc-items" v-show="opened[group.name]">
          <li
            v-for="node in group.items"
            :key="node.slug"
            :class="['tree-node', depthClass(node.depth), { active: activeSlug === node.slug }]"
            :style="{ paddingLeft: `${10 + node.depth * 22}px` }"
            @click="$emit('select', node.slug)"
          >
            <div class="node-title-row">
              <div class="node-title-main">
                <span class="node-branch" v-if="node.depth > 0">└</span>
                <span class="node-depth-pill" v-if="node.depth > 0">L{{ node.depth }}</span>
                <strong>{{ node.title }}</strong>
              </div>
              <button
                class="fav-toggle"
                :class="{ active: favorites.includes(node.slug) }"
                @click.stop="$emit('toggle-favorite', node.slug)"
              >
                {{ favorites.includes(node.slug) ? '★' : '☆' }}
              </button>
            </div>
            <div class="node-meta-row">
              <span class="node-slug">{{ node.slug }}</span>
              <span class="node-status" :class="(node.status || 'DRAFT').toLowerCase()">
                {{ node.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </span>
            </div>
            <p>{{ node.summary }}</p>
            <div class="label-row" v-if="node.labels && node.labels.length">
              <span class="doc-label" v-for="label in node.labels.slice(0, 3)" :key="label">{{ label }}</span>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const keyword = ref('')
const opened = ref({})
const statusFilter = ref('ALL')

const props = defineProps({
  docs: {
    type: Array,
    default: () => []
  },
  activeSlug: {
    type: String,
    default: ''
  },
  favorites: {
    type: Array,
    default: () => []
  },
  recent: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['search', 'toggle-favorite'])

const filteredDocs = computed(() => {
  if (statusFilter.value === 'ALL') {
    return props.docs
  }
  return props.docs.filter((doc) => (doc.status || 'DRAFT') === statusFilter.value)
})

const statusCounts = computed(() => {
  const published = props.docs.filter((doc) => (doc.status || 'DRAFT') === 'PUBLISHED').length
  const draft = props.docs.length - published
  return {
    ALL: props.docs.length,
    PUBLISHED: published,
    DRAFT: draft
  }
})

const groupedDocs = computed(() => {
  const sourceDocs = filteredDocs.value
  const docsBySlug = new Map(sourceDocs.map((doc) => [doc.slug, doc]))
  const childrenByParent = new Map()

  sourceDocs.forEach((doc) => {
    const parent = doc.parentSlug || '__root__'
    if (!childrenByParent.has(parent)) {
      childrenByParent.set(parent, [])
    }
    childrenByParent.get(parent).push(doc)
  })

  const roots = sourceDocs.filter((doc) => !doc.parentSlug || !docsBySlug.has(doc.parentSlug))
  const map = new Map()

  roots.forEach((root) => {
    const groupName = resolveGroup(root.slug)
    if (!map.has(groupName)) {
      map.set(groupName, [])
      if (opened.value[groupName] === undefined) {
        opened.value[groupName] = true
      }
    }
    map.get(groupName).push(...flattenTree(root, childrenByParent, 0))
  })

  return Array.from(map.entries())
    .map(([name, items]) => ({
      name,
      items
    }))
    .sort((a, b) => a.name.localeCompare(b.name, 'zh-Hans-CN'))
})

const favoriteDocs = computed(() => {
  const bySlug = new Map(props.docs.map((d) => [d.slug, d]))
  return props.favorites
    .map((slug) => bySlug.get(slug))
    .filter(Boolean)
})

const recentDocs = computed(() => {
  const bySlug = new Map(props.docs.map((d) => [d.slug, d]))
  return props.recent
    .map((slug) => bySlug.get(slug))
    .filter(Boolean)
})

function resolveGroup(slug) {
  if (!slug) {
    return 'General'
  }
  if (slug.includes('/')) {
    return slug.split('/')[0]
  }
  if (slug.includes('-')) {
    return slug.split('-')[0]
  }
  return 'General'
}

function flattenTree(node, childrenByParent, depth) {
  const result = [{
    ...node,
    depth
  }]

  const children = (childrenByParent.get(node.slug) || [])
    .slice()
    .sort((a, b) => a.title.localeCompare(b.title, 'zh-Hans-CN'))

  children.forEach((child) => {
    result.push(...flattenTree(child, childrenByParent, depth + 1))
  })

  return result
}

function clearSearch() {
  keyword.value = ''
  emit('search', '')
}

function toggleGroup(name) {
  opened.value[name] = !opened.value[name]
}

function depthClass(depth) {
  return `depth-${Math.min(depth, 4)}`
}
</script>
