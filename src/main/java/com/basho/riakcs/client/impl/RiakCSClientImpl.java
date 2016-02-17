/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package com.basho.riakcs.client.impl;

import com.basho.riakcs.client.api.RiakCSClient;
import com.basho.riakcs.client.api.RiakCSException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;


public class RiakCSClientImpl
{
    private final static Map<String, String> EMPTY_STRING_MAP= new HashMap<String, String>();

    private static final SimpleDateFormat iso8601DateFormat= new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    
    private String csAccessKey= null;
	private String csSecretKey= null;
	
	private String  csEndpoint= "s3.amazonaws.com";
	private boolean useSSL    = true;

	private boolean debugModeEnabled= false;
	

	public RiakCSClientImpl(String csAccessKey, String csSecretKey, String csEndpoint, boolean useSSL)
	{
		this.csAccessKey= csAccessKey;
		this.csSecretKey= csSecretKey;
		this.csEndpoint = csEndpoint;
		this.useSSL     = useSSL;
	}

	
	public RiakCSClientImpl(String csAccessKey, String csSecretKey)
	{
		this.csAccessKey= csAccessKey;
		this.csSecretKey= csSecretKey;
	}

	
	public void enableDebugOutput()
	{
		debugModeEnabled= true;
	}


	public boolean endpointIsS3()
	{
		return true;
	}


