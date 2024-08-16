package com.saiteja.Automation_Testing.group;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class UserAdd {

	private final RestTemplate restTemplate;

	@Autowired
	public UserAdd(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public static Socket s;
	private static String sessionId;
	private static int port = 2208;
	private static String user = "m106222";
	private static String SystemPassword = "Sai#123";
	private static String ip = "khk9dst11.ip.tdk.dk";

	@Value("${user.add.serviceProviderId}")
	private String serviceProviderId;

	@Value("${user.add.groupId}")
	private String groupId;

	@Value("${user.add.userId}")
	private String userId;

	@Value("${user.add.lastName}")
	private String lastName;

	@Value("${user.add.firstName}")
	private String firstName;

	@Value("${user.add.callingLineIdLastName}")
	private String callingLineIdLastName;

	@Value("${user.add.callingLineIdFirstName}")
	private String callingLineIdFirstName;

	@Value("${user.add.password}")
	private String password;

	@Value("${user.add.language}")
	private String language;

	@Value("${user.add.timeZone}")
	private String timeZone;

	@Value("${user.add.networkClassOfService}")
	private String networkClassOfService;

	@Value("${user.add.isPlace}")
	private boolean isPlace;

	public static void connect() {
		try {
			s = new Socket();
			s.connect(new InetSocketAddress(ip, port));
			System.out.println("Connected to the server.");
			s.setSoTimeout(60000);

			// Authenticate
			sessionId = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			String authCommand = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
					+ "<BroadsoftDocument protocol=\"OCI\" xmlns=\"C\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
					+ "<sessionId xmlns=\"\">" + sessionId + "</sessionId>"
					+ "<command xsi:type=\"AuthenticationRequest\" xmlns=\"\">" + "<userId>" + user + "</userId>"
					+ "</command>" + "</BroadsoftDocument>";

			sendCommand(authCommand);
			String authResponse = receiveResponse();
			//System.out.println("Authentication Response (XML):\n" + authResponse);

			String nonce = extractNonceFromResponse(authResponse);
			if (nonce == null) {
				System.out.println("Failed to extract nonce from the server response.");
				return;
			}
			//System.out.println("Extracted nonce: " + nonce);

			// Generate signed password
			String signedPassword = generateSignedPassword(nonce, SystemPassword);
			if (signedPassword == null || signedPassword.isEmpty()) {
				System.out.println("Failed to generate signed password.");
				return;
			}
			//System.out.println("Generated signed password: " + signedPassword);

			// Login
			String loginCommand = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
					+ "<BroadsoftDocument protocol=\"OCI\" xmlns=\"C\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
					+ "<sessionId xmlns=\"\">" + sessionId + "</sessionId>"
					+ "<command xsi:type=\"LoginRequest14sp4\" xmlns=\"\">" + "<userId>" + user + "</userId>"
					+ "<signedPassword>" + signedPassword + "</signedPassword>" + "</command>" + "</BroadsoftDocument>";

			sendCommand(loginCommand);
			String loginResponse = receiveResponse();
			//System.out.println("Server login response:\n" + loginResponse);

			if (!loginResponse.contains("LoginResponse14sp4")) {
				System.out.println("Login failed or unexpected login response.");
				return;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// User Add XML Request
	public String userAddRequest() throws Exception {
        // Ensure the socket connection is established
        if (s == null || s.isClosed()) {
            throw new IllegalStateException("Socket is not connected.");        
        }

        String UserAddRequest = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
                + "<BroadsoftDocument protocol=\"OCI\" xmlns=\"C\">"
                + "<sessionId xmlns=\"\">" + sessionId + "</sessionId>"
                + "<command xsi:type=\"UserAddRequest22V2\" xmlns=\"\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<serviceProviderId>" + serviceProviderId + "</serviceProviderId>"
                + "<groupId>" + groupId + "</groupId>"
                + "<userId>" + userId + "</userId>"
                + "<lastName>" + lastName + "</lastName>"
                + "<firstName>" + firstName + "</firstName>"
                + "<callingLineIdLastName>" + callingLineIdLastName + "</callingLineIdLastName>"
                + "<callingLineIdFirstName>" + callingLineIdFirstName + "</callingLineIdFirstName>"
                + "<password>" + password + "</password>"
                + "<language>" + language + "</language>"
                + "<timeZone>" + timeZone + "</timeZone>"
                + "<address/>"
                + "<networkClassOfService>" + networkClassOfService + "</networkClassOfService>"
                + "<isPlace>" + isPlace + "</isPlace>"
                + "</command>"
                + "</BroadsoftDocument>";

        sendCommand(UserAddRequest);
        String response = receiveResponse();
        
     // Extract domain from userId
        String userDomain = userId.substring(userId.indexOf("@") + 1);
        OCIResponse ociResponse = parseOCIResponse(response);
        
     // If user successfully added to the BW
        if(ociResponse.isSuccess()) {
        	System.out.println("User added is successfully to the BW.");
        	
        	// Check if user is whitelist user and invoke the get api
        	if(userDomain.startsWith(groupId.toLowerCase())){
        		System.out.println("User is not whitelist user..");
        		return "User found";
        	}	
        	else{
        		return checkUserStatus(userId);
        	}
        } else {
        	    System.out.println("UserAddRequest is unsuccessful " + ociResponse.getFailureReason());
        	    return "UserAddRequest is unsuccessful";
        }        
    }

	private String checkUserStatus(String userId) {
		String url = "http://localhost:8891/GetUser/" + userId;
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

		if (response.getStatusCode().is2xxSuccessful() && response.getBody().contains("User found")) {
			return "User found";
		} else {
			return "User not found";
		}
	}

	private static void sendCommand(String command) throws Exception {
		PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
		out.println(command);
		System.out.println("Command sent to the server:\n" + command);
	}

	private static String receiveResponse() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
		StringBuilder responseBuilder = new StringBuilder();
		String line;
		while ((line = in.readLine()) != null) {
			responseBuilder.append(line);
			System.out.println("Received line: " + line);
			if (line.contains("</BroadsoftDocument>")) {
				break;
			}
		}
		return responseBuilder.toString();
	}

	private static String extractNonceFromResponse(String response) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream is = new ByteArrayInputStream(response.getBytes("UTF-8"));
			Document doc = builder.parse(is);
			NodeList nodeList = doc.getElementsByTagName("nonce");
			if (nodeList.getLength() > 0) {
				return nodeList.item(0).getTextContent();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String generateSignedPassword(String nonce, String password) {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp",
					"C:\\Users\\2186068\\Downloads\\external_modules\\external_modules\\java", "com.crypt.Crypt",
					password, nonce);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			ArrayList<String> output = new ArrayList<>();
			while ((line = reader.readLine()) != null) {
				output.add(line);
			}
			String signedPassword = output.get(2);
			//System.out.println(signedPassword);
			int exitCode = process.waitFor();
			//System.out.println("Process exit code: " + exitCode);
			//System.out.println("Process output: " + output.toString().trim());
			return signedPassword;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private OCIResponse parseOCIResponse(String response) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream is = new ByteArrayInputStream(response.getBytes("ISO-8859-1"));
			Document doc = builder.parse(is);
			NodeList commandList = doc.getElementsByTagName("command");
			Node command = commandList.item(0);

			if (command != null
					&& command.getAttributes().getNamedItem("xsi:type").getTextContent().contains("SuccessResponse")) {
				return new OCIResponse(true, null);
			} else {
				NodeList nodeList = command.getChildNodes();
				StringBuilder failureReason = new StringBuilder();
				for (int i = 0; i < nodeList.getLength(); i++) {
					Node node = nodeList.item(i);
					failureReason.append(node.getTextContent()).append(" ");
				}
				return new OCIResponse(false, failureReason.toString().trim());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new OCIResponse(false, "Error parsing OCI response");
		}
	}

	private static class OCIResponse {

		private final boolean success;
		private final String failureReason;

		public OCIResponse(boolean success, String failureReason) {
			this.success = success;
			this.failureReason = failureReason;
		}

		public boolean isSuccess() {
			return success;
		}

		public String getFailureReason() {
			return failureReason;
		}
	}
}
