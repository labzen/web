package cn.labzen.web.api.definition;

import java.nio.file.Path;public interface UploadedFile {

  String contentType();

  long size();

  String originalFilename();

  String extension();

  void store(String path);

  void store(Path path);
}
