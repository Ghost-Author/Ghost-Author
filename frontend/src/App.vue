<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">GA</span>
        <div>
          <strong>Knowledge Space</strong>
        </div>
      </div>
      <div class="topbar-right">
        <input v-model="currentUser" class="user-input" placeholder="当前用户（如 liupeng）" />
        <span class="shortcut-hint">⌘/Ctrl+K 搜索 · ⌘/Ctrl+S 保存 · Alt+1/2/3/4</span>
        <button class="secondary tiny" @click="openHome">空间首页</button>
        <button class="secondary tiny" @click="toggleRightPanel">
          {{ rightPanelOpen ? '收起右栏' : '展开右栏' }}
        </button>
        <button class="secondary tiny" :class="{ active: focusMode }" @click="toggleFocusMode">
          {{ focusMode ? '退出专注' : '专注模式' }}
        </button>
        <div class="topbar-badge">{{ visibleDocs.length }} pages</div>
      </div>
    </header>

    <div class="breadcrumb" v-show="!focusMode">
      <span>🏠 Space</span>
      <span>/</span>
      <span>Knowledge</span>
      <template v-for="item in breadcrumbPath" :key="item.slug || item.title">
        <span>/</span>
        <strong v-if="item.slug === activeSlug">{{ item.title }}</strong>
        <button v-else class="crumb-link" @click="openBreadcrumb(item.slug)">{{ item.title }}</button>
      </template>
    </div>

    <div class="layout" :class="{ 'right-collapsed': !rightPanelOpen, 'focus-mode': focusMode }" :style="layoutStyle">
      <DocList
        class="layout-doclist"
        ref="docListRef"
        :docs="visibleDocs"
        :active-slug="activeSlug"
        :favorites="favorites"
        :recent="recent"
        :current-user="currentUser"
        @create="createNewDoc"
        @search="searchDocs"
        @select="loadDoc"
        @toggle-favorite="toggleFavorite"
        @move="moveDoc"
        @reorder="reorderDoc"
        @quick-action="handleDocQuickAction"
        @bulk-action="handleDocBulkAction"
      />
      <div
        class="layout-splitter"
        role="separator"
        aria-label="调整左侧栏宽度"
        @mousedown="startLeftResize"
      ></div>

      <div class="layout-main">
        <SpaceHome
          v-if="showHome"
          :stats="homeStats"
          :recent-docs="homeRecentDocs"
          :favorite-docs="homeFavoriteDocs"
          :overdue-docs="homeOverdueDocs"
          :today-docs="homeTodayDueDocs"
          :assignee-board="homeAssigneeBoard"
          :my-todo-docs="myTodoDocs"
          @create="createNewDoc"
          @select="loadDoc"
          @open-my-todo="openMyTodoView"
        />
        <EditorPane
          v-else
          :doc="currentDoc"
          :is-favorite="!!activeSlug && favorites.includes(activeSlug)"
          :prev-sibling-slug="siblingNav.prevSlug"
          :prev-sibling-title="siblingNav.prevTitle"
          :next-sibling-slug="siblingNav.nextSlug"
          :next-sibling-title="siblingNav.nextTitle"
          :outline="pageOutline"
          :comments="comments"
          :attachments="attachments"
          :child-pages="childPages"
          :current-user="currentUser"
          :can-edit="currentCanEdit"
          :share-link="currentShareLink"
          :templates="templates"
          @save="saveDoc"
          @delete="deleteDoc"
          @add-comment="addComment"
          @delete-comment="deleteComment"
          @upload-attachment="uploadAttachment"
          @delete-attachment="deleteAttachment"
          @insert-attachment="insertAttachment"
          @create-child="createChildPage"
          @create-child-with-template="createChildPageWithTemplate"
          @select-child="loadDoc"
          @open-parent="loadDoc"
          @open-sibling="loadDoc"
          @toggle-favorite="toggleFavorite"
          @toggle-share="toggleShare"
          @regenerate-share="regenerateShare"
          @create-template="createTemplate"
          @update-template="updateTemplate"
          @delete-template="deleteTemplate"
          @notify="handleEditorNotify"
        />
      </div>

      <VersionHistory
        v-if="rightPanelOpen"
        class="layout-version"
        :slug="activeSlug"
        :versions="versions"
        :outline="pageOutline"
        :diff-from="diffFrom"
        :diff-to="diffTo"
        :diff-text="diffText"
        @refresh="refreshVersions"
        @restore="restoreVersion"
        @pick-left="pickDiffLeft"
        @pick-right="pickDiffRight"
        @diff="buildDiff"
      />
    </div>

    <div class="command-overlay" v-if="commandOpen" @click.self="closeCommand">
      <div class="command-panel">
        <div class="command-head">
          <input
            v-model="commandQuery"
            placeholder="搜索页面标题或 slug，回车快速打开"
            @keydown="onCommandInputKeydown"
          />
        </div>
        <ul class="command-list">
          <li
            v-for="(item, idx) in commandResults"
            :key="item.slug"
            :class="{ active: idx === commandActiveIndex }"
            @mouseenter="commandActiveIndex = idx"
            @click="selectCommandDoc(item.slug)"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.slug }}</span>
          </li>
          <li v-if="commandResults.length === 0" class="command-empty">没有匹配页面</li>
        </ul>
      </div>
    </div>

    <div v-if="toast.show" class="app-toast" :class="toast.type">
      {{ toast.message }}
    </div>

    <div v-if="confirmDialog.open" class="confirm-overlay" @click.self="resolveConfirm(false)">
      <div class="confirm-panel">
        <h4>{{ confirmDialog.title }}</h4>
        <p>{{ confirmDialog.message }}</p>
        <div class="confirm-actions">
          <button class="secondary" @click="resolveConfirm(false)">{{ confirmDialog.cancelText }}</button>
          <button :class="confirmDialog.danger ? 'danger' : ''" @click="resolveConfirm(true)">
            {{ confirmDialog.confirmText }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="promptDialog.open" class="confirm-overlay" @click.self="resolvePrompt(null)">
      <div class="confirm-panel">
        <h4>{{ promptDialog.title }}</h4>
        <p>{{ promptDialog.message }}</p>
        <input
          ref="promptInputRef"
          v-model="promptDialog.value"
          :placeholder="promptDialog.placeholder"
          @keydown.enter.prevent="resolvePrompt(promptDialog.value)"
          @keydown.esc.prevent="resolvePrompt(null)"
        />
        <div class="confirm-actions">
          <button class="secondary" @click="resolvePrompt(null)">{{ promptDialog.cancelText }}</button>
          <button @click="resolvePrompt(promptDialog.value)">{{ promptDialog.confirmText }}</button>
        </div>
      </div>
    </div>

    <div v-if="bulkResult.open" class="confirm-overlay" @click.self="bulkResult.open = false">
      <div class="confirm-panel bulk-result-panel">
        <h4>批量操作结果</h4>
        <p>{{ bulkResult.actionLabel }}：成功 {{ bulkResult.updated.length }}，跳过 {{ bulkResult.skipped.length }}</p>
        <div class="bulk-result-columns">
          <div>
            <h5>成功</h5>
            <ul class="bulk-result-list">
              <li v-for="item in bulkResult.updated" :key="`ok-${item}`">{{ item }}</li>
              <li v-if="bulkResult.updated.length === 0" class="bulk-result-empty">无</li>
            </ul>
          </div>
          <div>
            <h5>跳过</h5>
            <ul class="bulk-result-list">
              <li v-for="item in bulkResult.skipped" :key="`skip-${item.slug}`">
                <strong>{{ item.slug }}</strong>
                <span>{{ item.reason }}</span>
              </li>
              <li v-if="bulkResult.skipped.length === 0" class="bulk-result-empty">无</li>
            </ul>
          </div>
        </div>
        <div class="confirm-actions">
          <button @click="bulkResult.open = false">我知道了</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { api } from './api/client'
import DocList from './components/DocList.vue'
import EditorPane from './components/EditorPane.vue'
import SpaceHome from './components/SpaceHome.vue'
import VersionHistory from './components/VersionHistory.vue'

const FAVORITES_KEY = 'ga-favorites'
const RECENT_KEY = 'ga-recent'
const CURRENT_USER_KEY = 'ga-current-user'
const RIGHT_PANEL_KEY = 'ga-right-panel-open'
const LEFT_PANE_KEY = 'ga-left-pane-width'
const FOCUS_MODE_KEY = 'ga-focus-mode'

const docs = ref([])
const versions = ref([])
const comments = ref([])
const attachments = ref([])
const templates = ref([])
const docListRef = ref(null)
const activeSlug = ref('')
const currentUser = ref('admin')
const currentDoc = ref(emptyDoc())
const showHome = ref(true)
const commandOpen = ref(false)
const commandQuery = ref('')
const commandActiveIndex = ref(0)
const toast = ref({
  show: false,
  message: '',
  type: 'info'
})
const confirmDialog = ref({
  open: false,
  title: '请确认',
  message: '',
  confirmText: '确认',
  cancelText: '取消',
  danger: false
})
const promptDialog = ref({
  open: false,
  title: '请输入',
  message: '',
  value: '',
  placeholder: '',
  confirmText: '确认',
  cancelText: '取消'
})
const promptInputRef = ref(null)
const bulkResult = ref({
  open: false,
  actionLabel: '',
  updated: [],
  skipped: []
})
const diffFrom = ref(null)
const diffTo = ref(null)
const diffText = ref('')
const favorites = ref([])
const recent = ref([])
const rightPanelOpen = ref(loadRightPanelState())
const leftPaneWidth = ref(loadLeftPaneWidth())
const focusMode = ref(loadFocusModeState())
const shareTokenFromUrl = ref('')
let toastTimer = null
let confirmResolver = null
let promptResolver = null
const breadcrumbTitle = computed(() => currentDoc.value.title || 'Untitled Page')
const breadcrumbPath = computed(() => {
  if (showHome.value) {
    return [{ slug: 'home', title: 'Home' }]
  }
  if (!activeSlug.value) {
    return [{ slug: '', title: breadcrumbTitle.value }]
  }
  const bySlug = new Map(docs.value.map((d) => [d.slug, d]))
  const path = []
  let cursor = bySlug.get(activeSlug.value)
  const visited = new Set()
  while (cursor && !visited.has(cursor.slug)) {
    visited.add(cursor.slug)
    path.unshift({ slug: cursor.slug, title: cursor.title || cursor.slug })
    cursor = cursor.parentSlug ? bySlug.get(cursor.parentSlug) : null
  }
  return path.length ? path : [{ slug: activeSlug.value, title: breadcrumbTitle.value }]
})
const childPages = computed(() => {
  if (!activeSlug.value) {
    return []
  }
  const byParent = new Map()
  visibleDocs.value.forEach((doc) => {
    if (!doc.parentSlug) {
      return
    }
    if (!byParent.has(doc.parentSlug)) {
      byParent.set(doc.parentSlug, [])
    }
    byParent.get(doc.parentSlug).push(doc)
  })
  const result = []
  const walk = (parentSlug, depth) => {
    const children = (byParent.get(parentSlug) || []).slice().sort(sortByOrder)
    children.forEach((child) => {
      result.push({
        ...child,
        depth,
        childCount: (byParent.get(child.slug) || []).length
      })
      walk(child.slug, depth + 1)
    })
  }
  walk(activeSlug.value, 0)
  return result
})
const siblingNav = computed(() => {
  if (!activeSlug.value) {
    return { prevSlug: '', prevTitle: '', nextSlug: '', nextTitle: '' }
  }
  const current = visibleDocs.value.find((doc) => doc.slug === activeSlug.value)
  if (!current) {
    return { prevSlug: '', prevTitle: '', nextSlug: '', nextTitle: '' }
  }
  const parent = current.parentSlug || ''
  const siblings = visibleDocs.value
    .filter((doc) => (doc.parentSlug || '') === parent)
    .slice()
    .sort(sortByOrder)
  const index = siblings.findIndex((doc) => doc.slug === activeSlug.value)
  if (index < 0) {
    return { prevSlug: '', prevTitle: '', nextSlug: '', nextTitle: '' }
  }
  const prev = siblings[index - 1]
  const next = siblings[index + 1]
  return {
    prevSlug: prev?.slug || '',
    prevTitle: prev?.title || '',
    nextSlug: next?.slug || '',
    nextTitle: next?.title || ''
  }
})
const visibleDocs = computed(() => docs.value.filter((doc) => canViewDoc(doc)))
const homeStats = computed(() => {
  const published = visibleDocs.value.filter((d) => (d.status || 'DRAFT') === 'PUBLISHED').length
  const archived = visibleDocs.value.filter((d) => (d.status || 'DRAFT') === 'ARCHIVED').length
  const draft = visibleDocs.value.length - published - archived
  const privateCount = visibleDocs.value.filter((d) => (d.visibility || 'SPACE') === 'PRIVATE').length
  return {
    total: visibleDocs.value.length,
    published,
    draft,
    privateCount,
    archived
  }
})
const homeRecentDocs = computed(() => {
  const bySlug = new Map(visibleDocs.value.map((d) => [d.slug, d]))
  return recent.value.map((slug) => bySlug.get(slug)).filter(Boolean).slice(0, 8)
})
const homeFavoriteDocs = computed(() => {
  const bySlug = new Map(visibleDocs.value.map((d) => [d.slug, d]))
  return favorites.value.map((slug) => bySlug.get(slug)).filter(Boolean).slice(0, 8)
})
const homeOverdueDocs = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return visibleDocs.value
    .filter((doc) => !!doc.dueDate && doc.dueDate < today && (doc.status || 'DRAFT') !== 'ARCHIVED')
    .sort((a, b) => (a.dueDate || '').localeCompare(b.dueDate || ''))
    .slice(0, 8)
})
const homeTodayDueDocs = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return visibleDocs.value
    .filter((doc) => doc.dueDate === today && (doc.status || 'DRAFT') !== 'ARCHIVED')
    .sort((a, b) => (a.priority || 'MEDIUM').localeCompare(b.priority || 'MEDIUM'))
    .slice(0, 8)
})
const homeAssigneeBoard = computed(() => {
  const board = new Map()
  visibleDocs.value.forEach((doc) => {
    const assignee = (doc.assignee || '未分配').trim() || '未分配'
    if (!board.has(assignee)) {
      board.set(assignee, { assignee, total: 0, overdue: 0, today: 0 })
    }
    const item = board.get(assignee)
    item.total += 1
    const today = new Date().toISOString().slice(0, 10)
    if (doc.dueDate && doc.dueDate < today && (doc.status || 'DRAFT') !== 'ARCHIVED') {
      item.overdue += 1
    }
    if (doc.dueDate && doc.dueDate === today && (doc.status || 'DRAFT') !== 'ARCHIVED') {
      item.today += 1
    }
  })
  return Array.from(board.values())
    .sort((a, b) => b.overdue - a.overdue || b.today - a.today || b.total - a.total)
    .slice(0, 10)
})
const myTodoDocs = computed(() => {
  const user = (currentUser.value || '').trim()
  if (!user) {
    return []
  }
  return visibleDocs.value
    .filter((doc) => (doc.assignee || '') === user && (doc.status || 'DRAFT') !== 'ARCHIVED')
    .sort((a, b) => {
      const dueA = a.dueDate || '9999-12-31'
      const dueB = b.dueDate || '9999-12-31'
      if (dueA !== dueB) {
        return dueA.localeCompare(dueB)
      }
      return (a.priority || 'MEDIUM').localeCompare(b.priority || 'MEDIUM')
    })
    .slice(0, 12)
})
const commandResults = computed(() => {
  const q = commandQuery.value.trim().toLowerCase()
  if (!q) {
    return visibleDocs.value.slice(0, 12)
  }
  return visibleDocs.value
    .filter((d) => (d.title || '').toLowerCase().includes(q) || (d.slug || '').toLowerCase().includes(q))
    .slice(0, 12)
})
const pageOutline = computed(() => {
  const content = currentDoc.value?.content || ''
  const lines = content.split('\n')
  const result = []
  let index = 0
  for (const line of lines) {
    const match = line.match(/^(#{1,4})\s+(.+)/)
    if (!match) {
      continue
    }
    result.push({
      id: `outline-${index++}`,
      level: match[1].length,
      text: match[2].trim()
    })
  }
  return result
})
const currentCanEdit = computed(() => !currentDoc.value?.id || canEditDoc(currentDoc.value))
const currentShareLink = computed(() => {
  if (!activeSlug.value || !currentDoc.value?.shareEnabled || !currentDoc.value?.shareToken) {
    return ''
  }
  return `${window.location.origin}?page=${encodeURIComponent(activeSlug.value)}&token=${encodeURIComponent(currentDoc.value.shareToken)}`
})
const layoutStyle = computed(() => ({
  '--left-col': `${leftPaneWidth.value}px`
}))

function loadRightPanelState() {
  if (typeof window === 'undefined') {
    return true
  }
  const raw = window.localStorage.getItem(RIGHT_PANEL_KEY)
  if (raw === null) {
    return true
  }
  return raw !== '0'
}

function persistRightPanelState(open) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(RIGHT_PANEL_KEY, open ? '1' : '0')
}

function toggleRightPanel() {
  rightPanelOpen.value = !rightPanelOpen.value
}

function loadFocusModeState() {
  if (typeof window === 'undefined') {
    return false
  }
  return window.localStorage.getItem(FOCUS_MODE_KEY) === '1'
}

function persistFocusModeState(open) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(FOCUS_MODE_KEY, open ? '1' : '0')
}

