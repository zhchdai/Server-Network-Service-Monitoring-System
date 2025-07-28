<script setup>
import {computed, reactive, ref, watch} from "vue";
import {get, post} from "@/net";
import {copyIp, cpuNameToImage, fitByUnit, judgeStatusByPercentage, osNameToIcon, rename} from "@/tools";
import {ElMessage, ElMessageBox} from "element-plus";
import RuntimeHistory from "@/component/RuntimeHistory.vue";
import {Check, Delete} from "@element-plus/icons-vue";

const props = defineProps({
  id: String,
  update: Function
})
const emits = defineEmits(['delete', 'terminal'])

const details = reactive({
  base: {},
  runtime: {
    list: []
  },
  port: {},
  monitor: {},
  editNode: false
})

const nodeEdit = reactive({
  name: '',
  location: ''
})

const enableNodeEdit = () => {
  details.editNode = true
  nodeEdit.name = details.base.node
}

const submitNodeEdit = () => {
  post('/api/monitor/node', {
    id: props.id,
    node: nodeEdit.name,
  }, () => {
    details.editNode = false
    updateDetails()
    ElMessage.success('节点信息已更新')
  })
}

function deleteClient() {
  ElMessageBox.confirm('删除此主机后所有统计数据都将丢失，您确定要这样做吗？', '删除主机', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    get(`/api/monitor/delete?clientId=${props.id}`, () => {
      emits('delete')
      props.update()
      ElMessage.success('主机已成功移除')
    })
  }).catch(() => {
  })
}

function saveMonitorSettings() {
  post('api/user/saveMonitorSettings',details.monitor,() => {
    ElMessage.success('服务监控设置保存成功')
  })
}

function updateDetails() {
  props.update()
  init(props.id)
}

setInterval(() => {
  if (props.id !== null) {
    if (details.runtime) {
      get(`api/monitor/runtime-now?clientId=${props.id}`, data => {
        if (details.runtime.list.length >= 360) {
          details.runtime.list.splice(0, 1)
        }
        details.runtime.list.push(data)
      })
    }
    if (details.port) {
      get(`api/monitor/port?clientId=${props.id}`, data => {
        details.port = data
      })
    }
  }
}, 10000)

const now = computed(() => details.runtime.list[details.runtime.list.length - 1])

const init = id => {
  if (id !== null) {
    details.base = {}
    details.runtime = {list: []}
    details.port = {}
    details.monitor = {}
    get(`/api/monitor/details?clientId=${id}`, data => {
      Object.assign(details.base, data)
    })
    get(`api/monitor/runtime-history?clientId=${id}`, data => {
      Object.assign(details.runtime, data)
    })
    get(`api/monitor/port?clientId=${id}`, data => {
      Object.assign(details.port, data)
    })
    get(`api/monitor/getMonitorSettings?clientId=${id}`,data => {
      Object.assign(details.monitor,data)
      details.monitor.clientId=props.id
    })
  }
}

watch(() => props.id, init, {immediate: true})
</script>

