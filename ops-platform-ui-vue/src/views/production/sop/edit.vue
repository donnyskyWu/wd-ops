<template>
  <div class="sop-edit-page">
    <!-- 面包屑 -->
    <el-breadcrumb separator="/">
      <el-breadcrumb-item>
        <el-link @click="handleBack">SOP管理</el-link>
      </el-breadcrumb-item>
      <el-breadcrumb-item>{{ templateName || '新建SOP模板' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleSave">
        <el-icon><Check /></el-icon>
        保存
      </el-button>
      <el-button @click="handleBack">
        返回
      </el-button>
      <el-divider direction="vertical" />
      <el-button @click="handleAutoLayout">
        <el-icon><Grid /></el-icon>
        自动布局
      </el-button>
      <el-button @click="handleFitView">
        <el-icon><FullScreen /></el-icon>
        适配视图
      </el-button>
      <el-button @click="handleValidateDag">
        <el-icon><CircleCheck /></el-icon>
        校验DAG
      </el-button>
      <el-divider direction="vertical" />
      <el-button @click="handleUndo" :disabled="!canUndo">
        <el-icon><RefreshLeft /></el-icon>
        撤销
      </el-button>
      <el-button @click="handleRedo" :disabled="!canRedo">
        <el-icon><RefreshRight /></el-icon>
        重做
      </el-button>
      <div class="toolbar-stats">
        <el-tag size="small" type="success">
          节点: {{ nodes.length }}
        </el-tag>
        <el-tag size="small" type="primary">
          连线: {{ edges.length }}
        </el-tag>
        <el-tag v-if="hasParallelGroups" size="small" type="warning">
          并行组: {{ parallelGroupCount }}
        </el-tag>
      </div>
    </div>

    <!-- 主内容区 -->
    <el-row :gutter="16" class="main-content">
      <!-- 左侧节点面板 -->
      <el-col :span="4">
        <el-card shadow="never" class="node-panel">
          <template #header>
            <div class="panel-header">
              <span>节点库</span>
              <el-tooltip content="拖拽节点到画布" placement="top">
                <el-icon class="help-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <div class="node-drag-list">
            <div
              v-for="template in nodeTemplates"
              :key="template.type"
              class="node-template-item"
              draggable="true"
              @dragstart="handleDragStart($event, template)"
            >
              <el-icon :size="20" :color="template.color">
                <component :is="template.icon" />
              </el-icon>
              <div class="template-info">
                <div class="template-name">{{ template.name }}</div>
                <div class="template-desc">{{ template.desc }}</div>
              </div>
            </div>
          </div>
          
          <el-divider />
          
          <div class="panel-header">
            <span>已添加节点</span>
            <el-button type="primary" size="small" @click="handleAddNode">
              <el-icon><Plus /></el-icon>
            </el-button>
          </div>
          <div class="node-list">
            <div
              v-for="node in nodes"
              :key="node.id"
              class="node-item"
              :class="{ 
                'active': selectedNodeId === node.id,
                'node-start': isStartNode(node),
                'node-review': node.needReview === 1
              }"
              @click="handleSelectNode(node.id)"
            >
              <div class="node-indicator"></div>
              <div class="node-content">
                <div class="node-header">
                  <el-icon :size="14" class="node-icon">
                    <Share v-if="isStartNode(node)" />
                    <CircleCheck v-else-if="node.needReview === 1" />
                    <Document v-else />
                  </el-icon>
                  <span class="node-name">{{ node.nodeName }}</span>
                </div>
                <div class="node-meta">
                  <el-tag size="small" effect="plain">{{ node.executorRole }}</el-tag>
                  <span v-if="node.slaHours" class="sla-tag">{{ node.slaHours }}h</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 中间DAG画布 -->
      <el-col :span="16">
        <el-card shadow="never" class="dag-canvas-card">
          <div 
            ref="containerRef" 
            class="dag-canvas"
            @drop="handleDrop"
            @dragover="handleDragOver"
          ></div>
          
          <!-- 画布工具栏 -->
          <div class="canvas-toolbar">
            <el-tooltip content="放大" placement="top">
              <el-button size="small" circle @click="handleZoomIn">
                <el-icon><ZoomIn /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="缩小" placement="top">
              <el-button size="small" circle @click="handleZoomOut">
                <el-icon><ZoomOut /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="实际大小" placement="top">
              <el-button size="small" circle @click="handleZoomReset">
                <el-icon><ScaleToOriginal /></el-icon>
              </el-button>
            </el-tooltip>
            <el-divider direction="vertical" />
            <el-tooltip content="居中显示" placement="top">
              <el-button size="small" circle @click="handleFitView">
                <el-icon><FullScreen /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
          
          <!-- 快捷键提示 -->
          <div class="keyboard-hints">
            <el-tag size="small" effect="plain">Delete: 删除</el-tag>
            <el-tag size="small" effect="plain">Ctrl+Z: 撤销</el-tag>
            <el-tag size="small" effect="plain">Ctrl+Y: 重做</el-tag>
            <el-tag size="small" effect="plain">Ctrl+S: 保存</el-tag>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧节点配置面板 -->
      <el-col :span="6">
        <el-card v-if="selectedNode" shadow="never" class="config-panel">
          <template #header>
            <div class="panel-header">
              <span>节点配置</span>
              <el-popconfirm title="确定删除该节点吗？" @confirm="handleDeleteNode">
                <template #reference>
                  <el-button type="danger" size="small">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
          <el-form :model="selectedNode" label-width="100px">
            <el-form-item label="节点名称">
              <el-input v-model="selectedNode.nodeName" maxlength="50" @blur="updateNodeLabel" />
            </el-form-item>
            <el-form-item label="执行岗位">
              <el-select v-model="selectedNode.executorRole" placeholder="请选择" @change="updateNodeRole">
                <el-option label="运营组长" value="运营组长" />
                <el-option label="公众号运营" value="公众号运营" />
                <el-option label="剪辑" value="剪辑" />
                <el-option label="直播运营" value="直播运营" />
                <el-option label="销售" value="销售" />
              </el-select>
            </el-form-item>
            <el-form-item label="是否需要审核">
              <el-switch v-model="selectedNode.needReview" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item v-if="selectedNode.needReview === 1" label="审核人岗位">
              <el-select v-model="selectedNode.reviewerRole" placeholder="请选择">
                <el-option label="运营组长" value="运营组长" />
                <el-option label="内容总监" value="内容总监" />
              </el-select>
            </el-form-item>
            <el-form-item label="前置节点">
              <el-select
                v-model="selectedNode.predecessors"
                multiple
                placeholder="请选择"
                filterable
              >
                <el-option
                  v-for="node in nodes.filter(n => n.id !== selectedNode.id)"
                  :key="node.id"
                  :label="node.nodeName"
                  :value="node.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="并行组">
              <el-input v-model="selectedNode.parallelGroup" placeholder="选填，相同组可并行" maxlength="50" />
            </el-form-item>
            <el-form-item label="SLA时限(小时)">
              <el-input-number v-model="selectedNode.slaHours" :min="1" :precision="0" />
            </el-form-item>
          </el-form>
        </el-card>
        <el-card v-else shadow="never" class="config-panel">
          <el-empty description="请选择一个节点" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Plus, Check, Delete, Refresh, CircleCheck, Share, Document, CircleCheckFilled, VideoPlay,
  Grid, FullScreen, ZoomIn, ZoomOut, ScaleToOriginal, RefreshLeft, RefreshRight,
  QuestionFilled
} from '@element-plus/icons-vue'
import LogicFlow, { CircleNode, CircleNodeModel, RectNode, RectNodeModel } from '@logicflow/core'
import '@logicflow/core/dist/index.css'
import { getSopNodeList, validateDag } from '@/api/sop'
import type { SopNodeVO } from '@/types/sop'

