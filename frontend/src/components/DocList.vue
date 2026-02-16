<template>
  <div class="doc-list">
    <div class="header">
      <div>
        <h2>Space Pages</h2>
      </div>
      <div class="header-actions">
        <button class="secondary tiny" :class="{ active: batchMode }" @click="toggleBatchMode">
          {{ batchMode ? '退出批量' : '批量操作' }}
        </button>
        <span class="batch-count" v-if="batchMode">已选 {{ selectedSlugs.length }}</span>
        <button class="secondary tiny" v-if="batchMode" :class="{ active: selectedOnlyMode }" @click="selectedOnlyMode = !selectedOnlyMode">
          仅看已选
        </button>
        <button class="secondary tiny" v-if="batchMode" :class="{ active: batchQuickOpen }" @click="batchQuickOpen = !batchQuickOpen">
          快速筛选
        </button>
        <button class="secondary tiny" v-if="batchMode" @click="selectAllVisible">全选</button>
        <button class="secondary tiny" v-if="batchMode" @click="invertSelection">反选</button>
        <button class="secondary tiny" v-if="batchMode" @click="clearSelected">清空</button>
        <button class="secondary tiny" v-if="batchMode" :disabled="selectedSlugs.length === 0" @click="emitBulkAction('BULK_MOVE_ROOT')">移到顶级</button>
        <button class="secondary tiny" v-if="batchMode" :disabled="selectedSlugs.length === 0" @click="emitBulkAction('BULK_ARCHIVE')">归档</button>
        <button class="secondary tiny" v-if="batchMode" :disabled="selectedSlugs.length === 0" @click="emitBulkAction('BULK_UNARCHIVE')">恢复</button>
        <button class="secondary tiny" v-if="batchMode" :disabled="selectedSlugs.length === 0" @click="emitBulkAction('BULK_FAVORITE')">收藏</button>
        <button class="secondary tiny" v-if="batchMode" :disabled="selectedSlugs.length === 0" @click="emitBulkAction('BULK_UNFAVORITE')">取消收藏</button>
        <button class="secondary tiny" @click="expandAll">展开</button>
        <button class="secondary tiny" @click="collapseAll">折叠</button>
        <button class="secondary tiny" @click="expandSidebarPanels">展开导航</button>
        <button class="secondary tiny" @click="collapseSidebarPanels">收起导航</button>
        <button class="secondary tiny" :class="{ active: compactMode }" @click="compactMode = !compactMode">
          {{ compactMode ? '舒适视图' : '紧凑视图' }}
        </button>
        <button @click="$emit('create')">+ 新建</button>
      </div>
    </div>

    <div class="doc-list-scroll" :class="{ compact: compactMode }" @click="quickMenuSlug = ''">
      <div class="sidebar-top-tools">
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索文档"
          @keyup.enter="$emit('search', keyword)"
        />

        <div class="search-actions">
          <button class="search-btn" @click="$emit('search', keyword)">搜索</button>
          <button class="search-btn clear" @click="clearSearch">清空</button>
        </div>

        <div class="filter-panel">
        <button class="filter-panel-head" @click="filtersOpen = !filtersOpen">
          <strong>筛选器</strong>
          <span>{{ filtersOpen ? '收起 ▾' : '展开 ▸' }}</span>
        </button>
        <div v-show="filtersOpen">
          <div class="status-filters">
          <button
            class="filter-btn"
            :class="{ active: statusFilter === 'ALL' }"
            @click="statusFilter = 'ALL'"
          >
            全部 {{ statusCounts.ALL }}
          </button>
          <button
            class="filter-btn"
            :class="{ active: statusFilter === 'PUBLISHED' }"
            @click="statusFilter = 'PUBLISHED'"
          >
            已发布 {{ statusCounts.PUBLISHED }}
          </button>
          <button
            class="filter-btn"
            :class="{ active: statusFilter === 'DRAFT' }"
            @click="statusFilter = 'DRAFT'"
          >
            草稿 {{ statusCounts.DRAFT }}
          </button>
          <button
            class="filter-btn"
            :class="{ active: statusFilter === 'ARCHIVED' }"
            @click="statusFilter = 'ARCHIVED'"
          >
            已归档 {{ statusCounts.ARCHIVED }}
          </button>
          </div>

          <div class="visibility-filters">
          <button
            class="filter-btn"
            :class="{ active: visibilityFilter === 'ALL' }"
            @click="visibilityFilter = 'ALL'"
          >
            全可见性 {{ visibilityCounts.ALL }}
          </button>
          <button
            class="filter-btn"
            :class="{ active: visibilityFilter === 'SPACE' }"
            @click="visibilityFilter = 'SPACE'"
          >
            空间可见 {{ visibilityCounts.SPACE }}
          </button>
          <button
            class="filter-btn"
            :class="{ active: visibilityFilter === 'PRIVATE' }"
            @click="visibilityFilter = 'PRIVATE'"
          >
            私有 {{ visibilityCounts.PRIVATE }}
          </button>
          </div>

          <div class="priority-filters">
          <button class="filter-btn" :class="{ active: priorityFilter === 'ALL' }" @click="priorityFilter = 'ALL'">
            优先级全部
          </button>
          <button class="filter-btn" :class="{ active: priorityFilter === 'HIGH' }" @click="priorityFilter = 'HIGH'">
            高优先级
          </button>
          <button class="filter-btn" :class="{ active: priorityFilter === 'MEDIUM' }" @click="priorityFilter = 'MEDIUM'">
            中优先级
          </button>
          <button class="filter-btn" :class="{ active: priorityFilter === 'LOW' }" @click="priorityFilter = 'LOW'">
            低优先级
          </button>
          </div>

          <div class="permission-filters">
          <button class="filter-btn" :class="{ active: permissionFilter === 'ALL' }" @click="permissionFilter = 'ALL'">
            权限全部 {{ permissionCounts.ALL }}
          </button>
          <button class="filter-btn" :class="{ active: permissionFilter === 'OWNED' }" @click="permissionFilter = 'OWNED'">
            我拥有 {{ permissionCounts.OWNED }}
          </button>
          <button class="filter-btn" :class="{ active: permissionFilter === 'EDITABLE' }" @click="permissionFilter = 'EDITABLE'">
            我可编辑 {{ permissionCounts.EDITABLE }}
          </button>
          <button class="filter-btn" :class="{ active: permissionFilter === 'VIEWABLE' }" @click="permissionFilter = 'VIEWABLE'">
            我可查看 {{ permissionCounts.VIEWABLE }}
          </button>
          <button class="filter-btn" :class="{ active: permissionFilter === 'SHARED' }" @click="permissionFilter = 'SHARED'">
            共享给我 {{ permissionCounts.SHARED }}
          </button>
          </div>

          <div class="meta-filters">
          <select v-model="assigneeFilter">
            <option value="">负责人（全部）</option>
            <option v-for="name in assigneeOptions" :key="name" :value="name">{{ name }}</option>
          </select>
          <select v-model="dueFilter">
            <option value="ALL">截止日期（全部）</option>
            <option value="HAS_DUE">有截止日期</option>
            <option value="OVERDUE">已逾期</option>
          </select>
          </div>
          <div class="todo-toggle-row">
            <button class="filter-btn" :class="{ active: myTodoMode }" @click="toggleMyTodoMode">
              我的待办视图
            </button>
          </div>
        </div>
        </div>

        <div class="batch-quick-panel" v-if="batchMode">
        <button class="batch-quick-head" @click="batchQuickOpen = !batchQuickOpen">
          <strong>批量快速筛选</strong>
          <span>{{ batchQuickOpen ? '收起 ▾' : '展开 ▸' }}</span>
        </button>
        <div class="batch-quick-body" v-show="batchQuickOpen">
          <button class="secondary tiny" @click="selectByStatus('DRAFT')">选草稿</button>
          <button class="secondary tiny" @click="selectByStatus('PUBLISHED')">选已发布</button>
          <button class="secondary tiny" @click="selectByStatus('ARCHIVED')">选已归档</button>
          <button class="secondary tiny" @click="selectByPriority('HIGH')">选高优</button>
          <button class="secondary tiny" @click="selectByPriority('MEDIUM')">选中优</button>
          <button class="secondary tiny" @click="selectByPriority('LOW')">选低优</button>
          <button class="secondary tiny" @click="selectByDue('TODAY')">选今天到期</button>
          <button class="secondary tiny" @click="selectByDue('OVERDUE')">选已逾期</button>
          <button class="secondary tiny" @click="selectByVisibility('SPACE')">选空间可见</button>
          <button class="secondary tiny" @click="selectByVisibility('PRIVATE')">选私有</button>
          <button class="secondary tiny" @click="selectByAssigneeMe">选我负责</button>
          <button class="secondary tiny" @click="selectByEditableMe">选我可编辑</button>
        </div>
        </div>

        <div class="quick-zones">
          <div class="quick-zones-tools">
            <button class="secondary tiny" @click="expandQuickZones">展开三组</button>
            <button class="secondary tiny" @click="collapseQuickZones">收起三组</button>
          </div>
          <div class="quick-zone" :class="{ open: quickOpenPinned }">
            <button class="quick-zone-head" @click="quickOpenPinned = !quickOpenPinned">
              <div class="quick-zone-title">
                <h4>📌 固定</h4>
                <em>{{ pinnedDocs.length }}</em>
              </div>
              <div class="quick-zone-actions">
                <button
                  v-if="pinnedDocs.length > 0"
                  class="secondary tiny"
                  @click.stop="emitQuickCollectionAction('CLEAR_PINNED')"
                >
                  清空
                </button>
                <span>{{ quickOpenPinned ? '▾' : '▸' }}</span>
              </div>
            </button>
            <div class="quick-zone-search" v-show="quickOpenPinned">
              <input ref="pinnedSearchRef" v-model.trim="quickPinnedQuery" placeholder="搜索固定页面" />
            </div>
            <ul class="quick-list" v-show="quickOpenPinned">
              <li
                v-for="doc in pinnedDocsFiltered"
                :key="`pin-${doc.slug}`"
                :class="{
                  active: activeSlug === doc.slug,
                  dragging: quickDragType === 'PINNED' && quickDragSlug === doc.slug,
                  'drop-target': quickDragType === 'PINNED' && quickDropSlug === doc.slug
                }"
                draggable="true"
                @dragstart="onQuickDragStart('PINNED', doc.slug)"
                @dragover.prevent="onQuickDragOver('PINNED', doc.slug)"
                @drop.prevent="onQuickDrop('PINNED', doc.slug)"
                @dragend="onQuickDragEnd"
                @click="$emit('select', doc.slug)"
              >
                <span>{{ doc.title }}</span>
              </li>
              <li class="quick-empty" v-if="pinnedDocs.length === 0">还没有固定页面</li>
              <li class="quick-empty" v-else-if="pinnedDocsFiltered.length === 0">没有匹配固定页面</li>
            </ul>
          </div>

          <div class="quick-zone" :class="{ open: quickOpenFavorites }">
            <button class="quick-zone-head" @click="quickOpenFavorites = !quickOpenFavorites">
              <div class="quick-zone-title">
                <h4>⭐ 收藏</h4>
                <em>{{ favoriteDocs.length }}</em>
              </div>
              <div class="quick-zone-actions">
                <button
                  v-if="favoriteDocs.length > 0"
                  class="secondary tiny"
                  @click.stop="emitQuickCollectionAction('CLEAR_FAVORITES')"
                >
                  清空
                </button>
                <span>{{ quickOpenFavorites ? '▾' : '▸' }}</span>
              </div>
            </button>
            <div class="quick-zone-search" v-show="quickOpenFavorites">
              <input ref="favoritesSearchRef" v-model.trim="quickFavoritesQuery" placeholder="搜索收藏页面" />
            </div>
            <ul class="quick-list" v-show="quickOpenFavorites">
              <li
                v-for="doc in favoriteDocsFiltered"
                :key="`fav-${doc.slug}`"
                :class="{
                  active: activeSlug === doc.slug,
                  dragging: quickDragType === 'FAVORITES' && quickDragSlug === doc.slug,
                  'drop-target': quickDragType === 'FAVORITES' && quickDropSlug === doc.slug
                }"
                draggable="true"
                @dragstart="onQuickDragStart('FAVORITES', doc.slug)"
                @dragover.prevent="onQuickDragOver('FAVORITES', doc.slug)"
                @drop.prevent="onQuickDrop('FAVORITES', doc.slug)"
                @dragend="onQuickDragEnd"
                @click="$emit('select', doc.slug)"
              >
                <span>{{ doc.title }}</span>
              </li>
              <li class="quick-empty" v-if="favoriteDocs.length === 0">还没有收藏页面</li>
              <li class="quick-empty" v-else-if="favoriteDocsFiltered.length === 0">没有匹配收藏页面</li>
            </ul>
          </div>

          <div class="quick-zone" :class="{ open: quickOpenRecent }">
            <button class="quick-zone-head" @click="quickOpenRecent = !quickOpenRecent">
              <div class="quick-zone-title">
                <h4>🕘 最近访问</h4>
                <em>{{ recentDocs.length }}</em>
              </div>
              <div class="quick-zone-actions">
                <button
                  class="secondary tiny"
                  :class="{ active: autoCleanRecentOlder }"
                  @click.stop="emitQuickCollectionAction('TOGGLE_AUTO_CLEAN_RECENT_OLDER')"
                >
                  自动清理更早 {{ autoCleanRecentOlder ? '开' : '关' }}
                </button>
                <button
                  v-if="olderRecentSlugs.length > 0"
                  class="secondary tiny"
                  @click.stop="emitQuickCollectionAction('CLEAR_RECENT_OLDER', olderRecentSlugs)"
                >
                  清理更早
                </button>
                <button
                  v-if="recentDocs.length > 0"
                  class="secondary tiny"
                  @click.stop="emitQuickCollectionAction('CLEAR_RECENT_ALL')"
                >
                  清空
                </button>
                <span>{{ quickOpenRecent ? '▾' : '▸' }}</span>
              </div>
            </button>
            <div class="quick-zone-search" v-show="quickOpenRecent">
              <input ref="recentSearchRef" v-model.trim="quickRecentQuery" placeholder="搜索最近访问页面" />
            </div>
            <ul class="quick-list" v-show="quickOpenRecent">
              <template v-for="section in recentSectionsFiltered" :key="section.key">
                <li class="quick-subhead">
                  <span>{{ section.title }}</span>
                  <em>{{ section.hint }}</em>
                </li>
                <li
                  v-for="doc in section.items"
                  :key="`recent-${doc.slug}`"
                  :class="{
                    active: activeSlug === doc.slug,
                    dragging: quickDragType === 'RECENT' && quickDragSlug === doc.slug,
                    'drop-target': quickDragType === 'RECENT' && quickDropSlug === doc.slug
                  }"
                  draggable="true"
                  @dragstart="onQuickDragStart('RECENT', doc.slug)"
                  @dragover.prevent="onQuickDragOver('RECENT', doc.slug)"
                  @drop.prevent="onQuickDrop('RECENT', doc.slug)"
                  @dragend="onQuickDragEnd"
                  @click="$emit('select', doc.slug)"
                >
                  <span>{{ doc.title }}</span>
                </li>
              </template>
              <li class="quick-empty" v-if="recentDocs.length === 0">还没有访问记录</li>
              <li class="quick-empty" v-else-if="recentSectionsFiltered.length === 0">没有匹配最近访问页面</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="tree-scroll-region" ref="treeScrollRef" @scroll="onTreeScroll">
        <div class="tree-nav">
        <button class="tree-nav-head" @click="treeOpen = !treeOpen">
          <span>{{ treeOpen ? '▾' : '▸' }} 页面树</span>
          <em>{{ totalVisibleNodes }}</em>
        </button>
        <div class="tree-nav-tools" v-show="treeOpen">
          <button class="secondary tiny" :class="{ active: treeFocusPath }" @click="toggleTreeFocusPath">
            {{ treeFocusPath ? '路径聚焦开' : '路径聚焦关' }}
          </button>
        </div>
        <div class="tree-nav-body" v-show="treeOpen">
          <div
            class="root-drop-zone"
            :class="{ active: dropTargetRoot }"
            @dragover.prevent="onRootDragOver"
            @dragleave="onRootDragLeave"
            @drop.prevent="onDropRoot"
          >
            拖拽到这里设为顶级页面
          </div>

          <div
            v-for="section in visibilitySections"
            :key="section.key"
            :class="['tree-section', section.key === 'PRIVATE' ? 'private' : 'space']"
          >
            <button class="tree-section-head" @click="toggleSection(section.key)">
              <span>{{ isSectionOpen(section.key) ? '▾' : '▸' }} {{ section.title }}</span>
              <em>{{ section.groups.reduce((sum, group) => sum + group.items.length, 0) }}</em>
            </button>
            <div v-show="isSectionOpen(section.key)">
              <div
                v-for="group in section.groups"
                :key="group.id"
                class="tree-group"
              >
                <button class="tree-group-head" @click="toggleGroup(group.id)">
                  <span>{{ opened[group.id] ? '▾' : '▸' }} {{ group.name }}</span>
                  <em>{{ group.items.length }}</em>
                </button>

                <ul class="doc-items" v-show="opened[group.id]">
                  <li
                    v-for="node in group.items"
                    :key="node.slug"
                    :data-node-slug="node.slug"
                    :class="[
                      'tree-node',
                      depthClass(node.depth),
                      {
                        active: activeSlug === node.slug,
                        ancestor: activeAncestorSet.has(node.slug) && activeSlug !== node.slug,
                        'drag-target': dropTargetSlug === node.slug
                      }
                    ]"
                    :style="{ '--node-indent': `${12 + node.depth * 22}px` }"
                    draggable="true"
                    @dragstart="onDragStart(node.slug)"
                    @dragend="onDragEnd"
                    @dragover.prevent="onDragOver(node.slug)"
                    @dragleave="onDragLeave(node.slug)"
                    @drop.prevent="onDropNode(node.slug)"
                    @click="onNodeClick(node.slug)"
                    tabindex="0"
                    @keydown="onNodeKeydown($event, node.slug)"
                  >
                    <div class="node-title-row">
                      <div class="node-title-main">
                        <input
                          v-if="batchMode"
                          type="checkbox"
                          class="node-check"
                          :checked="selectedSlugs.includes(node.slug)"
                          @click.stop="toggleSelected(node.slug)"
                        />
                        <button
                          v-if="node.hasChildren"
                          class="node-expand-btn"
                          @click.stop="toggleNodeExpanded(node.slug)"
                          :aria-label="node.expanded ? '收起子页面' : '展开子页面'"
                        >
                          {{ node.expanded ? '▾' : '▸' }}
                        </button>
                        <span v-else class="node-expand-spacer"></span>
                        <span class="node-type-dot" :class="{ root: node.depth === 0 }"></span>
                        <strong>{{ node.title }}</strong>
                        <span class="node-child-count" v-if="directChildrenCount[node.slug]">
                          {{ directChildrenCount[node.slug] }} 子页
                        </span>
                      </div>
                      <div class="node-more">
                        <button class="node-more-btn" @click.stop="toggleQuickMenu(node.slug)">⋯</button>
                        <div v-if="quickMenuSlug === node.slug" class="node-menu" @click.stop>
                          <button class="node-menu-item" @click="emitQuickAction('OPEN_PAGE', node.slug)">
                            打开页面
                          </button>
                          <button class="node-menu-item" @click="emitQuickAction('RENAME', node.slug)">
                            重命名标题
                          </button>
                          <button class="node-menu-item" @click="emitQuickAction('MOVE_ROOT', node.slug)">
                            设为顶级页面
                          </button>
                          <button
                            class="node-menu-item"
                            @click="emitQuickAction((node.status || 'DRAFT') === 'ARCHIVED' ? 'UNARCHIVE' : 'ARCHIVE', node.slug)"
                          >
                            {{ (node.status || 'DRAFT') === 'ARCHIVED' ? '恢复为草稿' : '归档页面' }}
                          </button>
                          <button class="node-menu-item" @click="emitQuickAction('COPY_LINK', node.slug)">
                            复制页面链接
                          </button>
                          <button class="node-menu-item" @click="emitQuickAction('COPY_SLUG', node.slug)">
                            复制 slug
                          </button>
                          <button class="node-menu-item" @click="emitQuickAction('TOGGLE_FAVORITE', node.slug)">
                            {{ favorites.includes(node.slug) ? '取消收藏' : '加入收藏' }}
                          </button>
                          <button class="node-menu-item" @click="$emit('toggle-pin', node.slug)">
                            {{ pinned.includes(node.slug) ? '取消固定' : '固定到侧栏' }}
                          </button>
                        </div>
                      </div>
                      <button
                        class="pin-toggle"
                        :class="{ active: pinned.includes(node.slug) }"
                        @click.stop="$emit('toggle-pin', node.slug)"
                      >
                        {{ pinned.includes(node.slug) ? '📌' : '📍' }}
                      </button>
                      <button
                        class="fav-toggle"
                        :class="{ active: favorites.includes(node.slug) }"
                        @click.stop="$emit('toggle-favorite', node.slug)"
                      >
                        {{ favorites.includes(node.slug) ? '★' : '☆' }}
                      </button>
                      <div class="order-controls">
                        <button class="order-btn" @click.stop="emit('reorder', { slug: node.slug, direction: 'UP' })">↑</button>
                        <button class="order-btn" @click.stop="emit('reorder', { slug: node.slug, direction: 'DOWN' })">↓</button>
                      </div>
                    </div>
                    <div class="node-meta-row">
                      <span class="node-slug">{{ node.slug }}</span>
                      <span class="node-visibility" :class="(node.visibility || 'SPACE').toLowerCase()">
                        {{ node.visibility === 'PRIVATE' ? '私有' : '空间' }}
                      </span>
                      <span class="node-perm" :class="resolvePermissionClass(node)">
                        {{ resolvePermissionLabel(node) }}
                      </span>
                      <span class="node-lock" :class="{ locked: !!node.locked }">
                        {{ node.locked ? '锁定' : '可编辑' }}
                      </span>
                      <span class="node-status" :class="(node.status || 'DRAFT').toLowerCase()">
                        {{ statusText(node.status) }}
                      </span>
                      <span class="node-priority" :class="(node.priority || 'MEDIUM').toLowerCase()">
                        {{ priorityText(node.priority) }}
                      </span>
                    </div>
                    <p>{{ node.summary }}</p>
                    <div class="label-row">
                      <span class="node-owner">👤 {{ node.assignee || '-' }}</span>
                      <span class="node-owner">⏰ {{ node.dueDate || '-' }}</span>
                    </div>
                    <div class="label-row" v-if="node.labels && node.labels.length">
                      <span class="doc-label" v-for="label in node.labels.slice(0, 3)" :key="label">{{ label }}</span>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
          </div>
          <div v-if="totalVisibleNodes === 0" class="quick-empty">当前筛选条件下没有页面</div>
        </div>
        </div>
      </div>
  </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'

