<script setup>
import {reactive, ref} from "vue";
import {Switch, Lock, Plus, Delete, EditPen, Message, Refresh} from "@element-plus/icons-vue";
import {ElMessage} from "element-plus";
import {logout, post, get} from "@/net";
import router from "@/router";
import CreateSubAccount from "@/component/CreateSubAccount.vue";
import {useStore} from "@/store";

const store = useStore()
const formRef = ref()
const emailFormRef = ref()
const valid = ref(false)
const onValidate = (prop, isValid) => valid.value = isValid

const form = reactive({
  password: '',
  newPassword: '',
  newPasswordRepeat: ''
})

const emailForm = reactive({
  email: store.user.email,
  code: ''
})

const validatePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const coldTime = ref(0)
const isEmailValid = ref(true)

const onEmailValidate = (prop, isValid) => {
  if(prop === 'email')
    isEmailValid.value = isValid
}

const validateEmail = () => {
  coldTime.value = 60
  let handle
  get(`/api/auth/ask-code?email=${emailForm.email}&type=modify`, () => {
    ElMessage.success(`验证码已发送到邮箱: ${emailForm.email}，请注意查收`)
    handle = setInterval(() => {
      coldTime.value--
      if(coldTime.value === 0) {
        clearInterval(handle)
      }
    }, 1000)
  }, (message) => {
    ElMessage.warning(message)
    coldTime.value = 0
  })
}

function modifyEmail() {
  post('/api/user/modify-email',emailForm,() => {
    ElMessage.success('邮件修改成功')
    logout(() => router.push('/'))
  })
}

const rules = {
  password: [
    {required: true, message: '请输入原密码', trigger: 'blur'}
  ],
  newPassword: [
    {required: true, message: '请输入新密码', trigger: 'blur'},
    {min: 6, max: 15, message: '密码的长度必须在6-15个字符之间', trigger: 'blur'}
  ],
  newPasswordRepeat: [
    {required: true, message: '请重复输入新密码', trigger: 'blur'},
    {validator: validatePassword, trigger: ['blur', 'change']}
  ],
  email: [
    {required: true, message: '请输入邮件地址', trigger: 'blur'},
    {type: 'email', message: '请输入合法的电子邮件地址', trigger: ['blur', 'change']}
  ],
}

function resetPassword() {
  formRef.value.validate(valid => {
    if (valid) {
      post('/api/user/change-password', form, () => {
        ElMessage.success('修改密码成功，请重新登陆！')
        logout(() => router.push('/'))
      })
    }
  })
}

const simpleList = ref([])
if(store.isAdmin){
  get('/api/monitor/simple-list',list => {
    simpleList.value = list
    initSubAccounts()
  })
}


const accounts = ref([])
const initSubAccounts = () => {
  get('/api/user/sub/list',list => accounts.value = list)
}

const createAccount = ref(false)

function deleteAccount(id) {
  get(`/api/user/sub/delete?uid=${id}`,() => {
    ElMessage.success('子账户删除成功')
    initSubAccounts()
  })
}
</script>

<template>
  <div style="display: flex;gap: 10px;margin: 0 200px">
    <div style="flex: 50%">
      <div class="info-card">
        <div class="title"><i class="fa-solid fa-lock"></i> 修改密码</div>
        <el-divider style="margin: 10px 0"/>
        <el-form :rules="rules" :model="form" ref="formRef" @validate="onValidate"
                 label-width="100px" style="margin: 20px">
          <el-form-item label="当前密码" prop="password">
            <el-input type="password" :prefix-icon="Lock" v-model="form.password" placeholder="请输入当前密码"
                      maxlength="16"/>
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input type="password" :prefix-icon="Lock" v-model="form.newPassword" placeholder="请输入新密码"
                      maxlength="16"/>
          </el-form-item>
          <el-form-item label="重复新密码" prop="newPasswordRepeat">
            <el-input type="password" :prefix-icon="Lock" v-model="form.newPasswordRepeat" placeholder="重新输入新密码"
                      maxlength="16"/>
          </el-form-item>
          <div style="text-align: center">
            <el-button @click="resetPassword()" :icon="Switch" type="danger" plain>立即重置</el-button>
          </div>
        </el-form>
      </div>
      <div class="info-card" style="margin-top: 10px">
        <div class="title"><i class="fa-solid fa-envelope"></i> 电子邮件设置</div>
        <el-divider style="margin: 10px 0"/>
        <el-form :model="emailForm" :rules="rules" @validate="onEmailValidate"
                 ref="emailFormRef" label-position="top" style="margin: 0 10px 10px 10px">
          <el-form-item prop="email" label="电子邮件">
            <el-input v-model="emailForm.email"/>
          </el-form-item>
          <el-form-item>
            <el-row :gutter="10" style="width: 100%">
              <el-col :span="18">
                <el-input v-model="emailForm.code" :maxlength="6" placeholder="请输入验证码"/>
              </el-col>
              <el-col :span="6">
                <el-button type="success" @click="validateEmail"
                           :disabled="!isEmailValid || coldTime > 0" plain>
                  {{coldTime > 0 ? '请稍后 ' + coldTime + ' 秒' : '获取验证码'}}
                </el-button>
              </el-col>
            </el-row>
          </el-form-item>
          <div>
            <el-button @click="modifyEmail" :disabled="!emailForm.email"
                       :icon="Refresh" type="success" plain>保存电子邮件</el-button>
          </div>
        </el-form>
      </div>
    </div>
    <div class="info-card" style="flex: 50%">
      <div class="title"><i class="fa-solid fa-users"></i> 子用户管理</div>
      <el-divider style="margin: 10px 0"/>
      <div v-if="accounts.length" style="text-align: center">
        <div v-for="item in accounts" class="account-card">
          <el-avatar class="avatar" :size="30" :src="'/avatar.png'"/>
          <div style="margin-left: 15px;line-height: 18px;flex: 1">
            <div>
              <span>{{item.username}}</span>
              <span style="font-size: 13px;color: grey;margin-left: 5px">
                管理{{item.clientList.length}}个服务器
              </span>
            </div>
            <div style="font-size: 13px;color: grey">{{item.email}}</div>
          </div>
          <el-button type="danger" :icon="Delete"
                     @click="deleteAccount(item.id)" text>删除子账户</el-button>
        </div>
        <el-button :icon="Plus" type="primary"
                   @click="createAccount = true" plain>添加更多子用户</el-button>
      </div>
      <div v-else>
        <el-empty :image-size="100" description="还没有任何子用户" v-if="store.isAdmin">
          <el-button :icon="Plus" type="primary" @click="createAccount = true" plain>添加子用户</el-button>
        </el-empty>
        <el-empty v-else description="子账号只能由管理员账号进行操作"/>
      </div>

    </div>
    <el-drawer v-model="createAccount" size="350" :with-header="false">
      <create-sub-account :clients="simpleList" @create="createAccount = false;initSubAccounts()"/>
    </el-drawer>
  </div>
</template>

<style scoped>
:deep(.el-drawer) {
  margin: 10px;
  height: calc(100% - 20px);
  border-radius: 10px;
}

:deep(.el-drawer__body) {
  padding: 0;
}

.info-card {
  border-radius: 7px;
  padding: 15px 20px;
  background-color: var(--el-bg-color);
  height: fit-content;

  .title {
    font-size: 18px;
    font-weight: bold;
    color: dodgerblue;
  }
}

.account-card {
  border-radius: 5px;
  background-color: var(--el-bg-color-page);
  padding: 10px;
  display: flex;
  align-items: center;
  text-align: left;
  margin: 10px 0;
}
</style>