package com.johnymuffin.beta.updater;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.logging.Level;

public class ManifestFetcher {

    public static JSONArray getManifest(MuffinUpdater plugin, String url, boolean debug) {
        try {
            InputStream inputStream = new URL(url).openStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            inputStream.close();
            JSONParser parser = new JSONParser();
            return (JSONArray) parser.parse(jsonText);
        } catch (IOException | ParseException e) {
            if(!debug) {
                plugin.logInfo(Level.WARNING, "Unable to fetch the update manifest. Either the requested website if offline, or your internet is offline.");
            } else {
                plugin.logInfo(Level.WARNING, e + " - " + e.getMessage());
            }
            return null;

        }

    }


    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }


}
