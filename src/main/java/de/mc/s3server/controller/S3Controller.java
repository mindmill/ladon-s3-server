/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.controller;

import de.mc.s3server.controller.response.entities.ListAllMyBucketsResult;
import de.mc.s3server.controller.response.mapper.RepositoryMapper;
import de.mc.s3server.entities.api.S3CallContext;
import de.mc.s3server.entities.impl.S3UserImpl;
import de.mc.s3server.repository.api.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by max on 17.02.16.
 */
@RestController
@RequestMapping("${s3.api.base.url}")
public class S3Controller {

    private Repository repository;

    @Autowired
    public S3Controller(Repository repository) {
        this.repository = repository;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    public ListAllMyBucketsResult listAllMyBucketsResult(S3CallContext callContext) {
        return RepositoryMapper.listAllMyBucketsResult(new S3UserImpl("maxid", "max"), repository.listAllBuckets(callContext));
    }

    @RequestMapping(value = "/{bucketName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    public ListAllMyBucketsResult listBucketsResult(S3CallContext callContext, @PathVariable("bucketName") String bucketName) {
        return RepositoryMapper.listAllMyBucketsResult(new S3UserImpl("maxid", "max"), repository.listAllBuckets(callContext));
    }


}
