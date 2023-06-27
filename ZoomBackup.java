package zoom_recording;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
    public static void main(String[] args) throws Exception {
    	ZoomRecorder zoom = new ZoomRecorder();
    	zoom.getInput(args);
    	
    	int count = 0;
    	int maxTries = 10;
    	while(true) {
    		try {
        		zoom.run();
        		break;
        	} catch(Exception e) {
        		System.out.println("ERROR "+count+"/"+maxTries);
        		if (++count == maxTries) throw e;
        	}
    	}
    }
    
    private String accountId;
    private String clientId;
    private String clientSecret;
    private String path;
    private String oauth_token;
    private List<String> emails = new ArrayList<String>();
    private int startYear;
    private int endYear;
    
    public void getInput(String[] args) {
        String emailString = args[0];
        System.out.println("Email addresses to be downloaded : " + emailString);
        
        if(emailString != null) {
        	if(!emailString.contains(";")) {
        		emails.add(emailString);
        	} else {
        		for(String email : emailString.split(";")) {
        			emails.add(email);
        		}
        	}
        }
        
        accountId  = args[1];
        System.out.println("accountId : " + accountId);
        
        clientId  = args[2];
        System.out.println("clientId : " + clientId);
        
        clientSecret = args[3];
        System.out.println("client secret : " + clientSecret);
        
        path = args[4];
        System.out.println("PATH : " + path);
        
        int thisYear = Year.now().getValue();
        startYear = thisYear-1;
        endYear = thisYear;
        System.out.println("Search " + startYear + " to " + endYear);
    }
    
    public void run() throws Exception {
        oauth_token = getServerSideAuthToken();
        
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
                for(String email : emails)
                	getRecordings(startDate.format(formatter), endDate.format(formatter), email);
            }
        }
        System.out.println("End");
    }

    private void getRecordings(String fromDate, String toDate, String email) throws Exception {
    	System.out.println("Email : "+ email);
        String urlStr = "https://api.zoom.us/v2/users/" + email + "/recordings?from=" + fromDate + "&to=" + toDate + "&page_size=300";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + oauth_token);
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
            	/* Create Meetings folder */
            	String folderPath = path+this.reformatPath(meeting.getString("start_time"))+ " "+ this.reformatPath(meeting.getString("topic"));
            	File theDir = new File(folderPath);
            	if(!theDir.exists())
            		theDir.mkdir();
            	
            	System.out.println("Recording: "+(j+1)+" / " + recordingFiles.length());
            	JSONObject recording = recordingFiles.getJSONObject(j);
                if (recording.getString("status").equals("completed")) {
                	String fileName = recording.getString("recording_start").replace(":", "-")+meeting.getString("topic")+"-"+j;
                    downloadRecording(recording.getString("download_url"), fileName, folderPath+"/");
                }
            }
        }
    }
    
    private String reformatPath(String path) {
    	path = path.replaceAll("/", "");
    	path = path.replaceAll("\\\\", "");
    	path = path.replaceAll("|", "");
    	path = path.replaceAll(":", "");
    	path = path.replaceAll("\\?", "");
    	path = path.replaceAll("\"", "");
    	path = path.replaceAll("<", "");
    	path = path.replaceAll(">", "");
    	path = path.replaceAll("|", "");
    	path = path.replaceAll("\\s+$", "");
    	return path;
    }

    private void downloadRecording(String downloadUrl, String filename, String folderPath) throws Exception {
    	/* Check if file existing */
    	String filePath = folderPath + this.reformatPath(filename) + ".mp4";
    	File file = new File(filePath);
    	if(file.exists()) {
    		System.out.println("file eixsting : "+filename);
    		return;
    	}
    	
    	
    	System.out.println("Downlaoding : "+filename);
        URL url = new URL(downloadUrl + "?access_token=" + oauth_token);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "video/mp4");
        InputStream inputStream = conn.getInputStream();
        FileOutputStream outputStream = new FileOutputStream(filePath);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();
        System.out.println("Downloaded : "+filename);
    }
    
    public String getServerSideAuthToken() throws IOException, JSONException {
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
