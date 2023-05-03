Zoom Recorder

This is a Java program that downloads recordings from Zoom using the Zoom API.

Requirements

Java 8 or higher

Zoom account with API access enabled

Zoom OAuth credentials (client ID and client secret)


Usage

Clone the repository.

Open the command prompt or terminal and navigate to the project directory.

Run the program with the following command: java -jar zoom_recorder.jar

Enter your email address, account ID, client ID, client secret, and the path where you want to save the recordings.

Enter the search range in the format yyyy-yyyy (e.g., 2022-2023).

The program will start downloading the recordings for the specified time period.


Notes

The program will download recordings only for meetings that have been completed.

The program downloads recordings in the MP4 format.

The program downloads recordings for all meetings, including recurring meetings.

The program can download a maximum of 300 recordings per request. If you have more than 300 recordings in a month, you may need to run the program multiple times for that month.

The program may take a long time to download all recordings, depending on the number of recordings and your internet speed.

The program will create a separate file for each recording. The file name will be in the format yyyy-MM-dd-HH-mm-ss-MeetingTopic-RecordingNumber.mp4.

The program will overwrite any existing files with the same name in the specified path.


Credits

This program uses the following libraries:

Apache HttpClient

JSON in Java by Douglas Crockford
