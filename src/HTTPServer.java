import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTTPServer implements Runnable {

    static final File WEB_ROOT = new File(".");
    static final String DOCUMENT_ROOT = "www/";
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_ALLOWED = "405.html";
    static final String BAD_REQUEST = "400.html";
    static final String UNSUPPORTED_MEDIA = "415.html";

    static final int PORT = 8083;

    private Socket clientSocket;

    public HTTPServer(Socket s) {
        clientSocket = s;
    }

    public static void main(String[] args) {

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port: " + PORT + "...\n");

            while (true) {
                HTTPServer myServer = new HTTPServer(serverSocket.accept());

                if (true) {
                    System.out.println("Connection opened. (" + new Date() + ")");
                }

                Thread thread = new Thread(myServer);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("Server Connection error: " + e.getMessage());
        }
    }

    @Override
    public void run() {

        BufferedReader in = null;
        PrintWriter out = null;
        DataOutputStream dataOut = null;
        String fileRequested = null;

        try {

            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream());
            dataOut = new DataOutputStream(clientSocket.getOutputStream());

            String requestHeaders = in.readLine();
            if (requestHeaders.isEmpty()) {
                clientSocket.close();
                System.out.println("Not a valid request. Connection closed (" + new Date() + ")");
                return;
            }

            StringTokenizer parse = new StringTokenizer(requestHeaders);
            String httpVerb = parse.nextToken().toUpperCase();
            fileRequested = parse.nextToken().toLowerCase();

            if (!httpVerb.equals("GET") && !httpVerb.equals("HEAD")) {
                if (true) {
                    System.out.println("405 Not Allowed: " + httpVerb + " httpVerb.");
                }

                File file = new File(WEB_ROOT, METHOD_NOT_ALLOWED);
                int fileLength = (int) file.length();
                String contentMimeType = "text/html";
                byte[] fileData = readFileData(file, fileLength);

                out.println("HTTP/1.1 405 Not Allowed");
                out.println("Server: Java HTTP Server from deiaff : 1.0");
                out.println("Content-type: " + contentMimeType);
                out.println("Content-length: " + fileLength);
                out.println();
                out.flush();

                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();
            }

            if (fileRequested == null) {
                if (true) {
                    System.out.println("400 Bad Request: resource not specified");
                }

                File file = new File(WEB_ROOT, BAD_REQUEST);
                int fileLength = (int) file.length();
                String contentMimeType = "text/html";
                byte[] fileData = readFileData(file, fileLength);

                out.println("HTTP/1.1 400 Bad Request");
                out.println("Server: Java HTTP Server from deiaff : 1.0");
                out.println("Content-type: " + contentMimeType);
                out.println("Content-length: " + fileLength);
                out.println();
                out.flush();

                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();
            }

            String filePath = getPathForResource(fileRequested);
            if (!HttpMedia.isSupported(filePath)) {
                if (true) {
                    System.out.println("415 Unsupported Media Type");
                }

                File file = new File(WEB_ROOT, UNSUPPORTED_MEDIA);
                int fileLength = (int) file.length();
                String contentMimeType = "text/html";
                byte[] fileData = readFileData(file, fileLength);

                out.println("HTTP/1.1 415 Unsupported Media Type");
                out.println("Server: Java HTTP Server from deiaff : 1.0");
                out.println("Content-type: " + contentMimeType);
                out.println("Content-length: " + fileLength);
                out.println();
                out.flush();

                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();



            } else {
                if(fileRequested.endsWith("/")) {
                    fileRequested += DEFAULT_FILE;
                }

                File file = new File(WEB_ROOT, fileRequested);
                int fileLength = (int) file.length();
                String content = getContentType(fileRequested);

                if(httpVerb.equals("GET")) {
                    byte[] fileData = readFileData(file,fileLength);

                    out.println("HTTP/1.1 200 OK. Document follows");
                    out.println("Server: Java HTTP Server from deiaff: 1.0");
                    out.println("Content-type: " + content);
                    out.println("Content-length: " + fileLength);
                    out.println();
                    out.flush();


                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();
                }

                if (true) {
                    System.out.println("File " + fileRequested + "of type " + content + " returned");
                }
            }

        } catch (FileNotFoundException fileNotFoundException) {
            try {
                fileNotFound(out, dataOut, fileRequested);
            } catch (IOException ioe) {
                System.err.println("Error with file not found exception : " + ioe.getMessage());
            }

        } catch (IOException ioe) {
            System.err.println("Server error : " + ioe);
        } finally {
            try {
                in.close();
                out.close();
                dataOut.close();
                clientSocket.close();

            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }

            if (true) {
                System.out.println("Connection closed. (" + new Date() + ") \n");
            }
        }
    }

    private byte[] readFileData(File file, int fileLength) throws IOException {

        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);

        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return fileData;
    }

    private String getContentType(String fileRequested) {

        if (HttpMedia.isImage(fileRequested)) {
            return " image/" + HttpMedia.getExtension(fileRequested);
        }

        return " text/html; charset=UTF-8";

    }

    private String getPathForResource(String resource) {

        String filePath = resource;

        Pattern pattern = Pattern.compile("(\\.[^.]+)$"); // regex for file extension
        Matcher matcher = pattern.matcher(filePath);

        if (!matcher.find()) {
            filePath += "/index.html";
        }

        filePath = HTTPServer.DOCUMENT_ROOT + filePath;

        return filePath;
    }


    private void fileNotFound(PrintWriter out, DataOutputStream dataOut, String fileRequested) throws IOException {

        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
        int fileLength = (int) file.length();
        String content = "Text/html";
        byte[] fileData = readFileData(file, fileLength);

        out.println("HTTP/1.1 404 File Not Found");
        out.println("Server: Java HTTP Server from deiaff: 1.0");
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);
        out.println();
        out.flush();


        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();

        if (true) {
            System.out.println("File " + fileRequested + " not found");
        }
    }
}
