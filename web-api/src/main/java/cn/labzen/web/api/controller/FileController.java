package cn.labzen.web.api.controller;

import cn.labzen.web.api.definition.FileFormat;
import cn.labzen.web.api.response.result.Result;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * 继承本接口，使Controller具备自动处理文件上传下载的能力
 * <p>
 * 本接口提供的能力属于增量功能，不能脱离{@link StandardController}或{@link SimplestController}单独使用
 *
 * @param <RB> Resource Bean - 指定当前Controller处理的资源Bean，这里指需要导出或导入的文件内容关联的资源Bean
 */
public interface FileController<RB> extends LabzenController {

  @PostMapping("export-{format}")
  Result exports(@ModelAttribute RB resource, @PathVariable FileFormat format);

  @PostMapping("import")
  Result imports(@RequestParam("file") MultipartFile multipartFile);
}
