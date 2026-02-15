<template>
  <div class="editor-pane">
    <div class="pane-head">
      <h2>Page Editor</h2>
      <p class="section-subtitle">Markdown + 实时预览</p>
      <div class="view-switch" v-if="!isCreateMode">
        <button class="secondary" @click="toggleEditMode" :disabled="!canEdit">
          {{ isEditing ? '查看预览' : '编辑页面' }}
        </button>
      </div>
    </div>

    <template v-if="isEditingSafe">
      <div class="template-bar">
        <select v-model="selectedTemplate">
          <option value="">套用模板（可选）</option>
          <option v-for="tpl in templates" :key="tpl.id" :value="tpl.id">{{ tpl.name }}</option>
        </select>
        <button class="secondary small" :disabled="!selectedTemplate" @click="applyTemplate">应用模板</button>
        <button class="secondary small" @click="templateCenterOpen = !templateCenterOpen">
          {{ templateCenterOpen ? '收起模板中心' : '模板中心' }}
        </button>
      </div>

      <div class="template-center" v-if="templateCenterOpen">
        <h4>模板中心</h4>
        <div class="template-form">
          <input v-model="newTemplate.name" placeholder="模板名称" />
          <input v-model="newTemplate.description" placeholder="模板描述（可选）" />
          <textarea v-model="newTemplate.content" placeholder="模板内容（Markdown）" />
          <button class="secondary small" :disabled="!canCreateTemplate" @click="createTemplate">新增模板</button>
        </div>
        <ul class="template-list">
          <li v-for="tpl in templates" :key="tpl.id">
            <template v-if="editingTemplateId === tpl.id">
              <input v-model="editTemplate.name" />
              <input v-model="editTemplate.description" />
              <textarea v-model="editTemplate.content" />
              <div class="template-item-actions">
                <button class="secondary small" @click="saveEditTemplate">保存</button>
                <button class="secondary small" @click="cancelEditTemplate">取消</button>
              </div>
            </template>
            <template v-else>
              <strong>{{ tpl.name }}</strong>
              <p>{{ tpl.description || '无描述' }}</p>
              <div class="template-item-actions">
                <button class="secondary small" @click="applyTemplateById(tpl.id)">套用</button>
                <button class="secondary small" @click="startEditTemplate(tpl)">编辑</button>
                <button class="danger small" @click="$emit('delete-template', tpl.id)">删除</button>
              </div>
            </template>
          </li>
          <li v-if="templates.length === 0" class="comment-empty">还没有模板，请先新增。</li>
        </ul>
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
        <label>
          页面锁定
          <select v-model="model.locked" :disabled="!isEditingSafe">
            <option :value="false">未锁定</option>
            <option :value="true">锁定（只读）</option>
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

      <div class="meta-grid">
        <label>
          Owner
          <input v-model="model.owner" :disabled="!isEditingSafe" placeholder="例如：liupeng" />
        </label>
        <label>
          Editors（逗号分隔）
          <input v-model="editorsText" :disabled="!isEditingSafe" placeholder="alice, bob" />
        </label>
      </div>

      <div class="meta-grid">
        <label>
          Viewers（逗号分隔）
          <input v-model="viewersText" :disabled="!isEditingSafe" placeholder="carol, david" />
        </label>
        <div />
      </div>

      <div class="meta-grid">
        <label>
          优先级
          <select v-model="model.priority" :disabled="!isEditingSafe">
            <option value="LOW">低</option>
            <option value="MEDIUM">中</option>
            <option value="HIGH">高</option>
          </select>
        </label>
        <label>
          截止日期
          <input type="date" v-model="model.dueDate" :disabled="!isEditingSafe" />
        </label>
      </div>

      <div class="meta-grid">
        <label>
          负责人
          <input v-model="model.assignee" :disabled="!isEditingSafe" placeholder="例如：liupeng" />
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
            <span class="read-locked" :class="{ locked: !!model.locked }">
              {{ model.locked ? '已锁定' : '未锁定' }}
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
        <div class="perm-tags">
          <span class="perm-chip owner">Owner: {{ model.owner || '-' }}</span>
          <span class="perm-chip">Editors: {{ (model.editors || []).join(', ') || '-' }}</span>
          <span class="perm-chip">Viewers: {{ (model.viewers || []).join(', ') || '-' }}</span>
          <span class="perm-chip current">当前用户: {{ currentUser || '-' }}</span>
          <span class="perm-chip share">{{ model.shareEnabled ? '分享已开启' : '未开启分享' }}</span>
          <span class="perm-chip priority">优先级: {{ priorityText(model.priority) }}</span>
          <span class="perm-chip">负责人: {{ model.assignee || '-' }}</span>
          <span class="perm-chip">截止: {{ model.dueDate || '-' }}</span>
        </div>
        <div class="share-bar" v-if="model.shareEnabled && shareLink">
          <input :value="shareLink" readonly />
          <button class="secondary small" @click="copyShareLink">复制链接</button>
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
      <div class="action-group primary">
        <button v-if="isEditingSafe" @click="$emit('save', model)">保存</button>
        <button v-if="!isCreateMode" class="secondary" :disabled="!canEdit" @click="$emit('create-child')">新建子页面</button>
      </div>
      <div class="action-group status" v-if="!isCreateMode && isEditingSafe">
        <button
          v-if="model.status !== 'ARCHIVED'"
          class="secondary"
          @click="quickToggleStatus"
        >
          {{ model.status === 'PUBLISHED' ? '转为草稿并保存' : '发布并保存' }}
        </button>
        <button
          v-if="model.status !== 'ARCHIVED'"
          class="secondary"
          @click="setStatusAndSave('ARCHIVED')"
        >
          归档页面
        </button>
        <button
          v-if="model.status === 'ARCHIVED'"
          class="secondary"
          @click="setStatusAndSave('DRAFT')"
        >
          从归档恢复
        </button>
      </div>
      <div class="action-group advanced" v-if="!isCreateMode">
        <button
          class="secondary"
          :disabled="!canEdit"
          @click="setLockedAndSave(!model.locked)"
        >
          {{ model.locked ? '解除锁定' : '锁定页面' }}
        </button>
        <button
          class="secondary"
          :disabled="!canEdit"
          @click="$emit('toggle-share', !model.shareEnabled)"
        >
          {{ model.shareEnabled ? '关闭分享' : '开启分享' }}
        </button>
        <button
          v-if="model.shareEnabled"
          class="secondary"
          :disabled="!canEdit"
          @click="$emit('regenerate-share')"
        >
          重置分享链接
        </button>
        <button class="danger" :disabled="isCreateMode || !canEdit" @click="$emit('delete', model.slug)">删除</button>
      </div>
    </div>

    <section class="comment-panel" v-if="!isCreateMode">
      <h3>附件（{{ attachments.length }}）</h3>
      <div class="attachment-actions">
        <input ref="fileInput" type="file" :disabled="!canEdit" @change="onSelectFile" />
      </div>
      <ul class="attachment-list">
        <li v-for="item in attachments" :key="item.id">
          <div class="attachment-main">
            <a :href="item.fullUrl" target="_blank" rel="noreferrer">{{ item.fileName }}</a>
            <span>{{ prettySize(item.fileSize) }}</span>
          </div>
          <div class="attachment-btns">
            <button class="secondary small" :disabled="!canEdit" @click="$emit('insert-attachment', item)">插入正文</button>
            <button class="danger small" :disabled="!canEdit" @click="$emit('delete-attachment', item.id)">删除</button>
          </div>
        </li>
        <li v-if="attachments.length === 0" class="comment-empty">还没有附件。</li>
      </ul>

      <h3>评论（{{ comments.length }}）</h3>
      <div class="comment-inputs">
        <input v-model="commentAuthor" placeholder="昵称（可选）" />
        <textarea v-model="commentContent" placeholder="写下你的评论..." />
        <button :disabled="!canEdit" @click="submitComment">发布评论</button>
      </div>

      <ul class="comment-list">
        <li v-for="comment in comments" :key="comment.id">
          <div class="comment-head">
            <strong>{{ comment.author }}</strong>
            <span>{{ formatTime(comment.createdAt) }}</span>
          </div>
          <p>{{ comment.content }}</p>
          <button class="danger small" :disabled="!canEdit" @click="$emit('delete-comment', comment.id)">删除</button>
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
  },
  currentUser: {
    type: String,
    default: 'admin'
  },
  canEdit: {
    type: Boolean,
    default: true
  },
  shareLink: {
    type: String,
    default: ''
  },
  templates: {
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
  'select-child',
  'toggle-share',
  'regenerate-share',
  'create-template',
  'update-template',
  'delete-template'
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
const isLocked = computed(() => !!props.doc.locked)
const isEditing = ref(false)
const isEditingSafe = computed(() => (isCreateMode.value || (isEditing.value && !isLocked.value)) && props.canEdit)
const commentAuthor = ref('')
const commentContent = ref('')
const fileInput = ref(null)
const selectedTemplate = ref('')
const templateCenterOpen = ref(false)
const editingTemplateId = ref(null)
const newTemplate = ref({
  name: '',
  description: '',
  content: ''
})
const editTemplate = ref({
  id: null,
  name: '',
  description: '',
  content: ''
})
const canCreateTemplate = computed(() => {
  return !!newTemplate.value.name.trim() && !!newTemplate.value.content.trim()
})

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

const editorsText = computed({
  get() {
    if (!Array.isArray(props.doc.editors)) {
      return ''
    }
    return props.doc.editors.join(', ')
  },
  set(value) {
    props.doc.editors = value
      .split(',')
      .map((v) => v.trim())
      .filter((v) => v.length > 0)
  }
})

const viewersText = computed({
  get() {
    if (!Array.isArray(props.doc.viewers)) {
      return ''
    }
    return props.doc.viewers.join(', ')
  },
  set(value) {
    props.doc.viewers = value
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

function setLockedAndSave(nextLocked) {
  if (!props.canEdit) {
    return
  }
  model.value.locked = nextLocked
  emit('save', model.value)
}

function setStatusAndSave(nextStatus) {
  model.value.status = nextStatus
  emit('save', model.value)
}

function toggleEditMode() {
  if (!props.canEdit) {
    alert('当前用户无编辑权限')
    return
  }
  if (isLocked.value && !isEditing.value) {
    alert('当前页面已锁定，请先解除锁定再编辑')
    return
  }
  isEditing.value = !isEditing.value
}

async function copyShareLink() {
  if (!props.shareLink) {
    return
  }
  try {
    await navigator.clipboard.writeText(props.shareLink)
    alert('分享链接已复制')
  } catch {
    alert('复制失败，请手动复制')
  }
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
  const id = Number(selectedTemplate.value)
  const item = props.templates.find((tpl) => tpl.id === id)
  if (!item) {
    return
  }
  model.value.content = item.content
}

function applyTemplateById(id) {
  const item = props.templates.find((tpl) => tpl.id === id)
  if (!item) {
    return
  }
  model.value.content = item.content
  selectedTemplate.value = String(id)
}

function createTemplate() {
  if (!canCreateTemplate.value) {
    return
  }
  emit('create-template', {
    name: newTemplate.value.name.trim(),
    description: newTemplate.value.description.trim(),
    content: newTemplate.value.content
  })
  newTemplate.value = {
    name: '',
    description: '',
    content: ''
  }
}

function startEditTemplate(template) {
  editingTemplateId.value = template.id
  editTemplate.value = {
    id: template.id,
    name: template.name || '',
    description: template.description || '',
    content: template.content || ''
  }
}

function cancelEditTemplate() {
  editingTemplateId.value = null
  editTemplate.value = {
    id: null,
    name: '',
    description: '',
    content: ''
  }
}

function saveEditTemplate() {
  if (!editTemplate.value.id || !editTemplate.value.name.trim() || !editTemplate.value.content.trim()) {
    return
  }
  emit('update-template', {
    id: editTemplate.value.id,
    name: editTemplate.value.name.trim(),
    description: editTemplate.value.description.trim(),
    content: editTemplate.value.content
  })
  cancelEditTemplate()
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

function priorityText(priority) {
  if (priority === 'HIGH') {
    return '高'
  }
  if (priority === 'LOW') {
    return '低'
  }
  return '中'
}
</script>
