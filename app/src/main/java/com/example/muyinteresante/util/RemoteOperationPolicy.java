package com.example.muyinteresante.util;

import android.content.Context;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

/** Cheap guards and failure classification for normal remote operations. */
public final class RemoteOperationPolicy {

    public enum FailureKind {
        NONE,
        SERVICE,
        CONNECTIVITY
    }

    private RemoteOperationPolicy() {}

    public static boolean hasUsableNetwork(boolean connected, boolean physicalNetwork) {
        return connected && physicalNetwork;
    }

    /** Cheap guard that rejects dangling VPN-only networks. */
    public static boolean hasUsableNetwork(Context context) {
        if (context == null) {
            return false;
        }
        return hasUsableNetwork(
                ConnectivityAndInternetAccess.isConnected(context),
                ConnectivityAndInternetAccess.hasPhysicalNetwork(context));
    }

    public static FailureKind classify(Throwable failure, int httpStatus) {
        if (httpStatus >= 200 && httpStatus < 300) {
            return FailureKind.NONE;
        }
        if (httpStatus > 0) {
            return FailureKind.SERVICE;
        }
        return isConnectivityFailure(failure) ? FailureKind.CONNECTIVITY : FailureKind.SERVICE;
    }

    public static boolean shouldRunGeneralDiagnostic(FailureKind failureKind) {
        return failureKind == FailureKind.CONNECTIVITY;
    }

    public static boolean isConnectivityFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof UnknownHostException
                    || current instanceof ConnectException
                    || current instanceof NoRouteToHostException
                    || current instanceof SocketTimeoutException
                    || current instanceof SocketException
                    || current instanceof SSLException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
