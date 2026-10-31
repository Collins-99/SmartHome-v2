package com.legacycodes.smarthome.data.remote;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Builds a single, shared Retrofit instance for the whole app.
 *
 * Why a shared instance:
 *   OkHttp keeps an internal connection pool and thread pool. Creating a
 *   new OkHttpClient per request causes thread churn. We build once.
 *
 * Why no base URL:
 *   The ESP32 IP is dynamic. Every API method takes a full @Url. This
 *   class doesn't need to know the IP at all — it just produces the
 *   typed Esp32Api interface. The repository (Group 8) passes the URL.
 *
 * Logging:
 *   HttpLoggingInterceptor at BODY level logs full request/response.
 *   ESSENTIAL for debugging during development.
 */
public final class ApiClient {

    private static final long CONNECT_TIMEOUT_SEC = 3;
    private static final long READ_TIMEOUT_SEC    = 5;
    private static final long WRITE_TIMEOUT_SEC   = 5;

    private static volatile Esp32Api INSTANCE;

    private ApiClient() {
        // no instances
    }

    public static Esp32Api api() {
        if (INSTANCE == null) {
            synchronized (ApiClient.class) {
                if (INSTANCE == null) {
                    INSTANCE = build();
                }
            }
        }
        return INSTANCE;
    }

    private static Esp32Api build() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

        // baseUrl is required by Retrofit's builder but is never used
        // because every method provides a full @Url. Just a placeholder.
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(Esp32Api.class);
    }
}