const QUICK_PANEL_KEY = 'ga-sidebar-quick-panels'
const FILTER_PANEL_KEY = 'ga-sidebar-filter-panel'
const GROUP_OPEN_KEY = 'ga-sidebar-open-groups'
const SECTION_OPEN_KEY = 'ga-sidebar-open-sections'
const DENSITY_KEY = 'ga-sidebar-density'
const TREE_PANEL_KEY = 'ga-sidebar-tree-panel'
const NODE_OPEN_KEY = 'ga-sidebar-open-nodes'
const TREE_SCROLL_KEY = 'ga-sidebar-tree-scroll'
const TREE_FOCUS_PATH_KEY = 'ga-sidebar-focus-path'

function loadQuickPanelsState() {
  if (typeof window === 'undefined') {
    return { pinned: true, favorites: true, recent: true }
  }
  try {
    const raw = window.localStorage.getItem(QUICK_PANEL_KEY)
    if (!raw) {
      return { pinned: true, favorites: true, recent: true }
    }
    const parsed = JSON.parse(raw)
    return {
      pinned: parsed.pinned !== false,
      favorites: parsed.favorites !== false,
      recent: parsed.recent !== false
    }
  } catch {
    return { pinned: true, favorites: true, recent: true }
  }
}

function persistQuickPanelsState(pinnedOpen, favoritesOpen, recentOpen) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(QUICK_PANEL_KEY, JSON.stringify({
    pinned: pinnedOpen,
    favorites: favoritesOpen,
    recent: recentOpen
  }))
}

