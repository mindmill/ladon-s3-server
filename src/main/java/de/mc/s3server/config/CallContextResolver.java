/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.config;

import de.mc.s3server.entities.api.S3CallContext;
import de.mc.s3server.entities.impl.S3CallContextImpl;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by Ralf Ulrich on 14.02.16.
 */
public class CallContextResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterType().equals(S3CallContext.class);
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
        return new S3CallContextImpl(SecurityContextHolder.getContext(), nativeWebRequest.getNativeRequest(HttpServletRequest.class), nativeWebRequest.getNativeResponse(HttpServletResponse.class), nativeWebRequest.getParameterMap());
    }
}
