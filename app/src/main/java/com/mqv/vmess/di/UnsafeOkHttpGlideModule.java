package com.mqv.vmess.di;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.mqv.vmess.R;
import com.mqv.vmess.util.Const;
import com.mqv.vmess.util.Logging;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.OkHttpClient;

@GlideModule
public class UnsafeOkHttpGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        super.applyOptions(context, builder);
    }

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        var client = new OkHttpClient.Builder();
        // add self-signed SSL
        try {
            var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            var fis = context.getResources().openRawResource(R.raw.tac);
            var bis = new BufferedInputStream(fis);
            var certificateFactory = CertificateFactory.getInstance("X.509");
            while (bis.available() > 0){
                var cert = certificateFactory.generateCertificate(bis);
                keyStore.setCertificateEntry(Const.BASE_IP, cert);
            }

            var trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            var trustManagers = trustManagerFactory.getTrustManagers();
            if (!(trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager))){
                Logging.show("Unexpected default trust manager " + Arrays.toString(trustManagers));
            }

            var x509TrustManager = (X509TrustManager) trustManagers[0];
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{x509TrustManager}, null);
            var sslSocketFactory = sslContext.getSocketFactory();

            client.sslSocketFactory(sslSocketFactory, x509TrustManager)
                    .hostnameVerifier((hostname, session) -> hostname.equals(Const.BASE_IP));
        } catch (KeyStoreException e) {
            e.printStackTrace();
            Logging.show("KeyStore exception");
        } catch (CertificateException e) {
            Logging.show("CertificateException exception");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            Logging.show("NoSuchAlgorithmException exception");
            e.printStackTrace();
        } catch (IOException e) {
            Logging.show("IO exception");
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory((Call.Factory) client.build()));
    }
}