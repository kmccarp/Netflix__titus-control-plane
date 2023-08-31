/*
 * Copyright 2020 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.titus.runtime.endpoint.rest;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.netflix.titus.common.util.jackson.CommonObjectMappers;
import org.springframework.web.context.request.WebRequest;

/**
 * Error representation returned as JSON document for failed REST requests.
 */
public final class ErrorResponse {

    /**
     * If 'debug' parameter is included in query, include error context.
     */
    public static final String DEBUG_PARAM = "debug";

    public static final String CLIENT_REQUEST = "clientRequest";
    public static final String THREAD_CONTEXT = "threadContext";
    public static final String SERVER_CONTEXT = "serverContext";
    public static final String EXCEPTION_CONTEXT = "exception";

    @JsonView(CommonObjectMappers.PublicView.class)
    private final int statusCode;

    @JsonView(CommonObjectMappers.PublicView.class)
    private final String message;

    @JsonView(CommonObjectMappers.PublicView.class)
    private final Object errorDetails;

    @JsonView(CommonObjectMappers.DebugView.class)
    private final Map<String, Object> errorContext;

    @JsonCreator
    private ErrorResponse(@JsonProperty("statusCode") int statusCode,
                          @JsonProperty("message") String message,
                          @JsonProperty("errorDetails") Object errorDetails,
                          @JsonProperty("errorContext") Map<String, Object> errorContext) {
        this.statusCode = statusCode;
        this.message = message;
        this.errorDetails = errorDetails;
        this.errorContext = errorContext;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public Object getErrorDetails() {
        return errorDetails;
    }

    /**
     * Arbitrary additional information that can be attached to an error. The only requirement is that
     * it must be serializable by Jackson.
     */
    public Map<String, Object> getErrorContext() {
        return errorContext;
    }

    public static ErrorResponseBuilder newError(int statusCode) {
        return new ErrorResponseBuilder().status(statusCode);
    }

    public static ErrorResponseBuilder newError(int statusCode, String message) {
        return new ErrorResponseBuilder().status(statusCode).message(message);
    }

    public static class ErrorResponseBuilder {
        private int statusCode;
        private String message;
        private final Map<String, Object> errorContext = new TreeMap<>();
        private Object errorDetails;
        private boolean debug;

        public ErrorResponseBuilder status(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public ErrorResponseBuilder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseBuilder errorDetails(Object errorDetails) {
            this.errorDetails = errorDetails;
            return this;
        }

        public ErrorResponseBuilder clientRequest(HttpServletRequest httpRequest) {
            setDebugFromRequest(httpRequest.getParameterMap());
            return withContext(CLIENT_REQUEST, ErrorResponses.buildHttpRequestContext(httpRequest));
        }

        public ErrorResponseBuilder clientRequest(WebRequest webRequest) {
            setDebugFromRequest(webRequest.getParameterMap());
            return withContext(CLIENT_REQUEST, ErrorResponses.buildWebRequestContext(webRequest));
        }

        public ErrorResponseBuilder threadContext() {
            return withContext(THREAD_CONTEXT, ErrorResponses.buildThreadContext());
        }

        public ErrorResponseBuilder serverContext() {
            return withContext(SERVER_CONTEXT, ErrorResponses.buildServerContext());
        }

        public ErrorResponseBuilder exceptionContext(Throwable cause) {
            return withContext(EXCEPTION_CONTEXT, ErrorResponses.buildExceptionContext(cause));
        }

        public ErrorResponseBuilder withContext(String name, Object details) {
            if (details == null) {
                errorContext.remove(name);
            } else {
                errorContext.put(name, details);
            }
            return this;
        }

        public ErrorResponse build() {
            if (debug) {
                return new ErrorResponse(
                        statusCode,
                        message,
                        errorDetails,
                        errorContext.isEmpty() ? null : Collections.unmodifiableMap(errorContext)
                );

            }
            return new ErrorResponse(
                    statusCode,
                    message,
                    null,
                    null
            );
        }

        private void setDebugFromRequest(Map<String, String[]> parameters) {
            if (parameters == null) {
                this.debug = false;
            } else {
                String[] debugParamValue = parameters.get(DEBUG_PARAM);
                this.debug = debugParamValue != null && debugParamValue.length > 0 && Boolean.parseBoolean(debugParamValue[0]);
            }
        }
    }
}
