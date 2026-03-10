package cn.labzen.web.file.internal.excel;

import cn.labzen.web.file.definition.bean.Schema;
import cn.labzen.web.file.internal.AbstractDataFileHandler;
import cn.labzen.web.file.internal.WritableDataBeanParser;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;

public class ExcelFileHandler<B> extends AbstractDataFileHandler<B> {

  public ExcelFileHandler(Class<B> type) {
    super(type);
  }

  @Override
  public void handle() {
    ExcelWriterBuilder builder;
    if (outputFile != null) {
      builder = EasyExcel.write(outputFile);
    } else {
      throw new RuntimeException("未指定输出文件");
    }

    WritableDataBeanParser parser = new WritableDataBeanParser(super.type);
    Schema schema = parser.parse();

    builder
      .head(readSchemaHead(schema))
      .includeColumnFieldNames(getSchemaIncludedFieldNames(schema))
      .useDefaultStyle(false)
      .orderByIncludeColumn(true)
      .registerWriteHandler(new DataConverterHandler(schema))
      .registerWriteHandler(new ColumnWidthHandler(schema))
      .registerWriteHandler(new CellStyleHandler(schema))
      .sheet("data")
      .doWrite(data);
  }

}
