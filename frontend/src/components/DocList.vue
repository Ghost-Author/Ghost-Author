<template>
  <div class="doc-list">
    <div class="header">
      <div>
        <h2>Space Pages</h2>
      </div>
      <div class="header-actions">
        <button class="secondary tiny" :class="{ active: batchMode }" @click="toggleBatchMode">
          {{ batchMode ? '退出批量' : '批量操作' }}
        </button>
        <span class="batch-count" v-if="batchMode">已选 {{ selectedSlugs.length }}</span>
        <button class="secondary tiny" v-if="batchMode" :class="{ active: selectedOnlyMode }" @click="selectedOnlyMode = !selectedOnlyMode">
          仅看已选
        </button>
        <button class="secondary tiny" v-if="batchMode" @click="selectAllVisible">全选</button>
        <button class="secondary tiny" v-if="batchMode" @click="selectByStatus('DRAFT')">选草稿</button>
        <button class="secondary tiny" v-if="batchMode" @click="selectByStatus('PUBLISHED')">选已发布</button>
        <button class="secondary tiny" v-if="batchMode" @click="selectByStatus('ARCHIVED')">选已归档</button>
        <button class="secondary tiny" v-if="batchMode" @click="invertSelection">反选</button>
        <button class="secondary tiny" v-if="batchMode" @click="clearSelected">清空</button>
        <button class="secondary tiny" v-if="batchMode" :disabled="selectedSlugs.length === 0" @click="emitBulkAction('BULK_MOVE_ROOT')">移到顶级</button>
        <button class="secondary tiny" v-if="batchMode" :disabled="selectedSlugs.length === 0" @click="emitBulkAction('BULK_ARCHIVE')">归档</button>
        <button class="secondary tiny" v-if="batchMode" :disabled="selectedSlugs.length === 0" @click="emitBulkAction('BULK_UNARCHIVE')">恢复</button>
        <button class="secondary tiny" v-if="batchMode" :disabled="selectedSlugs.length === 0" @click="emitBulkAction('BULK_FAVORITE')">收藏</button>
        <button class="secondary tiny" v-if="batchMode" :disabled="selectedSlugs.length === 0" @click="emitBulkAction('BULK_UNFAVORITE')">取消收藏</button>
        <button class="secondary tiny" @click="expandAll">展开</button>
        <button class="secondary tiny" @click="collapseAll">折叠</button>
        <button @click="$emit('create')">+ 新建</button>
      </div>
    </div>

    <div class="doc-list-scroll" @click="quickMenuSlug = ''">
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

      <div class="filter-panel">
        <button class="filter-panel-head" @click="filtersOpen = !filtersOpen">
          <strong>筛选器</strong>
          <span>{{ filtersOpen ? '收起 ▾' : '展开 ▸' }}</span>
        </button>
        <div v-show="filtersOpen">
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
          <button
            class="filter-btn"
            :class="{ active: statusFilter === 'ARCHIVED' }"
            @click="statusFilter = 'ARCHIVED'"
          >
            已归档 {{ statusCounts.ARCHIVED }}
          </button>
          </div>

          <div class="visibility-filters">
          <button
            class="filter-btn"
            :class="{ active: visibilityFilter === 'ALL' }"
            @click="visibilityFilter = 'ALL'"
          >
            全可见性 {{ visibilityCounts.ALL }}
          </button>
          <button
            class="filter-btn"
            :class="{ active: visibilityFilter === 'SPACE' }"
            @click="visibilityFilter = 'SPACE'"
          >
            空间可见 {{ visibilityCounts.SPACE }}
          </button>
          <button
            class="filter-btn"
            :class="{ active: visibilityFilter === 'PRIVATE' }"
            @click="visibilityFilter = 'PRIVATE'"
          >
            私有 {{ visibilityCounts.PRIVATE }}
          </button>
          </div>

          <div class="priority-filters">
          <button class="filter-btn" :class="{ active: priorityFilter === 'ALL' }" @click="priorityFilter = 'ALL'">
            优先级全部
          </button>
          <button class="filter-btn" :class="{ active: priorityFilter === 'HIGH' }" @click="priorityFilter = 'HIGH'">
            高优先级
          </button>
          <button class="filter-btn" :class="{ active: priorityFilter === 'MEDIUM' }" @click="priorityFilter = 'MEDIUM'">
            中优先级
          </button>
          <button class="filter-btn" :class="{ active: priorityFilter === 'LOW' }" @click="priorityFilter = 'LOW'">
            低优先级
          </button>
          </div>

          <div class="meta-filters">
          <select v-model="assigneeFilter">
            <option value="">负责人（全部）</option>
            <option v-for="name in assigneeOptions" :key="name" :value="name">{{ name }}</option>
          </select>
          <select v-model="dueFilter">
            <option value="ALL">截止日期（全部）</option>
            <option value="HAS_DUE">有截止日期</option>
            <option value="OVERDUE">已逾期</option>
          </select>
          </div>
          <div class="todo-toggle-row">
            <button class="filter-btn" :class="{ active: myTodoMode }" @click="toggleMyTodoMode">
              我的待办视图
            </button>
          </div>
        </div>
      </div>

      <div class="quick-zones">
      <div class="quick-zone">
        <button class="quick-zone-head" @click="quickOpenFavorites = !quickOpenFavorites">
          <h4>⭐ 收藏</h4>
          <span>{{ quickOpenFavorites ? '▾' : '▸' }}</span>
        </button>
        <ul class="quick-list" v-show="quickOpenFavorites">
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
        <button class="quick-zone-head" @click="quickOpenRecent = !quickOpenRecent">
          <h4>🕘 最近访问</h4>
          <span>{{ quickOpenRecent ? '▾' : '▸' }}</span>
        </button>
        <ul class="quick-list" v-show="quickOpenRecent">
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
        class="root-drop-zone"
        :class="{ active: dropTargetRoot }"
        @dragover.prevent="onRootDragOver"
        @dragleave="onRootDragLeave"
        @drop.prevent="onDropRoot"
      >
        拖拽到这里设为顶级页面
      </div>

      <div v-for="section in visibilitySections" :key="section.key" class="tree-section">
        <div class="tree-section-title">{{ section.title }}</div>
        <div
          v-for="group in section.groups"
          :key="group.id"
          class="tree-group"
        >
          <button class="tree-group-head" @click="toggleGroup(group.id)">
            <span>{{ opened[group.id] ? '▾' : '▸' }} {{ group.name }}</span>
            <em>{{ group.items.length }}</em>
          </button>

          <ul class="doc-items" v-show="opened[group.id]">
            <li
              v-for="node in group.items"
              :key="node.slug"
              :data-node-slug="node.slug"
              :class="[
                'tree-node',
                depthClass(node.depth),
                {
                  active: activeSlug === node.slug,
                  ancestor: activeAncestorSet.has(node.slug) && activeSlug !== node.slug,
                  'drag-target': dropTargetSlug === node.slug
                }
              ]"
              :style="{ paddingLeft: `${10 + node.depth * 22}px` }"
              draggable="true"
              @dragstart="onDragStart(node.slug)"
              @dragend="onDragEnd"
              @dragover.prevent="onDragOver(node.slug)"
              @dragleave="onDragLeave(node.slug)"
              @drop.prevent="onDropNode(node.slug)"
              @click="onNodeClick(node.slug)"
            >
              <div class="node-title-row">
                <div class="node-title-main">
                  <input
                    v-if="batchMode"
                    type="checkbox"
                    class="node-check"
                    :checked="selectedSlugs.includes(node.slug)"
                    @click.stop="toggleSelected(node.slug)"
                  />
                  <span class="node-branch" v-if="node.depth > 0">└</span>
                  <span class="node-depth-pill" v-if="node.depth > 0">L{{ node.depth }}</span>
                  <strong>{{ node.title }}</strong>
                </div>
                <div class="node-more">
                  <button class="node-more-btn" @click.stop="toggleQuickMenu(node.slug)">⋯</button>
                  <div v-if="quickMenuSlug === node.slug" class="node-menu" @click.stop>
                    <button class="node-menu-item" @click="emitQuickAction('OPEN_PAGE', node.slug)">
                      打开页面
                    </button>
                    <button class="node-menu-item" @click="emitQuickAction('RENAME', node.slug)">
                      重命名标题
                    </button>
                    <button class="node-menu-item" @click="emitQuickAction('MOVE_ROOT', node.slug)">
                      设为顶级页面
                    </button>
                    <button
                      class="node-menu-item"
                      @click="emitQuickAction((node.status || 'DRAFT') === 'ARCHIVED' ? 'UNARCHIVE' : 'ARCHIVE', node.slug)"
                    >
                      {{ (node.status || 'DRAFT') === 'ARCHIVED' ? '恢复为草稿' : '归档页面' }}
                    </button>
                    <button class="node-menu-item" @click="emitQuickAction('COPY_LINK', node.slug)">
                      复制页面链接
                    </button>
                    <button class="node-menu-item" @click="emitQuickAction('COPY_SLUG', node.slug)">
                      复制 slug
                    </button>
                    <button class="node-menu-item" @click="emitQuickAction('TOGGLE_FAVORITE', node.slug)">
                      {{ favorites.includes(node.slug) ? '取消收藏' : '加入收藏' }}
                    </button>
                  </div>
                </div>
                <button
                  class="fav-toggle"
                  :class="{ active: favorites.includes(node.slug) }"
                  @click.stop="$emit('toggle-favorite', node.slug)"
                >
                  {{ favorites.includes(node.slug) ? '★' : '☆' }}
                </button>
                <div class="order-controls">
                  <button class="order-btn" @click.stop="emit('reorder', { slug: node.slug, direction: 'UP' })">↑</button>
                  <button class="order-btn" @click.stop="emit('reorder', { slug: node.slug, direction: 'DOWN' })">↓</button>
                </div>
              </div>
              <div class="node-meta-row">
                <span class="node-slug">{{ node.slug }}</span>
                <span class="node-visibility" :class="(node.visibility || 'SPACE').toLowerCase()">
                  {{ node.visibility === 'PRIVATE' ? '私有' : '空间' }}
                </span>
                <span class="node-lock" :class="{ locked: !!node.locked }">
                  {{ node.locked ? '锁定' : '可编辑' }}
                </span>
                <span class="node-status" :class="(node.status || 'DRAFT').toLowerCase()">
                  {{ statusText(node.status) }}
                </span>
                <span class="node-priority" :class="(node.priority || 'MEDIUM').toLowerCase()">
                  {{ priorityText(node.priority) }}
                </span>
              </div>
              <p>{{ node.summary }}</p>
              <div class="label-row">
                <span class="node-owner">👤 {{ node.assignee || '-' }}</span>
                <span class="node-owner">⏰ {{ node.dueDate || '-' }}</span>
              </div>
              <div class="label-row" v-if="node.labels && node.labels.length">
                <span class="doc-label" v-for="label in node.labels.slice(0, 3)" :key="label">{{ label }}</span>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'

