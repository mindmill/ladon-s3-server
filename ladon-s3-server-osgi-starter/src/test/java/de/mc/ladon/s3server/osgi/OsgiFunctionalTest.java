package de.mc.ladon.s3server.osgi;

import de.mc.ladon.s3server.auth.SigV4;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.jar.Manifest;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * OSGi runtime test for the Ladon S3 Server bundles.
 *
 * <p>This test boots a real <b>Apache Felix</b> OSGi framework inside the JVM and
 * installs, resolves and starts the three Ladon bundles (core, fs, osgi-starter,
 * taken from the sibling {@code target/} folders). It then exercises the real code
 * paths <em>inside the OSGi world</em>:</p>
 *
 * <ul>
 *     <li>all bundles resolve, i.e. every {@code Import-Package} is satisfiable
 *         (a missing or misnamed import package fails the test hard);</li>
 *     <li>both bundle activators run — the fs bundle registers an
 *         {@code S3Repository} OSGi service and the starter bundle picks it up and
 *         registers the whiteboard servlet (exactly what the Karaf/Felix host does);</li>
 *     <li>{@code FSRepository} performs a full bucket/object round-trip (JAXB
 *         marshal/unmarshal of the meta data, slf4j logging, content streaming)
 *         inside the bundle classloaders;</li>
 *     <li>a SigV4 authorization request is verified by the bundle code
 *         (canonical request, HMAC-SHA256 chain, constant-time comparison),
 *         including the negative (tampered signature) path.</li>
 * </ul>
 *
 * <p>The API jars (servlet API, slf4j, JAXB API and the JAXB runtime) are not
 * installed as bundles but provided to the framework as <em>system packages</em>
 * from the test classpath (see {@code org.osgi.framework.system.packages.extended}).
 * This mirrors a real OSGi runtime where the host container ships the API bundles
 * and keeps the installed bundle set to exactly the bundles of this project.</p>
 *
 * <p>Requires the sibling modules to be built (run from the root reactor or
 * {@code mvn -pl ladon-s3-server-osgi-starter -am verify}).</p>
 */
@Timeout(90)
class OsgiFunctionalTest {

    /** The default user that {@code FSRepository.loadUserFile} writes on first use. */
    private static final String ACCESS_KEY = "rHUYeAk58Ilhg6iUEFtr";
    private static final String SECRET_KEY = "IVimdW7BIQLq9PLyVpXzZUq8zS4nLfrsoiZSJanu";
    private static final String REGION = "us-east-1";
    private static final String SERVICE = "s3";

    private static Framework framework;
    private static Bundle coreBundle;
    private static Bundle fsBundle;
    private static Bundle starterBundle;

    @TempDir
    static Path felixStorage;

    @TempDir
    static Path s3data;