function loadFilterPanelState() {
  if (typeof window === 'undefined') {
    return true
  }
  const raw = window.localStorage.getItem(FILTER_PANEL_KEY)
  if (raw === null) {
    return true
  }
  return raw !== '0'
}

function persistFilterPanelState(open) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(FILTER_PANEL_KEY, open ? '1' : '0')
}

function loadGroupOpenState() {
  if (typeof window === 'undefined') {
    return {}
  }
  try {
    const raw = window.localStorage.getItem(GROUP_OPEN_KEY)
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

function persistGroupOpenState(state) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(GROUP_OPEN_KEY, JSON.stringify(state))
}

function loadSectionOpenState() {
  if (typeof window === 'undefined') {
    return {}
  }
  try {
    const raw = window.localStorage.getItem(SECTION_OPEN_KEY)
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

function persistSectionOpenState(state) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(SECTION_OPEN_KEY, JSON.stringify(state))
}

function loadCompactMode() {
  if (typeof window === 'undefined') {
    return false
  }
  return window.localStorage.getItem(DENSITY_KEY) === 'compact'
}

function persistCompactMode(compact) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(DENSITY_KEY, compact ? 'compact' : 'comfortable')
}

function loadTreePanelState() {
  if (typeof window === 'undefined') {
    return true
  }
  const raw = window.localStorage.getItem(TREE_PANEL_KEY)
  if (raw === null) {
    return true
  }
  return raw !== '0'
}

function persistTreePanelState(open) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(TREE_PANEL_KEY, open ? '1' : '0')
}

