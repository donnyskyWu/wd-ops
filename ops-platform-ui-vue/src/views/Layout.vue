<template>
  <el-container style="height: 100vh">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <el-icon v-if="!isCollapse" :size="24"><DataAnalysis /></el-icon>
        <span v-if="!isCollapse" class="logo-title">运营数据平台</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        router
        background-color="#001529"
        text-color="rgba(255, 255, 255, 0.65)"
        active-text-color="#ffffff"
      >
        <!-- 1. 首页仪表盘 -->
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <template #title>首页仪表盘</template>
        </el-menu-item>

        <!-- 2. 运营管理（M1） -->
        <el-sub-menu index="operations">
          <template #title>
            <el-icon><User /></el-icon>
            <span>运营管理</span>
          </template>
          <el-menu-item index="/ip-group">IP组管理</el-menu-item>
          <el-menu-item index="/author">作者管理</el-menu-item>
          <el-menu-item index="/account-analysis">账号分析</el-menu-item>
          <el-menu-item index="/fans-analysis">粉丝分析</el-menu-item>
          <el-menu-item index="/works-analysis">作品分析</el-menu-item>
          <el-menu-item index="/internal-content">内部内容分析</el-menu-item>
          <el-menu-item index="/efficiency">人效盘点</el-menu-item>
        </el-sub-menu>

        <!-- 3. 内容生产（M2） -->
        <el-sub-menu index="production">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>内容生产</span>
          </template>
          <el-menu-item index="/sop">SOP管理</el-menu-item>
          <el-menu-item index="/sop/review">SOP审核</el-menu-item>
          <el-menu-item index="/plan">计划管理</el-menu-item>
          <el-menu-item index="/task">任务管理</el-menu-item>
          <el-menu-item index="/content">内容管理</el-menu-item>
          <el-menu-item index="/content/edit">内容创作</el-menu-item>
          <el-menu-item index="/content/review">内容审核</el-menu-item>
          <el-menu-item index="/knowledge">内容知识库</el-menu-item>
        </el-sub-menu>

        <!-- 4. 绩效核算 -->
        <el-sub-menu index="performance">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>绩效核算</span>
          </template>
          <el-menu-item index="/perf-template">考核模板</el-menu-item>
          <el-menu-item index="/perf-execution">考核执行</el-menu-item>
          <el-menu-item index="/perf-result">绩效结果</el-menu-item>
          <el-menu-item index="/order-attribution">订单归因分析</el-menu-item>
        </el-sub-menu>

        <!-- 5. 内部管理（M4 账号管理） -->
        <el-sub-menu index="internal">
          <template #title>
            <el-icon><OfficeBuilding /></el-icon>
            <span>账号管理</span>
          </template>
          <el-menu-item index="/company">公司管理</el-menu-item>
          <el-menu-item index="/realname">实名人管理</el-menu-item>
          <el-menu-item index="/phone">手机管理</el-menu-item>
          <el-menu-item index="/simcard">手机卡管理</el-menu-item>
          <el-menu-item index="/internal-account">平台账号管理</el-menu-item>
          <el-menu-item index="/personal-account">个人账号管理</el-menu-item>
          <el-menu-item index="/triple-rel">三方关联统计</el-menu-item>
        </el-sub-menu>

        <!-- 6. 财务管理（M5） -->
        <el-sub-menu index="finance">
          <template #title>
            <el-icon><Coin /></el-icon>
            <span>财务管理</span>
          </template>
          <el-menu-item index="/account-cost">账号成本管理</el-menu-item>
          <el-menu-item index="/roi-analysis">ROI分析</el-menu-item>
        </el-sub-menu>

        <!-- 6.5 数据采集（M10） -->
        <el-sub-menu index="collect">
          <template #title>
            <el-icon><DataLine /></el-icon>
            <span>数据采集</span>
          </template>
          <el-menu-item index="/collect/task">采集任务</el-menu-item>
          <el-menu-item index="/collect/log">采集日志</el-menu-item>
          <el-menu-item index="/collect/quality">数据质量</el-menu-item>
        </el-sub-menu>

        <!-- 7. 数据分析（M6） -->
        <el-sub-menu index="analysis">
          <template #title>
            <el-icon><TrendCharts /></el-icon>
            <span>数据分析</span>
          </template>
          <el-menu-item index="/metric">指标管理</el-menu-item>
          <el-menu-item index="/metric-analysis">指标分析</el-menu-item>
          <el-menu-item index="/data-report">数据报表</el-menu-item>
          <el-menu-item index="/financial-analysis">总体财务分析</el-menu-item>
          <el-menu-item index="/funnel-analysis">漏斗分析</el-menu-item>
          <el-menu-item index="/custom-query">自定义查询</el-menu-item>
          <el-menu-item index="/data-screen">数据大屏</el-menu-item>
          <el-menu-item index="/screen-config">大屏配置</el-menu-item>
        </el-sub-menu>

        <!-- 8. 作品监测（M7） -->
        <el-sub-menu index="monitor">
          <template #title>
            <el-icon><VideoCamera /></el-icon>
            <span>作品监测</span>
          </template>
          <el-menu-item index="/external-account">外部账号分析</el-menu-item>
          <el-menu-item index="/low-score">低分作品分析</el-menu-item>
          <el-menu-item index="/hot-works">爆款作品分析</el-menu-item>
          <el-menu-item index="/high-fans-account">高粉账号分析</el-menu-item>
          <el-menu-item index="/low-fans-account">低粉账号分析</el-menu-item>
          <el-menu-item index="/ip-theme">IP主题数据</el-menu-item>
        </el-sub-menu>

        <!-- 9. 配置管理 -->
        <el-sub-menu index="config">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>配置管理</span>
          </template>
          <el-menu-item index="/config-internal-collect">内部采集配置</el-menu-item>
          <el-menu-item index="/config-external-collect">外部采集配置</el-menu-item>
          <el-menu-item index="/config-external-data">外部数据配置</el-menu-item>
          <el-menu-item index="/config-order-collect">订单采集配置</el-menu-item>
          <el-menu-item index="/config-threshold">阈值规则配置</el-menu-item>
          <el-menu-item index="/config-ai-model">AI模型</el-menu-item>
          <el-menu-item index="/config-ai-prompt">AI提示词</el-menu-item>
        </el-sub-menu>

        <!-- 10. 系统管理（M9） -->
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system-user">用户管理</el-menu-item>
          <el-menu-item index="/system-role">角色权限</el-menu-item>
          <el-menu-item index="/system-tenant">租户管理</el-menu-item>
          <el-menu-item index="/system-param">系统参数</el-menu-item>
          <el-menu-item index="/system-dict">字典配置</el-menu-item>
          <el-menu-item index="/system-log/operation">操作日志</el-menu-item>
          <el-menu-item index="/system-log/login">登录日志</el-menu-item>
          <el-menu-item index="/system-message">消息管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部导航 -->
      <el-header>
        <div class="header-left">
          <el-icon @click="toggleCollapse" style="cursor: pointer; font-size: 20px">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentRoute.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-badge :value="5" class="notification-badge">
            <el-icon :size="20"><Bell /></el-icon>
          </el-badge>
          <el-dropdown>
            <div class="user-info">
              <el-avatar :size="32" src="https://cube.elemecdn.com/0/887eb20769b446af6a2126ecf2e.jpg" />
              <span class="username">管理员</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人中心</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { DataAnalysis, User, Fold, Expand, Bell, DataBoard, Document, Money, OfficeBuilding, TrendCharts, Coin, VideoCamera, Setting, Tools } from '@element-plus/icons-vue'

const route = useRoute()
const isCollapse = ref(false)

const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route)

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}
</script>

<style scoped lang="scss">
.sidebar {
  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: #fff;
    font-size: 18px;
    font-weight: 600;
    background: linear-gradient(180deg, #001529 0%, #000c17 100%);
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background: linear-gradient(180deg, #1890ff, #40a9ff);
    }

    .logo-title {
      white-space: nowrap;
      letter-spacing: 1px;
    }
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;

  .notification-badge {
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      opacity: 0.8;
    }
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 6px 12px;
    border-radius: 8px;
    transition: all 0.3s;

    &:hover {
      background: #f5f7fa;
    }

    .username {
      font-size: 14px;
      font-weight: 500;
      color: #303133;
    }
  }
}
</style>
