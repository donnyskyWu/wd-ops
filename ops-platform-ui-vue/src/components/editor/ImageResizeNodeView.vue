<template>
  <node-view-wrapper class="image-resize-node" :class="{ 'is-selected': selected }">
    <div ref="containerRef" class="image-resize-container" :style="containerStyle">
      <img
        :src="node.attrs.src"
        :alt="node.attrs.alt || ''"
        draggable="false"
        @click="selectImage"
      />
      <div
        v-if="selected && props.editor.isEditable"
        class="image-resize-handle"
        title="拖动调整宽度"
        @mousedown.prevent="startResize"
      />
    </div>
  </node-view-wrapper>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'

const props = defineProps(nodeViewProps)

const containerRef = ref<HTMLElement | null>(null)

const selected = computed(() => props.selected)

const containerStyle = computed(() => {
  const width = props.node.attrs.width as string | null
  if (!width) return {}
  return { width, maxWidth: '100%' }
})

function selectImage() {
  const pos = props.getPos()
  if (typeof pos !== 'number') return
  props.editor.chain().focus().setNodeSelection(pos).run()
}

function startResize(event: MouseEvent) {
  const container = containerRef.value
  if (!container || !props.editor.isEditable) return

  const startX = event.clientX
  const startWidth = container.offsetWidth
  const editorEl = container.closest('.ProseMirror') as HTMLElement | null
  const maxWidth = editorEl?.clientWidth || startWidth

  const onMouseMove = (moveEvent: MouseEvent) => {
    const delta = moveEvent.clientX - startX
    const nextWidth = Math.min(maxWidth, Math.max(80, startWidth + delta))
    const width = `${Math.round(nextWidth)}px`
    props.updateAttributes({
      width,
      style: `width:${width};max-width:100%;height:auto;`,
      dataW: width,
    })
  }

  const onMouseUp = () => {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}
</script>

<style scoped>
.image-resize-node {
  display: block;
  margin: 12px 0;
}

.image-resize-container {
  position: relative;
  display: inline-block;
  max-width: 100%;
  line-height: 0;
}

.image-resize-container img {
  display: block;
  width: 100%;
  height: auto;
  border-radius: 4px;
}

.image-resize-node.is-selected .image-resize-container {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
  border-radius: 4px;
}

.image-resize-handle {
  position: absolute;
  right: -4px;
  bottom: -4px;
  width: 12px;
  height: 12px;
  background: var(--el-color-primary);
  border: 2px solid #fff;
  border-radius: 2px;
  cursor: nwse-resize;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}
</style>
