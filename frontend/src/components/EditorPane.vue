<template>
  <div class="editor-pane">
    <div class="pane-head">
      <h2>Page Editor</h2>
      <p class="section-subtitle">Markdown + 实时预览</p>
      <div class="view-switch" v-if="!isCreateMode">
        <button class="secondary" @click="isEditing = !isEditing">
          {{ isEditing ? '查看预览' : '编辑页面' }}
        </button>
      </div>
    </div>

    <template v-if="isEditingSafe">
      <div class="template-bar">
        <select v-model="selectedTemplate">
          <option value="">套用模板（可选）</option>
          <option v-for="tpl in templates" :key="tpl.key" :value="tpl.key">{{ tpl.name }}</option>
        </select>
        <button class="secondary small" :disabled="!selectedTemplate" @click="applyTemplate">应用模板</button>
      </div>

      <div class="meta-grid">
        <label>
          Slug
          <input v-model="model.slug" :disabled="!isCreateMode || !isEditingSafe" />
        </label>
        <label>
          标题
          <input v-model="model.title" :disabled="!isEditingSafe" />
        </label>
      </div>

      <div class="meta-grid">
        <label>
          父页面 Slug
          <input v-model="model.parentSlug" :disabled="!isEditingSafe" placeholder="例如：product-guide（可选）" />
        </label>
        <label>
          页面状态
          <select v-model="model.status" :disabled="!isEditingSafe">
            <option value="DRAFT">草稿</option>
            <option value="PUBLISHED">已发布</option>
            <option value="ARCHIVED">已归档</option>
          </select>
        </label>
      </div>

      <div class="meta-grid">
        <label>
          可见性
          <select v-model="model.visibility" :disabled="!isEditingSafe">
            <option value="SPACE">空间可见</option>
            <option value="PRIVATE">仅自己</option>
          </select>
        </label>
        <div />
      </div>

      <div class="meta-grid">
        <label>
          标签（逗号分隔）
          <input v-model="labelsText" :disabled="!isEditingSafe" placeholder="产品, 入门, SOP" />
        </label>
        <div />
      </div>

      <label>
        摘要
        <input v-model="model.summary" :disabled="!isEditingSafe" />
      </label>

      <MdEditor v-model="model.content" :toolbars="toolbars" />
    </template>

    <template v-else>
      <div class="read-meta">
        <div class="read-meta-row">
          <h3>{{ model.title || 'Untitled' }}</h3>
          <div class="read-badges">
            <span class="read-status" :class="(model.status || 'DRAFT').toLowerCase()">
              {{ statusText(model.status) }}
            </span>
            <span class="read-visibility" :class="(model.visibility || 'SPACE').toLowerCase()">
              {{ model.visibility === 'PRIVATE' ? '仅自己' : '空间可见' }}
            </span>
          </div>
        </div>
        <p class="read-summary">{{ model.summary || '暂无摘要' }}</p>
        <p class="read-times" v-if="model.updatedAt || model.createdAt">
          更新于 {{ formatTime(model.updatedAt) }} · 创建于 {{ formatTime(model.createdAt) }}
        </p>
        <div class="read-tags" v-if="model.labels && model.labels.length">
          <span class="doc-label" v-for="label in model.labels" :key="label">{{ label }}</span>
        </div>
        <div class="child-pages" v-if="childPages.length">
          <p>子页面</p>
          <ul>
            <li
              v-for="item in childPages"
              :key="item.slug"
              @click="$emit('select-child', item.slug)"
            >
              <strong>{{ item.title }}</strong>
              <span>{{ item.slug }}</span>
              <em class="child-status" :class="(item.status || 'DRAFT').toLowerCase()">{{ statusText(item.status) }}</em>
            </li>
          </ul>
        </div>
      </div>
      <div class="preview-only">
        <MdPreview :model-value="model.content" />
      </div>
    </template>

    <div class="actions">
      <button v-if="isEditingSafe" @click="$emit('save', model)">保存</button>
      <button
        v-if="!isCreateMode && isEditingSafe && model.status !== 'ARCHIVED'"
        class="secondary"
        @click="quickToggleStatus"
      >
        {{ model.status === 'PUBLISHED' ? '转为草稿并保存' : '发布并保存' }}
      </button>
      <button
        v-if="!isCreateMode && isEditingSafe && model.status !== 'ARCHIVED'"
        class="secondary"
        @click="setStatusAndSave('ARCHIVED')"
      >
        归档页面
      </button>
      <button
        v-if="!isCreateMode && isEditingSafe && model.status === 'ARCHIVED'"
        class="secondary"
        @click="setStatusAndSave('DRAFT')"
      >
        从归档恢复
      </button>
      <button v-if="!isCreateMode" class="secondary" @click="$emit('create-child')">新建子页面</button>
      <button class="danger" :disabled="isCreateMode" @click="$emit('delete', model.slug)">删除</button>
    </div>

    <section class="comment-panel" v-if="!isCreateMode">
      <h3>附件（{{ attachments.length }}）</h3>
      <div class="attachment-actions">
        <input ref="fileInput" type="file" @change="onSelectFile" />
      </div>
      <ul class="attachment-list">
        <li v-for="item in attachments" :key="item.id">
          <div class="attachment-main">
            <a :href="item.fullUrl" target="_blank" rel="noreferrer">{{ item.fileName }}</a>
            <span>{{ prettySize(item.fileSize) }}</span>
          </div>
          <div class="attachment-btns">
            <button class="secondary small" @click="$emit('insert-attachment', item)">插入正文</button>
            <button class="danger small" @click="$emit('delete-attachment', item.id)">删除</button>
          </div>
        </li>
        <li v-if="attachments.length === 0" class="comment-empty">还没有附件。</li>
      </ul>

      <h3>评论（{{ comments.length }}）</h3>
      <div class="comment-inputs">
        <input v-model="commentAuthor" placeholder="昵称（可选）" />
        <textarea v-model="commentContent" placeholder="写下你的评论..." />
        <button @click="submitComment">发布评论</button>
      </div>

      <ul class="comment-list">
        <li v-for="comment in comments" :key="comment.id">
          <div class="comment-head">
            <strong>{{ comment.author }}</strong>
            <span>{{ formatTime(comment.createdAt) }}</span>
          </div>
          <p>{{ comment.content }}</p>
          <button class="danger small" @click="$emit('delete-comment', comment.id)">删除</button>
        </li>
        <li v-if="comments.length === 0" class="comment-empty">还没有评论，来写第一条吧。</li>
      </ul>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { MdEditor, MdPreview } from 'md-editor-v3'

