<template>
  <div ref="editorPaneRef" class="editor-pane">
    <div class="pane-head">
      <h2>Page Editor</h2>
      <div class="view-switch" v-if="!isCreateMode">
        <button class="secondary" @click="toggleEditMode" :disabled="!canEdit">
          {{ isEditing ? '查看预览' : '编辑页面' }}
        </button>
      </div>
    </div>
    <div class="page-context-bar" v-if="!isCreateMode">
      <span class="context-chip mode">{{ isEditingSafe ? '编辑模式' : '阅读模式' }}</span>
      <span class="context-chip slug">slug: {{ model.slug || '-' }}</span>
      <span class="context-chip" v-if="model.parentSlug">父级: {{ model.parentSlug }}</span>
      <span class="context-chip status" :class="(model.status || 'DRAFT').toLowerCase()">{{ statusText(model.status) }}</span>
      <span class="context-chip visibility">{{ model.visibility === 'PRIVATE' ? '仅自己可见' : '空间可见' }}</span>
      <span class="context-chip">负责人: {{ model.assignee || '-' }}</span>
      <span class="context-chip">截止: {{ model.dueDate || '-' }}</span>
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

      <div class="meta-section">
        <button class="panel-fold-head section-fold-head" @click="basicMetaOpen = !basicMetaOpen">
          <strong>基础信息</strong>
          <span>{{ basicMetaOpen ? '收起 ▾' : '展开 ▸' }}</span>
        </button>
        <div v-show="basicMetaOpen">
          <div class="meta-grid">
            <label>
              Slug
              <input v-model="model.slug" :disabled="!isCreateMode || !isEditingSafe" />
            </label>
            <label>
              标题
              <input ref="titleInputRef" v-model="model.title" :disabled="!isEditingSafe" />
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
            <label>
              摘要
              <input v-model="model.summary" :disabled="!isEditingSafe" />
            </label>
          </div>
        </div>
      </div>

      <div class="meta-section">
        <button class="panel-fold-head section-fold-head" @click="permissionMetaOpen = !permissionMetaOpen">
          <strong>权限设置</strong>
          <span>{{ permissionMetaOpen ? '收起 ▾' : '展开 ▸' }}</span>
        </button>
        <div v-show="permissionMetaOpen">
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
        </div>
      </div>

      <div class="meta-section">
        <button class="panel-fold-head section-fold-head" @click="taskMetaOpen = !taskMetaOpen">
          <strong>任务属性</strong>
          <span>{{ taskMetaOpen ? '收起 ▾' : '展开 ▸' }}</span>
        </button>
        <div v-show="taskMetaOpen">
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
        </div>
      </div>

      <MdEditor v-model="model.content" :toolbars="toolbars" />
    </template>

    <template v-else>
      <div class="read-mode-head">
        <h3>{{ model.title || 'Untitled' }}</h3>
        <div class="read-mode-actions">
          <div class="read-width-presets">
            <button
              v-for="mode in readWidthModes"
              :key="mode.key"
              class="secondary tiny"
              :class="{ active: readWidthMode === mode.key }"
              @click="setReadWidthMode(mode.key)"
            >
              {{ mode.label }}
            </button>
          </div>
          <span class="read-shortcut-tip">快捷键 1/2/3/4</span>
          <button class="secondary small" @click="resetReadSidebarLayout">
            恢复默认布局
          </button>
          <button class="secondary small" @click="toggleReadDock">
            停靠{{ readPanelDock === 'right' ? '左侧' : '右侧' }}
          </button>
          <button class="secondary small" @click="readInfoOpen = !readInfoOpen">
            {{ readInfoOpen ? '隐藏页面信息' : '显示页面信息' }}
          </button>
        </div>
      </div>
      <div class="read-layout" :class="{ 'panel-left': readPanelDock === 'left' }">
        <aside class="read-side-panel" v-if="readInfoOpen">
          <div class="read-side-stack">
            <section
              v-for="card in visibleReadCards"
              :key="card.key"
              class="read-side-card"
              :class="{ collapsed: isReadCardCollapsed(card.key) }"
              :ref="(el) => setReadCardRef(card.key, el)"
            >
              <div class="read-card-tools">
                <span class="read-card-title">{{ card.title }}</span>
                <div class="read-card-order">
                  <button class="secondary tiny" @click="toggleReadCardCollapsed(card.key)">
                    {{ isReadCardCollapsed(card.key) ? '展开' : '折叠' }}
                  </button>
                  <button class="secondary tiny" :disabled="!canMoveReadCard(card.key, -1)" @click="moveReadCard(card.key, -1)">↑</button>
                  <button class="secondary tiny" :disabled="!canMoveReadCard(card.key, 1)" @click="moveReadCard(card.key, 1)">↓</button>
                </div>
              </div>

              <div class="read-card-body" v-show="!isReadCardCollapsed(card.key)">
              <template v-if="card.key === 'overview'">
                <div class="read-meta-row">
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
              </template>

              <template v-else-if="card.key === 'outline'">
                <button class="panel-fold-head section-fold-head compact" @click="readOutlineOpen = !readOutlineOpen">
                  <strong>页面目录（{{ outline.length }}）</strong>
                  <span>{{ readOutlineOpen ? '收起 ▾' : '展开 ▸' }}</span>
                </button>
                <div class="outline-default-action-tip" v-if="readOutlineOpen && outlineDefaultAction !== 'NONE'">
                  双击默认：{{ outlineDefaultAction === 'COPY_LINK' ? '复制标题链接' : '复制标题文本' }}
                </div>
                <div class="read-outline-filter" v-if="readOutlineOpen && outline.length">
                  <input v-model="readOutlineQuery" placeholder="过滤目录标题" />
                  <button class="secondary tiny" @click="readOutlineQuery = ''">清空</button>
                </div>
                <div class="read-outline-progress" v-if="readOutlineOpen && outline.length">
                  <div class="read-outline-progress-head">
                    <span>阅读进度</span>
                    <strong>{{ readProgressPercent }}%</strong>
                  </div>
                  <div class="read-outline-progress-track">
                    <div class="read-outline-progress-fill" :style="{ width: `${readProgressPercent}%` }"></div>
                  </div>
                </div>
                <ul class="read-outline-list" v-if="readOutlineOpen && filteredOutline.length">
                  <li
                    v-for="item in filteredOutline"
                    :key="item.id"
                    :class="{ active: isOutlineActive(item), done: isOutlineDone(item) }"
                    :style="{ '--outline-indent': `${(item.level - 1) * 14}px` }"
                    @click="jumpToOutline(item)"
                    @dblclick.prevent="runOutlineDefaultAction(item)"
                    @contextmenu.prevent="openOutlineMenu($event, item)"
                  >
                    <span class="read-outline-dot">•</span>
                    <span>{{ item.text }}</span>
                  </li>
                </ul>
                <div class="comment-empty" v-else-if="readOutlineOpen && outline.length">没有匹配的目录标题</div>
                <div class="comment-empty" v-else-if="readOutlineOpen">没有可识别标题（# ## ###）</div>
                <div
                  v-if="outlineMenu.open"
                  ref="outlineMenuRef"
                  class="outline-context-menu"
                  :style="{ left: `${outlineMenu.x}px`, top: `${outlineMenu.y}px` }"
                >
                  <button
                    v-for="(action, idx) in outlineMenuActions"
                    :key="action.key"
                    class="outline-context-item"
                    :class="{ active: outlineMenu.activeIndex === idx }"
                    @mouseenter="outlineMenu.activeIndex = idx"
                    @click="selectOutlineMenuAction(action.key)"
                  >
                    {{ action.label }}
                  </button>
                  <div class="outline-context-divider"></div>
                  <button class="outline-context-item" @click="setOutlineDefaultAction('COPY_LINK')">设为双击默认：复制链接</button>
                  <button class="outline-context-item" @click="setOutlineDefaultAction('COPY_TEXT')">设为双击默认：复制标题</button>
                  <button class="outline-context-item" @click="setOutlineDefaultAction('NONE')">清除双击默认</button>
                </div>
              </template>

              <template v-else-if="card.key === 'permission'">
                <button class="panel-fold-head section-fold-head compact" @click="readPermOpen = !readPermOpen">
                  <strong>权限与访问</strong>
                  <span>{{ readPermOpen ? '收起 ▾' : '展开 ▸' }}</span>
                </button>
                <div class="perm-tags" v-show="readPermOpen">
                  <span class="perm-chip owner">Owner: {{ model.owner || '-' }}</span>
                  <span class="perm-chip">Editors: {{ (model.editors || []).join(', ') || '-' }}</span>
                  <span class="perm-chip">Viewers: {{ (model.viewers || []).join(', ') || '-' }}</span>
                  <span class="perm-chip current">当前用户: {{ currentUser || '-' }}</span>
                  <span class="perm-chip share">{{ model.shareEnabled ? '分享已开启' : '未开启分享' }}</span>
                  <span class="perm-chip priority">优先级: {{ priorityText(model.priority) }}</span>
                  <span class="perm-chip">负责人: {{ model.assignee || '-' }}</span>
                  <span class="perm-chip">截止: {{ model.dueDate || '-' }}</span>
                </div>
                <div class="share-bar" v-if="model.shareEnabled && shareLink && readPermOpen">
                  <input :value="shareLink" readonly />
                  <button class="secondary small" @click="copyShareLink">复制链接</button>
                </div>
              </template>

              <template v-else-if="card.key === 'children'">
                <button class="panel-fold-head section-fold-head compact" @click="readChildrenOpen = !readChildrenOpen">
                  <strong>子页面导航（{{ childPages.length }}）</strong>
                  <span>{{ readChildrenOpen ? '收起 ▾' : '展开 ▸' }}</span>
                </button>
                <div class="child-pages" v-if="readChildrenOpen">
                  <div class="child-filter">
                    <input v-model="childQuery" placeholder="过滤子页面" />
                    <button class="secondary tiny" @click="childQuery = ''">清空</button>
                  </div>
                  <ul>
                    <li
                      v-for="item in filteredChildTreeRows"
                      :key="item.slug"
                      :style="{ '--child-indent': `${item.depth * 16}px` }"
                      @click="$emit('select-child', item.slug)"
                    >
                      <div class="child-main">
                        <button
                          v-if="item.childCount > 0"
                          class="child-expand"
                          @click.stop="toggleChildOpen(item.slug)"
                        >
                          {{ isChildOpen(item.slug) ? '▾' : '▸' }}
                        </button>
                        <span v-else class="child-expand placeholder">·</span>
                        <span class="child-tree-dot"></span>
                        <strong>{{ item.title }}</strong>
                        <span>{{ item.slug }}</span>
                      </div>
                      <div class="child-extra">
                        <em class="child-status" :class="(item.status || 'DRAFT').toLowerCase()">{{ statusText(item.status) }}</em>
                        <em class="child-priority" :class="(item.priority || 'MEDIUM').toLowerCase()">优先级: {{ priorityText(item.priority) }}</em>
                        <em class="child-due">截止: {{ item.dueDate || '-' }}</em>
                      </div>
                    </li>
                  </ul>
                  <div class="comment-empty" v-if="filteredChildTreeRows.length === 0">没有匹配的子页面</div>
                </div>
              </template>
              </div>
            </section>
          </div>
        </aside>
        <div
          ref="readPreviewRef"
          class="preview-only read-preview"
          :class="`read-width-${readWidthMode}`"
        >
          <MdPreview :model-value="model.content" />
        </div>
      </div>
    </template>

    <div class="actions">
      <div class="action-group primary">
        <button v-if="isEditingSafe" @click="$emit('save', model)">保存</button>
        <button
          v-if="!isCreateMode && isEditingSafe && model.status !== 'ARCHIVED'"
          class="secondary"
          @click="quickToggleStatus"
        >
          {{ model.status === 'PUBLISHED' ? '转草稿并保存' : '发布并保存' }}
        </button>
        <button
          v-if="!isCreateMode && isEditingSafe && model.status !== 'ARCHIVED'"
          class="secondary"
          @click="setStatusAndSave('ARCHIVED')"
        >
          归档
        </button>
        <button
          v-if="!isCreateMode && isEditingSafe && model.status === 'ARCHIVED'"
          class="secondary"
          @click="setStatusAndSave('DRAFT')"
        >
          恢复
        </button>
        <button v-if="!isCreateMode" class="secondary" :disabled="!canEdit" @click="$emit('create-child')">新建子页面</button>
      </div>
      <div class="action-group more" v-if="!isCreateMode" ref="actionMenuRef">
        <button class="secondary" :class="{ active: actionMenuOpen }" @click.stop="actionMenuOpen = !actionMenuOpen">
          更多操作 {{ actionMenuOpen ? '▴' : '▾' }}
        </button>
        <div class="action-menu" v-if="actionMenuOpen" @click.stop>
          <button class="secondary small" :disabled="!canEdit" @click="runMenuAction('toggle-lock')">
            {{ model.locked ? '解除锁定' : '锁定页面' }}
          </button>
          <button class="secondary small" :disabled="!canEdit" @click="runMenuAction('toggle-share')">
            {{ model.shareEnabled ? '关闭分享' : '开启分享' }}
          </button>
          <button v-if="model.shareEnabled" class="secondary small" :disabled="!canEdit" @click="runMenuAction('regen-share')">
            重置分享链接
          </button>
          <button class="danger small" :disabled="isCreateMode || !canEdit" @click="runMenuAction('delete')">删除页面</button>
        </div>
      </div>
    </div>

    <section class="comment-panel" v-if="!isCreateMode">
      <button class="panel-fold-head section-fold-head" @click="attachmentsOpen = !attachmentsOpen">
        <strong>附件（{{ attachments.length }}）</strong>
        <span>{{ attachmentsOpen ? '收起 ▾' : '展开 ▸' }}</span>
      </button>
      <div v-show="attachmentsOpen">
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
      </div>

      <button class="panel-fold-head section-fold-head" @click="commentsOpen = !commentsOpen">
        <strong>评论（{{ comments.length }}）</strong>
        <span>{{ commentsOpen ? '收起 ▾' : '展开 ▸' }}</span>
      </button>
      <div v-show="commentsOpen">
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
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { MdEditor, MdPreview } from 'md-editor-v3'

