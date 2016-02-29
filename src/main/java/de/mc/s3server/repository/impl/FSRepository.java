package de.mc.s3server.repository.impl;

import de.mc.s3server.entities.api.*;
import de.mc.s3server.entities.impl.*;
import de.mc.s3server.exceptions.*;
import de.mc.s3server.jaxb.entities.CreateBucketConfiguration;
import de.mc.s3server.repository.api.S3Repository;
import org.apache.tomcat.util.buf.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
    private Logger logger = LoggerFactory.getLogger(FSRepository.class);
    private static final String DATA_FOLDER = "data";
    private static final String META_FOLDER = "meta";


    public static final Predicate<Path> IS_DIRECTORY = p -> Files.isDirectory(p);
    @Value("${s3server.fsrepo.root}")
    private String fsrepoBaseUrl;


    @Override
    public List<S3Bucket> listAllBuckets(S3CallContext callContext) {
        try {
            return Files.list(Paths.get(fsrepoBaseUrl)).filter(IS_DIRECTORY).map(
                    path -> {
                        String principal = getUserPrincipal(callContext, path, path.getFileName().toString());
                        return new S3BucketImpl(path.getFileName().toString(), new Date(path.toFile().lastModified()), new S3UserImpl(principal, principal));
                    }
            ).collect(Collectors.toList());
        } catch (IOException e) {
            throw new NoSuchBucketException(null, callContext.getRequestId());
        }
    }

    @Override
    public void createBucket(S3CallContext callContext, String bucketName, CreateBucketConfiguration configuration) {
        Path dataBucket = Paths.get(fsrepoBaseUrl, bucketName, DATA_FOLDER);
        Path metaBucket = Paths.get(fsrepoBaseUrl, bucketName, META_FOLDER);
        Path metaBucketFile = Paths.get(fsrepoBaseUrl, bucketName + META_XML_EXTENSION);
        //if (bucket.toFile().exists())
        //  throw new BucketAlreadyExistsException(bucketName, callContext.getRequestId());
        try {
            Files.createDirectories(dataBucket);
            Files.createDirectories(metaBucket);
            writeMetaFile(metaBucketFile, callContext);
        } catch (IOException e) {
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


        lock(metaBucket, objectKey, FSLock.LockType.write, callContext);
        try (InputStream in = callContext.getContent()) {
            if (!Files.exists(obj)) {
                Files.createDirectories(obj.getParent());
                Files.createFile(obj);
            }

            DigestInputStream din = new DigestInputStream(in, MessageDigest.getInstance("MD5"));
            OutputStream out = Files.newOutputStream(obj);
            StreamUtils.copy(din, out);
            S3ResponseHeader header = new S3ResponseHeaderImpl();
            header.setEtag(HexUtils.toHexString(din.getMessageDigest().digest()));
            header.setDate(new Date());
            callContext.setResponseHeader(header);

            Files.createDirectories(meta.getParent());
            writeMetaFile(meta, callContext);

        } catch (IOException | NoSuchAlgorithmException e) {
            //logger.error("internal error", e);
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        } finally {
            unlock(metaBucket, objectKey, callContext);
        }
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
            loadMetaFile(objectMeta, callContext);
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
        try {
            Long count = getPathStream(bucketName, prefix, bucket).limit(maxKeys + 1).count();
            return new S3ListBucketResultImpl(count > maxKeys, bucketName, getPathStream(bucketName, prefix, bucket)
                    .limit(maxKeys)
                    .map(path -> {
                                String key = bucket.relativize(path).toString();
                                String owner = getUserPrincipal(callContext, path, key);
                                try {
                                    return new S3ObjectImpl(key,
                                            new Date(path.toFile().lastModified()),
                                            bucketName, path.toFile().length(),
                                            new S3UserImpl(owner, owner),
                                            new S3MetadataImpl(),
                                            null, getMimeType(path));
                                } catch (IOException e) {
                                    logger.error("internal error", e);
                                    throw new InternalErrorException(bucketName, callContext.getRequestId());
                                }
                            }
                    ).collect(Collectors.toList()));
        } catch (IOException e) {
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        }
    }

    private Stream<Path> getPathStream(String bucketName, String prefix, Path bucket) throws IOException {
        return Files.walk(Paths.get(fsrepoBaseUrl, bucketName, DATA_FOLDER))
                .filter(p -> Files.isRegularFile(p))
                .filter(p -> bucket.relativize(p).toString().startsWith(prefix));
    }

    private MimeType getMimeType(Path path) throws IOException {
        String mime = Files.probeContentType(path);
        return mime != null ? MimeType.valueOf(mime) : MimeTypeUtils.APPLICATION_OCTET_STREAM;
    }

    private String getUserPrincipal(S3CallContext callContext, Path path, String key) {
        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
        UserPrincipal owner;
        try {
            owner = ownerAttributeView.getOwner();
        } catch (IOException e) {
            logger.error("internal error", e);
            throw new InternalErrorException(key, callContext.getRequestId());
        }
        return owner.getName();
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

    private void writeMetaFile(Path meta, S3CallContext callContext) throws IOException {
        Map<String, String> header = callContext.getHeader().getFullHeader();
        Properties p = new Properties();
        p.putAll(header);
        try (OutputStream out = Files.newOutputStream(meta)) {
            p.storeToXML(out, null);
        }
    }


    private void loadMetaFile(Path meta, S3CallContext callContext) {
        try (InputStream in = Files.newInputStream(meta)) {
            Properties p = new Properties();
            p.loadFromXML(in);
            S3Metadata userMetadata = new S3MetadataImpl(p);
            S3ResponseHeader header = new S3ResponseHeaderImpl(userMetadata);
            callContext.setResponseHeader(header);
        } catch (IOException e) {
            logger.error("error reading meta file", e);
        }
    }
}
