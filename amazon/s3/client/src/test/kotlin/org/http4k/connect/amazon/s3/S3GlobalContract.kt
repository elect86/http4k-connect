package org.http4k.connect.amazon.s3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.model.BucketName
import org.http4k.connect.amazon.s3.action.DeleteBucket
import org.http4k.connect.successValue
import org.http4k.core.HttpHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

abstract class S3GlobalContract(http: HttpHandler) : AwsContract(http) {
    private val s3 by lazy {
        S3.Http(aws.region, { aws.credentials }, http)
    }

    private val bucket = BucketName.of(UUID.randomUUID().toString())

    @BeforeEach
    fun deleteBucket() {
        s3(DeleteBucket(bucket)).successValue()
    }

    @Test
    fun `bucket lifecycle`() {
        assertThat(s3.listBuckets().successValue().contains(bucket), equalTo(false))
        assertThat(s3.createBucket(bucket, aws.region), equalTo(Success(Unit)))
        assertThat(s3.listBuckets().successValue().contains(bucket), equalTo(true))
        assertThat(s3.deleteBucket(bucket), equalTo(Success(Unit)))
        assertThat(s3.listBuckets().successValue().contains(bucket), equalTo(false))
    }
}

