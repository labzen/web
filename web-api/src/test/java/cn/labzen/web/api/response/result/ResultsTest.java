package cn.labzen.web.api.response.result;

import cn.labzen.web.api.definition.HttpStatusExt;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class ResultsTest {

  @Test
  void testSuccess() {
    Result result = Results.success();
    assertNotNull(result);
    assertEquals(200, result.code());
    assertNull(result.value());
    assertNull(result.message());
  }

  @Test
  void testFailure() {
    Result result = Results.failure();
    assertNotNull(result);
    assertEquals(500, result.code());
    assertNull(result.value());
    assertNull(result.message());
  }

  @Test
  void testFailureWithMessage() {
    String errorMessage = "Error occurred";
    Result result = Results.failure(errorMessage);
    assertNotNull(result);
    assertEquals(500, result.code());
    assertNull(result.value());
    assertEquals(errorMessage, result.message());
  }

  @Test
  void testStatusWithCode() {
    Result result = Results.status(404);
    assertNotNull(result);
    assertEquals(404, result.code());
    assertNull(result.value());
    assertNull(result.message());
  }

  @Test
  void testStatusWithHttpStatus() {
    Result result = Results.status(HttpStatus.NOT_FOUND);
    assertNotNull(result);
    assertEquals(404, result.code());
  }

  @Test
  void testStatusWithHttpStatusExt() {
    Result result = Results.status(HttpStatusExt.INVALID_PARAMETER);
    assertNotNull(result);
    assertEquals(482, result.code());
  }

  @Test
  void testWithValue() {
    String data = "test data";
    Result result = Results.with(data);
    assertNotNull(result);
    assertEquals(200, result.code());
    assertEquals(data, result.value());
  }

  @Test
  void testWithCodeAndValue() {
    String data = "test data";
    Result result = Results.with(201, data);
    assertNotNull(result);
    assertEquals(201, result.code());
    assertEquals(data, result.value());
  }

  @Test
  void testWithHttpStatusAndValue() {
    String data = "test data";
    Result result = Results.with(HttpStatus.CREATED, data);
    assertNotNull(result);
    assertEquals(201, result.code());
    assertEquals(data, result.value());
  }

  @Test
  void testWithHttpStatusExtAndValue() {
    String data = "test data";
    Result result = Results.with(HttpStatusExt.APPENDING, data);
    assertNotNull(result);
    assertEquals(280, result.code());
    assertEquals(data, result.value());
  }

  @Test
  void testWithCodeValueAndMessage() {
    String data = "test data";
    String message = "Operation successful";
    Result result = Results.with(200, data, message);
    assertNotNull(result);
    assertEquals(200, result.code());
    assertEquals(data, result.value());
    assertEquals(message, result.message());
  }

  @Test
  void testMessage() {
    String message = "Operation completed";
    Result result = Results.message(message);
    assertNotNull(result);
    assertEquals(200, result.code());
    assertNull(result.value());
    assertEquals(message, result.message());
  }

  @Test
  void testMessageWithCode() {
    String message = "Not found";
    Result result = Results.message(404, message);
    assertNotNull(result);
    assertEquals(404, result.code());
    assertNull(result.value());
    assertEquals(message, result.message());
  }

  @Test
  void testMessageWithHttpStatus() {
    String message = "Bad request";
    Result result = Results.message(HttpStatus.BAD_REQUEST, message);
    assertNotNull(result);
    assertEquals(400, result.code());
    assertEquals(message, result.message());
  }

  @Test
  void testMessageWithHttpStatusExt() {
    String message = "Invalid parameter";
    Result result = Results.message(HttpStatusExt.INVALID_PARAMETER, message);
    assertNotNull(result);
    assertEquals(482, result.code());
    assertEquals(message, result.message());
  }

  @Test
  void testFile() {
    File tempFile = new File("test.txt");
    Result result = Results.file(tempFile);
    assertNotNull(result);
    assertTrue(result instanceof FileResult);
    assertEquals("test.txt", ((FileResult) result).filename());
  }

  @Test
  void testFileWithCustomFilename() {
    File tempFile = new File("test.txt");
    String customName = "custom.txt";
    Result result = Results.file(tempFile, customName);
    assertNotNull(result);
    assertTrue(result instanceof FileResult);
    assertEquals(customName, ((FileResult) result).filename());
  }
}