const props = defineProps({
  doc: {
    type: Object,
    required: true
  },
  outline: {
    type: Array,
    default: () => []
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
  'delete-template',
  'notify'
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
const editorPaneRef = ref(null)
const fileInput = ref(null)
const readPreviewRef = ref(null)
const titleInputRef = ref(null)
const selectedTemplate = ref('')
const templateCenterOpen = ref(false)
const editingTemplateId = ref(null)
const actionMenuOpen = ref(false)
const actionMenuRef = ref(null)
const attachmentsOpen = ref(true)
const commentsOpen = ref(true)
const basicMetaOpen = ref(true)
const permissionMetaOpen = ref(true)
const taskMetaOpen = ref(true)
const readPermOpen = ref(false)
const readChildrenOpen = ref(true)
const readOutlineOpen = ref(true)
const readOutlineQuery = ref('')
const readOutlineLevelFilter = ref(null)
const outlineDefaultAction = ref(loadOutlineDefaultAction())
const childQuery = ref('')
const activeOutlineText = ref('')
const readInfoOpen = ref(false)
const readPanelDock = ref('right')
const readCardOrder = ref(loadReadCardOrder())
const readCardCollapsed = ref(loadReadCardCollapsed())
const readWidthMode = ref(loadReadWidthMode())
const readCardRefs = ref({})
const outlineMenuRef = ref(null)
const childOpenMap = ref({})
const outlineMenu = ref({
  open: false,
  x: 0,
  y: 0,
  item: null,
  activeIndex: 0
})
const READ_PANEL_DOCK_KEY = 'ga-read-panel-dock'
const CHILD_OPEN_KEY = 'ga-read-child-open-state'
const READ_CARD_ORDER_KEY = 'ga-read-card-order'
const READ_CARD_COLLAPSED_KEY = 'ga-read-card-collapsed'
const READ_WIDTH_KEY = 'ga-read-width-mode'
const OUTLINE_DEFAULT_ACTION_KEY = 'ga-outline-default-action'
let readScrollRaf = null
let outlineMenuRaf = null
const readWidthModes = [
  { key: 'compact', label: '紧凑' },
  { key: 'standard', label: '标准' },
  { key: 'comfortable', label: '舒适' }
]
const outlineMenuActions = [
  { key: 'copy-text', label: '复制标题文本' },
  { key: 'copy-link', label: '复制标题链接' },
  { key: 'filter-level', label: '仅看同级标题' },
  { key: 'clear-level', label: '显示全部标题' }
]

const activeOutlineIndex = computed(() => {
  const activeKey = normalizeText(activeOutlineText.value)
  if (!activeKey) {
    return -1
  }
  return props.outline.findIndex((item) => normalizeText(item.text) === activeKey)
})

const readProgressPercent = computed(() => {
  const total = props.outline.length
  if (!total) {
    return 0
  }
  if (activeOutlineIndex.value < 0) {
    return 0
  }
  const ratio = (activeOutlineIndex.value + 1) / total
  return Math.max(0, Math.min(100, Math.round(ratio * 100)))
})

const filteredOutline = computed(() => {
  const q = readOutlineQuery.value.trim().toLowerCase()
  const source = readOutlineLevelFilter.value
    ? props.outline.filter((item) => item.level === readOutlineLevelFilter.value)
    : props.outline
  if (!q) {
    return source
  }
  return source.filter((item) => (item.text || '').toLowerCase().includes(q))
})

const childTreeRows = computed(() => {
  const rows = []
  const openStack = []
  for (const item of props.childPages) {
    while (openStack.length > item.depth) {
      openStack.pop()
    }
    const ancestorsOpen = openStack.every(Boolean)
    if (!ancestorsOpen) {
      openStack.push(false)
      continue
    }
    rows.push(item)
    const open = item.childCount > 0 ? isChildOpen(item.slug) : true
    openStack.push(open)
  }
  return rows
})

const filteredChildTreeRows = computed(() => {
  const q = childQuery.value.trim().toLowerCase()
  if (!q) {
    return childTreeRows.value
  }

  const bySlug = new Map(props.childPages.map((item) => [item.slug, item]))
  const include = new Set()
  props.childPages.forEach((item) => {
    const matched = (item.title || '').toLowerCase().includes(q) || (item.slug || '').toLowerCase().includes(q)
    if (!matched) {
      return
    }
    include.add(item.slug)
    let parent = item.parentSlug
    while (parent && bySlug.has(parent)) {
      include.add(parent)
      parent = bySlug.get(parent)?.parentSlug
    }
  })

  return childTreeRows.value.filter((item) => include.has(item.slug))
})

const visibleReadCards = computed(() => {
  const definitions = new Map([
    ['overview', { key: 'overview', title: '概览' }],
    ['outline', { key: 'outline', title: '目录' }],
    ['permission', { key: 'permission', title: '权限' }],
    ['children', { key: 'children', title: '子页面' }]
  ])
  const order = normalizeReadCardOrder(readCardOrder.value)
  const cards = []
  order.forEach((key) => {
    if (key === 'children' && !props.childPages.length) {
      return
    }
    const card = definitions.get(key)
    if (card) {
      cards.push(card)
    }
  })
  return cards
})
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
    actionMenuOpen.value = false
    readInfoOpen.value = false
    readPermOpen.value = false
    readChildrenOpen.value = true
    readOutlineLevelFilter.value = null
    outlineMenu.value = { open: false, x: 0, y: 0, item: null, activeIndex: 0 }
    childQuery.value = ''
    childOpenMap.value = loadChildOpenState(id)
  },
  { immediate: true }
)

watch(
  () => props.childPages,
  (items) => {
    const valid = new Set((items || []).map((item) => item.slug))
    const next = {}
    Object.entries(childOpenMap.value).forEach(([slug, open]) => {
      if (valid.has(slug)) {
        next[slug] = open
      }
    })
    childOpenMap.value = next
  },
  { deep: true }
)

watch(
  [() => props.doc.id, childOpenMap],
  ([id, map]) => {
    persistChildOpenState(id, map)
  },
  { deep: true }
)

watch(readCardOrder, (order) => {
  persistReadCardOrder(order)
}, { deep: true })

watch(readCardCollapsed, (state) => {
  persistReadCardCollapsed(state)
}, { deep: true })

watch(readWidthMode, (mode) => {
  persistReadWidthMode(mode)
})

watch(outlineDefaultAction, (value) => {
  persistOutlineDefaultAction(value)
})

watch([isCreateMode, isEditingSafe], async ([createMode, editable]) => {
  if (!createMode || !editable) {
    return
  }
  await nextTick()
  if (titleInputRef.value && typeof titleInputRef.value.focus === 'function') {
    titleInputRef.value.focus()
    titleInputRef.value.select?.()
  }
}, { immediate: true })

function onGlobalClick(event) {
  closeOutlineMenu()
  if (!actionMenuRef.value) {
    return
  }
  if (!actionMenuRef.value.contains(event.target)) {
    actionMenuOpen.value = false
  }
}

onMounted(() => {
  if (typeof window !== 'undefined') {
    const rawDock = window.localStorage.getItem(READ_PANEL_DOCK_KEY)
    if (rawDock === 'left' || rawDock === 'right') {
      readPanelDock.value = rawDock
    }
    window.addEventListener('click', onGlobalClick)
    window.addEventListener('keydown', onGlobalKeydown)
    window.addEventListener('resize', onViewportChange)
    window.addEventListener('scroll', onViewportChange, true)
  }
  if (editorPaneRef.value) {
    editorPaneRef.value.addEventListener('scroll', onReadScroll, { passive: true })
  }
})

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('click', onGlobalClick)
    window.removeEventListener('keydown', onGlobalKeydown)
    window.removeEventListener('resize', onViewportChange)
    window.removeEventListener('scroll', onViewportChange, true)
  }
  if (editorPaneRef.value) {
    editorPaneRef.value.removeEventListener('scroll', onReadScroll)
  }
  if (readScrollRaf !== null && typeof window !== 'undefined') {
    window.cancelAnimationFrame(readScrollRaf)
    readScrollRaf = null
  }
  if (outlineMenuRaf !== null && typeof window !== 'undefined') {
    window.cancelAnimationFrame(outlineMenuRaf)
    outlineMenuRaf = null
  }
})

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

