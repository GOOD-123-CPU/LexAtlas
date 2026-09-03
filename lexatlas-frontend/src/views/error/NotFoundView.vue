<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found-page">
    <div class="nf-card">
      <div class="nf-code">404</div>
      <div class="nf-title">页面不存在</div>
      <div class="nf-desc">您访问的页面可能已被移除、重命名或暂时不可用。</div>
      <div class="nf-actions">
        <el-button type="primary" @click="router.push('/')">返回首页</el-button>
        <el-button @click="router.back()">返回上一页</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f1b2d 0%, #16283f 100%);
}

.nf-card {
  text-align: center;
  color: #e8edf4;
  padding: 48px 64px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.nf-code {
  font-size: 96px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(120deg, #d4b06a, #f0d9a8);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.nf-title {
  font-size: 22px;
  margin-top: 16px;
  font-weight: 600;
}

.nf-desc {
  margin-top: 8px;
  color: #9aa7b8;
  font-size: 14px;
}

.nf-actions {
  margin-top: 28px;
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
