package jerryc05.unlockme.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import jerryc05.unlockme.BuildConfig;

/**
 * A builder for URLConnection class.
 *
 * @author \
 * \           d88b d88888b d8888b. d8888b. db    db  .o88b.  .d88b.    ooooo
 * \           `8P' 88'     88  `8D 88  `8D `8b  d8' d8P  Y8 .8P  88.  8P~~~~
 * \            88  88ooooo 88oobY' 88oobY'  `8bd8'  8P      88  d'88 dP
 * \            88  88~~~~~ 88`8b   88`8b      88    8b      88 d' 88 V8888b.
 * \        db. 88  88.     88 `88. 88 `88.    88    Y8b  d8 `88  d8'     `8D
 * \        Y8888P  Y88888P 88   YD 88   YD    YP     `Y88P'  `Y88P'  88oobY'
 * @see java.net.URLConnection
 * @see java.net.HttpURLConnection
 * @see javax.net.ssl.HttpsURLConnection
 */
public final class URLConnectionBuilder {

  private final static String
          TAG = "URLConnectionBuilder";

  private final static int
          TRANSPORT_CELLULAR = 0,
          TRANSPORT_WIFI     = 1;

  final static String
          METHOD_GET     = "GET",
          METHOD_POST    = "POST",
          METHOD_HEAD    = "HEAD",
          METHOD_OPTIONS = "OPTIONS",
          METHOD_PUT     = "PUT",
          METHOD_DELETE  = "DELETE",
          METHOD_TRACE   = "TRACE";

  private boolean isHTTP, wifiOnly = true;
  private URLConnection urlConnection;

  @StringDef({METHOD_GET, METHOD_POST, METHOD_HEAD, METHOD_OPTIONS,
          METHOD_PUT, METHOD_DELETE, METHOD_TRACE})
  @Retention(RetentionPolicy.SOURCE)
  @interface RequestMethods {
  }

  private URLConnectionBuilder(@NonNull String baseURL)
          throws IOException {
    baseURL = baseURL.trim();

    if (baseURL.startsWith("http")) {
      if (baseURL.startsWith("://", 4))
        isHTTP = true;
      else if (baseURL.startsWith("s://", 4))
        isHTTP = false;
      else
        throw new UnsupportedOperationException(
                "URL scheme prefix not recognized: " + baseURL);

      urlConnection = new URL(baseURL).openConnection();
      urlConnection.setConnectTimeout(5 * 1000);
      urlConnection.setReadTimeout(5 * 1000);
    }
  }

  public static URLConnectionBuilder get(@NonNull final String baseURL)
          throws IOException {
    return new URLConnectionBuilder(baseURL);
  }

  @SuppressWarnings("unused")
  public static URLConnectionBuilder post(@NonNull final String baseURL)
          throws IOException {
    return new URLConnectionBuilder(baseURL)
            .setRequestMethod(METHOD_POST);
  }

  public URLConnectionBuilder connect(@NonNull final Context context)
          throws IOException {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "connect: " + urlConnection.getURL());

