package org.apptank.horus.client.auth

import org.apptank.horus.client.TestCase
import org.junit.Assert
import org.junit.Test


class UserAuthenticationTest : TestCase() {

    private val token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiI5Y2VhN2MyZS1jNTgwLTQ3ODEtYTYxOS01ZmQ3ZTMzODFlYTgiLCJqdGkiOiJhOTY1MTYyMDM4ZGVjMjdhMjNiYjM1MGNlMzAzYWJkNmRmMTFiMjQ1YjQ0MWFiYjIzOWI3Mjg1YjJkYzI1NWNmMmQ4YzczOTZjZGU1NTQxZiIsImlhdCI6MTcyNTMwOTE5OC4xNDkxNjUsIm5iZiI6MTcyNTMwOTE5OC4xNDkxNjYsImV4cCI6MTc1Njg0NTE5OC4xNDcwODQsInN1YiI6ImRiMDVmOTYwLWVlNWQtNDA0ZS05OTI5LTViZWI0OGE2MTJhMSIsInNjb3BlcyI6WyJ1c2VyLnNpZ251cCIsInVzZXIucHJvZmlsZS5yZWFkIiwidXNlci5hdXRoZW50aWNhdGUiLCJ1c2VyLmludml0YXRpb24uY3JlYXRlIiwidXNlci5wYXNzd29yZC5yZWNvdmVyeSIsInVzZXIucHJvZmlsZS5jcmVhdGUiLCJ1c2VyLnBhc3N3b3JkLnVwZGF0ZSIsInVzZXIucHJvZmlsZS51cGRhdGUiLCJ1c2VyLnByb2ZpbGUuZGVsZXRlIiwiZGF0YS5zeW5jIl0sImVudGl0aWVzX2dyYW50ZWQiOlt7InVzZXJfb3duZXJfaWQiOiI3YTc2ODQ3NC1kZTEyLTQzNjgtOWVkMS0zNzUyZGE4MDBkNjAiLCJlbnRpdHlfaWQiOiI2ZDc3NjdlYy0wOWJmLTRlZDgtOTZkOS05NTU2ZTRmMjExNDQiLCJlbnRpdHlfbmFtZSI6ImxldmkzNiIsImFjY2Vzc19sZXZlbCI6IlJDVUQifV19.D9ANGMsIvTnLFr5yn8PiuPWExZfofkgwoPibDsfVhJWZVT2wbU-N1K8qpFBsp_4PMopvelMZPyc28b8jU5ZZthY36FjM93oC0Xa9CtKc8cnY2qFSnP3XZ7tpYndloIkPqax53AApojCS3mJV_swbilTtisrS3bwoZzt8CKgTdHm0cpmPj06VGCbGBx-Rk1Y24KCMRONSRiJBiiTo7Oyi3kOw1Xv7G9r6WtR44wz2dEqK6PN9S2tK9tCLKVx1y_Wq6PZZXCuK83VFuycCBgLTXivNRTVgoOSxfTMwTcLVG_TtsVECdjpL4PpKoa3NAuTtiG9Xntx8wl-MNbWqfZpY5k3Kc33grsCkYHlJOysBgCjzRHkBivNK0Z6YUcTuAkR8Yz-2AwZ7eDm9ZJvjmafq5N_EKetNBegtw7jG_6UMbqLr9dSyxOi5FBUTeaoccckYxhoMiXSRRLq5Abi_DodMoBklF2P7ACG1pT-iMKECO8GynoaTknyO116NN0MJpoyUUJL1GeYW2p3wzWucV_Wf9g4JZHtwb_KTwvBJOK2rQCQ3ZvmaOwQQEQfwnSD2oRmlDIr0uGNf1_q9JvPvd3aXhSwLr34k7QEx1vpX0JNiiVx8Jyh4pJLYitZyuTh9DjvG1kciYniMDnqVpVKOdo7aNb-2MDUsS3zVckdLpxiDSy0"

    @Test
    fun validateDecodeToken() {
        // Given
        val userAuthentication = UserAuthentication(token)

        // Then
        Assert.assertEquals("db05f960-ee5d-404e-9929-5beb48a612a1", userAuthentication.userId)
        Assert.assertEquals(token, userAuthentication.accessToken)
        Assert.assertFalse(userAuthentication.isUserActingAs())
    }

    @Test
    fun setUserActingAs() {
        // Given
        val userActingAs = uuid()
        val userAuthentication = UserAuthentication(token)

        // When
        userAuthentication.setUserActingAs(userActingAs)

        // Then
        Assert.assertTrue(userAuthentication.isUserActingAs())
        Assert.assertEquals(userActingAs, userAuthentication.getActingAsUserId())
    }

    @Test
    fun clearUserActingAs() {
        // Given
        val userAuthentication = UserAuthentication(token)
        userAuthentication.setUserActingAs(uuid())

        // When
        userAuthentication.clearUserActingAs()

        // Then
        Assert.assertFalse(userAuthentication.isUserActingAs())
        Assert.assertNull(userAuthentication.getActingAsUserId())
    }

}