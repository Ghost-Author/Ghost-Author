<template>
  <div class="app-shell">
    <div v-if="!isAuthenticated" class="login-shell">
      <div class="login-card">
        <div class="login-brand">
          <span class="brand-mark">GA</span>
          <div>
            <strong>Ghost Author</strong>
            <p>请先登录后再访问知识库</p>
          </div>
        </div>
        <form class="login-form" @submit.prevent="submitLogin">
          <label>
            用户名
            <input v-model.trim="loginForm.username" placeholder="例如：liupeng" autocomplete="username" />
          </label>
          <label>
            密码
            <input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
          </label>
          <label class="login-check">
            <input v-model="loginForm.rememberMe" type="checkbox" />
            <span>记住我（更长登录有效期）</span>
          </label>
          <p v-if="loginError" class="login-error">{{ loginError }}</p>
          <button type="submit">登录并进入</button>
        </form>
      </div>
    </div>
    <template v-else>
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">GA</span>
        <div>
          <strong>Knowledge Space</strong>
        </div>
      </div>
      <div class="topbar-right">
        <span class="topbar-user">当前用户：{{ currentUser }}</span>
        <span class="topbar-user role">角色：{{ currentUserRole }}</span>
        <span class="shortcut-hint">⌘/Ctrl+K 搜索 · ⌘/Ctrl+S 保存 · Alt+0/1/2/3/4/5/6/7/8/9</span>
        <button
          class="topbar-user tree-focus"
          :class="{ on: treeFocusPathEnabled }"
          type="button"
          title="点击切换页面树路径聚焦（Alt+0）"
          @click="toggleTreeFocusPathFromTopbar"
        >
          路径聚焦：{{ treeFocusPathEnabled ? '开' : '关' }}
        </button>
        <button class="secondary tiny" @click="openHome">空间首页</button>
        <button class="secondary tiny" @click="toggleRightPanel">
          {{ rightPanelOpen ? '收起右栏' : '展开右栏' }}
        </button>
        <button class="secondary tiny" :class="{ active: focusMode }" @click="toggleFocusMode">
          {{ focusMode ? '退出专注' : '专注模式' }}
        </button>
        <button class="secondary tiny" @click="openPasswordDialog">修改密码</button>
        <button v-if="canManageUsers" class="secondary tiny" @click="openUserAdmin">用户管理</button>
        <button class="secondary tiny" @click="logout">退出登录</button>
        <div class="topbar-badge">{{ visibleDocs.length }} pages</div>
      </div>
    </header>

    <div class="breadcrumb" v-show="!focusMode">
      <span>🏠 Space</span>
      <span>/</span>
      <span>Knowledge</span>
      <template v-for="item in breadcrumbPath" :key="item.slug || item.title">
        <span>/</span>
        <strong v-if="item.slug === activeSlug">{{ item.title }}</strong>
        <button v-else class="crumb-link" @click="openBreadcrumb(item.slug)">{{ item.title }}</button>
      </template>
    </div>

    <div class="layout" :class="{ 'right-collapsed': !rightPanelOpen, 'focus-mode': focusMode }" :style="layoutStyle">
      <DocList
        class="layout-doclist"
        ref="docListRef"
        :docs="visibleDocs"
        :active-slug="activeSlug"
        :pinned="pinned"
        :favorites="favorites"
        :recent="recent"
        :recent-meta="recentMeta"
        :auto-clean-recent-older="autoCleanRecentOlder"
        :current-user="currentUser"
        @create="createNewDoc"
        @search="searchDocs"
        @select="loadDoc"
        @toggle-favorite="toggleFavorite"
        @toggle-pin="togglePinned"
        @move="moveDoc"
        @reorder="reorderDoc"
        @quick-action="handleDocQuickAction"
        @bulk-action="handleDocBulkAction"
        @reorder-quick="handleQuickCollectionReorder"
        @quick-collection-action="handleQuickCollectionAction"
        @tree-focus-change="handleTreeFocusChange"
      />
      <div
        class="layout-splitter"
        role="separator"
        aria-label="调整左侧栏宽度"
        @mousedown="startLeftResize"
      ></div>

      <div class="layout-main">
        <SpaceHome
          v-if="showHome"
          :stats="homeStats"
          :recent-docs="homeRecentDocs"
          :favorite-docs="homeFavoriteDocs"
          :overdue-docs="homeOverdueDocs"
          :today-docs="homeTodayDueDocs"
          :assignee-board="homeAssigneeBoard"
          :my-todo-docs="myTodoDocs"
          @create="createNewDoc"
          @select="loadDoc"
          @open-my-todo="openMyTodoView"
        />
        <EditorPane
          v-else
          :doc="currentDoc"
          :is-favorite="!!activeSlug && favorites.includes(activeSlug)"
          :right-panel-open="rightPanelOpen"
          :prev-sibling-slug="siblingNav.prevSlug"
          :prev-sibling-title="siblingNav.prevTitle"
          :next-sibling-slug="siblingNav.nextSlug"
          :next-sibling-title="siblingNav.nextTitle"
          :outline="pageOutline"
          :comments="comments"
          :attachments="attachments"
          :child-pages="childPages"
          :current-user="currentUser"
          :can-edit="currentCanEdit"
          :can-manage-templates="canManageTemplates"
          :share-link="currentShareLink"
          :templates="templates"
          @save="saveDoc"
          @delete="deleteDoc"
          @add-comment="addComment"
          @delete-comment="deleteComment"
          @upload-attachment="uploadAttachment"
          @delete-attachment="deleteAttachment"
          @insert-attachment="insertAttachment"
          @create-child="createChildPage"
          @create-child-with-template="createChildPageWithTemplate"
          @select-child="loadDoc"
          @open-parent="loadDoc"
          @open-sibling="loadDoc"
          @toggle-right-panel="toggleRightPanel"
          @duplicate-page="duplicateCurrentPage"
          @toggle-favorite="toggleFavorite"
          @toggle-share="toggleShare"
          @regenerate-share="regenerateShare"
          @create-template="createTemplate"
          @update-template="updateTemplate"
          @delete-template="deleteTemplate"
          @notify="handleEditorNotify"
          @quick-filter-permission="handlePermissionQuickFilter"
        />
      </div>

      <VersionHistory
        v-if="rightPanelOpen"
        class="layout-version"
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

    <div class="command-overlay" v-if="commandOpen" @click.self="closeCommand">
      <div class="command-panel">
        <div class="command-head">
          <input
            v-model="commandQuery"
            placeholder="搜索页面标题或 slug，回车快速打开"
            @keydown="onCommandInputKeydown"
          />
        </div>
        <ul class="command-list">
          <li
            v-for="(item, idx) in commandResults"
            :key="item.slug"
            :class="{ active: idx === commandActiveIndex }"
            @mouseenter="commandActiveIndex = idx"
            @click="selectCommandDoc(item.slug)"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.slug }}</span>
          </li>
          <li v-if="commandResults.length === 0" class="command-empty">没有匹配页面</li>
        </ul>
      </div>
    </div>

    <div v-if="toast.show" class="app-toast" :class="toast.type">
      {{ toast.message }}
    </div>

    <div v-if="confirmDialog.open" class="confirm-overlay" @click.self="resolveConfirm(false)">
      <div class="confirm-panel">
        <h4>{{ confirmDialog.title }}</h4>
        <p>{{ confirmDialog.message }}</p>
        <div class="confirm-actions">
          <button class="secondary" @click="resolveConfirm(false)">{{ confirmDialog.cancelText }}</button>
          <button :class="confirmDialog.danger ? 'danger' : ''" @click="resolveConfirm(true)">
            {{ confirmDialog.confirmText }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="promptDialog.open" class="confirm-overlay" @click.self="resolvePrompt(null)">
      <div class="confirm-panel">
        <h4>{{ promptDialog.title }}</h4>
        <p>{{ promptDialog.message }}</p>
        <input
          ref="promptInputRef"
          v-model="promptDialog.value"
          :placeholder="promptDialog.placeholder"
          @keydown.enter.prevent="resolvePrompt(promptDialog.value)"
          @keydown.esc.prevent="resolvePrompt(null)"
        />
        <div class="confirm-actions">
          <button class="secondary" @click="resolvePrompt(null)">{{ promptDialog.cancelText }}</button>
          <button @click="resolvePrompt(promptDialog.value)">{{ promptDialog.confirmText }}</button>
        </div>
      </div>
    </div>

    <div v-if="bulkResult.open" class="confirm-overlay" @click.self="bulkResult.open = false">
      <div class="confirm-panel bulk-result-panel">
        <h4>批量操作结果</h4>
        <p>{{ bulkResult.actionLabel }}：成功 {{ bulkResult.updated.length }}，跳过 {{ bulkResult.skipped.length }}</p>
        <div class="bulk-result-columns">
          <div>
            <h5>成功</h5>
            <ul class="bulk-result-list">
              <li v-for="item in bulkResult.updated" :key="`ok-${item}`">{{ item }}</li>
              <li v-if="bulkResult.updated.length === 0" class="bulk-result-empty">无</li>
            </ul>
          </div>
          <div>
            <h5>跳过</h5>
            <ul class="bulk-result-list">
              <li v-for="item in bulkResult.skipped" :key="`skip-${item.slug}`">
                <strong>{{ item.slug }}</strong>
                <span>{{ item.reason }}</span>
              </li>
              <li v-if="bulkResult.skipped.length === 0" class="bulk-result-empty">无</li>
            </ul>
          </div>
        </div>
        <div class="confirm-actions">
          <button @click="bulkResult.open = false">我知道了</button>
        </div>
      </div>
    </div>

    <div v-if="userAdminOpen" class="confirm-overlay" @click.self="userAdminOpen = false">
      <div class="confirm-panel user-admin-panel">
        <h4>用户管理</h4>
        <div class="user-admin-head">
          <p>仅管理员可见。支持 `ADMIN / EDITOR / VIEWER`。</p>
          <button class="secondary tiny" @click="loadAuthUsers">刷新</button>
        </div>
        <div class="user-admin-form">
          <input v-model.trim="authUserForm.username" placeholder="用户名" />
          <input v-model="authUserForm.password" type="password" placeholder="密码（留空则不改）" />
          <select v-model="authUserForm.role">
            <option value="ADMIN">ADMIN</option>
            <option value="EDITOR">EDITOR</option>
            <option value="VIEWER">VIEWER</option>
          </select>
          <button :disabled="authUserLoading" @click="saveAuthUser">保存用户</button>
        </div>
        <ul class="user-admin-list">
          <li v-for="item in authUsers" :key="item.username">
            <div class="user-admin-item">
              <strong>{{ item.username }}</strong>
              <span class="user-role">{{ item.role }}</span>
            </div>
            <div class="user-admin-actions">
              <button class="secondary tiny" :disabled="authUserLoading" @click="prefillAuthUser(item)">编辑</button>
              <button class="danger tiny" :disabled="authUserLoading" @click="deleteAuthUser(item.username)">删除</button>
            </div>
          </li>
          <li v-if="authUsers.length === 0" class="bulk-result-empty">暂无用户</li>
        </ul>
        <div class="confirm-actions">
          <button class="secondary" @click="userAdminOpen = false">关闭</button>
        </div>
      </div>
    </div>

    <div v-if="forcePwdDialog.open" class="confirm-overlay">
      <div class="confirm-panel">
        <h4>请先修改初始密码</h4>
        <p>为了账号安全，首次登录或管理员重置密码后需要先修改密码。</p>
        <input v-model="forcePwdDialog.newPassword" type="password" placeholder="新密码（至少8位，含字母+数字）" />
        <input v-model="forcePwdDialog.confirmPassword" type="password" placeholder="确认新密码" />
        <p v-if="forcePwdDialog.error" class="login-error">{{ forcePwdDialog.error }}</p>
        <div class="confirm-actions">
          <button :disabled="forcePwdDialog.saving" @click="submitForcePasswordChange">保存新密码</button>
        </div>
      </div>
    </div>

    <div v-if="passwordDialog.open" class="confirm-overlay" @click.self="passwordDialog.open = false">
      <div class="confirm-panel">
        <h4>修改密码</h4>
        <p>请输入当前密码和新密码。</p>
        <input v-model="passwordDialog.currentPassword" type="password" placeholder="当前密码" />
        <input v-model="passwordDialog.newPassword" type="password" placeholder="新密码（至少8位，含字母+数字）" />
        <input v-model="passwordDialog.confirmPassword" type="password" placeholder="确认新密码" />
        <p v-if="passwordDialog.error" class="login-error">{{ passwordDialog.error }}</p>
        <div class="confirm-actions">
          <button class="secondary" :disabled="passwordDialog.saving" @click="passwordDialog.open = false">取消</button>
          <button :disabled="passwordDialog.saving" @click="submitPasswordChange">保存新密码</button>
        </div>
      </div>
    </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { api, loadApiAuthToken, setApiAuthToken, setUnauthorizedHandler } from './api/client'
import DocList from './components/DocList.vue'
import EditorPane from './components/EditorPane.vue'
import SpaceHome from './components/SpaceHome.vue'
import VersionHistory from './components/VersionHistory.vue'

const FAVORITES_KEY = 'ga-favorites'
const RECENT_KEY = 'ga-recent'
const RECENT_META_KEY = 'ga-recent-meta'
const AUTO_CLEAN_RECENT_OLDER_KEY = 'ga-auto-clean-recent-older'
const PINNED_KEY = 'ga-pinned'
const CURRENT_USER_KEY = 'ga-current-user'
const AUTH_SESSION_KEY = 'ga-auth-session'
const RIGHT_PANEL_KEY = 'ga-right-panel-open'
const LEFT_PANE_KEY = 'ga-left-pane-width'
const FOCUS_MODE_KEY = 'ga-focus-mode'

const docs = ref([])
const versions = ref([])
const comments = ref([])
const attachments = ref([])
const templates = ref([])
const docListRef = ref(null)
const activeSlug = ref('')
const currentUser = ref('admin')
const currentUserRole = ref('ADMIN')
const isAuthenticated = ref(false)
const pendingPageSlug = ref('')
const loginForm = ref({
  username: '',
  password: '',
  rememberMe: true
})
const loginError = ref('')
const loggingOut = ref(false)
const userAdminOpen = ref(false)
const authUsers = ref([])
const authUserLoading = ref(false)
const forcePwdDialog = ref({
  open: false,
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
  saving: false,
  error: ''
})
const passwordDialog = ref({
  open: false,
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
  saving: false,
  error: ''
})
const authUserForm = ref({
  username: '',
  password: '',
  role: 'EDITOR'
})
const currentDoc = ref(emptyDoc())
const showHome = ref(true)
const commandOpen = ref(false)
const commandQuery = ref('')
const commandActiveIndex = ref(0)
const toast = ref({
  show: false,
  message: '',
  type: 'info'
})
const confirmDialog = ref({
  open: false,
  title: '请确认',
  message: '',
  confirmText: '确认',
  cancelText: '取消',
  danger: false
})
const promptDialog = ref({
  open: false,
  title: '请输入',
  message: '',
  value: '',
  placeholder: '',
  confirmText: '确认',
  cancelText: '取消'
})
const promptInputRef = ref(null)
const bulkResult = ref({
  open: false,
  actionLabel: '',
  updated: [],
  skipped: []
})
const diffFrom = ref(null)
const diffTo = ref(null)
const diffText = ref('')
const favorites = ref([])
const recent = ref([])
const recentMeta = ref({})
const autoCleanRecentOlder = ref(false)
const pinned = ref([])
const treeFocusPathEnabled = ref(false)
const rightPanelOpen = ref(loadRightPanelState())
const leftPaneWidth = ref(loadLeftPaneWidth())
const focusMode = ref(loadFocusModeState())
const shareTokenFromUrl = ref('')
let toastTimer = null
let confirmResolver = null
let promptResolver = null
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
  const byParent = new Map()
  visibleDocs.value.forEach((doc) => {
    if (!doc.parentSlug) {
      return
    }
    if (!byParent.has(doc.parentSlug)) {
      byParent.set(doc.parentSlug, [])
    }
    byParent.get(doc.parentSlug).push(doc)
  })
  const result = []
  const walk = (parentSlug, depth) => {
    const children = (byParent.get(parentSlug) || []).slice().sort(sortByOrder)
    children.forEach((child) => {
      result.push({
        ...child,
        depth,
        childCount: (byParent.get(child.slug) || []).length
      })
      walk(child.slug, depth + 1)
    })
  }
  walk(activeSlug.value, 0)
  return result
})
const siblingNav = computed(() => {
  if (!activeSlug.value) {
    return { prevSlug: '', prevTitle: '', nextSlug: '', nextTitle: '' }
  }
  const current = visibleDocs.value.find((doc) => doc.slug === activeSlug.value)
  if (!current) {
    return { prevSlug: '', prevTitle: '', nextSlug: '', nextTitle: '' }
  }
  const parent = current.parentSlug || ''
  const siblings = visibleDocs.value
    .filter((doc) => (doc.parentSlug || '') === parent)
    .slice()
    .sort(sortByOrder)
  const index = siblings.findIndex((doc) => doc.slug === activeSlug.value)
  if (index < 0) {
    return { prevSlug: '', prevTitle: '', nextSlug: '', nextTitle: '' }
  }
  const prev = siblings[index - 1]
  const next = siblings[index + 1]
  return {
    prevSlug: prev?.slug || '',
    prevTitle: prev?.title || '',
    nextSlug: next?.slug || '',
    nextTitle: next?.title || ''
  }
})
const visibleDocs = computed(() => docs.value.filter((doc) => canViewDoc(doc)))
const homeStats = computed(() => {
  const published = visibleDocs.value.filter((d) => (d.status || 'DRAFT') === 'PUBLISHED').length
  const archived = visibleDocs.value.filter((d) => (d.status || 'DRAFT') === 'ARCHIVED').length
  const draft = visibleDocs.value.length - published - archived
  const privateCount = visibleDocs.value.filter((d) => (d.visibility || 'SPACE') === 'PRIVATE').length
  return {
    total: visibleDocs.value.length,
    published,
    draft,
    privateCount,
    archived
  }
})
const homeRecentDocs = computed(() => {
  const bySlug = new Map(visibleDocs.value.map((d) => [d.slug, d]))
  return recent.value.map((slug) => bySlug.get(slug)).filter(Boolean).slice(0, 8)
})
const homeFavoriteDocs = computed(() => {
  const bySlug = new Map(visibleDocs.value.map((d) => [d.slug, d]))
  return favorites.value.map((slug) => bySlug.get(slug)).filter(Boolean).slice(0, 8)
})
const homeOverdueDocs = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return visibleDocs.value
    .filter((doc) => !!doc.dueDate && doc.dueDate < today && (doc.status || 'DRAFT') !== 'ARCHIVED')
    .sort((a, b) => (a.dueDate || '').localeCompare(b.dueDate || ''))
    .slice(0, 8)
})
const homeTodayDueDocs = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return visibleDocs.value
    .filter((doc) => doc.dueDate === today && (doc.status || 'DRAFT') !== 'ARCHIVED')
    .sort((a, b) => (a.priority || 'MEDIUM').localeCompare(b.priority || 'MEDIUM'))
    .slice(0, 8)
})
const homeAssigneeBoard = computed(() => {
  const board = new Map()
  visibleDocs.value.forEach((doc) => {
    const assignee = (doc.assignee || '未分配').trim() || '未分配'
    if (!board.has(assignee)) {
      board.set(assignee, { assignee, total: 0, overdue: 0, today: 0 })
    }
    const item = board.get(assignee)
    item.total += 1
    const today = new Date().toISOString().slice(0, 10)
    if (doc.dueDate && doc.dueDate < today && (doc.status || 'DRAFT') !== 'ARCHIVED') {
      item.overdue += 1
    }
    if (doc.dueDate && doc.dueDate === today && (doc.status || 'DRAFT') !== 'ARCHIVED') {
      item.today += 1
    }
  })
  return Array.from(board.values())
    .sort((a, b) => b.overdue - a.overdue || b.today - a.today || b.total - a.total)
    .slice(0, 10)
})
const myTodoDocs = computed(() => {
  const user = (currentUser.value || '').trim()
  if (!user) {
    return []
  }
  return visibleDocs.value
    .filter((doc) => (doc.assignee || '') === user && (doc.status || 'DRAFT') !== 'ARCHIVED')
    .sort((a, b) => {
      const dueA = a.dueDate || '9999-12-31'
      const dueB = b.dueDate || '9999-12-31'
      if (dueA !== dueB) {
        return dueA.localeCompare(dueB)
      }
      return (a.priority || 'MEDIUM').localeCompare(b.priority || 'MEDIUM')
    })
    .slice(0, 12)
})
const commandResults = computed(() => {
  const q = commandQuery.value.trim().toLowerCase()
  if (!q) {
    return visibleDocs.value.slice(0, 12)
  }
  return visibleDocs.value
    .filter((d) => (d.title || '').toLowerCase().includes(q) || (d.slug || '').toLowerCase().includes(q))
    .slice(0, 12)
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
const currentCanEdit = computed(() => {
  const role = (currentUserRole.value || '').trim().toUpperCase()
  if (role === 'VIEWER') {
    return false
  }
  if (!currentDoc.value?.id) {
    return role === 'ADMIN' || role === 'EDITOR'
  }
  return canEditDoc(currentDoc.value)
})
const canManageTemplates = computed(() => currentUserRole.value === 'ADMIN')
const canManageUsers = computed(() => currentUserRole.value === 'ADMIN')
const currentShareLink = computed(() => {
  if (!activeSlug.value || !currentDoc.value?.shareEnabled || !currentDoc.value?.shareToken) {
    return ''
  }
  return `${window.location.origin}?page=${encodeURIComponent(activeSlug.value)}&token=${encodeURIComponent(currentDoc.value.shareToken)}`
})
const layoutStyle = computed(() => ({
  '--left-col': `${leftPaneWidth.value}px`
}))

function loadRightPanelState() {
  if (typeof window === 'undefined') {
    return true
  }
  const raw = window.localStorage.getItem(RIGHT_PANEL_KEY)
  if (raw === null) {
    return true
  }
  return raw !== '0'
}

function persistRightPanelState(open) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(RIGHT_PANEL_KEY, open ? '1' : '0')
}

function toggleRightPanel() {
  rightPanelOpen.value = !rightPanelOpen.value
}

function loadFocusModeState() {
  if (typeof window === 'undefined') {
    return false
  }
  return window.localStorage.getItem(FOCUS_MODE_KEY) === '1'
}

function persistFocusModeState(open) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(FOCUS_MODE_KEY, open ? '1' : '0')
}

