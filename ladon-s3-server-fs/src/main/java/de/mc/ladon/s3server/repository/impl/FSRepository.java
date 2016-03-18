package de.mc.ladon.s3server.repository.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import de.mc.ladon.s3server.common.S3Constants;
import de.mc.ladon.s3server.common.StreamUtils;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple FileSystem Repository
 *
 * @author Ralf Ulrich on 21.02.16.
 */
public class FSRepository implements S3Repository {


    public static final String META_XML_EXTENSION = "_meta.xml";
    public static final String USER_FOLDER = "users";
    public static final String SYSTEM_DEFAULT_USER = "SYSTEM";
    private final JAXBContext jaxbContext;
    private Logger logger = LoggerFactory.getLogger(FSRepository.class);
    private static final String DATA_FOLDER = "data";
    private static final String META_FOLDER = "meta";
    private final String fsrepoBaseUrl;
    private final ConcurrentMap<String, S3User> userMap;

    public FSRepository(String fsrepoBaseUrl) {
        this.fsrepoBaseUrl = fsrepoBaseUrl;
        try {
            jaxbContext = JAXBContext.newInstance(FSStorageMeta.class, FSUserData.class);
            userMap = new ConcurrentHashMap<>(loadUserFile());
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Predicate<Path> IS_DIRECTORY = p -> Files.isDirectory(p);


    @Override
    public List<S3Bucket> listAllBuckets(S3CallContext callContext) {
        try {
            return Files.list(Paths.get(fsrepoBaseUrl)).filter(IS_DIRECTORY).map(
                    path -> {
                        return new S3BucketImpl(path.getFileName().toString(), new Date(path.toFile().lastModified()), new S3UserImpl());
                    }
            ).collect(Collectors.toList());
        } catch (IOException e) {
            throw new NoSuchBucketException(null, callContext.getRequestId());
        }
    }

    @Override
    public void createBucket(S3CallContext callContext, String bucketName, String locationConstraint) {
        Path dataBucket = Paths.get(fsrepoBaseUrl, bucketName, DATA_FOLDER);
        Path metaBucket = Paths.get(fsrepoBaseUrl, bucketName, META_FOLDER);
        Path metaBucketFile = Paths.get(fsrepoBaseUrl, bucketName + META_XML_EXTENSION);
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
        Path dataBucket = Paths.get(fsrepoBaseUrl, bucketName);
        Path metaBucket = Paths.get(fsrepoBaseUrl, bucketName + META_XML_EXTENSION);
        if (!Files.exists(dataBucket))
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());

        try {
            Files.walkFileTree(dataBucket, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
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
    public void createObject(S3CallContext callContext, String bucketName, String objectKey) {
        Path dataBucket = Paths.get(fsrepoBaseUrl, bucketName, DATA_FOLDER);
        Path metaBucket = Paths.get(fsrepoBaseUrl, bucketName, META_FOLDER);
        if (!Files.exists(dataBucket))
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        Path obj = dataBucket.resolve(objectKey);
        Path meta = metaBucket.resolve(objectKey + META_XML_EXTENSION);

        Long contentLength = callContext.getHeader().getContentLength();
        String md5 = callContext.getHeader().getContentMD5();

        lock(metaBucket, objectKey, FSLock.LockType.write, callContext);
        try (InputStream in = callContext.getContent()) {
            if (!Files.exists(obj)) {
                Files.createDirectories(obj.getParent());
                Files.createFile(obj);
            }

            DigestInputStream din = new DigestInputStream(in, MessageDigest.getInstance("MD5"));
            OutputStream out = Files.newOutputStream(obj);

            long bytesCopied = StreamUtils.copy(din, out);
            byte[] md5bytes = din.getMessageDigest().digest();
            String storageMd5base64 = BaseEncoding.base64().encode(md5bytes);
            String storageMd5base16 = BaseEncoding.base16().encode(md5bytes);

            if (contentLength != null && contentLength != bytesCopied
                    || md5 != null && !md5.equals(storageMd5base64)) {
                Files.delete(obj);
                Files.deleteIfExists(meta);
                throw new InvalidDigestException(objectKey, callContext.getRequestId());
            }

            S3ResponseHeader header = new S3ResponseHeaderImpl();
            header.setEtag(inQuotes(storageMd5base16));
            header.setDate(new Date(Files.getLastModifiedTime(obj).toMillis()));
            callContext.setResponseHeader(header);

            Files.createDirectories(meta.getParent());
            writeMetaFile(meta, callContext, S3Constants.ETAG, inQuotes(storageMd5base16));

        } catch (IOException | NoSuchAlgorithmException | JAXBException e) {
            logger.error("internal error", e);
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        } catch (InterruptedException e) {
            logger.error("interrupted thread", e);
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        } finally {
            unlock(metaBucket, objectKey, callContext);
        }
    }

    private String inQuotes(String etag) {
        return "\"" + etag + "\"";
    }


    @Override
    public void getObject(S3CallContext callContext, String bucketName, String objectKey, boolean head) {
        Path bucketData = Paths.get(fsrepoBaseUrl, bucketName, DATA_FOLDER);
        Path bucketMeta = Paths.get(fsrepoBaseUrl, bucketName, META_FOLDER);
        if (!Files.exists(bucketData))
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        Path object = bucketData.resolve(objectKey);
        Path objectMeta = bucketMeta.resolve(objectKey + META_XML_EXTENSION);

        if (!Files.exists(object))
            throw new NoSuchKeyException(objectKey, callContext.getRequestId());

        lock(bucketMeta, objectKey, FSLock.LockType.read, callContext);
        if (Files.exists(objectMeta)) {
            loadMetaFileIntoHeader(objectMeta, callContext);
        }

        try {
            S3ResponseHeader header = new S3ResponseHeaderImpl();
            header.setContentLength(Files.size(object));
            header.setContentType(getMimeType(object));
            callContext.setResponseHeader(header);
            if (!head)
                callContext.setContent(Files.newInputStream(object));
        } catch (IOException e) {
            logger.error("internal error", e);
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        } finally {
            unlock(bucketMeta, objectKey, callContext);
        }
    }

    @Override
    public void getBucket(S3CallContext callContext, String bucketName) {
        Path bucket = Paths.get(fsrepoBaseUrl, bucketName);
        if (!Files.exists(bucket))
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());


    }

    @Override
    public S3ListBucketResult listBucket(S3CallContext callContext, String bucketName) {
        Integer maxKeys = callContext.getParams().getMaxKeys();
        String marker = callContext.getParams().getMarker();
        String prefix = callContext.getParams().getPrefix() != null ?
                callContext.getParams().getPrefix() : "";

        Path bucket = Paths.get(fsrepoBaseUrl, bucketName, DATA_FOLDER);
        Path bucketMeta = Paths.get(fsrepoBaseUrl, bucketName, META_FOLDER);

        try {
            Long count = getPathStream(bucketName, prefix, bucket, marker).limit(maxKeys + 1).count();

            return new S3ListBucketResultImpl(count > maxKeys, bucketName, getPathStream(bucketName, prefix, bucket, marker)
                    .limit(maxKeys)
                    .map(path -> {
                                String key = bucket.relativize(path).toString();
                                Path objectMeta = bucketMeta.resolve(key + META_XML_EXTENSION);
                                S3Metadata meta = loadMetaFile(objectMeta);

                                return new S3ObjectImpl(key,
                                        new Date(path.toFile().lastModified()),
                                        bucketName, path.toFile().length(),
                                        new S3UserImpl(),
                                        meta,
                                        null, meta.get(S3Constants.CONTENT_TYPE),
                                        meta.get(S3Constants.ETAG));
                            }
                    ).collect(Collectors.toList()));
        } catch (IOException e) {
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        }
    }

    private Stream<Path> getPathStream(String bucketName, String prefix, Path bucket, String marker) throws IOException {
        final Boolean[] markerFound = new Boolean[]{marker == null};
        return Files.walk(Paths.get(fsrepoBaseUrl, bucketName, DATA_FOLDER)).sorted()
                .filter(p -> {
                    String relpath = bucket.relativize(p).toString();
                    boolean include = markerFound[0];
                    if (!markerFound[0]) {
                        markerFound[0] = relpath.equals(marker);
                    }
                    return Files.isRegularFile(p)
                            && relpath.startsWith(prefix)
                            && include;
                });
    }

    private String getMimeType(Path path) throws IOException {
        String mime = Files.probeContentType(path);
        return mime != null ? mime : "application/octet-stream";
    }


    @Override
    public void deleteObject(S3CallContext callContext, String bucketName, String objectKey) {
        Path bucketData = Paths.get(fsrepoBaseUrl, bucketName, DATA_FOLDER);
        Path bucketMeta = Paths.get(fsrepoBaseUrl, bucketName, META_FOLDER);
        if (!Files.exists(bucketData))
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        Path objectData = bucketData.resolve(objectKey);
        Path objectMeta = bucketMeta.resolve(objectKey + META_XML_EXTENSION);
        if (!Files.exists(objectData))
            throw new NoSuchKeyException(objectKey, callContext.getRequestId());

        lock(bucketMeta, objectKey, FSLock.LockType.write, callContext);
        try {
            Files.delete(objectData);
            Files.delete(objectMeta);
        } catch (IOException e) {
            logger.error("internal error", e);
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        } finally {
            unlock(bucketMeta, objectKey, callContext);
        }
    }

    @Override
    public S3User getUser(S3CallContext callContext, String accessKey) {
        return loadUser(callContext, accessKey);
    }

    private void lock(Path metaPath, String objectKey, FSLock.LockType lockType, S3CallContext callContext) {
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

    private void unlock(Path metaPath, String objectKey, S3CallContext callContext) {
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


    private S3User loadUser(S3CallContext callContext, String accessKey) {
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
        Path userFile = basePath.resolve("userdb" + META_XML_EXTENSION);
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
        Path userFile = basePath.resolve("userdb" + META_XML_EXTENSION);
        try (OutputStream out = Files.newOutputStream(userFile)) {
            Marshaller m = jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(new FSUserData(ImmutableMap.of(SYSTEM_DEFAULT_USER, new FSUser(SYSTEM_DEFAULT_USER, SYSTEM_DEFAULT_USER, SYSTEM_DEFAULT_USER, SYSTEM_DEFAULT_USER, null))), out);
        }

    }


    private void writeMetaFile(Path meta, S3CallContext callContext, String... additional) throws IOException, JAXBException {
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
        callContext.setResponseHeader(header);
    }

    private S3Metadata loadMetaFile(Path meta) {
        try (InputStream in = Files.newInputStream(meta)) {
            FSStorageMeta metaData = (FSStorageMeta) jaxbContext.createUnmarshaller().unmarshal(in);
            return new S3MetadataImpl(metaData.getMeta());
        } catch (IOException | JAXBException e) {
            logger.error("error reading meta file", e);
        }
        return new S3MetadataImpl();
    }

}
