/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package com.basho.riakcs.client.impl;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;


public class CommunicationLayer
{
    public enum HttpMethod {GET, HEAD, PUT, DELETE, POST};

    private static final SimpleDateFormat rfc822DateFormat= new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    private String csAccessKey= null;
	private String csSecretKey= null;
	
	private String  csEndpoint= null;
	private boolean useSSL    = true;

	private boolean debugModeEnabled= false;

	
	public CommunicationLayer(String csAccessKey, String csSecretKey, String csEndpoint, boolean useSSL, boolean debugModeEnabled)
	{
		this.csAccessKey     = csAccessKey;
		this.csSecretKey     = csSecretKey;
		this.csEndpoint      = csEndpoint;
		this.useSSL          = useSSL;
		this.debugModeEnabled= debugModeEnabled;
		
		rfc822DateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
	}


	public URL generateCSUrl(String path) throws Exception
	{
		String requestUrl= (useSSL ? "https://" : "http://") + csEndpoint + path;

		return new URL(requestUrl);
	}

	public URL generateCSUrl(String bucketName, String objectKey, Map<String, String> parameters) throws Exception
	{
		// Decide between the default and sub-domain host name formats
		String hostname= null;
		if (isValidDnsName(bucketName))
		{
			hostname= bucketName + "" + csEndpoint;
		} else
		{
			hostname= csEndpoint;
		}

		// Build an initial secure or non-secure URI for the end point.
		String requestUrl= (useSSL ? "https://" : "http://") + hostname;

		// Include the bucket name in the URI except for alternative hostnames
		if (bucketName.length() > 0 && hostname.equals(csEndpoint))
		{
			if(!requestUrl.endsWith("/"))
				requestUrl+= "/";
			
			requestUrl+= URLEncoder.encode(bucketName, "UTF-8");
		}

		// Add object name component to URI if present
		if (objectKey.length() > 0)
		{
			if(!requestUrl.endsWith("/"))
				requestUrl+= "/";
			
			requestUrl+= URLEncoder.encode(objectKey, "UTF-8");
		}

		// Ensure URL includes at least a slash in the path, if nothing else
		if (objectKey.length() == 0 && !hostname.equals(csEndpoint))
		{
			if(!requestUrl.endsWith("/"))
				requestUrl+= "/";
		}

		// Add request parameters to the URI.
		StringBuffer query= new StringBuffer();
		for (Map.Entry<String, String> parameter : parameters.entrySet())
		{
			if (query.length() > 0)
			{
				query.append("&");
			}

			if (parameter.getValue() == null)
			{
				query.append(parameter.getKey());
			} else
			{
				query.append(parameter.getKey() + "=" + URLEncoder.encode(parameter.getValue(), "UTF-8"));
			}
		}
		
		if (query.length() > 0)
		{
			requestUrl+= "?" + query;
		}

		return new URL(requestUrl);
	}

	public HttpURLConnection makeCall(HttpMethod method, URL url) throws Exception
	{
		return makeCallImpl(method, url, true, null, new HashMap<String, String>());
	}

	public HttpURLConnection makeCall(HttpMethod method, URL url, InputStream dataInputStream, Map<String, String> headers) throws Exception
	{
		return makeCallImpl(method, url, true, dataInputStream, headers);
	}