const QUICK_PANEL_KEY = 'ga-sidebar-quick-panels'
const FILTER_PANEL_KEY = 'ga-sidebar-filter-panel'
const GROUP_OPEN_KEY = 'ga-sidebar-open-groups'

function loadQuickPanelsState() {
  if (typeof window === 'undefined') {
    return { favorites: true, recent: true }
  }
  try {
    const raw = window.localStorage.getItem(QUICK_PANEL_KEY)
    if (!raw) {
      return { favorites: true, recent: true }
    }
    const parsed = JSON.parse(raw)
    return {
      favorites: parsed.favorites !== false,
      recent: parsed.recent !== false
    }
  } catch {
    return { favorites: true, recent: true }
  }
}

function persistQuickPanelsState(favoritesOpen, recentOpen) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(QUICK_PANEL_KEY, JSON.stringify({
    favorites: favoritesOpen,
    recent: recentOpen
  }))
}

function loadFilterPanelState() {
  if (typeof window === 'undefined') {
    return true
  }
  const raw = window.localStorage.getItem(FILTER_PANEL_KEY)
  if (raw === null) {
    return true
  }
  return raw !== '0'
}

function persistFilterPanelState(open) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(FILTER_PANEL_KEY, open ? '1' : '0')
}

function loadGroupOpenState() {
  if (typeof window === 'undefined') {
    return {}
  }
  try {
    const raw = window.localStorage.getItem(GROUP_OPEN_KEY)
    if (!raw) {
      return {}
    }
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') {
      return {}
    }
    return parsed
  } catch {
    return {}
  }
}

