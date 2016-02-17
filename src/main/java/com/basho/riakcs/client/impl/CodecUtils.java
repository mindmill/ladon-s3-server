/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package com.basho.riakcs.client.impl;

import java.lang.reflect.Method;

public class CodecUtils
{
	public static String encodeBase64(byte[] data) throws Exception
	{
		// Try loading Sun's Base64 encoder implementation
		try
		{
			Class<?> b64Class= ClassLoader.getSystemClassLoader().loadClass("sun.misc.BASE64Encoder");
			if (b64Class != null)
			{
				Method encodeMethod= b64Class.getMethod("encode", new Class[]{byte[].class});
				return (String)encodeMethod.invoke(b64Class.newInstance(), new Object[]{data});
			}
		} catch(ClassNotFoundException cnfe)
		{
		}

		// Try loading the Apache Commons Base64 encoder implementation
		try
		{
			Class<?> b64Class= ClassLoader.getSystemClassLoader().loadClass("org.apache.commons.codec.binary.Base64");
			if (b64Class != null)
			{
				Method encodeMethod= b64Class.getMethod("encodeBase64", new Class[]{byte[].class});
				byte[] encodedData= (byte[])encodeMethod.invoke(b64Class, new Object[]{data});
				return new String(encodedData, "UTF-8");
			}
		} catch(ClassNotFoundException cnfe)
		{
		}

		throw new ClassNotFoundException("Cannot find a recognized Base64 decoder implementation, please include Apache Commons Codec library in your classpath.");
	}

	public static byte[] decodeBase64(String data) throws Exception
	{
		// Try loading Sun's Base64 decoder implementation
		try
		{
			Class<?> b64Class= ClassLoader.getSystemClassLoader().loadClass("sun.misc.BASE64Decoder");
			if (b64Class != null)
			{
				Method decodeMethod= b64Class.getMethod("decodeBuffer", new Class[]{String.class});
				return (byte[])decodeMethod.invoke(b64Class.newInstance(), new Object[]{data});
			}
		} catch(ClassNotFoundException cnfe)
		{
		}

		// Try loading the Apache Commons Base64 decoder implementation
		try
		{
			Class<?> b64Class= ClassLoader.getSystemClassLoader().loadClass("org.apache.commons.codec.binary.Base64");
			if (b64Class != null)
			{
				Method decodeMethod= b64Class.getMethod("decodeBase64", new Class[]{byte[].class});
				return (byte[])decodeMethod.invoke(b64Class, new Object[]{data.getBytes("UTF-8")});
			}
		} catch(ClassNotFoundException cnfe)
		{
		}

		throw new ClassNotFoundException("Cannot find a recognized Base64 decoder implementation, please include Apache Commons Codec library in your classpath.");
	}


}
