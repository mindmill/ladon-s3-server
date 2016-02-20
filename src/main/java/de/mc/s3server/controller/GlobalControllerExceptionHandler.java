/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.controller;

import de.mc.s3server.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
@ControllerAdvice
public class GlobalControllerExceptionHandler {

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
    public void handleForbidden() {
    }


    @ResponseStatus(value = HttpStatus.CONFLICT)
    @ExceptionHandler({
            BucketAlreadyExistsException.class,
            BucketAlreadyOwnedByYouException.class,
            BucketNotEmptyException.class,
            InvalidBucketStateException.class,
            OperationAbortedException.class,
            RestoreAlreadyInProgressException.class})
    public void handleConflict() {
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
    public void handleBadRequest() {
    }


    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(InternalErrorException.class)
    public void handleInternalError() {
    }


    @ResponseStatus(value = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
    @ExceptionHandler(InvalidRangeException.class)
    public void handleInvalidRange() {
    }

    @ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(MethodNotAllowedException.class)
    public void handleMethodNotAllowed() {
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
    public void handleNotFound() {
    }

    @ResponseStatus(value = HttpStatus.NOT_IMPLEMENTED)
    @ExceptionHandler(NotImplementedException.class)
    public void handleNotImplemented() {
    }

    @ResponseStatus(value = HttpStatus.PERMANENT_REDIRECT)
    @ExceptionHandler(PermanentRedirectException.class)
    public void handlePermanentRedirect() {
    }

    @ResponseStatus(value = HttpStatus.TEMPORARY_REDIRECT)
    @ExceptionHandler(RedirectException.class)
    public void handleTempRedirect() {
    }

    @ResponseStatus(value = HttpStatus.PRECONDITION_FAILED)
    @ExceptionHandler(PreconditionFailedException.class)
    public void handlePreconditionFailed() {
    }

    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(ServiceUnavailableException.class)
    public void handleServiceUnavailable() {
    }

    @ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(SlowDownException.class)
    public void handleTooManyRequests() {
    }


}