function toggleFocusMode() {
  focusMode.value = !focusMode.value
}

function loadLeftPaneWidth() {
  if (typeof window === 'undefined') {
    return 320
  }
  const raw = Number(window.localStorage.getItem(LEFT_PANE_KEY) || '')
  if (!Number.isFinite(raw) || raw <= 0) {
    return 320
  }
  return Math.max(260, Math.min(460, Math.round(raw)))
}

function persistLeftPaneWidth(width) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(LEFT_PANE_KEY, String(width))
}

let resizingLeft = false
let leftResizeStartX = 0
let leftResizeStartWidth = 320

function onLeftResizeMove(event) {
  if (!resizingLeft) {
    return
  }
  const delta = event.clientX - leftResizeStartX
  const next = Math.max(260, Math.min(460, Math.round(leftResizeStartWidth + delta)))
  leftPaneWidth.value = next
}

function stopLeftResize() {
  if (!resizingLeft) {
    return
  }
  resizingLeft = false
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  window.removeEventListener('mousemove', onLeftResizeMove)
  window.removeEventListener('mouseup', stopLeftResize)
}

function startLeftResize(event) {
  if (event.button !== 0) {
    return
  }
  event.preventDefault()
  resizingLeft = true
  leftResizeStartX = event.clientX
  leftResizeStartWidth = leftPaneWidth.value
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', onLeftResizeMove)
  window.addEventListener('mouseup', stopLeftResize)
}

