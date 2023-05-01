package zoom_recording;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

public class ZoomRecorder {

    private static String OAUTH_TOKEN = "";
    private static String USER_ID = "zoom1@fll.cc";
    private static String PATH = "";

    public static void main(String[] args) throws Exception {
    	Scanner myObj = new Scanner(System.in);  // Create a Scanner object
    	
    	System.out.println("Enter User ID");
        USER_ID = myObj.nextLine();  // Read user input
        
        System.out.println("Enter Oauth token");
        OAUTH_TOKEN = myObj.nextLine();  // Read user input
        
        System.out.println("Enter PATH");
        PATH = myObj.nextLine();  // Read user input
    	
    	System.out.println("Start");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int year = 2023; year <= 2023; year++) {
            for (int month = 1; month <= 12; month++) {
                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.plusMonths(1);

                if (month == 12) {
                    endDate = LocalDate.of(year + 1, 1, 1);
                }

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
}