	public HttpURLConnection makeUnsignedCall(HttpMethod method, URL url, InputStream dataInputStream, Map<String, String> headers) throws Exception
	{
		return makeCallImpl(method, url, false, dataInputStream, headers);
	}

	
	private HttpURLConnection makeCallImpl(HttpMethod method, URL url, boolean isSigned, InputStream dataInputStream, Map<String, String> headers) throws Exception
	{
		// Generate request description and signature for Authorization Header
		if (isSigned)
		{
			String signature= generateRestSignature(method, url, headers);
			headers.put("Authorization", "AWS " + csAccessKey + ":" + signature);
		}

		headers.put("Host", url.getHost());

		int redirectCount= 0;
		while (redirectCount < 4) // Repeat requests after a Temporary Redirect
		{
			HttpURLConnection conn= (HttpURLConnection)url.openConnection();

			for (Map.Entry<String, String> header : headers.entrySet())
			{
				if (debugModeEnabled) System.out.println(header.getKey() + " " + header.getValue());
				conn.setRequestProperty(header.getKey(), header.getValue());
			}

			conn.setRequestMethod(method.toString());

			if (method == HttpMethod.PUT || method == HttpMethod.POST)
			{
				// Set streaming mode
				if(headers.containsKey("Content-Length"))
					conn.setFixedLengthStreamingMode(Integer.parseInt(headers.get("Content-Length")));

				if (debugModeEnabled) debugRequest(conn, dataInputStream);

				conn.setDoOutput(true);
				conn.connect();

				if (dataInputStream != null)
				{
					OutputStream outputStream= conn.getOutputStream();
					byte[] buffer= new byte[8192];
					int count= -1;
					while ((count= dataInputStream.read(buffer)) != -1)
					{
						outputStream.write(buffer, 0, count);
					}
					outputStream.close();
				}
			} else {
				if (debugModeEnabled) debugRequest(conn, dataInputStream);

				conn.setRequestProperty("Content-Type", "");
				conn.setDoInput(true);
				conn.connect();
			}

			if (debugModeEnabled) debugResponse(conn);

			try
			{
				int responseCode= conn.getResponseCode();

				// Handle Temporary Redirects
				if (responseCode == 307)
				{
					String location= conn.getHeaderField("Location");
					conn.disconnect();
					url= new URL(location);
					redirectCount+= 1; // Count to prevent infinite redirects

					if (dataInputStream != null)
					{
						dataInputStream.reset();
					}

				} else if (responseCode >= 200 && responseCode < 300)
				{
					if (dataInputStream != null)
					{
						dataInputStream.close();
					}
					
					// .. all is good
					return conn;
				} else
				{
					outputErrorResponse(conn);

					if (dataInputStream != null)
					{
						dataInputStream.close();
					}
					throw new Exception("Received Error: " + responseCode);
				}
			} catch(IOException e)
			{
				throw e;
			} finally
			{
				if (dataInputStream != null)
				{
					dataInputStream.close();
				}
			}
		}

		// We shouldn't never reach this point.
		throw new IllegalStateException("Weird State ..");
	}


	private void debugRequest(HttpURLConnection conn, InputStream dataInputStream) throws Exception
	{
		System.out.println("REQUEST\n=======");
		System.out.println("Method : " + conn.getRequestMethod());

		// Print URI
		String[] portions= conn.getURL().toString().split("&");
		System.out.println("URI    : " + portions[0]);
		for (int i= 1; i < portions.length; i++)
		{
			System.out.println("\t &" + portions[i]);
		}

		// Print Headers
		if (conn.getRequestProperties().size() > 0)
		{
			System.out.println("Headers:");
			for (Map.Entry<String, List<String>> header : conn.getRequestProperties().entrySet())
			{
				System.out.println("  " + header.getKey() + "=" + header.getValue().get(0));
			}
		}

		// Print Request Data
		if (dataInputStream != null && dataInputStream.markSupported())
		{
			System.out.println("Request Body Data:");

			if (conn.getRequestProperties().get("Content-Type").get(0).equals("application/xml"))
			{
				System.out.println(XMLUtils.serializeDocument(XMLUtils.parseToDocument(dataInputStream, false)));
			} else {
				System.out.println(getInputStreamAsString(dataInputStream));
			}
			dataInputStream.reset();
			System.out.println();
		}
	}

	private void debugResponse(HttpURLConnection conn) throws Exception
	{
		System.out.println("\nRESPONSE\n========");
		System.out.println("Status : " + conn.getResponseCode() + " " + conn.getResponseMessage());

		// Print Headers
		if (conn.getHeaderFields().size() > 0)
		{
			System.out.println("Headers:");
			for (Map.Entry<String, List<String>> header : conn.getHeaderFields().entrySet())
			{
				System.out.println("  " + header.getKey() + "=" + header.getValue().get(0));
			}
		}

		System.out.println();

		/*
		 * We cannot print the response body here in the Java implementation as
		 * the HTTP response's input stream cannot be reset, and therefore
		 * cannot be read multiple times. Instead, the response body will be
		 * printed by the methods below that read the response input stream into
		 * a String or Document.
		 */
	}