function runMenuAction(action) {
  actionMenuOpen.value = false
  if (action === 'toggle-lock') {
    setLockedAndSave(!model.value.locked)
    return
  }
  if (action === 'toggle-share') {
    emit('toggle-share', !model.value.shareEnabled)
    return
  }
  if (action === 'regen-share') {
    emit('regenerate-share')
    return
  }
  if (action === 'delete') {
    emit('delete', model.value.slug)
  }
}

function toggleEditMode() {
  if (!props.canEdit) {
    emit('notify', { type: 'error', message: '当前用户无编辑权限' })
    return
  }
  if (isLocked.value && !isEditing.value) {
    emit('notify', { type: 'error', message: '当前页面已锁定，请先解除锁定再编辑' })
    return
  }
  isEditing.value = !isEditing.value
}

function toggleReadDock() {
  readPanelDock.value = readPanelDock.value === 'right' ? 'left' : 'right'
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(READ_PANEL_DOCK_KEY, readPanelDock.value)
  }
}

function isChildOpen(slug) {
  return childOpenMap.value[slug] !== false
}

function toggleChildOpen(slug) {
  childOpenMap.value = {
    ...childOpenMap.value,
    [slug]: !isChildOpen(slug)
  }
}

function canMoveReadCard(cardKey, delta) {
  const keys = visibleReadCards.value.map((card) => card.key)
  const index = keys.indexOf(cardKey)
  if (index < 0) {
    return false
  }
  const target = index + delta
  return target >= 0 && target < keys.length
}

