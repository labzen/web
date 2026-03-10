package cn.labzen.web.file.internal;

import cn.labzen.tool.structure.Pair;
import cn.labzen.tool.util.Strings;
import cn.labzen.web.file.annotation.DataColumn;
import cn.labzen.web.file.annotation.DataStyle;
import cn.labzen.web.file.annotation.DataStyles;
import cn.labzen.web.file.annotation.WritableDataBean;
import cn.labzen.web.file.definition.bean.Column;
import cn.labzen.web.file.definition.bean.Schema;
import cn.labzen.web.file.definition.bean.Style;
import cn.labzen.web.file.definition.enums.Alignment;
import cn.labzen.web.file.internal.convert.*;
import com.google.common.collect.Maps;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static cn.labzen.web.file.definition.bean.Column.DEFAULT_COLUMN_BLANKABLE_HINT;
import static cn.labzen.web.file.definition.bean.Column.DEFAULT_COLUMN_NULLABLE_HINT;

public class WritableDataBeanParser {

  private static final Map<Class<?>, Schema> BEAN_SCHEMA_CACHE = Maps.newHashMap();

  private final Class<?> clazz;

  private Schema schema;
  private Style globalHeaderStyle;
  private Style globalContentStyle;

  public WritableDataBeanParser(Class<?> clazz) {
    this.clazz = clazz;
  }

  public Schema parse() {
    if (BEAN_SCHEMA_CACHE.containsKey(clazz)) {
      return BEAN_SCHEMA_CACHE.get(clazz);
    }

    if (!clazz.isAnnotationPresent(WritableDataBean.class)) {
      return null;
    }

    readGlobalStyle();


    WritableDataBean annotation = clazz.getAnnotation(WritableDataBean.class);

    schema = new Schema();
    schema.setFileName(annotation.fileName());

    readFields();

    BEAN_SCHEMA_CACHE.put(clazz, schema);
    return schema;
  }

  private void readGlobalStyle() {
    if (clazz.isAnnotationPresent(DataStyles.class)) {
      DataStyles annotation = clazz.getAnnotation(DataStyles.class);
      this.globalHeaderStyle = convertStyle(annotation.header());
      this.globalContentStyle = convertStyle(annotation.content());
    } else {
      this.globalHeaderStyle = new Style();
      this.globalContentStyle = new Style();
    }
  }

  private Style convertStyle(DataStyle annotation) {
    Style style = new Style();
    Alignment[] alignArg = annotation.align();
    if (alignArg.length == 2) {
      style.setAlign(new Pair<>(alignArg[0], alignArg[1]));
    } else {
      style.setAlign(Style.DEFAULT_ALIGNS);
    }
    style.setBackgroundColor(annotation.backgroundColor());
    style.setFontSize(annotation.fontSize());
    style.setFontColor(annotation.fontColor());
    style.setFontBold(annotation.fontBold());
    style.setBorderWidth(annotation.borderWidth());
    style.setWrapped(annotation.wrapped());
    style.setHidden(annotation.hidden());
    return style;
  }

  private void readFields() {
    Field[] fields = clazz.getDeclaredFields();
    List<Column> columns = Arrays.stream(fields)
      .map(this::parseField)
      .filter(column -> !column.isIgnore())
      .collect(Collectors.toList());

    resortColumns(columns);
    schema.setColumns(columns);
  }

  private void resortColumns(List<Column> columns) {
    Set<Integer> assignedIndexes = columns.stream().map(Column::getIndex).collect(Collectors.toSet());
    assignedIndexes.remove(-1);
    int index = 0;
    for (Column column : columns) {
      for (; ; index++) {
        if (assignedIndexes.contains(column.getIndex())) {
          break;
        }
        if (!assignedIndexes.contains(index)) {
          column.setIndex(index);
          index++;
          break;
        }
      }
    }

    columns.sort(Comparator.comparingInt(Column::getIndex));
  }

  private Column parseField(Field field) {
    Column column = readColumn(field);

    Pair<Style, Style> fieldStyles = readFieldStyle(field);
    column.setStyleHeader(fieldStyles.first());
    column.setStyleContent(fieldStyles.second());

    return column;
  }

  private Column readColumn(Field field) {
    Column column = new Column();
    column.setFieldName(field.getName());
    column.setFieldType(field.getType());

    DataColumn annotation = field.getAnnotation(DataColumn.class);
    if (annotation == null) {
      column.setIgnore(true);
      return column;
    }

    String[] header = annotation.header();
    boolean ignored = annotation.ignore();
    if (header.length == 0 || Strings.isBlank(header[0]) || ignored) {
      column.setHeader(new String[]{field.getName()});
    } else {
      column.setHeader(header);
    }
    column.setIndex(annotation.index());
    column.setWidth(annotation.width());
    column.setIgnore(ignored);
    column.setDatePattern(annotation.datePattern());
    column.setDecimalsPattern(annotation.decimalsPattern());

    column.setReferEnum(annotation.referEnum());
    column.setReferEnumMethod(annotation.referEnumMethod());
    column.setReferEquationString(annotation.referEquationString());

//    parseFieldConverter(column, annotation);

    return column;
  }

//  private void parseFieldConverter(Column column, DataColumn annotation) {
//
//    if (!DEFAULT_COLUMN_NULLABLE_HINT.equals(annotation.defaultWhenNull())) {
//      column.setNullableConverter(new NullableDataConverter(column.getFieldType(), annotation.defaultWhenNull()));
//    }
//
//    List<AbstractStringConverter> stringConverters = new ArrayList<>();
//    if (!DEFAULT_COLUMN_BLANKABLE_HINT.equals(annotation.defaultWhenBlank())) {
//      stringConverters.add(new BlankableStringConverter(annotation.defaultWhenBlank()));
//    }
//    if (Strings.isNotBlank(annotation.prefix())) {
//      stringConverters.add(new StringPrefixConverter(annotation.prefix()));
//    }
//    if (Strings.isNotBlank(annotation.suffix())) {
//      stringConverters.add(new StringSuffixConverter(annotation.suffix()));
//    }
//    if (!stringConverters.isEmpty()) {
//      column.setStringConverters(stringConverters);
//    }
//  }

  private Pair<Style, Style> readFieldStyle(Field field) {
    if (field.isAnnotationPresent(DataStyles.class)) {
      DataStyles annotation = field.getAnnotation(DataStyles.class);
      Style headerStyle = globalHeaderStyle.merge(convertStyle(annotation.header()));
      Style contentStyle = globalContentStyle.merge(convertStyle(annotation.content()));
      return new Pair<>(headerStyle, contentStyle);
    }

    if (!field.isAnnotationPresent(DataStyle.class)) {
      return new Pair<>(globalHeaderStyle, globalContentStyle);
    }

    DataStyle annotation = field.getAnnotation(DataStyle.class);
    Style contentStyle = globalHeaderStyle.merge(convertStyle(annotation));
    return new Pair<>(globalHeaderStyle, contentStyle);
  }
}
