<template>
  <div class="app-container creative-container">
    <el-row :gutter="20">
      <el-col :span="10">
        <el-card class="box-card mini-card" shadow="never">
          <div slot="header" class="card-header-flex">
            <span class="header-title">创作参数配置</span>
            <el-tag v-if="currentTitle" type="warning" size="mini" effect="plain">{{ currentTitle }}</el-tag>
          </div>
          
          <el-form :model="creationForm" label-position="top" size="small">
            <el-form-item label="剧本片段原文">
              <el-input type="textarea" v-model="creationForm.originalContent" :rows="3" readonly class="readonly-textarea"></el-input>
            </el-form-item>

            <el-form-item>
              <div slot="label" class="label-with-btn">
                <span style="font-weight: bold; color: #333;">AI 创作提示词 (Prompt)</span>
                <div class="opt-group">
                  <el-select v-model="creationForm.textModelId" placeholder="美化模型" size="mini" style="width: 110px; margin-right: 8px;">
                    <el-option v-for="m in textModelOptions" :key="m.id" :label="m.displayName || m.name" :value="m.id"></el-option>
                  </el-select>
                  <el-button type="primary" size="mini" icon="el-icon-magic-stick" @click="handleOptimizePrompt" :loading="optimizing">美化提示词</el-button>
                </div>
              </div>
              <div :class="['prompt-input-wrapper', isHighlighted ? 'is-optimized' : '']">
                <el-input type="textarea" v-model="creationForm.prompt" :rows="6" @focus="isHighlighted = false" placeholder="在此输入描述词..."></el-input>
                <transition name="el-fade-in"><div class="optimized-tag" v-if="isHighlighted">✨ AI 已优化</div></transition>
              </div>
            </el-form-item>

            <el-form-item label="图像参考 (多维参考融合)">
              <div class="reference-grid">
                <div class="ref-item" v-for="type in referenceTypes" :key="type.key">
                  <span class="ref-label">{{ type.label }} <small v-if="type.key === 'person'">(多选)</small></span>
                  <el-select 
                    v-model="creationForm.selectedAssetIds[type.key]" 
                    :placeholder="type.key === 'person' ? '多选' : '单选'" 
                    size="mini" 
                    clearable
                    :multiple="type.key === 'person'"
                    collapse-tags
                    @change="(val) => handleAssetSelect(val, type.key)"
                    style="margin-bottom: 8px; width: 100%"
                  >
                    <el-option v-for="asset in getAssetsByType(type.assetType)" :key="asset.id" :label="asset.name" :value="asset.id">
                      <div class="asset-option-slot">
                        <span style="float: left">{{ asset.name }}</span>
                        <el-image v-if="asset.url" :src="asset.url" style="float: right; width: 20px; height: 20px; margin-top: 4px; border-radius: 2px;" fit="cover"></el-image>
                      </div>
                    </el-option>
                  </el-select>

                  <el-upload
                    action="#"
                    list-type="picture-card"
                    :auto-upload="false"
                    :on-change="(file, fileList) => handleRefChange(file, fileList, type.key)"
                    :on-remove="(file, fileList) => handleRefRemove(fileList, type.key)"
                    :file-list="creationForm.references[type.key]"
                    :limit="type.key === 'person' ? 5 : 1"
                    class="mini-uploader">
                    <i class="el-icon-plus"></i>
                  </el-upload>
                </div>
              </div>
            </el-form-item>

            <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item label="生图模型">
                  <el-select v-model="creationForm.modelId" placeholder="选择生图模型" style="width: 100%">
                    <el-option v-for="m in imageModelOptions" :key="m.id" :label="m.displayName || m.name" :value="m.id"></el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="画面比例">
                  <el-select v-model="creationForm.aspectRatio" style="width: 100%">
                    <el-option label="16:9 电影横屏" value="16:9"></el-option>
                    <el-option label="9:16 短视频竖屏" value="9:16"></el-option>
                    <el-option label="1:1 正方形" value="1:1"></el-option>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-button type="primary" class="generate-btn" icon="el-icon-picture" @click="handleGenerate" :loading="generating">融合生成最终画面</el-button>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card class="box-card result-card" shadow="never">
          <div slot="header" class="card-header-flex">
             <span class="header-title">创作结果展示</span>
             <el-button v-if="generating" type="text" size="mini" icon="el-icon-refresh" @click="checkTaskStatus">手动刷新状态</el-button>
          </div>
          <div class="result-display">
            <div v-if="!resultImageUrl && !generating" class="empty-result">
              <i class="el-icon-picture-outline" style="font-size: 80px; color: #444"></i>
              <p>等待 AI 融合创作...</p>
            </div>
            
            <div v-loading="generating" 
                  :element-loading-text="taskStatusText" 
                  element-loading-background="rgba(0, 0, 0, 0.8)" 
                  class="image-canvas-wrapper">
              <el-image 
                v-if="resultImageUrl" 
                :key="resultImageUrl"
                :src="resultImageUrl" 
                :preview-src-list="[resultImageUrl]" 
                fit="contain" 
                class="inner-img"
              >
                <div slot="error" class="image-slot">
                  <i class="el-icon-picture-outline"></i> 正在读取图片资源...
                </div>
              </el-image>
            </div>
          </div>
          <div class="result-actions" v-if="resultImageUrl && !generating">
            <div class="action-hint">确认将此图片转存并绑定至资产封面吗？</div>
            <el-button type="primary" size="medium" icon="el-icon-link" @click="handleBindToAsset" :disabled="!creationForm.selectedAssetIds.scene">绑定到当前场景资产</el-button>
            <el-button type="success" size="medium" icon="el-icon-folder-add" @click="handleSaveAsNewAsset">存为新资产</el-button>
            <el-button type="info" size="medium" icon="el-icon-download" @click="downloadImage">下载本地</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import request from '@/utils/request';