function moveReadCard(cardKey, delta) {
  const keys = visibleReadCards.value.map((card) => card.key)
  const index = keys.indexOf(cardKey)
  const target = index + delta
  if (index < 0 || target < 0 || target >= keys.length) {
    return
  }
  const targetKey = keys[target]
  const full = normalizeReadCardOrder(readCardOrder.value)
  const left = full.indexOf(cardKey)
  const right = full.indexOf(targetKey)
  if (left < 0 || right < 0) {
    return
  }
  const next = full.slice()
  const temp = next[left]
  next[left] = next[right]
  next[right] = temp
  readCardOrder.value = next
}

function isReadCardCollapsed(cardKey) {
  return readCardCollapsed.value[cardKey] === true
}

function toggleReadCardCollapsed(cardKey) {
  readCardCollapsed.value = {
    ...readCardCollapsed.value,
    [cardKey]: !isReadCardCollapsed(cardKey)
  }
}

function setReadCardRef(cardKey, el) {
  if (el) {
    readCardRefs.value = {
      ...readCardRefs.value,
      [cardKey]: el
    }
    return
  }
  const next = { ...readCardRefs.value }
  delete next[cardKey]
  readCardRefs.value = next
}

function resetReadSidebarLayout() {
  readCardOrder.value = normalizeReadCardOrder([])
  readCardCollapsed.value = {}
  readWidthMode.value = 'standard'
}

