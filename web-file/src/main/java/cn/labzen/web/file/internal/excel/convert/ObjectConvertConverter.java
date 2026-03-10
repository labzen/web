package cn.labzen.web.file.internal.excel.convert;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import lombok.RequiredArgsConstructor;

@Deprecated
@RequiredArgsConstructor
public class ObjectConvertConverter<T> implements Converter<T> {

  private final Class<T> type;

  @Override
  public Class<?> supportJavaTypeKey() {
    return type;
  }

  @Override
  public CellDataTypeEnum supportExcelTypeKey() {
    return CellDataTypeEnum.STRING;
  }

  @Override
  public T convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                  GlobalConfiguration globalConfiguration) throws Exception {
    return Converter.super.convertToJavaData(cellData, contentProperty, globalConfiguration);
  }

  @Override
  public WriteCellData<?> convertToExcelData(T value, ExcelContentProperty contentProperty,
                                             GlobalConfiguration globalConfiguration) throws Exception {
    return Converter.super.convertToExcelData(value, contentProperty, globalConfiguration);
  }
}
