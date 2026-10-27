/*
 * Test harness for the repository unit tests: a plain in-memory
 * {@code S3CallContext} — no mocking framework involved. Request headers and
 * parameters are plain maps; response headers and the written content are
 * captured into the context object.
 */
package de.mc.ladon.s3server.repository.vfs;

import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.entities.api.S3RequestHeader;
import de.mc.ladon.s3server.entities.api.S3RequestParams;
import de.mc.ladon.s3server.entities.api.S3ResponseHeader;
import de.mc.ladon.s3server.entities.api.S3User;
import de.mc.ladon.s3server.entities.impl.S3UserImpl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

final class TestContexts {

    static final String ACCESS_KEY = "rHUYeAk58Ilhg6iUEFtr";
    static final String SECRET_KEY = "IVimdW7BIQLq9PLyVpXzZUq8zS4nLfrsoiZSJanu";

    private TestContexts() {
    }

    static S3User user(String accessKey, String secretKey) {
        return new S3UserImpl("user-1", "Test User", accessKey, secretKey, Collections.emptySet());
    }

    /**
     * A captured call context: request headers, request parameters, the request
     * body — and the repository's response (captured headers + written bytes).
     */
    static final class Context {
        final S3RequestHeader requestHeader;
        final S3RequestParams requestParams;
        final byte[] requestBody;
        final S3CallContext callContext;

        S3ResponseHeader responseHeader;
        byte[] writtenContent;

        Context(String method, Map<String, String> headers, Map<String, String> params, byte[] body) {
            this.requestHeader = headerProxy(headers);
            this.requestParams = paramsProxy(params);
            this.requestBody = body == null ? new byte[0] : body.clone();
            final String m = method;
            this.callContext = new S3CallContext() {
                private final S3User theUser = user(ACCESS_KEY, SECRET_KEY);
                private final String theRequestId = UUID.randomUUID().toString();

                @Override
                public void setContent(InputStream inputStream) {
                    try {
                        writtenContent = readAll(inputStream);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }

                @Override
                public S3User getUser() {
                    return theUser;
                }

                @Override
                public S3RequestHeader getHeader() {
                    return Context.this.requestHeader;
                }

                @Override
                public S3RequestParams getParams() {
                    return Context.this.requestParams;
                }

                @Override
                public de.mc.ladon.s3server.entities.api.S3RequestId getRequestId() {
                    return () -> theRequestId;
                }

                @Override
                public String getMethod() {
                    return m;
                }

                @Override
                public String getUri() {
                    return "/fake-uri";
                }

                @Override
                public String getQueryString() {
                    return null;
                }

                @Override
                public HttpServletRequest getRawRequest() {
                    return null;
                }

                @Override
                public InputStream getContent() {
                    return new ByteArrayInputStream(Context.this.requestBody);
                }

                @Override
                public void setResponseHeader(S3ResponseHeader responseHeader) {
                    Context.this.responseHeader = responseHeader;
                }
            };
        }

        static byte[] readAll(InputStream in) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static S3RequestHeader headerProxy(Map<String, String> headers) {
        return (S3RequestHeader) Proxy.newProxyInstance(
                TestContexts.class.getClassLoader(),
                new Class<?>[]{S3RequestHeader.class},
                new HeaderHandler(headers));
    }

    private static S3RequestParams paramsProxy(Map<String, String> params) {
        return (S3RequestParams) Proxy.newProxyInstance(
                TestContexts.class.getClassLoader(),
                new Class<?>[]{S3RequestParams.class},
                new ParamsHandler(params));
    }

    private static String firstOf(Map<String, String> map, String name) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static final class HeaderHandler implements InvocationHandler {
        private final Map<String, String> headers;

        HeaderHandler(Map<String, String> headers) {
            this.headers = headers == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<>(headers);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            String name = method.getName();
            if ("getFullHeader".equals(name)) {
                return new LinkedHashMap<>(headers);
            }
            if ("copyMetadata".equals(name)) {
                return "replace".equalsIgnoreCase(firstOf(headers, "x-amz-metadata-directive"));
            }
            if ("toString".equals(name)) {
                return "header-fake" + headers;
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(this);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            String mapped;
            if ("getContentType".equals(name)) {
                mapped = "content-type";
            } else if ("getContentMD5".equals(name)) {
                mapped = "content-md5";
            } else if ("getContentLength".equals(name)) {
                mapped = "content-length";
            } else if ("getXamzContentSha256".equals(name)) {
                mapped = "x-amz-content-sha256";
            } else if ("getXamzDate".equals(name)) {
                mapped = "x-amz-date";
            } else if (name.startsWith("get")) {
                mapped = name.substring(3);
            } else {
                mapped = name;
            }
            String value = firstOf(headers, mapped);
            if (value == null) {
                // object-typed getters must report "header absent" as null, not 0
                return defaultValue(method.getReturnType());
            }
            return "Long".equals(method.getReturnType().getSimpleName()) ? Long.valueOf(value) : value;
        }
    }

    private static final class ParamsHandler implements InvocationHandler {
        private final Map<String, String> params;

        ParamsHandler(Map<String, String> params) {
            this.params = params == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<>(params);
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
            String name = method.getName();
            switch (name) {
                case "getAllParams": {
                    TreeMap<String, String> sorted = new TreeMap<String, String>(Comparator.naturalOrder());
                    sorted.putAll(params);
                    return sorted;
                }
                case "getPrefix":
                    return firstOf(params, "prefix");
                case "getMarker":
                    return firstOf(params, "marker") != null ? firstOf(params, "marker") : firstOf(params, "continuation-token");
                case "getKeyMarker":
                    return firstOf(params, "key-marker");
                case "getVersionIdMarker":
                    return firstOf(params, "version-id-marker");
                case "getDelimiter":
                    return firstOf(params, "delimiter");
                case "getEncodingType":
                    return firstOf(params, "encoding-type");
                case "getMaxKeys": {
                    String v = firstOf(params, "max-keys");
                    try {
                        int max = Integer.parseInt(v);
                        return max > 1000 ? 1000 : max;
                    } catch (NumberFormatException e) {
                        return 1000;
                    }
                }
                case "listVersions":
                    return contains(params, "versions");
                case "acl":
                    return contains(params, "acl");
                case "toString":
                    return "params-fake" + params;
                case "hashCode":
                    return System.identityHashCode(this);
                case "equals":
                    return proxy == args[0];
                default:
                    return defaultValue(method.getReturnType());
            }
        }

        private static boolean contains(Map<String, String> params, String name) {
            for (String key : params.keySet()) {
                if (key.equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.FALSE;
        }
        if (type == int.class || type == Integer.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        // all object types (Long, String, ...): absence = null
        return null;
    }
}