function setReadWidthMode(mode) {
  if (!readWidthModes.some((item) => item.key === mode)) {
    return
  }
  readWidthMode.value = mode
}

function onGlobalKeydown(event) {
  if (event.defaultPrevented) {
    return
  }
  if (outlineMenu.value.open) {
    handleOutlineMenuKeydown(event)
    return
  }
  if (isCreateMode.value || isEditingSafe.value) {
    return
  }
  if (event.metaKey || event.ctrlKey || event.altKey || event.shiftKey) {
    return
  }
  if (!['1', '2', '3', '4'].includes(event.key)) {
    return
  }
  const target = event.target
  if (isInteractiveTarget(target)) {
    return
  }
  const index = Number(event.key) - 1
  const card = visibleReadCards.value[index]
  if (!card) {
    return
  }
  event.preventDefault()
  if (!readInfoOpen.value) {
    readInfoOpen.value = true
  }
  if (isReadCardCollapsed(card.key)) {
    toggleReadCardCollapsed(card.key)
  }
  if (typeof window !== 'undefined') {
    window.requestAnimationFrame(() => {
      const el = readCardRefs.value[card.key]
      if (el && typeof el.scrollIntoView === 'function') {
        el.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
      }
    })
  }
}

function isInteractiveTarget(target) {
  const tag = target?.tagName?.toUpperCase?.() || ''
  if (['INPUT', 'TEXTAREA', 'SELECT', 'BUTTON'].includes(tag)) {
    return true
  }
  return !!target?.isContentEditable
}

