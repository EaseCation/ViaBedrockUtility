package org.oryxel.viabedrockutility.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;

// Also somewhat taken from ViaBedrock.
public class NetworkUtil {
    public static byte[] download(final URL uri) {
        try {
            final HttpURLConnection connection = createConnection(uri);
            connection.setRequestMethod("GET");
            connection.connect();
            checkResponseCode(connection);
            return connection.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static HttpURLConnection createConnection(final URL uri) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
        connection.setConnectTimeout(6000);
        connection.setReadTimeout(6000 * 2);
        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("User-Agent", "libhttpclient/1.0.0.0");
        connection.setRequestProperty("Cache-Control", "no-cache");
        return connection;
    }

    private static void checkResponseCode(final HttpURLConnection connection) throws IOException {
        if (connection.getResponseCode() / 100 != 2) {
            throw new IOException("HTTP response code: " + connection.getResponseCode());
        }
    }
}