function toggleFocusMode() {
  focusMode.value = !focusMode.value
}

function loadLeftPaneWidth() {
  if (typeof window === 'undefined') {
    return 320
  }
  const raw = Number(window.localStorage.getItem(LEFT_PANE_KEY) || '')
  if (!Number.isFinite(raw) || raw <= 0) {
    return 320
  }
  return Math.max(260, Math.min(460, Math.round(raw)))
}

function persistLeftPaneWidth(width) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(LEFT_PANE_KEY, String(width))
}

let resizingLeft = false
let leftResizeStartX = 0
let leftResizeStartWidth = 320

function onLeftResizeMove(event) {
  if (!resizingLeft) {
    return
  }
  const delta = event.clientX - leftResizeStartX
  const next = Math.max(260, Math.min(460, Math.round(leftResizeStartWidth + delta)))
  leftPaneWidth.value = next
}

function stopLeftResize() {
  if (!resizingLeft) {
    return
  }
  resizingLeft = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onLeftResizeMove)
  window.removeEventListener('mouseup', stopLeftResize)
}

function startLeftResize(event) {
  if (event.button !== 0) {
    return
  }
  event.preventDefault()
  resizingLeft = true
  leftResizeStartX = event.clientX
  leftResizeStartWidth = leftPaneWidth.value
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onLeftResizeMove)
  window.addEventListener('mouseup', stopLeftResize)
}

