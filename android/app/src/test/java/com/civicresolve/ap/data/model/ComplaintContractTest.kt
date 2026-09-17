package com.civicresolve.ap.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class ComplaintContractTest {
    private val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        .adapter(AdminComplaintStatsResponse::class.java)

    private fun response(user: String) = """{
        "success":true,"stats":{"newComplaint":[{
          "_id":"complaint-test","user":$user,"description":"Road repair",
          "latitude":"17.0","longitude":"78.0","city":"Test city","state":"Test state",
          "landmark":"Crossing","category":"road","status":"new","rating":0
        }],"inProgressComplaint":[],"resolvedComplaint":[]}
    }"""

    @Test fun adminStatsAcceptPublicUserProjectionWithoutPrivateFields() {
        val parsed = adapter.fromJson(response("""{"_id":"user-test","fullName":"Citizen","profilePic":""}"""))!!
        val complaint = parsed.stats!!.newComplaint!!.single()
        assertEquals("user-test", complaint.user!!.id)
        assertEquals("Citizen", complaint.user.fullName)
        assertNull(complaint.user.email)
        assertNull(complaint.user.address)
        assertTrue(parsed.stats.inProgressComplaint!!.isEmpty())
        assertNull(parsed.stats.pendingVerificationComplaint)
    }

    @Test fun deletedReporterCanBeNull() {
        assertNull(adapter.fromJson(response("null"))!!.stats!!.newComplaint!!.single().user)
    }
}
