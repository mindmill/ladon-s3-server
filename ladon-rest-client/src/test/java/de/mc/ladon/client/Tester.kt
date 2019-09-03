package de.mc.ladon.client

import java.util.*


fun main() {

    val client = LadonRestClient("http://localhost:8080/services/s3/", "tBMYxafsAEpmu6ARR2BO", "bMRT4cNwBs61OJKcVejszXKmteis4Dzbv1iKXfxT")


    val b = UUID.randomUUID().toString()
    client.createBucket(b)

//    println(client.listBuckets().bucketList.map { it.name })
    println(client.listObjects(b).next().objectSummaryList?.size)
    for (i in 1..100) {
        client.putObject(b, "test$i.txt", "geht".toByteArray().inputStream(), mapOf("head1" to "geht auch"))
    }
    client.putObject(b, "test.txt", "geht".toByteArray().inputStream(), mapOf("head1" to "geht auch"))
    println(client.listObjects(b).next().objectSummaryList.size)
    println(client.listObjects(b).next().objectSummaryList?.map { it.userMetadata })
    println(client.listVersions(b).versionSummaries.size)
    println(client.getObjectContent(b, "test.txt").reader().readText())
    println(client.getUserMetadata(b, "test.txt"))
    client.putUserMetadata(b, "test.txt", mapOf("head1" to "neu"))
    println(client.getUserMetadata(b, "test.txt"))
    println(client.getObjectContent(b, "test.txt").reader().readText())

    client.listObjects(bucket = b, limit = 10).forEach {
        it.objectSummaryList.forEach {
            println(it.key)
            client.deleteObject(b, it.key)
        }
    }

    client.listObjects(bucket = b, limit = 10,prefix = "test93",  since = Date().time - 10000).forEach {
        it.objectSummaryList?.forEach {
            println("latest: " + it.key)
        }
    }
    client.deleteBucket(b)
//    client.deleteBucket("79c1aa02-6c23-4619-ac19-29fd2bab0634")
}
