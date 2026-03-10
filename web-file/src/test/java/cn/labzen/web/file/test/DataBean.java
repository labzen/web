package cn.labzen.web.file.test;

import cn.labzen.web.file.annotation.DataColumn;
import cn.labzen.web.file.annotation.DataStyle;
import cn.labzen.web.file.annotation.DataStyles;
import cn.labzen.web.file.annotation.WritableDataBean;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@WritableDataBean
@DataStyles(
  header = @DataStyle(backgroundColor = "#FFFAC5", fontColor = "#9C102A", fontSize = 13, fontBold = true),
  content = @DataStyle(backgroundColor = "#BBE1CE")
)
@Data
public class DataBean {

  @DataColumn(header = "编号", prefix = "NO. ")
  private Long id;
  @DataColumn(header = "姓名", width = 15, suffix = " 先生")
  private String name;
  @DataColumn(header = "年龄")
  private Integer age;
  @DataColumn(header = "是否毕业")
  private boolean graduated;
  @DataColumn(header = "生日", width = 50)
  private Date birthDate;
  @DataColumn(header = "成绩", index = 2)
  private Double score;
  @DataColumn(header = "身高")
  private Float height;
  @DataColumn(header = "创建时间", ignore = true)
  private LocalDateTime createTime;
}
