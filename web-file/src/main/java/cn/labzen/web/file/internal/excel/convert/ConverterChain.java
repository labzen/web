package cn.labzen.web.file.internal.excel.convert;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Deprecated
@RequiredArgsConstructor
public class ConverterChain<T> implements Converter<T> {

  private final Class<T> type;
  private final List<Converter<T>> converters = Lists.newArrayList();

  public void addConverter(Converter<T> converter) {
    converters.add(converter);
  }

  @Override
  public Class<?> supportJavaTypeKey() {
    return type;
  }

  @Override
  public CellDataTypeEnum supportExcelTypeKey() {
    return CellDataTypeEnum.STRING;
  }

//  @Override
//  public T convertToJavaData(ReadCellData cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
//    ReadCellData leastData = cellData;
//    for (Converter<?> converter : converters) {
//      T o = converter.convertToJavaData(leastData, contentProperty, globalConfiguration);
//      leastData = new ReadCellData(o);
//    }
//  }
//
//  @Override
//  public WriteCellData<?> convertToExcelData(Object value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
//    return Converter.super.convertToExcelData(value, contentProperty, globalConfiguration);
//  }
}
