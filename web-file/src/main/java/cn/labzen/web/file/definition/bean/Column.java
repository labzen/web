package cn.labzen.web.file.definition.bean;

import lombok.Data;

@Data
public class Column {

  public static final String DEFAULT_COLUMN_NULLABLE_HINT = "___=== NEVER_NEVER_HIT_NULL ===___";
  public static final String DEFAULT_COLUMN_BLANKABLE_HINT = "___=== NEVER_NEVER_HIT_BLANK ===___";

  private Style styleHeader;
  private Style styleContent;

  private String[] header;
  private int index;
  private int width;
  private boolean ignore;
  private String datePattern;
  private String decimalsPattern;
  private Class<? extends Enum<?>> referEnum;
  private String referEnumMethod;
  private String referEquationString;

  private String fieldName;
  private Class<?> fieldType;

//  private NullableDataConverter nullableConverter;
//  private List<AbstractStringConverter> stringConverters;
}