function showToast(message, type = 'info') {
  if (!message) {
    return
  }
  toast.value = {
    show: true,
    message,
    type
  }
  if (toastTimer) {
    clearTimeout(toastTimer)
  }
  toastTimer = setTimeout(() => {
    toast.value.show = false
  }, 2200)
}

function askConfirm(message, options = {}) {
  return new Promise((resolve) => {
    confirmDialog.value = {
      open: true,
      title: options.title || '请确认',
      message,
      confirmText: options.confirmText || '确认',
      cancelText: options.cancelText || '取消',
      danger: !!options.danger
    }
    confirmResolver = resolve
  })
}

function resolveConfirm(accepted) {
  confirmDialog.value.open = false
  if (confirmResolver) {
    confirmResolver(accepted)
    confirmResolver = null
  }
}

function askPrompt(message, options = {}) {
  return new Promise((resolve) => {
    promptDialog.value = {
      open: true,
      title: options.title || '请输入',
      message,
      value: options.initialValue || '',
      placeholder: options.placeholder || '',
      confirmText: options.confirmText || '确认',
      cancelText: options.cancelText || '取消'
    }
    promptResolver = resolve
    setTimeout(() => {
      if (promptInputRef.value && typeof promptInputRef.value.focus === 'function') {
        promptInputRef.value.focus()
        promptInputRef.value.select?.()
      }
    }, 0)
  })
}

