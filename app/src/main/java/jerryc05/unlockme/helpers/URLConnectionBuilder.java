package jerryc05.unlockme.helpers;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
@SuppressWarnings({"WeakerAccess", "unused"})
public final class URLConnectionBuilder implements AutoCloseable {

  private final static String
          TAG = URLConnectionBuilder.class.getSimpleName();

  public final static String
          METHOD_GET     = "GET",
          METHOD_POST    = "POST",
          METHOD_HEAD    = "HEAD",
          METHOD_OPTIONS = "OPTIONS",
          METHOD_PUT     = "PUT",
          METHOD_DELETE  = "DELETE",
          METHOD_TRACE   = "TRACE";

  private boolean       isHTTP;
  private URLConnection urlConnection;

  private URLConnectionBuilder(String _baseURL) throws IOException {
    _baseURL = _baseURL.trim();
    if (!_baseURL.startsWith("http://") && !_baseURL.startsWith("https://"))
      throw new UnsupportedOperationException(
              "${baseURL} prefix not recognized: " + _baseURL);

    urlConnection = new URL(_baseURL).openConnection();
    urlConnection.setConnectTimeout(5 * 1000);
    urlConnection.setReadTimeout(5 * 1000);
    isHTTP = _baseURL.charAt(4) == ':';
  }

  public static URLConnectionBuilder get(String _baseURL) throws IOException {
    return new URLConnectionBuilder(_baseURL);
  }

  public static URLConnectionBuilder post(String _baseURL) throws IOException {
    return new URLConnectionBuilder(_baseURL).setRequestMethod(METHOD_POST);
  }

  public URLConnectionBuilder connect() throws IOException {
    checkNullUrlConnection("run");
    (isHTTP ? urlConnection
            : (HttpsURLConnection) urlConnection).connect();
    return this;
  }

  @Override
  public void close() {
    disconnect();
  }

  public String getResult(String charset) throws IOException {
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
        Log.d(TAG, "connect: Response code = " + (isHTTP
                ? (HttpURLConnection) urlConnection
                : (HttpsURLConnection) urlConnection)
                .getResponseCode()
                + "\n================ Respond content ================\n"
                + result);

      return result;

    } catch (Exception e) {
      Log.e(TAG, "connect: ", e);
      throw e;
    }
  }

  public String getResult() throws IOException {
    return getResult(StandardCharsets.UTF_8.name());
  }

  public void disconnect() {
    checkNullUrlConnection("disconnect");
    (isHTTP ? (HttpURLConnection) urlConnection
            : (HttpsURLConnection) urlConnection)
            .disconnect();

    if (BuildConfig.DEBUG)
      Log.d(TAG, "disconnect: " + urlConnection.getURL());
  }

  public URLConnectionBuilder setRequestMethod(String _requestMethod) {
    try {
      (isHTTP ? (HttpURLConnection) urlConnection
              : (HttpsURLConnection) urlConnection)
              .setRequestMethod(_requestMethod);
    } catch (java.net.ProtocolException e) {
      Log.e(TAG, "setRequestMethod: ", e);
    }
    return this;
  }

  public URLConnectionBuilder setConnectTimeout(int _connectTimeout) {
    urlConnection.setConnectTimeout(_connectTimeout);
    return this;
  }

  public URLConnectionBuilder setReadTimeout(int _readTimeout) {
    urlConnection.setReadTimeout(_readTimeout);
    return this;
  }

  public URLConnectionBuilder setUseCache(boolean _useCache) {
    urlConnection.setUseCaches(_useCache);
    return this;
  }

  public URLConnectionBuilder setRequestProperty(String key, String value) {
    urlConnection.setRequestProperty(key, value);
    return this;
  }

  public URLConnection getUrlConnection() {
    checkNullUrlConnection("get");
    return urlConnection;
  }

  public URLConnection _getUrlConnection() {
    return urlConnection;
  }

  private void checkNullUrlConnection(String action) {
    if (urlConnection == null)
      throw new UnsupportedOperationException(
              "Cannot " + action + " null instance decodeURL ${urlConnection}!");
  }
}
