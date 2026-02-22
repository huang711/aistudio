<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick" style="margin-bottom: 20px;">
      <el-tab-pane label="全部资产" name="all"></el-tab-pane>
      <el-tab-pane label="角色" name="character"></el-tab-pane>
      <el-tab-pane label="场景" name="scene"></el-tab-pane>
      <el-tab-pane label="道具" name="prop"></el-tab-pane>
      <el-tab-pane label="剧本" name="script"></el-tab-pane>
      <el-tab-pane label="其他" name="other"></el-tab-pane>
    </el-tabs>

    <div class="storage-stat-container" v-if="storageStat">
      <div class="stat-header">
        <span class="stat-title"><i class="el-icon-pie-chart"></i> 资源存储状态</span>
        <div class="stat-details">
          <span>文件总数: <b>{{ storageStat.count }}</b></span>
          <span class="divider">|</span>
          <span>已用空间: <b :class="usageClass">{{ formatSize(storageStat.usage) }}</b> / {{ formatSize(storageStat.limit) }}</span>
        </div>
      </div>
      <el-progress 
        :percentage="storagePercentage" 
        :color="progressColors" 
        :stroke-width="12" 
        :format="p => p + '%'"
      ></el-progress>
      <p class="stat-tip">* 数据由阿里云 OSS 提供，约有 1 小时左右同步延迟</p>
    </div>

    <el-row :gutter="10" class="mb20" style="display: flex; align-items: center;">
      <el-col :span="1.5">
        <el-button type="primary" icon="el-icon-upload" size="small" @click="handleUpload">上传资产 / 剧本</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button 
          type="danger" 
          icon="el-icon-delete" 
          size="small" 
          plain 
          :disabled="multipleSelection.length === 0"
          @click="handleBatchDelete"
        >批量删除 ({{ multipleSelection.length }})</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-checkbox 
          v-model="isAllSelected" 
          @change="handleSelectAll" 
          style="margin-left: 15px;"
        >全选</el-checkbox>
      </el-col>
    </el-row>

    <el-row :gutter="20" v-loading="loading">
      <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="item in assetList" :key="item.id" class="mb20">
        <el-card 
          :body-style="{ padding: '0px' }" 
          shadow="hover" 
          class="asset-card" 
          :class="{ 'is-selected': isSelected(item) }"
        >
          <div class="image-container">
            <div class="card-checkbox" @click.stop>
              <el-checkbox :value="isSelected(item)" @change="toggleSelect(item)"></el-checkbox>
            </div>

            <el-image 
              v-if="isImage(item.url)"
              class="asset-image"
              :src="item.url" 
              :preview-src-list="[item.url]"
              fit="cover"
            >
              <div slot="error" class="image-slot"><i class="el-icon-picture-outline"></i></div>
            </el-image>

            <div v-else class="script-display" @click="handleDownload(item)">
               <i v-if="item.type === 'script'" class="el-icon-document" style="font-size: 48px; color: #409EFF;"></i>
               <i v-else class="el-icon-files" style="font-size: 48px; color: #909399;"></i>
               
               <span class="file-ext">{{ getFileExt(item.url) }}</span>
               <div class="download-mask"><i class="el-icon-download"></i> 点击下载</div>
            </div>
          </div>
          
          <div class="asset-info">
            <div class="asset-title" :title="item.name">{{ item.name || '未命名资产' }}</div>
            <div class="asset-footer">
              <el-tag size="mini" :type="getTagType(item.type)" effect="plain">
                {{ formatType(item.type) }}
              </el-tag>
              <el-button 
                type="text" 
                icon="el-icon-delete" 
                class="del-btn-text"
                @click="handleDelete(item)"
              >删除</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && assetList.length === 0" description="暂无内容"></el-empty>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog title="上传资产 / 剧本" :visible.sync="upload.open" width="450px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入资产或剧本名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
            <el-option label="角色 (图片)" value="character" />
            <el-option label="场景 (图片)" value="scene" />
            <el-option label="道具 (图片)" value="prop" />
            <el-option label="剧本 (文档)" value="script" />
            <el-option label="其他 (文件)" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            ref="upload"
            :limit="1"
            :auto-upload="false"
            :on-change="handleChange"
            :file-list="fileList"
            drag
          >
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">将图片、剧本或文件拖到此处，或<em>点击上传</em></div>
            <div class="el-upload__tip" slot="tip">支持图片、文档或任意格式文件</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFile" :loading="upload.isUploading">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAssets, uploadAsset, delAsset, getStorageStat } from "@/api/aigc/assets";

