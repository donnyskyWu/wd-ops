<template>
  <div class="content-wrap">
    <div v-if="title || $slots.header" class="content-wrap__header">
      <slot name="header">
        <span class="content-wrap__title">{{ title }}</span>
      </slot>
      <div v-if="$slots.extra" class="content-wrap__extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="content-wrap__body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  /** 标题 */
  title?: string
}

withDefaults(defineProps<Props>(), {
  title: '',
})
</script>

<style scoped lang="scss">
.content-wrap {
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  box-shadow: var(--el-box-shadow-light);
  padding: 20px;
  margin-bottom: 16px;
  transition: all 0.3s ease;
  color: var(--el-text-color-primary);

  &:hover {
    box-shadow: var(--el-box-shadow);
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    position: relative;
    padding-left: 12px;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 16px;
      background: linear-gradient(
        180deg,
        var(--el-color-primary),
        var(--el-color-primary-light-3)
      );
      border-radius: 2px;
    }
  }

  &__extra {
    flex-shrink: 0;
  }

  &__body {
    // 内容区域
  }
}
</style>