const route = useRoute()
const router = useRouter()
const templateId = computed(() => Number(route.params.id))

// ==================== 响应式数据 ====================

const templateName = ref('')
const loading = ref(false)
const nodes = ref<SopNodeVO[]>([])
const edges = ref<any[]>([])
const selectedNodeId = ref<number | null>(null)
const containerRef = ref<HTMLElement>()
let lf: LogicFlow | null = null

// 撤销/重做栈
const historyStack = ref<any[]>([])
const historyIndex = ref(-1)
const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < historyStack.value.length - 1)

// 并行组统计
const hasParallelGroups = computed(() => {
  return nodes.value.some(n => n.parallelGroup)
})
const parallelGroupCount = computed(() => {
  const groups = new Set(nodes.value.filter(n => n.parallelGroup).map(n => n.parallelGroup))
  return groups.size
})

// 节点模板
const nodeTemplates = [
  { 
    type: 'start', 
    name: '开始节点', 
    desc: '流程起点',
    icon: 'VideoPlay',
    color: '#10b981' 
  },
  { 
    type: 'process', 
    name: '流程节点', 
    desc: '常规步骤',
    icon: 'Document',
    color: '#3b82f6' 
  },
  { 
    type: 'audit', 
    name: '审核节点', 
    desc: '需要审批',
    icon: 'CircleCheck',
    color: '#f59e0b' 
  },
]