function resolvePrompt(value) {
  promptDialog.value.open = false
  if (promptResolver) {
    const next = typeof value === 'string' ? value.trim() : ''
    promptResolver(next || null)
    promptResolver = null
  }
}

function handleEditorNotify(payload) {
  if (!payload || !payload.message) {
    return
  }
  showToast(payload.message, payload.type || 'info')
}

function firstAccessibleSlug(slugs) {
  const list = Array.isArray(slugs) ? slugs : []
  for (const slug of list) {
    const doc = docs.value.find((d) => d.slug === slug)
    if (doc && canViewDoc(doc)) {
      return slug
    }
  }
  return ''
}

function showBulkResult(actionLabel, updated, skipped) {
  bulkResult.value = {
    open: true,
    actionLabel,
    updated: updated.slice(0, 80),
    skipped: skipped.slice(0, 80)
  }
}

function emptyDoc() {
  return {
    id: null,
    slug: '',
    title: '',
    summary: '',
    parentSlug: '',
    labels: [],
    owner: currentUser.value || 'admin',
    editors: [],
    viewers: [],
    priority: 'MEDIUM',
    dueDate: '',
    assignee: '',
    status: 'DRAFT',
    visibility: 'SPACE',
    locked: false,
    sortOrder: 0,
    shareEnabled: false,
    shareToken: '',
    content: '# 新文档\n\n开始编辑...'
  }
}

async function fetchDocs() {
  const { data } = await api.get('/documents')
  docs.value = data
  syncCollectionsWithDocs()
}

async function fetchTemplates() {
  const { data } = await api.get('/templates')
  templates.value = data
}

async function loadDoc(slug) {
  const candidate = docs.value.find((d) => d.slug === slug)
  if (candidate && !canViewDoc(candidate)) {
    showToast('当前用户无权限查看该页面', 'error')
    return
  }
  const { data } = await api.get(`/documents/${slug}`)
  if (!canViewDoc(data)) {
    showToast('当前用户无权限查看该页面', 'error')
    return
  }
  currentDoc.value = data
  if (!currentDoc.value.status) {
    currentDoc.value.status = 'DRAFT'
  }
  if (!currentDoc.value.visibility) {
    currentDoc.value.visibility = 'SPACE'
  }
  if (currentDoc.value.locked === undefined || currentDoc.value.locked === null) {
    currentDoc.value.locked = false
  }
  if (currentDoc.value.shareEnabled === undefined || currentDoc.value.shareEnabled === null) {
    currentDoc.value.shareEnabled = false
  }
  if (!currentDoc.value.shareToken) {
    currentDoc.value.shareToken = ''
  }
  if (!Array.isArray(currentDoc.value.editors)) {
    currentDoc.value.editors = []
  }
  if (!Array.isArray(currentDoc.value.viewers)) {
    currentDoc.value.viewers = []
  }
  if (!currentDoc.value.priority) {
    currentDoc.value.priority = 'MEDIUM'
  }
  if (!currentDoc.value.assignee) {
    currentDoc.value.assignee = ''
  }
  if (!currentDoc.value.dueDate) {
    currentDoc.value.dueDate = ''
  }
  activeSlug.value = slug
  showHome.value = false
  await loadComments(slug)
  await loadAttachments(slug)
  touchRecent(slug)
  await loadVersions(slug)
  diffFrom.value = null
  diffTo.value = null
  diffText.value = ''
}

function createNewDoc() {
  showHome.value = false
  activeSlug.value = ''
  currentDoc.value = emptyDoc()
  versions.value = []
  comments.value = []
  attachments.value = []
  diffFrom.value = null
  diffTo.value = null
  diffText.value = ''
}

