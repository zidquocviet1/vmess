package com.mqv.vmess.network

import android.annotation.SuppressLint
import android.content.Context
import com.mqv.vmess.R
import com.mqv.vmess.util.Const
import com.mqv.vmess.util.Logging
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.net.ssl.*

class OkHttpProvider {
    companion object {
        @JvmStatic
        fun provideAcceptAllCABuilder(): OkHttpClient.Builder {
            val client = OkHttpClient.Builder()
            try {
                // Create a trust manager that does not validate certificate chains
                val trustAllCerts = arrayOf<TrustManager>(
                    @SuppressLint("CustomX509TrustManager")
                    object : X509TrustManager {
                        @SuppressLint("TrustAllX509TrustManager")
                        override fun checkClientTrusted(
                            chain: Array<X509Certificate>,
                            authType: String
                        ) {
                        }

                        @SuppressLint("TrustAllX509TrustManager")
                        override fun checkServerTrusted(
                            chain: Array<X509Certificate>,
                            authType: String
                        ) {
                        }

                        override fun getAcceptedIssuers(): Array<X509Certificate> {
                            return arrayOf()
                        }
                    }
                )

                // Install the all-trusting trust manager
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())
                // Create an ssl socket factory with our all-trusting manager
                val sslSocketFactory = sslContext.socketFactory
                client.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                client.hostnameVerifier { _: String?, _: SSLSession? -> true }

                return client
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        @JvmStatic
        fun provideSelfSignedCABuilder(context: Context): OkHttpClient.Builder {
//            val loggingInterceptor = HttpLoggingInterceptor()
//            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

            val client = OkHttpClient.Builder()
                .connectTimeout(Const.NETWORK_TIME_OUT, TimeUnit.SECONDS)
                .readTimeout(Const.NETWORK_TIME_OUT, TimeUnit.SECONDS)
                .writeTimeout(Const.NETWORK_TIME_OUT, TimeUnit.SECONDS)
//                .addInterceptor(loggingInterceptor)
                .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                    val request = chain.request()
                    val requestBuilder = request.newBuilder()
                        .addHeader("Accept", Const.CONTENT_TYPE)
                        .addHeader("Content-Type", Const.CONTENT_TYPE)
                    chain.proceed(requestBuilder.build())
                })

            // add self-signed SSL
            try {
                val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                keyStore.load(null, null)
                val fis: InputStream = context.resources.openRawResource(R.raw.tac)
                val bis = BufferedInputStream(fis)
                val certificateFactory = CertificateFactory.getInstance("X.509")
                while (bis.available() > 0) {
                    val cert = certificateFactory.generateCertificate(bis)
                    keyStore.setCertificateEntry(Const.BASE_IP, cert)
                }
                val trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManagerFactory.init(keyStore)
                val trustManagers = trustManagerFactory.trustManagers
                if (!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
                    Logging.show("Unexpected default trust manager " + Arrays.toString(trustManagers))
                }
                val x509TrustManager = trustManagers[0] as X509TrustManager
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf<TrustManager>(x509TrustManager), null)
                val sslSocketFactory = sslContext.socketFactory
                client.sslSocketFactory(sslSocketFactory, x509TrustManager)
                    .hostnameVerifier { hostname: String, _: SSLSession? -> hostname == Const.BASE_IP }
            } catch (e: KeyStoreException) {
                e.printStackTrace()
                Logging.show("KeyStore exception")
            } catch (e: CertificateException) {
                Logging.show("CertificateException exception")
                e.printStackTrace()
            } catch (e: NoSuchAlgorithmException) {
                Logging.show("NoSuchAlgorithmException exception")
                e.printStackTrace()
            } catch (e: IOException) {
                Logging.show("IO exception")
                e.printStackTrace()
            } catch (e: KeyManagementException) {
                e.printStackTrace()
            }
            return client
        }
    }

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class UnsafeOkHttpClient
}