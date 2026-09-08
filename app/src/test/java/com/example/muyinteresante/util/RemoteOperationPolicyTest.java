package com.example.muyinteresante.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.junit.Test;

public class RemoteOperationPolicyTest {

    @Test
    public void offlineGuardStopsRemoteRequestImmediately() {
        assertFalse(RemoteOperationPolicy.hasUsableNetwork(false, false));
        assertFalse(RemoteOperationPolicy.hasUsableNetwork(false, true));
        assertFalse(RemoteOperationPolicy.hasUsableNetwork(true, false));
        assertTrue(RemoteOperationPolicy.hasUsableNetwork(true, true));
    }

    @Test
    public void successfulHttpResponseDoesNotNeedGeneralDiagnostic() {
        RemoteOperationPolicy.FailureKind result = RemoteOperationPolicy.classify(null, 200);
        assertEquals(RemoteOperationPolicy.FailureKind.NONE, result);
        assertFalse(RemoteOperationPolicy.shouldRunGeneralDiagnostic(result));
    }

    @Test
    public void feedFailureWithValidHttpResponseIsServiceFailure() {
        RemoteOperationPolicy.FailureKind result = RemoteOperationPolicy.classify(
                new SocketTimeoutException("read timeout after headers"), 503);
        assertEquals(RemoteOperationPolicy.FailureKind.SERVICE, result);
        assertFalse(RemoteOperationPolicy.shouldRunGeneralDiagnostic(result));
    }

    @Test
    public void ambiguousNetworkFailureRunsGeneralDiagnosticAfterward() {
        assertEquals(RemoteOperationPolicy.FailureKind.CONNECTIVITY,
                RemoteOperationPolicy.classify(new UnknownHostException("feed"), -1));
        assertEquals(RemoteOperationPolicy.FailureKind.CONNECTIVITY,
                RemoteOperationPolicy.classify(new ConnectException("refused"), -1));
        assertTrue(RemoteOperationPolicy.shouldRunGeneralDiagnostic(
                RemoteOperationPolicy.classify(new SocketTimeoutException("timeout"), -1)));
    }
}