function loadNodeOpenState() {
  if (typeof window === 'undefined') {
    return {}
  }
  try {
    const raw = window.localStorage.getItem(NODE_OPEN_KEY)
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

function persistNodeOpenState(state) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(NODE_OPEN_KEY, JSON.stringify(state))
}

function loadTreeScrollPosition() {
  if (typeof window === 'undefined') {
    return 0
  }
  const raw = Number(window.localStorage.getItem(TREE_SCROLL_KEY) || '')
  if (!Number.isFinite(raw) || raw < 0) {
    return 0
  }
  return raw
}

function persistTreeScrollPosition(top) {
  if (typeof window === 'undefined') {
    return
  }
  const next = Math.max(0, Math.round(top))
  window.localStorage.setItem(TREE_SCROLL_KEY, String(next))
}

function loadTreeFocusPathState() {
  if (typeof window === 'undefined') {
    return false
  }
  return window.localStorage.getItem(TREE_FOCUS_PATH_KEY) === '1'
}

function persistTreeFocusPathState(enabled) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(TREE_FOCUS_PATH_KEY, enabled ? '1' : '0')
}

const keyword = ref('')
const opened = ref(loadGroupOpenState())
const sectionOpened = ref(loadSectionOpenState())
const statusFilter = ref('ALL')
const visibilityFilter = ref('ALL')
const priorityFilter = ref('ALL')
const permissionFilter = ref('ALL')
const assigneeFilter = ref('')
const dueFilter = ref('ALL')
const myTodoMode = ref(false)
const filtersOpen = ref(loadFilterPanelState())
const quickPanelsState = loadQuickPanelsState()
const quickOpenPinned = ref(quickPanelsState.pinned)
const quickOpenFavorites = ref(quickPanelsState.favorites)
const quickOpenRecent = ref(quickPanelsState.recent)
const draggingSlug = ref('')
const dropTargetSlug = ref('')
const dropTargetRoot = ref(false)
const quickMenuSlug = ref('')
const quickDragType = ref('')
const quickDragSlug = ref('')
const quickDropSlug = ref('')
const quickPinnedQuery = ref('')
const quickFavoritesQuery = ref('')
const quickRecentQuery = ref('')
const pinnedSearchRef = ref(null)
const favoritesSearchRef = ref(null)
const recentSearchRef = ref(null)
const batchMode = ref(false)
const selectedSlugs = ref([])
const selectedOnlyMode = ref(false)
const compactMode = ref(loadCompactMode())
const batchQuickOpen = ref(false)
const treeOpen = ref(loadTreePanelState())
const treeFocusPath = ref(loadTreeFocusPathState())
const nodeOpened = ref(loadNodeOpenState())
const treeScrollRef = ref(null)

