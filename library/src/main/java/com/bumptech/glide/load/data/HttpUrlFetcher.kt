package com.bumptech.glide.load.data

import android.text.TextUtils
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.HttpException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.util.ContentLengthInputStream
import com.bumptech.glide.util.LogTime
import com.bumptech.glide.util.Synthetic
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL

/** A DataFetcher that retrieves an [java.io.InputStream] for a Url.  */
class HttpUrlFetcher @VisibleForTesting internal constructor(private val glideUrl: GlideUrl, private val timeout: Int, private val connectionFactory: HttpUrlConnectionFactory) : DataFetcher<InputStream?> {
    private var urlConnection: HttpURLConnection? = null
    private var stream: InputStream? = null

    @Volatile
    private var isCancelled = false

    constructor(glideUrl: GlideUrl, timeout: Int) : this(glideUrl, timeout, DEFAULT_CONNECTION_FACTORY) {}

    override fun loadData(
            priority: Priority, callback: DataFetcher.DataCallback<in InputStream?>) {
        val startTime = LogTime.getLogTime()
        try {
            val result = loadDataWithRedirects(glideUrl.toURL(), 0, null, glideUrl.headers)
            callback.onDataReady(result)
        } catch (e: IOException) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to load data for url", e)
            }
            callback.onLoadFailed(e)
        } finally {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Finished http url fetcher fetch in " + LogTime.getElapsedMillis(startTime))
            }
        }
    }

    @Throws(HttpException::class)
    private fun loadDataWithRedirects(
            url: URL, redirects: Int, lastUrl: URL?, headers: Map<String, String>): InputStream? {
        if (redirects >= MAXIMUM_REDIRECTS) {
            throw HttpException(
                    "Too many (> $MAXIMUM_REDIRECTS) redirects!", INVALID_STATUS_CODE)
        } else {
            // Comparing the URLs using .equals performs additional network I/O and is generally broken.
            // See http://michaelscharf.blogspot.com/2006/11/javaneturlequals-and-hashcode-make.html.
            try {
                if (lastUrl != null && url.toURI() == lastUrl.toURI()) {
                    throw HttpException("In re-direct loop", INVALID_STATUS_CODE)
                }
            } catch (e: URISyntaxException) {
                // Do nothing, this is best effort.
            }
        }
        urlConnection = buildAndConfigureConnection(url, headers)
        stream = try {
            // Connect explicitly to avoid errors in decoders if connection fails.
            urlConnection?.connect()
            // Set the stream so that it's closed in cleanup to avoid resource leaks. See #2352.
            urlConnection?.inputStream
        } catch (e: IOException) {
            throw HttpException(
                    "Failed to connect or obtain data", getHttpStatusCodeOrInvalid(urlConnection!!), e)
        }
        if (isCancelled) {
            return null
        }
        val statusCode = getHttpStatusCodeOrInvalid(urlConnection!!)
        return if (isHttpOk(statusCode)) {
            getStreamForSuccessfulRequest(urlConnection!!)
        } else if (isHttpRedirect(statusCode)) {
            val redirectUrlString = urlConnection!!.getHeaderField(REDIRECT_HEADER_FIELD)
            if (TextUtils.isEmpty(redirectUrlString)) {
                throw HttpException("Received empty or null redirect url", statusCode)
            }
            val redirectUrl: URL
            redirectUrl = try {
                URL(url, redirectUrlString)
            } catch (e: MalformedURLException) {
                throw HttpException("Bad redirect url: $redirectUrlString", statusCode, e)
            }
            // Closing the stream specifically is required to avoid leaking ResponseBodys in addition
            // to disconnecting the url connection below. See #2352.
            cleanup()
            loadDataWithRedirects(redirectUrl, redirects + 1, url, headers)
        } else if (statusCode == INVALID_STATUS_CODE) {
            throw HttpException(statusCode)
        } else {
            try {
                throw HttpException(urlConnection!!.responseMessage, statusCode)
            } catch (e: IOException) {
                throw HttpException("Failed to get a response message", statusCode, e)
            }
        }
    }

    @Throws(HttpException::class)
    private fun buildAndConfigureConnection(url: URL, headers: Map<String, String>): HttpURLConnection {
        val urlConnection: HttpURLConnection
        urlConnection = try {
            connectionFactory.build(url)
        } catch (e: IOException) {
            throw HttpException("URL.openConnection threw",  /*statusCode=*/0, e)
        }
        for ((key, value) in headers) {
            urlConnection.addRequestProperty(key, value)
        }
        urlConnection.connectTimeout = timeout
        urlConnection.readTimeout = timeout
        urlConnection.useCaches = false
        urlConnection.doInput = true
        // Stop the urlConnection instance of HttpUrlConnection from following redirects so that
        // redirects will be handled by recursive calls to this method, loadDataWithRedirects.
        urlConnection.instanceFollowRedirects = false
        return urlConnection
    }

    @Throws(HttpException::class)
    private fun getStreamForSuccessfulRequest(urlConnection: HttpURLConnection): InputStream? {
        stream = try {
            if (TextUtils.isEmpty(urlConnection.contentEncoding)) {
                val contentLength = urlConnection.contentLength
                ContentLengthInputStream.obtain(urlConnection.inputStream, contentLength.toLong())
            } else {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Got non empty content encoding: " + urlConnection.contentEncoding)
                }
                urlConnection.inputStream
            }
        } catch (e: IOException) {
            throw HttpException(
                    "Failed to obtain InputStream", getHttpStatusCodeOrInvalid(urlConnection), e)
        }
        return stream
    }

    override fun cleanup() {
        try {
            stream?.close()
        } catch (e: IOException) {
            // Ignore
        }
        urlConnection?.disconnect()
        urlConnection = null
    }

    override fun cancel() {
        // TODO: we should consider disconnecting the url connection here, but we can't do so
        // directly because cancel is often called on the main thread.
        isCancelled = true
    }

    @Suppress("UNCHECKED_CAST")
    override val dataClass: Class<InputStream?>
        get() = InputStream::class.java as Class<InputStream?>

    override val dataSource: DataSource
        get() = DataSource.REMOTE

    interface HttpUrlConnectionFactory {
        @Throws(IOException::class)
        fun build(url: URL): HttpURLConnection
    }

    private class DefaultHttpUrlConnectionFactory @Synthetic internal constructor() : HttpUrlConnectionFactory {
        @Throws(IOException::class)
        override fun build(url: URL): HttpURLConnection {
            return url.openConnection() as HttpURLConnection
        }
    }

    companion object {
        private const val TAG = "HttpUrlFetcher"
        private const val MAXIMUM_REDIRECTS = 5

        @VisibleForTesting
        const val REDIRECT_HEADER_FIELD = "Location"

        @JvmField
        @VisibleForTesting
        val DEFAULT_CONNECTION_FACTORY: HttpUrlConnectionFactory = DefaultHttpUrlConnectionFactory()

        /** Returned when a connection error prevented us from receiving an http error.  */
        @VisibleForTesting
        const val INVALID_STATUS_CODE = -1
        private fun getHttpStatusCodeOrInvalid(urlConnection: HttpURLConnection): Int {
            try {
                return urlConnection.responseCode
            } catch (e: IOException) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Failed to get a response code", e)
                }
            }
            return INVALID_STATUS_CODE
        }

        // Referencing constants is less clear than a simple static method.
        private fun isHttpOk(statusCode: Int): Boolean {
            return statusCode / 100 == 2
        }

        // Referencing constants is less clear than a simple static method.
        private fun isHttpRedirect(statusCode: Int): Boolean {
            return statusCode / 100 == 3
        }
    }
}