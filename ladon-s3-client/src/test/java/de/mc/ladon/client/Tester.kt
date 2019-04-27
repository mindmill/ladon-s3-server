package de.mc.ladon.client

import java.util.*


fun main() {

    val client = LadonS3Client("http://localhost:8080/services/s3/", "tBMYxafsAEpmu6ARR2BO", "bMRT4cNwBs61OJKcVejszXKmteis4Dzbv1iKXfxT")


    val b = UUID.randomUUID().toString()
    client.createBucket(b)

//    println(client.listBuckets().bucketList.map { it.name })
    println(client.listObjects(b).next().contentsList?.size)
    for (i in 1..100) {
        client.createObject(b, "test$i.txt", "geht".toByteArray().inputStream(), mapOf("head1" to "geht auch"))
    }
    client.createObject(b, "test.txt", "geht".toByteArray().inputStream(), mapOf("head1" to "geht auch"))
    println(client.listObjects(b).next().contentsList.size)
    println(client.listObjects(b).next().contentsList?.map { it.meta })
    println(client.listVersions(b).versions.size)
    println(client.getObject(b, "test.txt").reader().readText())
    println(client.getObjectMeta(b, "test.txt"))
    client.updateMetadata(b, "test.txt", mapOf("head1" to "neu"))
    println(client.getObjectMeta(b, "test.txt"))
    println(client.getObject(b, "test.txt").reader().readText())

    client.listObjects(bucket = b, limit = 10).forEach {
        it.contentsList.forEach {
            println(it.key)
            client.deleteObject(b, it.key)
        }
    }
    client.deleteBucket(b)
//    client.deleteBucket("79c1aa02-6c23-4619-ac19-29fd2bab0634")
}