	private void outputErrorResponse(HttpURLConnection conn) throws UnsupportedEncodingException, IOException
	{
		InputStream inputStream= conn.getErrorStream();
		if(inputStream == null) return;

		BufferedReader reader= new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));

		StringBuffer rawResult= new StringBuffer();
		for (String line= null; (line= reader.readLine()) != null;)
			rawResult.append(line);

		reader.close();
		
		System.out.println("Body:\n  " + rawResult.toString());
		System.out.println("");
	}

	
	private Date currentTime()
	{
		return new Date(System.currentTimeMillis());
	}

	private boolean isValidDnsName(String bucketName)
	{
		// Ensure bucket name is within length constraints
		if (bucketName == null || bucketName.length() > 63 || bucketName.length() < 3)
		{
			return false;
		}

		// Only lower-case letters, numbers, '.' or '-' characters allowed
		if (!Pattern.matches("^[a-z0-9][a-z0-9.-]+$", bucketName))
		{
			return false;
		}

		// Cannot be an IP address (must contain at least one lower-case letter)
		if (!Pattern.matches(".*[a-z].*", bucketName))
		{
			return false;
		}

		// Components of name between '.' characters cannot start or end with
		// '-',
		// and cannot be empty
		String[] fragments= bucketName.split("\\.");
		for (int i= 0; i < fragments.length; i++)
		{
			if (Pattern.matches("^-.*", fragments[i]) || Pattern.matches(".*-$", fragments[i]) || Pattern.matches("^$", fragments[i]))
			{
				return false;
			}
		}

		return true;
	}
    
	private String generateRestSignature(HttpMethod method, URL url, Map<String, String> headers) throws Exception
	{
		// Set mandatory Date header if it is missing
		if (!headers.containsKey("Date"))
		{
			headers.put("Date", rfc822DateFormat.format(currentTime()));
		}

		// Describe main components of REST request. If Content-MD5
		// or Content-Type headers are missing, use an empty string
		StringBuffer requestDescription= new StringBuffer();
		requestDescription.append(method.toString() + "\n" + (headers.containsKey("Content-MD5") ? headers.get("Content-MD5") : "") + "\n"
			+ (headers.containsKey("Content-Type") ? headers.get("Content-Type") : "") + "\n" + headers.get("Date") + "\n");

		// Find any x-amz-* headers and store them in a sorted (Tree) map
		Map<String, String> amzHeaders= new TreeMap<String, String>();
		for (Map.Entry<String, String> header : headers.entrySet())
		{
			if (header.getKey().startsWith("x-amz-"))
			{
				amzHeaders.put(header.getKey().toLowerCase(), header.getValue());
			}
		}
		// Append x-maz-* headers to the description string
		for (Map.Entry<String, String> amzHeader : amzHeaders.entrySet())
		{
			requestDescription.append(amzHeader.getKey() + ":" + amzHeader.getValue() + "\n");
		}

		String path= "";

		// Handle special case of hostname URLs. The bucket
		// portion of alternative hostnames must be included in the request
		// description's URL path.
		String endpointNoPort= csEndpoint;
		if (endpointNoPort.indexOf(':') > 0)
			endpointNoPort= endpointNoPort.substring(0, endpointNoPort.indexOf(':'));

		if (url.getHost().contains('.' + endpointNoPort))
		{

			if (url.getHost().endsWith(endpointNoPort))
			{
				path= "/" + url.getHost().replace('.' + endpointNoPort, "");
			} else
			{
				path= "/" + url.getHost();
			}
			if (url.getPath().equals(""))
			{
				path+= "/";
			}
		}

		// Append the request's URL path to the description
		path+= url.getPath();

		// Ensure the request description's URL path includes at least a slash.
		if (path.length() == 0)
		{
			requestDescription.append("/");
		} else
		{
			requestDescription.append(path);
		}

		// Append special S3 parameters to request description
		if (url.getQuery() != null)
		{
			for (String param : url.getQuery().split("&"))
			{
				if (param.equals("acl") || param.equals("torrent") || param.equals("logging") || param.equals("location"))
				{
					requestDescription.append("?" + param);
				}
			}
		}

		if (debugModeEnabled)
		{
			System.out.println("REQUEST DESCRIPTION\n=======");
			System.out.println(requestDescription.toString().replaceAll("\n", "\\\\n\n"));
			System.out.println();
		}

		// Generate signature
		return generateSignature(requestDescription.toString());
	}

	private String generateSignature(String requestDescription) throws Exception
	{
		// Create an HMAC signing object
		Mac hmac= Mac.getInstance("HmacSHA1");

		// Use your AWS Secret Key as the crypto secret key
		SecretKeySpec secretKey= new SecretKeySpec(csSecretKey.getBytes("UTF-8"), "HmacSHA1");
		hmac.init(secretKey);

		// Compute the signature using the HMAC algorithm
		byte[] signature= hmac.doFinal(requestDescription.getBytes("UTF-8"));

		// Encode the signature bytes into a Base64 string
		return CodecUtils.encodeBase64(signature);
	}
    
	protected String getInputStreamAsString(InputStream is) throws IOException
	{
		StringBuffer responseBody= new StringBuffer();
		BufferedReader reader= new BufferedReader(new InputStreamReader(is));
		String line= null;
		while ((line= reader.readLine()) != null)
		{
			responseBody.append(line + "\n");
		}
		reader.close();

		return responseBody.toString();
	}


}
