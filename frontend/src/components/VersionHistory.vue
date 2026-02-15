<template>
  <aside class="version-history">
    <div class="version-head">
      <div>
        <h3>Page Tools</h3>
        <p class="section-subtitle">目录导航 + 版本管理</p>
      </div>
      <button :disabled="!slug" @click="$emit('refresh')">刷新</button>
    </div>

    <div v-if="!slug" class="hint">先保存文档后才会产生目录和版本</div>

    <template v-else>
      <div class="outline-panel">
        <button class="panel-fold-head" @click="outlineOpen = !outlineOpen">
          <strong>页面目录</strong>
          <span>{{ outlineOpen ? '▾' : '▸' }}</span>
        </button>
        <ul class="outline-list" v-if="outlineOpen && outline.length">
          <li
            v-for="item in outline"
            :key="item.id"
            :class="{ active: activeOutlineText === item.text }"
            :style="{ paddingLeft: `${(item.level - 1) * 12}px` }"
            :title="item.text"
            @click="scrollToHeading(item.text)"
          >
            <span class="outline-dot">•</span>
            <span>{{ item.text }}</span>
          </li>
        </ul>
        <div v-else-if="outlineOpen" class="outline-empty">没有可识别的标题（可用 # ## ###）</div>
      </div>

      <button class="panel-fold-head version-list-head" @click="historyOpen = !historyOpen">
        <strong>版本历史</strong>
        <span>{{ historyOpen ? '▾' : '▸' }}</span>
      </button>
      <input
        v-show="historyOpen"
        class="version-search"
        v-model="versionKeyword"
        placeholder="筛选版本（版本号/时间）"
      />
      <ul v-show="historyOpen">
        <li v-for="item in filteredVersions" :key="item.id">
          <div>
            <strong>v{{ item.versionNo }}</strong>
            <p>{{ formatTime(item.createdAt) }}</p>
            <div class="version-picked">
              <span v-if="diffFrom === item.versionNo" class="pick-tag left">左侧</span>
              <span v-if="diffTo === item.versionNo" class="pick-tag right">右侧</span>
            </div>
          </div>
          <div class="version-actions">
            <button class="secondary" :class="{ selected: diffFrom === item.versionNo }" @click="$emit('pick-left', item.versionNo)">左侧</button>
            <button class="secondary" :class="{ selected: diffTo === item.versionNo }" @click="$emit('pick-right', item.versionNo)">右侧</button>
            <button @click="$emit('restore', item.versionNo)">回滚</button>
          </div>
        </li>
        <li v-if="filteredVersions.length === 0" class="version-empty">没有匹配版本</li>
      </ul>

      <div class="diff-panel">
        <button class="panel-fold-head" @click="diffOpen = !diffOpen">
          <strong>版本对比</strong>
          <span>{{ diffOpen ? '▾' : '▸' }}</span>
        </button>
        <div v-show="diffOpen">
          <div class="diff-toolbar">
            <span>对比: v{{ diffFrom || '-' }} -> v{{ diffTo || '-' }}</span>
            <button :disabled="!diffFrom || !diffTo" @click="$emit('diff')">生成 Diff</button>
          </div>
          <input
            class="diff-search"
            v-model="diffKeyword"
            placeholder="高亮 Diff 关键字"
          />
          <div class="diff-box" v-if="diffText">
            <div
              v-for="(line, idx) in highlightedDiffLines"
              :key="idx"
              class="diff-line"
              :class="lineClass(line.raw)"
              v-html="line.html"
            >
            </div>
          </div>
          <pre class="diff-box placeholder" v-else>请选择两个版本并点击“生成 Diff”</pre>
        </div>
      </div>
    </template>
  </aside>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  slug: {
    type: String,
    default: ''
  },
  versions: {
    type: Array,
    default: () => []
  },
  outline: {
    type: Array,
    default: () => []
  },
  diffFrom: {
    type: Number,
    default: null
  },
  diffTo: {
    type: Number,
    default: null
  },
  diffText: {
    type: String,
    default: ''
  }
})

const outlineOpen = ref(true)
const historyOpen = ref(true)
const diffOpen = ref(true)
const activeOutlineText = ref('')
const versionKeyword = ref('')
const diffKeyword = ref('')
const diffLines = computed(() => props.diffText.split('\n'))
const highlightedDiffLines = computed(() => {
  const query = diffKeyword.value.trim()
  return diffLines.value.map((line) => ({
    raw: line,
    html: highlightLine(line || ' ', query)
  }))
})
const filteredVersions = computed(() => {
  const keyword = versionKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return props.versions
  }
  return props.versions.filter((item) => {
    const versionText = `v${item.versionNo}`.toLowerCase()
    const timeText = formatTime(item.createdAt).toLowerCase()
    return versionText.includes(keyword) || timeText.includes(keyword)
  })
})
let previewContainer = null
let previewHeadings = []

function syncActiveOutlineByScroll() {
  if (!previewContainer || previewHeadings.length === 0) {
    return
  }
  const topOffset = 84
  let current = previewHeadings[0]
  for (const item of previewHeadings) {
    if ((item.node.offsetTop - previewContainer.scrollTop) <= topOffset) {
      current = item
      continue
    }
    break
  }
  activeOutlineText.value = current.text
}

function bindPreviewScroll() {
  if (previewContainer) {
    previewContainer.removeEventListener('scroll', syncActiveOutlineByScroll)
  }
  previewContainer = document.querySelector('.preview-only')
  if (!previewContainer) {
    previewHeadings = []
    return
  }
  const nodes = Array.from(previewContainer.querySelectorAll('h1, h2, h3, h4'))
  previewHeadings = nodes.map((node) => ({
    node,
    text: (node.textContent || '').trim()
  })).filter((item) => item.text.length > 0)
  previewContainer.addEventListener('scroll', syncActiveOutlineByScroll, { passive: true })
  syncActiveOutlineByScroll()
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function lineClass(line) {
  if (line.startsWith('@@')) {
    return 'chunk'
  }
  if (line.startsWith('+++') || line.startsWith('---')) {
    return 'meta'
  }
  if (line.startsWith('+')) {
    return 'add'
  }
  if (line.startsWith('-')) {
    return 'del'
  }
  return ''
}

function normalizeText(value) {
  return (value || '').replace(/\s+/g, ' ').trim().toLowerCase()
}

function escapeHtml(value) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
}

function highlightLine(text, keyword) {
  const escaped = escapeHtml(text)
  if (!keyword) {
    return escaped || ' '
  }
  const safeKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(safeKeyword, 'gi')
  return escaped.replace(regex, (matched) => `<mark>${matched}</mark>`) || ' '
}

function scrollToHeading(text) {
  const container = document.querySelector('.preview-only')
  if (!container) {
    return
  }
  const headings = Array.from(container.querySelectorAll('h1, h2, h3, h4'))
  const target = headings.find((node) => normalizeText(node.textContent) === normalizeText(text))
  if (!target) {
    return
  }
  activeOutlineText.value = text
  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

watch(() => props.slug, () => {
  activeOutlineText.value = ''
  nextTick(() => bindPreviewScroll())
})

watch(() => props.outline, () => {
  nextTick(() => bindPreviewScroll())
}, { deep: true })

onMounted(() => {
  nextTick(() => bindPreviewScroll())
})

onBeforeUnmount(() => {
  if (previewContainer) {
    previewContainer.removeEventListener('scroll', syncActiveOutlineByScroll)
  }
})
</script>
