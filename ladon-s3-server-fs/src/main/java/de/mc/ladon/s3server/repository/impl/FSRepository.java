package de.mc.ladon.s3server.repository.impl;

import com.google.common.collect.ImmutableMap;
import de.mc.ladon.s3server.common.*;
import de.mc.ladon.s3server.enc.FileEncryptor;
import de.mc.ladon.s3server.entities.api.*;
import de.mc.ladon.s3server.entities.impl.*;
import de.mc.ladon.s3server.exceptions.*;
import de.mc.ladon.s3server.jaxb.fsmeta.FSStorageMeta;
import de.mc.ladon.s3server.jaxb.fsmeta.FSUserData;
import de.mc.ladon.s3server.repository.api.S3Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.io.BaseEncoding.base64;

/**
 * Simple FileSystem Repository
 *
 * @author Ralf Ulrich on 21.02.16.
 */
public class FSRepository implements S3Repository {


    public static final String META_XML_EXTENSION = "_meta.xml";
    public static final String SYSTEM_DEFAULT_USER = "SYSTEM";
    private final JAXBContext jaxbContext;
    private final Logger logger = LoggerFactory.getLogger(FSRepository.class);
    private static final String DATA_FOLDER = "data";
    private static final String META_FOLDER = "meta";
    protected String fsrepoBaseUrl;
    private ConcurrentMap<String, S3User> userMap;
    private final FileEncryptor fileEncryptor;