const props = defineProps({
  docs: {
    type: Array,
    default: () => []
  },
  activeSlug: {
    type: String,
    default: ''
  },
  favorites: {
    type: Array,
    default: () => []
  },
  pinned: {
    type: Array,
    default: () => []
  },
  recent: {
    type: Array,
    default: () => []
  },
  recentMeta: {
    type: Object,
    default: () => ({})
  },
  autoCleanRecentOlder: {
    type: Boolean,
    default: false
  },
  currentUser: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['search', 'toggle-favorite', 'toggle-pin', 'move', 'reorder', 'quick-action', 'bulk-action', 'reorder-quick', 'quick-collection-action', 'tree-focus-change'])

watch([quickOpenPinned, quickOpenFavorites, quickOpenRecent], ([pinnedOpen, favoritesOpen, recentOpen]) => {
  persistQuickPanelsState(pinnedOpen, favoritesOpen, recentOpen)
})

watch(filtersOpen, (open) => {
  persistFilterPanelState(open)
})

watch(opened, (value) => {
  persistGroupOpenState(value)
}, { deep: true })

watch(sectionOpened, (value) => {
  persistSectionOpenState(value)
}, { deep: true })

watch(compactMode, (compact) => {
  persistCompactMode(compact)
})

watch(treeOpen, (open) => {
  persistTreePanelState(open)
})

watch(treeFocusPath, (enabled) => {
  persistTreeFocusPathState(enabled)
  emit('tree-focus-change', enabled)
})

watch(nodeOpened, (state) => {
  persistNodeOpenState(state)
}, { deep: true })

onMounted(() => {
  emit('tree-focus-change', treeFocusPath.value)
  const node = treeScrollRef.value
  if (!node) {
    return
  }
  node.scrollTop = loadTreeScrollPosition()
})

watch(() => props.docs, () => {
  const valid = new Set(props.docs.map((doc) => doc.slug))
  selectedSlugs.value = selectedSlugs.value.filter((slug) => valid.has(slug))
  const next = {}
  Object.entries(nodeOpened.value).forEach(([slug, open]) => {
    if (valid.has(slug)) {
      next[slug] = open
    }
  })
  nodeOpened.value = next
}, { deep: true })

watch(selectedSlugs, (list) => {
  if (list.length === 0) {
    selectedOnlyMode.value = false
  }
})

const statusFilteredDocs = computed(() => {
  if (statusFilter.value === 'ALL') {
    return props.docs
  }
  return props.docs.filter((doc) => (doc.status || 'DRAFT') === statusFilter.value)
})

const propertyFilteredDocs = computed(() => {
  const selectedSet = new Set(selectedSlugs.value)
  return statusFilteredDocs.value.filter((doc) => {
    const permissionPass = matchPermissionFilter(doc)
    const priorityPass = priorityFilter.value === 'ALL' || (doc.priority || 'MEDIUM') === priorityFilter.value
    const assigneePass = !assigneeFilter.value || (doc.assignee || '') === assigneeFilter.value
    let duePass = true
    if (dueFilter.value === 'HAS_DUE') {
      duePass = !!doc.dueDate
    } else if (dueFilter.value === 'OVERDUE') {
      duePass = !!doc.dueDate && doc.dueDate < new Date().toISOString().slice(0, 10)
    }
    const todoPass = !myTodoMode.value || ((doc.assignee || '') === assigneeFilter.value && (doc.status || 'DRAFT') !== 'ARCHIVED')
    const selectedPass = !selectedOnlyMode.value || selectedSet.has(doc.slug)
    return permissionPass && priorityPass && assigneePass && duePass && todoPass && selectedPass
  })
})

const assigneeOptions = computed(() => {
  return Array.from(new Set(
    props.docs
      .map((doc) => (doc.assignee || '').trim())
      .filter((name) => name.length > 0)
  )).sort((a, b) => a.localeCompare(b, 'zh-Hans-CN'))
})

const statusCounts = computed(() => {
  const published = props.docs.filter((doc) => (doc.status || 'DRAFT') === 'PUBLISHED').length
  const archived = props.docs.filter((doc) => (doc.status || 'DRAFT') === 'ARCHIVED').length
  const draft = props.docs.length - published - archived
  return {
    ALL: props.docs.length,
    PUBLISHED: published,
    DRAFT: draft,
    ARCHIVED: archived
  }
})

const visibilityCounts = computed(() => {
  const space = props.docs.filter((doc) => (doc.visibility || 'SPACE') === 'SPACE').length
  const privateCount = props.docs.length - space
  return {
    ALL: props.docs.length,
    SPACE: space,
    PRIVATE: privateCount
  }
})

const permissionCounts = computed(() => {
  return {
    ALL: props.docs.length,
    OWNED: props.docs.filter((doc) => isOwnedByMe(doc)).length,
    EDITABLE: props.docs.filter((doc) => canEditMe(doc)).length,
    VIEWABLE: props.docs.filter((doc) => canViewMe(doc)).length,
    SHARED: props.docs.filter((doc) => isSharedWithMe(doc)).length
  }
})

const visibilitySections = computed(() => {
  if (visibilityFilter.value === 'ALL') {
    return [
      {
        key: 'SPACE',
        title: '空间页面',
        groups: buildGroups(propertyFilteredDocs.value.filter((doc) => (doc.visibility || 'SPACE') === 'SPACE'), 'SPACE')
      },
      {
        key: 'PRIVATE',
        title: '私有页面',
        groups: buildGroups(propertyFilteredDocs.value.filter((doc) => (doc.visibility || 'SPACE') === 'PRIVATE'), 'PRIVATE')
      }
    ].filter((section) => section.groups.length > 0)
  }

  const sourceDocs = propertyFilteredDocs.value.filter((doc) => (doc.visibility || 'SPACE') === visibilityFilter.value)
  const title = visibilityFilter.value === 'PRIVATE' ? '私有页面' : '空间页面'
  return [{
    key: visibilityFilter.value,
    title,
    groups: buildGroups(sourceDocs, visibilityFilter.value)
  }]
})

const totalVisibleNodes = computed(() => {
  return visibilitySections.value.reduce((sectionSum, section) => {
    return sectionSum + section.groups.reduce((groupSum, group) => groupSum + group.items.length, 0)
  }, 0)
})

const directChildrenCount = computed(() => {
  const result = {}
  props.docs.forEach((doc) => {
    const parent = (doc.parentSlug || '').trim()
    if (!parent) {
      return
    }
    result[parent] = (result[parent] || 0) + 1
  })
  return result
})

const visibleRows = computed(() => {
  const rows = []
  visibilitySections.value.forEach((section) => {
    if (!isSectionOpen(section.key)) {
      return
    }
    section.groups.forEach((group) => {
      if (!opened.value[group.id]) {
        return
      }
      group.items.forEach((item) => {
        rows.push({
          slug: item.slug,
          groupId: group.id,
          sectionKey: section.key,
          item
        })
      })
    })
  })
  return rows
})

const visibleRowIndex = computed(() => {
  const map = new Map()
  visibleRows.value.forEach((row, index) => {
    map.set(row.slug, index)
  })
  return map
})

const visibleRowBySlug = computed(() => {
  const map = new Map()
  visibleRows.value.forEach((row) => {
    map.set(row.slug, row)
  })
  return map
})

watch(() => props.activeSlug, async (slug) => {
  if (!slug) {
    return
  }
  applyPathFocusState(slug)
  await nextTick()
  const node = document.querySelector(`[data-node-slug="${slug}"]`)
  if (node && typeof node.scrollIntoView === 'function') {
    node.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }
}, { immediate: true })

const activeAncestorSet = computed(() => {
  if (!props.activeSlug) {
    return new Set()
  }
  const bySlug = new Map(props.docs.map((doc) => [doc.slug, doc]))
  const result = new Set()
  let cursor = bySlug.get(props.activeSlug)
  const visited = new Set()
  while (cursor && cursor.parentSlug && !visited.has(cursor.parentSlug)) {
    visited.add(cursor.parentSlug)
    result.add(cursor.parentSlug)
    cursor = bySlug.get(cursor.parentSlug)
  }
  return result
})

function buildGroups(sourceDocs, sectionKey) {
  const docsBySlug = new Map(sourceDocs.map((doc) => [doc.slug, doc]))
  const childrenByParent = new Map()

  sourceDocs.forEach((doc) => {
    const parent = doc.parentSlug || '__root__'
    if (!childrenByParent.has(parent)) {
      childrenByParent.set(parent, [])
    }
    childrenByParent.get(parent).push(doc)
  })

  const roots = sourceDocs
    .filter((doc) => !doc.parentSlug || !docsBySlug.has(doc.parentSlug))
    .slice()
    .sort(sortByOrder)
  const map = new Map()

  roots.forEach((root) => {
    const groupName = resolveGroup(root.slug)
    const groupId = `${sectionKey}:${groupName}`
    if (!map.has(groupId)) {
      map.set(groupId, { name: groupName, items: [] })
      if (opened.value[groupId] === undefined) {
        opened.value[groupId] = true
      }
    }
    map.get(groupId).items.push(...flattenTree(root, childrenByParent, 0))
  })

  return Array.from(map.entries())
    .map(([id, group]) => ({
      id,
      name: group.name,
      items: group.items
    }))
    .sort((a, b) => a.name.localeCompare(b.name, 'zh-Hans-CN'))
}

function applyPathFocusState(slug) {
  const bySlug = new Map(props.docs.map((doc) => [doc.slug, doc]))
  const ancestors = new Set()
  let cursor = bySlug.get(slug)
  const visited = new Set()
  while (cursor && cursor.parentSlug && !visited.has(cursor.parentSlug)) {
    visited.add(cursor.parentSlug)
    ancestors.add(cursor.parentSlug)
    cursor = bySlug.get(cursor.parentSlug)
  }

  if (treeFocusPath.value) {
    const parentSlugs = new Set(
      props.docs
        .map((doc) => (doc.parentSlug || '').trim())
        .filter((item) => item.length > 0)
    )
    const nextNodes = {}
    parentSlugs.forEach((parent) => {
      nextNodes[parent] = ancestors.has(parent)
    })
    nodeOpened.value = nextNodes

    const nextSections = {}
    const nextGroups = {}
    visibilitySections.value.forEach((section) => {
      const sectionContains = section.groups.some((group) => group.items.some((item) => item.slug === slug))
      nextSections[section.key] = sectionContains
      section.groups.forEach((group) => {
        nextGroups[group.id] = sectionContains && group.items.some((item) => item.slug === slug)
      })
    })
    sectionOpened.value = nextSections
    opened.value = nextGroups
    return
  }

  if (ancestors.size > 0) {
    const nextNodes = { ...nodeOpened.value }
    ancestors.forEach((item) => {
      nextNodes[item] = true
    })
    nodeOpened.value = nextNodes
  }
  const nextGroups = { ...opened.value }
  visibilitySections.value.forEach((section) => {
    section.groups.forEach((group) => {
      if (group.items.some((item) => item.slug === slug)) {
        nextGroups[group.id] = true
      }
    })
  })
  opened.value = nextGroups
}

const favoriteDocs = computed(() => {
  const bySlug = new Map(props.docs.map((d) => [d.slug, d]))
  return props.favorites
    .map((slug) => bySlug.get(slug))
    .filter(Boolean)
})

const pinnedDocs = computed(() => {
  const bySlug = new Map(props.docs.map((d) => [d.slug, d]))
  return props.pinned
    .map((slug) => bySlug.get(slug))
    .filter(Boolean)
})

const recentDocs = computed(() => {
  const bySlug = new Map(props.docs.map((d) => [d.slug, d]))
  return props.recent
    .map((slug) => bySlug.get(slug))
    .filter(Boolean)
})

function matchesQuickQuery(doc, rawQuery) {
  const query = String(rawQuery || '').trim().toLowerCase()
  if (!query) {
    return true
  }
  const title = String(doc?.title || '').toLowerCase()
  const slug = String(doc?.slug || '').toLowerCase()
  return title.includes(query) || slug.includes(query)
}

const pinnedDocsFiltered = computed(() => pinnedDocs.value.filter((doc) => matchesQuickQuery(doc, quickPinnedQuery.value)))
const favoriteDocsFiltered = computed(() => favoriteDocs.value.filter((doc) => matchesQuickQuery(doc, quickFavoritesQuery.value)))

const recentSections = computed(() => {
  if (!recentDocs.value.length) {
    return []
  }
  const now = Date.now()
  const dayMs = 24 * 3600 * 1000
  const buckets = {
    today: [],
    week: [],
    older: []
  }
  recentDocs.value.forEach((doc) => {
    const visitAt = Number(props.recentMeta?.[doc.slug] || 0)
    const diff = now - visitAt
    if (visitAt > 0 && diff < dayMs) {
      buckets.today.push(doc)
    } else if (visitAt > 0 && diff < dayMs * 7) {
      buckets.week.push(doc)
    } else {
      buckets.older.push(doc)
    }
  })
  return [
    { key: 'today', title: `今天 (${buckets.today.length})`, hint: '24 小时内访问', items: buckets.today },
    { key: 'week', title: `近 7 天 (${buckets.week.length})`, hint: '1-7 天内访问', items: buckets.week },
    { key: 'older', title: `更早 (${buckets.older.length})`, hint: '超过 7 天', items: buckets.older }
  ].filter((section) => section.items.length > 0)
})

const recentSectionsFiltered = computed(() => {
  const query = quickRecentQuery.value
  if (!query) {
    return recentSections.value
  }
  return recentSections.value
    .map((section) => ({
      ...section,
      items: section.items.filter((doc) => matchesQuickQuery(doc, query))
    }))
    .filter((section) => section.items.length > 0)
})

const olderRecentSlugs = computed(() => {
  const older = recentSections.value.find((section) => section.key === 'older')
  if (!older) {
    return []
  }
  return older.items.map((item) => item.slug)
})

function resolveGroup(slug) {
  if (!slug) {
    return 'General'
  }
  if (slug.includes('/')) {
    return slug.split('/')[0]
  }
  if (slug.includes('-')) {
    return slug.split('-')[0]
  }
  return 'General'
}

function moveItem(list, fromIndex, toIndex) {
  if (fromIndex < 0 || toIndex < 0 || fromIndex >= list.length || toIndex >= list.length) {
    return list
  }
  if (fromIndex === toIndex) {
    return list
  }
  const next = list.slice()
  const [item] = next.splice(fromIndex, 1)
  next.splice(toIndex, 0, item)
  return next
}

function onQuickDragStart(type, slug) {
  quickDragType.value = type
  quickDragSlug.value = slug
  quickDropSlug.value = ''
}

function onQuickDragOver(type, slug) {
  if (quickDragType.value !== type || !quickDragSlug.value || quickDragSlug.value === slug) {
    return
  }
  quickDropSlug.value = slug
}

function onQuickDrop(type, targetSlug) {
  if (quickDragType.value !== type || !quickDragSlug.value || quickDragSlug.value === targetSlug) {
    onQuickDragEnd()
    return
  }
  const source = type === 'FAVORITES'
    ? favoriteDocs.value.map((item) => item.slug)
    : type === 'RECENT'
      ? recentDocs.value.map((item) => item.slug)
      : pinnedDocs.value.map((item) => item.slug)
  const from = source.indexOf(quickDragSlug.value)
  const to = source.indexOf(targetSlug)
  const ordered = moveItem(source, from, to)
  emit('reorder-quick', { type, slugs: ordered })
  onQuickDragEnd()
}

function onQuickDragEnd() {
  quickDragType.value = ''
  quickDragSlug.value = ''
  quickDropSlug.value = ''
}

function emitQuickCollectionAction(action, slugs = []) {
  emit('quick-collection-action', {
    action,
    slugs: Array.isArray(slugs) ? slugs : []
  })
}

function expandQuickZones() {
  quickOpenPinned.value = true
  quickOpenFavorites.value = true
  quickOpenRecent.value = true
}

function collapseQuickZones() {
  quickOpenPinned.value = false
  quickOpenFavorites.value = false
  quickOpenRecent.value = false
}

async function focusQuickSearch(type) {
  const mode = String(type || '').trim().toUpperCase()
  if (mode === 'PINNED') {
    quickOpenPinned.value = true
    await nextTick()
    pinnedSearchRef.value?.focus?.()
    pinnedSearchRef.value?.select?.()
    return
  }
  if (mode === 'FAVORITES') {
    quickOpenFavorites.value = true
    await nextTick()
    favoritesSearchRef.value?.focus?.()
    favoritesSearchRef.value?.select?.()
    return
  }
  quickOpenRecent.value = true
  await nextTick()
  recentSearchRef.value?.focus?.()
  recentSearchRef.value?.select?.()
}

function flattenTree(node, childrenByParent, depth) {
  const children = (childrenByParent.get(node.slug) || [])
    .slice()
    .sort(sortByOrder)
  const hasChildren = children.length > 0
  const expanded = hasChildren ? isNodeExpanded(node.slug, depth) : false
  const result = [{
    ...node,
    depth,
    hasChildren,
    expanded
  }]

  if (expanded) {
    children.forEach((child) => {
      result.push(...flattenTree(child, childrenByParent, depth + 1))
    })
  }

  return result
}

function isNodeExpanded(slug, depth) {
  const current = nodeOpened.value[slug]
  if (typeof current === 'boolean') {
    return current
  }
  const defaultOpen = depth < 2
  nodeOpened.value = {
    ...nodeOpened.value,
    [slug]: defaultOpen
  }
  return defaultOpen
}

function toggleNodeExpanded(slug) {
  nodeOpened.value = {
    ...nodeOpened.value,
    [slug]: !nodeOpened.value[slug]
  }
}

function clearSearch() {
  keyword.value = ''
  emit('search', '')
}

function toggleQuickMenu(slug) {
  quickMenuSlug.value = quickMenuSlug.value === slug ? '' : slug
}

function emitQuickAction(action, slug) {
  quickMenuSlug.value = ''
  emit('quick-action', { action, slug })
}

function toggleBatchMode() {
  batchMode.value = !batchMode.value
  quickMenuSlug.value = ''
  if (!batchMode.value) {
    selectedSlugs.value = []
    selectedOnlyMode.value = false
  }
}

function toggleSelected(slug) {
  if (selectedSlugs.value.includes(slug)) {
    selectedSlugs.value = selectedSlugs.value.filter((item) => item !== slug)
  } else {
    selectedSlugs.value = [...selectedSlugs.value, slug]
  }
}

function onNodeClick(slug) {
  if (batchMode.value) {
    toggleSelected(slug)
    return
  }
  emit('select', slug)
}

function onNodeKeydown(event, slug) {
  const key = event.key
  if (!['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Enter', ' '].includes(key)) {
    return
  }
  event.preventDefault()

  if (key === 'Enter') {
    onNodeClick(slug)
    return
  }
  if (key === ' ') {
    if (batchMode.value) {
      toggleSelected(slug)
    } else {
      onNodeClick(slug)
    }
    return
  }
  if (batchMode.value) {
    return
  }

  const index = visibleRowIndex.value.get(slug)
  if (index === undefined) {
    return
  }

  if (key === 'ArrowDown') {
    const next = visibleRows.value[index + 1]
    if (next) {
      emit('select', next.slug)
    }
    return
  }

  if (key === 'ArrowUp') {
    const prev = visibleRows.value[index - 1]
    if (prev) {
      emit('select', prev.slug)
    }
    return
  }

  const row = visibleRows.value[index]
  if (!row) {
    return
  }

  if (key === 'ArrowRight') {
    if (row.item.hasChildren && !row.item.expanded) {
      nodeOpened.value = {
        ...nodeOpened.value,
        [slug]: true
      }
      return
    }
    if (!isSectionOpen(row.sectionKey)) {
      sectionOpened.value[row.sectionKey] = true
      return
    }
    if (!opened.value[row.groupId]) {
      opened.value[row.groupId] = true
      return
    }
    const child = visibleRows.value.find((candidate) => candidate.item.parentSlug === slug)
    if (child) {
      emit('select', child.slug)
    }
    return
  }

  if (key === 'ArrowLeft') {
    if (row.item.hasChildren && row.item.expanded) {
      nodeOpened.value = {
        ...nodeOpened.value,
        [slug]: false
      }
      return
    }
    const parentSlug = row.item.parentSlug
    if (parentSlug && visibleRowBySlug.value.has(parentSlug)) {
      emit('select', parentSlug)
      return
    }
    if (opened.value[row.groupId]) {
      opened.value[row.groupId] = false
      return
    }
    if (isSectionOpen(row.sectionKey)) {
      sectionOpened.value[row.sectionKey] = false
    }
  }
}

function emitBulkAction(action) {
  emit('bulk-action', {
    action,
    slugs: [...selectedSlugs.value]
  })
}

function selectAllVisible() {
  const all = visibilitySections.value
    .flatMap((section) => section.groups)
    .flatMap((group) => group.items)
    .map((item) => item.slug)
  selectedSlugs.value = Array.from(new Set(all))
}

function clearSelected() {
  selectedSlugs.value = []
}

function invertSelection() {
  const all = visibilitySections.value
    .flatMap((section) => section.groups)
    .flatMap((group) => group.items)
    .map((item) => item.slug)
  const selected = new Set(selectedSlugs.value)
  selectedSlugs.value = all.filter((slug) => !selected.has(slug))
}

function selectByStatus(status) {
  const all = visibilitySections.value
    .flatMap((section) => section.groups)
    .flatMap((group) => group.items)
  selectedSlugs.value = all
    .filter((item) => (item.status || 'DRAFT') === status)
    .map((item) => item.slug)
}

function selectByPriority(priority) {
  const all = visibilitySections.value
    .flatMap((section) => section.groups)
    .flatMap((group) => group.items)
  selectedSlugs.value = all
    .filter((item) => (item.priority || 'MEDIUM') === priority)
    .map((item) => item.slug)
}

function selectByDue(mode) {
  const all = visibilitySections.value
    .flatMap((section) => section.groups)
    .flatMap((group) => group.items)
  const today = new Date().toISOString().slice(0, 10)
  selectedSlugs.value = all
    .filter((item) => {
      if (!item.dueDate) {
        return false
      }
      if (mode === 'TODAY') {
        return item.dueDate === today
      }
      if (mode === 'OVERDUE') {
        return item.dueDate < today
      }
      return false
    })
    .map((item) => item.slug)
}

function selectByVisibility(visibility) {
  const all = visibilitySections.value
    .flatMap((section) => section.groups)
    .flatMap((group) => group.items)
  selectedSlugs.value = all
    .filter((item) => (item.visibility || 'SPACE') === visibility)
    .map((item) => item.slug)
}

function selectByAssigneeMe() {
  const me = (props.currentUser || '').trim()
  if (!me) {
    selectedSlugs.value = []
    return
  }
  const all = visibilitySections.value
    .flatMap((section) => section.groups)
    .flatMap((group) => group.items)
  selectedSlugs.value = all
    .filter((item) => (item.assignee || '').trim() === me)
    .map((item) => item.slug)
}

function selectByEditableMe() {
  const me = (props.currentUser || '').trim()
  if (!me) {
    selectedSlugs.value = []
    return
  }
  const all = visibilitySections.value
    .flatMap((section) => section.groups)
    .flatMap((group) => group.items)
  selectedSlugs.value = all
    .filter((item) => {
      const owner = (item.owner || '').trim()
      const editors = Array.isArray(item.editors) ? item.editors.map((x) => (x || '').trim()) : []
      if (!owner && editors.length === 0) {
        return true
      }
      return owner === me || editors.includes(me)
    })
    .map((item) => item.slug)
}

function clearBatchSelection() {
  selectedSlugs.value = []
  selectedOnlyMode.value = false
}

function toggleCompactMode() {
  compactMode.value = !compactMode.value
}

function toggleMyTodoMode() {
  if (!myTodoMode.value && !assigneeFilter.value) {
    assigneeFilter.value = props.currentUser || ''
  }
  myTodoMode.value = !myTodoMode.value
}

function setMyTodoFilter(userName) {
  assigneeFilter.value = userName || ''
  dueFilter.value = 'ALL'
  priorityFilter.value = 'ALL'
  myTodoMode.value = true
}

function clearMyTodoFilter() {
  myTodoMode.value = false
}

function setPermissionFilter(mode) {
  const next = String(mode || 'ALL').trim().toUpperCase()
  const allow = new Set(['ALL', 'OWNED', 'EDITABLE', 'VIEWABLE', 'SHARED'])
  permissionFilter.value = allow.has(next) ? next : 'ALL'
  filtersOpen.value = true
  treeOpen.value = true
}

function toggleGroup(name) {
  opened.value[name] = !opened.value[name]
}

function isSectionOpen(sectionKey) {
  return sectionOpened.value[sectionKey] !== false
}

function toggleSection(sectionKey) {
  sectionOpened.value[sectionKey] = !isSectionOpen(sectionKey)
}

function toggleTreeFocusPath() {
  treeFocusPath.value = !treeFocusPath.value
  if (treeFocusPath.value && props.activeSlug) {
    applyPathFocusState(props.activeSlug)
  }
}

function depthClass(depth) {
  return `depth-${Math.min(depth, 4)}`
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
    return '高优先'
  }
  if (priority === 'LOW') {
    return '低优先'
  }
  return '中优先'
}

