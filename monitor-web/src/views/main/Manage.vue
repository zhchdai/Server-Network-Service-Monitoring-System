<script setup>
import PreviewCard from "@/component/PreviewCard.vue";
import {computed, reactive, ref} from "vue";
import {get} from "@/net";
import ClientDetails from "@/component/ClientDetails.vue";
import RegisterCard from "@/component/RegisterCard.vue";
import {Plus} from "@element-plus/icons-vue";
import {useRoute} from "vue-router";
import {useStore} from "@/store";

const store = useStore()
const checkedNodes = ref([])

const list = ref([])

const route = useRoute()

const updateList = () => {
  if (route.name === 'manage') {
    get('/api/monitor/list', data => list.value = data)
  }
}
setInterval(updateList, 10000)
updateList()


const detail = reactive({
  show: false,
  id: null
})

const displayClientDetails = (id) => {
  detail.show = true
  detail.id = id
}

const clientList = computed(() => {
  if (checkedNodes.value.length === 0) {
    return list.value
  } else {
    return list.value.filter(item => checkedNodes.value.indexOf(item.location) >= 0)
  }
})

const register = reactive({
  show: false,
  token: ''
})

const refreshToken = () => get('/api/monitor/register', token => register.token = token)

function openTerminal(id) {
  terminal.show = true
  terminal.id = id
  detail.show = false
}

</script>

<template>
  <div class="manage-main">
    <div style="display: flex;justify-content: space-between;align-items: end">
      <div>
        <div class="title"><i class="fa-solid fa-server"></i> 管理主机列表</div>
        <div class="desc">在这里管理所有已经注册的主机实例，实时监控主机运行状态，快速进行管理和操作</div>
      </div>
      <div>
        <el-button :icon="Plus" type="primary" plain :disabled="!store.isAdmin"
                   @click="register.show = true">添加新主机</el-button>
      </div>
    </div>
    <el-divider style="margin: 10px 0"/>
    <div class="card-list" v-if="list.length">
      <preview-card v-for="item in clientList" :data="item" :update="updateList"
                    @click="displayClientDetails(item.id)"/>
    </div>
    <el-empty description="还没有任何主机，点击右上角添加" v-else/>
    <el-drawer size="520" :show-close="false" v-model="detail.show" :with-header="false" v-if="list.length"
               @close="detail.id=null">
      <client-details :id="detail.id" :update="updateList" @delete="updateList" @terminal="openTerminal"/>
    </el-drawer>
    <el-drawer v-model="register.show" direction="btt" :with-header="false"
               style="width: 600px;margin: 10px auto" size="320" @open="refreshToken">
      <register-card :token="register.token"/>
    </el-drawer>
  </div>
</template>

<style scoped>
:deep(.el-drawer__header) {
  margin-bottom: 10px;
}

:deep(.el-checkbox-group .el-checkbox) {
  margin-right: 10px;
}

:deep(.el-drawer) {
  margin: 10px;
  height: calc(100% - 20px);
  border-radius: 10px;
}

:deep(.el-drawer__body) {
  padding: 0;
}

.manage-main {
  margin: 0 50px;

  .title {
    font-size: 22px;
    font-weight: bold;
  }

  .desc {
    font-size: 15px;
    color: grey;
  }
}

.card-list {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}
</style>