// 当前选中的节点
const selectedNode = computed(() => {
  if (!selectedNodeId.value) return null
  return nodes.value.find(n => n.id === selectedNodeId.value) || null
})

// ==================== LogicFlow 初始化 ====================

const initLogicFlow = () => {
  if (!containerRef.value) return

  lf = new LogicFlow({
    container: containerRef.value,
    grid: {
      size: 10,
      visible: true,
      type: 'dot',
    },
    background: {
      color: '#f8fafc',
    },
    keyboard: {
      enabled: true,
    },
    style: {
      rect: {
        rx: 8,
        ry: 8,
        stroke: '#e2e8f0',
        fill: '#ffffff',
      },
      circle: {
        stroke: '#e2e8f0',
        fill: '#ffffff',
      },
      polyline: {
        stroke: '#94a3b8',
        strokeWidth: 2,
      },
      text: {
        color: '#1e293b',
        fontSize: 14,
      },
      nodeSelected: {
        stroke: '#3b82f6',
        strokeWidth: 2,
      },
    },
  })

  // 注册自定义节点
  registerCustomNodes()

  // 监听节点点击
  lf.on('node:click', ({ data }) => {
    selectedNodeId.value = Number(data.id)
  })

  // 监听画布点击（取消选中）
  lf.on('blank:click', () => {
    selectedNodeId.value = null
  })

  // 监听连线创建
  lf.on('edge:add', (data: any) => {
    handleEdgeCreated(data)
  })

  // 监听连线删除
  lf.on('edge:delete', (data: any) => {
    handleEdgeDeleted(data)
  })

  // 渲染
  renderGraph()
}

// 注册自定义节点
const registerCustomNodes = () => {
  if (!lf) return

  // 注册开始节点（圆形）
  class StartNodeModel extends CircleNodeModel {
    initNodeData(data: any) {
      super.initNodeData(data)
      this.r = 30
      this.fill = '#10b981'
      this.stroke = '#059669'
      this.strokeWidth = 2
      // 配置文本样式
      this.text = {
        ...this.text,
        fill: '#ffffff',
        fontSize: 12,
        fontWeight: 'bold',
      }
    }
  }

  class StartNodeView extends CircleNode {}

  lf.register({
    type: 'start-node',
    view: StartNodeView,
    model: StartNodeModel,
  })

  // 注册流程节点（矩形）
  class ProcessNodeModel extends RectNodeModel {
    initNodeData(data: any) {
      super.initNodeData(data)
      this.width = 180
      this.height = 80
      this.fill = '#eff6ff'
      this.stroke = '#3b82f6'
      this.strokeWidth = 2
      this.rx = 8
      this.ry = 8
      // 配置文本样式
      this.text = {
        ...this.text,
        fill: '#1e293b',
        fontSize: 13,
        fontWeight: '600',
      }
    }
  }

  class ProcessNodeView extends RectNode {}

  lf.register({
    type: 'process-node',
    view: ProcessNodeView,
    model: ProcessNodeModel,
  })

  // 注册审核节点（矩形，黄色）
  class AuditNodeModel extends RectNodeModel {
    initNodeData(data: any) {
      super.initNodeData(data)
      this.width = 180
      this.height = 80
      this.fill = '#fef3c7'
      this.stroke = '#f59e0b'
      this.strokeWidth = 2
      this.rx = 8
      this.ry = 8
      // 配置文本样式
      this.text = {
        ...this.text,
        fill: '#92400e',
        fontSize: 13,
        fontWeight: '600',
      }
    }
  }

  class AuditNodeView extends RectNode {}

  lf.register({
    type: 'audit-node',
    view: AuditNodeView,
    model: AuditNodeModel,
  })
}

// 判断是否为开始节点
const isStartNode = (node: SopNodeVO): boolean => {
  return !node.predecessors || node.predecessors.length === 0
}