function createChildPage() {
  if (!activeSlug.value) {
    return
  }
  if (!canEditDoc(currentDoc.value)) {
    showToast('当前用户无编辑权限，不能创建子页面', 'error')
    return
  }
  const parent = currentDoc.value
  activeSlug.value = ''
  currentDoc.value = {
    ...emptyDoc(),
    parentSlug: parent.slug,
    title: `${parent.title || parent.slug} - 子页面`,
    slug: `${parent.slug}-child-${Date.now().toString().slice(-6)}`,
    content: `# ${parent.title || parent.slug} 子页面\n\n`
  }
  versions.value = []
  comments.value = []
  attachments.value = []
  diffFrom.value = null
  diffTo.value = null
  diffText.value = ''
}

function createChildPageWithTemplate(templateId) {
  if (!activeSlug.value) {
    return
  }
  if (!canEditDoc(currentDoc.value)) {
    showToast('当前用户无编辑权限，不能创建子页面', 'error')
    return
  }
  const tpl = templates.value.find((item) => item.id === Number(templateId))
  if (!tpl) {
    createChildPage()
    return
  }
  const parent = currentDoc.value
  const suffix = slugPartFromText(tpl.name || 'template')
  activeSlug.value = ''
  currentDoc.value = {
    ...emptyDoc(),
    parentSlug: parent.slug,
    title: `${parent.title || parent.slug} - ${tpl.name || '模板子页'}`,
    slug: `${parent.slug}-${suffix}-${Date.now().toString().slice(-6)}`,
    content: tpl.content || `# ${tpl.name || '模板子页'}\n\n`
  }
  versions.value = []
  comments.value = []
  attachments.value = []
  diffFrom.value = null
  diffTo.value = null
  diffText.value = ''
  showToast(`已创建模板子页面：${tpl.name}`, 'success')
}

function slugPartFromText(text) {
  const cleaned = String(text || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9\u4e00-\u9fa5]+/g, '-')
    .replace(/^-+|-+$/g, '')
  return cleaned || 'child'
}

async function saveDoc(doc) {
  if (doc.id && !canEditDoc(doc)) {
    showToast('当前用户无编辑权限', 'error')
    return
  }
  if (!doc.slug || !doc.title || !doc.summary || !doc.content) {
    showToast('请填写完整字段', 'error')
    return
  }

  if (doc.id) {
    await api.put(`/documents/${doc.slug}`, {
      title: doc.title,
      summary: doc.summary,
      content: doc.content,
      parentSlug: doc.parentSlug || null,
      labels: doc.labels || [],
      owner: doc.owner || null,
      editors: doc.editors || [],
      viewers: doc.viewers || [],
      priority: doc.priority || 'MEDIUM',
      dueDate: doc.dueDate || null,
      assignee: doc.assignee || null,
      status: doc.status || 'DRAFT',
      visibility: doc.visibility || 'SPACE',
      locked: !!doc.locked
    })
  } else {
    await api.post('/documents', {
      slug: doc.slug,
      title: doc.title,
      summary: doc.summary,
      content: doc.content,
      parentSlug: doc.parentSlug || null,
      labels: doc.labels || [],
      owner: doc.owner || currentUser.value || 'admin',
      editors: doc.editors || [],
      viewers: doc.viewers || [],
      priority: doc.priority || 'MEDIUM',
      dueDate: doc.dueDate || null,
      assignee: doc.assignee || null,
      status: doc.status || 'DRAFT',
      visibility: doc.visibility || 'SPACE',
      locked: !!doc.locked
    })
  }

  await fetchDocs()
  await loadDoc(doc.slug)
  showToast('页面已保存', 'success')
}

async function deleteDoc(slug) {
  if (!slug) {
    return
  }
  const target = docs.value.find((d) => d.slug === slug)
  if (target && !canEditDoc(target)) {
    showToast('当前用户无删除权限', 'error')
    return
  }
  const confirmed = await askConfirm(`确认删除文档 ${slug} ?`, {
    title: '删除页面',
    confirmText: '删除',
    danger: true
  })
  if (!confirmed) {
    return
  }
  await api.delete(`/documents/${slug}`)
  await fetchDocs()
  favorites.value = favorites.value.filter((s) => s !== slug)
  recent.value = recent.value.filter((s) => s !== slug)
  comments.value = []
  attachments.value = []
  persistCollections()
  openHome()
  showToast('页面已删除', 'success')
}

async function loadVersions(slug) {
  const { data } = await api.get(`/documents/${slug}/versions`)
  versions.value = data
}

async function loadComments(slug) {
  const { data } = await api.get(`/documents/${slug}/comments`)
  comments.value = data
}

async function loadAttachments(slug) {
  const { data } = await api.get(`/documents/${slug}/attachments`)
  const base = (api.defaults.baseURL || '').replace(/\/$/, '')
  attachments.value = data.map((item) => ({
    ...item,
    fullUrl: `${base}${item.contentUrl}`
  }))
}

async function refreshVersions() {
  if (!activeSlug.value) {
    return
  }
  await loadVersions(activeSlug.value)
}

async function restoreVersion(versionNo) {
  if (!activeSlug.value) {
    return
  }
  const confirmed = await askConfirm(`确认回滚到 v${versionNo} ?`, {
    title: '回滚版本',
    confirmText: '回滚',
    danger: true
  })
  if (!confirmed) {
    return
  }
  const { data } = await api.post(`/documents/${activeSlug.value}/versions/${versionNo}/restore`)
  currentDoc.value = data
  await fetchDocs()
  await loadAttachments(activeSlug.value)
  await refreshVersions()
  diffText.value = ''
  showToast(`已回滚到 v${versionNo}`, 'success')
}

function pickDiffLeft(versionNo) {
  diffFrom.value = versionNo
}

function pickDiffRight(versionNo) {
  diffTo.value = versionNo
}

async function buildDiff() {
  if (!activeSlug.value || !diffFrom.value || !diffTo.value) {
    return
  }
  const { data } = await api.get(`/documents/${activeSlug.value}/versions/diff`, {
    params: {
      from: diffFrom.value,
      to: diffTo.value
    }
  })
  diffText.value = data.diff || ''
}

async function searchDocs(keyword) {
  if (!keyword) {
    await fetchDocs()
    return
  }
  const { data } = await api.get('/documents/search', { params: { q: keyword } })
  docs.value = data
}

