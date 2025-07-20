package cn.labzen.web.paging;

import cn.labzen.web.paging.internal.Paging;
import com.google.common.collect.Lists;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class PageableDTOTest {

  public static void main(String[] args) {
    Paging paging = new Paging(false, 2, 52, Lists.newArrayList());
    System.out.println(paging.isUnpaged());
    System.out.println(paging.pageNumber());
    System.out.println(paging.pageSize());

    TestDto dto = new TestDto();
    dto.setName("张三");
    dto.setAge(18);
    dto.setGender("<UNK>");

    System.out.println(dto.getName());
    System.out.println(dto.getAge());
    System.out.println(dto.getGender());

    System.out.println("-------------------");

    DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<TestDto> builder = new ByteBuddy().subclass(TestDto.class)
      .method(named("getName"))
      .intercept(MethodCall.call(() -> "dynamic name"))
      .method(named("getAge"))
      .intercept(MethodCall.call(() -> 99))
      .method(ElementMatchers.not(named("getName").or(named("getAge"))))
      .intercept(MethodDelegation.to(new DelegatingInterceptor<>(dto)));
    try (DynamicType.Unloaded<TestDto> loader = builder.make()) {
      TestDto proxied = loader.load(TestDto.class.getClassLoader()).getLoaded().getDeclaredConstructor().newInstance();
      System.out.println(proxied.getName());
      System.out.println(proxied.getAge());
      System.out.println(proxied.getGender());
    } catch (Exception e) {
      e.printStackTrace();
    }
//    TestDto proxied = (TestDto) Proxy.newProxyInstance(
//      TestDto.class.getClassLoader(),
//      new Class[]{TestDto.class}, (proxy, method, args1) -> method.invoke(dto, args1));

  }

  // 委托其他方法给原始实例
  public static class DelegatingInterceptor<T> {

    private final T target;

    public DelegatingInterceptor(T target) {
      this.target = target;
    }

    @RuntimeType
    public Object intercept(@This Object proxy, @Origin Method method, @AllArguments Object[] args) throws Exception {
      return method.invoke(target, args);
    }
  }
}