<template>
  <el-scrollbar>
    <div class="client-details" v-loading="Object.keys(details.base).length === 0">
      <div v-if="Object.keys(details.base).length">
        <div style="display: flex;justify-content: space-between">
          <div class="title">
            <i class="fa-solid fa-server"></i>
            服务器信息
          </div>
          <div>
            <el-button :icon="Delete" type="danger" @click="deleteClient" style="margin-left: 0" plain text>删除此主机
            </el-button>
          </div>

        </div>
        <el-divider style="margin: 10px 0"/>
        <div class="details-list">
          <div>
            <span>服务器ID</span>
            <span>{{ details.base.id }}</span>
          </div>
          <div>
            <span>服务器名称</span>
            <span>{{ details.base.name }}</span>&nbsp;
            <i class="fa-solid fa-pen-to-square interact-item"
               @click.stop="rename(details.base.id,details.base.name,updateDetails)"></i>
          </div>
          <div>
            <span>运行状态</span>
            <span>
            <i style="color:#18cb18" class="fa-solid fa-circle-play" v-if="details.base.online"></i>
            <i style="color:#8a8a8a" class="fa-solid fa-circle-stop" v-else></i>
            {{ details.base.online ? '运行中' : '离线' }}
          </span>
          </div>
          <div v-if="!details.editNode">
            <span>服务器节点</span>
            <span style="margin-left: 5px">{{ details.base.node }}</span>&nbsp;
            <i @click.stop="enableNodeEdit"
               class="fa solid fa-pen-to-square interact-item"/>
          </div>
          <div v-else>
            <span>服务器节点</span>
            <div style="display: inline-block;height: 15px">
              <div style="display: flex">
                <el-input v-model="nodeEdit.name" size="small" placeholder="请输入节点名称..."/>
                <div style="margin-left: 10px">
                  <i @click.stop="submitNodeEdit" class="fa-solid fa-check interact-item"/>
                </div>
              </div>
            </div>
          </div>
          <div>
            <span>公网IP地址</span>
            <span>
            {{ details.base.ip }}
            <i class="fa-solid fa-copy interact-item" @click.stop="copyIp(details.base.ip)"
               style="color: dodgerblue"></i>
          </span>
          </div>
          <div style="display: flex">
            <span>处理器</span>
            <span>{{ details.base.cpuName }}</span>
            <el-image style="height: 20px;margin-left: 10px"
                      :src="`/cpu-icons/${cpuNameToImage(details.base.cpuName)}`"/>
          </div>
          <div>
            <span>硬件配置信息</span>
            <span>
            <i class="fa-solid fa-microchip"></i>
            <span style="margin-right: 10px">{{ ` ${details.base.cpuCore} CPU 核心数 /` }}</span>
            <i class="fa-solid fa-memory"></i>
            <span>{{ ` ${details.base.memory.toFixed(1)} GB 内存容量` }}</span>
          </span>
          </div>
          <div>
            <span>操作系统</span>
            <i :style="{color: osNameToIcon(details.base.osName).color}"
               :class="`fa-brands ${osNameToIcon(details.base.osName).icon}`"></i>
            <span style="margin-left: 10px">{{ `${details.base.osName} ${details.base.osVersion}` }}</span>
          </div>
        </div>
        <div class="title" style="margin-top: 20px">
          <i class="fa-solid fa-server"></i>
          实时监控
        </div>
        <el-divider style="margin: 10px 0"/>
        <div v-if="details.base.online" v-loading="!details.runtime.list.length" style="min-height: 200px">
          <div style="display: flex" v-if="details.runtime.list.length">
            <el-progress type="dashboard" :width="100" :percentage="now.cpuUsage * 100"
                         :status="judgeStatusByPercentage(now.cpuUsage * 100)">
              <div style="font-size: 17px;font-weight: bold;color: initial">CPU</div>
              <div style="font-size: 13px;color: grey;margin-top: 5px">{{ (now.cpuUsage * 100).toFixed(1) }}%</div>
            </el-progress>
            <el-progress style="margin-left: 20px"
                         type="dashboard" :width="100" :percentage="now.memoryUsage / details.runtime.memory * 100"
                         :status="judgeStatusByPercentage(now.memoryUsage / details.runtime.memory * 100)">
              <div style="font-size: 16px;font-weight: bold;color: initial">内存</div>
              <div style="font-size: 13px;color: grey;margin-top: 5px">{{ (now.memoryUsage).toFixed(1) }}GB</div>
            </el-progress>
            <div style="flex: 1;margin-left: 30px;display: flex;flex-direction: column;height: 80px">
              <div style="flex: 1;font-size: 14px">
                <div>实时网络速度</div>
                <div>
                  <i style="color: orange" class="fa-solid fa-arrow-up"></i>
                  <span>{{ ` ${fitByUnit(now.networkUpload, 'KB')}/s` }}</span>
                  <el-divider direction="vertical"/>
                  <i style="color: dodgerblue" class="fa-solid fa-arrow-down"></i>
                  <span>{{ ` ${fitByUnit(now.networkDownload, 'KB')}/s` }}</span>
                </div>
              </div>
              <div>
                <div style="font-size: 13px;display: flex;justify-content: space-between">
                  <div>
                    <i class="fa-solid fa-hard-drive"></i>
                    <span> 磁盘总容量</span>
                  </div>
                  <div>{{ now.diskUsage.toFixed(1) }} GB / {{ details.runtime.disk.toFixed(1) }} GB</div>
                </div>
                <el-progress type="line" :status="judgeStatusByPercentage(now.diskUsage / details.runtime.disk * 100)"
                             :percentage="now.diskUsage / details.runtime.disk * 100" :show-text="false"/>
              </div>
            </div>
          </div>
          <runtime-history style="margin-top: 20px" :data="details.runtime.list"/>
        </div>
        <el-empty description="服务器处于离线状态，请检查服务器是否正常运行" v-else/>

        <div class="title" style="margin-top: 20px;display: flex;justify-content: space-between">
          <div>
            <i class="fa-solid fa-server"></i>
            服务监控
          </div>

          <div>
            <el-button :icon="Check" type="info" @click="saveMonitorSettings" style="margin-left: 0" plain text>保存监控设置
            </el-button>
          </div>
        </div>

        <el-divider style="margin: 10px 0"/>
        <div class="details-list">
          <div>
            <span>主机状态</span>
            <span>
              <i style="color:#18cb18" class="fa-solid fa-circle-play" v-if="details.base.online"></i>
              <i style="color:#8a8a8a" class="fa-solid fa-circle-stop" v-else></i>
              {{ details.base.online ? '开机' : '离线' }}
              <i style="margin-left: 150px;color: #63E6BE" class="fa-solid fa-circle-check" @click="details.monitor.host = false" v-if="details.monitor.host"></i>
              <i style="margin-left: 150px" class="fa-regular fa-circle" @click="details.monitor.host = true" v-else></i>
              点击监控主机状态
            </span>
          </div>
          <div>
            <span>HTTP服务</span>
            <span>
              <i style="color:#18cb18" class="fa-solid fa-circle-play" v-if="details.port.httpStatus"></i>
              <i style="color:#8a8a8a" class="fa-solid fa-circle-stop" v-else></i>
              {{ details.port.httpStatus ? '开启' : '关闭' }}
              <i style="margin-left: 150px;color: #63E6BE" class="fa-solid fa-circle-check" @click="details.monitor.http = false" v-if="details.monitor.http"></i>
              <i style="margin-left: 150px" class="fa-regular fa-circle" @click="details.monitor.http = true" v-else></i>
              点击监控HTTP服务
            </span>
          </div>
          <div>
            <span>HTTPS服务</span>
            <span>
              <i style="color:#18cb18" class="fa-solid fa-circle-play" v-if="details.port.httpsStatus"></i>
              <i style="color:#8a8a8a" class="fa-solid fa-circle-stop" v-else></i>
              {{ details.port.httpsStatus ? '开启' : '关闭' }}
              <i style="margin-left: 150px;color: #63E6BE" class="fa-solid fa-circle-check" @click="details.monitor.https = false" v-if="details.monitor.https"></i>
              <i style="margin-left: 150px" class="fa-regular fa-circle" @click="details.monitor.https = true" v-else></i>
              点击监控HTTPS服务
            </span>
          </div>
          <div>
            <span>FTP服务</span>
            <span>
              <i style="color:#18cb18" class="fa-solid fa-circle-play" v-if="details.port.ftpStatus"></i>
              <i style="color:#8a8a8a" class="fa-solid fa-circle-stop" v-else></i>
              {{ details.port.ftpStatus ? '开启' : '关闭' }}
              <i style="margin-left: 150px;color: #63E6BE" class="fa-solid fa-circle-check" @click="details.monitor.ftp = false" v-if="details.monitor.ftp"></i>
              <i style="margin-left: 150px" class="fa-regular fa-circle" @click="details.monitor.ftp = true" v-else></i>
              点击监控FTP服务
            </span>
          </div>
          <div>
            <span>SFTP服务</span>
            <span>
              <i style="color:#18cb18" class="fa-solid fa-circle-play" v-if="details.port.sftpStatus"></i>
              <i style="color:#8a8a8a" class="fa-solid fa-circle-stop" v-else></i>
              {{ details.port.sftpStatus ? '开启' : '关闭' }}
              <i style="margin-left: 150px;color: #63E6BE" class="fa-solid fa-circle-check" @click="details.monitor.sftp = false" v-if="details.monitor.sftp"></i>
              <i style="margin-left: 150px" class="fa-regular fa-circle" @click="details.monitor.sftp = true" v-else></i>
              点击监控SFTP服务
            </span>
          </div>
          <div>
            <span>FTPS服务</span>
            <span>
              <i style="color:#18cb18" class="fa-solid fa-circle-play" v-if="details.port.ftpsStatus"></i>
              <i style="color:#8a8a8a" class="fa-solid fa-circle-stop" v-else></i>
              {{ details.port.ftpsStatus ? '开启' : '关闭' }}
              <i style="margin-left: 150px;color: #63E6BE" class="fa-solid fa-circle-check" @click="details.monitor.ftps = false" v-if="details.monitor.ftps"></i>
              <i style="margin-left: 150px" class="fa-regular fa-circle" @click="details.monitor.ftps = true" v-else></i>
              点击监控FTPS服务
            </span>
          </div>
          <div>
            <span>SMTP服务</span>
            <span>
              <i style="color:#18cb18" class="fa-solid fa-circle-play" v-if="details.port.smtpStatus"></i>
              <i style="color:#8a8a8a" class="fa-solid fa-circle-stop" v-else></i>
              {{ details.port.smtpStatus ? '开启' : '关闭' }}
              <i style="margin-left: 150px;color: #63E6BE" class="fa-solid fa-circle-check" @click="details.monitor.smtp = false" v-if="details.monitor.smtp"></i>
              <i style="margin-left: 150px" class="fa-regular fa-circle" @click="details.monitor.smtp = true" v-else></i>
              点击监控SMTP服务
            </span>
          </div>
        </div>
      </div>
    </div>
  </el-scrollbar>
</template>

<style scoped>
.client-details {
  height: 100%;
  padding: 20px;

  .title {
    color: dodgerblue;
    font-size: 18px;
    font-weight: bold;
  }

  .details-list {
    font-size: 14px;

    & div {
      margin-bottom: 10px;

      & span:first-child {
        color: grey;
        font-size: 13px;
        font-weight: normal;
        width: 120px;
        display: inline-block;
      }

      & span {
        font-weight: bold;
      }
    }
  }
}
</style>