function showToast(message, type = 'info') {
  if (!message) {
    return
  }
  toast.value = {
    show: true,
    message,
    type
  }
  if (toastTimer) {
    clearTimeout(toastTimer)
  }
  toastTimer = setTimeout(() => {
    toast.value.show = false
  }, 2200)
}

function askConfirm(message, options = {}) {
  return new Promise((resolve) => {
    confirmDialog.value = {
      open: true,
      title: options.title || '请确认',
      message,
      confirmText: options.confirmText || '确认',
      cancelText: options.cancelText || '取消',
      danger: !!options.danger
    }
    confirmResolver = resolve
  })
}

function resolveConfirm(accepted) {
  confirmDialog.value.open = false
  if (confirmResolver) {
    confirmResolver(accepted)
    confirmResolver = null
  }
}

function askPrompt(message, options = {}) {
  return new Promise((resolve) => {
    promptDialog.value = {
      open: true,
      title: options.title || '请输入',
      message,
      value: options.initialValue || '',
      placeholder: options.placeholder || '',
      confirmText: options.confirmText || '确认',
      cancelText: options.cancelText || '取消'
    }
    promptResolver = resolve
    setTimeout(() => {
      if (promptInputRef.value && typeof promptInputRef.value.focus === 'function') {
        promptInputRef.value.focus()
        promptInputRef.value.select?.()
      }
    }, 0)
  })
}