async function addComment(payload) {
  if (!activeSlug.value) {
    return
  }
  if (!canViewDoc(currentDoc.value)) {
    return
  }
  await api.post(`/documents/${activeSlug.value}/comments`, payload)
  await loadComments(activeSlug.value)
}

async function deleteComment(commentId) {
  if (!activeSlug.value) {
    return
  }
  await api.delete(`/documents/${activeSlug.value}/comments/${commentId}`)
  await loadComments(activeSlug.value)
}

async function uploadAttachment(file) {
  if (!activeSlug.value || !file) {
    return
  }
  if (!canEditDoc(currentDoc.value)) {
    showToast('当前用户无编辑权限', 'error')
    return
  }
  const form = new FormData()
  form.append('file', file)
  await api.post(`/documents/${activeSlug.value}/attachments`, form)
  await loadAttachments(activeSlug.value)
}

async function deleteAttachment(attachmentId) {
  if (!activeSlug.value) {
    return
  }
  if (!canEditDoc(currentDoc.value)) {
    showToast('当前用户无编辑权限', 'error')
    return
  }
  await api.delete(`/documents/${activeSlug.value}/attachments/${attachmentId}`)
  await loadAttachments(activeSlug.value)
}

function insertAttachment(attachment) {
  if (!attachment?.fullUrl) {
    return
  }
  const isImage = (attachment.contentType || '').startsWith('image/')
  const markdown = isImage
    ? `\n![${attachment.fileName}](${attachment.fullUrl})\n`
    : `\n[${attachment.fileName}](${attachment.fullUrl})\n`
  currentDoc.value.content = (currentDoc.value.content || '') + markdown
}

function openHome() {
  showHome.value = true
  activeSlug.value = ''
  currentDoc.value = emptyDoc()
  comments.value = []
  attachments.value = []
  versions.value = []
  diffFrom.value = null
  diffTo.value = null
  diffText.value = ''
}

function openBreadcrumb(slug) {
  if (!slug || slug === 'home') {
    openHome()
    return
  }
  loadDoc(slug)
}

function openMyTodoView() {
  docListRef.value?.setMyTodoFilter(currentUser.value)
  showHome.value = false
  if (myTodoDocs.value.length > 0) {
    loadDoc(myTodoDocs.value[0].slug)
  }
}

async function moveDoc(payload) {
  if (!payload?.slug) {
    return
  }
  const target = docs.value.find((d) => d.slug === payload.slug)
  if (target && !canEditDoc(target)) {
    showToast('当前用户无编辑权限', 'error')
    return
  }
  await api.patch(`/documents/${payload.slug}/move`, {
    parentSlug: payload.parentSlug || null
  })
  await fetchDocs()
  if (activeSlug.value) {
    await loadDoc(activeSlug.value)
  }
}

async function reorderDoc(payload) {
  if (!payload?.slug || !payload?.direction) {
    return
  }
  const target = docs.value.find((d) => d.slug === payload.slug)
  if (target && !canEditDoc(target)) {
    showToast('当前用户无编辑权限', 'error')
    return
  }
  await api.patch(`/documents/${payload.slug}/reorder`, {
    direction: payload.direction
  })
  await fetchDocs()
  if (activeSlug.value) {
    await loadDoc(activeSlug.value)
  }
}

async function handleDocQuickAction(payload) {
  if (!payload?.slug || !payload?.action) {
    return
  }
  const target = docs.value.find((d) => d.slug === payload.slug)
  if (!target) {
    return
  }

  if (payload.action === 'COPY_LINK') {
    const shareableLink = `${window.location.origin}?page=${encodeURIComponent(payload.slug)}`
    try {
      await navigator.clipboard.writeText(shareableLink)
      showToast('页面链接已复制', 'success')
    } catch {
      showToast('复制失败，请手动复制', 'error')
    }
    return
  }

  if (payload.action === 'COPY_SLUG') {
    try {
      await navigator.clipboard.writeText(payload.slug)
      showToast('slug 已复制', 'success')
    } catch {
      showToast('复制失败，请手动复制', 'error')
    }
    return
  }

  if (payload.action === 'TOGGLE_FAVORITE') {
    toggleFavorite(payload.slug)
    return
  }

  if (payload.action === 'OPEN_PAGE') {
    await loadDoc(payload.slug)
    return
  }

  if (!canEditDoc(target)) {
    showToast('当前用户无编辑权限', 'error')
    return
  }

  if (payload.action === 'MOVE_ROOT') {
    await moveDoc({
      slug: payload.slug,
      parentSlug: null
    })
    return
  }

  if (payload.action === 'RENAME') {
    const { data } = await api.get(`/documents/${payload.slug}`)
    const nextTitle = await askPrompt('请输入新的页面标题', {
      title: '重命名页面',
      placeholder: '输入新标题',
      initialValue: data.title || payload.slug,
      confirmText: '保存'
    })
    if (!nextTitle) {
      return
    }
    await api.put(`/documents/${payload.slug}`, {
      title: nextTitle,
      summary: data.summary,
      content: data.content,
      parentSlug: data.parentSlug || null,
      labels: data.labels || [],
      owner: data.owner || null,
      editors: data.editors || [],
      viewers: data.viewers || [],
      priority: data.priority || 'MEDIUM',
      dueDate: data.dueDate || null,
      assignee: data.assignee || null,
      status: data.status || 'DRAFT',
      visibility: data.visibility || 'SPACE',
      locked: !!data.locked
    })
    await fetchDocs()
    if (activeSlug.value === payload.slug) {
      await loadDoc(payload.slug)
    }
    return
  }

  if (payload.action === 'ARCHIVE' || payload.action === 'UNARCHIVE') {
    const nextStatus = payload.action === 'ARCHIVE' ? 'ARCHIVED' : 'DRAFT'
    const { data } = await api.get(`/documents/${payload.slug}`)
    await api.put(`/documents/${payload.slug}`, {
      title: data.title,
      summary: data.summary,
      content: data.content,
      parentSlug: data.parentSlug || null,
      labels: data.labels || [],
      owner: data.owner || null,
      editors: data.editors || [],
      viewers: data.viewers || [],
      priority: data.priority || 'MEDIUM',
      dueDate: data.dueDate || null,
      assignee: data.assignee || null,
      status: nextStatus,
      visibility: data.visibility || 'SPACE',
      locked: !!data.locked
    })
    await fetchDocs()
    if (activeSlug.value === payload.slug) {
      await loadDoc(payload.slug)
    }
  }
}

