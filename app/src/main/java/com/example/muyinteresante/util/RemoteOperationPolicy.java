package com.example.muyinteresante.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

/** Cheap guards and failure classification for normal remote operations. */
@SuppressWarnings("deprecation")
public final class RemoteOperationPolicy {

    public enum FailureKind {
        NONE,
        SERVICE,
        CONNECTIVITY
    }

    private RemoteOperationPolicy() {}

    public static boolean hasUsableNetwork(boolean connected) {
        return connected;
    }

    /**
     * Cheap guard for starting a remote operation. The Gist's capabilities
     * snapshot is combined with the current legacy-compatible active interface
     * state so a stale network capability cannot start a download or spinner.
     */
    public static boolean hasUsableNetwork(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = manager != null ? manager.getActiveNetworkInfo() : null;
        return activeInfo != null
                && activeInfo.isConnected()
                && ConnectivityAndInternetAccess.isConnected(context);
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