	public JsonObject createUser(String fullname, String emailAddress) throws RiakCSException
	{
		if(endpointIsS3()) throw new RiakCSException("Not Supported on AWS S3");

		JsonObject result= null;

		try {
			JsonObject postData= new JsonObject();
			postData.addProperty("email", emailAddress);
			postData.addProperty("name", fullname);
			
			InputStream dataInputStream= new ByteArrayInputStream(postData.toString().getBytes("UTF-8"));
	
			Map<String, String> headers= new HashMap<String, String>();
			headers.put("Content-Type", "application/json");
			headers.put("Content-Length", String.valueOf(dataInputStream.available()));
			
			CommunicationLayer comLayer= getCommunicationLayer();
			
			URL url= comLayer.generateCSUrl("", "user", EMPTY_STRING_MAP);		
			HttpURLConnection connection= comLayer.makeCall(CommunicationLayer.HttpMethod.POST, url, dataInputStream, headers);
			InputStreamReader inputStreamReader= new InputStreamReader(connection.getInputStream(), "UTF-8");
			
			result= new JsonParser().parse(inputStreamReader).getAsJsonObject();

		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
		
		return result;
	}


	public static enum UserListMode{ ALL, ENABLED_ONLY, DISABLED_ONLY };
	
	public JsonObject listUsers(UserListMode listMode) throws RiakCSException
	{
		if(endpointIsS3()) throw new RiakCSException("Not Supported on AWS S3");

		JsonObject result= null;
		
		try {
			Map<String, String> headers= new HashMap<String, String>();
			headers.put("Accept", "application/json");
	
			String filterArgument= "";
			if(listMode == UserListMode.ENABLED_ONLY)
				filterArgument= "?status=enabled";
			else if(listMode == UserListMode.DISABLED_ONLY)
				filterArgument= "?status=disabled";

			CommunicationLayer comLayer= getCommunicationLayer();
			
			URL url= comLayer.generateCSUrl("/riak-cs/users"+filterArgument);
			HttpURLConnection connection= comLayer.makeCall(CommunicationLayer.HttpMethod.GET, url, null, headers);
			InputStreamReader inputStreamReader= new InputStreamReader(connection.getInputStream(), "UTF-8");
			
			JsonArray userList= new JsonArray();
	
			BufferedReader reader= new BufferedReader(inputStreamReader);
			for (String line; (line= reader.readLine()) != null;)
			{
				if(line.startsWith("["))
				{
					JsonArray aUserlist= new JsonParser().parse(line).getAsJsonArray();
					userList.addAll(aUserlist);
                }
			}
			  
			result= new JsonObject();
			result.add("userlist", userList);
		
		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
		
		return result;
	}


	public JsonObject getUserInfo(String key_id) throws RiakCSException
	{
		if(endpointIsS3()) throw new RiakCSException("Not Supported on AWS S3");

		JsonObject result= null;
		
		try {
			Map<String, String> headers= new HashMap<String, String>();
			headers.put("Accept", "application/json");

			CommunicationLayer comLayer= getCommunicationLayer();
			
			URL url= comLayer.generateCSUrl("/riak-cs/user/"+key_id);		
			HttpURLConnection connection= comLayer.makeCall(CommunicationLayer.HttpMethod.GET, url, null, headers);
			InputStreamReader inputStreamReader= new InputStreamReader(connection.getInputStream(), "UTF-8");
			
			result= new JsonParser().parse(inputStreamReader).getAsJsonObject();
		
		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
		
		return result;
	}

	
	public JsonObject getMyUserInfo() throws RiakCSException
	{
		if(endpointIsS3()) throw new RiakCSException("Not Supported on AWS S3");

		JsonObject result= null;
		
		try {
			Map<String, String> headers= new HashMap<String, String>();
			headers.put("Accept", "application/json");

			CommunicationLayer comLayer= getCommunicationLayer();
			
			URL url= comLayer.generateCSUrl("/riak-cs/user");		
			HttpURLConnection connection= comLayer.makeCall(CommunicationLayer.HttpMethod.GET, url, null, headers);
			InputStreamReader inputStreamReader= new InputStreamReader(connection.getInputStream(), "UTF-8");
			
			result= new JsonParser().parse(inputStreamReader).getAsJsonObject();
		
		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
		
		return result;
	}


	public void enableUser(String key_id) throws RiakCSException
	{
		enableDisableUser(key_id, true);
	}

	public void disableUser(String key_id) throws RiakCSException
	{
		enableDisableUser(key_id, false);
	}

	private void enableDisableUser(String key_id, boolean enable) throws RiakCSException
	{
		if(endpointIsS3()) throw new RiakCSException("Not Supported on AWS S3");

		try {
			JsonObject postData= new JsonObject();
			if(enable)
				postData.addProperty("status", "enabled");
			else
				postData.addProperty("status", "disabled");
			
			InputStream dataInputStream= new ByteArrayInputStream(postData.toString().getBytes("UTF-8"));
	
			Map<String, String> headers= new HashMap<String, String>();
			headers.put("Content-Type", "application/json");
			headers.put("Content-Length", String.valueOf(dataInputStream.available()));
			
			CommunicationLayer comLayer= getCommunicationLayer();
			
			URL url= comLayer.generateCSUrl("/riak-cs/user/"+key_id);		
			comLayer.makeCall(CommunicationLayer.HttpMethod.PUT, url, dataInputStream, headers);

		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
	}


	public void createBucket(String bucketName) throws RiakCSException
	{
		CommunicationLayer comLayer= getCommunicationLayer();

		try {
			URL url= comLayer.generateCSUrl(bucketName, "", EMPTY_STRING_MAP);
			comLayer.makeCall(CommunicationLayer.HttpMethod.PUT, url);
		
		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
	}


	public JsonObject listBuckets() throws RiakCSException
	{
		CommunicationLayer comLayer= getCommunicationLayer();

		JsonObject bucketList= null;
		
		try {
			URL url= comLayer.generateCSUrl("", "", EMPTY_STRING_MAP);
			HttpURLConnection conn= comLayer.makeCall(CommunicationLayer.HttpMethod.GET, url);
			Document xmlDoc= XMLUtils.parseToDocument(conn.getInputStream(), debugModeEnabled);
	
			bucketList= new JsonObject();
            JsonArray buckets = new JsonArray();
			for (Node node : XMLUtils.xpathToNodeList("//Buckets/Bucket", xmlDoc))
			{
				JsonObject bucket= new JsonObject();
				bucket.addProperty("name", XMLUtils.xpathToContent("Name", node));
				bucket.addProperty("creationDate", XMLUtils.xpathToContent("CreationDate", node));
                buckets.add(bucket);
            }
	
            bucketList.add("bucketList", buckets);
			JsonObject owner= new JsonObject();
			owner.addProperty("id", XMLUtils.xpathToContent("//Owner/ID", xmlDoc));
			owner.addProperty("displayName", XMLUtils.xpathToContent("//Owner/DisplayName", xmlDoc));
	
			bucketList.add("owner", owner);

		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
		
		return bucketList;
	}
    
	public boolean isBucketAccessible(String bucketName) throws RiakCSException
	{
		boolean result= false;
		
		try {
			CommunicationLayer comLayer= getCommunicationLayer();
	
			URL url= comLayer.generateCSUrl(bucketName, "", EMPTY_STRING_MAP);
			HttpURLConnection conn= comLayer.makeCall(CommunicationLayer.HttpMethod.HEAD, url);

			for (String headerName : conn.getHeaderFields().keySet())
			{
				// .. just check for something ..
				if(headerName != null && headerName.equals("Server"))
					result= true;
			}
			
		} catch(Exception e)
		{
			if(e.getMessage().contains("404")) return false;
			if(e.getMessage().contains("403")) return false;

			throw new RiakCSException(e);
		}
	
		return result;
	}

	public void deleteBucket(String bucketName) throws RiakCSException
	{
		try {
			CommunicationLayer comLayer= getCommunicationLayer();
	
			URL url= comLayer.generateCSUrl(bucketName, "", EMPTY_STRING_MAP);
			comLayer.makeCall(CommunicationLayer.HttpMethod.DELETE, url);
			
		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
	}


	public void createObject(String bucketName, String objectKey, InputStream dataInputStream, Map<String, String> headers, Map<String, String> metadata) throws RiakCSException
	{
		createObject(bucketName, objectKey, dataInputStream, headers, metadata, null);
	}

	
	public void createObject(String bucketName, String objectKey, InputStream dataInputStream, Map<String, String> headers, Map<String, String> metadata, String policy) throws RiakCSException
	{
		try {
			if (headers == null)  headers= new HashMap<String, String>();
			
			if (!headers.containsKey("Content-Length")) headers.put("Content-Length", String.valueOf(dataInputStream.available()));
			if (!headers.containsKey("Content-Type"))   headers.put("Content-Type", "application/octet-stream");
	
			// Add user-meta info
			if (metadata != null)
			{
				for (Map.Entry<String, String> metadataHeader : metadata.entrySet())
				{
					headers.put("x-amz-meta-" + metadataHeader.getKey(), metadataHeader.getValue());
				}
			}
	
			CommunicationLayer comLayer= getCommunicationLayer();
			
			URL url= comLayer.generateCSUrl(bucketName, objectKey, EMPTY_STRING_MAP);
			comLayer.makeCall(CommunicationLayer.HttpMethod.PUT, url, dataInputStream, headers);
			
		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
	}

	
	public JsonObject getObject(String bucketName, String objectKey) throws RiakCSException
	{
		return getObject(bucketName, objectKey, null);
	}

	public JsonObject getObject(String bucketName, String objectKey, OutputStream dataOutputStream) throws RiakCSException
	{
		JsonObject object= null;
		
		try {
			CommunicationLayer comLayer= getCommunicationLayer();
			
			URL url= comLayer.generateCSUrl(bucketName, objectKey, EMPTY_STRING_MAP);
			HttpURLConnection conn= comLayer.makeCall(CommunicationLayer.HttpMethod.GET, url, null, EMPTY_STRING_MAP);
	
			object= extractMetaInfoForObject(objectKey, conn);
	
			// Download data
			if (dataOutputStream != null)
			{
				InputStream inputStream= conn.getInputStream();
				byte[] buffer= new byte[8192];
				int count= -1;
				while ((count= inputStream.read(buffer)) != -1)
				{
					dataOutputStream.write(buffer, 0, count);
				}
				dataOutputStream.close();
				inputStream.close();
			} else
			{
				object.addProperty("body", getInputStreamAsString(conn.getInputStream()));
			}
		
		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
		
		return object;
	}


	public JsonObject getObjectInfo(String bucketName, String objectKey) throws RiakCSException
	{
		JsonObject object= null;
		
		try {
			CommunicationLayer comLayer= getCommunicationLayer();
			
			URL url= comLayer.generateCSUrl(bucketName, objectKey, EMPTY_STRING_MAP);
			HttpURLConnection conn= comLayer.makeCall(CommunicationLayer.HttpMethod.HEAD, url, null, EMPTY_STRING_MAP);
	
			object= extractMetaInfoForObject(objectKey, conn);
		
		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}

		return object;
	}


	private JsonObject extractMetaInfoForObject(String objectKey, HttpURLConnection conn) 
	{
		Map<String, String> responseHeaders= new HashMap<String, String>();
		JsonObject metadata= new JsonObject();

		for (String headerName : conn.getHeaderFields().keySet())
		{
			if (headerName == null)
				continue;

			if (headerName.startsWith("x-amz-meta"))
			{
				metadata.addProperty(headerName.substring(11), conn.getHeaderFields().get(headerName).get(0));
			} else {
				responseHeaders.put(headerName, conn.getHeaderFields().get(headerName).get(0));
			}
		}

		JsonObject object= new JsonObject();
		object.addProperty("key", objectKey);
		object.addProperty("etag", responseHeaders.get("ETag"));
		object.addProperty("lastModified", responseHeaders.get("Last-Modified"));
		object.addProperty("size", responseHeaders.get("Content-Length"));
		object.addProperty("contenttype", responseHeaders.get("Content-Type"));
		object.add("metadata", metadata);
		return object;
	}


	public JsonObject listObjects(String bucketName) throws RiakCSException
	{
		return listObjects(bucketName, true);
	}

	public JsonObject listObjects(String bucketName, boolean extendedList) throws RiakCSException
	{
		//TODO switch to more streaming type of mode
		JsonObject result= new JsonObject();
		
		try {
			result.addProperty("bucketName", bucketName);
			
			boolean isTruncated= true;
			while (isTruncated)
			{
				CommunicationLayer comLayer= getCommunicationLayer();
				
				URL url= comLayer.generateCSUrl(bucketName, "", EMPTY_STRING_MAP);
				HttpURLConnection conn= comLayer.makeCall(CommunicationLayer.HttpMethod.GET, url);
				
				JsonArray objectList = new JsonArray();
				
				Document xmlDoc= XMLUtils.parseToDocument(conn.getInputStream(), debugModeEnabled);
				List<Node> nodeList= XMLUtils.xpathToNodeList("//Contents", xmlDoc);
				for (Node node : nodeList)
				{
					JsonObject object = new JsonObject();
					object.addProperty("key", XMLUtils.xpathToContent("Key", node));
					if (extendedList)
					{
						object.addProperty("size", XMLUtils.xpathToContent("Size", node));
						object.addProperty("lastModified", XMLUtils.xpathToContent("LastModified", node));
						object.addProperty("etag", XMLUtils.xpathToContent("ETag", node));
						
						JsonObject owner= new JsonObject();
						owner.addProperty("id", XMLUtils.xpathToContent("Owner/ID", node));
						owner.addProperty("displayName", XMLUtils.xpathToContent("Owner/DisplayName", node));
						object.add("owner", owner);
					}
					
					objectList.add(object);
				}

                result.add("objectList", objectList);
				isTruncated= "true".equals(XMLUtils.xpathToContent("//IsTruncated", xmlDoc));				
			}

		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
		
		return result;
	}

	public void deleteObject(String bucketName, String objectKey) throws RiakCSException
	{
		try {
			CommunicationLayer comLayer= getCommunicationLayer();
			
	        URL url= comLayer.generateCSUrl(bucketName, objectKey, EMPTY_STRING_MAP);
	        comLayer.makeCall(CommunicationLayer.HttpMethod.DELETE, url);

		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
	}

	public void setCannedACLForBucket(String bucketName, String cannedAcl) throws RiakCSException
	{
		setCannedACLImplt(bucketName, "", cannedAcl);
	}

	public void setCannedACLForObject(String bucketName, String objectKey, String cannedAcl) throws RiakCSException
	{
		setCannedACLImplt(bucketName, objectKey, cannedAcl);
	}

	public void setCannedACLImplt(String bucketName, String objectKey, String cannedAcl) throws RiakCSException
	{
		try {
			CommunicationLayer comLayer= getCommunicationLayer();
	
			Map<String, String> parameters= new HashMap<String, String>();
			parameters.put("acl", null);
			URL url= comLayer.generateCSUrl(bucketName, objectKey, parameters);
	
			Map<String, String> headers= new HashMap<String, String>();
			headers.put("x-amz-acl", cannedAcl);
			comLayer.makeCall(CommunicationLayer.HttpMethod.PUT, url, null, headers);

		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
	}

	public void addAdditionalACLToBucket(String bucketName, String emailAddress, RiakCSClient.Permission permission) throws RiakCSException
	{
		try {
			JsonObject oldACL= getACLForBucket(bucketName);
			
			JsonObject newGrant= new JsonObject();
			JsonObject grantee= new JsonObject();
			grantee.addProperty("emailAddress", emailAddress);
			newGrant.add("grantee", grantee);
			newGrant.addProperty("permission", permission.toString());
	
			oldACL.get("grantsList").getAsJsonArray().add(newGrant);
	
			addAdditionalACLImpl(bucketName, "", oldACL);

		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
	}

	public void addAdditionalACLToObject(String bucketName, String objectKey, String emailAddress, RiakCSClient.Permission permission) throws RiakCSException
	{
		try {
			JsonObject oldACL= getACLForObject(bucketName, objectKey);
			
			JsonObject newGrant= new JsonObject();
			JsonObject grantee= new JsonObject();
			grantee.addProperty("emailAddress", emailAddress);
			newGrant.add("grantee", grantee);
			newGrant.addProperty("permission", permission.toString());
	
            oldACL.get("grantsList").getAsJsonArray().add(newGrant);
            
			addAdditionalACLImpl(bucketName, objectKey, oldACL);

		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
	}
	
	private void addAdditionalACLImpl(String bucketName, String objectKey, JsonObject acl) throws Exception
	{
		StringBuilder aclText= new StringBuilder();

		aclText.append("<AccessControlPolicy>");
		aclText.append("<Owner><ID>").append(acl.getAsJsonObject("owner").get("id").getAsString()).append("</ID>");
		aclText.append("<DisplayName>").append(acl.getAsJsonObject("owner").get("displayName").getAsString()).append("</DisplayName></Owner>");
		aclText.append("<AccessControlList>");

		JsonArray grantsList= acl.getAsJsonArray("grantsList");
		for (int pt= 0; pt < grantsList.size(); pt++)
		{
			JsonObject grant= grantsList.get(pt).getAsJsonObject();

			aclText.append("<Grant><Permission>").append(grant.getAsJsonObject("permission").getAsString()).append("</Permission>");
			aclText.append("<Grantee");
			aclText.append(" ").append("xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'");
			
			if (grant.getAsJsonObject("grantee").has("id"))
			{
				aclText.append(" xsi:type='CanonicalUser'>");
				aclText.append("<ID>").append(grant.getAsJsonObject("grantee").get("id").getAsString()).append("</ID>");
			} else
			{
				aclText.append(" xsi:type='AmazonCustomerByEmail'>");
				aclText.append("<EmailAddress>").append(grant.getAsJsonObject("grantee").get("emailAddress").getAsString()).append("</EmailAddress>");
			}
			
			aclText.append("</Grantee>");
			aclText.append("</Grant>");
		}
		aclText.append("</AccessControlList>");
		aclText.append("</AccessControlPolicy>");

		Map<String, String> parameters= new HashMap<String, String>();
		parameters.put("acl", null);

		InputStream dataInputStream= new ByteArrayInputStream(aclText.toString().getBytes("UTF-8"));

		Map<String, String> headers= new HashMap<String, String>();
		headers.put("Content-Type", "application/xml");
		
		CommunicationLayer comLayer= getCommunicationLayer();
		
        URL url= comLayer.generateCSUrl(bucketName, objectKey, parameters);
        comLayer.makeCall(CommunicationLayer.HttpMethod.PUT, url, dataInputStream, headers);
	}

	public JsonObject getACLForBucket(String bucketName) throws RiakCSException
	{
		JsonObject result= null;
		
		try {
			result= getAclImpl(bucketName, "");

		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
		
		return result;
	}

	public JsonObject getACLForObject(String bucketName, String objectKey) throws RiakCSException
	{
		JsonObject result= null;
		
		try {
			result= getAclImpl(bucketName, objectKey);
			
		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
		
		return result;
	}

	private JsonObject getAclImpl(String bucketName, String objectKey) throws Exception
	{
		Map<String, String> parameters= new HashMap<String, String>();
		parameters.put("acl", null);

		CommunicationLayer comLayer= getCommunicationLayer();

		URL url= comLayer.generateCSUrl(bucketName, objectKey, parameters);
		HttpURLConnection conn= comLayer.makeCall(CommunicationLayer.HttpMethod.GET, url);

		Document xmlDoc= XMLUtils.parseToDocument(conn.getInputStream(), debugModeEnabled);
		JsonObject acl= new JsonObject();
        JsonArray grantsList = new JsonArray();

		for (Node grantNode : XMLUtils.xpathToNodeList("//Grant", xmlDoc))
		{
			JsonObject grant= new JsonObject();
			grant.addProperty("permission", XMLUtils.xpathToContent("Permission", grantNode));

			String type= XMLUtils.xpathToContent("Grantee/@type", grantNode);

			JsonObject grantee= new JsonObject();
			grantee.addProperty("type", type);

			if (type.equals("Group"))
			{
				grantee.addProperty("uri", XMLUtils.xpathToContent("Grantee/URI", grantNode));
			} else
			{
				grantee.addProperty("id",XMLUtils.xpathToContent("Grantee/ID", grantNode));
				grantee.addProperty("displayName", XMLUtils.xpathToContent("Grantee/DisplayName", grantNode));
			}
			grant.add("grantee", grantee);
			grantsList.add(grant);
		}
		acl.add("grantsList", grantsList);
		JsonObject owner= new JsonObject();
		owner.addProperty("id", XMLUtils.xpathToContent("//Owner/ID", xmlDoc));
		owner.addProperty("displayName", XMLUtils.xpathToContent("//Owner/DisplayName", xmlDoc));
		acl.add("owner", owner);
		return acl;
	}

	public JsonObject getAccessStatistic(String keyForUser, int howManyHrsBack) throws RiakCSException
	{		
		if(endpointIsS3()) throw new RiakCSException("Not supported by AWS S3");

		JsonObject result= null;
		
		try {
			iso8601DateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
			
			Date endTime  = new Date(System.currentTimeMillis());
			Date startTime= new Date(System.currentTimeMillis()-(howManyHrsBack*60*60*1000));
	
			StringBuilder path = new StringBuilder();
			path.append("/usage");
			path.append("/");
			path.append(keyForUser);
			path.append("/aj");
			path.append("/");
			path.append(iso8601DateFormat.format(startTime));
			path.append("/");
			path.append(iso8601DateFormat.format(endTime));
	
			CommunicationLayer comLayer= getCommunicationLayer();
	
			URL url= comLayer.generateCSUrl(path.toString());
			HttpURLConnection connection= comLayer.makeCall(CommunicationLayer.HttpMethod.GET, url, null, EMPTY_STRING_MAP);
	
			InputStreamReader inputStreamReader= new InputStreamReader(connection.getInputStream(), "UTF-8");
			result= new JsonParser().parse(inputStreamReader).getAsJsonObject();
			result= result.getAsJsonObject("Access");

		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}

		return result;
	}

	public JsonObject getStorageStatistic(String keyForUser, int howManyHrsBack) throws RiakCSException
	{
		if(endpointIsS3()) throw new RiakCSException("Not supported by AWS S3");

		JsonObject result= null;
		
		try {
			iso8601DateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
			
			Date endTime  = new Date(System.currentTimeMillis());
			Date startTime= new Date(System.currentTimeMillis()-(howManyHrsBack*60*60*1000));
	
			StringBuilder path= new StringBuilder();
			path.append("/usage");
			path.append("/");
			path.append(keyForUser);
			path.append("/bj");
			path.append("/");
			path.append(iso8601DateFormat.format(startTime));
			path.append("/");
			path.append(iso8601DateFormat.format(endTime));
	
			CommunicationLayer comLayer= getCommunicationLayer();
	
			URL url= comLayer.generateCSUrl(path.toString());
			HttpURLConnection connection= comLayer.makeCall(CommunicationLayer.HttpMethod.GET, url, null, EMPTY_STRING_MAP);
	
			InputStreamReader inputStreamReader= new InputStreamReader(connection.getInputStream(), "UTF-8");
			result= new JsonParser().parse(inputStreamReader).getAsJsonObject();
			result= result.getAsJsonObject("Storage");

		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}

		return result;
	}


	public void removeContentOfBucket(String bucketName) throws RiakCSException
	{
		
        JsonObject response= listObjects(bucketName, false);
        JsonArray resultList= response.getAsJsonArray("objectList");

//			if (debugModeEnabled) System.out.println("Number of Objects to delete: "+ resultList.length() + "\n");
        System.out.println("Number of Objects to delete: "+ resultList.size() + "\n");

        for(int pt=0; pt < resultList.size(); pt++)
        {
            String key= resultList.get(pt).getAsJsonObject().get("key").getAsString();
            deleteObject(bucketName, key);
        }
	}


	public void uploadContentOfDirectory(File fromDirectory, String toBucket) throws RiakCSException
	{
		Set<String> objectNames= new HashSet<String>();

		try {
			uploadContentOfDirectoryImpl(fromDirectory, toBucket, "", objectNames);
			System.out.println("Number of Objects uploaded: " + objectNames.size());

		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
	}

	private void uploadContentOfDirectoryImpl(File fromDirectory, String toBucket, String subFolderPath, Set<String> objectNames) throws Exception
	{
		if (fromDirectory.isDirectory() == false) throw new RiakCSException(fromDirectory + " is not a valid directory");
		
		File[] folderContent= fromDirectory.listFiles();

		for(File item : folderContent)
		{
			if(item.isHidden()) continue;

			if(item.isDirectory())
			{
				uploadContentOfDirectoryImpl(item, toBucket, subFolderPath+fixName(item.getName())+'/', objectNames);
			} else {
				String objectName= subFolderPath+fixName(item.getName());
				
				if(objectNames.contains(objectName)) throw new RiakCSException("ObjectName/Key conflict: " + objectName);
				objectNames.add(objectName);

				FileInputStream inputStream= new FileInputStream(item);
				createObject(toBucket, objectName, inputStream, null, null);
				inputStream.close();
			}
		}

	}

	private String fixName(String name) throws RiakCSException
	{
		StringBuilder result= new StringBuilder();

		for(int pt= 0; pt < name.length(); pt++)
		{
			char chr= name.charAt(pt);
			if(Character.isLetter(chr) || Character.isDigit(chr) || chr == '.' || chr == '-' || chr == '_' || chr == '/')
				result.append(chr);
		}

		if(result.length() == 0) throw new RiakCSException("Funny name: " + name);
		
		return result.toString();
	}


	public static void copyBucketBetweenSystems(RiakCSClient fromSystem, String fromBucket, RiakCSClient toSystem, String toBucket) throws RiakCSException
	{
		try
		{
			JsonObject response= fromSystem.listObjectNames(fromBucket);
			JsonArray resultList= response.getAsJsonArray("objectList");
	
			System.out.println("Number of Objects to transfer: "+ resultList.size() + "\n");
			
			for(int pt=0; pt < resultList.size(); pt++)
			{
				String key = resultList.get(pt).getAsJsonObject().get("key").getAsString();
				File tempFile= File.createTempFile("cscopy-", ".bin");
				
				//Retrieve Object
				FileOutputStream outputStream= new FileOutputStream(tempFile);
				JsonObject objectData= fromSystem.getObject(fromBucket, key, outputStream);
				outputStream.close();

				//Upload Object
				Map<String, String> headers = new HashMap<String, String>();
				headers.put("Content-Type", objectData.get("contenttype").getAsString());

				Map<String, String> metadata= null;
				if (objectData.has("metadata") && !objectData.getAsJsonObject("metadata").entrySet().isEmpty())
				{
					metadata= new HashMap<String, String>();

					Set<Map.Entry<String, JsonElement>> metadataRaw = objectData.getAsJsonObject("metadata").entrySet();

                    for(Map.Entry<String, JsonElement> entry : metadataRaw)
					{
						metadata.put(entry.getKey(), entry.getValue().getAsString());
					}
				}

				FileInputStream inputStream= new FileInputStream(tempFile);
				toSystem.createObject(toBucket, key, inputStream, headers, metadata);
				inputStream.close();

				tempFile.delete();
			}
			
		
		} catch(Exception e)
		{
			throw new RiakCSException(e);
		}
	}


	private CommunicationLayer getCommunicationLayer()
	{
		return new CommunicationLayer(csAccessKey, csSecretKey, csEndpoint, useSSL, debugModeEnabled);
	}

	private String getInputStreamAsString(InputStream is) throws IOException
	{
		StringBuffer responseBody= new StringBuffer();
		BufferedReader reader= new BufferedReader(new InputStreamReader(is));
		String line= null;
		while ((line= reader.readLine()) != null)
		{
			responseBody.append(line + "\n");
		}
		reader.close();

		if (debugModeEnabled) System.out.println("Body:\n" + responseBody + "\n");

		return responseBody.toString();
	}



}
