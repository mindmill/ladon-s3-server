/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */
package com.basho.riakcs.client.api;

public class RiakCSException extends Exception
{
	private static final long serialVersionUID= 3652091041677170597L;

	public RiakCSException(Exception e)
	{
		super(e);
	}

	public RiakCSException(String errorMessage)
	{
		super(errorMessage);
	}

}
