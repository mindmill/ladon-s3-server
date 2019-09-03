package de.mc.ladon.client

import de.mc.ladon.s3server.common.S3Constants
import de.mc.ladon.s3server.jaxb.entities.Error
import de.mc.ladon.s3server.jaxb.entities.BucketList
import de.mc.ladon.s3server.jaxb.entities.ObjectListing
import de.mc.ladon.s3server.jaxb.entities.VersionListing
import java.io.InputStream
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

class LadonRestClient(url: String,
                      private val accessKey: String,
                      private val secretKey: String) {

    private val target: WebTarget


    init {
        val client = ClientBuilder.newClient()
        target = client.target(url)
    }


    fun listBuckets(): BucketList {
        val builder = target.request()
        addAuthHeader(builder, target.uri.path, "GET")
        return builder.get(BucketList::class.java)
    }

    fun createBucket(name: String) {
        val builder = target.path(name).request()
        addAuthHeader(builder, target.path(name).uri.path, "PUT", mapOf("content-type" to "application/octet-stream"))
        builder.put(Entity.entity("", MediaType.APPLICATION_OCTET_STREAM)).checkResponse()
    }

    fun deleteBucket(name: String) {
        val builder = target.path(name).request()
        addAuthHeader(builder, target.path(name).uri.path, "DELETE")
        builder.delete().checkResponse()
    }

    private fun WebTarget.andIf(cond: Boolean, body: WebTarget.() -> WebTarget): WebTarget {
        return if (cond) body() else this
    }

    fun listObjects(bucket: String,
                    prefix: String? = null,
                    delimiter: Boolean = false,
                    limit: Int? = null,
                    since: Long? = null): Iterator<ObjectListing> {
        return object : Iterator<ObjectListing> {
            var result: ObjectListing? = null
            override fun hasNext(): Boolean {
                return result?.isTruncated ?: true
            }

            override fun next(): ObjectListing {
                val request = target.path(bucket)
                        .andIf(delimiter) {
                            queryParam("delimiter", "/")
                        }.andIf(prefix != null) {
                            queryParam("prefix", prefix)
                        }.andIf(limit != null) {
                            queryParam("max-keys", limit)
                        }.andIf(result != null && result!!.isTruncated) {
                            queryParam("marker", result!!.marker)
                        }.andIf(since != null) {
                            queryParam("ladonChangesSince", since)
                        }.request()
                addAuthHeader(request, target.path(bucket).uri.path, "GET")
                result = request.get(ObjectListing::class.java)
                return result!!
            }
        }
    }

    fun listVersions(bucket: String): VersionListing {
        val request = target.path(bucket)
                .queryParam(S3Constants.VERSIONS, "")
                .request()
        addAuthHeader(request, target.path(bucket).uri.path + "?versions", "GET")
        return request.get(VersionListing::class.java)
    }


    fun putObject(bucket: String, key: String, content: InputStream, userMetadata: Map<String, String> = hashMapOf()) {
        val request = target.path("$bucket/$key").request()
        val headerMap = userMetadata.entries.map { S3Constants.X_AMZ_META_PREFIX + it.key to it.value }
        headerMap.forEach { request.header(it.first, it.second) }
        addAuthHeader(request, target.path("$bucket/$key").uri.path, "PUT",
                (headerMap + ("content-type" to "application/octet-stream")).toMap())
        request.put(Entity.entity(content, MediaType.APPLICATION_OCTET_STREAM))
    }

    fun putUserMetadata(bucket: String, key: String, userMetadata: Map<String, String> = hashMapOf()) {
        val request = target.path("$bucket/$key").queryParam("ladonupdatemeta", "true").request()
        val metaMap = userMetadata.entries.map { S3Constants.X_AMZ_META_PREFIX + it.key to it.value }
        metaMap.forEach { request.header(it.first, it.second) }
        addAuthHeader(request, target.path("$bucket/$key").uri.path, "PUT",
                mapOf("content-type" to "application/octet-stream") + metaMap.toMap())
        request.put(Entity.entity("", MediaType.APPLICATION_OCTET_STREAM)).checkResponse()
    }

    fun getObjectContent(bucket: String, key: String): InputStream {
        val request = target.path("$bucket/$key").request()
        addAuthHeader(request, target.path("$bucket/$key").uri.path, "GET")
        return request.get(InputStream::class.java)
    }

    fun getUserMetadata(bucket: String, key: String): Map<String, String> {
        val request = target.path("$bucket/$key").request()
        addAuthHeader(request, target.path("$bucket/$key").uri.path, "HEAD")
        return request.head().checkResponse().headers.filter { it.key.startsWith(S3Constants.X_AMZ_META_PREFIX) }
                .map {
                    it.key.substringAfter(S3Constants.X_AMZ_META_PREFIX) to (it.value.firstOrNull()?.toString() ?: "")
                }.toMap()
    }

    fun deleteObject(bucket: String, key: String) {
        val request = target.path("$bucket/$key").request()
        addAuthHeader(request, target.path("$bucket/$key").uri.path, "DELETE")
        request.delete().checkResponse()

    }

    fun Response.checkResponse(): Response {
        if (status !in 200..205) {
            throw LadonClientException(this.readEntity(Error::class.java))
        }
        return this
    }


    private fun addAuthHeader(request: Invocation.Builder, url: String, method: String, additionalHeaders: Map<String, String> = hashMapOf()) {
        val ds = getDateString()
        request.header("Date", ds)
        val signature = computeV2(secretKey, method, url, hashMapOf("date" to ds, "content-md5" to "") +
                additionalHeaders.mapKeys { it.key.toLowerCase() })
        val authHeader = "AWS $accessKey:$signature"
        request.header("Authorization", authHeader)
    }
}