function resolvePrompt(value) {
  promptDialog.value.open = false
  if (promptResolver) {
    const next = typeof value === 'string' ? value.trim() : ''
    promptResolver(next || null)
    promptResolver = null
  }
}

function handleEditorNotify(payload) {
  if (!payload || !payload.message) {
    return
  }
  showToast(payload.message, payload.type || 'info')
}

function handlePermissionQuickFilter(mode) {
  docListRef.value?.setPermissionFilter(mode)
  showToast('已按权限筛选左侧页面', 'success')
}

function handleTreeFocusChange(enabled) {
  treeFocusPathEnabled.value = !!enabled
}

function toggleTreeFocusPathFromTopbar() {
  docListRef.value?.toggleTreeFocusPath()
}

function firstAccessibleSlug(slugs) {
  const list = Array.isArray(slugs) ? slugs : []
  for (const slug of list) {
    const doc = docs.value.find((d) => d.slug === slug)
    if (doc && canViewDoc(doc)) {
      return slug
    }
  }
  return ''
}

function showBulkResult(actionLabel, updated, skipped) {
  bulkResult.value = {
    open: true,
    actionLabel,
    updated: updated.slice(0, 80),
    skipped: skipped.slice(0, 80)
  }
}

function emptyDoc() {
  return {
    id: null,
    slug: '',
    title: '',
    summary: '',
    parentSlug: '',
    labels: [],
    owner: currentUser.value || 'admin',
    editors: [],
    viewers: [],
    priority: 'MEDIUM',
    dueDate: '',
    assignee: '',
    status: 'DRAFT',
    visibility: 'SPACE',
    locked: false,
    sortOrder: 0,
    shareEnabled: false,
    shareToken: '',
    content: '# 新文档\n\n开始编辑...'
  }
}

async function fetchDocs() {
  const { data } = await api.get('/documents')
  docs.value = data
  syncCollectionsWithDocs()
}

async function fetchTemplates() {
  const { data } = await api.get('/templates')
  templates.value = data
}

