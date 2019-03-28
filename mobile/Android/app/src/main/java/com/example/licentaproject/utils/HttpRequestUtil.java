package com.example.licentaproject.utils;

import android.util.Log;

import com.example.licentaproject.models.History;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class HttpRequestUtil {

    private HttpRequestUtil() {

    }

    public static <T> Object sendRequest(String url, String method, Object obj, Class<T> returnType, boolean isCollection) {
        HttpURLConnection conn = null;
        Object result = null;
        String completeUrl = String.format("http://%s/%s", SessionData.getServerUrl(), url);
        // send a HTTP request
        try {
            conn = (HttpURLConnection) new URL(completeUrl).openConnection();
            conn.setDoInput(true);
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");
            byte[] reqBytes = null;
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS).enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
            if (method.equals("POST") || method.equals("PUT")) {
                conn.setDoOutput(true);
                reqBytes = mapper.writeValueAsBytes(obj);
                conn.setRequestProperty("Content-Length", "" + reqBytes.length);
            }

            if (SessionData.getToken() != null) {
                conn.setRequestProperty("Authorization", "Bearer " + SessionData.getToken());
            }

            if (method.equals("POST") || method.equals("PUT")) {
                OutputStream out = new BufferedOutputStream(conn.getOutputStream());

                out.write(reqBytes);

                Log.d("REQUEST_UTIL_SENT_DATA", mapper.writeValueAsString(obj));

                out.close();
            }


            // Get the response
            InputStream in = conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream();

            if (conn.getResponseCode() == 200) {
                if (!isCollection) {
                    result = mapper.readValue(in, returnType);
                } else {
                    CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, returnType);
                    mapper.getTypeFactory().constructCollectionType(List.class, History.class);
                    result = mapper.readValue(in, type);
                }
                Log.d("MARSHALL_RESULT", mapper.writeValueAsString(result));

                Log.d("GET_SUCCESS", "Succeeded");
                Log.d("REQ_CODE", "" + conn.getResponseCode());
                Log.d("REQ_MESSAGE", conn.getResponseMessage());


                Log.d("REQUEST_UTIL_RECV_DATA", result.toString());
            } else {
                byte[] resp = new byte[in.available()];
                int readLen = in.read(resp);
                String log = new String(resp);
                Log.d("HTTP_ERROR_LEN", "" + readLen);
                Log.d("HTTP_ERROR_CODE", "" + conn.getResponseCode() + " " + conn.getResponseMessage());
                Log.d("HTTP_ERROR_URL", conn.getURL().getHost() + " " + conn.getURL().getPath());
                Log.d("HTTP_ERROR", log);
            }

            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return result;
    }
}
