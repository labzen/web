package cn.labzen.web.log.bean;

import cn.labzen.web.api.log.config.ApiEndpointLogConfig;

import java.util.List;

public record ApiEndpointDetail(String httpMethod, String urlPattern, String methodName, String controllerName,
                                List<String> parameterTypes, ApiEndpointLogConfig logConfig) {

}
