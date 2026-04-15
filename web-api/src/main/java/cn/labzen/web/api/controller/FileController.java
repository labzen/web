package cn.labzen.web.api.controller;

import cn.labzen.web.api.definition.FileFormat;
import cn.labzen.web.api.response.result.Result;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件处理 Controller 接口。
 * <p>
 * 继承本接口，使 Controller 具备自动处理文件上传下载的能力。
 * <p>
 * <b>注意：</b> 本接口为增量功能，不能脱离 {@link StandardController} 或 {@link SimplestController} 单独使用。
 *
 * @param <RB> 资源 Bean，指定当前 Controller 处理的资源内容，用于导出或导入的文件关联
 */
public interface FileController<RB> extends LabzenController {

  @PostMapping("export-{format}")
  Result exports(@ModelAttribute RB resource, @PathVariable FileFormat format);

  @PostMapping("import")
  Result imports(@RequestParam("file") MultipartFile multipartFile);
}