function normalizeUser(value) {
  return (value || '').trim()
}

function normalizeMembers(list) {
  if (!Array.isArray(list)) {
    return []
  }
  return list.map((item) => normalizeUser(item)).filter((item) => item.length > 0)
}

function isOwnedByMe(doc) {
  const me = normalizeUser(props.currentUser)
  if (!me) {
    return false
  }
  return normalizeUser(doc.owner) === me
}

function canEditMe(doc) {
  const me = normalizeUser(props.currentUser)
  if (!me) {
    return false
  }
  const owner = normalizeUser(doc.owner)
  const editors = normalizeMembers(doc.editors)
  if (!owner && editors.length === 0) {
    return true
  }
  return owner === me || editors.includes(me)
}

function canViewMe(doc) {
  if (canEditMe(doc)) {
    return true
  }
  if ((doc.visibility || 'SPACE') === 'SPACE') {
    return true
  }
  const me = normalizeUser(props.currentUser)
  if (!me) {
    return false
  }
  const viewers = normalizeMembers(doc.viewers)
  if (viewers.length === 0) {
    return false
  }
  return viewers.includes(me)
}

function isSharedWithMe(doc) {
  const me = normalizeUser(props.currentUser)
  if (!me) {
    return false
  }
  if (isOwnedByMe(doc)) {
    return false
  }
  const editors = normalizeMembers(doc.editors)
  const viewers = normalizeMembers(doc.viewers)
  return editors.includes(me) || viewers.includes(me)
}

