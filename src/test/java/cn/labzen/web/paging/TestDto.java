package cn.labzen.web.paging;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TestDto implements Pageable {

  private String name;
  private int age;
  private String gender;

}
