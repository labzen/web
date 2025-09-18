package cn.labzen.web.defination;

import cn.labzen.meta.Labzens;
import cn.labzen.web.meta.WebConfiguration;

public interface Constants {

  String REST_REQUEST_TIME = "labzen.runtime.web.request.time";
  String REST_REQUEST_TIME_MILLIS = "labzen.runtime.web.request.time.millis";
  String REST_EXECUTION_TIME = "labzen.runtime.web.execution.time";
  String LOGGER_SCENE_CONTROLLER = "Controller";
  String EXCEPTION_WAS_LOGGED_DURING_REQUEST = "labzen.request.exception.logged";
  String JUNIT_OUTPUT_DIR = "__WITH_JUNIT_OUTPUT_DIR__";

  int DEFAULT_PAGE_NUMBER = 1;
  int DEFAULT_PAGE_SIZE = Labzens.configurationWith(WebConfiguration.class).pageSize();
}