function persistGroupOpenState(state) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(GROUP_OPEN_KEY, JSON.stringify(state))
}

const keyword = ref('')
const opened = ref(loadGroupOpenState())
const statusFilter = ref('ALL')
const visibilityFilter = ref('ALL')
const priorityFilter = ref('ALL')
const assigneeFilter = ref('')
const dueFilter = ref('ALL')
const myTodoMode = ref(false)
const filtersOpen = ref(loadFilterPanelState())
const quickPanelsState = loadQuickPanelsState()
const quickOpenFavorites = ref(quickPanelsState.favorites)
const quickOpenRecent = ref(quickPanelsState.recent)
const draggingSlug = ref('')
const dropTargetSlug = ref('')
const dropTargetRoot = ref(false)
const quickMenuSlug = ref('')
const batchMode = ref(false)
const selectedSlugs = ref([])
const selectedOnlyMode = ref(false)

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
  },
  currentUser: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['search', 'toggle-favorite', 'move', 'reorder', 'quick-action', 'bulk-action'])

watch([quickOpenFavorites, quickOpenRecent], ([favoritesOpen, recentOpen]) => {
  persistQuickPanelsState(favoritesOpen, recentOpen)
})

watch(filtersOpen, (open) => {
  persistFilterPanelState(open)
})

