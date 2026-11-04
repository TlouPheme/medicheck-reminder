package com.example.medicheckreminder.ui.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.example.medicheckreminder.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class GoogleSignInErrorsTest {

    @Test
    fun cancelledSignIn_showsNoError() {
        assertNull(GoogleSignInErrors.messageFor(GetCredentialCancellationException()))
    }

    @Test
    fun noGoogleAccount_explainsHowToFixIt() {
        assertEquals(R.string.error_google_no_account, GoogleSignInErrors.messageFor(NoCredentialException()))
    }

    @Test
    fun networkFailure_saysThereIsNoConnection() {
        assertEquals(R.string.error_no_network, GoogleSignInErrors.messageFor(IOException("offline")))
    }

    @Test
    fun otherFailures_showTheGeneralMessage() {
        assertEquals(R.string.error_google_sign_in, GoogleSignInErrors.messageFor(GetCredentialUnknownException()))
        assertEquals(R.string.error_google_sign_in, GoogleSignInErrors.messageFor(IllegalStateException()))
    }
}
