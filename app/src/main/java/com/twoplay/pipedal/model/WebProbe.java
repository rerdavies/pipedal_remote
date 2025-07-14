package com.twoplay.pipedal.model;

import android.os.Handler;
import android.util.Log;

import com.twoplay.pipedal.Promise;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 04/08/2024.
 */
public class WebProbe {

    Handler handler;
    Thread thread;
    public static Promise<Boolean> checkForPiPedalWebsiteAsync(final String webAddress) {
        final Handler handler = new Handler();
        return new Promise<>(
                (completion) -> {
                    final Thread thread = new Thread(() -> {
                        try {
                            boolean result = checkForPiPedalWebsite("http://" + webAddress);
                            handler.post(() -> {
                                completion.fulfill(result);
                            });
                        } catch (Exception e) {
                            handler.post(() -> {
                                completion.reject(e);

                            });
                        }
                    });
                    thread.start();
                }
        );
    }
    public static boolean checkForPiPedalWebsite(String webAddress)  throws Exception {
        // get manifest.json from the website, and verify that it contains "short_name": "PiPedal"
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            String urlString = webAddress + "/manifest.json";
            URL url = new URL(urlString);


            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode >= 300) throw new Exception("Invalid response code: " + responseCode);

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
            } catch (Exception e) {
                throw new Exception("Content doesn't match.");
            }
            String response = responseBuilder.toString();

            if (responseCode != 200) {
                StringBuilder s = new StringBuilder();
                s.append("HTTP Error: ");
                s.append(responseCode);
                if (!response.isEmpty())
                {
                    s.append("\n\n");
                    s.append(response);
                }
                throw new Exception("Invalid response code: " + responseCode);
            }

            int pos = response.indexOf("\"short_name\": \"PiPedal\"");
            if (pos == -1) {
                return false;
            }

            return true;
        } catch (Exception e)
        {
            Log.e("WebProbe",e.getMessage());
            throw e;
        } finally
        {
            if (reader != null)
            {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
            try {
                if (connection != null) connection.disconnect();
            } catch (Exception ignored) {

            }
        }
    }

}
