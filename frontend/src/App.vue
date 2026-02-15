<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">GA</span>
        <div>
          <strong>Knowledge Space</strong>
        </div>
      </div>
      <div class="topbar-badge">{{ docs.length }} pages</div>
    </header>

    <div class="breadcrumb">
      <span>🏠 Space</span>
      <span>/</span>
      <span>Knowledge</span>
      <span>/</span>
      <strong>{{ breadcrumbTitle }}</strong>
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
      />

      <EditorPane
        :doc="currentDoc"
        :comments="comments"
        @save="saveDoc"
        @delete="deleteDoc"
        @add-comment="addComment"
        @delete-comment="deleteComment"
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
import VersionHistory from './components/VersionHistory.vue'

const docs = ref([])
const versions = ref([])
const comments = ref([])
const activeSlug = ref('')
const currentDoc = ref(emptyDoc())
const diffFrom = ref(null)
const diffTo = ref(null)
const diffText = ref('')
const favorites = ref([])
const recent = ref([])
const breadcrumbTitle = computed(() => currentDoc.value.title || 'Untitled Page')
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
  activeSlug.value = slug
  await loadComments(slug)
  touchRecent(slug)
  await loadVersions(slug)
  diffFrom.value = null
  diffTo.value = null
  diffText.value = ''
}

function createNewDoc() {
  activeSlug.value = ''
  currentDoc.value = emptyDoc()
  versions.value = []
  comments.value = []
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
      status: doc.status || 'DRAFT'
    })
  } else {
    await api.post('/documents', {
      slug: doc.slug,
      title: doc.title,
      summary: doc.summary,
      content: doc.content,
      parentSlug: doc.parentSlug || null,
      labels: doc.labels || [],
      status: doc.status || 'DRAFT'
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
  persistCollections()
  createNewDoc()
}

async function loadVersions(slug) {
  const { data } = await api.get(`/documents/${slug}/versions`)
  versions.value = data
}

async function loadComments(slug) {
  const { data } = await api.get(`/documents/${slug}/comments`)
  comments.value = data
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