async function loadDoc(slug) {
  const candidate = docs.value.find((d) => d.slug === slug)
  if (candidate && !canViewDoc(candidate)) {
    showToast('当前用户无权限查看该页面', 'error')
    return
  }
  const { data } = await api.get(`/documents/${slug}`)
  if (!canViewDoc(data)) {
    showToast('当前用户无权限查看该页面', 'error')
    return
  }
  currentDoc.value = data
  if (!currentDoc.value.status) {
    currentDoc.value.status = 'DRAFT'
  }
  if (!currentDoc.value.visibility) {
    currentDoc.value.visibility = 'SPACE'
  }
  if (currentDoc.value.locked === undefined || currentDoc.value.locked === null) {
    currentDoc.value.locked = false
  }
  if (currentDoc.value.shareEnabled === undefined || currentDoc.value.shareEnabled === null) {
    currentDoc.value.shareEnabled = false
  }
  if (!currentDoc.value.shareToken) {
    currentDoc.value.shareToken = ''
  }
  if (!Array.isArray(currentDoc.value.editors)) {
    currentDoc.value.editors = []
  }
  if (!Array.isArray(currentDoc.value.viewers)) {
    currentDoc.value.viewers = []
  }
  if (!currentDoc.value.priority) {
    currentDoc.value.priority = 'MEDIUM'
  }
  if (!currentDoc.value.assignee) {
    currentDoc.value.assignee = ''
  }
  if (!currentDoc.value.dueDate) {
    currentDoc.value.dueDate = ''
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

function duplicateCurrentPage() {
  if (!activeSlug.value) {
    return
  }
  if (!canViewDoc(currentDoc.value)) {
    showToast('当前页面不可复制', 'error')
    return
  }
  const source = currentDoc.value
  const suffix = Date.now().toString().slice(-6)
  const baseSlug = (source.slug || 'page').replace(/-copy-\d+$/, '')
  activeSlug.value = ''
  currentDoc.value = {
    ...emptyDoc(),
    parentSlug: source.parentSlug || null,
    title: `${source.title || source.slug}（副本）`,
    slug: `${baseSlug}-copy-${suffix}`,
    summary: source.summary || '',
    labels: Array.isArray(source.labels) ? [...source.labels] : [],
    owner: source.owner || currentUser.value || 'admin',
    editors: Array.isArray(source.editors) ? [...source.editors] : [],
    viewers: Array.isArray(source.viewers) ? [...source.viewers] : [],
    priority: source.priority || 'MEDIUM',
    dueDate: source.dueDate || '',
    assignee: source.assignee || '',
    visibility: source.visibility || 'SPACE',
    content: source.content || ''
  }
  versions.value = []
  comments.value = []
  attachments.value = []
  diffFrom.value = null
  diffTo.value = null
  diffText.value = ''
  showToast('已生成页面副本草稿', 'success')
}

function createChildPage() {
  if (!activeSlug.value) {
    return
  }
  if (!canEditDoc(currentDoc.value)) {
    showToast('当前用户无编辑权限，不能创建子页面', 'error')
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

function createChildPageWithTemplate(templateId) {
  if (!activeSlug.value) {
    return
  }
  if (!canEditDoc(currentDoc.value)) {
    showToast('当前用户无编辑权限，不能创建子页面', 'error')
    return
  }
  const tpl = templates.value.find((item) => item.id === Number(templateId))
  if (!tpl) {
    createChildPage()
    return
  }
  const parent = currentDoc.value
  const suffix = slugPartFromText(tpl.name || 'template')
  activeSlug.value = ''
  currentDoc.value = {
    ...emptyDoc(),
    parentSlug: parent.slug,
    title: `${parent.title || parent.slug} - ${tpl.name || '模板子页'}`,
    slug: `${parent.slug}-${suffix}-${Date.now().toString().slice(-6)}`,
    content: tpl.content || `# ${tpl.name || '模板子页'}\n\n`
  }
  versions.value = []
  comments.value = []
  attachments.value = []
  diffFrom.value = null
  diffTo.value = null
  diffText.value = ''
  showToast(`已创建模板子页面：${tpl.name}`, 'success')
}

function slugPartFromText(text) {
  const cleaned = String(text || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9\u4e00-\u9fa5]+/g, '-')
    .replace(/^-+|-+$/g, '')
  return cleaned || 'child'
}

async function saveDoc(doc) {
  if ((doc.id && !canEditDoc(doc)) || (!doc.id && !currentCanEdit.value)) {
    showToast('当前用户无编辑权限', 'error')
    return
  }
  if (!doc.slug || !doc.title || !doc.summary || !doc.content) {
    showToast('请填写完整字段', 'error')
    return
  }

  if (doc.id) {
    await api.put(`/documents/${doc.slug}`, {
      title: doc.title,
      summary: doc.summary,
      content: doc.content,
      parentSlug: doc.parentSlug || null,
      labels: doc.labels || [],
      owner: doc.owner || null,
      editors: doc.editors || [],
      viewers: doc.viewers || [],
      priority: doc.priority || 'MEDIUM',
      dueDate: doc.dueDate || null,
      assignee: doc.assignee || null,
      status: doc.status || 'DRAFT',
      visibility: doc.visibility || 'SPACE',
      locked: !!doc.locked
    })
  } else {
    await api.post('/documents', {
      slug: doc.slug,
      title: doc.title,
      summary: doc.summary,
      content: doc.content,
      parentSlug: doc.parentSlug || null,
      labels: doc.labels || [],
      owner: doc.owner || currentUser.value || 'admin',
      editors: doc.editors || [],
      viewers: doc.viewers || [],
      priority: doc.priority || 'MEDIUM',
      dueDate: doc.dueDate || null,
      assignee: doc.assignee || null,
      status: doc.status || 'DRAFT',
      visibility: doc.visibility || 'SPACE',
      locked: !!doc.locked
    })
  }

  await fetchDocs()
  await loadDoc(doc.slug)
  showToast('页面已保存', 'success')
}

async function deleteDoc(slug) {
  if (!slug) {
    return
  }
  const target = docs.value.find((d) => d.slug === slug)
  if (target && !canEditDoc(target)) {
    showToast('当前用户无删除权限', 'error')
    return
  }
  const confirmed = await askConfirm(`确认删除文档 ${slug} ?`, {
    title: '删除页面',
    confirmText: '删除',
    danger: true
  })
  if (!confirmed) {
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
  showToast('页面已删除', 'success')
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
  const confirmed = await askConfirm(`确认回滚到 v${versionNo} ?`, {
    title: '回滚版本',
    confirmText: '回滚',
    danger: true
  })
  if (!confirmed) {
    return
  }
  const { data } = await api.post(`/documents/${activeSlug.value}/versions/${versionNo}/restore`)
  currentDoc.value = data
  await fetchDocs()
  await loadAttachments(activeSlug.value)
  await refreshVersions()
  diffText.value = ''
  showToast(`已回滚到 v${versionNo}`, 'success')
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
  if (!canViewDoc(currentDoc.value)) {
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
  if (!canEditDoc(currentDoc.value)) {
    showToast('当前用户无编辑权限', 'error')
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
  if (!canEditDoc(currentDoc.value)) {
    showToast('当前用户无编辑权限', 'error')
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

function openBreadcrumb(slug) {
  if (!slug || slug === 'home') {
    openHome()
    return
  }
  loadDoc(slug)
}

function openMyTodoView() {
  docListRef.value?.setMyTodoFilter(currentUser.value)
  showHome.value = false
  if (myTodoDocs.value.length > 0) {
    loadDoc(myTodoDocs.value[0].slug)
  }
}

async function moveDoc(payload) {
  if (!payload?.slug) {
    return
  }
  const target = docs.value.find((d) => d.slug === payload.slug)
  if (target && !canEditDoc(target)) {
    showToast('当前用户无编辑权限', 'error')
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

async function reorderDoc(payload) {
  if (!payload?.slug || !payload?.direction) {
    return
  }
  const target = docs.value.find((d) => d.slug === payload.slug)
  if (target && !canEditDoc(target)) {
    showToast('当前用户无编辑权限', 'error')
    return
  }
  await api.patch(`/documents/${payload.slug}/reorder`, {
    direction: payload.direction
  })
  await fetchDocs()
  if (activeSlug.value) {
    await loadDoc(activeSlug.value)
  }
}

async function handleDocQuickAction(payload) {
  if (!payload?.slug || !payload?.action) {
    return
  }
  const target = docs.value.find((d) => d.slug === payload.slug)
  if (!target) {
    return
  }

  if (payload.action === 'COPY_LINK') {
    const shareableLink = `${window.location.origin}?page=${encodeURIComponent(payload.slug)}`
    try {
      await navigator.clipboard.writeText(shareableLink)
      showToast('页面链接已复制', 'success')
    } catch {
      showToast('复制失败，请手动复制', 'error')
    }
    return
  }

  if (payload.action === 'COPY_SLUG') {
    try {
      await navigator.clipboard.writeText(payload.slug)
      showToast('slug 已复制', 'success')
    } catch {
      showToast('复制失败，请手动复制', 'error')
    }
    return
  }

  if (payload.action === 'TOGGLE_FAVORITE') {
    toggleFavorite(payload.slug)
    return
  }

  if (payload.action === 'OPEN_PAGE') {
    await loadDoc(payload.slug)
    return
  }

  if (!canEditDoc(target)) {
    showToast('当前用户无编辑权限', 'error')
    return
  }

  if (payload.action === 'MOVE_ROOT') {
    await moveDoc({
      slug: payload.slug,
      parentSlug: null
    })
    return
  }

  if (payload.action === 'RENAME') {
    const { data } = await api.get(`/documents/${payload.slug}`)
    const nextTitle = await askPrompt('请输入新的页面标题', {
      title: '重命名页面',
      placeholder: '输入新标题',
      initialValue: data.title || payload.slug,
      confirmText: '保存'
    })
    if (!nextTitle) {
      return
    }
    await api.put(`/documents/${payload.slug}`, {
      title: nextTitle,
      summary: data.summary,
      content: data.content,
      parentSlug: data.parentSlug || null,
      labels: data.labels || [],
      owner: data.owner || null,
      editors: data.editors || [],
      viewers: data.viewers || [],
      priority: data.priority || 'MEDIUM',
      dueDate: data.dueDate || null,
      assignee: data.assignee || null,
      status: data.status || 'DRAFT',
      visibility: data.visibility || 'SPACE',
      locked: !!data.locked
    })
    await fetchDocs()
    if (activeSlug.value === payload.slug) {
      await loadDoc(payload.slug)
    }
    return
  }

  if (payload.action === 'ARCHIVE' || payload.action === 'UNARCHIVE') {
    const nextStatus = payload.action === 'ARCHIVE' ? 'ARCHIVED' : 'DRAFT'
    const { data } = await api.get(`/documents/${payload.slug}`)
    await api.put(`/documents/${payload.slug}`, {
      title: data.title,
      summary: data.summary,
      content: data.content,
      parentSlug: data.parentSlug || null,
      labels: data.labels || [],
      owner: data.owner || null,
      editors: data.editors || [],
      viewers: data.viewers || [],
      priority: data.priority || 'MEDIUM',
      dueDate: data.dueDate || null,
      assignee: data.assignee || null,
      status: nextStatus,
      visibility: data.visibility || 'SPACE',
      locked: !!data.locked
    })
    await fetchDocs()
    if (activeSlug.value === payload.slug) {
      await loadDoc(payload.slug)
    }
  }
}

async function handleDocBulkAction(payload) {
  const slugs = Array.isArray(payload?.slugs) ? payload.slugs.filter(Boolean) : []
  if (!payload?.action || slugs.length === 0) {
    return
  }

  if (payload.action === 'BULK_FAVORITE') {
    slugs.forEach((slug) => {
      if (!favorites.value.includes(slug)) {
        favorites.value = [slug, ...favorites.value]
      }
    })
    persistCollections()
    docListRef.value?.clearBatchSelection()
    return
  }

  if (payload.action === 'BULK_UNFAVORITE') {
    favorites.value = favorites.value.filter((slug) => !slugs.includes(slug))
    persistCollections()
    docListRef.value?.clearBatchSelection()
    return
  }

  if (payload.action === 'BULK_MOVE_ROOT') {
    let updated = 0
    let skipped = 0
    const updatedSlugs = []
    const skippedItems = []
    for (const slug of slugs) {
      const target = docs.value.find((d) => d.slug === slug)
      if (!target || !canEditDoc(target)) {
        skipped += 1
        skippedItems.push({
          slug,
          reason: target ? '无编辑权限' : '页面不存在'
        })
        continue
      }
      try {
        await api.patch(`/documents/${slug}/move`, {
          parentSlug: null
        })
        updated += 1
        updatedSlugs.push(slug)
      } catch {
        skipped += 1
        skippedItems.push({
          slug,
          reason: '请求失败'
        })
      }
    }
    await fetchDocs()
    if (activeSlug.value && slugs.includes(activeSlug.value)) {
      await loadDoc(activeSlug.value)
    } else {
      const fallback = firstAccessibleSlug(updatedSlugs)
      if (fallback) {
        await loadDoc(fallback)
      }
    }
    showToast(`批量移到顶级完成：成功 ${updated}，跳过 ${skipped}`, updated > 0 ? 'success' : 'info')
    showBulkResult('批量移到顶级', updatedSlugs, skippedItems)
    docListRef.value?.clearBatchSelection()
    return
  }

  const nextStatus = payload.action === 'BULK_ARCHIVE'
    ? 'ARCHIVED'
    : payload.action === 'BULK_UNARCHIVE'
      ? 'DRAFT'
      : ''
  if (!nextStatus) {
    return
  }

  let updated = 0
  let skipped = 0
  const updatedSlugs = []
  const skippedItems = []
  for (const slug of slugs) {
    const target = docs.value.find((d) => d.slug === slug)
    if (!target || !canEditDoc(target)) {
      skipped += 1
      skippedItems.push({
        slug,
        reason: target ? '无编辑权限' : '页面不存在'
      })
      continue
    }
    try {
      const { data } = await api.get(`/documents/${slug}`)
      await api.put(`/documents/${slug}`, {
        title: data.title,
        summary: data.summary,
        content: data.content,
        parentSlug: data.parentSlug || null,
        labels: data.labels || [],
        owner: data.owner || null,
        editors: data.editors || [],
        viewers: data.viewers || [],
        priority: data.priority || 'MEDIUM',
        dueDate: data.dueDate || null,
        assignee: data.assignee || null,
        status: nextStatus,
        visibility: data.visibility || 'SPACE',
        locked: !!data.locked
      })
      updated += 1
      updatedSlugs.push(slug)
    } catch {
      skipped += 1
      skippedItems.push({
        slug,
        reason: '请求失败'
      })
    }
  }

  await fetchDocs()
  if (activeSlug.value && slugs.includes(activeSlug.value)) {
    await loadDoc(activeSlug.value)
  } else {
    const fallback = firstAccessibleSlug(updatedSlugs)
    if (fallback) {
      await loadDoc(fallback)
    }
  }
  showToast(`批量操作完成：成功 ${updated}，跳过 ${skipped}`, updated > 0 ? 'success' : 'info')
  showBulkResult(nextStatus === 'ARCHIVED' ? '批量归档' : '批量恢复草稿', updatedSlugs, skippedItems)
  docListRef.value?.clearBatchSelection()
}

async function toggleShare(enabled) {
  if (!activeSlug.value || !currentCanEdit.value) {
    return
  }
  const { data } = await api.patch(`/documents/${activeSlug.value}/share`, {
    enabled,
    regenerate: false
  })
  currentDoc.value = {
    ...currentDoc.value,
    ...data
  }
  await fetchDocs()
  showToast(enabled ? '已开启分享' : '已关闭分享', 'success')
}

async function regenerateShare() {
  if (!activeSlug.value || !currentCanEdit.value) {
    return
  }
  const { data } = await api.patch(`/documents/${activeSlug.value}/share`, {
    enabled: true,
    regenerate: true
  })
  currentDoc.value = {
    ...currentDoc.value,
    ...data
  }
  await fetchDocs()
  showToast('分享链接已重置', 'success')
}

async function createTemplate(payload) {
  if (!canManageTemplates.value) {
    showToast('仅管理员可管理模板', 'error')
    return
  }
  if (!payload?.name || !payload?.content) {
    return
  }
  await api.post('/templates', {
    name: payload.name,
    description: payload.description || '',
    content: payload.content
  })
  await fetchTemplates()
  showToast('模板已创建', 'success')
}

async function updateTemplate(payload) {
  if (!canManageTemplates.value) {
    showToast('仅管理员可管理模板', 'error')
    return
  }
  if (!payload?.id || !payload?.name || !payload?.content) {
    return
  }
  await api.put(`/templates/${payload.id}`, {
    name: payload.name,
    description: payload.description || '',
    content: payload.content
  })
  await fetchTemplates()
  showToast('模板已更新', 'success')
}

async function deleteTemplate(templateId) {
  if (!canManageTemplates.value) {
    showToast('仅管理员可管理模板', 'error')
    return
  }
  if (!templateId) {
    return
  }
  const confirmed = await askConfirm('确认删除该模板？', {
    title: '删除模板',
    confirmText: '删除',
    danger: true
  })
  if (!confirmed) {
    return
  }
  await api.delete(`/templates/${templateId}`)
  await fetchTemplates()
  showToast('模板已删除', 'success')
}

function toggleFavorite(slug) {
  if (favorites.value.includes(slug)) {
    favorites.value = favorites.value.filter((s) => s !== slug)
  } else {
    favorites.value = [slug, ...favorites.value]
  }
  persistCollections()
}

function togglePinned(slug) {
  if (!slug) {
    return
  }
  if (pinned.value.includes(slug)) {
    pinned.value = pinned.value.filter((s) => s !== slug)
  } else {
    pinned.value = [slug, ...pinned.value]
  }
  persistCollections()
}

function handleQuickCollectionReorder(payload) {
  const type = String(payload?.type || '').toUpperCase()
  const ordered = Array.isArray(payload?.slugs) ? payload.slugs : []
  if (type !== 'FAVORITES' && type !== 'RECENT' && type !== 'PINNED') {
    return
  }
  const current = type === 'FAVORITES'
    ? favorites.value
    : type === 'RECENT'
      ? recent.value
      : pinned.value
  const uniqueOrdered = Array.from(new Set(ordered.filter((slug) => current.includes(slug))))
  const remaining = current.filter((slug) => !uniqueOrdered.includes(slug))
  const next = [...uniqueOrdered, ...remaining]
  if (type === 'FAVORITES') {
    favorites.value = next
  } else if (type === 'RECENT') {
    recent.value = next.slice(0, 12)
  } else {
    pinned.value = next
  }
  persistCollections()
}

function handleQuickCollectionAction(payload) {
  const action = String(payload?.action || '').trim().toUpperCase()
  const slugs = Array.isArray(payload?.slugs) ? payload.slugs : []
  if (action === 'CLEAR_PINNED') {
    pinned.value = []
    persistCollections()
    showToast('已清空固定页面', 'success')
    return
  }
  if (action === 'CLEAR_FAVORITES') {
    favorites.value = []
    persistCollections()
    showToast('已清空收藏', 'success')
    return
  }
  if (action === 'CLEAR_RECENT_ALL') {
    recent.value = []
    recentMeta.value = {}
    persistCollections()
    showToast('已清空最近访问', 'success')
    return
  }
  if (action === 'CLEAR_RECENT_OLDER') {
    const drop = new Set(slugs)
    recent.value = recent.value.filter((slug) => !drop.has(slug))
    const nextMeta = {}
    Object.entries(recentMeta.value || {}).forEach(([slug, time]) => {
      if (!drop.has(slug)) {
        nextMeta[slug] = Number(time) || 0
      }
    })
    recentMeta.value = nextMeta
    persistCollections()
    showToast('已清理更早访问记录', 'success')
    return
  }
  if (action === 'TOGGLE_AUTO_CLEAN_RECENT_OLDER') {
    autoCleanRecentOlder.value = !autoCleanRecentOlder.value
    if (autoCleanRecentOlder.value) {
      cleanupRecentOlder(7)
    }
    persistCollections()
    showToast(autoCleanRecentOlder.value ? '已开启自动清理更早记录' : '已关闭自动清理更早记录', 'success')
  }
}

function cleanupRecentOlder(days = 7) {
  const now = Date.now()
  const threshold = now - days * 24 * 3600 * 1000
  const drop = new Set(
    recent.value.filter((slug) => {
      const visitedAt = Number(recentMeta.value?.[slug] || 0)
      return visitedAt <= 0 || visitedAt < threshold
    })
  )
  if (drop.size === 0) {
    return
  }
  recent.value = recent.value.filter((slug) => !drop.has(slug))
  const nextMeta = {}
  Object.entries(recentMeta.value || {}).forEach(([slug, time]) => {
    if (!drop.has(slug)) {
      nextMeta[slug] = Number(time) || 0
    }
  })
  recentMeta.value = nextMeta
}

function touchRecent(slug) {
  recent.value = [slug, ...recent.value.filter((s) => s !== slug)].slice(0, 12)
  recentMeta.value = {
    ...recentMeta.value,
    [slug]: Date.now()
  }
  if (autoCleanRecentOlder.value) {
    cleanupRecentOlder(7)
  }
  persistCollections()
}

function syncCollectionsWithDocs() {
  const allSlugs = new Set(visibleDocs.value.map((d) => d.slug))
  pinned.value = pinned.value.filter((s) => allSlugs.has(s))
  favorites.value = favorites.value.filter((s) => allSlugs.has(s))
  recent.value = recent.value.filter((s) => allSlugs.has(s))
  const nextRecentMeta = {}
  Object.entries(recentMeta.value || {}).forEach(([slug, time]) => {
    if (allSlugs.has(slug)) {
      nextRecentMeta[slug] = Number(time) || 0
    }
  })
  recentMeta.value = nextRecentMeta
  persistCollections()
}

function persistCollections() {
  localStorage.setItem(PINNED_KEY, JSON.stringify(pinned.value))
  localStorage.setItem(FAVORITES_KEY, JSON.stringify(favorites.value))
  localStorage.setItem(RECENT_KEY, JSON.stringify(recent.value))
  localStorage.setItem(RECENT_META_KEY, JSON.stringify(recentMeta.value || {}))
  localStorage.setItem(AUTO_CLEAN_RECENT_OLDER_KEY, autoCleanRecentOlder.value ? '1' : '0')
  localStorage.setItem(CURRENT_USER_KEY, currentUser.value || 'admin')
}

function loadCollections() {
  try {
    pinned.value = JSON.parse(localStorage.getItem(PINNED_KEY) || '[]')
    favorites.value = JSON.parse(localStorage.getItem(FAVORITES_KEY) || '[]')
    recent.value = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]')
    const parsedRecentMeta = JSON.parse(localStorage.getItem(RECENT_META_KEY) || '{}')
    recentMeta.value = parsedRecentMeta && typeof parsedRecentMeta === 'object' ? parsedRecentMeta : {}
    autoCleanRecentOlder.value = localStorage.getItem(AUTO_CLEAN_RECENT_OLDER_KEY) === '1'
    if (autoCleanRecentOlder.value) {
      cleanupRecentOlder(7)
    }
    currentUser.value = localStorage.getItem(CURRENT_USER_KEY) || 'admin'
  } catch {
    pinned.value = []
    favorites.value = []
    recent.value = []
    recentMeta.value = {}
    autoCleanRecentOlder.value = false
    currentUser.value = 'admin'
  }
}

function loadAuthSessionUser() {
  if (typeof window === 'undefined') {
    return null
  }
  try {
    const raw = window.localStorage.getItem(AUTH_SESSION_KEY)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw)
    const username = (parsed?.username || '').trim()
    const role = String(parsed?.role || 'ADMIN').trim().toUpperCase()
    const expiresAt = Number(parsed?.expiresAt || 0)
    if (!username || !Number.isFinite(expiresAt)) {
      return null
    }
    return {
      username,
      role: role || 'ADMIN',
      expiresAt
    }
  } catch {
    return null
  }
}

function persistAuthSession(username, role = 'ADMIN', expiresAt = 0) {
  if (typeof window === 'undefined') {
    return
  }
  const clean = (username || '').trim()
  const cleanRole = String(role || 'ADMIN').trim().toUpperCase() || 'ADMIN'
  const expireNumber = Number(expiresAt || 0)
  if (!clean || !Number.isFinite(expireNumber) || expireNumber <= 0) {
    window.localStorage.removeItem(AUTH_SESSION_KEY)
    return
  }
  window.localStorage.setItem(AUTH_SESSION_KEY, JSON.stringify({
    username: clean,
    role: cleanRole,
    expiresAt: expireNumber
  }))
}

async function bootstrapWorkspace(initialPage = '') {
  await fetchDocs()
  await fetchTemplates()
  if (initialPage) {
    await loadDoc(initialPage)
  }
}

async function submitLogin() {
  const username = (loginForm.value.username || '').trim()
  const password = String(loginForm.value.password || '')
  if (!username || !password) {
    loginError.value = '请输入用户名和密码'
    return
  }
  try {
    const { data } = await api.post('/auth/login', {
      username,
      password,
      rememberMe: !!loginForm.value.rememberMe
    })
    const authUser = (data?.username || username).trim()
    const authRole = String(data?.role || 'ADMIN').trim().toUpperCase() || 'ADMIN'
    const mustChangePassword = Boolean(data?.mustChangePassword)
    const token = (data?.token || '').trim()
    const expiresAt = Number(data?.expiresAt || 0)
    if (!token || !authUser || !Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
      loginError.value = '登录响应无效，请稍后重试'
      return
    }
    setApiAuthToken(token)
    currentUser.value = authUser
    currentUserRole.value = authRole
    persistCollections()
    persistAuthSession(authUser, authRole, expiresAt)
    if (mustChangePassword) {
      openForcePasswordDialog(password)
    }
  } catch {
    loginError.value = '用户名或密码错误'
    return
  }
  isAuthenticated.value = true
  loginForm.value.password = ''
  loginError.value = ''
  await bootstrapWorkspace(pendingPageSlug.value)
  pendingPageSlug.value = ''
  showToast(`欢迎回来，${username}`, 'success')
}

function openForcePasswordDialog(currentPassword) {
  forcePwdDialog.value = {
    open: true,
    currentPassword: currentPassword || '',
    newPassword: '',
    confirmPassword: '',
    saving: false,
    error: ''
  }
}

function openPasswordDialog() {
  passwordDialog.value = {
    open: true,
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
    saving: false,
    error: ''
  }
}

async function submitForcePasswordChange() {
  const next = String(forcePwdDialog.value.newPassword || '')
  const confirm = String(forcePwdDialog.value.confirmPassword || '')
  if (next.length < 8) {
    forcePwdDialog.value.error = '密码长度至少 8 位'
    return
  }
  const hasLetter = /[A-Za-z]/.test(next)
  const hasDigit = /\d/.test(next)
  if (!hasLetter || !hasDigit) {
    forcePwdDialog.value.error = '密码需同时包含字母和数字'
    return
  }
  if (next !== confirm) {
    forcePwdDialog.value.error = '两次输入的密码不一致'
    return
  }

  forcePwdDialog.value.saving = true
  forcePwdDialog.value.error = ''
  try {
    await api.post('/auth/password/change', {
      currentPassword: forcePwdDialog.value.currentPassword,
      newPassword: next
    })
    forcePwdDialog.value.open = false
    showToast('密码已更新', 'success')
  } catch (error) {
    const message = error?.response?.data?.message || '修改密码失败'
    forcePwdDialog.value.error = message
  } finally {
    forcePwdDialog.value.saving = false
  }
}

async function submitPasswordChange() {
  const current = String(passwordDialog.value.currentPassword || '')
  const next = String(passwordDialog.value.newPassword || '')
  const confirm = String(passwordDialog.value.confirmPassword || '')
  if (!current) {
    passwordDialog.value.error = '请输入当前密码'
    return
  }
  if (next.length < 8) {
    passwordDialog.value.error = '密码长度至少 8 位'
    return
  }
  const hasLetter = /[A-Za-z]/.test(next)
  const hasDigit = /\d/.test(next)
  if (!hasLetter || !hasDigit) {
    passwordDialog.value.error = '密码需同时包含字母和数字'
    return
  }
  if (next !== confirm) {
    passwordDialog.value.error = '两次输入的密码不一致'
    return
  }
  passwordDialog.value.saving = true
  passwordDialog.value.error = ''
  try {
    await api.post('/auth/password/change', {
      currentPassword: current,
      newPassword: next
    })
    passwordDialog.value.open = false
    showToast('密码已更新', 'success')
  } catch (error) {
    passwordDialog.value.error = error?.response?.data?.message || '修改密码失败'
  } finally {
    passwordDialog.value.saving = false
  }
}

async function loadAuthUsers() {
  if (!canManageUsers.value) {
    return
  }
  authUserLoading.value = true
  try {
    const { data } = await api.get('/auth/users')
    authUsers.value = Array.isArray(data) ? data : []
  } finally {
    authUserLoading.value = false
  }
}

async function openUserAdmin() {
  if (!canManageUsers.value) {
    return
  }
  userAdminOpen.value = true
  await loadAuthUsers()
}

function prefillAuthUser(item) {
  authUserForm.value = {
    username: item.username || '',
    password: '',
    role: item.role || 'EDITOR'
  }
}

async function saveAuthUser() {
  const username = (authUserForm.value.username || '').trim()
  const role = String(authUserForm.value.role || 'EDITOR').trim().toUpperCase()
  if (!username || !role) {
    showToast('请填写用户名和角色', 'error')
    return
  }
  authUserLoading.value = true
  try {
    await api.post('/auth/users', {
      username,
      password: authUserForm.value.password || '',
      role
    })
    authUserForm.value.password = ''
    await loadAuthUsers()
    showToast('用户已保存', 'success')
  } catch (error) {
    const message = error?.response?.data?.message || '保存失败'
    showToast(message, 'error')
  } finally {
    authUserLoading.value = false
  }
}

async function deleteAuthUser(username) {
  if (!username) {
    return
  }
  const confirmed = await askConfirm(`确认删除用户 ${username} ?`, {
    title: '删除用户',
    confirmText: '删除',
    danger: true
  })
  if (!confirmed) {
    return
  }
  authUserLoading.value = true
  try {
    await api.delete(`/auth/users/${encodeURIComponent(username)}`)
    await loadAuthUsers()
    showToast('用户已删除', 'success')
  } catch (error) {
    const message = error?.response?.data?.message || '删除失败'
    showToast(message, 'error')
  } finally {
    authUserLoading.value = false
  }
}

function clearWorkspaceAfterLogout() {
  isAuthenticated.value = false
  pendingPageSlug.value = ''
  currentUserRole.value = 'ADMIN'
  loginForm.value = {
    username: currentUser.value || '',
    password: '',
    rememberMe: true
  }
  loginError.value = ''
  docs.value = []
  versions.value = []
  comments.value = []
  attachments.value = []
  templates.value = []
  forcePwdDialog.value.open = false
  passwordDialog.value.open = false
  openHome()
}

function forceLogoutDueToAuthFailure() {
  if (loggingOut.value) {
    return
  }
  persistAuthSession('', 'ADMIN', 0)
  setApiAuthToken('')
  clearWorkspaceAfterLogout()
  showToast('登录已过期，请重新登录', 'error')
}

async function logout() {
  if (loggingOut.value) {
    return
  }
  loggingOut.value = true
  try {
    await api.post('/auth/logout')
  } catch {
    // Token may already be expired; continue local cleanup.
  } finally {
    persistAuthSession('', 'ADMIN', 0)
    setApiAuthToken('')
    clearWorkspaceAfterLogout()
    loggingOut.value = false
  }
}

function handleKeydown(event) {
  if (!isAuthenticated.value) {
    return
  }
  const target = event.target
  const tagName = (target?.tagName || '').toUpperCase()
  const typingElement = target?.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(tagName)

  const isCommand = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k'
  if (isCommand) {
    event.preventDefault()
    commandOpen.value = true
    commandActiveIndex.value = 0
    return
  }

  const isToggleTreePathFocus = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '0'
  if (isToggleTreePathFocus) {
    event.preventDefault()
    docListRef.value?.toggleTreeFocusPath()
    return
  }

  const isExpandTree = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '1'
  if (isExpandTree) {
    event.preventDefault()
    docListRef.value?.expandAll()
    return
  }

  const isCollapseTree = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '2'
  if (isCollapseTree) {
    event.preventDefault()
    docListRef.value?.collapseAll()
    return
  }

  const isToggleDensity = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '3'
  if (isToggleDensity) {
    event.preventDefault()
    docListRef.value?.toggleCompactMode()
    return
  }

  const isToggleFocusMode = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '4'
  if (isToggleFocusMode) {
    event.preventDefault()
    toggleFocusMode()
    return
  }

  const isCollapseSidebarPanels = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '5'
  if (isCollapseSidebarPanels) {
    event.preventDefault()
    docListRef.value?.collapseSidebarPanels()
    return
  }

  const isExpandSidebarPanels = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '6'
  if (isExpandSidebarPanels) {
    event.preventDefault()
    docListRef.value?.expandSidebarPanels()
    return
  }

  const isFocusPinnedSearch = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '7'
  if (isFocusPinnedSearch) {
    event.preventDefault()
    docListRef.value?.focusQuickSearch('PINNED')
    return
  }

  const isFocusFavoritesSearch = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '8'
  if (isFocusFavoritesSearch) {
    event.preventDefault()
    docListRef.value?.focusQuickSearch('FAVORITES')
    return
  }

  const isFocusRecentSearch = event.altKey && !event.ctrlKey && !event.metaKey && event.key === '9'
  if (isFocusRecentSearch) {
    event.preventDefault()
    docListRef.value?.focusQuickSearch('RECENT')
    return
  }

  const isSave = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's'
  if (isSave) {
    event.preventDefault()
    saveDoc(currentDoc.value)
    return
  }

  if (typingElement) {
    return
  }
}

function closeCommand() {
  commandOpen.value = false
  commandQuery.value = ''
  commandActiveIndex.value = 0
}

function openFirstCommandResult() {
  if (commandResults.value.length === 0 || commandActiveIndex.value < 0) {
    return
  }
  const target = commandResults.value[commandActiveIndex.value] || commandResults.value[0]
  if (!target) {
    return
  }
  selectCommandDoc(target.slug)
}

function onCommandInputKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault()
    closeCommand()
    return
  }
  if (event.key === 'Enter') {
    event.preventDefault()
    openFirstCommandResult()
    return
  }
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    if (commandResults.value.length === 0) {
      return
    }
    commandActiveIndex.value = (commandActiveIndex.value + 1) % commandResults.value.length
    return
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    if (commandResults.value.length === 0) {
      return
    }
    commandActiveIndex.value = (commandActiveIndex.value - 1 + commandResults.value.length) % commandResults.value.length
  }
}

async function selectCommandDoc(slug) {
  closeCommand()
  await loadDoc(slug)
}

onMounted(async () => {
  setUnauthorizedHandler(() => {
    forceLogoutDueToAuthFailure()
  })
  const params = new URLSearchParams(window.location.search)
  shareTokenFromUrl.value = params.get('token') || ''
  const initialPage = params.get('page') || ''
  loadCollections()
  pendingPageSlug.value = initialPage
  const session = loadAuthSessionUser()
  const token = loadApiAuthToken()
  if (session && token && session.expiresAt > Date.now()) {
    try {
      const { data } = await api.get('/auth/me')
      currentUser.value = (data?.username || session.username || '').trim() || session.username
      currentUserRole.value = String(data?.role || session.role || 'ADMIN').trim().toUpperCase() || 'ADMIN'
      loginForm.value.username = currentUser.value
      isAuthenticated.value = true
      await bootstrapWorkspace(initialPage)
      pendingPageSlug.value = ''
    } catch {
      persistAuthSession('', 'ADMIN', 0)
      setApiAuthToken('')
      isAuthenticated.value = false
      loginForm.value.username = currentUser.value || ''
    }
  } else {
    persistAuthSession('', 'ADMIN', 0)
    setApiAuthToken('')
    isAuthenticated.value = false
    loginForm.value.username = currentUser.value || ''
  }
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  setUnauthorizedHandler(null)
  window.removeEventListener('keydown', handleKeydown)
  stopLeftResize()
  if (toastTimer) {
    clearTimeout(toastTimer)
  }
  if (confirmResolver) {
    confirmResolver(false)
    confirmResolver = null
  }
  if (promptResolver) {
    promptResolver(null)
    promptResolver = null
  }
})

watch(commandQuery, () => {
  commandActiveIndex.value = 0
})

watch(commandResults, (list) => {
  if (!list.length) {
    commandActiveIndex.value = 0
    return
  }
  if (commandActiveIndex.value >= list.length) {
    commandActiveIndex.value = list.length - 1
  }
})

watch(currentUser, () => {
  persistCollections()
  syncCollectionsWithDocs()
  if (activeSlug.value && !canViewDoc(currentDoc.value)) {
    openHome()
  }
})

watch(rightPanelOpen, (open) => {
  persistRightPanelState(open)
})

watch(leftPaneWidth, (width) => {
  persistLeftPaneWidth(width)
})

watch(focusMode, (open) => {
  persistFocusModeState(open)
})

function normalizeMembers(values) {
  if (!Array.isArray(values)) {
    return []
  }
  return values
    .map((v) => (v || '').trim())
    .filter((v) => v.length > 0)
}

function canEditDoc(doc) {
  if (!doc) {
    return false
  }
  const role = (currentUserRole.value || '').trim().toUpperCase()
  if (role === 'VIEWER') {
    return false
  }
  if (role === 'ADMIN') {
    return true
  }
  const user = (currentUser.value || '').trim()
  if (!user) {
    return false
  }
  const owner = (doc.owner || '').trim()
  const editors = normalizeMembers(doc.editors)
  if (!owner && editors.length === 0) {
    return true
  }
  return user === owner || editors.includes(user)
}

function canViewDoc(doc) {
  if (!doc) {
    return false
  }
  if (doc.shareEnabled === true && doc.shareToken && shareTokenFromUrl.value === doc.shareToken) {
    return true
  }
  if (canEditDoc(doc)) {
    return true
  }
  const user = (currentUser.value || '').trim()
  if (!user) {
    return false
  }
  const viewers = normalizeMembers(doc.viewers)
  if (viewers.length === 0) {
    return true
  }
  return viewers.includes(user)
}

function sortByOrder(a, b) {
  const orderA = Number.isFinite(a.sortOrder) ? a.sortOrder : 0
  const orderB = Number.isFinite(b.sortOrder) ? b.sortOrder : 0
  if (orderA !== orderB) {
    return orderA - orderB
  }
  return (a.title || '').localeCompare((b.title || ''), 'zh-Hans-CN')
}
</script>
