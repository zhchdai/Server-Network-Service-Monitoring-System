<script setup>

</script>
<template>
    <div style="width: 100vw;height: 100vh;overflow: hidden;display: flex">
        <div style="flex: 1">
          <el-image style="width: 100%;height: 100%; object-fit: cover; opacity: 0.9"
               :src="'/background.png'"/>
        </div>
        <div class="welcome-title">
          <div style="font-size: 30px;font-weight: bold">服务器监控系统</div>
          <div style="margin-top: 10px">B21030913代展驰</div>
        </div>
        <div class="right-card">
            <router-view v-slot="{ Component }">
                <transition name="el-fade-in-linear" mode="out-in">
                    <component :is="Component" style="height: 100%"/>
                </transition>
            </router-view>
        </div>
    </div>
</template>

<style scoped>
.right-card {
  width: 400px;
  z-index: 1;
  background-color: var(--el-bg-color);
}

.welcome-title {
    position: absolute;
    bottom: 30px;
    left: 30px;
    color: white;
    text-shadow: 0 0 10px black;
}
</style>
