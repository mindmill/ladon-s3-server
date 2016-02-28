package de.mc.s3server.repository.impl;

import de.mc.s3server.jaxb.entities.CreateBucketConfiguration;
import de.mc.s3server.entities.api.*;
import de.mc.s3server.entities.impl.*;
import de.mc.s3server.exceptions.*;
import de.mc.s3server.repository.api.S3Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Simple FileSystem Repository
 *
 * @author Ralf Ulrich on 21.02.16.
 */
public class FSRepository implements S3Repository {

    public static final Predicate<Path> IS_DIRECTORY = p -> p.toFile().isDirectory();
    public static final Predicate<Path> IS_FILE = p -> p.toFile().isFile();
    @Value("${s3server.fsrepo.baseurl}")
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
        Path bucket = Paths.get(fsrepoBaseUrl, bucketName);
        //if (bucket.toFile().exists())
          //  throw new BucketAlreadyExistsException(bucketName, callContext.getRequestId());
        try {
            Files.createDirectories(bucket);
        } catch (IOException e) {
            throw new InternalErrorException(bucketName, callContext.getRequestId());
        }
    }

    @Override
    public void updateBucket(S3CallContext callContext, S3Bucket bucket) {

    }


    @Override
    public void deleteBucket(S3CallContext callContext, String bucketName) {
        Path bucket = Paths.get(fsrepoBaseUrl, bucketName);
        if (!bucket.toFile().exists())
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());

        try {
            Files.walkFileTree(bucket, new SimpleFileVisitor<Path>() {
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
        } catch (IOException e) {
            throw new InternalErrorException(bucketName, callContext.getRequestId());
        }


    }

    @Override
    public void createObject(S3CallContext callContext, String bucketName, String objectKey) {
        Path bucket = Paths.get(fsrepoBaseUrl, bucketName);
        if (!bucket.toFile().exists())
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        Path obj = Paths.get(bucket.toString(), objectKey);
        File objectFile = obj.toFile();

        try (InputStream in = callContext.getContent()) {
            if (!objectFile.exists()) {
                Files.createDirectories(obj.getParent());
                Files.createFile(obj);
            }
            OutputStream out = Files.newOutputStream(obj);
            StreamUtils.copy(in, out);
        } catch (IOException e) {
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        }
    }

    @Override
    public void updateObject(S3CallContext callContext, String bucketName, S3Object object) {

    }


    @Override
    public S3Object getObject(S3CallContext callContext, String bucketName, String objectKey) {
        Path bucket = Paths.get(fsrepoBaseUrl, bucketName);
        if (!bucket.toFile().exists())
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        Path object = Paths.get(bucket.toString(), objectKey);
        File objectFile = object.toFile();
        if (!objectFile.exists())
            throw new NoSuchKeyException(objectKey, callContext.getRequestId());
        String username = getUserPrincipal(callContext, object, objectKey);

        try {
            return new S3ObjectImpl(objectKey,
                    new Date(objectFile.lastModified()),
                    bucketName,
                    objectFile.length(),
                    new S3UserImpl(username, username), new S3UserMetadataImpl(), Files.newInputStream(object), getMimeType(object));
        } catch (IOException e) {
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        }
    }

    @Override
    public S3ListBucketResult listBucket(S3CallContext callContext, String bucketName) {
        Integer maxKeys = callContext.getParams().getMaxKeys();
        String marker = callContext.getParams().getMarker();
        String prefix = callContext.getParams().getPrefix() != null ?
                callContext.getParams().getPrefix() : "";

        Path bucket = Paths.get(fsrepoBaseUrl, bucketName);

        try {
            return new S3ListBucketResultImpl(false, bucketName, Files.walk(Paths.get(fsrepoBaseUrl, bucketName))
                    .filter(p -> p.toFile().isFile())
                    .filter(p -> bucket.relativize(p).toString().startsWith(prefix))
                    .limit(maxKeys)
                    .map(path -> {
                                String key = bucket.relativize(path).toString();
                                String owner = getUserPrincipal(callContext, path, key);
                                try {
                                    return new S3ObjectImpl(key,
                                            new Date(path.toFile().lastModified()),
                                            bucketName, path.toFile().length(),
                                            new S3UserImpl(owner, owner),
                                            new S3UserMetadataImpl(),
                                            null, getMimeType(path));
                                } catch (IOException e) {
                                    throw new InternalErrorException(bucketName, callContext.getRequestId());
                                }
                            }
                    ).collect(Collectors.toList()));
        } catch (IOException e) {
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        }
    }

    private MimeType getMimeType(Path path) throws IOException {
        String mime =  Files.probeContentType(path);
        return mime != null ? MimeType.valueOf(mime): MimeTypeUtils.APPLICATION_OCTET_STREAM;
    }

    private String getUserPrincipal(S3CallContext callContext, Path path, String key) {
        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
        UserPrincipal owner;
        try {
            owner = ownerAttributeView.getOwner();
        } catch (IOException e) {
            throw new InternalErrorException(key, callContext.getRequestId());
        }
        return owner.getName();
    }

    @Override
    public void deleteObject(S3CallContext callContext, String bucketName, String objectKey) {
        Path bucket = Paths.get(fsrepoBaseUrl, bucketName);
        if (!bucket.toFile().exists())
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        Path object = Paths.get(bucket.toString(), objectKey);
        if (!object.toFile().exists())
            throw new NoSuchKeyException(objectKey, callContext.getRequestId());

        try {
            Files.delete(object);
        } catch (IOException e) {
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        }
    }

    @Override
    public S3UserMetadata getObjectMetadata(S3CallContext callContext, String bucketName, String objectKey) {
        return null;
    }


}