async function handleDocBulkAction(payload) {
  const slugs = Array.isArray(payload?.slugs) ? payload.slugs.filter(Boolean) : []
  if (!payload?.action || slugs.length === 0) {
    return
  }

  if (payload.action === 'BULK_FAVORITE') {
    slugs.forEach((slug) => {
      if (!favorites.value.includes(slug)) {
        favorites.value = [slug, ...favorites.value]
      }
    })
    persistCollections()
    docListRef.value?.clearBatchSelection()
    return
  }

  if (payload.action === 'BULK_UNFAVORITE') {
    favorites.value = favorites.value.filter((slug) => !slugs.includes(slug))
    persistCollections()
    docListRef.value?.clearBatchSelection()
    return
  }

  if (payload.action === 'BULK_MOVE_ROOT') {
    let updated = 0
    let skipped = 0
    const updatedSlugs = []
    const skippedItems = []
    for (const slug of slugs) {
      const target = docs.value.find((d) => d.slug === slug)
      if (!target || !canEditDoc(target)) {
        skipped += 1
        skippedItems.push({
          slug,
          reason: target ? '无编辑权限' : '页面不存在'
        })
        continue
      }
      try {
        await api.patch(`/documents/${slug}/move`, {
          parentSlug: null
        })
        updated += 1
        updatedSlugs.push(slug)
      } catch {
        skipped += 1
        skippedItems.push({
          slug,
          reason: '请求失败'
        })
      }
    }
    await fetchDocs()
    if (activeSlug.value && slugs.includes(activeSlug.value)) {
      await loadDoc(activeSlug.value)
    } else {
      const fallback = firstAccessibleSlug(updatedSlugs)
      if (fallback) {
        await loadDoc(fallback)
      }
    }
    showToast(`批量移到顶级完成：成功 ${updated}，跳过 ${skipped}`, updated > 0 ? 'success' : 'info')
    showBulkResult('批量移到顶级', updatedSlugs, skippedItems)
    docListRef.value?.clearBatchSelection()
    return
  }

  const nextStatus = payload.action === 'BULK_ARCHIVE'
    ? 'ARCHIVED'
    : payload.action === 'BULK_UNARCHIVE'
      ? 'DRAFT'
      : ''
  if (!nextStatus) {
    return
  }

  let updated = 0
  let skipped = 0
  const updatedSlugs = []
  const skippedItems = []
  for (const slug of slugs) {
    const target = docs.value.find((d) => d.slug === slug)
    if (!target || !canEditDoc(target)) {
      skipped += 1
      skippedItems.push({
        slug,
        reason: target ? '无编辑权限' : '页面不存在'
      })
      continue
    }
    try {
      const { data } = await api.get(`/documents/${slug}`)
      await api.put(`/documents/${slug}`, {
        title: data.title,
        summary: data.summary,
        content: data.content,
        parentSlug: data.parentSlug || null,
        labels: data.labels || [],
        owner: data.owner || null,
        editors: data.editors || [],
        viewers: data.viewers || [],
        priority: data.priority || 'MEDIUM',
        dueDate: data.dueDate || null,
        assignee: data.assignee || null,
        status: nextStatus,
        visibility: data.visibility || 'SPACE',
        locked: !!data.locked
      })
      updated += 1
      updatedSlugs.push(slug)
    } catch {
      skipped += 1
      skippedItems.push({
        slug,
        reason: '请求失败'
      })
    }
  }

  await fetchDocs()
  if (activeSlug.value && slugs.includes(activeSlug.value)) {
    await loadDoc(activeSlug.value)
  } else {
    const fallback = firstAccessibleSlug(updatedSlugs)
    if (fallback) {
      await loadDoc(fallback)
    }
  }
  showToast(`批量操作完成：成功 ${updated}，跳过 ${skipped}`, updated > 0 ? 'success' : 'info')
  showBulkResult(nextStatus === 'ARCHIVED' ? '批量归档' : '批量恢复草稿', updatedSlugs, skippedItems)
  docListRef.value?.clearBatchSelection()
}

async function toggleShare(enabled) {
  if (!activeSlug.value || !currentCanEdit.value) {
    return
  }
  const { data } = await api.patch(`/documents/${activeSlug.value}/share`, {
    enabled,
    regenerate: false
  })
  currentDoc.value = {
    ...currentDoc.value,
    ...data
  }
  await fetchDocs()
  showToast(enabled ? '已开启分享' : '已关闭分享', 'success')
}

async function regenerateShare() {
  if (!activeSlug.value || !currentCanEdit.value) {
    return
  }
  const { data } = await api.patch(`/documents/${activeSlug.value}/share`, {
    enabled: true,
    regenerate: true
  })
  currentDoc.value = {
    ...currentDoc.value,
    ...data
  }
  await fetchDocs()
  showToast('分享链接已重置', 'success')
}

async function createTemplate(payload) {
  if (!payload?.name || !payload?.content) {
    return
  }
  await api.post('/templates', {
    name: payload.name,
    description: payload.description || '',
    content: payload.content
  })
  await fetchTemplates()
  showToast('模板已创建', 'success')
}

async function updateTemplate(payload) {
  if (!payload?.id || !payload?.name || !payload?.content) {
    return
  }
  await api.put(`/templates/${payload.id}`, {
    name: payload.name,
    description: payload.description || '',
    content: payload.content
  })
  await fetchTemplates()
  showToast('模板已更新', 'success')
}

async function deleteTemplate(templateId) {
  if (!templateId) {
    return
  }
  const confirmed = await askConfirm('确认删除该模板？', {
    title: '删除模板',
    confirmText: '删除',
    danger: true
  })
  if (!confirmed) {
    return
  }
  await api.delete(`/templates/${templateId}`)
  await fetchTemplates()
  showToast('模板已删除', 'success')
}

