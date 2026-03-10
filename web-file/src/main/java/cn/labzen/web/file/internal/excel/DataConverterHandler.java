package cn.labzen.web.file.internal.excel;

import cn.labzen.web.file.definition.bean.Column;
import cn.labzen.web.file.definition.bean.Schema;
import com.alibaba.excel.converters.date.DateStringConverter;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import org.apache.poi.ss.usermodel.Cell;

import java.util.Date;

/**
 * 数据转换器处理
 */
public class DataConverterHandler extends AbstractCellWriteHandler {

  private final DateStringConverter dateStringConverter = new DateStringConverter();

  public DataConverterHandler(Schema schema) {
    super(schema);
  }

  /**
   * 在单元格创建之后，对一些特定的java类型，设置Converter
   */
  @Override
  public void afterCellCreate(CellWriteHandlerContext context) {
    Cell cell = context.getCell();
    Column currentColumn = super.findColumn(cell);
    if (currentColumn == null) {
      return;
    }

    ExcelContentProperty excelContentProperty = context.getExcelContentProperty();
//    if (currentColumn.getNullableConverter() != null) {
//      excelContentProperty.setConverter(new ObjectConvertConverter<>(currentColumn.getFieldType()));
//    }
//
//    if (currentColumn.getStringConverters() != null && !currentColumn.getStringConverters().isEmpty()) {
//      excelContentProperty.setConverter(new StringConvertConverter(currentColumn.getStringConverters()));
//    }

    Class<?> type = currentColumn.getFieldType();
    if (Date.class.equals(type)) {
      excelContentProperty.setConverter(dateStringConverter);
    }
  }
}
