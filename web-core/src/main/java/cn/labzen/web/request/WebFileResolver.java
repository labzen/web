//package cn.labzen.web.request;
//
//import cn.labzen.meta.Labzens;
//import cn.labzen.tool.util.DateTimes;
//import cn.labzen.tool.util.Strings;
//import cn.labzen.web.api.definition.UploadedFile;
//import cn.labzen.web.exception.WebFileException;
//import cn.labzen.web.meta.WebCoreConfiguration;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.nio.file.Paths;
//import java.util.List;
//
//public final class WebFileResolver {
//
//  //  private static final WebCoreConfiguration CONFIGURATION;
//  private static final List<String> acceptedUploadFileExtensions;
//
//  static {
//    WebCoreConfiguration configuration = Labzens.configurationWith(WebCoreConfiguration.class);
//    acceptedUploadFileExtensions = configuration.acceptedUploadFileExtensions();
//  }
//
//  public static UploadedFile resolve(MultipartFile file) {
//    if (file == null || file.isEmpty()) {
//      throw new WebFileException(500, "文件不能为空");
//    }
//    String originalFilename = file.getOriginalFilename();
//    if (originalFilename == null || originalFilename.trim().isEmpty()) {
//      throw new IllegalArgumentException("文件名不能为空");
//    }
//    if (Strings.containsAny(originalFilename, "..", "/", "\\")) {
//      throw new IllegalArgumentException("文件名包含非法字符");
//    }
//    String extension = Strings.lastUntil(originalFilename, ".", false);
//    if (originalFilename.equals(extension)) {
//      extension = "";
//    }
//    if (extension.isEmpty()) {
//      throw new IllegalArgumentException("文件缺少扩展名");
//    }
//    if (!acceptedUploadFileExtensions.contains(extension)) {
//      throw new IllegalArgumentException("不支持的文件类型: " + extension);
//    }
//
////    String contentType = file.getContentType();
////    String safeName = DateTimes.formattedNow("yyyyMMddHHmmss_") + clearFilename(originalFilename);
//    // TODO 可进一步扩展通过验证文件内容头部信息，做更严格的格式验证
//
//    return new UploadedFile(file,originalFilename, extension);
//  }
//
//  private static String clearFilename(String originalFilename) {
//    String filename = Strings.value(originalFilename, "unknown");
//    // 只保留文件名，干掉路径；再做字符白名单/替换
//    String name = Paths.get(filename).getFileName().toString();
//    return name.replaceAll("[\\\\/\\r\\n\\t]", "_");
//  }
//}
