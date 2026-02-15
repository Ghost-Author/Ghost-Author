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
        <div class="outline-head">页面目录</div>
        <ul class="outline-list" v-if="outline.length">
          <li
            v-for="item in outline"
            :key="item.id"
            :style="{ paddingLeft: `${(item.level - 1) * 12}px` }"
            :title="item.text"
          >
            <span class="outline-dot">•</span>
            <span>{{ item.text }}</span>
          </li>
        </ul>
        <div v-else class="outline-empty">没有可识别的标题（可用 # ## ###）</div>
      </div>

      <div class="version-list-head">版本历史</div>
      <ul>
        <li v-for="item in versions" :key="item.id">
          <div>
            <strong>v{{ item.versionNo }}</strong>
            <p>{{ formatTime(item.createdAt) }}</p>
          </div>
          <div class="version-actions">
            <button class="secondary" @click="$emit('pick-left', item.versionNo)">左侧</button>
            <button class="secondary" @click="$emit('pick-right', item.versionNo)">右侧</button>
            <button @click="$emit('restore', item.versionNo)">回滚</button>
          </div>
        </li>
      </ul>

      <div class="diff-panel">
        <div class="diff-toolbar">
          <span>对比: v{{ diffFrom || '-' }} -> v{{ diffTo || '-' }}</span>
          <button :disabled="!diffFrom || !diffTo" @click="$emit('diff')">生成 Diff</button>
        </div>
        <div class="diff-box" v-if="diffText">
          <div
            v-for="(line, idx) in diffLines"
            :key="idx"
            class="diff-line"
            :class="lineClass(line)"
          >
            {{ line || ' ' }}
          </div>
        </div>
        <pre class="diff-box placeholder" v-else>请选择两个版本并点击“生成 Diff”</pre>
      </div>
    </template>
  </aside>
</template>

<script setup>
import { computed } from 'vue'

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

const diffLines = computed(() => props.diffText.split('\n'))

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
</script>