// 渲染图
const renderGraph = () => {
  if (!lf || nodes.value.length === 0) {
    console.warn('LogicFlow未初始化或没有节点数据', { lf: !!lf, nodesCount: nodes.value.length })
    return
  }

  console.log('🎨 开始渲染节点:', nodes.value.map(n => ({ id: n.id, name: n.nodeName, type: n.needReview === 1 ? 'audit' : 'process' })))

  const graphData = {
    nodes: nodes.value.map(node => ({
      id: String(node.id),
      type: node.predecessors?.length === 0 ? 'start-node' : node.needReview === 1 ? 'audit-node' : 'process-node',
      x: calculateNodeX(node),
      y: calculateNodeY(node),
      text: node.nodeName,  // LogicFlow 2.x 直接使用字符串
      properties: {
        ...node,
      },
    })),
    edges: edges.value.map(edge => ({
      id: `e${edge.source}-${edge.target}`,
      type: 'polyline',
      sourceNodeId: String(edge.source),
      targetNodeId: String(edge.target),
    })),
  }

  console.log('渲染图数据:', graphData)
  lf.render(graphData)
}

// 保存快照到历史栈
const saveHistory = () => {
  const snapshot = {
    nodes: JSON.parse(JSON.stringify(nodes.value)),
    edges: JSON.parse(JSON.stringify(edges.value)),
  }
  
  // 删除当前索引之后的所有历史
  historyStack.value = historyStack.value.slice(0, historyIndex.value + 1)
  
  // 添加新快照
  historyStack.value.push(snapshot)
  historyIndex.value++
  
  // 限制历史栈大小
  if (historyStack.value.length > 50) {
    historyStack.value.shift()
    historyIndex.value--
  }
}

// 撤销
const handleUndo = () => {
  if (!canUndo.value) return
  
  historyIndex.value--
  const snapshot = historyStack.value[historyIndex.value]
  
  nodes.value = JSON.parse(JSON.stringify(snapshot.nodes))
  edges.value = JSON.parse(JSON.stringify(snapshot.edges))
  
  renderGraph()
  ElMessage.success('已撤销')
}

// 重做
const handleRedo = () => {
  if (!canRedo.value) return
  
  historyIndex.value++
  const snapshot = historyStack.value[historyIndex.value]
  
  nodes.value = JSON.parse(JSON.stringify(snapshot.nodes))
  edges.value = JSON.parse(JSON.stringify(snapshot.edges))
  
  renderGraph()
  ElMessage.success('已重做')
}

// 自动布局
const handleAutoLayout = () => {
  if (!lf || nodes.value.length === 0) {
    ElMessage.warning('没有可布局的节点')
    return
  }
  
  // 重新计算所有节点位置
  renderGraph()
  
  // 适配视图
  setTimeout(() => {
    handleFitView()
  }, 100)
  
  ElMessage.success('自动布局完成')
}

// 画布缩放
const handleZoomIn = () => {
  if (!lf) return
  lf.zoom(true, 1.2)
}

const handleZoomOut = () => {
  if (!lf) return
  lf.zoom(true, 0.8)
}

const handleZoomReset = () => {
  if (!lf) return
  lf.zoom(false, 1)
}

// 拖拽创建节点
const handleDragStart = (event: DragEvent, template: any) => {
  event.dataTransfer?.setData('nodeType', template.type)
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
}

const handleDrop = (event: DragEvent) => {
  event.preventDefault()
  
  const nodeType = event.dataTransfer?.getData('nodeType')
  if (!nodeType) return
  
  // 获取画布坐标
  const rect = containerRef.value?.getBoundingClientRect()
  if (!rect) return
  
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  
  // 创建新节点
  const newNode: SopNodeVO = {
    id: Date.now(),
    templateId: templateId.value,
    nodeName: `${getNodeTypeName(nodeType)}${nodes.value.length + 1}`,
    nodeOrder: nodes.value.length + 1,
    nodeDescription: '',
    executorRole: '运营组长',
    needReview: nodeType === 'audit' ? 1 : 0,
    predecessors: [],
  }
  
  nodes.value.push(newNode)
  
  // 保存历史
  saveHistory()
  
  // 重新渲染
  renderGraph()
  
  // 自动选中
  selectedNodeId.value = newNode.id
  
  ElMessage.success('节点已添加')
}

const getNodeTypeName = (type: string): string => {
  const names: Record<string, string> = {
    start: '开始',
    process: '流程',
    audit: '审核',
  }
  return names[type] || '节点'
}

