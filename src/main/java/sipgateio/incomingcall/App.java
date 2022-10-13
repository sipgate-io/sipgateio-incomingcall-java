package sipgateio.incomingcall;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class App {

	private static int port;

	public static void main(String[] args) throws IOException {
		loadConfiguration();
		SimpleHttpServer simpleHttpServer = new SimpleHttpServer(port);
		simpleHttpServer.addPostHandler("/", App::handlePost);
		simpleHttpServer.start();
	}

	private static void handlePost(HttpExchange httpExchange) throws IOException {
		InputStream requestBody = httpExchange.getRequestBody();
		OutputStream responseBody = httpExchange.getResponseBody();
		Headers responseHeaders = httpExchange.getResponseHeaders();

		BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
		String urlEncodedContent = reader.readLine();

		Map<String, String> keyValuePairs = parseUrlEncodedLine(urlEncodedContent);
		printCallEvent(keyValuePairs);

		String res = "This is a dummy response";

		responseHeaders.set("ContentType", "Application/XML");
		httpExchange.sendResponseHeaders(200, res.length());

		responseBody.write(res.getBytes());
		requestBody.close();
		responseBody.close();
	}

	private static void printCallEvent(Map<String, String> keyValuePairs) {

		System.out.println("********************");
		System.out.println("Received call event:");
		keyValuePairs.forEach(
				(key, value) -> {
					String pair = String.format("%s: %s", key, value);
					System.out.println(pair);
				});
	}

	private static Map<String, String> parseUrlEncodedLine(String line) throws UnsupportedEncodingException {
		String[] pairs = line.split("&");
		Map<String, String> keyValuePairs = new HashMap<>();

		for (String pair : pairs) {
			String[] fields = pair.split("=");
			String key = URLDecoder.decode(fields[0], "UTF-8");
			String value = URLDecoder.decode(fields[1], "UTF-8");

			keyValuePairs.put(key, value);
		}

		return keyValuePairs;
	}

	private static void loadConfiguration() {
		Dotenv dotenv = Dotenv.load();
		String portString = dotenv.get("WEBHOOK_PORT");
		try {
			port = Integer.parseInt(portString);
		} catch (NumberFormatException e) {
			System.err.println("Could not parse " + portString + " to int");
			System.exit(5);
		}
	}
}
