package cn.labzen.web.meta;

import cn.labzen.meta.component.DeclaredComponent;

public class WebMeta implements DeclaredComponent {
  @Override
  public String mark() {
    return "Labzen-Web";
  }

  @Override
  public String packageBased() {
    return "cn.labzen.web";
  }

  @Override
  public String description() {
    return "对WEB层做的极简开发优化，防止该层代码掺杂业务逻辑代码，避免造成臃肿";
  }
}