// 快捷键监听
const handleKeyDown = (event: KeyboardEvent) => {
  // Ctrl+S 保存
  if (event.ctrlKey && event.key === 's') {
    event.preventDefault()
    handleSave()
  }
  
  // Ctrl+Z 撤销
  if (event.ctrlKey && event.key === 'z') {
    event.preventDefault()
    handleUndo()
  }
  
  // Ctrl+Y 重做
  if (event.ctrlKey && event.key === 'y') {
    event.preventDefault()
    handleRedo()
  }
  
  // Delete 删除选中节点
  if (event.key === 'Delete' && selectedNodeId.value) {
    handleDeleteNode()
  }
}

// 计算节点X坐标
const calculateNodeX = (node: SopNodeVO): number => {
  const levels = calculateNodeLevels(nodes.value)
  const level = levels[node.id] || 0
  return 100 + level * 250
}

// 计算节点Y坐标
const calculateNodeY = (node: SopNodeVO): number => {
  const levels = calculateNodeLevels(nodes.value)
  const level = levels[node.id] || 0
  const siblings = nodes.value.filter(n => levels[n.id] === level)
  const index = siblings.indexOf(node)
  return 100 + index * 120
}

// 计算节点层级
const calculateNodeLevels = (nodeList: SopNodeVO[]): Record<number, number> => {
  const levels: Record<number, number> = {}
  const visited = new Set<number>()
  
  const getLevel = (nodeId: number): number => {
    if (levels[nodeId] !== undefined) return levels[nodeId]
    if (visited.has(nodeId)) return 0
    
    visited.add(nodeId)
    const node = nodeList.find(n => n.id === nodeId)
    if (!node || !node.predecessors || node.predecessors.length === 0) {
      levels[nodeId] = 0
      return 0
    }
    
    const maxPredLevel = Math.max(...node.predecessors.map(getLevel))
    levels[nodeId] = maxPredLevel + 1
    return levels[nodeId]
  }
  
  nodeList.forEach(node => getLevel(node.id))
  return levels
}

// ==================== 方法 ====================

// 加载节点列表
const loadNodes = async () => {
  loading.value = true
  try {
    const data = await getSopNodeList(templateId.value)
    
    if (!data || data.length === 0) {
      ElMessage.warning('该模板暂无节点数据，请添加节点')
      nodes.value = []
      edges.value = []
      return
    }

    nodes.value = data
    
    // 构建边数据
    edges.value = []
    data.forEach(node => {
      if (node.predecessors && node.predecessors.length > 0) {
        node.predecessors.forEach(predId => {
          edges.value.push({
            source: predId,
            target: node.id,
          })
        })
      }
    })
    
    // 等待DOM更新后初始化
    nextTick(() => {
      initLogicFlow()
      ElMessage.success(`已加载 ${data.length} 个节点`)
    })
  } catch (error) {
    console.error('加载节点失败:', error)
    ElMessage.error('加载节点失败，请重试')
  } finally {
    loading.value = false
  }
}

// 连线创建处理
const handleEdgeCreated = (data: any) => {
  const edgeData = data.data || data
  const sourceId = Number(edgeData.sourceNodeId)
  const targetId = Number(edgeData.targetNodeId)

  console.log('创建连线:', { sourceId, targetId, edgeData })

  // 验证：不能连接自己
  if (sourceId === targetId) {
    ElMessage.error('不能连接节点到自身')
    lf?.deleteEdge(edgeData.id)
    return
  }

  // 验证：避免循环依赖
  if (hasCycle(sourceId, targetId)) {
    ElMessage.error('连接会导致循环依赖，不允许')
    lf?.deleteEdge(edgeData.id)
    return
  }

  // 更新目标节点的前置节点
  const targetNode = nodes.value.find(n => n.id === targetId)
  if (targetNode && !targetNode.predecessors.includes(sourceId)) {
    targetNode.predecessors.push(sourceId)
  }

  edges.value.push({
    source: sourceId,
    target: targetId,
  })
  
  // 保存历史
  saveHistory()

  ElMessage.success('节点连接成功')
}