function matchPermissionFilter(doc) {
  if (permissionFilter.value === 'ALL') {
    return true
  }
  if (permissionFilter.value === 'OWNED') {
    return isOwnedByMe(doc)
  }
  if (permissionFilter.value === 'EDITABLE') {
    return canEditMe(doc)
  }
  if (permissionFilter.value === 'VIEWABLE') {
    return canViewMe(doc)
  }
  if (permissionFilter.value === 'SHARED') {
    return isSharedWithMe(doc)
  }
  return true
}

function resolvePermissionLabel(doc) {
  if (isOwnedByMe(doc)) {
    return '我拥有'
  }
  if (canEditMe(doc)) {
    return '我可编辑'
  }
  if (isSharedWithMe(doc)) {
    return '共享给我'
  }
  if (canViewMe(doc)) {
    return '我可查看'
  }
  return '未知'
}

function resolvePermissionClass(doc) {
  if (isOwnedByMe(doc)) {
    return 'owned'
  }
  if (canEditMe(doc)) {
    return 'editable'
  }
  if (isSharedWithMe(doc)) {
    return 'shared'
  }
  if (canViewMe(doc)) {
    return 'viewable'
  }
  return 'unknown'
}

function sortByOrder(a, b) {
  const orderA = Number.isFinite(a.sortOrder) ? a.sortOrder : 0
  const orderB = Number.isFinite(b.sortOrder) ? b.sortOrder : 0
  if (orderA !== orderB) {
    return orderA - orderB
  }
  return (a.title || '').localeCompare(b.title || '', 'zh-Hans-CN')
}