const props = defineProps({
  doc: {
    type: Object,
    required: true
  },
  comments: {
    type: Array,
    default: () => []
  },
  attachments: {
    type: Array,
    default: () => []
  },
  childPages: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits([
  'add-comment',
  'delete-comment',
  'upload-attachment',
  'delete-attachment',
  'insert-attachment',
  'create-child',
  'select-child'
])

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
const isEditing = ref(false)
const isEditingSafe = computed(() => isCreateMode.value || isEditing.value)
const commentAuthor = ref('')
const commentContent = ref('')
const fileInput = ref(null)
const selectedTemplate = ref('')
const templates = [
  {
    key: 'meeting',
    name: '会议纪要',
    content: `# 会议纪要

## 会议主题

## 时间与参会人
- 时间：
- 参会人：

## 议题
1. 
2. 

## 结论

## 待办事项
- [ ] 负责人：
`
  },
  {
    key: 'review',
    name: '需求评审',
    content: `# 需求评审

## 背景

## 目标

## 方案说明

## 风险与边界

## 验收标准
- [ ] 
`
  },
  {
    key: 'change',
    name: '变更记录',
    content: `# 变更记录

## 变更摘要

## 影响范围

## 回滚方案

## 验证结果
`
  }
]

watch(
  () => props.doc.id,
  (id) => {
    // Existing docs default to preview-only view; new docs default to edit.
    isEditing.value = !id
  },
  { immediate: true }
)

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

function submitComment() {
  if (!commentContent.value.trim()) {
    return
  }
  emit('add-comment', {
    author: commentAuthor.value.trim(),
    content: commentContent.value.trim()
  })
  commentAuthor.value = ''
  commentContent.value = ''
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function quickToggleStatus() {
  model.value.status = model.value.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  emit('save', model.value)
}

function setStatusAndSave(nextStatus) {
  model.value.status = nextStatus
  emit('save', model.value)
}

function onSelectFile(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }
  emit('upload-attachment', file)
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

function prettySize(size) {
  if (!size || size < 1024) {
    return `${size || 0} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function applyTemplate() {
  const item = templates.find((tpl) => tpl.key === selectedTemplate.value)
  if (!item) {
    return
  }
  model.value.content = item.content
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
</script>
