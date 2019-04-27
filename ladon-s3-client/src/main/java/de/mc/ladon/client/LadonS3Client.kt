package de.mc.ladon.client

import de.mc.ladon.s3server.common.S3Constants
import de.mc.ladon.s3server.jaxb.entities.ListAllMyBucketsResult
import de.mc.ladon.s3server.jaxb.entities.ListBucketResult
import de.mc.ladon.s3server.jaxb.entities.ListVersionsResult
import org.glassfish.jersey.client.ClientProperties
import java.io.InputStream
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.WebTarget
import javax.ws.rs.core.MediaType

class LadonS3Client(url: String,
                    private val accessKey: String,
                    private val secretKey: String) {

    private val target: WebTarget


    init {
        val client = ClientBuilder.newClient()
        client.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true)
        target = client.target(url)
    }


    fun listBuckets(): ListAllMyBucketsResult {
        val builder = target.request()
        val ds = getDateString()
        builder.header("Date", ds)
        val signature = computeV2(secretKey, "GET", target.uri.path, hashMapOf("date" to ds))
        val authHeader = "AWS $accessKey:$signature"
        builder.header("Authorization", authHeader)
        return builder.get(ListAllMyBucketsResult::class.java)
    }

    fun createBucket(name: String) {
        val builder = target.path(name).request()
        val ds = getDateString()
        builder.header("Date", ds)
        val signature = computeV2(secretKey, "PUT", target.path(name).uri.path, hashMapOf("date" to ds))
        val authHeader = "AWS $accessKey:$signature"
        builder.header("Authorization", authHeader).put(null)
    }

    fun deleteBucket(name: String) {
        val builder = target.path(name).request()
        val ds = getDateString()
        builder.header("Date", ds)
        val signature = computeV2(secretKey, "DELETE", target.path(name).uri.path, hashMapOf("date" to ds))
        val authHeader = "AWS $accessKey:$signature"
        builder.header("Authorization", authHeader)
        target.path(name).request().delete()
    }

    private fun WebTarget.andIf(cond: Boolean, body: WebTarget.() -> WebTarget): WebTarget {
        return if (cond) body() else this
    }

    fun listObjects(bucket: String,
                    prefix: String? = null,
                    delimiter: Boolean = false,
                    limit: Int? = null,
                    order: ListOrder = ListOrder.DEFAULT): Iterator<ListBucketResult> {
        return object : Iterator<ListBucketResult> {
            var result: ListBucketResult? = null
            override fun hasNext(): Boolean {
                return result?.isTruncated ?: true
            }

            override fun next(): ListBucketResult {
                val request = target.path(bucket)
                        .andIf(delimiter) {
                            queryParam("delimiter", "/")
                        }.andIf(prefix != null) {
                            queryParam("prefix", prefix)
                        }.andIf(limit != null) {
                            queryParam("max-keys", limit)
                        }.andIf(result != null && result!!.isTruncated) {
                            queryParam("marker", result!!.marker)
                        }.andIf(order == ListOrder.TIMESTAMP) {
                            queryParam("ladonOrderedByTimestamp", "true")
                        }.request()
                val ds = getDateString()
                request.header("Date", ds)
                val signature = computeV2(secretKey, "GET", target.path(bucket).uri.path, hashMapOf("date" to ds))
                val authHeader = "AWS $accessKey:$signature"
                request.header("Authorization", authHeader)
                result = request.get(ListBucketResult::class.java)
                return result!!
            }
        }
    }

    fun listVersions(bucket: String): ListVersionsResult {
        val request = target.path(bucket)
                .queryParam(S3Constants.VERSIONS, "")
                .request()
        return request.get(ListVersionsResult::class.java)
    }

    fun createObject(bucket: String, key: String, content: InputStream, userMeta: Map<String, String> = hashMapOf()) {
        val request = target.path("$bucket/$key").request()
        userMeta.entries.map { S3Constants.X_AMZ_META_PREFIX + it.key to it.value }.forEach {
            request.header(it.first, it.second)
        }
        request.put(Entity.entity(content, MediaType.APPLICATION_OCTET_STREAM))
    }

    fun updateMetadata(bucket: String, key: String, userMeta: Map<String, String> = hashMapOf()) {
        val request = target.path("$bucket/$key").queryParam("ladonupdatemeta", "true").request()
        userMeta.entries.map { S3Constants.X_AMZ_META_PREFIX + it.key to it.value }.forEach {
            request.header(it.first, it.second)
        }
        request.put(Entity.entity("", MediaType.APPLICATION_OCTET_STREAM))
    }

    fun getObject(bucket: String, key: String): InputStream {
        val request = target.path("$bucket/$key").request()
        return request.get(InputStream::class.java)
    }

    fun getObjectMeta(bucket: String, key: String): Map<String, String> {
        val request = target.path("$bucket/$key").request()
        return request.head().headers.filter { it.key.startsWith(S3Constants.X_AMZ_META_PREFIX) }
                .map {
                    it.key.substringAfter(S3Constants.X_AMZ_META_PREFIX) to (it.value.firstOrNull()?.toString() ?: "")
                }.toMap()
    }

    fun deleteObject(bucket: String, key: String) {
        val request = target.path("$bucket/$key").request()
        request.delete()
    }


}