watch(opened, (value) => {
  persistGroupOpenState(value)
}, { deep: true })

watch(() => props.docs, () => {
  const valid = new Set(props.docs.map((doc) => doc.slug))
  selectedSlugs.value = selectedSlugs.value.filter((slug) => valid.has(slug))
}, { deep: true })

watch(selectedSlugs, (list) => {
  if (list.length === 0) {
    selectedOnlyMode.value = false
  }
})

const statusFilteredDocs = computed(() => {
  if (statusFilter.value === 'ALL') {
    return props.docs
  }
  return props.docs.filter((doc) => (doc.status || 'DRAFT') === statusFilter.value)
})

const propertyFilteredDocs = computed(() => {
  const selectedSet = new Set(selectedSlugs.value)
  return statusFilteredDocs.value.filter((doc) => {
    const priorityPass = priorityFilter.value === 'ALL' || (doc.priority || 'MEDIUM') === priorityFilter.value
    const assigneePass = !assigneeFilter.value || (doc.assignee || '') === assigneeFilter.value
    let duePass = true
    if (dueFilter.value === 'HAS_DUE') {
      duePass = !!doc.dueDate
    } else if (dueFilter.value === 'OVERDUE') {
      duePass = !!doc.dueDate && doc.dueDate < new Date().toISOString().slice(0, 10)
    }
    const todoPass = !myTodoMode.value || ((doc.assignee || '') === assigneeFilter.value && (doc.status || 'DRAFT') !== 'ARCHIVED')
    const selectedPass = !selectedOnlyMode.value || selectedSet.has(doc.slug)
    return priorityPass && assigneePass && duePass && todoPass && selectedPass
  })
})

const assigneeOptions = computed(() => {
  return Array.from(new Set(
    props.docs
      .map((doc) => (doc.assignee || '').trim())
      .filter((name) => name.length > 0)
  )).sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
})

const statusCounts = computed(() => {
  const published = props.docs.filter((doc) => (doc.status || 'DRAFT') === 'PUBLISHED').length
  const archived = props.docs.filter((doc) => (doc.status || 'DRAFT') === 'ARCHIVED').length
  const draft = props.docs.length - published - archived
  return {
    ALL: props.docs.length,
    PUBLISHED: published,
    DRAFT: draft,
    ARCHIVED: archived
  }
})

const visibilityCounts = computed(() => {
  const space = props.docs.filter((doc) => (doc.visibility || 'SPACE') === 'SPACE').length
  const privateCount = props.docs.length - space
  return {
    ALL: props.docs.length,
    SPACE: space,
    PRIVATE: privateCount
  }
})