function normalizeReadCardOrder(value) {
  const defaults = ['overview', 'outline', 'permission', 'children']
  const source = Array.isArray(value) ? value : []
  const unique = []
  source.forEach((item) => {
    if (defaults.includes(item) && !unique.includes(item)) {
      unique.push(item)
    }
  })
  defaults.forEach((item) => {
    if (!unique.includes(item)) {
      unique.push(item)
    }
  })
  return unique
}

function loadReadCardOrder() {
  if (typeof window === 'undefined') {
    return ['overview', 'outline', 'permission', 'children']
  }
  try {
    const raw = window.localStorage.getItem(READ_CARD_ORDER_KEY)
    if (!raw) {
      return ['overview', 'outline', 'permission', 'children']
    }
    return normalizeReadCardOrder(JSON.parse(raw))
  } catch {
    return ['overview', 'outline', 'permission', 'children']
  }
}

function persistReadCardOrder(order) {
  if (typeof window === 'undefined') {
    return
  }
  try {
    window.localStorage.setItem(READ_CARD_ORDER_KEY, JSON.stringify(normalizeReadCardOrder(order)))
  } catch {
    // ignore persistence failures
  }
}

function loadReadCardCollapsed() {
  if (typeof window === 'undefined') {
    return {}
  }
  try {
    const raw = window.localStorage.getItem(READ_CARD_COLLAPSED_KEY)
    if (!raw) {
      return {}
    }
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') {
      return {}
    }
    return parsed
  } catch {
    return {}
  }
}

function persistReadCardCollapsed(state) {
  if (typeof window === 'undefined') {
    return
  }
  try {
    window.localStorage.setItem(READ_CARD_COLLAPSED_KEY, JSON.stringify(state || {}))
  } catch {
    // ignore persistence failures
  }
}

function loadReadWidthMode() {
  if (typeof window === 'undefined') {
    return 'standard'
  }
  const raw = window.localStorage.getItem(READ_WIDTH_KEY)
  if (raw === 'compact' || raw === 'standard' || raw === 'comfortable') {
    return raw
  }
  return 'standard'
}