export default {
  name: "AigcCreative",
  data() {
    return {
      optimizing: false,
      generating: false,
      isHighlighted: false, 
      resultImageUrl: "",
      currentTitle: "",
      currentTaskId: null,
      pollingTimer: null,
      taskStatusText: "AI 正在多维融合...",
      allDbAssets: [], 
      referenceTypes: [
        { key: 'person', label: '人物', assetType: 'character' },
        { key: 'scene', label: '场景', assetType: 'scene' },
        { key: 'item', label: '道具', assetType: 'prop' }, 
        { key: 'other', label: '风格', assetType: 'style' } 
      ],
      imageModelOptions: [], 
      textModelOptions: [],  
      creationForm: {
        originalContent: "", 
        prompt: "",          
        modelId: undefined,     
        textModelId: undefined, 
        aspectRatio: "16:9",
        selectedAssetIds: { person: [], scene: null, item: null, other: null },
        references: { person: [], scene: [], item: [], other: [] }
      }
    };
  },
  created() {
    this.getAllModelList();
    this.getDbAssets(); 
    const { title, prompt } = this.$route.query;
    if (title || prompt) {
      this.currentTitle = title || "未命名片段";
      this.creationForm.originalContent = prompt || "";
      this.creationForm.prompt = prompt || "";
    }
  },
  beforeDestroy() {
    this.stopPolling();
  },
  methods: {
    getDbAssets() {
      request({ url: '/aigc/assets/list', method: 'get', params: { workspaceId: 1 } }).then(res => { 
        this.allDbAssets = res.rows || res.data || (Array.isArray(res) ? res : []); 
      });
    },
    getAssetsByType(type) {
      return this.allDbAssets.filter(a => a.type === type);
    },
    handleAssetSelect(val, key) {
      if (!val || (Array.isArray(val) && val.length === 0)) {
        this.$set(this.creationForm.references, key, []);
        return;
      }
      if (key === 'person') {
        const selectedRefs = [];
        val.forEach(id => {
          const asset = this.allDbAssets.find(a => a.id === id);
          if (asset && asset.url) {
            selectedRefs.push({ name: asset.name, url: asset.url, isFromDb: true });
          }
        });
        this.$set(this.creationForm.references, key, selectedRefs);
      } else {
        const asset = this.allDbAssets.find(a => a.id === val);
        if (asset && asset.url) {
          this.$set(this.creationForm.references, key, [{ name: asset.name, url: asset.url, isFromDb: true }]);
        } else {
          this.$set(this.creationForm.references, key, []);
        }
      }
    },
    handleRefChange(file, fileList, key) {
      this.$set(this.creationForm.references, key, fileList);
      if (key === 'person') { this.creationForm.selectedAssetIds[key] = []; } 
      else { this.creationForm.selectedAssetIds[key] = null; }
    },
    handleRefRemove(fileList, key) {
      this.$set(this.creationForm.references, key, fileList);
    },
    getAllModelList() {
      request({ url: '/system/pricing/list', method: 'get' }).then(res => {
        const all = res.rows || res.data || [];
        this.imageModelOptions = all.filter(m => m.isActive === 1 && m.type?.toLowerCase().includes('image'));
        this.textModelOptions = all.filter(m => m.isActive === 1 && (m.type?.toLowerCase().includes('chat') || m.type?.toLowerCase().includes('text')));
        if (this.imageModelOptions.length > 0) this.creationForm.modelId = this.imageModelOptions[0].id;
        if (this.textModelOptions.length > 0) this.creationForm.textModelId = this.textModelOptions[0].id;
      });
    },
    handleOptimizePrompt() {
      if (!this.creationForm.prompt) return;
      this.optimizing = true;
      request({
        url: '/aigc/tasks/optimize-prompt', 
        method: 'post',
        data: { text: this.creationForm.prompt, usedModelId: this.creationForm.textModelId }
      }).then(res => {
        this.creationForm.prompt = res.data || res; 
        this.isHighlighted = true;
      }).finally(() => { this.optimizing = false; });
    },

    handleGenerate() {
      if (!this.creationForm.prompt) return;
      this.generating = true;
      this.resultImageUrl = "";
      this.currentTaskId = null;

      const multiRefs = {};
      Object.keys(this.creationForm.references).forEach(key => {
        const files = this.creationForm.references[key];
        if (files.length > 0) {
          if (key === 'person') { multiRefs[key] = files.map(f => f.url || f.raw); } 
          else { multiRefs[key] = files[0].url || files[0].raw; }
        }
      });

      request({
        url: '/aigc/tasks/draw', 
        method: 'post',
        data: {
          prompt: this.creationForm.prompt,
          usedModelId: this.creationForm.modelId,
          aspectRatio: this.creationForm.aspectRatio,
          multiRefs: multiRefs 
        }
      }).then(res => {
        const data = res.data || res;
        if (typeof data === 'string' && data.includes('http')) {
          this.setResultImage(data);
        } else if (data && data.url) {
          this.setResultImage(data.url);
        } else {
          this.currentTaskId = data.taskId || data.id || data;
          this.startPolling();
        }
      }).catch(() => { this.generating = false; });
    },

    setResultImage(url) {
      if (!url) return;
      this.resultImageUrl = String(url).replace(/^"|"$/g, '').trim();
      this.generating = false;
      this.stopPolling();
    },

    startPolling() {
      this.stopPolling();
      this.pollingTimer = setInterval(() => { this.checkTaskStatus(); }, 3000);
    },

    stopPolling() {
      if (this.pollingTimer) { 
        clearInterval(this.pollingTimer); 
        this.pollingTimer = null; 
      }
    },

    checkTaskStatus() {
      if (!this.currentTaskId || String(this.currentTaskId).includes('http')) return;
      request({ 
        url: `/aigc/tasks/status/${this.currentTaskId}`, 
        method: 'get' 
      }).then(res => {
        const data = res.data || res;
        const url = data.url || (typeof data === 'string' && data.includes('http') ? data : null);
        if (url) {
          this.setResultImage(url);
        }
      });
    },

    /**
     * 绑定到现有资产
     */
    handleBindToAsset() {
      const assetId = this.creationForm.selectedAssetIds.scene;
      if (!assetId || !this.resultImageUrl) {
        this.$modal.msgWarning("请选择一个场景资产作为绑定目标");
        return;
      }
      this.$modal.loading("正在下载 AI 图片并同步至资产...");
      request({
        url: '/aigc/assets/updateImage', 
        method: 'post',
        data: { assetId: assetId, imageUrl: this.resultImageUrl }
      }).then(res => {
        this.$modal.closeLoading();
        this.$modal.msgSuccess("资产封面转存并绑定成功");
        this.getDbAssets(); 
      }).catch(() => {
        this.$modal.closeLoading();
      });
    },

    /**
     * 【深度优化版】存为新资产
     * 得益于后端的升级，这里只需要发送一个 url 字段，后端会自动下载并存入本地存储
     */
    handleSaveAsNewAsset() {
      if (!this.resultImageUrl) return;
      
      let defaultName = "AI生成_" + new Date().getTime();
      this.$prompt('请输入新资产名称', '存为新资产', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: defaultName
      }).then(({ value }) => {
        this.$modal.loading("AI 正在下载并处理图片，请稍候...");
        
        // 发送一次 add 请求，把 URL 传给后端
        // 后端 Service 层现在会自动检测 url 字段并执行转存 OSS 逻辑
        request({
          url: '/aigc/assets/add',
          method: 'post',
          data: { 
            name: value, 
            url: this.resultImageUrl, // 关键：后端会自动根据这个 URL 转存
            type: 'scene', 
            workspaceId: 1 
          }
        }).then(res => {
          this.$modal.closeLoading();
          this.$modal.msgSuccess("新资产保存成功，图片已存入服务器");
          this.getDbAssets(); // 刷新本地资产列表
        }).catch(() => {
          this.$modal.closeLoading();
          this.$modal.msgError("新增资产失败");
        });
      });
    },

    downloadImage() {
      if (!this.resultImageUrl) return;
      const a = document.createElement('a');
      a.href = this.resultImageUrl;
      a.download = 'AI_Creative_' + new Date().getTime() + '.png';
      a.click();
    }
  }
};
</script>

