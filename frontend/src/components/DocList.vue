<template>
  <div class="doc-list">
    <div class="header">
      <div>
        <h2>Space Pages</h2>
        <p class="section-subtitle">像 Confluence 一样管理文档</p>
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
            :class="{ active: activeSlug === node.slug }"
            :style="{ paddingLeft: `${10 + node.depth * 18}px` }"
            @click="$emit('select', node.slug)"
          >
            <strong>{{ node.title }}</strong>
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

const props = defineProps({
  docs: {
    type: Array,
    default: () => []
  },
  activeSlug: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['search'])

const groupedDocs = computed(() => {
  const docsBySlug = new Map(props.docs.map((doc) => [doc.slug, doc]))
  const childrenByParent = new Map()

  props.docs.forEach((doc) => {
    const parent = doc.parentSlug || '__root__'
    if (!childrenByParent.has(parent)) {
      childrenByParent.set(parent, [])
    }
    childrenByParent.get(parent).push(doc)
  })

  const roots = props.docs.filter((doc) => !doc.parentSlug || !docsBySlug.has(doc.parentSlug))
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
</script>