// 连线删除处理
const handleEdgeDeleted = (data: any) => {
  const edgeData = data.data || data
  const sourceId = Number(edgeData.sourceNodeId)
  const targetId = Number(edgeData.targetNodeId)

  console.log('删除连线:', { sourceId, targetId })

  // 从目标节点的前置节点中移除
  const targetNode = nodes.value.find(n => n.id === targetId)
  if (targetNode) {
    targetNode.predecessors = targetNode.predecessors.filter(id => id !== sourceId)
  }

  // 从边列表中移除
  edges.value = edges.value.filter(e => !(e.source === sourceId && e.target === targetId))
  
  // 保存历史
  saveHistory()
}

// 检查是否会产生循环
const hasCycle = (sourceId: number, targetId: number): boolean => {
  const visited = new Set<number>()
  const queue = [targetId]
  
  while (queue.length > 0) {
    const current = queue.shift()!
    if (current === sourceId) return true
    
    if (visited.has(current)) continue
    visited.add(current)
    
    const node = nodes.value.find(n => n.id === current)
    if (node && node.predecessors) {
      queue.push(...node.predecessors)
    }
  }
  
  return false
}

// 添加节点
const handleAddNode = () => {
  const newNode: SopNodeVO = {
    id: Date.now(),
    templateId: templateId.value,
    nodeName: `新节点${nodes.value.length + 1}`,
    nodeOrder: nodes.value.length + 1,
    nodeDescription: '',
    executorRole: '运营组长',
    needReview: 0,
    predecessors: [],
  }

  nodes.value.push(newNode)
  
  // 保存历史
  saveHistory()
  
  // 重新渲染
  renderGraph()

  // 自动选中新节点
  selectedNodeId.value = newNode.id
  ElMessage.success('节点已添加，请在右侧配置')
}

// 选中节点
const handleSelectNode = (nodeId: number) => {
  selectedNodeId.value = nodeId
}

// 更新节点标签
const updateNodeLabel = () => {
  if (!lf || !selectedNode.value) return
  
  lf.updateText(selectedNode.value.id, selectedNode.value.nodeName)
}

// 更新节点岗位
const updateNodeRole = () => {
  // LogicFlow会自动更新
}

// 删除节点
const handleDeleteNode = () => {
  if (!selectedNodeId.value || !lf) return

  const nodeId = selectedNodeId.value
  
  // 从 nodes 中删除
  nodes.value = nodes.value.filter(n => n.id !== nodeId)
  
  // 从 edges 中删除相关连线
  edges.value = edges.value.filter(e => e.source !== nodeId && e.target !== nodeId)

  // 清除选中
  selectedNodeId.value = null
  
  // 保存历史
  saveHistory()
  
  // 重新渲染
  renderGraph()
  
  ElMessage.success('节点已删除')
}

// 适配视图
const handleFitView = () => {
  if (!lf) return
  lf.translateCenter()
  lf.zoom(true)
}

// 校验DAG
const handleValidateDag = async () => {
  try {
    const dagData = {
      templateId: templateId.value,
      nodes: nodes.value.map(node => ({
        id: node.id,
        predecessors: node.predecessors,
      })),
    }

    const validationResult = await validateDag(dagData)

    if (validationResult.valid) {
      ElMessage.success('✅ DAG校验通过：无循环依赖，流程有效')
    } else {
      ElMessage.error(`❌ DAG校验失败：${validationResult.message}`)
    }
  } catch (error) {
    console.error('校验失败:', error)
    ElMessage.error('校验失败，请重试')
  }
}

// 保存
const handleSave = async () => {
  try {
    // 校验节点名称
    const emptyNameNodes = nodes.value.filter(n => !n.nodeName || n.nodeName.trim() === '')
    if (emptyNameNodes.length > 0) {
      ElMessage.error('请为所有节点填写名称')
      return
    }

    // 校验DAG
    const dagData = {
      templateId: templateId.value,
      nodes: nodes.value.map(node => ({
        id: node.id,
        predecessors: node.predecessors,
      })),
    }

    const validationResult = await validateDag(dagData)

    if (!validationResult.valid) {
      ElMessage.error(`DAG校验失败：${validationResult.message}`)
      return
    }

    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败，请重试')
  }
}

// 返回
const handleBack = () => {
  router.push('/sop')
}

// ==================== 生命周期 ====================