const visibilitySections = computed(() => {
  if (visibilityFilter.value === 'ALL') {
    return [
      {
        key: 'SPACE',
        title: '空间页面',
        groups: buildGroups(propertyFilteredDocs.value.filter((doc) => (doc.visibility || 'SPACE') === 'SPACE'), 'SPACE')
      },
      {
        key: 'PRIVATE',
        title: '私有页面',
        groups: buildGroups(propertyFilteredDocs.value.filter((doc) => (doc.visibility || 'SPACE') === 'PRIVATE'), 'PRIVATE')
      }
    ].filter((section) => section.groups.length > 0)
  }

  const sourceDocs = propertyFilteredDocs.value.filter((doc) => (doc.visibility || 'SPACE') === visibilityFilter.value)
  const title = visibilityFilter.value === 'PRIVATE' ? '私有页面' : '空间页面'
  return [{
    key: visibilityFilter.value,
    title,
    groups: buildGroups(sourceDocs, visibilityFilter.value)
  }]
})

watch(() => props.activeSlug, async (slug) => {
  if (!slug) {
    return
  }
  visibilitySections.value.forEach((section) => {
    section.groups.forEach((group) => {
      if (group.items.some((item) => item.slug === slug)) {
        opened.value[group.id] = true
      }
    })
  })
  await nextTick()
  const node = document.querySelector(`[data-node-slug="${slug}"]`)
  if (node && typeof node.scrollIntoView === 'function') {
    node.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }
}, { immediate: true })

const activeAncestorSet = computed(() => {
  if (!props.activeSlug) {
    return new Set()
  }
  const bySlug = new Map(props.docs.map((doc) => [doc.slug, doc]))
  const result = new Set()
  let cursor = bySlug.get(props.activeSlug)
  const visited = new Set()
  while (cursor && cursor.parentSlug && !visited.has(cursor.parentSlug)) {
    visited.add(cursor.parentSlug)
    result.add(cursor.parentSlug)
    cursor = bySlug.get(cursor.parentSlug)
  }
  return result
})

function buildGroups(sourceDocs, sectionKey) {
  const docsBySlug = new Map(sourceDocs.map((doc) => [doc.slug, doc]))
  const childrenByParent = new Map()

  sourceDocs.forEach((doc) => {
    const parent = doc.parentSlug || '__root__'
    if (!childrenByParent.has(parent)) {
      childrenByParent.set(parent, [])
    }
    childrenByParent.get(parent).push(doc)
  })

  const roots = sourceDocs
    .filter((doc) => !doc.parentSlug || !docsBySlug.has(doc.parentSlug))
    .slice()
    .sort(sortByOrder)
  const map = new Map()

  roots.forEach((root) => {
    const groupName = resolveGroup(root.slug)
    const groupId = `${sectionKey}:${groupName}`
    if (!map.has(groupId)) {
      map.set(groupId, { name: groupName, items: [] })
      if (opened.value[groupId] === undefined) {
        opened.value[groupId] = true
      }
    }
    map.get(groupId).items.push(...flattenTree(root, childrenByParent, 0))
  })

  return Array.from(map.entries())
    .map(([id, group]) => ({
      id,
      name: group.name,
      items: group.items
    }))
    .sort((a, b) => a.name.localeCompare(b.name, 'zh-Hans-CN'))
}

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
    .sort(sortByOrder)

  children.forEach((child) => {
    result.push(...flattenTree(child, childrenByParent, depth + 1))
  })

  return result
}

function clearSearch() {
  keyword.value = ''
  emit('search', '')
}

function toggleQuickMenu(slug) {
  quickMenuSlug.value = quickMenuSlug.value === slug ? '' : slug
}

function emitQuickAction(action, slug) {
  quickMenuSlug.value = ''
  emit('quick-action', { action, slug })
}

function toggleBatchMode() {
  batchMode.value = !batchMode.value
  quickMenuSlug.value = ''
  if (!batchMode.value) {
    selectedSlugs.value = []
    selectedOnlyMode.value = false
  }
}

function toggleSelected(slug) {
  if (selectedSlugs.value.includes(slug)) {
    selectedSlugs.value = selectedSlugs.value.filter((item) => item !== slug)
  } else {
    selectedSlugs.value = [...selectedSlugs.value, slug]
  }
}

function onNodeClick(slug) {
  if (batchMode.value) {
    toggleSelected(slug)
    return
  }
  emit('select', slug)
}

