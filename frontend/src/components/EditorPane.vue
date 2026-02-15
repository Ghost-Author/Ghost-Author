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
        </select>
      </label>
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

    <div v-if="!isEditingSafe" class="preview-only">
      <MdPreview :model-value="model.content" />
    </div>
    <MdEditor v-else v-model="model.content" :toolbars="toolbars" />

    <div class="actions">
      <button v-if="isEditingSafe" @click="$emit('save', model)">保存</button>
      <button class="danger" :disabled="isCreateMode" @click="$emit('delete', model.slug)">删除</button>
    </div>

    <section class="comment-panel" v-if="!isCreateMode">
      <h3>评论</h3>
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
  }
})

const emit = defineEmits(['add-comment', 'delete-comment'])

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
</script>
