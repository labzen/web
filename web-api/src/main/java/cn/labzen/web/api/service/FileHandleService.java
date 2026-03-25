package cn.labzen.web.api.service;

import cn.labzen.web.api.definition.FileFormat;
import cn.labzen.web.api.definition.UploadedFile;
import cn.labzen.web.api.response.result.Result;
import cn.labzen.web.api.response.result.Results;

import java.io.File;

/**
 * 文件处理服务接口
 * <p>
 * 提供文件导入导出功能的标准定义
 *
 * @param <RB> 资源类型（Resource Bean）
 */
public interface FileHandleService<RB> {

  /**
   * 导出资源数据到指定格式的文件
   *
   * @param resource 资源对象
   * @param format   导出文件格式（如 Excel、PDF 等）
   * @return 使用{@link Results#file(File)}导出结果文件，可包含文件流或文件路径
   */
  Result exports(RB resource, FileFormat format);

  /**
   * 从上传的文件中导入数据到数据库等目标
   *
   * @param uploadedFile 已上传的文件对象封装
   * @return 导入结果，通常包含导入状态和消息
   */
  Result imports(UploadedFile uploadedFile);
}
