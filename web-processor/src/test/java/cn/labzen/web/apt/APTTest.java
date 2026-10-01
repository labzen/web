package cn.labzen.web.apt;

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

import static cn.labzen.web.apt.definition.JUnitConstants.JUNIT_OUTPUT_DIR;
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
    // 创建 JavaFileObject
    JavaFileObject fileObject = readTextFile();

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

    // 验证元数据文件已生成
    boolean metaFound = compilation.generatedFiles().stream()
      .anyMatch(f -> f.getName().endsWith(".meta.json"));
    Assertions.assertTrue(metaFound, "应生成 META-INF/labzen/MenuController.meta.json 元数据文件");

    // 验证默认（FORM）模式下生成的实现类落型
    Path implPath = Paths.get("target",
        "generated-test-sources",
        "cn",
        "labzen",
        "web",
        "apt",
        "MenuControllerImpl.java");
    Assertions.assertTrue(Files.exists(implPath), "应生成 MenuControllerImpl.java");
    String impl = Files.readString(implPath);
    Assertions.assertTrue(impl.contains("@ModelAttribute MenuDto resource"), "FORM 模式下资源入参应落为 @ModelAttribute");
    Assertions.assertFalse(impl.contains("@RequestBody"), "FORM 模式下不应生成 @RequestBody");
    Assertions.assertFalse(impl.contains("== null ?"), "默认不应生成空请求体兜底表达式");

    // 打印生成的元数据文件内容
    compilation.generatedFiles().stream()
      .filter(f -> f.getName().endsWith(".meta.json"))
      .forEach(f -> {
        try {
          System.out.println("--- Generated: " + f.getName() + " ---");
          System.out.println(f.getCharContent(true));
        } catch (IOException e) {
          System.out.println("无法读取生成文件: " + e.getMessage());
        }
      });
  }

  private JavaFileObject readTextFile() throws IOException {
    // 读取 test-source/TestDto.java 内容
    URL resource = this.getClass().getClassLoader().getResource("MenuController.txt");
    Assertions.assertNotNull(resource);
    String resourcePath = resource.getPath();
    File file = new File(resourcePath);
    String sourceCode = Files.readString(file.toPath());

    // 创建 JavaFileObject
    return JavaFileObjects.forSourceString("cn.labzen.web.example.MenuController", sourceCode);
  }

  private JavaFileObject readJavaFile() throws IOException {
    // 读取 test-source/TestDto.java 内容
    File file = new File("src/test/java/cn/labzen/web/ap/MenuController.java");
    String sourceCode = Files.readString(file.toPath());

    // 创建 JavaFileObject
    return JavaFileObjects.forSourceString("cn.labzen.web.example.MenuController", sourceCode);
  }
}
