<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="商品名" prop="displayName">
        <el-input v-model="queryParams.displayName" placeholder="请输入商品名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="模型代码" prop="modelCode">
        <el-input v-model="queryParams.modelCode" placeholder="请输入模型代码" clearable @keyup.enter.native="handleQuery" />
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:pricing:add']">新增配置</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['system:pricing:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:pricing:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="pricingList" @selection-change="handleSelectionChange" border>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="商品名称" align="center" prop="displayName" min-width="150" show-overflow-tooltip />
      <el-table-column label="累计调用" align="center" prop="totalCalls" width="100">
        <template slot-scope="scope">
          <el-tag type="info">{{ scope.row.totalCalls || 0 }} 次</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="总成本(￥)" align="center" prop="totalCost" width="130">
        <template slot-scope="scope">
          <span style="color: #f56c6c; font-weight: bold;">
            {{ scope.row.totalCost ? scope.row.totalCost.toFixed(4) : '0.0000' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="计费模式" align="center" prop="billingMode" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.billingMode === 'req'">按次</el-tag>
          <el-tag v-else-if="scope.row.billingMode === 'token'" type="success">Token</el-tag>
          <el-tag v-else-if="scope.row.billingMode === 'second'" type="warning">按秒</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="isActive" width="80">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.isActive" :active-value="1" :inactive-value="0" @change="handleStatusChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:pricing:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:pricing:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="商品名称" prop="displayName">
              <el-input v-model="form.displayName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型代码" prop="modelCode">
              <el-input v-model="form.modelCode" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="模型类型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option label="文本(text)" value="text" />
                <el-option label="图片(image)" value="image" />
                <el-option label="视频(video)" value="video" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="厂商" prop="provider">
              <el-input v-model="form.provider" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="API地址" prop="apiEndpoint">
          <el-input v-model="form.apiEndpoint" />
        </el-form-item>
        <el-row>
          <el-col :span="12">
            <el-form-item label="Access Key" prop="accessKeyId">
              <el-input v-model="form.accessKeyId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Secret Key" prop="secretAccessKey">
              <el-input v-model="form.secretAccessKey" type="password" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="center">计费设置 (6位精度)</el-divider>
        <el-row>
          <el-col :span="12">
            <el-form-item label="计费模式" prop="billingMode">
              <el-select v-model="form.billingMode" style="width: 100%">
                <el-option label="按次" value="req" />
                <el-option label="按Token" value="token" />
                <el-option label="按秒" value="second" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序权重" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="按次积分" prop="pointsPerReq">
              <el-input-number v-model="form.pointsPerReq" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="1K积分" prop="pointsPer1kTokens">
              <el-input-number v-model="form.pointsPer1kTokens" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="按秒积分" prop="pointsPerSecond">
              <el-input-number v-model="form.pointsPerSecond" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="单次/秒成本" prop="costPerReqCny">
              <el-input-number v-model="form.costPerReqCny" :precision="6" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="1K Token成本" prop="costPer1kCny">
              <el-input-number v-model="form.costPer1kCny" :precision="6" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="🔬 AI 独立实验室" :visible.sync="labOpen" width="850px" append-to-body>
      <el-row :gutter="20">
        <el-col :span="6">
          <div style="font-weight:bold; margin-bottom:10px">1. 选择测试模型</div>
          <el-select v-model="labModelId" placeholder="请选择" style="width:100%" @change="onLabModelChange">
            <el-option v-for="item in pricingList" :key="item.id" :label="item.displayName" :value="item.id" />
          </el-select>
        </el-col>
        <el-col :span="18">
          <div class="chat-window" ref="chatWindow" style="height:400px; border:1px solid #EBEEF5; background:#F8F9FB; overflow-y:auto; padding:15px; border-radius:4px">
            <div v-for="(msg, i) in labHistory" :key="i" :class="['msg-box', msg.role]">
               <div class="msg-content">
                  <template v-if="msg.isImg"><el-image :src="msg.content" style="width:200px" /></template>
                  <template v-else-if="msg.isVid"><video :src="msg.content" controls style="width:100%" /></template>
                  <template v-else>{{ msg.content }}</template>
               </div>
            </div>
            <div v-if="labLoading" style="font-size:12px;color:#999"><i class="el-icon-loading"></i> AI正在思考...</div>
          </div>
          <div style="margin-top:15px; display:flex; gap:10px">
            <el-input v-model="labInput" placeholder="输入测试内容..." @keyup.enter.native="sendLabMsg" />
            <el-button type="primary" :loading="labLoading" @click="sendLabMsg">发送</el-button>
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
      labOpen: false, labLoading: false, labModelId: null, selectedLabModel: null, labInput: "", labHistory: [],
      queryParams: { pageNum: 1, pageSize: 10, displayName: null, modelCode: null },
      form: {},
      rules: {
        displayName: [{ required: true, message: "必填", trigger: "blur" }],
        modelCode: [{ required: true, message: "必填", trigger: "blur" }],
        apiEndpoint: [{ required: true, message: "必填", trigger: "blur" }],
        accessKeyId: [{ required: true, message: "必填", trigger: "blur" }]
      }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true;
      listPricing(this.queryParams).then(res => { this.pricingList = res.rows; this.total = res.total; this.loading = false; });
    },
    handleStatusChange(row) {
      updatePricing(row).then(() => { this.$modal.msgSuccess("状态更新成功"); }).catch(() => { row.isActive = row.isActive === 1 ? 0 : 1; });
    },
    handleOpenLab() { this.labOpen = true; },
    onLabModelChange(val) {
      this.selectedLabModel = this.pricingList.find(i => i.id === val);
      this.labHistory = [{ role: 'assistant', content: '连接成功，可以开始测试。' }];
    },
    sendLabMsg() {
      if (!this.labInput.trim() || !this.selectedLabModel) return;
      const text = this.labInput;
      this.labHistory.push({ role: 'user', content: text });
      this.labInput = ""; this.labLoading = true;
      testConnection({ ...this.selectedLabModel, remark: text }).then(res => {
        const data = res.data || "";
        this.labHistory.push({
          role: 'assistant',
          content: data.replace("[图片]: ", "").replace("[视频]: ", ""),
          isImg: data.includes("[图片]"), isVid: data.includes("[视频]")
        });
        this.$nextTick(() => { this.$refs.chatWindow.scrollTop = this.$refs.chatWindow.scrollHeight; });
      }).finally(() => { this.labLoading = false; });
    },
    reset() { this.form = { isActive: 1, billingMode: 'req', type: 'text', pointsPerReq: 0, costPerReqCny: 0 }; this.resetForm("form"); },
    handleAdd() { this.reset(); this.open = true; this.title = "新增配置"; },
    handleUpdate(row) {
      getPricing(row.id || this.ids).then(res => { this.form = res.data; this.open = true; this.title = "修改配置"; });
    },
    submitForm() {
      this.$refs["form"].validate(v => {
        if (v) {
          const p = this.form.id ? updatePricing(this.form) : addPricing(this.form);
          p.then(() => { this.$modal.msgSuccess("提交成功"); this.open = false; this.getList(); });
        }
      });
    },
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('确认删除?').then(() => delPricing(ids)).then(() => { this.getList(); this.$modal.msgSuccess("已删除"); });
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList(); },
    resetQuery() { this.resetForm("queryForm"); this.handleQuery(); },
    handleSelectionChange(s) { this.ids = s.map(i => i.id); this.single = s.length !== 1; this.multiple = !s.length; },
    cancel() { this.open = false; this.reset(); }
  }
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.msg-box { margin-bottom: 15px; display: flex; }
.msg-box.user { justify-content: flex-end; }
.msg-content { max-width: 80%; padding: 10px; border-radius: 8px; font-size: 14px; line-height: 1.5; }
.user .msg-content { background: #409EFF; color: #fff; }
.assistant .msg-content { background: #fff; border: 1px solid #ddd; color: #333; }
.chat-window::-webkit-scrollbar { width: 4px; }
.chat-window::-webkit-scrollbar-thumb { background: #ccc; border-radius: 2px; }
</style>