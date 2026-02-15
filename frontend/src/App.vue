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
        <button class="secondary tiny" @click="openHome">空间首页</button>
        <div class="topbar-badge">{{ visibleDocs.length }} pages</div>
      </div>
    </header>

    <div class="breadcrumb">
      <span>🏠 Space</span>
      <span>/</span>
      <span>Knowledge</span>
      <template v-for="item in breadcrumbPath" :key="item.slug || item.title">
        <span>/</span>
        <strong v-if="item.slug === activeSlug">{{ item.title }}</strong>
        <button v-else class="crumb-link" @click="openBreadcrumb(item.slug)">{{ item.title }}</button>
      </template>
    </div>

    <div class="layout">
      <DocList
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
      />

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
        @select-child="loadDoc"
        @toggle-share="toggleShare"
        @regenerate-share="regenerateShare"
        @create-template="createTemplate"
        @update-template="updateTemplate"
        @delete-template="deleteTemplate"
      />

      <VersionHistory
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
            @keydown.esc.prevent="closeCommand"
            @keydown.enter.prevent="openFirstCommandResult"
          />
        </div>
        <ul class="command-list">
          <li
            v-for="item in commandResults"
            :key="item.slug"
            @click="selectCommandDoc(item.slug)"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.slug }}</span>
          </li>
          <li v-if="commandResults.length === 0" class="command-empty">没有匹配页面</li>
        </ul>
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

const docs = ref([])
const versions = ref([])
const comments = ref([])
const attachments = ref([])
const templates = ref([])
const docListRef = ref(null)
const activeSlug = ref('')
const currentDoc = ref(emptyDoc())
const showHome = ref(true)
const commandOpen = ref(false)
const commandQuery = ref('')
const diffFrom = ref(null)
const diffTo = ref(null)
const diffText = ref('')
const favorites = ref([])
const recent = ref([])
const currentUser = ref('admin')
const shareTokenFromUrl = ref('')
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
  return visibleDocs.value
    .filter((d) => d.parentSlug === activeSlug.value)
    .sort(sortByOrder)
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

const FAVORITES_KEY = 'ga-favorites'
const RECENT_KEY = 'ga-recent'
const CURRENT_USER_KEY = 'ga-current-user'

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
    alert('当前用户无权限查看该页面')
    return
  }
  const { data } = await api.get(`/documents/${slug}`)
  if (!canViewDoc(data)) {
    alert('当前用户无权限查看该页面')
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
    alert('当前用户无编辑权限，不能创建子页面')
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

async function saveDoc(doc) {
  if (doc.id && !canEditDoc(doc)) {
    alert('当前用户无编辑权限')
    return
  }
  if (!doc.slug || !doc.title || !doc.summary || !doc.content) {
    alert('请填写完整字段')
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
}

async function deleteDoc(slug) {
  if (!slug) {
    return
  }
  const target = docs.value.find((d) => d.slug === slug)
  if (target && !canEditDoc(target)) {
    alert('当前用户无删除权限')
    return
  }
  if (!confirm(`确认删除文档 ${slug} ?`)) {
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
  if (!confirm(`确认回滚到 v${versionNo} ?`)) {
    return
  }
  const { data } = await api.post(`/documents/${activeSlug.value}/versions/${versionNo}/restore`)
  currentDoc.value = data
  await fetchDocs()
  await loadAttachments(activeSlug.value)
  await refreshVersions()
  diffText.value = ''
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
    alert('当前用户无编辑权限')
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
    alert('当前用户无编辑权限')
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
    alert('当前用户无编辑权限')
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
    alert('当前用户无编辑权限')
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
}

async function deleteTemplate(templateId) {
  if (!templateId) {
    return
  }
  if (!confirm('确认删除该模板？')) {
    return
  }
  await api.delete(`/templates/${templateId}`)
  await fetchTemplates()
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
  const isCommand = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k'
  if (isCommand) {
    event.preventDefault()
    commandOpen.value = true
    return
  }
  const isSave = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's'
  if (!isSave) {
    return
  }
  event.preventDefault()
  saveDoc(currentDoc.value)
}

function closeCommand() {
  commandOpen.value = false
  commandQuery.value = ''
}

function openFirstCommandResult() {
  if (commandResults.value.length === 0) {
    return
  }
  selectCommandDoc(commandResults.value[0].slug)
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
})

watch(currentUser, () => {
  persistCollections()
  syncCollectionsWithDocs()
  if (activeSlug.value && !canViewDoc(currentDoc.value)) {
    openHome()
  }
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