onMounted(() => {
  if (!templateId.value || Number.isNaN(templateId.value)) {
    ElMessage.error('无效的模板 ID')
    router.push('/sop')
    return
  }
  loadNodes()

  // 监听快捷键
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  if (lf) {
    lf.destroy()
  }
  
  // 移除快捷键监听
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped lang="scss">
.sop-edit-page {
  .toolbar {
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    
    .toolbar-stats {
      margin-left: auto;
      display: flex;
      gap: 8px;
    }
  }

  .main-content {
    height: calc(100vh - 200px);

    .node-panel,
    .dag-canvas-card,
    .config-panel {
      height: 100%;
      
      :deep(.el-card__body) {
        height: calc(100% - 60px);
        overflow: auto;
        padding: 12px;
      }
    }

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: 600;
      font-size: 14px;
      margin-bottom: 12px;
      
      .help-icon {
        cursor: help;
        color: #94a3b8;
        
        &:hover {
          color: #3b82f6;
        }
      }
    }
    
    // 节点模板列表
    .node-drag-list {
      margin-bottom: 16px;
      
      .node-template-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px;
        margin-bottom: 8px;
        border: 1.5px dashed #e2e8f0;
        border-radius: 8px;
        cursor: grab;
        transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
        background: #ffffff;
        
        &:hover {
          border-color: #3b82f6;
          background-color: #f8fafc;
          box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
          transform: translateX(2px);
        }
        
        &:active {
          cursor: grabbing;
        }
        
        .template-info {
          flex: 1;
          
          .template-name {
            font-size: 13px;
            font-weight: 600;
            color: #1e293b;
            margin-bottom: 2px;
          }
          
          .template-desc {
            font-size: 11px;
            color: #64748b;
          }
        }
      }
    }

    // 已添加节点列表
    .node-list {
      .node-item {
        display: flex;
        align-items: flex-start;
        gap: 10px;
        padding: 12px;
        margin-bottom: 8px;
        border: 1.5px solid #e2e8f0;
        border-radius: 10px;
        cursor: pointer;
        transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
        background: #ffffff;
        position: relative;
        overflow: hidden;

        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 0;
          bottom: 0;
          width: 3px;
          background: #3b82f6;
          opacity: 0;
          transition: opacity 0.25s;
        }

        &:hover {
          border-color: #3b82f6;
          background-color: #f8fafc;
          transform: translateX(4px);
          box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
          
          &::before {
            opacity: 1;
          }
        }

        &.active {
          border-color: #3b82f6;
          background-color: #eff6ff;
          box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
          
          &::before {
            opacity: 1;
            background: #2563eb;
          }
        }
        
        &.node-start {
          border-color: #10b981;
          
          &:hover,
          &.active {
            border-color: #10b981;
            background-color: #ecfdf5;
            
            &::before {
              background: #059669;
            }
          }
          
          .node-indicator {
            background: #10b981;
          }
        }
        
        &.node-review {
          border-color: #f59e0b;
          
          &:hover,
          &.active {
            border-color: #f59e0b;
            background-color: #fffbeb;
            
            &::before {
              background: #d97706;
            }
          }
          
          .node-indicator {
            background: #f59e0b;
          }
        }

        .node-indicator {
          width: 4px;
          min-height: 40px;
          background: #3b82f6;
          border-radius: 2px;
          flex-shrink: 0;
        }

        .node-content {
          flex: 1;
          min-width: 0;
          
          .node-header {
            display: flex;
            align-items: center;
            gap: 6px;
            margin-bottom: 6px;
            
            .node-icon {
              flex-shrink: 0;
            }
            
            .node-name {
              font-weight: 600;
              color: #1e293b;
              font-size: 13px;
              line-height: 1.4;
              flex: 1;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }
          }
          
          .node-meta {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 11px;
            
            .sla-tag {
              color: #ef4444;
              font-weight: 500;
            }
          }
        }
      }
    }

    .dag-canvas-card {
      position: relative;
      
      :deep(.el-card__body) {
        padding: 0;
        height: 100%;
      }
      
      .dag-canvas {
        width: 100%;
        height: 100%;
        background-color: #f8fafc;
      }
      
      // 画布工具栏
      .canvas-toolbar {
        position: absolute;
        bottom: 16px;
        right: 16px;
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 8px;
        background: rgba(255, 255, 255, 0.95);
        border-radius: 12px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        backdrop-filter: blur(8px);
      }
      
      // 快捷键提示
      .keyboard-hints {
        position: absolute;
        top: 12px;
        right: 12px;
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        opacity: 0.7;
        transition: opacity 0.25s;
        
        &:hover {
          opacity: 1;
        }
      }
    }
    
    .config-panel {
      :deep(.el-form-item) {
        margin-bottom: 18px;
      }
    }
  }
}
</style>
