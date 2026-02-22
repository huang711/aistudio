<template>
  <div class="app-container workbench-container">
    <el-row :gutter="15">
      <el-col :span="4">
        <div class="history-side-panel">
          <div class="side-header">
            <span><i class="el-icon-time"></i> 历史记录</span>
          </div>
          <div class="history-list-mini">
            <el-empty v-if="tasksList.length === 0" :image-size="40"></el-empty>
            <div 
              v-for="item in tasksList" 
              :key="item.id" 
              @click="handleSelect(item)" 
              :class="['history-item-mini', currentId === item.id ? 'active' : '']"
            >
              <div class="item-top">
                <span class="item-title">{{ formatTitle(item.inputContent) }}</span>
                <i class="el-icon-close delete-icon" @click.stop="handleDelete(item)"></i>
              </div>
              <div class="item-time">{{ item.createTime }}</div>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :span="20">
        <el-card class="main-card-compact" shadow="never">
          <div slot="header" class="main-header">
            <div class="header-left">
              <span class="title">AI 剧本拆解工作台</span>
              <el-select v-model="form.usedModelId" size="mini" placeholder="解析模型" style="margin-left: 15px; width: 140px;">
                <el-option v-for="dict in textModelOptions" :key="dict.id" :label="dict.displayName" :value="dict.id"></el-option>
              </el-select>
              <el-button 
                type="primary" 
                size="mini" 
                icon="el-icon-upload" 
                style="margin-left: 15px" 
                @click="handleUpdateTask" 
                v-if="localParsedData.script_flow && localParsedData.script_flow.length > 0"
              >
                同步至云端
              </el-button>
            </div>
          </div>

          <div class="editor-section-mini">
            <el-input 
              type="textarea" 
              :rows="3" 
              placeholder="在此粘贴剧本内容..." 
              v-model="form.inputContent"
            ></el-input>
            <div class="action-bar-mini">
              <el-button type="primary" size="mini" icon="el-icon-cpu" @click="handleExecute" :loading="loading">
                开始解析
              </el-button>
            </div>
          </div>

          <div v-if="localParsedData.script_flow && localParsedData.script_flow.length > 0" class="output-container">
            <el-tabs v-model="activeWorkTab" type="border-card" class="work-tabs">
              
              <el-tab-pane name="asset">
                <span slot="label"><i class="el-icon-receiving"></i> 资产拆解</span>
                
                <div class="assets-library-compact ai-pending-box">
                  <div class="lib-header">
                    <span class="lib-title"><i class="el-icon-magic-stick"></i> AI 预拆解清单 (点击标签入库)</span>
                  </div>
                  <div class="lib-body">
                    <div class="pending-group" v-for="(list, type) in {角色: 'characters', 场景: 'scenes', 物品: 'props'}" :key="type">
                      <span class="group-label">{{ type }}:</span>
                      <el-tag v-for="(name, idx) in localParsedData.assets[list]" :key="idx" 
                        :type="isAssetExist(name, getAssetTypeKey(list)) ? 'success' : 'info'" 
                        size="small" class="m-tag clickable-tag" @click="quickAddAsset(name, getAssetTypeKey(list))">
                        {{ name }} <i :class="isAssetExist(name, getAssetTypeKey(list)) ? 'el-icon-check' : 'el-icon-plus'"></i>
                      </el-tag>
                    </div>
                  </div>
                </div>

                <div class="assets-library-compact">
                  <div class="lib-header">
                    <span class="lib-title"><i class="el-icon-collection"></i> 资产仓库 (已入库)</span>
                    <el-button type="text" size="mini" icon="el-icon-plus" @click="handleAddAsset">手动新增</el-button>
                  </div>
                  <div class="lib-body">
                    <el-tabs v-model="activeAssetTab" class="compact-tabs">
                      <el-tab-pane label="角色" name="character">
                        <el-tag v-for="a in dbCharacters" :key="a.id" size="mini" class="m-tag">👤 {{ a.name }}</el-tag>
                      </el-tab-pane>
                      <el-tab-pane label="场景" name="scene">
                        <el-tag v-for="a in dbScenes" :key="a.id" size="mini" type="success" class="m-tag">📍 {{ a.name }}</el-tag>
                      </el-tab-pane>
                      <el-tab-pane label="物品" name="prop">
                        <el-tag v-for="a in dbProps" :key="a.id" size="mini" type="warning" class="m-tag">📦 {{ a.name }}</el-tag>
                      </el-tab-pane>
                    </el-tabs>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane name="image">
                <span slot="label"><i class="el-icon-picture"></i> 资产生图</span>
                <div class="image-gen-container">
                  <el-row :gutter="20">
                    <el-col :span="9">
                      <div class="gen-box-card">
                        <div class="box-label">选择待生成的资产</div>
                        <el-select v-model="imageGenForm.targetAssetId" placeholder="从入库资产中选择" size="small" style="width: 100%" @change="handleAssetChange">
                          <el-option-group label="角色"><el-option v-for="c in dbCharacters" :key="c.id" :label="c.name" :value="c.id"></el-option></el-option-group>
                          <el-option-group label="场景"><el-option v-for="s in dbScenes" :key="s.id" :label="s.name" :value="s.id"></el-option></el-option-group>
                          <el-option-group label="物品"><el-option v-for="p in dbProps" :key="p.id" :label="p.name" :value="p.id"></el-option></el-option-group>
                        </el-select>

                        <div class="box-label" style="margin-top: 15px; display: flex; justify-content: space-between; align-items: center;">
                          <span>提示词 (Prompt)</span>
                          <el-button type="text" size="mini" icon="el-icon-magic-stick" :loading="optimizing" @click="handleOptimizeImagePrompt">美化</el-button>
                        </div>
                        <el-input type="textarea" :rows="6" v-model="imageGenForm.prompt" placeholder="选择资产后自动填充..."></el-input>

                        <div class="box-label" style="margin-top: 15px;">生图模型</div>
                        <el-select v-model="imageGenForm.modelId" placeholder="选择生图模型" size="small" style="width: 100%">
                          <el-option v-for="m in imageModelOptions" :key="m.id" :label="m.displayName" :value="m.id"></el-option>
                        </el-select>
                        <el-button type="primary" style="width: 100%; margin-top: 20px" icon="el-icon-magic-stick" :loading="genLoading" @click="handleGenerateImage">开始生成</el-button>
                      </div>
                    </el-col>
                    
                    <el-col :span="9">
                      <div class="image-preview-black">
                        <div v-if="genLoading" class="empty-preview">
                          <i class="el-icon-loading" style="font-size: 40px; color: #409EFF;"></i>
                          <p>AI 正在融合生成中...</p>
                        </div>
                        <div v-else-if="!generatedImageUrl" class="empty-preview">
                          <i class="el-icon-picture-outline" style="font-size: 40px; color: #444;"></i>
                          <p>生成图像预览区</p>
                        </div>
                        <el-image v-else :src="generatedImageUrl" fit="contain" class="full-img" :preview-src-list="[generatedImageUrl]"></el-image>
                      </div>
                    </el-col>

                    <el-col :span="6">
                      <div class="gen-box-card" v-if="generatedImageUrl && !genLoading">
                        <div class="box-label">操作确认</div>
                        <p class="hint-text">确认将此图片转存至私有云并绑定资产吗？</p>
                        <el-button type="success" size="small" icon="el-icon-check" @click="handleBindImage" style="width: 100%; margin-bottom: 10px;">转存并永久绑定</el-button>
                        <el-button type="info" size="small" icon="el-icon-delete" @click="generatedImageUrl = ''" style="width: 100%; margin-left: 0">弃用</el-button>
                      </div>
                    </el-col>
                  </el-row>
                </div>
              </el-tab-pane>

              <el-tab-pane name="script">
                <span slot="label"><i class="el-icon-film"></i> 剧本拆解</span>
                <div class="fragment-grid">
                  <div v-for="(node, index) in localParsedData.script_flow" :key="index" class="fragment-card-mini">
                    <div class="card-head-mini">
                      <el-tag size="mini" type="primary" effect="dark">片段 {{ index + 1 }}</el-tag>
                      <el-input size="mini" v-model="node.title" placeholder="标题" class="title-input-mini"></el-input>
                      <el-button type="text" size="mini" icon="el-icon-magic-stick" @click="goToCreative(node)">去创作</el-button>
                    </div>
                    <div class="card-body-mini">
                      <el-input type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" v-model="node.content"></el-input>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { listTasks, getTasks, addTasks, delTasks, updateTasks } from "@/api/aigc/tasks";
