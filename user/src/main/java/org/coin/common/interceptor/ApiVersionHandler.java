package org.coin.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.coin.common.request.ApiVersion;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API Versioning
 */
public class ApiVersionHandler implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            ApiVersion apiVersion = handlerMethod.getMethodAnnotation(ApiVersion.class);
            if (apiVersion != null) {
                String version = apiVersion.value();
                String versionHeader = request.getHeader("API-Version");

                if(versionHeader != null && versionHeader.equals(version)){
                    return true;
                }
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid API version");
                return false;
            }
        }
        return true;
    }
}