export default {
  name: "Assets",
  data() {
    return {
      activeTab: 'all',
      loading: true,
      total: 0,
      assetList: [],
      multipleSelection: [], 
      isAllSelected: false,  
      fileList: [],
      upload: { open: false, isUploading: false },
      storageStat: { usage: 0, limit: 10737418240, count: 0 }, 
      queryParams: {
        pageNum: 1,
        pageSize: 12,
        name: undefined,
        type: undefined,
        workspaceId: 1
      },
      form: { name: undefined, type: 'character', workspaceId: 1 },
      rules: { type: [{ required: true, message: "类型不能为空", trigger: "change" }] },
      progressColors: [
        { color: '#67C23A', percentage: 50 },
        { color: '#E6A23C', percentage: 80 },
        { color: '#F56C6C', percentage: 100 }
      ]
    };
  },
  computed: {
    storagePercentage() {
      if (!this.storageStat.limit) return 0;
      return Math.min(100, Math.round((this.storageStat.usage / this.storageStat.limit) * 100));
    },
    usageClass() {
      return this.storagePercentage > 85 ? 'text-danger' : 'text-primary';
    }
  },
  created() {
    this.getList();
    this.getStats();
  },
  methods: {
    /** 判断是否为图片 */
    isImage(url) {
      if (!url) return false;
      const extensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp'];
      return extensions.some(ext => url.toLowerCase().endsWith(ext));
    },
    /** 获取文件后缀 */
    getFileExt(url) {
      if (!url) return '';
      return url.substring(url.lastIndexOf('.') + 1).toUpperCase();
    },
    /** 标签视觉样式切换 */
    getTagType(type) {
      const map = { script: 'success', other: 'warning', character: 'info', scene: 'info', prop: 'info' };
      return map[type] || 'info';
    },
    /** 下载资源 */
    handleDownload(item) {
      window.open(item.url, '_blank');
    },
    /** 查询统计 */
    getStats() {
      getStorageStat().then(res => {
        if (res.data) this.storageStat = res.data;
      });
    },
    /** 查询列表 */
    getList() {
      this.loading = true;
      listAssets(this.queryParams).then(response => {
        this.assetList = response.rows;
        this.total = response.total;
        this.loading = false;
        this.isAllSelected = false;
        this.multipleSelection = [];
      });
    },
    formatSize(bytes) {
      if (bytes === 0) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },
    isSelected(item) {
      return this.multipleSelection.indexOf(item.id) > -1;
    },
    toggleSelect(item) {
      const index = this.multipleSelection.indexOf(item.id);
      if (index > -1) {
        this.multipleSelection.splice(index, 1);
      } else {
        this.multipleSelection.push(item.id);
      }
      this.isAllSelected = this.multipleSelection.length === this.assetList.length;
    },
    handleSelectAll(val) {
      this.multipleSelection = val ? this.assetList.map(item => item.id) : [];
    },
    handleDelete(row) {
      this.$modal.confirm('确定删除资产 "' + (row.name || row.id) + '" 吗？').then(() => {
        return delAsset(row.id);
      }).then(() => {
        this.getList();
        this.getStats();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleBatchDelete() {
      const ids = this.multipleSelection.join(',');
      this.$modal.confirm('确定删除选中的 ' + this.multipleSelection.length + ' 项吗？').then(() => {
        return delAsset(ids);
      }).then(() => {
        this.getList();
        this.getStats();
        this.$modal.msgSuccess("批量删除成功");
      }).catch(() => {});
    },
    handleTabClick(tab) {
      this.queryParams.type = tab.name === 'all' ? undefined : tab.name;
      this.queryParams.pageNum = 1;
      this.getList();
    },
    formatType(type) {
      const map = { character: '角色', scene: '场景', prop: '道具', script: '剧本', other: '其他' };
      return map[type] || type;
    },
    handleUpload() {
      this.upload.open = true;
      this.fileList = [];
      this.form.name = undefined;
    },
    handleChange(file, fileList) {
      this.fileList = fileList;
      if (!this.form.name && file.name) {
        this.form.name = file.name.substring(0, file.name.lastIndexOf('.'));
      }
      
      const ext = this.getFileExt(file.name);
      // 自动识别分类
      if (['DOCX', 'PDF', 'TXT'].includes(ext)) {
        this.form.type = 'script';
      } else if (!['JPG', 'JPEG', 'PNG', 'GIF', 'WEBP'].includes(ext)) {
        this.form.type = 'other';
      }
    },
    submitFile() {
      if (this.fileList.length === 0) return this.$message.error("请选择文件");
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.upload.isUploading = true;
          let formData = new FormData();
          formData.append("file", this.fileList[0].raw); 
          formData.append("type", this.form.type);
          formData.append("workspaceId", this.form.workspaceId);
          if (this.form.name) formData.append("name", this.form.name);

          uploadAsset(formData).then(response => {
            this.$modal.msgSuccess("上传成功");
            this.upload.open = false;
            this.upload.isUploading = false;
            this.getList();
            this.getStats();
          }).catch(() => { this.upload.isUploading = false; });
        }
      });
    }
  }
};
</script>

<style scoped>
.mb20 { margin-bottom: 20px; }

/* OSS 存储统计条 */
.storage-stat-container {
  background-color: #f8f9fb;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 15px 20px;
  margin-bottom: 25px;
}
.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.stat-title { font-size: 14px; font-weight: bold; color: #606266; }
.stat-details { font-size: 13px; color: #909399; }
.divider { margin: 0 10px; color: #dcdfe6; }
.stat-tip { font-size: 11px; color: #C0C4CC; margin-top: 8px; }
.text-danger { color: #F56C6C; }
.text-primary { color: #409EFF; }

/* 卡片样式 */
.asset-card {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.2s;
  border: 2px solid transparent; 
}
.asset-card.is-selected {
  border-color: #409EFF;
  background-color: #f0f7ff;
}

/* 内容容器 */
.image-container {
  position: relative;
  width: 100%;
  height: 180px;
  overflow: hidden;
  background-color: #f5f7fa;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 剧本/文件展示样式 */
.script-display {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  background-color: #f0f4f8;
  transition: background 0.3s;
}
.script-display:hover {
  background-color: #e6eef5;
}
.script-display:hover .download-mask {
  opacity: 1;
}
.file-ext {
  margin-top: 8px;
  font-size: 12px;
  font-weight: bold;
  color: #909399;
}
.download-mask {
  position: absolute;
  bottom: 0;
  width: 100%;
  height: 30px;
  background: rgba(64, 158, 255, 0.9);
  color: #fff;
  font-size: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.card-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.7);
  padding: 2px 4px;
  border-radius: 4px;
  line-height: 1;
}

.asset-image { width: 100%; height: 100%; cursor: zoom-in; }
.asset-info { padding: 12px; }
.asset-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.asset-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.del-btn-text { color: #F56C6C; padding: 0; font-size: 13px; }
</style>