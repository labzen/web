package cn.labzen.web.meta;

import cn.labzen.meta.component.DeclaredComponent;

public class WebCoreMeta implements DeclaredComponent {

  @Override
  public String mark() {
    return "Labzen-Web-Core";
  }

  @Override
  public String packageBased() {
    return "cn.labzen.web.core";
  }

  @Override
  public String description() {
    return "对WEB层做的极简开发优化，防止该层代码掺杂业务逻辑代码，避免造成臃肿";
  }
}