function toggleFavorite(slug) {
  if (favorites.value.includes(slug)) {
    favorites.value = favorites.value.filter((s) => s !== slug)
  } else {
    favorites.value = [slug, ...favorites.value]
  }
  persistCollections()
}

function touchRecent(slug) {
  recent.value = [slug, ...recent.value.filter((s) => s !== slug)].slice(0, 12)
  persistCollections()
}

function syncCollectionsWithDocs() {
  const allSlugs = new Set(visibleDocs.value.map((d) => d.slug))
  favorites.value = favorites.value.filter((s) => allSlugs.has(s))
  recent.value = recent.value.filter((s) => allSlugs.has(s))
  persistCollections()
}

function persistCollections() {
  localStorage.setItem(FAVORITES_KEY, JSON.stringify(favorites.value))
  localStorage.setItem(RECENT_KEY, JSON.stringify(recent.value))
  localStorage.setItem(CURRENT_USER_KEY, currentUser.value || 'admin')
}

function loadCollections() {
  try {
    favorites.value = JSON.parse(localStorage.getItem(FAVORITES_KEY) || '[]')
    recent.value = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]')
    currentUser.value = localStorage.getItem(CURRENT_USER_KEY) || 'admin'
  } catch {
    favorites.value = []
    recent.value = []
    currentUser.value = 'admin'
  }
}

function handleKeydown(event) {
  const target = event.target
  const tagName = (target?.tagName || '').toUpperCase()
  const typingElement = target?.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(tagName)

  const isCommand = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k'
  if (isCommand) {
    event.preventDefault()
    commandOpen.value = true
    commandActiveIndex.value = 0
    return
  }

  const isExpandTree = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '1'
  if (isExpandTree) {
    event.preventDefault()
    docListRef.value?.expandAll()
    return
  }

  const isCollapseTree = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '2'
  if (isCollapseTree) {
    event.preventDefault()
    docListRef.value?.collapseAll()
    return
  }

  const isToggleDensity = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '3'
  if (isToggleDensity) {
    event.preventDefault()
    docListRef.value?.toggleCompactMode()
    return
  }

  const isToggleFocusMode = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '4'
  if (isToggleFocusMode) {
    event.preventDefault()
    toggleFocusMode()
    return
  }

  const isSave = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's'
  if (isSave) {
    event.preventDefault()
    saveDoc(currentDoc.value)
    return
  }

  if (typingElement) {
    return
  }
}

function closeCommand() {
  commandOpen.value = false
  commandQuery.value = ''
  commandActiveIndex.value = 0
}

function openFirstCommandResult() {
  if (commandResults.value.length === 0 || commandActiveIndex.value < 0) {
    return
  }
  const target = commandResults.value[commandActiveIndex.value] || commandResults.value[0]
  if (!target) {
    return
  }
  selectCommandDoc(target.slug)
}

function onCommandInputKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault()
    closeCommand()
    return
  }
  if (event.key === 'Enter') {
    event.preventDefault()
    openFirstCommandResult()
    return
  }
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    if (commandResults.value.length === 0) {
      return
    }
    commandActiveIndex.value = (commandActiveIndex.value + 1) % commandResults.value.length
    return
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    if (commandResults.value.length === 0) {
      return
    }
    commandActiveIndex.value = (commandActiveIndex.value - 1 + commandResults.value.length) % commandResults.value.length
  }
}

async function selectCommandDoc(slug) {
  closeCommand()
  await loadDoc(slug)
}

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  shareTokenFromUrl.value = params.get('token') || ''
  const initialPage = params.get('page') || ''
  loadCollections()
  await fetchDocs()
  await fetchTemplates()
  if (initialPage) {
    await loadDoc(initialPage)
  }
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
  stopLeftResize()
  if (toastTimer) {
    clearTimeout(toastTimer)
  }
  if (confirmResolver) {
    confirmResolver(false)
    confirmResolver = null
  }
  if (promptResolver) {
    promptResolver(null)
    promptResolver = null
  }
})

watch(commandQuery, () => {
  commandActiveIndex.value = 0
})

watch(commandResults, (list) => {
  if (!list.length) {
    commandActiveIndex.value = 0
    return
  }
  if (commandActiveIndex.value >= list.length) {
    commandActiveIndex.value = list.length - 1
  }
})

watch(currentUser, () => {
  persistCollections()
  syncCollectionsWithDocs()
  if (activeSlug.value && !canViewDoc(currentDoc.value)) {
    openHome()
  }
})

watch(rightPanelOpen, (open) => {
  persistRightPanelState(open)
})

watch(leftPaneWidth, (width) => {
  persistLeftPaneWidth(width)
})

watch(focusMode, (open) => {
  persistFocusModeState(open)
})

function normalizeMembers(values) {
  if (!Array.isArray(values)) {
    return []
  }
  return values
    .map((v) => (v || '').trim())
    .filter((v) => v.length > 0)
}

function canEditDoc(doc) {
  if (!doc) {
    return false
  }
  const user = (currentUser.value || '').trim()
  if (!user) {
    return false
  }
  const owner = (doc.owner || '').trim()
  const editors = normalizeMembers(doc.editors)
  if (!owner && editors.length === 0) {
    return true
  }
  return user === owner || editors.includes(user)
}

function canViewDoc(doc) {
  if (!doc) {
    return false
  }
  if (doc.shareEnabled === true && doc.shareToken && shareTokenFromUrl.value === doc.shareToken) {
    return true
  }
  if (canEditDoc(doc)) {
    return true
  }
  const user = (currentUser.value || '').trim()
  if (!user) {
    return false
  }
  const viewers = normalizeMembers(doc.viewers)
  if (viewers.length === 0) {
    return true
  }
  return viewers.includes(user)
}

function sortByOrder(a, b) {
  const orderA = Number.isFinite(a.sortOrder) ? a.sortOrder : 0
  const orderB = Number.isFinite(b.sortOrder) ? b.sortOrder : 0
  if (orderA !== orderB) {
    return orderA - orderB
  }
  return (a.title || '').localeCompare((b.title || ''), 'zh-Hans-CN')
}
</script>