function emitBulkAction(action) {
  emit('bulk-action', {
    action,
    slugs: [...selectedSlugs.value]
  })
}

function selectAllVisible() {
  const all = visibilitySections.value
    .flatMap((section) => section.groups)
    .flatMap((group) => group.items)
    .map((item) => item.slug)
  selectedSlugs.value = Array.from(new Set(all))
}

function clearSelected() {
  selectedSlugs.value = []
}

function invertSelection() {
  const all = visibilitySections.value
    .flatMap((section) => section.groups)
    .flatMap((group) => group.items)
    .map((item) => item.slug)
  const selected = new Set(selectedSlugs.value)
  selectedSlugs.value = all.filter((slug) => !selected.has(slug))
}

function selectByStatus(status) {
  const all = visibilitySections.value
    .flatMap((section) => section.groups)
    .flatMap((group) => group.items)
  selectedSlugs.value = all
    .filter((item) => (item.status || 'DRAFT') === status)
    .map((item) => item.slug)
}

function clearBatchSelection() {
  selectedSlugs.value = []
  selectedOnlyMode.value = false
}

function toggleMyTodoMode() {
  if (!myTodoMode.value && !assigneeFilter.value) {
    assigneeFilter.value = props.currentUser || ''
  }
  myTodoMode.value = !myTodoMode.value
}

function setMyTodoFilter(userName) {
  assigneeFilter.value = userName || ''
  dueFilter.value = 'ALL'
  priorityFilter.value = 'ALL'
  myTodoMode.value = true
}

function clearMyTodoFilter() {
  myTodoMode.value = false
}

function toggleGroup(name) {
  opened.value[name] = !opened.value[name]
}

function depthClass(depth) {
  return `depth-${Math.min(depth, 4)}`
}

function statusText(status) {
  if (status === 'PUBLISHED') {
    return '已发布'
  }
  if (status === 'ARCHIVED') {
    return '已归档'
  }
  return '草稿'
}

function priorityText(priority) {
  if (priority === 'HIGH') {
    return '高优先'
  }
  if (priority === 'LOW') {
    return '低优先'
  }
  return '中优先'
}

function sortByOrder(a, b) {
  const orderA = Number.isFinite(a.sortOrder) ? a.sortOrder : 0
  const orderB = Number.isFinite(b.sortOrder) ? b.sortOrder : 0
  if (orderA !== orderB) {
    return orderA - orderB
  }
  return (a.title || '').localeCompare(b.title || '', 'zh-Hans-CN')
}

function expandAll() {
  visibilitySections.value.forEach((section) => {
    section.groups.forEach((group) => {
      opened.value[group.id] = true
    })
  })
}

function collapseAll() {
  visibilitySections.value.forEach((section) => {
    section.groups.forEach((group) => {
      opened.value[group.id] = false
    })
  })
}

function onDragStart(slug) {
  draggingSlug.value = slug
}

function onDragEnd() {
  draggingSlug.value = ''
  dropTargetSlug.value = ''
  dropTargetRoot.value = false
}

function onDragOver(targetSlug) {
  if (!draggingSlug.value || draggingSlug.value === targetSlug) {
    return
  }
  dropTargetRoot.value = false
  dropTargetSlug.value = targetSlug
}

function onDragLeave(targetSlug) {
  if (dropTargetSlug.value === targetSlug) {
    dropTargetSlug.value = ''
  }
}

function onDropNode(targetSlug) {
  if (!draggingSlug.value || draggingSlug.value === targetSlug) {
    onDragEnd()
    return
  }
  emit('move', {
    slug: draggingSlug.value,
    parentSlug: targetSlug
  })
  onDragEnd()
}

function onRootDragOver() {
  if (!draggingSlug.value) {
    return
  }
  dropTargetSlug.value = ''
  dropTargetRoot.value = true
}

function onRootDragLeave() {
  dropTargetRoot.value = false
}

function onDropRoot() {
  if (!draggingSlug.value) {
    onDragEnd()
    return
  }
  emit('move', {
    slug: draggingSlug.value,
    parentSlug: null
  })
  onDragEnd()
}

defineExpose({
  setMyTodoFilter,
  clearMyTodoFilter,
  clearBatchSelection
})
</script>
