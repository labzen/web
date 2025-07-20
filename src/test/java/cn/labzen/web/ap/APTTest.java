package cn.labzen.web.ap;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static cn.labzen.web.ConstKt.JUNIT_OUTPUT_DIR;
import static com.google.testing.compile.Compiler.javac;

public class APTTest {

  @BeforeAll
  static void setup() throws IOException {
    System.setProperty(JUNIT_OUTPUT_DIR, "generated-test-sources");
  }

  @AfterAll
  static void teardown() throws IOException {
    System.clearProperty(JUNIT_OUTPUT_DIR);
  }

  @Test
  public void test() throws IOException {
    // 读取 test-source/TestDto.java 内容
    File file = new File("src/test/java/cn/labzen/web/ap/MenuController.java");
    String sourceCode = Files.readString(file.toPath());

    // 创建 JavaFileObject
    JavaFileObject fileObject = JavaFileObjects.forSourceString("cn.labzen.web.example.MenuController", sourceCode);

    // 执行编译 + 注解处理器
    Compilation compilation = javac()
      .withProcessors(new LabzenWebProcessor())
      .compile(fileObject);

    // 打印编译日志
    List<Diagnostic<? extends JavaFileObject>> messages = compilation.diagnostics();
    for (Diagnostic<? extends JavaFileObject> msg : messages) {
      System.out.println(msg);
    }

    // 断言编译成功
    Assertions.assertEquals(Compilation.Status.SUCCESS, compilation.status());
  }
}
