<template>
  <div class="editor-pane">
    <div class="pane-head">
      <h2>Page Editor</h2>
      <p class="section-subtitle">Markdown + 实时预览</p>
    </div>

    <div class="meta-grid">
      <label>
        Slug
        <input v-model="model.slug" :disabled="!isCreateMode" />
      </label>
      <label>
        标题
        <input v-model="model.title" />
      </label>
    </div>

    <div class="meta-grid">
      <label>
        父页面 Slug
        <input v-model="model.parentSlug" placeholder="例如：product-guide（可选）" />
      </label>
      <label>
        标签（逗号分隔）
        <input v-model="labelsText" placeholder="产品, 入门, SOP" />
      </label>
    </div>

    <label>
      摘要
      <input v-model="model.summary" />
    </label>

    <MdEditor v-model="model.content" :toolbars="toolbars" />

    <div class="actions">
      <button @click="$emit('save', model)">保存</button>
      <button class="danger" :disabled="isCreateMode" @click="$emit('delete', model.slug)">删除</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { MdEditor } from 'md-editor-v3'

const props = defineProps({
  doc: {
    type: Object,
    required: true
  }
})

const toolbars = [
  'bold', 'underline', 'italic', '-',
  'title', 'strikeThrough', 'quote', '-',
  'unorderedList', 'orderedList', 'task', '-',
  'codeRow', 'code', 'link', 'image', '-',
  'table', 'mermaid', 'katex', '-',
  'revoke', 'next', 'save', '=',
  'pageFullscreen', 'fullscreen', 'preview', 'htmlPreview'
]

const model = computed(() => props.doc)
const isCreateMode = computed(() => !props.doc.id)
const labelsText = computed({
  get() {
    if (!Array.isArray(props.doc.labels)) {
      return ''
    }
    return props.doc.labels.join(', ')
  },
  set(value) {
    props.doc.labels = value
      .split(',')
      .map((v) => v.trim())
      .filter((v) => v.length > 0)
  }
})
</script>