import request from '@/utils/request';

export default {
  name: "AigcWorkbench",
  data() {
    return {
      loading: false,
      genLoading: false,
      optimizing: false,
      currentId: null,
      activeWorkTab: 'asset',
      activeAssetTab: 'character',
      tasksList: [],
      textModelOptions: [],
      imageModelOptions: [],
      allDbAssets: [], 
      form: { 
        inputContent: "", 
        outputContent: "", 
        usedModelId: undefined, 
        taskType: 'script_parsing' 
      },
      localParsedData: { 
        assets: { characters: [], scenes: [], props: [] }, 
        script_flow: [] 
      },
      imageGenForm: { 
        targetAssetId: null, 
        prompt: "", 
        modelId: null, 
        textModelId: null 
      },
      generatedImageUrl: "" 
    };
  },
  computed: {
    dbCharacters() { return this.allDbAssets.filter(a => a.type === 'character'); },
    dbScenes() { return this.allDbAssets.filter(a => a.type === 'scene'); },
    dbProps() { return this.allDbAssets.filter(a => a.type === 'prop'); }
  },
  created() {
    this.getModelList();
    this.getDbAssets();
    this.getList();
  },
  methods: {
    // 助手方法：转换映射
    getAssetTypeKey(listName) {
      const map = { characters: 'character', scenes: 'scene', props: 'prop' };
      return map[listName] || 'prop';
    },

    // 获取模型列表
    getModelList() {
      request({ url: '/system/pricing/list', method: 'get' }).then(res => {
        const all = res.rows || res.data || [];
        this.imageModelOptions = all.filter(m => m.isActive === 1 && m.type?.toLowerCase().includes('image'));
        this.textModelOptions = all.filter(m => m.isActive === 1 && (m.type?.toLowerCase().includes('chat') || m.type?.toLowerCase().includes('text')));
        
        if (this.textModelOptions.length > 0) {
          this.form.usedModelId = this.textModelOptions[0].id;
          this.imageGenForm.textModelId = this.textModelOptions[0].id;
        }
        if (this.imageModelOptions.length > 0) {
          this.imageGenForm.modelId = this.imageModelOptions[0].id;
        }
      });
    },

    // 获取资产清单
    getDbAssets() {
      request({ url: '/aigc/assets/list', method: 'get', params: { workspaceId: 1 } }).then(res => { 
        this.allDbAssets = res.rows || res.data || []; 
      });
    },

    /**
     * 修改后的 handleBindImage (核心变化)
     * 利用后端 updateImage 的 HttpURLConnection 逻辑实现“真实不转发”
     */
    handleBindImage() {
      if (!this.imageGenForm.targetAssetId || !this.generatedImageUrl) {
        return this.$modal.msgWarning("请选择资产并生成图片");
      }

      this.$modal.loading("正在抓取 AI 图片并转存至云端...");

      // 这里直接传 AI 的临时 URL 给后端，后端 Service 会负责下载并上传 OSS
      request({
        url: '/aigc/assets/updateImage', 
        method: 'post',
        data: { 
          assetId: this.imageGenForm.targetAssetId, 
          imageUrl: this.generatedImageUrl 
        }
      }).then(res => {
        this.$modal.closeLoading();
        this.$modal.msgSuccess("转存成功！资产封面已永久固定。");
        this.generatedImageUrl = ""; // 成功后清除预览
        this.getDbAssets();           // 刷新资产列表获取最新 OSS 路径
      }).catch(err => {
        this.$modal.closeLoading();
        this.$modal.msgError("转存失败，请检查后端网络权限");
      });
    },

    // 快速入库 (如后端 add 接口也支持 url 传参，此处亦可扩展)
    quickAddAsset(name, type) {
      if (this.isAssetExist(name, type)) {
        return this.$modal.msgInfo("资产已存在");
      }
      this.$modal.loading("入库中...");
      request({ 
        url: '/aigc/assets/add', 
        method: 'post', 
        data: { name, type, workspaceId: 1 } 
      }).then(() => {
        this.$modal.closeLoading();
        this.$modal.msgSuccess(`[${name}] 入库成功`);
        this.getDbAssets(); 
      }).catch(() => this.$modal.closeLoading());
    },

    // 校验资产
    isAssetExist(name, type) { 
      return this.allDbAssets.some(a => a.name === name && a.type === type); 
    },

    // 执行解析
    handleExecute() {
      if (!this.form.inputContent) return this.$modal.msgWarning("请输入内容");
      this.loading = true;
      addTasks(this.form).then(res => {
        const data = res.data || res;
        this.currentId = data.id;
        this.parseOutputToLocal(data.outputContent);
        this.getList();
        this.$modal.msgSuccess("解析完成");
      }).finally(() => { this.loading = false; });
    },

    parseOutputToLocal(content) {
      try {
        const obj = typeof content === 'string' ? JSON.parse(content) : content;
        if (!obj.assets) obj.assets = { characters: [], scenes: [], props: [] };
        if (!obj.script_flow) obj.script_flow = [];
        this.localParsedData = obj;
      } catch (e) {
        this.localParsedData = { assets: { characters: [], scenes: [], props: [] }, script_flow: [] };
      }
    },

    handleSelect(row) {
      getTasks(row.id).then(res => {
        this.form = res.data;
        this.currentId = res.data.id;
        this.parseOutputToLocal(res.data.outputContent);
      });
    },

    getList() {
      listTasks({ pageNum: 1, pageSize: 20, taskType: 'script_parsing' }).then(res => { this.tasksList = res.rows || []; });
    },

    handleDelete(row) {
      this.$modal.confirm('确定删除该条记录？').then(() => delTasks(row.id)).then(() => this.getList());
    },

    formatTitle(c) { return c?.length > 15 ? c.substring(0, 15) + "..." : c || "未命名"; },

    handleUpdateTask() {
      updateTasks({ 
        id: this.currentId, 
        outputContent: JSON.stringify(this.localParsedData) 
      }).then(() => { this.$modal.msgSuccess("同步成功"); });
    },

    handleAssetChange(assetId) {
      const asset = this.allDbAssets.find(a => a.id === assetId);
      if (asset) this.$set(this.imageGenForm, 'prompt', asset.name);
    },

    handleGenerateImage() {
      if (!this.imageGenForm.prompt) return;
      this.genLoading = true;
      request({
        url: '/aigc/tasks/draw', 
        method: 'post',
        data: { prompt: this.imageGenForm.prompt, usedModelId: this.imageGenForm.modelId }
      }).then(res => {
        this.generatedImageUrl = res.data || res;
      }).finally(() => { this.genLoading = false; });
    },

    handleOptimizeImagePrompt() {
      if (!this.imageGenForm.prompt) return;
      this.optimizing = true;
      request({
        url: '/aigc/tasks/optimize-prompt', 
        method: 'post',
        data: { text: this.imageGenForm.prompt, usedModelId: this.imageGenForm.textModelId }
      }).then(res => {
        this.$set(this.imageGenForm, 'prompt', res.data || res);
      }).finally(() => { this.optimizing = false; });
    },

    handleAddAsset() {
      this.$prompt(`请输入名称`, `手动新增资产`, { confirmButtonText: '确定' }).then(({ value }) => {
        request({ 
          url: '/aigc/assets/add', 
          method: 'post', 
          data: { name: value, type: this.activeAssetTab, workspaceId: 1 } 
        }).then(() => { this.getDbAssets(); });
      });
    },

    goToCreative(node) {
      if (!node.content) return this.$modal.msgWarning("内容不能为空");
      this.$router.push({
        path: '/creative',
        query: { title: node.title, prompt: node.content }
      });
    }
  }
};
</script>