    checkNullUrlConnection("run");
    if (!wifiOnly || getNetworkType(context) == TRANSPORT_WIFI)
      (isHTTP ? urlConnection
              : (HttpsURLConnection) urlConnection).connect();
    else
      throw new IllegalStateException(
              "Wifi-only mode is on. Cannot connect through cellular data!");
    return this;
  }

  @SuppressWarnings("WeakerAccess")
  public String getResult(@NonNull final String charset) throws IOException {
    try {
      String result;
      {
        InputStream inputStream = (isHTTP
                ? urlConnection
                : (HttpsURLConnection) urlConnection).getInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[]                buffer       = new byte[1024];
        int                   length;

        while ((length = inputStream.read(buffer)) != -1)
          outputStream.write(buffer, 0, length);

        result = outputStream.toString(charset);
        inputStream.close();
      }

      if (BuildConfig.DEBUG)
        Log.v(TAG, "connect: Response code = " + (isHTTP
                ? (HttpURLConnection) urlConnection
                : (HttpsURLConnection) urlConnection)
                .getResponseCode()
                + "\n================ Respond content ================\n"
                + result);

      return result;

    } catch (final Exception e) {
      if (BuildConfig.DEBUG)
        Log.e(TAG, "connect: ", e);
      throw e;
    }
  }

  public String getResult() throws IOException {
    return getResult(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
            ? StandardCharsets.UTF_8.name()
            : "utf-8");
  }

  public void disconnect() {
    if (BuildConfig.DEBUG)
      Log.d(TAG, "disconnect: " + urlConnection.getURL());

    checkNullUrlConnection("disconnect");
    (isHTTP ? (HttpURLConnection) urlConnection
            : (HttpsURLConnection) urlConnection)
            .disconnect();
  }

  private static int getNetworkType(@NonNull final Context context)
          throws IllegalStateException {
    final ConnectivityManager mConnectivityManager = (ConnectivityManager)
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
    assert mConnectivityManager != null;

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      final NetworkInfo mNetworkInfo =
              mConnectivityManager.getActiveNetworkInfo();
      assert mNetworkInfo != null;

      if (mNetworkInfo.isConnected())
        return mNetworkInfo.getType();
      else
        throw new IllegalStateException("Network disconnected!");

    } else {
      final Network network = mConnectivityManager.getActiveNetwork();
      if (network == null)
        throw new IllegalStateException("No active network!");

      final NetworkCapabilities networkCapabilities =
              mConnectivityManager.getNetworkCapabilities(network);
      assert networkCapabilities != null;
      if (networkCapabilities.hasTransport(
              NetworkCapabilities.TRANSPORT_CELLULAR))
        return TRANSPORT_CELLULAR;
      if (networkCapabilities.hasTransport(
              NetworkCapabilities.TRANSPORT_WIFI))
        return TRANSPORT_WIFI;

      throw new IllegalStateException("TransportInfo unrecognized!");
    }
  }

  @SuppressWarnings("WeakerAccess")
  public URLConnectionBuilder setRequestMethod(
          @RequestMethods final String _requestMethod) {
    try {
      (isHTTP ? (HttpURLConnection) urlConnection
              : (HttpsURLConnection) urlConnection)
              .setRequestMethod(_requestMethod);
    } catch (final Exception e) {
      if (BuildConfig.DEBUG)
        Log.e(TAG, "setRequestMethod: ", e);
    }
    return this;
  }

  public URLConnectionBuilder setConnectTimeout(int connectTimeout) {
    urlConnection.setConnectTimeout(connectTimeout);
    return this;
  }

  public URLConnectionBuilder setReadTimeout(int readTimeout) {
    urlConnection.setReadTimeout(readTimeout);
    return this;
  }

  @SuppressWarnings("unused")
  public URLConnectionBuilder setWifiOnly(final boolean _wifiOnly) {
    wifiOnly = _wifiOnly;
    return this;
  }

  public URLConnectionBuilder setUseCache(final boolean useCache) {
    urlConnection.setUseCaches(useCache);
    return this;
  }

  @SuppressWarnings("unused")
  public URLConnectionBuilder setRequestProperty(
          @NonNull final String key, @Nullable final String value) {
    urlConnection.setRequestProperty(key, value);
    return this;
  }

  @SuppressWarnings("unused")
  public URLConnection getUrlConnection() {
    checkNullUrlConnection("get");
    return urlConnection;
  }

  @SuppressWarnings("unused")
  public URLConnection _getUrlConnection() {
    return urlConnection;
  }

  private void checkNullUrlConnection(@NonNull final String action) {
    if (urlConnection == null)
      throw new UnsupportedOperationException(
              "Cannot " + action + " null instance decodeURL ${urlConnection}!");
  }
}
