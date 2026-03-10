package cn.labzen.web.file.internal;

import cn.labzen.web.file.definition.bean.Column;
import cn.labzen.web.file.definition.bean.Schema;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Arrays;
import java.util.List;

@Setter
public abstract class AbstractDataFileHandler<B> {

  protected final Class<B> type;
  protected List<B> data;
  protected File outputFile;

  public AbstractDataFileHandler(@Nonnull Class<B> type) {
    this.type = type;
  }

  public final void setData(List<B> data) {
    this.data = data;
  }

  public final void setOutputFilePath(String outputFilePath) {
    this.outputFile = new File(outputFilePath);
  }

  public final void setOutputFile(File outputFile) {
    this.outputFile = outputFile;
  }

  public final File getOutputFile() {
    return outputFile;
  }

  public abstract void handle();

  protected List<List<String>> readSchemaHead(Schema schema) {
    return schema.getColumns().stream().map(column -> Arrays.stream(column.getHeader()).toList()).toList();
  }

  protected List<String> getSchemaIncludedFieldNames(Schema schema) {
    return schema.getColumns().stream().map(Column::getFieldName).toList();
  }
}
