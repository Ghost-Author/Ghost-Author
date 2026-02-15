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
        <button class="secondary tiny" @click="openHome">空间首页</button>
        <div class="topbar-badge">{{ docs.length }} pages</div>
      </div>
    </header>

    <div class="breadcrumb">
      <span>🏠 Space</span>
      <span>/</span>
      <span>Knowledge</span>
      <template v-for="item in breadcrumbPath" :key="item.slug || item.title">
        <span>/</span>
        <strong v-if="item.slug === activeSlug">{{ item.title }}</strong>
        <span v-else>{{ item.title }}</span>
      </template>
    </div>

    <div class="layout">
      <DocList
        :docs="docs"
        :active-slug="activeSlug"
        :favorites="favorites"
        :recent="recent"
        @create="createNewDoc"
        @search="searchDocs"
        @select="loadDoc"
        @toggle-favorite="toggleFavorite"
        @move="moveDoc"
      />

      <SpaceHome
        v-if="showHome"
        :stats="homeStats"
        :recent-docs="homeRecentDocs"
        :favorite-docs="homeFavoriteDocs"
        @create="createNewDoc"
        @select="loadDoc"
      />
      <EditorPane
        v-else
        :doc="currentDoc"
        :comments="comments"
        :attachments="attachments"
        :child-pages="childPages"
        @save="saveDoc"
        @delete="deleteDoc"
        @add-comment="addComment"
        @delete-comment="deleteComment"
        @upload-attachment="uploadAttachment"
        @delete-attachment="deleteAttachment"
        @insert-attachment="insertAttachment"
        @create-child="createChildPage"
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
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { api } from './api/client'
import DocList from './components/DocList.vue'
import EditorPane from './components/EditorPane.vue'
import SpaceHome from './components/SpaceHome.vue'
import VersionHistory from './components/VersionHistory.vue'

const docs = ref([])
const versions = ref([])
const comments = ref([])
const attachments = ref([])
const activeSlug = ref('')
const currentDoc = ref(emptyDoc())
const showHome = ref(true)
const diffFrom = ref(null)
const diffTo = ref(null)
const diffText = ref('')
const favorites = ref([])
const recent = ref([])
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
  return docs.value
    .filter((d) => d.parentSlug === activeSlug.value)
    .sort((a, b) => a.title.localeCompare(b.title, 'zh-Hans-CN'))
})
const homeStats = computed(() => {
  const published = docs.value.filter((d) => (d.status || 'DRAFT') === 'PUBLISHED').length
  const privateCount = docs.value.filter((d) => (d.visibility || 'SPACE') === 'PRIVATE').length
  return {
    total: docs.value.length,
    published,
    draft: docs.value.length - published,
    privateCount
  }
})
const homeRecentDocs = computed(() => {
  const bySlug = new Map(docs.value.map((d) => [d.slug, d]))
  return recent.value.map((slug) => bySlug.get(slug)).filter(Boolean).slice(0, 8)
})
const homeFavoriteDocs = computed(() => {
  const bySlug = new Map(docs.value.map((d) => [d.slug, d]))
  return favorites.value.map((slug) => bySlug.get(slug)).filter(Boolean).slice(0, 8)
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

const FAVORITES_KEY = 'ga-favorites'
const RECENT_KEY = 'ga-recent'

function emptyDoc() {
  return {
    id: null,
    slug: '',
    title: '',
    summary: '',
    parentSlug: '',
    labels: [],
    status: 'DRAFT',
    visibility: 'SPACE',
    content: '# 新文档\n\n开始编辑...'
  }
}

async function fetchDocs() {
  const { data } = await api.get('/documents')
  docs.value = data
  syncCollectionsWithDocs()
}

async function loadDoc(slug) {
  const { data } = await api.get(`/documents/${slug}`)
  currentDoc.value = data
  if (!currentDoc.value.status) {
    currentDoc.value.status = 'DRAFT'
  }
  if (!currentDoc.value.visibility) {
    currentDoc.value.visibility = 'SPACE'
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
      status: doc.status || 'DRAFT',
      visibility: doc.visibility || 'SPACE'
    })
  } else {
    await api.post('/documents', {
      slug: doc.slug,
      title: doc.title,
      summary: doc.summary,
      content: doc.content,
      parentSlug: doc.parentSlug || null,
      labels: doc.labels || [],
      status: doc.status || 'DRAFT',
      visibility: doc.visibility || 'SPACE'
    })
  }

  await fetchDocs()
  await loadDoc(doc.slug)
}

async function deleteDoc(slug) {
  if (!slug) {
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
  const form = new FormData()
  form.append('file', file)
  await api.post(`/documents/${activeSlug.value}/attachments`, form)
  await loadAttachments(activeSlug.value)
}

async function deleteAttachment(attachmentId) {
  if (!activeSlug.value) {
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

async function moveDoc(payload) {
  if (!payload?.slug) {
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
  const allSlugs = new Set(docs.value.map((d) => d.slug))
  favorites.value = favorites.value.filter((s) => allSlugs.has(s))
  recent.value = recent.value.filter((s) => allSlugs.has(s))
  persistCollections()
}

function persistCollections() {
  localStorage.setItem(FAVORITES_KEY, JSON.stringify(favorites.value))
  localStorage.setItem(RECENT_KEY, JSON.stringify(recent.value))
}

function loadCollections() {
  try {
    favorites.value = JSON.parse(localStorage.getItem(FAVORITES_KEY) || '[]')
    recent.value = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]')
  } catch {
    favorites.value = []
    recent.value = []
  }
}

function handleKeydown(event) {
  const isSave = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's'
  if (!isSave) {
    return
  }
  event.preventDefault()
  saveDoc(currentDoc.value)
}

onMounted(async () => {
  loadCollections()
  await fetchDocs()
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>
