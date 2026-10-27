/*
 * Test harness (T13): a mock-free, in-memory {@code S3CallContext} for the signature
 * unit tests. The servlet request/response are plain JDK dynamic proxies backed by
 * simple maps and byte arrays — no mocking framework involved.
 */

package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.entities.api.S3Bucket;
import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.entities.api.S3ListBucketResult;
import de.mc.ladon.s3server.entities.api.S3Object;
import de.mc.ladon.s3server.entities.api.S3User;
import de.mc.ladon.s3server.entities.impl.S3CallContextImpl;
import de.mc.ladon.s3server.entities.impl.S3UserImpl;
import de.mc.ladon.s3server.repository.api.S3Repository;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * In-memory, mock-free call contexts and repositories for auth tests.
 */
public final class TestSignContexts {

    private TestSignContexts() {
    }

    /**
     * Builds a standard {@link S3CallContextImpl} (the implementation the server uses,
     * required because {@code Authorization} sets the user on it explicitly) backed by
     * an in-memory fake servlet request.
     *
     * @param method      HTTP method
     * @param uri         request path (as {@code request.getRequestURI()} reports it, i.e. percent-encoded, no query)
     * @param queryString raw query string without the leading '?', or null when absent
     * @param headers     request headers (keys may be in any case)
     * @param params      query parameters as delivered by the servlet container (decoded values)
     * @param content     request body, may be null
     */
    public static S3CallContext newContext(String method, String uri, String queryString,
                                           Map<String, String> headers,
                                           Map<String, String[]> params,
                                           byte[] content) {
        Map<String, String[]> safeParams = params == null ? Map.of() : params;
        HttpServletRequest request = fakeRequest(method, uri, queryString, headers, safeParams, content);
        HttpServletResponse response = fakeResponse();
        return new S3CallContextImpl(request, response, safeParams);
    }

    // ------------------------------------------------------------------
    // fake servlet request / response (JDK proxies, no mocking framework)
    // ------------------------------------------------------------------

    private static HttpServletRequest fakeRequest(String method, String uri, String queryString,
                                                   Map<String, String> headers,
                                                   Map<String, String[]> params,
                                                   byte[] content) {
        Map<String, String> lowerCasedHeaders = new LinkedHashMap<>();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                lowerCasedHeaders.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }
        InvocationHandler handler = (proxy, invoked, args) -> {
            String name = invoked.getName();
            if ("getMethod".equals(name)) {
                return method;
            }
            if ("getRequestURI".equals(name)) {
                return uri;
            }
            if ("getRequestURL".equals(name)) {
                return "http://example.amazonaws.com" + (uri == null ? "/" : uri);
            }
            if ("getQueryString".equals(name)) {
                return queryString;
            }
            if ("getHeader".equals(name)) {
                if (args == null || args.length == 0) {
                    return null;
                }
                return lowerCasedHeaders.get(String.valueOf(args[0]).toLowerCase(Locale.ROOT));
            }
            if ("getHeaderNames".equals(name)) {
                return Collections.enumeration(lowerCasedHeaders.keySet());
            }
            if ("getParameter".equals(name)) {
                if (args == null || args.length == 0) {
                    return null;
                }
                return firstOf(params.get(String.valueOf(args[0])));
            }
            if ("getParameterMap".equals(name)) {
                return params;
            }
            if ("getInputStream".equals(name)) {
                return new ByteArrayInputStream(content == null ? new byte[0] : content);
            }
            if ("toString".equals(name)) {
                return "FakeHttpServletRequest<" + method + " " + uri + ">";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            return defaultValue(invoked.getReturnType());
        };
        return (HttpServletRequest) Proxy.newProxyInstance(
                TestSignContexts.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                handler);
    }

    private static HttpServletResponse fakeResponse() {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        ServletOutputStream out = new ServletOutputStream() {
            @Override
            public void write(int b) {
                sink.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
                throw new UnsupportedOperationException("not used in tests");
            }
        };
        InvocationHandler handler = (proxy, invoked, args) -> {
            String name = invoked.getName();
            if ("getOutputStream".equals(name)) {
                return out;
            }
            if ("toString".equals(name)) {
                return "FakeHttpServletResponse";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            return defaultValue(invoked.getReturnType());
        };
        return (HttpServletResponse) Proxy.newProxyInstance(
                TestSignContexts.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                handler);
    }

    private static String firstOf(String[] values) {
        return values != null && values.length > 0 ? values[0] : null;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        String name = type.getName();
        if ("boolean".equals(name)) {
            return false;
        }
        if ("byte".equals(name)) {
            return (byte) 0;
        }
        if ("char".equals(name)) {
            return (char) 0;
        }
        if ("short".equals(name)) {
            return (short) 0;
        }
        if ("int".equals(name)) {
            return 0;
        }
        if ("long".equals(name)) {
            return 0L;
        }
        if ("float".equals(name)) {
            return 0f;
        }
        if ("double".equals(name)) {
            return 0d;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // S3User / S3Repository
    // ------------------------------------------------------------------

    /**
     * In-memory S3 user. The access key id is stored as the "public key", mirroring
     * the FS repository layout.
     */
    public static S3User user(String accessKeyId, String secretKey) {
        return new S3UserImpl("test", "test", accessKeyId, secretKey, Set.of());
    }

    /**
     * In-memory repository exposing only {@code getUser(...)}; every other operation
     * is unsupported (the auth tests must not touch them).
     */
    public static S3Repository repository(S3User... users) {
        Map<String, S3User> byAccessKey = new HashMap<>();
        for (S3User user : users) {
            byAccessKey.put(user.getPublicKey(), user);
        }
        return new S3Repository() {
            @Override
            public S3User getUser(S3CallContext callContext, String accessKey) {
                return byAccessKey.get(accessKey);
            }

            @Override
            public List<S3Bucket> listAllBuckets(S3CallContext callContext) {
                throw notUsed();
            }

            @Override
            public void createBucket(S3CallContext callContext, String bucketName, String locationConstraint) {
                throw notUsed();
            }

            @Override
            public void deleteBucket(S3CallContext callContext, String bucketName) {
                throw notUsed();
            }

            @Override
            public void createObject(S3CallContext callContext, String bucketName, String objectKey) {
                throw notUsed();
            }

            @Override
            public S3Object copyObject(S3CallContext callContext, String srcBucket, String srcObjectKey,
                                       String destBucket, String destObjectKey, boolean copyMetadata) {
                throw notUsed();
            }

            @Override
            public void getObject(S3CallContext callContext, String bucketName, String objectKey, boolean head) {
                throw notUsed();
            }

            @Override
            public S3ListBucketResult listBucket(S3CallContext callContext, String bucketName) {
                throw notUsed();
            }

            @Override
            public void deleteObject(S3CallContext callContext, String bucketName, String objectKey) {
                throw notUsed();
            }

            @Override
            public void getBucket(S3CallContext callContext, String bucketName) {
                throw notUsed();
            }
        };
    }

    private static UnsupportedOperationException notUsed() {
        return new UnsupportedOperationException("not needed in this test");
    }
}
