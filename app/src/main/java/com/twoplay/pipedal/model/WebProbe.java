package com.twoplay.pipedal.model;

import android.net.InetAddresses;
import android.os.Handler;
import android.util.Log;

import com.twoplay.pipedal.Promise;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 04/08/2024.
 */
public class WebProbe {

    Handler handler;

    public static boolean checkForPipedalWebsite(String webAddress)  throws Exception {
        // get manifest.json from the website, and verify that it contains "short_name": "PiPedal"
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            String urlString = webAddress + "/manifest.json";
            URL url = new URL(urlString);


            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) throw new Exception("Invalid response code: " + responseCode);

            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line = "";
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
