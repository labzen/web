package cn.labzen.web.file.definition.bean;

import lombok.Data;

import java.util.List;

@Data
public class Schema {

  private String fileName;

  private List<Column> columns;
}