function persistReadWidthMode(mode) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(READ_WIDTH_KEY, mode)
}

function loadOutlineDefaultAction() {
  if (typeof window === 'undefined') {
    return 'NONE'
  }
  const raw = window.localStorage.getItem(OUTLINE_DEFAULT_ACTION_KEY)
  if (raw === 'COPY_LINK' || raw === 'COPY_TEXT' || raw === 'NONE') {
    return raw
  }
  return 'NONE'
}

function persistOutlineDefaultAction(value) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(OUTLINE_DEFAULT_ACTION_KEY, value)
}

function loadChildOpenState(docId) {
  if (typeof window === 'undefined' || !docId) {
    return {}
  }
  try {
    const raw = window.localStorage.getItem(CHILD_OPEN_KEY)
    if (!raw) {
      return {}
    }
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') {
      return {}
    }
    const key = String(docId)
    const value = parsed[key]
    if (!value || typeof value !== 'object') {
      return {}
    }
    return value
  } catch {
    return {}
  }
}

function persistChildOpenState(docId, map) {
  if (typeof window === 'undefined' || !docId) {
    return
  }
  try {
    const raw = window.localStorage.getItem(CHILD_OPEN_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    const safeParsed = parsed && typeof parsed === 'object' ? parsed : {}
    safeParsed[String(docId)] = map || {}
    window.localStorage.setItem(CHILD_OPEN_KEY, JSON.stringify(safeParsed))
  } catch {
    // ignore persistence failures
  }
}

function normalizeText(value) {
  return (value || '').replace(/\s+/g, ' ').trim().toLowerCase()
}

function jumpToOutline(item) {
  const container = readPreviewRef.value
  if (!container || !item?.text) {
    return
  }
  const headings = Array.from(container.querySelectorAll('h1, h2, h3, h4'))
  const target = headings.find((node) => normalizeText(node.textContent) === normalizeText(item.text))
  if (!target) {
    emit('notify', { type: 'error', message: '未在正文中找到该标题' })
    return
  }
  activeOutlineText.value = item.text
  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openOutlineMenu(event, item) {
  outlineMenu.value = {
    open: true,
    x: event.clientX,
    y: event.clientY,
    item,
    activeIndex: 0
  }
  nextTick(() => {
    adjustOutlineMenuPosition()
  })
}

function onReadScroll() {
  if (outlineMenu.value.open) {
    onViewportChange()
  }
  if (readScrollRaf !== null || isEditingSafe.value || !readPreviewRef.value) {
    return
  }
  readScrollRaf = window.requestAnimationFrame(() => {
    readScrollRaf = null
    updateActiveOutlineByScroll()
  })
}

function updateActiveOutlineByScroll() {
  const container = readPreviewRef.value
  const pane = editorPaneRef.value
  if (!container || !pane) {
    return
  }
  const headings = Array.from(container.querySelectorAll('h1, h2, h3, h4'))
  if (!headings.length) {
    activeOutlineText.value = ''
    return
  }
  const paneTop = pane.getBoundingClientRect().top
  let current = headings[0]
  for (const heading of headings) {
    const delta = heading.getBoundingClientRect().top - paneTop
    if (delta <= 110) {
      current = heading
    } else {
      break
    }
  }
  activeOutlineText.value = (current.textContent || '').trim()
}

function isOutlineActive(item) {
  return normalizeText(item?.text) === normalizeText(activeOutlineText.value)
}

function isOutlineDone(item) {
  const index = props.outline.findIndex((node) => node.id === item.id)
  return activeOutlineIndex.value >= 0 && index >= 0 && index < activeOutlineIndex.value
}

async function copyOutlineText() {
  await copyOutlineTextByItem(outlineMenu.value.item)
  closeOutlineMenu()
}

async function copyOutlineLink() {
  await copyOutlineLinkByItem(outlineMenu.value.item)
  closeOutlineMenu()
}

async function copyOutlineTextByItem(item) {
  if (!item?.text) {
    return
  }
  try {
    await navigator.clipboard.writeText(item.text)
    emit('notify', { type: 'success', message: '标题文本已复制' })
  } catch {
    emit('notify', { type: 'error', message: '复制失败，请手动复制' })
  }
}

async function copyOutlineLinkByItem(item) {
  if (!item?.text) {
    return
  }
  const headingId = findHeadingIdByText(item.text) || headingSlug(item.text)
  const base = typeof window !== 'undefined' ? window.location.origin + window.location.pathname : ''
  const pagePart = model.value.slug ? `?page=${encodeURIComponent(model.value.slug)}` : ''
  const hashPart = headingId ? `#${encodeURIComponent(headingId)}` : ''
  const link = `${base}${pagePart}${hashPart}`
  try {
    await navigator.clipboard.writeText(link)
    emit('notify', { type: 'success', message: '标题链接已复制' })
  } catch {
    emit('notify', { type: 'error', message: '复制失败，请手动复制' })
  }
}

function filterOutlineByLevel() {
  const item = outlineMenu.value.item
  readOutlineLevelFilter.value = item?.level || null
  closeOutlineMenu()
}

function clearOutlineLevelFilter() {
  readOutlineLevelFilter.value = null
  closeOutlineMenu()
}

function findHeadingIdByText(text) {
  const container = readPreviewRef.value
  if (!container || !text) {
    return ''
  }
  const headings = Array.from(container.querySelectorAll('h1, h2, h3, h4'))
  const found = headings.find((node) => normalizeText(node.textContent) === normalizeText(text))
  return found?.id || ''
}

function headingSlug(text) {
  return (text || '')
    .toLowerCase()
    .trim()
    .replace(/[^\w\u4e00-\u9fa5\s-]/g, '')
    .replace(/\s+/g, '-')
}

function closeOutlineMenu() {
  outlineMenu.value = {
    ...outlineMenu.value,
    open: false,
    activeIndex: 0
  }
}

function onViewportChange() {
  if (!outlineMenu.value.open || typeof window === 'undefined') {
    return
  }
  if (outlineMenuRaf !== null) {
    return
  }
  outlineMenuRaf = window.requestAnimationFrame(() => {
    outlineMenuRaf = null
    adjustOutlineMenuPosition()
  })
}

function adjustOutlineMenuPosition() {
  const menu = outlineMenuRef.value
  if (!menu || typeof window === 'undefined') {
    return
  }
  const rect = menu.getBoundingClientRect()
  const margin = 8
  let nextX = outlineMenu.value.x
  let nextY = outlineMenu.value.y
  const overflowRight = rect.right > window.innerWidth - margin
  const overflowBottom = rect.bottom > window.innerHeight - margin
  if (overflowRight) {
    nextX = Math.max(margin, nextX - rect.width)
  }
  if (overflowBottom) {
    nextY = Math.max(margin, nextY - rect.height)
  }
  nextX = Math.min(Math.max(margin, nextX), Math.max(margin, window.innerWidth - rect.width - margin))
  nextY = Math.min(Math.max(margin, nextY), Math.max(margin, window.innerHeight - rect.height - margin))
  if (nextX !== outlineMenu.value.x || nextY !== outlineMenu.value.y) {
    outlineMenu.value = {
      ...outlineMenu.value,
      x: nextX,
      y: nextY
    }
  }
}

function handleOutlineMenuKeydown(event) {
  if (!['ArrowUp', 'ArrowDown', 'Enter', 'Escape'].includes(event.key)) {
    return
  }
  event.preventDefault()
  const total = outlineMenuActions.length
  if (!total) {
    return
  }
  if (event.key === 'Escape') {
    closeOutlineMenu()
    return
  }
  if (event.key === 'ArrowDown') {
    outlineMenu.value.activeIndex = (outlineMenu.value.activeIndex + 1) % total
    return
  }
  if (event.key === 'ArrowUp') {
    outlineMenu.value.activeIndex = (outlineMenu.value.activeIndex - 1 + total) % total
    return
  }
  if (event.key === 'Enter') {
    const action = outlineMenuActions[outlineMenu.value.activeIndex]
    if (action) {
      selectOutlineMenuAction(action.key)
    }
  }
}

function selectOutlineMenuAction(actionKey) {
  if (actionKey === 'copy-text') {
    copyOutlineText()
    return
  }
  if (actionKey === 'copy-link') {
    copyOutlineLink()
    return
  }
  if (actionKey === 'filter-level') {
    filterOutlineByLevel()
    return
  }
  if (actionKey === 'clear-level') {
    clearOutlineLevelFilter()
  }
}

function setOutlineDefaultAction(action) {
  if (!['NONE', 'COPY_LINK', 'COPY_TEXT'].includes(action)) {
    return
  }
  outlineDefaultAction.value = action
  closeOutlineMenu()
}

function runOutlineDefaultAction(item) {
  if (outlineDefaultAction.value === 'COPY_LINK') {
    copyOutlineLinkByItem(item)
    return
  }
  if (outlineDefaultAction.value === 'COPY_TEXT') {
    copyOutlineTextByItem(item)
    return
  }
  jumpToOutline(item)
}

watch(
  () => props.doc.id,
  () => {
    readOutlineQuery.value = ''
    readOutlineLevelFilter.value = null
    activeOutlineText.value = ''
    if (typeof window === 'undefined') {
      return
    }
    window.setTimeout(() => {
      updateActiveOutlineByScroll()
    }, 30)
  }
)

async function copyShareLink() {
  if (!props.shareLink) {
    return
  }
  try {
    await navigator.clipboard.writeText(props.shareLink)
    emit('notify', { type: 'success', message: '分享链接已复制' })
  } catch {
    emit('notify', { type: 'error', message: '复制失败，请手动复制' })
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
