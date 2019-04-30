package de.mc.ladon.client

import com.google.common.base.Charsets
import com.google.common.base.Strings
import com.google.common.io.BaseEncoding
import de.mc.ladon.s3server.auth.AwsSignatureVersion2.HMAC_SHA_1
import de.mc.ladon.s3server.auth.ResponseHeaderOverrides
import de.mc.ladon.s3server.common.S3Constants
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


fun computeV2(secretKey: String,
              method: String,
              uri: String,
              headers: Map<String, String> = hashMapOf(),
              queryParams: Map<String, String> = hashMapOf()): String {

    val canonicalString = makeS3CanonicalString(method, uri, headers, queryParams)
    val keySpec = SecretKeySpec(secretKey.toByteArray(), HMAC_SHA_1)

    val mac = Mac.getInstance(HMAC_SHA_1)
    mac.init(keySpec)
    val result = mac.doFinal(canonicalString.toByteArray(charset(Charsets.UTF_8.name())))
    return BaseEncoding.base64().encode(result)

}

fun getDateString(): String {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    dateFormat.timeZone = TimeZone.getTimeZone("GMT")
    return dateFormat.format(calendar.time)
}


private fun makeS3CanonicalString(method: String,
                                  uri: String,
                                  headers: Map<String, String> = hashMapOf(),
                                  queryParams: Map<String, String> = hashMapOf()): String {
    val buf = StringBuilder()
    buf.append(method).append("\n")

    // Add all interesting headers to a list, then sort them.  "Interesting"
    // is defined as Content-MD5, Content-Type, Date, and x-amz-

    val interestingHeaders = TreeMap<String, String>()
    if (headers.isNotEmpty()) {
        for (entry in headers.entries) {
            val key = entry.key
            val value = entry.value
            val lk = key.toLowerCase(Locale.getDefault())

            // Ignore any headers that are not particularly interesting.
            if (lk == "content-type" || lk == "content-md5" || lk == "date" ||
                    lk.startsWith(S3Constants.X_AMZ_PREFIX)) {
                interestingHeaders[lk] = value
            }
        }
    }

    // Remove default date timestamp if "x-amz-date" is set.
    if (interestingHeaders.containsKey(S3Constants.X_AMZ_DATE)) {
        interestingHeaders["date"] = ""
    }
    // These headers require that we still put a new line in after them,
    // even if they don't exist.
    if (!interestingHeaders.containsKey("content-type")) {
        interestingHeaders["content-type"] = ""
    }
    if (!interestingHeaders.containsKey("content-md5")) {
        interestingHeaders["content-md5"] = ""
    }

    // Any parameters that are prefixed with "x-amz-" need to be included
    // in the headers section of the canonical string to sign
    headers.filter { parameter -> parameter.key.startsWith(S3Constants.X_AMZ_PREFIX) }
            .forEach { parameter -> interestingHeaders[parameter.key] = parameter.value }

    // Add all the interesting headers (i.e.: all that startwith x-amz- ;-))
    for ((key, value) in interestingHeaders) {

        if (key.startsWith(S3Constants.X_AMZ_PREFIX)) {
            buf.append(key).append(':')
            buf.append(value)
        } else {
            buf.append(value)
        }
        buf.append("\n")
    }

    // Add all the interesting parameters
    buf.append(uri)
    val parameterNames = queryParams.keys.toTypedArray()
    Arrays.sort(parameterNames)
    var separator = '?'
    for (parameterName in parameterNames) {
        // Skip any parameters that aren't part of the canonical signed string
        if (!SIGNED_PARAMETERS.contains(parameterName)) continue

        buf.append(separator)
        buf.append(parameterName)
        val parameterValue = queryParams[parameterName]
        if (!Strings.isNullOrEmpty(parameterValue)) {
            buf.append("=").append(parameterValue)
        }
        separator = '&'
    }

    return buf.toString()
}

internal var SIGNED_PARAMETERS = Arrays.asList("acl", "torrent", "logging", "location", "policy", "requestPayment", "versioning",
        "versions", "versionId", "notification", "uploadId", "uploads", "partNumber", "website",
        "delete", "lifecycle", "tagging", "cors", "restore",
        ResponseHeaderOverrides.RESPONSE_HEADER_CACHE_CONTROL,
        ResponseHeaderOverrides.RESPONSE_HEADER_CONTENT_DISPOSITION,
        ResponseHeaderOverrides.RESPONSE_HEADER_CONTENT_ENCODING,
        ResponseHeaderOverrides.RESPONSE_HEADER_CONTENT_LANGUAGE,
        ResponseHeaderOverrides.RESPONSE_HEADER_CONTENT_TYPE,
        ResponseHeaderOverrides.RESPONSE_HEADER_EXPIRES)
