package com.bw.vrtnumm.androidApp;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.ExoMediaDrm;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VuDrmCallback implements MediaDrmCallback {
    private static final String TAG = "VuDrmCallback";

    private static final int MAX_MANUAL_REDIRECTS = 5;

    private final HttpDataSource.Factory dataSourceFactory;
    private final String defaultLicenseUrl;
    private final boolean forceDefaultLicenseUrl;
    private final Map<String, String> keyRequestProperties;

    private String drmToken;
    private String kid;

    /**
     * @param defaultLicenseUrl The default license URL. Used for key requests that do not specify
     *     their own license URL.
     * @param dataSourceFactory A factory from which to obtain {@link HttpDataSource} instances.
     */
    public VuDrmCallback(String defaultLicenseUrl, HttpDataSource.Factory dataSourceFactory) {
        this(defaultLicenseUrl, false, dataSourceFactory);
    }

    /**
     * @param defaultLicenseUrl The default license URL. Used for key requests that do not specify
     *     their own license URL, or for all key requests if {@code forceDefaultLicenseUrl} is
     *     set to true.
     * @param forceDefaultLicenseUrl Whether to use {@code defaultLicenseUrl} for key requests that
     *     include their own license URL.
     * @param dataSourceFactory A factory from which to obtain {@link HttpDataSource} instances.
     */
    public VuDrmCallback(String defaultLicenseUrl, boolean forceDefaultLicenseUrl,
                         HttpDataSource.Factory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
        this.defaultLicenseUrl = defaultLicenseUrl;
        this.forceDefaultLicenseUrl = forceDefaultLicenseUrl;
        this.keyRequestProperties = new HashMap<>();
    }

    /**
     * Sets a header for key requests made by the callback.
     *
     * @param name The name of the header field.
     * @param value The value of the field.
     */
    public void setKeyRequestProperty(String name, String value) {
        Assertions.checkNotNull(name);
        Assertions.checkNotNull(value);
        synchronized (keyRequestProperties) {
            keyRequestProperties.put(name, value);
        }
    }

    /**
     * Clears a header for key requests made by the callback.
     *
     * @param name The name of the header field.
     */
    public void clearKeyRequestProperty(String name) {
        Assertions.checkNotNull(name);
        synchronized (keyRequestProperties) {
            keyRequestProperties.remove(name);
        }
    }

    /**
     * Clears all headers for key requests made by the callback.
     */
    public void clearAllKeyRequestProperties() {
        synchronized (keyRequestProperties) {
            keyRequestProperties.clear();
        }
    }

    public void setDrmToken(String drmToken) {
        this.drmToken = drmToken;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    @Override
    public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) {
        try {
            String url = request.getDefaultUrl() + "&signedRequest=" + com.google.android.exoplayer2.util.Util.fromUtf8Bytes(request.getData());
            return executePost(dataSourceFactory, url, /* httpBody= */ null, /* requestProperties= */ null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) {
        String url = request.getLicenseServerUrl();
            if (forceDefaultLicenseUrl || TextUtils.isEmpty(url)) {
                url = defaultLicenseUrl;
            }
        Map<String, String> requestProperties = new HashMap<>();
            requestProperties.put("Content-Type", "text/html; charset=utf-8");
        synchronized (keyRequestProperties) {
            requestProperties.putAll(keyRequestProperties);
        }

        try {
            JSONArray array = new JSONArray();
                for (byte b : request.getData()) {
                    array.put((int) (b & 0xff));
                }
            JSONObject json = new JSONObject();
                json.put("token", drmToken);
                json.put("drm_info", array);
                json.put("kid", kid);
            byte[] result = executePost(dataSourceFactory, url, json.toString().getBytes("utf-8"), requestProperties);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] executePost(
            HttpDataSource.Factory dataSourceFactory,
            String url,
            @Nullable byte[] httpBody,
            @Nullable Map<String, String> requestProperties)
            throws IOException {
        HttpDataSource dataSource = dataSourceFactory.createDataSource();
        if (requestProperties != null) {
            for (Map.Entry<String, String> requestProperty : requestProperties.entrySet()) {
                dataSource.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
            }
        }

        int manualRedirectCount = 0;
        while (true) {
            DataSpec dataSpec =
                    new DataSpec(
                            Uri.parse(url),
                            DataSpec.HTTP_METHOD_POST,
                            httpBody,
                            /* absoluteStreamPosition= */ 0,
                            /* position= */ 0,
                            /* length= */ C.LENGTH_UNSET,
                            /* key= */ null,
                            DataSpec.FLAG_ALLOW_GZIP);
            DataSourceInputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
            try {
                return com.google.android.exoplayer2.util.Util.toByteArray(inputStream);
            } catch (HttpDataSource.InvalidResponseCodeException e) {
                // For POST requests, the underlying network stack will not normally follow 307 or 308
                // redirects automatically. Do so manually here.
                boolean manuallyRedirect =
                        (e.responseCode == 307 || e.responseCode == 308)
                                && manualRedirectCount++ < MAX_MANUAL_REDIRECTS;
                String redirectUrl = manuallyRedirect ? getRedirectUrl(e) : null;
                if (redirectUrl == null) {
                    throw e;
                }
                url = redirectUrl;
            } finally {
                Util.closeQuietly(inputStream);
            }
        }
    }

    private static @Nullable String getRedirectUrl(HttpDataSource.InvalidResponseCodeException exception) {
        Map<String, List<String>> headerFields = exception.headerFields;
        if (headerFields != null) {
            List<String> locationHeaders = headerFields.get("Location");
            if (locationHeaders != null && !locationHeaders.isEmpty()) {
                return locationHeaders.get(0);
            }
        }
        return null;
    }

    public static String print(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
            sb.append("[ ");
            for (byte b : bytes) {
                sb.append(String.format("0x%02X ", b));
            }
            sb.append("]");
        return sb.toString();
    }
}