<style lang="scss" scoped>
/* 样式保持一致 */
.creative-container {
  padding: 20px; background: #f4f7f9; min-height: calc(100vh - 84px);
  .mini-card { margin-bottom: 20px; }
  .card-header-flex { display: flex; justify-content: space-between; align-items: center; }
  .header-title { font-weight: bold; color: #333; }
  .label-with-btn { display: flex; justify-content: space-between; align-items: center; width: 100%; }
  
  .prompt-input-wrapper {
    position: relative;
    &.is-optimized ::v-deep .el-textarea__inner { border-color: #409EFF; background-color: #f0f7ff; }
    .optimized-tag {
      position: absolute; right: 10px; bottom: 10px; background: #409EFF;
      color: white; padding: 2px 8px; border-radius: 4px; font-size: 10px; z-index: 10;
    }
  }

  .reference-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; }
  .ref-item { 
    background: #fff; padding: 10px; border: 1px solid #eee; border-radius: 4px;
    .ref-label { font-size: 12px; color: #666; margin-bottom: 8px; display: block; font-weight: bold; }
  }

  .generate-btn { width: 100%; margin-top: 10px; padding: 12px; font-weight: bold; font-size: 14px; }

  .result-card { 
    height: calc(100vh - 124px); 
    .result-display {
      height: 520px; background: #000; border-radius: 8px; position: relative;
      display: flex; align-items: center; justify-content: center; overflow: hidden; border: 1px solid #333;
      .image-canvas-wrapper { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
      .inner-img { width: 100%; height: 100%; }
      .empty-result { text-align: center; color: #666; }
    }
    .result-actions { 
      margin-top: 20px; text-align: center; 
      .action-hint { font-size: 13px; color: #909399; margin-bottom: 12px; }
    }
  }

  .mini-uploader ::v-deep .el-upload--picture-card { width: 48px; height: 48px; line-height: 54px; }
  .mini-uploader ::v-deep .el-upload-list--picture-card .el-upload-list__item { width: 48px; height: 48px; }
  .asset-option-slot { width: 100%; font-size: 13px; }
  .readonly-textarea ::v-deep .el-textarea__inner { background-color: #f9f9f9; border-color: #eee; color: #888; }
}
</style>