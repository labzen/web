package cn.labzen.web.api.response;

import cn.labzen.web.api.definition.HttpStatusExt;
import org.springframework.http.HttpStatus;

public final class Results {

  private Results() {
  }

  public static Result success() {
    return new Result(200, null, null);
  }

  public static Result failure() {
    return new Result(500, null, null);
  }

  public static Result failure(String message) {
    return new Result(500, null, message);
  }

  /* ======================== with status ======================== */

  public static Result status(int code) {
    return new Result(code, null, null);
  }

  public static Result status(HttpStatus status) {
    return new Result(status.value(), null, null);
  }

  public static Result status(HttpStatusExt status) {
    return new Result(status.code(), null, null);
  }

  /* ======================== with value ======================== */

  public static Result with(Object value) {
    return new Result(200, value, null);
  }

  public static Result with(int code, Object value) {
    return new Result(code, value, null);
  }

  public static Result with(HttpStatus status, Object value) {
    return new Result(status.value(), value, null);
  }

  public static Result with(HttpStatusExt status, Object value) {
    return new Result(status.code(), value, null);
  }

  public static Result with(int code, Object value, String message) {
    return new Result(code, value, message);
  }

  public static Result with(HttpStatus status, Object value, String message) {
    return new Result(status.value(), value, message);
  }

  public static Result with(HttpStatusExt status, Object value, String message) {
    return new Result(status.code(), value, message);
  }

  /* ======================== with message ======================== */

  public static Result message(String message) {
    return new Result(200, null, message);
  }

  public static Result message(int code, String message) {
    return new Result(code, null, message);
  }

  public static Result message(HttpStatus status, String message) {
    return new Result(status.value(), null, message);
  }

  public static Result message(HttpStatusExt status, String message) {
    return new Result(status.code(), null, message);
  }

//  /* ======================== with paging records ======================== */
//
//  public static Result asPaging(List<?> records, int pageNumber, int pageSize) {
//    return asPaging(records, pageNumber, pageSize, null);
//  }
//
//  public static Result asPaging(List<?> records, int pageNumber, int pageSize, Long totalOfRecords) {
//    long totalOfPages = 0;
//    if (totalOfRecords != null) {
//      totalOfPages = totalOfRecords / pageSize + ((totalOfRecords % pageSize == 0) ? 0 : 1);
//    }
//
//    Pagination<?> pagination = new DefaultPagination<>(true, pageNumber, pageSize, totalOfRecords == null ? 0 : totalOfRecords, totalOfPages, records);
//    return new Result(200, pagination, null);
//  }
}