<style lang="scss" scoped>
/* 保持原有样式不变 */
.workbench-container {
  padding: 15px; background: #f4f7f9; min-height: calc(100vh - 84px);
  .history-side-panel {
    background: #fff; height: calc(100vh - 120px); border-radius: 8px; display: flex; flex-direction: column; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
    .side-header { padding: 15px; font-size: 14px; font-weight: bold; border-bottom: 1px solid #f0f0f0; }
    .history-list-mini { flex: 1; overflow-y: auto; }
  }
  .history-item-mini {
    padding: 12px 15px; border-bottom: 1px solid #f9f9f9; cursor: pointer; transition: all 0.2s;
    &:hover { background: #fcfcfc; }
    &.active { background: #e6f7ff; border-right: 3px solid #1890ff; }
    .item-title { font-size: 13px; color: #444; display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .item-time { font-size: 11px; color: #999; margin-top: 5px; }
  }
  .editor-section-mini { 
    background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; padding: 12px; margin-bottom: 20px;
    ::v-deep .el-textarea__inner { border: none; resize: none; font-size: 14px; line-height: 1.6; }
    .action-bar-mini { text-align: right; margin-top: 10px; border-top: 1px solid #f5f5f5; padding-top: 10px; }
  }
  .assets-library-compact {
    background: #fff; border: 1px solid #ebeef5; border-radius: 8px; padding: 15px; margin-bottom: 20px;
    .lib-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
    .lib-title { font-size: 14px; font-weight: 600; }
    .pending-group { 
      margin-bottom: 12px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
      .group-label { width: 45px; font-size: 12px; color: #909399; font-weight: bold; }
    }
  }
  .ai-pending-box { background: #fdfaf5; border: 1px dashed #e6a23c; }
  .image-gen-container {
    .gen-box-card { background: #fafafa; border-radius: 8px; padding: 20px; border: 1px solid #eee; }
    .box-label { font-size: 13px; font-weight: bold; margin-bottom: 10px; color: #606266; }
    .image-preview-black {
      background: #000; height: 420px; border-radius: 8px; display: flex; justify-content: center; align-items: center; border: 4px solid #333;
      .empty-preview { text-align: center; color: #666; font-size: 13px; }
      .full-img { max-width: 100%; max-height: 100%; }
    }
  }
  .fragment-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 15px; }
  .fragment-card-mini {
    background: #fff; border: 1px solid #e4e7ed; border-radius: 8px; padding: 15px;
    .card-head-mini { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
    .title-input-mini { flex: 1; }
  }
  .m-tag { margin-right: 5px; margin-bottom: 5px; }
  .clickable-tag { cursor: pointer; }
  .delete-icon { float: right; color: #ccc; &:hover { color: #f56c6c; } }
  .hint-text { font-size: 12px; color: #909399; margin-bottom: 15px; }
}
</style>