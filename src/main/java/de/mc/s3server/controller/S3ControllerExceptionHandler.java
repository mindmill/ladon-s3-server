/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.controller;

import de.mc.s3server.controller.response.entities.Error;
import de.mc.s3server.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * General s3server exception to response code mapping
 *
 * @author Ralf Ulrich on 20.02.16.
 */
@ControllerAdvice
public class S3ControllerExceptionHandler {

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler({
            AccessDeniedException.class,
            CrossLocationLoggingProhibitedException.class,
            InvalidAccessKeyIdException.class,
            InvalidObjectStateException.class,
            InvalidPayerException.class,
            InvalidSecurityException.class,
            NotSignedUpException.class,
            RequestTimeTooSkewedException.class,
            SignatureDoesNotMatchException.class})
    @ResponseBody
    public Error handleForbidden(S3ServerException e) {
        return new Error(e);
    }


    @ResponseStatus(value = HttpStatus.CONFLICT)
    @ExceptionHandler({
            BucketAlreadyExistsException.class,
            BucketAlreadyOwnedByYouException.class,
            BucketNotEmptyException.class,
            InvalidBucketStateException.class,
            OperationAbortedException.class,
            RestoreAlreadyInProgressException.class})
    @ResponseBody
    public Error handleConflict(S3ServerException e) {
        return new Error(e);
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            AmbiguousGrantByEmailAddressException.class,
            BadDigestException.class,
            CredentialsNotSupportedException.class,
            EntityTooSmallException.class,
            EntityTooLargeException.class,
            ExpiredTokenException.class,
            IllegalVersioningConfigurationException.class,
            IncompleteBodyException.class,
            IncorrectNumberOfFilesInPostRequestException.class,
            InlineDataTooLargeException.class,
            InvalidArgumentException.class,
            InvalidDigestException.class,
            InvalidEncryptionAlgorithmErrorException.class,
            InvalidLocationConstraintException.class,
            InvalidPartException.class,
            InvalidPartOrderException.class,
            InvalidPolicyDocumentException.class,
            InvalidRequestException.class,
            InvalidSOAPRequestException.class,
            InvalidStorageClassException.class,
            InvalidTargetBucketForLoggingException.class,
            InvalidURIException.class,
            KeyTooLongException.class,
            InvalidTokenException.class,
            MalformedACLErrorException.class,
            MalformedPOSTRequestException.class,
            MalformedXMLException.class,
            MaxMessageLengthExceededException.class,
            MaxPostPreDataLengthExceededException.class,
            MetadataTooLargeException.class,
            MissingAttachmentException.class,
            MissingRequestBodyException.class,
            MissingSecurityElementException.class,
            MissingSecurityHeaderException.class,
            RequestIsNotMultiPartContentException.class,
            RequestTimeoutException.class,
            RequestTorrentOfBucketException.class,
            TokenRefreshRequiredException.class,
            TooManyBucketsException.class,
            UnexpectedContentException.class,
            UnresolvableGrantByEmailAddressException.class,
            UserKeyMustBeSpecifiedException.class
    })
    @ResponseBody
    public Error handleBadRequest(S3ServerException e) {
        return new Error(e);
    }


    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(InternalErrorException.class)
    @ResponseBody
    public Error handleInternalError(S3ServerException e) {
        return new Error(e);
    }


    @ResponseStatus(value = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
    @ExceptionHandler(InvalidRangeException.class)
    @ResponseBody
    public Error handleInvalidRange(S3ServerException e) {
        return new Error(e);
    }

    @ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(MethodNotAllowedException.class)
    @ResponseBody
    public Error handleMethodNotAllowed(S3ServerException e) {
        return new Error(e);
    }

    @ResponseStatus(value = HttpStatus.LENGTH_REQUIRED)
    @ExceptionHandler(MissingContentLengthException.class)
    public void handleLengthRequired() {
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler({
            NoSuchBucketException.class,
            NoSuchKeyException.class,
            NoSuchLifecycleConfigurationException.class,
            NoSuchUploadException.class,
            NoSuchVersionException.class,
            NoSuchBucketPolicyException.class
    })
    @ResponseBody
    public Error handleNotFound(S3ServerException e) {
        return new Error(e);
    }

    @ResponseStatus(value = HttpStatus.NOT_IMPLEMENTED)
    @ExceptionHandler(NotImplementedException.class)
    @ResponseBody
    public Error handleNotImplemented(S3ServerException e) {
        return new Error(e);
    }

    @ResponseStatus(value = HttpStatus.PERMANENT_REDIRECT)
    @ExceptionHandler(PermanentRedirectException.class)
    public Error handlePermanentRedirect(S3ServerException e) {
        return new Error(e);
    }

    @ResponseStatus(value = HttpStatus.TEMPORARY_REDIRECT)
    @ExceptionHandler(RedirectException.class)
    @ResponseBody
    public Error handleTempRedirect(S3ServerException e) {
        return new Error(e);
    }

    @ResponseStatus(value = HttpStatus.PRECONDITION_FAILED)
    @ExceptionHandler(PreconditionFailedException.class)
    @ResponseBody
    public Error handlePreconditionFailed(S3ServerException e) {
        return new Error(e);
    }

    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseBody
    public Error handleServiceUnavailable(S3ServerException e) {
        return new Error(e);
    }

    @ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(SlowDownException.class)
    @ResponseBody
    public Error handleTooManyRequests(S3ServerException e) {
        return new Error(e);
    }


}
