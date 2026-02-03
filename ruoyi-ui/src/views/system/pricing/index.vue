<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="商品名" prop="displayName">
        <el-input v-model="queryParams.displayName" placeholder="请输入商品名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="模型代码" prop="modelCode">
        <el-input v-model="queryParams.modelCode" placeholder="请输入模型代码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="厂商" prop="provider">
        <el-input v-model="queryParams.provider" placeholder="请输入厂商" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="计费模式" prop="billingMode">
        <el-select v-model="queryParams.billingMode" placeholder="计费模式" clearable>
          <el-option label="按次" value="req" />
          <el-option label="按Token" value="token" />
          <el-option label="按秒" value="second" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        <el-button type="warning" icon="el-icon-cpu" size="mini" @click="handleOpenLab">AI 实验室</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:pricing:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['system:pricing:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:pricing:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="pricingList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="商品名" align="center" prop="displayName" />
      <el-table-column label="模型代码" align="center" prop="modelCode" />
      <el-table-column label="类型" align="center" prop="type" width="80" />
      <el-table-column label="厂商" align="center" prop="provider" />
      <el-table-column label="计费模式" align="center" prop="billingMode" width="100" />
      <el-table-column label="状态" align="center" prop="isActive">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isActive === 1 ? 'success' : 'danger'">
            {{ scope.row.isActive === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:pricing:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:pricing:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="750px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="商品名" prop="displayName"><el-input v-model="form.displayName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型代码" prop="modelCode"><el-input v-model="form.modelCode" /></el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="模型类型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option label="文本(text)" value="text" /><el-option label="图片(image)" value="image" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="厂商" prop="provider"><el-input v-model="form.provider" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="API地址" prop="apiEndpoint"><el-input v-model="form.apiEndpoint" /></el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="Access Key" prop="accessKeyId"><el-input v-model="form.accessKeyId" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Secret Key" prop="secretAccessKey"><el-input v-model="form.secretAccessKey" type="password" show-password /></el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="center">计费与成本设置</el-divider>
        <el-row>
          <el-col :span="12">
            <el-form-item label="计费模式" prop="billingMode">
              <el-select v-model="form.billingMode" style="width: 100%">
                <el-option label="按次" value="req" /><el-option label="按Token" value="token" /><el-option label="按秒" value="second" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上架状态" prop="isActive">
              <el-radio-group v-model="form.isActive">
                <el-radio :label="1">上架中</el-radio><el-radio :label="0">已下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="按次积分" prop="pointsPerReq"><el-input-number v-model="form.pointsPerReq" :precision="2" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="1K积分" prop="pointsPer1kTokens"><el-input-number v-model="form.pointsPer1kTokens" :precision="2" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="按秒积分" prop="pointsPerSecond"><el-input-number v-model="form.pointsPerSecond" :precision="2" style="width: 100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="次成本(￥)" prop="costPerReqCny"><el-input-number v-model="form.costPerReqCny" :precision="4" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="1K成本(￥)" prop="costPer1kCny"><el-input-number v-model="form.costPer1kCny" :precision="4" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序" prop="sortOrder"><el-input-number v-model="form.sortOrder" style="width: 100%" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer"><el-button type="primary" @click="submitForm">确 定</el-button><el-button @click="cancel">取 消</el-button></div>
    </el-dialog>

    <el-dialog title="🔬 AI 独立实验室" :visible.sync="labOpen" width="800px" append-to-body custom-class="lab-dialog">
      <el-row :gutter="20">
        <el-col :span="7">
          <div style="font-weight: bold; margin-bottom: 15px;">选择对话模型</div>
          <el-select v-model="labModelId" placeholder="选择模型" style="width: 100%" @change="onLabModelChange">
            <el-option v-for="item in pricingList" :key="item.id" :label="item.displayName" :value="item.id" />
          </el-select>
          <div v-if="selectedLabModel" style="margin-top: 20px; font-size: 13px; color: #666; background: #f9f9f9; padding: 10px; border-radius: 4px;">
            <p><b>代码:</b> {{ selectedLabModel.modelCode }}</p>
            <p><b>厂商:</b> {{ selectedLabModel.provider }}</p>
          </div>
        </el-col>
        <el-col :span="17">
          <div class="chat-window" ref="chatWindow" style="height: 350px; border: 1px solid #ddd; overflow-y: auto; padding: 15px; margin-bottom: 10px; background: #fff;">
            <div v-for="(msg, index) in labHistory" :key="index" :style="{ textAlign: msg.role === 'user' ? 'right' : 'left', marginBottom: '10px' }">
              <div :style="{ display: 'inline-block', padding: '8px 12px', borderRadius: '8px', maxWidth: '80%', background: msg.role === 'user' ? '#409EFF' : '#f4f4f5', color: msg.role === 'user' ? '#fff' : '#333' }">
                <template v-if="msg.isImg"><el-image :src="msg.content" :preview-src-list="[msg.content]" style="width: 150px" /></template>
                <template v-else>{{ msg.content }}</template>
              </div>
            </div>
            <div v-if="labLoading" style="color: #999; font-size: 12px;">AI正在思考...</div>
          </div>
          <el-input v-model="labInput" type="textarea" :rows="3" placeholder="请输入对话内容，Ctrl+Enter发送" @keyup.ctrl.enter.native="sendLabMsg" />
          <div style="text-align: right; margin-top: 10px;">
            <el-button size="mini" @click="labHistory = []">清空记录</el-button>
            <el-button size="mini" type="primary" :loading="labLoading" @click="sendLabMsg">发送消息</el-button>
          </div>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script>
import { listPricing, getPricing, delPricing, addPricing, updatePricing, testConnection } from "@/api/system/pricing"

export default {
  name: "Pricing",
  data() {
    return {
      loading: true, pricingList: [], total: 0, ids: [], single: true, multiple: true, showSearch: true,
      title: "", open: false,
      // 实验室相关
      labOpen: false, labLoading: false, labModelId: null, selectedLabModel: null, labInput: "", labHistory: [],
      queryParams: { pageNum: 1, pageSize: 10, displayName: null, modelCode: null },
      form: {},
      rules: { displayName: [{ required: true, message: "必填", trigger: "blur" }], modelCode: [{ required: true, message: "必填", trigger: "blur" }] }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true;
      listPricing(this.queryParams).then(res => { this.pricingList = res.rows; this.total = res.total; this.loading = false; });
    },
    // 实验室逻辑
    handleOpenLab() { this.labOpen = true; },
    onLabModelChange(val) { this.selectedLabModel = this.pricingList.find(i => i.id === val); this.labHistory = []; },
    sendLabMsg() {
      if (!this.labInput.trim() || !this.selectedLabModel) return;
      const text = this.labInput;
      this.labHistory.push({ role: 'user', content: text });
      this.labInput = ""; this.labLoading = true;
      testConnection({ ...this.selectedLabModel, remark: text }).then(res => {
        const isImg = res.data && (res.data.startsWith("http") || res.data.includes("[图片]"));
        this.labHistory.push({ role: 'assistant', content: isImg ? res.data.replace("[图片]: ", "") : res.data, isImg });
        this.$nextTick(() => { this.$refs.chatWindow.scrollTop = this.$refs.chatWindow.scrollHeight; });
      }).finally(() => { this.labLoading = false; });
    },
    // 原有逻辑
    reset() { this.form = { isActive: 1, billingMode: 'req', type: 'text', pointsPerReq: 0, pointsPer1kTokens: 0, costPerReqCny: 0, costPer1kCny: 0, sortOrder: 0 }; this.resetForm("form"); },
    handleAdd() { this.reset(); this.open = true; this.title = "新增配置"; },
    handleUpdate(row) { const id = row.id || this.ids; getPricing(id).then(res => { this.form = res.data; this.open = true; this.title = "修改配置"; }); },
    submitForm() {
      this.$refs["form"].validate(v => {
        if (v) {
          const api = this.form.id ? updatePricing : addPricing;
          api(this.form).then(() => { this.$modal.msgSuccess("成功"); this.open = false; this.getList(); });
        }
      });
    },
    handleDelete(row) { const ids = row.id || this.ids; this.$modal.confirm('确定删除?').then(() => delPricing(ids)).then(() => { this.getList(); this.$modal.msgSuccess("已删除"); }); },
    handleQuery() { this.queryParams.pageNum = 1; this.getList(); },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery(); },
    handleSelectionChange(s) { this.ids = s.map(i => i.id); this.single = s.length !== 1; this.multiple = !s.length; },
    cancel() { this.open = false; this.reset(); }
  }
}
</script>

<style scoped>.mb8 { margin-bottom: 8px; } .chat-window::-webkit-scrollbar { width: 5px; } .chat-window::-webkit-scrollbar-thumb { background: #ccc; border-radius: 10px; }</style>