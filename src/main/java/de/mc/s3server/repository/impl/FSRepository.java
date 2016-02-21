package de.mc.s3server.repository.impl;

import de.mc.s3server.entities.api.*;
import de.mc.s3server.entities.impl.*;
import de.mc.s3server.exceptions.InternalErrorException;
import de.mc.s3server.exceptions.NoSuchBucketException;
import de.mc.s3server.repository.api.Repository;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Ralf Ulrich on 21.02.16.
 */
@org.springframework.stereotype.Repository
public class FSRepository implements Repository {

    @Value("${s3server.fsrepo.baseurl}")
    private String fsrepoBaseUrl;


    @Override
    public List<S3Bucket> listAllBuckets(S3CallContext callContext) {
        try {
            return Files.list(Paths.get(fsrepoBaseUrl)).filter(path1 -> path1.toFile().isDirectory()).map(
                    path -> new S3BucketImpl(path.getFileName().toString(), new Date(path.toFile().lastModified()))
            ).collect(Collectors.toList());
        } catch (IOException e) {
            throw new NoSuchBucketException(null, callContext.getRequestId());
        }
    }

    @Override
    public void createBucket(S3CallContext callContext, S3Bucket bucket) {

    }

    @Override
    public void updateBucket(S3CallContext callContext, S3Bucket bucket) {

    }

    @Override
    public S3Bucket getBucket(S3CallContext callContext, String bucketName) {
        return null;
    }

    @Override
    public void deleteBucket(S3CallContext callContext, String bucketName) {

    }

    @Override
    public void createObject(S3CallContext callContext, String bucketName, S3Object object) {

    }

    @Override
    public void updateObject(S3CallContext callContext, String bucketName, S3Object object) {

    }

    @Override
    public void putContent(S3CallContext callContext, String bucketNAme, String objectKey, InputStream contentStream) {

    }

    @Override
    public S3Object getObject(S3CallContext callContext, String bucketName, String objectKey) {
        return null;
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
                                FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
                                UserPrincipal owner = null;
                                try {
                                    owner = ownerAttributeView.getOwner();
                                } catch (IOException e) {
                                    throw new InternalErrorException(key, callContext.getRequestId());
                                }
                                return new S3ObjectImpl(key,
                                        new Date(path.toFile().lastModified()),
                                        bucketName, path.toFile().length(),
                                        new S3UserImpl(owner.toString(), owner.getName()),
                                        new S3MetadataImpl(),
                                        null);
                            }
                    ).collect(Collectors.toList()));
        } catch (IOException e) {
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        }
    }

    @Override
    public void deleteObject(S3CallContext callContext, String bucketName, String objectKey) {

    }

    @Override
    public S3Metadata getObjectMetadata(S3CallContext callContext, String bucketName, String objectKey) {
        return null;
    }

    @Override
    public void setBucketACL(S3CallContext callContext, String buckeName, S3ACL bucketACL) {

    }

    @Override
    public S3ACL getBucketACL(S3CallContext callContext, String bucketName) {
        return null;
    }

    @Override
    public void setObjectACL(S3CallContext callContext, String bucketName, String objectKey) {

    }

    @Override
    public S3ACL getObjectACL(S3CallContext callContext, String bucketName, String objectKey) {
        return null;
    }
}
