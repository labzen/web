package cn.labzen.web.file.internal.excel.convert;

import cn.labzen.web.file.internal.convert.AbstractStringConverter;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Deprecated
@RequiredArgsConstructor
public class StringConvertConverter implements Converter<String> {

  private final List<AbstractStringConverter> converters;

  @Override
  public Class<?> supportJavaTypeKey() {
    return String.class;
  }

  @Override
  public CellDataTypeEnum supportExcelTypeKey() {
    return CellDataTypeEnum.STRING;
  }

  @Override
  public String convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty,
                                  GlobalConfiguration globalConfiguration) {
    String resultValue = cellData.getStringValue();
    for (AbstractStringConverter converter : converters.reversed()) {
      Object restoreValue = converter.restore(resultValue);
      if (restoreValue == null) {
        break;
      }
      resultValue = restoreValue.toString();
    }
    return resultValue;
  }

  @Override
  public WriteCellData<?> convertToExcelData(String value, ExcelContentProperty contentProperty,
                                             GlobalConfiguration globalConfiguration) {
    String resultValue = value;

    for (AbstractStringConverter converter : converters) {
      resultValue = converter.convert(resultValue);
    }

    return new WriteCellData<>(resultValue);
  }
}