function expandAll() {
  visibilitySections.value.forEach((section) => {
    section.groups.forEach((group) => {
      opened.value[group.id] = true
    })
  })
  const parents = new Set(
    props.docs
      .map((doc) => (doc.parentSlug || '').trim())
      .filter((slug) => slug.length > 0)
  )
  const next = {}
  parents.forEach((slug) => {
    next[slug] = true
  })
  nodeOpened.value = next
}

function collapseAll() {
  visibilitySections.value.forEach((section) => {
    section.groups.forEach((group) => {
      opened.value[group.id] = true
    })
  })
  const parents = new Set(
    props.docs
      .map((doc) => (doc.parentSlug || '').trim())
      .filter((slug) => slug.length > 0)
  )
  const next = {}
  parents.forEach((slug) => {
    next[slug] = false
  })
  nodeOpened.value = next
}

function expandSidebarPanels() {
  quickOpenPinned.value = true
  filtersOpen.value = true
  quickOpenFavorites.value = true
  quickOpenRecent.value = true
  treeOpen.value = true
}

function collapseSidebarPanels() {
  quickOpenPinned.value = false
  filtersOpen.value = false
  quickOpenFavorites.value = false
  quickOpenRecent.value = false
  treeOpen.value = false
}

function onDragStart(slug) {
  draggingSlug.value = slug
}

function onDragEnd() {
  draggingSlug.value = ''
  dropTargetSlug.value = ''
  dropTargetRoot.value = false
}

function onDragOver(targetSlug) {
  if (!draggingSlug.value || draggingSlug.value === targetSlug) {
    return
  }
  dropTargetRoot.value = false
  dropTargetSlug.value = targetSlug
}

function onDragLeave(targetSlug) {
  if (dropTargetSlug.value === targetSlug) {
    dropTargetSlug.value = ''
  }
}

function onDropNode(targetSlug) {
  if (!draggingSlug.value || draggingSlug.value === targetSlug) {
    onDragEnd()
    return
  }
  emit('move', {
    slug: draggingSlug.value,
    parentSlug: targetSlug
  })
  onDragEnd()
}

function onRootDragOver() {
  if (!draggingSlug.value) {
    return
  }
  dropTargetSlug.value = ''
  dropTargetRoot.value = true
}

function onRootDragLeave() {
  dropTargetRoot.value = false
}

function onDropRoot() {
  if (!draggingSlug.value) {
    onDragEnd()
    return
  }
  emit('move', {
    slug: draggingSlug.value,
    parentSlug: null
  })
  onDragEnd()
}

function onTreeScroll(event) {
  persistTreeScrollPosition(event?.target?.scrollTop || 0)
}

defineExpose({
  setMyTodoFilter,
  clearMyTodoFilter,
  setPermissionFilter,
  focusQuickSearch,
  toggleTreeFocusPath,
  clearBatchSelection,
  expandAll,
  collapseAll,
  expandSidebarPanels,
  collapseSidebarPanels,
  toggleCompactMode
})
</script>