    @BeforeAll
    static void bootFramework() throws Exception {
        // The API packages (servlet API, slf4j, JAXB API, JAXB runtime) are provided as
        // shared <em>system packages</em> from the test classpath. The Ladon bundles keep
        // their imports but with the version ranges stripped (see bundleRelaxed), because
        // unversioned system packages only satisfy unversioned requirements.
        //
        // This mirrors the OSGi host shipping the API bundles AND keeps both worlds on a
        // single class identity: the JAXB ContextFactory (found through the thread
        // context class loader) and the jakarta.xml.bind interface classes must come
        // from the same world, otherwise the provider would not be a subtype of the
        // service interface.
        File core = moduleJar("ladon-s3-server", "ladon-s3-server");
        File fs = moduleJar("ladon-s3-server-fs", "ladon-s3-server-fs");
        File starter = moduleJar("ladon-s3-server-osgi-starter", "ladon-s3-server-osgi-starter");

        Map<String, String> config = new Hashtable<>();
        config.put(Constants.FRAMEWORK_STORAGE, felixStorage.resolve("felix").toString());
        config.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        config.put("org.osgi.framework.system.packages.extra", String.join(",",
                "org.slf4j", "org.slf4j.event", "org.slf4j.helpers", "org.slf4j.spi",
                "jakarta.xml.bind", "jakarta.xml.bind.annotation",
                "jakarta.xml.bind.annotation.adapters", "jakarta.xml.bind.attachment",
                "jakarta.xml.bind.helpers", "jakarta.xml.bind.util",
                "jakarta.servlet", "jakarta.servlet.annotation",
                "jakarta.servlet.descriptor", "jakarta.servlet.http",
                "jakarta.servlet.resources"));

        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).stream().findFirst()
                .orElseThrow(() -> new AssertionError("no OSGi FrameworkFactory on the test classpath"))
                .get();
        framework = factory.newFramework(config);
        framework.start();
        try {
            BundleContext systemContext = framework.getBundleContext();
            Path workDir = felixStorage.resolve("relaxed");
            Files.createDirectories(workDir);

            coreBundle = systemContext.installBundle(bundleRelaxed(core, workDir).toURI().toString());
            fsBundle = systemContext.installBundle(bundleRelaxed(fs, workDir).toURI().toString());
            starterBundle = systemContext.installBundle(bundleRelaxed(starter, workDir).toURI().toString());

            coreBundle.start();
            fsBundle.start();
            starterBundle.start();
        } catch (BundleException e) {
            dumpFrameworkState();
            throw e;
        }
    }

    /**
     * Rewraps a Ladon bundle with a <em>relaxed</em> manifest for the tiny test host:
     *
     * <ul>
     *     <li>{@code Import-Package} version ranges are stripped, since the API packages
     *         are provided as (unversioned) system packages and unversioned system
     *         packages only satisfy unversioned requirements — the package wiring
     *         itself stays intact and is still enforced;</li>
     *     <li>mandatory {@code Require-Capability} requirements (extenders etc.) are
     *         removed, since a bare test host has none of them.</li>
     * </ul>
     */
    private static File bundleRelaxed(File bundleJar, Path workDir) throws java.io.IOException {
        Path out = workDir.resolve("relaxed-" + bundleJar.getName());
        byte[] buffer = new byte[8 * 1024];
        try (java.util.jar.JarInputStream in = new java.util.jar.JarInputStream(Files.newInputStream(bundleJar.toPath()));
             java.util.zip.ZipOutputStream zout = new java.util.zip.ZipOutputStream(Files.newOutputStream(out))) {

            java.util.jar.Attributes original = in.getManifest().getMainAttributes();
            Manifest cleaned = new Manifest();
            for (Object nameObj : original.keySet()) {
                java.util.jar.Attributes.Name name = (java.util.jar.Attributes.Name) nameObj;
                String normalizedName = name.toString();
                if ("Require-Capability".equals(normalizedName)) {
                    continue;
                }
                if ("Import-Package".equals(normalizedName)) {
                    String importPackage = original.getValue(name);
                    if (importPackage != null) {
                        cleaned.getMainAttributes().putValue(normalizedName,
                                importPackage.replaceAll(";version=\"[^\"]*\"", ""));
                        continue;
                    }
                }
                cleaned.getMainAttributes().putValue(normalizedName, original.getValue(name));
            }
            zout.putNextEntry(new java.util.zip.ZipEntry("META-INF/MANIFEST.MF"));
            cleaned.write(zout);
            zout.closeEntry();

            java.util.zip.ZipEntry entry;
            while ((entry = in.getNextJarEntry()) != null) {
                String name = entry.getName();
                if ("META-INF/MANIFEST.MF".equals(name)) {
                    in.closeEntry();
                    continue;
                }
                zout.putNextEntry(new java.util.zip.ZipEntry(name));
                if (!entry.isDirectory()) {
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        zout.write(buffer, 0, read);
                    }
                }
                zout.closeEntry();
            }
        }
        return out.toFile();
    }

    /** Locates the jar of a class that is on the test classpath. */
    private static File jarOf(Class<?> cls) {
        try {
            java.net.URL location = cls.getProtectionDomain().getCodeSource().getLocation();
            return new java.io.File(location.toURI());
        } catch (Exception e) {
            fail("cannot locate the jar containing " + cls.getName() + " on the test classpath: " + e);
            return null;
        }
    }

    @AfterAll
    static void stopFramework() {
        if (framework != null) {
            try {
                framework.stop();
                framework.waitForStop(Long.MAX_VALUE);
            } catch (Exception e) {
                System.err.println("[osgi-test] stopping the framework failed: " + e);
            }
        }
    }

    private static void dumpFrameworkState() {
        BundleContext systemContext = framework.getBundleContext();
        for (Bundle b : systemContext.getBundles()) {
            System.err.println("[osgi-test] bundle " + b.getLocation() + " state=" + stateName(b.getState()));
        }
    }

    private static String stateName(int state) {
        if (state == Bundle.ACTIVE) return "ACTIVE";
        if (state == Bundle.RESOLVED) return "RESOLVED";
        if (state == Bundle.INSTALLED) return "INSTALLED";
        if (state == Bundle.STARTING) return "STARTING";
        if (state == Bundle.STOPPING) return "STOPPING";
        return Integer.toString(state);
    }

    /**
     * Resolves a bundle jar from a sibling module's target folder. The reactor
     * build order guarantees that the module was built before this one.
     */
    private static File moduleJar(String module, String artifact) throws java.io.IOException {
        Path target = Path.of("..", module, "target");
        if (!Files.isDirectory(target)) {
            fail("sibling module folder not found (build the reactor first): " + target.toAbsolutePath());
        }
        List<File> jars = Files.list(target)
                .map(Path::toFile)
                .filter(p -> {
                    String name = p.getName();
                    return name.startsWith(artifact + "-")
                            && name.endsWith(".jar")
                            && !name.endsWith("-sources.jar")
                            && !name.endsWith("-javadoc.jar");
                })
                .collect(Collectors.toList());
        if (jars.isEmpty()) {
            fail("no bundle jar of " + artifact + " found in " + target.toAbsolutePath());
        }
        return jars.get(0);
    }

    // ---------------------------------------------------------------------------
    // 1. resolution, activation, service wiring
    // ---------------------------------------------------------------------------

    @Test
    void bundlesAreResolvedAndActive() {
        assertEquals(Bundle.ACTIVE, coreBundle.getState(), "core bundle must be active");
        assertEquals(Bundle.ACTIVE, fsBundle.getState(), "fs bundle must be active");
        assertEquals(Bundle.ACTIVE, starterBundle.getState(), "starter bundle must be active");
    }

    @Test
    void systemPackagesAreSharedAndAllKeyClassesLoad() throws Exception {
        // The core bundle must see the *same* servlet API classes as this test:
        // without a shared system package the repository code was broken before
        // any line even runs (ClassCastException at the bundle boundary).
        Class<?> servletClass = coreBundle.loadClass("de.mc.ladon.s3server.servlet.S3Servlet");
        assertEquals(jakarta.servlet.http.HttpServlet.class, servletClass.getSuperclass(),
                "core bundle must resolve jakarta.servlet.* from the shared system packages");

        // Key classes load from their bundles, i.e. all Import-Package directives resolve.
        assertNotNull(coreBundle.loadClass("de.mc.ladon.s3server.auth.SigV4"));
        assertNotNull(coreBundle.loadClass("de.mc.ladon.s3server.auth.Authorization"));
        assertNotNull(coreBundle.loadClass("de.mc.ladon.s3server.common.S3ChunkedInputStream"));
        assertNotNull(coreBundle.loadClass("de.mc.ladon.s3server.auth.SigV4ChunkVerifier"));
        assertNotNull(fsBundle.loadClass("de.mc.ladon.s3server.repository.impl.FSRepository"));
        assertNotNull(fsBundle.loadClass("de.mc.ladon.s3server.enc.PlaintextEncryptor"));
        assertNotNull(starterBundle.loadClass("de.mc.ladon.s3server.osgi.S3ServerActivator"));
    }

    @Test
    void bundlesRegisterAndWireTheirServices() throws Exception {
        String S3REPOSITORY = "de.mc.ladon.s3server.repository.api.S3Repository";

        // 1) the fs bundle (service provider) must offer its S3Repository service, visible
        //    in its own context — exactly the perspective of the consuming bundles.
        BundleContext fsContext = fsBundle.getBundleContext();
        ServiceReference<?> repositoryRef = fsContext.getServiceReference(S3REPOSITORY);
        assertNotNull(repositoryRef, "the fs bundle must register an S3Repository OSGi service");
        Object repositoryService = fsContext.getService(repositoryRef);
        assertNotNull(repositoryService, "the S3Repository service must be retrievable");
        assertEquals("de.mc.ladon.s3server.repository.impl.FSRepository",
                repositoryService.getClass().getName(),
                "the fs bundle must offer the FSRepository implementation as its service");
        fsContext.ungetService(repositoryRef);

        // NOTE: the service intentionally cannot be looked up from the *system bundle*
        // context: OSGi R8 service visibility (Felix' ServiceReference.isAssignableTo)
        // hides a service from a caller whose classload resolution of the service
        // interface differs from the provider's. The system bundle resolves
        // de.mc.ladon.* through the test classpath, the fs bundle resolves it through
        // the core bundle — two different class identities, so the service is (correctly)
        // invisible there. The queries below use the proper bundle perspectives.

        // 2) the starter bundle (service consumer) must find the repository (it imports
        //    the same package from the same provider bundle — core) and inject it into
        //    the S3Servlet it publishes on the whiteboard.
        BundleContext starterContext = starterBundle.getBundleContext();
        ServiceReference<?> servletRef = starterContext.getServiceReference("jakarta.servlet.Servlet");
        assertNotNull(servletRef, "the starter bundle must publish the S3Servlet on the servlet whiteboard");
        assertEquals("/api/s3/*", servletRef.getProperty("osgi.http.whiteboard.servlet.pattern"),
                "the whiteboard servlet must be registered for /api/s3/*");
        Object servlet = starterContext.getService(servletRef);
        assertNotNull(servlet, "the whiteboard servlet must be retrievable");
        assertEquals("de.mc.ladon.s3server.servlet.S3Servlet", servlet.getClass().getName());

        // 3) proof of the actual OSGi wiring: S3Servlet.repository must have been set by
        //    the starter activator from the service it looked up — a LoggingRepository
        //    wrapping the FSRepository delivered by the fs bundle's service.
        java.lang.reflect.Field repositoryField = servlet.getClass().getDeclaredField("repository");
        repositoryField.setAccessible(true);
        Object servletRepository = repositoryField.get(servlet);
        assertNotNull(servletRepository,
                "the S3Servlet must have received a repository through OSGi service injection");
        assertEquals("de.mc.ladon.s3server.logging.LoggingRepository", servletRepository.getClass().getName());

        java.lang.reflect.Field delegateField = servletRepository.getClass().getDeclaredField("delegate");
        delegateField.setAccessible(true);
        Object delegate = delegateField.get(servletRepository);
        assertNotNull(delegate, "the LoggingRepository must wrap the looked-up service");
        assertEquals("de.mc.ladon.s3server.repository.impl.FSRepository", delegate.getClass().getName(),
                "the delegate must be the FSRepository instance registered by the fs bundle");
        starterContext.ungetService(servletRef);
    }

    // ---------------------------------------------------------------------------
    // 2. FSRepository functional round-trip inside the OSGi world
    // ---------------------------------------------------------------------------

    @Test
    void fsRepositoryRoundTrip() throws Throwable {
        Object repo = newFsRepository();
        Class<?> ctxClass = coreBundle.loadClass("de.mc.ladon.s3server.entities.impl.S3CallContextImpl");
        Class<?> ctxIface = coreBundle.loadClass("de.mc.ladon.s3server.entities.api.S3CallContext");

        Path bucketFolder = s3data.resolve("osgi-test");
        Object ctx = newContext(ctxClass, "PUT", "/osgi-test", headers(Map.of()), null, null);

        // create bucket
        invoke(repo, "createBucket", new Class[]{ctxIface, String.class, String.class}, ctx, "osgi-test", null);
        assertTrue(Files.exists(bucketFolder), "createBucket must create the bucket folder on disk");

        // list buckets
        Object listing = invoke(repo, "listAllBuckets", new Class[]{ctxIface}, ctx);
        @SuppressWarnings("unchecked")
        List<Object> buckets = (List<Object>) listing;
        assertEquals(1, buckets.size(), "exactly the created bucket is expected");
        assertEquals("osgi-test",
                buckets.get(0).getClass().getMethod("getBucketName").invoke(buckets.get(0)));

        // put an object (payload flows through the servlet input stream)
        byte[] payload = "Hallo OSGi — Ladon S3 (Felix test)".getBytes(StandardCharsets.UTF_8);
        Object putCtx = newContext(ctxClass, "PUT", "/osgi-test/hallo.txt",
                headers(Map.of("Content-Length", String.valueOf(payload.length))), payload, null);
        invoke(repo, "createObject", new Class[]{ctxIface, String.class, String.class},
                putCtx, "osgi-test", "hallo.txt");
        Path stored = bucketFolder.resolve("data").resolve("hallo.txt");
        assertTrue(Files.exists(stored), "object data must be written: " + stored);
        assertArrayEquals(payload, Files.readAllBytes(stored), "stored object must equal the payload (unencrypted)");

        // get the object back — the content is streamed through the servlet output stream
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        Object getCtx = newContext(ctxClass, "GET", "/osgi-test/hallo.txt", headers(Map.of()), null, responseBody);
        invoke(repo, "getObject", new Class[]{ctxIface, String.class, String.class, boolean.class},
                getCtx, "osgi-test", "hallo.txt", false);
        assertArrayEquals(payload, responseBody.toByteArray(),
                "getObject must stream the stored content back through the response");

        // delete object + bucket
        invoke(repo, "deleteObject", new Class[]{ctxIface, String.class, String.class}, ctx, "osgi-test", "hallo.txt");
        assertFalse(Files.exists(stored), "deleteObject must remove the object data");
        invoke(repo, "deleteBucket", new Class[]{ctxIface, String.class}, ctx, "osgi-test");
        assertFalse(Files.exists(bucketFolder), "deleteBucket must remove the bucket folder");
    }

    // ---------------------------------------------------------------------------
    // 3. SigV4 authorization verification (the bundle code computes + compares the signature)
    // ---------------------------------------------------------------------------

    @Test
    void sigV4HeaderAuthorizationVerifiesInOsgi() throws Throwable {
        Object repo = newFsRepository();
        Class<?> ctxClass = coreBundle.loadClass("de.mc.ladon.s3server.entities.impl.S3CallContextImpl");
        Class<?> ctxIface = coreBundle.loadClass("de.mc.ladon.s3server.entities.api.S3CallContext");
        Class<?> repoIface = coreBundle.loadClass("de.mc.ladon.s3server.repository.api.S3Repository");
        Class<?> authClass = coreBundle.loadClass("de.mc.ladon.s3server.auth.Authorization");

        String xamzDate = ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        String date = xamzDate.substring(0, 8);
        String host = "s3.ladon.test";
        String payloadHash = SigV4.sha256Hex("");
        String scope = date + "/" + REGION + "/" + SERVICE + "/aws4_request";

        // Canonical request per the AWS SigV4 spec (GET /, three signed headers).
        String canonicalRequest = "GET\n" + "/\n" + "\n"
                + "host:" + host + "\n"
                + "x-amz-content-sha256:" + payloadHash + "\n"
                + "x-amz-date:" + xamzDate + "\n"
                + "\n"
                + "host;x-amz-content-sha256;x-amz-date\n"
                + payloadHash;
        String stringToSign = "AWS4-HMAC-SHA256\n" + xamzDate + "\n" + scope + "\n"
                + SigV4.sha256Hex(canonicalRequest);
        byte[] signingKey = SigV4.signingKey(SECRET_KEY, date, REGION, SERVICE);
        String signature = SigV4.hex(SigV4.hmacSha256(signingKey, stringToSign));
        String authorization = "AWS4-HMAC-SHA256 Credential=" + ACCESS_KEY + "/" + scope
                + ", SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=" + signature;

        Map<String, String> h = new LinkedHashMap<>();
        h.put("Host", host);
        h.put("X-Amz-Date", xamzDate);
        h.put("X-Amz-Content-Sha256", payloadHash);
        h.put("Authorization", authorization);

        Method check = authClass.getMethod("checkAuthHeader", ctxIface, repoIface);

        Object ctx = newContext(ctxClass, "GET", "/", headers(h), null, null);
        check.invoke(null, ctx, repo); // must not throw: valid signature

        Object user = ctxClass.getMethod("getUser").invoke(ctx);
        assertNotNull(user, "the authenticated user must be set into the call context");
        // In the fs model the AWS access key id is carried as the "public key"; the
        // account must be the one from the repository (userId stays "SYSTEM" there).
        assertEquals(ACCESS_KEY, publicKeyOf(user),
                "the authenticated user must be the repository account for the presented access key");

        // A tampered signature must be rejected with the proper S3 error code.
        Map<String, String> bad = new LinkedHashMap<>(h);
        bad.put("Authorization", authorization.replace(signature, flipLastHexChar(signature)));
        Object badCtx = newContext(ctxClass, "GET", "/", headers(bad), null, null);
        try {
            check.invoke(null, badCtx, repo);
            fail("a tampered SigV4 signature must be rejected");
        } catch (InvocationTargetException e) {
            assertEquals("de.mc.ladon.s3server.exceptions.InvalidSecurityException",
                    e.getCause().getClass().getName(), "SigV4 rejection must use the S3 error code");
        }
    }

    // ---------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------

    private static Object newFsRepository() throws Exception {
        Class<?> feIface = fsBundle.loadClass("de.mc.ladon.s3server.enc.FileEncryptor");
        Class<?> plaintext = fsBundle.loadClass("de.mc.ladon.s3server.enc.PlaintextEncryptor");
        Object encryptor = plaintext.getConstructor().newInstance();
        Class<?> repoClass = fsBundle.loadClass("de.mc.ladon.s3server.repository.impl.FSRepository");
        return repoClass.getConstructor(String.class, feIface).newInstance(s3data.toString(), encryptor);
    }

    private static Map<String, String> headers(Map<String, String> raw) {
        Map<String, String> out = new LinkedHashMap<>();
        raw.forEach((k, v) -> out.put(k.toLowerCase(Locale.ROOT), v));
        return out;
    }

    private static String publicKeyOf(Object user) throws Exception {
        for (Method m : user.getClass().getMethods()) {
            if ("getPublicKey".equals(m.getName())) {
                return (String) m.invoke(user);
            }
        }
        return "<no getPublicKey>";
    }

    private static String flipLastHexChar(String hex) {
        char last = hex.charAt(hex.length() - 1);
        return hex.substring(0, hex.length() - 1) + (last == 'a' ? 'b' : 'a');
    }

    /**
     * Builds an {@code S3CallContextImpl} instance <em>inside the core bundle's</em>
     * classloader (so it implements the bundle-side {@code S3CallContext} interface),
     * backed by Mockito mocks for the servlet request/response. The mocks implement
     * the <em>same</em> {@code jakarta.servlet} interfaces that the bundle code links
     * against (shared system packages), crossing objects between the JUnit world and
     * the OSGi world is therefore safe.
     */
    private static Object newContext(Class<?> ctxClass, String method, String uri,
                                     Map<String, String> lowerCasedHeaders,
                                     byte[] requestBody,
                                     ByteArrayOutputStream responseSink) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getQueryString()).thenReturn(null);
        when(request.getHeaderNames())
                .thenAnswer(inv -> Collections.enumeration(lowerCasedHeaders.keySet()));
        when(request.getHeader(anyString())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            return name == null ? null : lowerCasedHeaders.get(name.toLowerCase(Locale.ROOT));
        });
        if (requestBody != null) {
            when(request.getInputStream()).thenReturn(servletInputStream(requestBody));
        }

        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream out = mock(ServletOutputStream.class);
        doAnswer(inv -> {
            if (responseSink != null) {
                byte[] b = inv.getArgument(0);
                int off = inv.getArgument(1);
                int len = inv.getArgument(2);
                responseSink.write(b, off, len);
            }
            return 0;
        }).when(out).write(any(byte[].class), anyInt(), anyInt());
        doNothing().when(out).flush();
        doNothing().when(response).flushBuffer();
        when(response.getOutputStream()).thenReturn(out);

        Constructor<?> ctor = ctxClass.getConstructor(
                HttpServletRequest.class, HttpServletResponse.class, Map.class);
        return ctor.newInstance(request, response, new LinkedHashMap<String, String[]>() {});
    }

    /** Wraps a byte array as the abstract {@link ServletInputStream}. */
    private static jakarta.servlet.ServletInputStream servletInputStream(byte[] body) {
        final ByteArrayInputStream in = new ByteArrayInputStream(body);
        return new jakarta.servlet.ServletInputStream() {
            @Override
            public boolean isFinished() {
                return in.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(jakarta.servlet.ReadListener readListener) {
                throw new UnsupportedOperationException("not used in this test");
            }

            @Override
            public int read() {
                return in.read();
            }

            @Override
            public int read(byte[] b, int off, int len) {
                return in.read(b, off, len);
            }
        };
    }

    private static Object invoke(Object target, String method, Class<?>[] paramTypes, Object... args) throws Throwable {
        Method m = target.getClass().getMethod(method, paramTypes);
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause() == null ? e : e.getCause();
        }
    }
}