    public FSRepository(String fsrepoBaseUrl, FileEncryptor encryptor) {
        this.fsrepoBaseUrl = toOsPath(fsrepoBaseUrl);
        fileEncryptor = encryptor;
        try {
            jaxbContext = JAXBContext.newInstance(FSStorageMeta.class, FSUserData.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * This is the path of the meta data file for a given bucket.
     * Will be deleted when {@link #deleteBucket(S3CallContext, String)}  is called.
     *
     * @param bucketName name of the bucket
     * @return filesystem path of the bucket meta file
     */
    protected Path getBucketMetaFile(String bucketName) {
        return Paths.get(fsrepoBaseUrl, bucketName + metaFileSuffix());
    }

    /**
     * This folder contains all meta data of the given bucket.
     * By default, this is located under the bucket base folder {@link #getBucketBaseFolder(String)}
     * and gets cleaned up with it when {@link #deleteBucket(S3CallContext, String)}  is called.
     * If not you have to take care of it by yourself.
     *
     * @param bucketName name of the bucket
     * @return filesystem path for the meta data files
     */
    protected Path getBucketMetaFolder(String bucketName) {
        return Paths.get(fsrepoBaseUrl, bucketName, META_FOLDER);
    }

    /**
     * This folder contains all binary data of the given bucket.
     * By default, this is located under the bucket base folder {@link #getBucketBaseFolder(String)}
     * and gets cleaned up with it when {@link #deleteBucket(S3CallContext, String)}  is called.
     * If not you have to take care of it by yourself.
     *
     * @param bucketName name of the bucket
     * @return filesystem path for the binary data files
     */
    protected Path getBucketDataFolder(String bucketName) {
        return Paths.get(fsrepoBaseUrl, bucketName, DATA_FOLDER);
    }

    /**
     * This is the parent folder where all data for a bucket is located under.
     * When delete bucket is called, all files under this folder are deleted as well.
     * (and also the bucket meta file)
     * Can be overridden to create your own structure
     *
     * @param bucketName name of the bucket
     * @return filesystem path of the bucket folder
     */
    protected Path getBucketBaseFolder(String bucketName) {
        return Paths.get(fsrepoBaseUrl, bucketName);
    }

    /**
     * Suffix to append to the s3 key to identify meta data and to prevent collisions
     *
     * @return suffix to be appended. Should end with a normal file extension.
     * Default is _meta.xml
     */
    protected String metaFileSuffix() {
        return META_XML_EXTENSION;
    }

    @Override
    public List<S3Bucket> listAllBuckets(S3CallContext callContext) {
        try {
            return Files.list(Paths.get(fsrepoBaseUrl)).filter(Files::isDirectory).map(
                    path -> new S3BucketImpl(path.getFileName().toString(), new Date(path.toFile().lastModified()), new S3UserImpl())
            ).collect(Collectors.toList());
        } catch (IOException e) {
            throw new NoSuchBucketException(null, callContext.getRequestId());
        }
    }

    @Override
    public void createBucket(S3CallContext callContext, String bucketName, String locationConstraint) {
        Path dataBucket = getBucketDataFolder(bucketName);
        Path metaBucket = getBucketMetaFolder(bucketName);
        Path metaBucketFile = getBucketMetaFile(bucketName);
        //if (bucket.toFile().exists())
        //  throw new BucketAlreadyExistsException(bucketName, callContext.getRequestId());
        try {
            Files.createDirectories(dataBucket);
            Files.createDirectories(metaBucket);
            writeMetaFile(metaBucketFile, callContext);
        } catch (IOException | JAXBException e) {
            logger.error("internal error", e);
            throw new InternalErrorException(bucketName, callContext.getRequestId());
        }
    }

    @Override
    public void deleteBucket(S3CallContext callContext, String bucketName) {
        Path dataBucket = getBucketBaseFolder(bucketName);
        Path metaBucket = getBucketMetaFile(bucketName);
        if (!Files.exists(dataBucket))
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());

        try {
            Files.walkFileTree(dataBucket, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    throw new BucketNotEmptyException(bucketName, callContext.getRequestId());
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return super.postVisitDirectory(dir, exc);
                }
            });
            Files.delete(metaBucket);
        } catch (IOException e) {
            logger.error("internal error", e);
            throw new InternalErrorException(bucketName, callContext.getRequestId());
        }


    }

    @Override
    public S3Object copyObject(S3CallContext callContext, String srcBucket, String srcObjectKey,
                               String destBucket, String destObjectKey, boolean copyMetadata) {
        FSPaths src = getBucketPaths(callContext, srcBucket, srcObjectKey);
        FSPaths dest = getBucketPaths(callContext, destBucket, destObjectKey);
        if (!Files.exists(src.objectData))
            throw new NoSuchKeyException(srcObjectKey, callContext.getRequestId());

        lock(src.bucketMeta, src.osKey, FSLock.LockType.read, callContext);
        lock(dest.bucketMeta, dest.osKey, FSLock.LockType.write, callContext);
        long bytesCopied;
        S3Metadata srcMetadata = loadMetaFile(src.objectMeta);
        try (InputStream in = Files.newInputStream(src.objectData)) {
            if (!Files.exists(dest.objectData)) {
                Files.createDirectories(dest.objectData.getParent());
                Files.createFile(dest.objectData);
            }
            try (OutputStream out = Files.newOutputStream(dest.objectData)) {
                bytesCopied = StreamUtils.copy(in, out);
                if (copyMetadata) {
                    if (Files.exists(src.objectMeta)) {
                        writeMetaFile(dest.objectMeta, callContext, toArray(srcMetadata));
                    }
                } else {
                    writeMetaFile(dest.objectMeta, callContext);
                }
            }
            S3Metadata destMetadata = loadMetaFile(dest.objectMeta);
            return new S3ObjectImpl(destObjectKey,
                    new Date(dest.objectData.toFile().lastModified()),
                    destBucket,
                    bytesCopied,
                    new S3UserImpl(),
                    destMetadata,
                    null, destMetadata.get(S3Constants.CONTENT_TYPE),
                    destMetadata.get(S3Constants.ETAG), destMetadata.get(S3Constants.VERSION_ID), false, true);
        } catch (IOException | JAXBException e) {
            logger.error("internal error", e);
            throw new InternalErrorException(destObjectKey, callContext.getRequestId());
        } catch (InterruptedException e) {
            logger.error("interrupted thread", e);
            throw new InternalErrorException(destObjectKey, callContext.getRequestId());
        } finally {
            unlock(src.bucketMeta, src.osKey, callContext);
            unlock(dest.bucketMeta, dest.osKey, callContext);
        }

    }

    private String[] toArray(S3Metadata srcObjectMeta) {
        List<String> result = new ArrayList<>();
        ((S3MetadataImpl) srcObjectMeta).forEach((k, v) -> {
            result.add(k);
            result.add(v);
        });
        return result.toArray(new String[0]);
    }

    @Override
    public void createObject(S3CallContext callContext, String bucketName, String objectKey) {
        FSPaths p = getBucketPaths(callContext, bucketName, objectKey);
        boolean isFolder = objectKey.endsWith("/");
        if (isFolder) {
            try {
                Files.createDirectories(p.objectData);
            } catch (IOException e) {
                logger.error("could not create directory " + p.objectData, e);
            }
        } else {


            Long contentLength = callContext.getHeader().getContentLength();
            String decodedContentLength = callContext.getHeader()
                    .getFullHeader().get(S3Constants.X_AMZ_DECODED_CONTENT_LENGTH);
            String md5 = callContext.getHeader().getContentMD5();
            boolean isChunked = isChunked(callContext);

            lock(p.bucketMeta, p.osKey, FSLock.LockType.write, callContext);
            try (InputStream in = callContext.getContent()) {
                if (!Files.exists(p.objectData)) {
                    Files.createDirectories(p.objectData.getParent());
                    Files.createFile(p.objectData);
                }
                DigestInputStream din;
                if (isChunked) {
                    din = new DigestInputStream(new S3ChunkedInputStream(in), MessageDigest.getInstance("MD5"));
                } else {
                    din = new DigestInputStream(in, MessageDigest.getInstance("MD5"));
                }

                try (OutputStream out = fileEncryptor.getEncryptedOutputStream(p.objectData)) {
                    long bytesCopied = StreamUtils.copy(din, out);
                    byte[] md5bytes = din.getMessageDigest().digest();
                    String storageMd5base64 = base64().encode(md5bytes);
                    String storageMd5base16 = Encoding.toHex(md5bytes);

                    if (isChunked && decodedContentLength != null && !decodedContentLength.equals(Long.toString(bytesCopied))
                            || !isChunked && contentLength != null && contentLength != bytesCopied
                            || md5 != null && !md5.equals(storageMd5base64)) {
                        Files.delete(p.objectData);
                        Files.deleteIfExists(p.objectMeta);
                        throw new InvalidDigestException(objectKey, callContext.getRequestId());
                    }

                    S3ResponseHeader header = new S3ResponseHeaderImpl();
                    header.setEtag(inQuotes(storageMd5base16));
                    header.setDate(new Date(Files.getLastModifiedTime(p.objectData).toMillis()));
                    callContext.setResponseHeader(header);

                    Files.createDirectories(p.objectMeta.getParent());
                    writeMetaFile(p.objectMeta, callContext,
                            S3Constants.ETAG, inQuotes(storageMd5base16),
                            S3Constants.X_AMZ_DECODED_CONTENT_LENGTH, Long.toString(bytesCopied));
                }

            } catch (IOException | NoSuchAlgorithmException | JAXBException e) {
                logger.error("internal error", e);
                throw new InternalErrorException(objectKey, callContext.getRequestId());
            } catch (InterruptedException e) {
                logger.error("interrupted thread", e);
                throw new InternalErrorException(objectKey, callContext.getRequestId());
            } finally {
                unlock(p.bucketMeta, p.osKey, callContext);
            }
        }

    }

    private boolean isChunked(S3CallContext callContext) {
        try {
            return callContext.getHeader().getAuthorization().startsWith("AWS4");
        } catch (NullPointerException e){
            return false;
        }
    }

    private String inQuotes(String etag) {
        return "\"" + etag + "\"";
    }


    @Override
    public void getObject(S3CallContext callContext, String bucketName, String objectKey, boolean head) {
        FSPaths p = getBucketPaths(callContext, bucketName, objectKey);

        if (!Files.exists(p.objectData))
            throw new NoSuchKeyException(objectKey, callContext.getRequestId());

        lock(p.bucketMeta, p.osKey, FSLock.LockType.read, callContext);
        if (Files.exists(p.objectMeta)) {
            loadMetaFileIntoHeader(p.objectMeta, callContext);
        }

        try {
            S3ResponseHeader header = new S3ResponseHeaderImpl();
            header.setContentType(getMimeType(p.objectData));
            callContext.setResponseHeader(header);
            if (!head)
                callContext.setContent(fileEncryptor.getDecryptedInputStream(p.objectData));
        } catch (IOException e) {
            logger.error("internal error", e);
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        } finally {
            unlock(p.bucketMeta, p.osKey, callContext);
        }
    }

    @Override
    public void getBucket(S3CallContext callContext, String bucketName) {
        Path bucket = getBucketBaseFolder(bucketName);
        if (!Files.exists(bucket))
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
    }

    @Override
    public S3ListBucketResult listBucket(S3CallContext callContext, String bucketName) {
        int maxKeys = callContext.getParams().getMaxKeys();
        String marker = callContext.getParams().getMarker();
        String prefix = callContext.getParams().getPrefix() != null ?
                callContext.getParams().getPrefix() : "";
        String delimiter = callContext.getParams().getDelimiter();

        Path bucket = getBucketDataFolder(bucketName);
        Path bucketMeta = getBucketMetaFolder(bucketName);

        try {
            long count = getPathStream(bucketName, prefix, bucket, marker).limit(maxKeys + 1).count();
            List<String> emptyFolders = new ArrayList<>();
            Stream<S3ObjectImpl> listing = getPathStream(bucketName, prefix, bucket, marker)
                    .map(path -> {
                                String key = bucket.relativize(path).toString();
                                if (Files.isDirectory(path)) {
                                    emptyFolders.add(toS3Path(key));
                                    return null;
                                } else {
                                    Path objectMeta = bucketMeta.resolve(key + metaFileSuffix());
                                    S3Metadata meta = loadMetaFile(objectMeta);
                                    String contentLength = meta.get(S3Constants.X_AMZ_DECODED_CONTENT_LENGTH);
                                    long size;
                                    if (contentLength != null) {
                                        size = Long.parseLong(contentLength);
                                    } else {
                                        size = path.toFile().length();
                                    }
                                    return new S3ObjectImpl(toS3Path(key),
                                            new Date(path.toFile().lastModified()),
                                            bucketName,
                                            size,
                                            new S3UserImpl(),
                                            meta,
                                            null,
                                            meta.get(S3Constants.CONTENT_TYPE),
                                            meta.get(S3Constants.ETAG),
                                            meta.get(S3Constants.VERSION_ID),
                                            false,
                                            true);
                                }
                            }
                    )
                    .filter(Objects::nonNull)
                    .limit(maxKeys);

            if (delimiter == null) {
                return new S3ListBucketResultImpl(listing.collect(Collectors.toList()), emptyFolders, count > maxKeys, bucketName, null, null);
            } else {
                if (!delimiter.equals("/")) throw new InvalidTokenException(delimiter, callContext.getRequestId());
                Set<String> prefixes = new HashSet<>();
                List<S3Object> objects = new ArrayList<>();
                listing.forEach(obj -> {
                    String commonPrefix = DelimiterUtil.getCommonPrefix(obj.getKey(), prefix, "/");
                    if (commonPrefix != null) {
                        prefixes.add(commonPrefix);
                    } else {
                        objects.add(obj);
                    }
                });
                List<String> emptyFoldersInPath = emptyFolders.stream()
                        .map(s -> DelimiterUtil.getCommonPrefix(s + "/", prefix, "/"))
                        .filter(Objects::nonNull).collect(Collectors.toList());
                prefixes.addAll(emptyFoldersInPath);
                List<String> prefList = new ArrayList<>(prefixes);
                return new S3ListBucketResultImpl(objects, prefList, objects.size() > maxKeys, bucketName, null, null);
            }

        } catch (IOException e) {
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        }
    }

    private Stream<Path> getPathStream(String bucketName, String prefix, Path bucket, String marker) throws IOException {
        final Boolean[] markerFound = new Boolean[]{marker == null};
        return Files.walk(getBucketDataFolder(bucketName)).sorted()
                .filter(p -> {
                    String relpath = toS3Path(bucket.relativize(p).toString());
                    boolean include = markerFound[0];
                    if (!markerFound[0]) {
                        markerFound[0] = relpath.equals(marker);
                    }
                    return (Files.isRegularFile(p) || Files.isDirectory(p) && p.toFile().list().length == 0)
                            && relpath.startsWith(prefix)
                            && include;
                });
    }


    @Override
    public void deleteObject(S3CallContext callContext, String bucketName, String objectKey) {
        FSPaths p = getBucketPaths(callContext, bucketName, objectKey);
        if (!Files.exists(p.objectData))
            throw new NoSuchKeyException(objectKey, callContext.getRequestId());

        lock(p.bucketMeta, p.osKey, FSLock.LockType.write, callContext);
        try {
            Files.delete(p.objectData);
            Files.delete(p.objectMeta);
        } catch (IOException e) {
            logger.error("internal error", e);
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        } finally {
            unlock(p.bucketMeta, p.osKey, callContext);
        }
    }

    @Override
    public S3User getUser(S3CallContext callContext, String accessKey) {
        return loadUser(callContext, accessKey);
    }

    protected void lock(Path metaPath, String objectKey, FSLock.LockType lockType, S3CallContext callContext) {
        FSLock lock;
        try {
            if (Files.exists(FSLock.getPath(metaPath, objectKey))) {
                lock = FSLock.load(metaPath, objectKey);
                if (!lock.isObsolete()) {
                    throw new OperationAbortedException(objectKey, callContext.getRequestId());
                }
            }
            lock = new FSLock(lockType, callContext.getUser());
            lock.save(metaPath, objectKey);
        } catch (IOException e) {
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        }
    }

    protected void unlock(Path metaPath, String objectKey, S3CallContext callContext) {
        FSLock lock;
        try {
            lock = FSLock.load(metaPath, objectKey);
            if (lock.isUnlockAllowed(callContext.getUser())) {
                lock.delete(callContext.getUser(), metaPath, objectKey);
            } else {
                throw new OperationAbortedException(objectKey, callContext.getRequestId());
            }
        } catch (IOException e) {
            logger.debug("resource not locked", e);
        }
    }


    private String getMimeType(Path path) throws IOException {
        String mime = URLConnection.guessContentTypeFromName(path.toString());
        return mime != null ? mime : "application/octet-stream";
    }

    private static class FSPaths {
        public Path bucketMeta, bucketData, objectMeta, objectData;
        public String osKey;
    }

    private FSPaths getBucketPaths(S3CallContext cc, String bucketName, String key) {
        FSPaths fsp = new FSPaths();
        fsp.osKey = toOsPath(key);
        fsp.bucketData = getBucketDataFolder(bucketName);
        fsp.bucketMeta = getBucketMetaFolder(bucketName);
        if (!Files.exists(fsp.bucketData))
            throw new NoSuchBucketException(bucketName, cc.getRequestId());
        fsp.objectData = fsp.bucketData.resolve(fsp.osKey);
        fsp.objectMeta = fsp.bucketMeta.resolve(fsp.osKey + metaFileSuffix());
        return fsp;
    }

    private S3User loadUser(S3CallContext callContext, String accessKey) {
        if (accessKey == null){
             throw new InvalidAccessKeyIdException("", callContext.getRequestId());
        }
        if (userMap == null) {
            userMap = new ConcurrentHashMap<>(loadUserFile());
        }
        S3User user = userMap.get(accessKey);
        if (user != null) return user;
        else {
            S3User reloaded = loadUserFile().get(accessKey);
            if (reloaded != null) {
                return userMap.put(accessKey, reloaded);
            }
        }
        throw new InvalidAccessKeyIdException("", callContext.getRequestId());
    }

    private Map<String, FSUser> loadUserFile() {
        Path basePath = Paths.get(fsrepoBaseUrl);
        Path userFile = basePath.resolve("userdb" + metaFileSuffix());
        try {
            if (!Files.exists(userFile)) {
                initDefaultUser();
            }
            try (InputStream in = Files.newInputStream(userFile)) {
                FSUserData userData = (FSUserData) jaxbContext.createUnmarshaller().unmarshal(in);
                return userData.getUsers();
            }
        } catch (IOException | JAXBException e) {
            logger.error("error reading user file ", e);
            throw new RuntimeException(e);
        }

    }

    private void initDefaultUser() throws IOException, JAXBException {
        Path basePath = Paths.get(fsrepoBaseUrl);
        Files.createDirectories(basePath);
        Path userFile = basePath.resolve("userdb" + metaFileSuffix());
        try (OutputStream out = Files.newOutputStream(userFile)) {
            Marshaller m = jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(new FSUserData(ImmutableMap.of("rHUYeAk58Ilhg6iUEFtr",
                    new FSUser(SYSTEM_DEFAULT_USER,
                            SYSTEM_DEFAULT_USER,
                            "rHUYeAk58Ilhg6iUEFtr",
                            "IVimdW7BIQLq9PLyVpXzZUq8zS4nLfrsoiZSJanu",
                            null))), out);
        }

    }


    /**
     * Writes the meta data as xml using jaxb. Override to implement your own format.
     */
    protected void writeMetaFile(Path meta, S3CallContext callContext, String... additional) throws IOException, JAXBException {
        Map<String, String> header = callContext.getHeader().getFullHeader();
        if (additional.length > 0 && additional.length % 2 == 0) {
            for (int i = 0; i < additional.length; i = i + 2) {
                header.put(additional[i], additional[i + 1]);
            }
        }
        Files.createDirectories(meta.getParent());
        try (OutputStream out = Files.newOutputStream(meta)) {
            Marshaller m = jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(new FSStorageMeta(header), out);
        }
    }

    private void loadMetaFileIntoHeader(Path meta, S3CallContext callContext) {
        S3Metadata userMetadata = loadMetaFile(meta);
        S3ResponseHeader header = new S3ResponseHeaderImpl(userMetadata);
        String contentLength = userMetadata.get(S3Constants.X_AMZ_DECODED_CONTENT_LENGTH);
        if (contentLength != null) {
            header.setContentLength(Long.parseLong(contentLength));
        }
        callContext.setResponseHeader(header);
    }

    /**
     * Reads the meta data xml file using jaxb. Override to implement your own format.
     */
    protected S3Metadata loadMetaFile(Path meta) {
        try (InputStream in = Files.newInputStream(meta)) {
            FSStorageMeta metaData = (FSStorageMeta) jaxbContext.createUnmarshaller().unmarshal(in);
            return new S3MetadataImpl(metaData.getMeta());
        } catch (IOException | JAXBException e) {
            logger.warn("error reading meta file at " + meta, e);
        }
        return new S3MetadataImpl();
    }

    protected String toOsPath(String unixStyle) {
        return unixStyle.replace("/", File.separator);
    }

    protected String toS3Path(String osStyle) {
        return osStyle.replace(File.separator, "/");
    }
}
