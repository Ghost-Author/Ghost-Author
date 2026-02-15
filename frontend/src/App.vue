<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">GA</span>
        <div>
          <strong>Knowledge Space</strong>
          <p>Confluence-like Workspace · Cute Edition</p>
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
        @create="createNewDoc"
        @search="searchDocs"
        @select="loadDoc"
      />

      <EditorPane
        :doc="currentDoc"
        @save="saveDoc"
        @delete="deleteDoc"
      />

      <VersionHistory
        :slug="activeSlug"
        :versions="versions"
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
import { computed, onMounted, ref } from 'vue'
import { api } from './api/client'
import DocList from './components/DocList.vue'
import EditorPane from './components/EditorPane.vue'
import VersionHistory from './components/VersionHistory.vue'

const docs = ref([])
const versions = ref([])
const activeSlug = ref('')
const currentDoc = ref(emptyDoc())
const diffFrom = ref(null)
const diffTo = ref(null)
const diffText = ref('')
const breadcrumbTitle = computed(() => currentDoc.value.title || 'Untitled Page')

function emptyDoc() {
  return {
    id: null,
    slug: '',
    title: '',
    summary: '',
    parentSlug: '',
    labels: [],
    content: '# 新文档\n\n开始编辑...'
  }
}

async function fetchDocs() {
  const { data } = await api.get('/documents')
  docs.value = data
}

async function loadDoc(slug) {
  const { data } = await api.get(`/documents/${slug}`)
  currentDoc.value = data
  activeSlug.value = slug
  await loadVersions(slug)
  diffFrom.value = null
  diffTo.value = null
  diffText.value = ''
}

function createNewDoc() {
  activeSlug.value = ''
  currentDoc.value = emptyDoc()
  versions.value = []
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
      labels: doc.labels || []
    })
  } else {
    await api.post('/documents', {
      slug: doc.slug,
      title: doc.title,
      summary: doc.summary,
      content: doc.content,
      parentSlug: doc.parentSlug || null,
      labels: doc.labels || []
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
  createNewDoc()
}

async function loadVersions(slug) {
  const { data } = await api.get(`/documents/${slug}/versions`)
  versions.value = data
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

onMounted(async () => {
  await fetchDocs()
})
</script>
