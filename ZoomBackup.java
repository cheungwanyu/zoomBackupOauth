package zoom_recording;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ZoomRecorder {

    private static String OAUTH_TOKEN = "";
    private static String USER_ID = "zoom1@fll.cc";
    private static String PATH = "";

    public static void main(String[] args) throws Exception {
    	Scanner myObj = new Scanner(System.in);  // Create a Scanner object
    	
    	System.out.println("Enter Email Address");
        USER_ID = myObj.nextLine();  // Read user input
        
        System.out.println("Enter accountId");
        String accountId  = myObj.nextLine();  // Read user input
        
        System.out.println("Enter clientId");
        String clientId  = myObj.nextLine();  // Read user input
        
        System.out.println("Enter client secret");
        String clientSecret  = myObj.nextLine();  // Read user input
        
        OAUTH_TOKEN = getServerSideAuthToken(accountId, clientId, clientSecret);
        
        System.out.println("Enter PATH");
        PATH = myObj.nextLine();  // Read user input
    	
    	

        System.out.println("Enter search range e.g. 2022-2023");
        String searchYears = myObj.nextLine();
        int startYear = Integer.parseInt(searchYears.split("-")[0]);
        int endYear = Integer.parseInt(searchYears.split("-")[1]);
        
        System.out.println("Start");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int year = startYear; year <= endYear; year++) {
            for (int month = 1; month <= 12; month++) {
                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.plusMonths(1);

                if (month == 12) {
                    endDate = LocalDate.of(year + 1, 1, 1);
                }
                System.out.println("Searching : "+year+"-"+month);
                getRecordings(startDate.format(formatter), endDate.format(formatter), PATH);
            }
        }
        System.out.println("End");
    }

    private static void getRecordings(String fromDate, String toDate, String path) throws Exception {
        String urlStr = "https://api.zoom.us/v2/users/" + USER_ID + "/recordings?from=" + fromDate + "&to=" + toDate + "&page_size=300";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + OAUTH_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");

        InputStream inputStream = conn.getInputStream();
   
        
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();
        String data =  sb.toString();
        
        
        inputStream.close();

        JSONObject response = new JSONObject(data);
        JSONArray meetings = response.getJSONArray("meetings");

        for (int i=0 ; i < meetings.length() ; i++) {
        	System.out.println("Meeting: "+(i+1)+" / " + meetings.length());
        	JSONObject meeting = meetings.getJSONObject(i);
        	JSONArray recordingFiles = meeting.getJSONArray("recording_files");

            for (int j=0 ; j < recordingFiles.length() ; j++) {
            	System.out.println("Recording: "+(j+1)+" / " + recordingFiles.length());
            	JSONObject recording = recordingFiles.getJSONObject(j);
                if (recording.getString("status").equals("completed")) {
                	String fileName = recording.getString("recording_start").replace(":", "-")+meeting.getString("topic")+"-"+j;
                    downloadRecording(recording.getString("download_url"), fileName , path);
                }
            }
        }
    }

    private static void downloadRecording(String downloadUrl, String filename, String path) throws Exception {
    	System.out.println("Downlaoding : "+filename);
        URL url = new URL(downloadUrl + "?access_token=" + OAUTH_TOKEN);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "video/mp4");
        InputStream inputStream = conn.getInputStream();
        FileOutputStream outputStream = new FileOutputStream(path + filename + ".mp4");
        byte[] buffer = new byte[4096];
        int bytesRead = -1;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();
        System.out.println("Downloaded : "+filename);
    }
    
    public static String getServerSideAuthToken(String accountId, String clientId, String clientSecret) throws IOException, JSONException {
        HttpClient httpClient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost("https://zoom.us/oauth/token?grant_type=account_credentials&account_id="+accountId);
        String auth = clientId + ":" + clientSecret;
        byte[] encodedAuth = java.util.Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            String responseBody = EntityUtils.toString(entity, "UTF-8");
            JSONObject json = new JSONObject(responseBody);
            String authToken = json.getString("access_token");
            return authToken;
        }
        return null;
    }
}
