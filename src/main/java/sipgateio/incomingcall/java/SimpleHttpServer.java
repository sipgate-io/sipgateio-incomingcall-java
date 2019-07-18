package sipgateio.incomingcall.java;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

class SimpleHttpServer {

	private HttpServer httpServer;

	SimpleHttpServer(int port) throws IOException {
		httpServer = HttpServer.create(new InetSocketAddress(port), 0);
	}

	void addPostHandler(String contextString, HttpHandler requestHandler) {
		httpServer.createContext(contextString, httpExchange -> {
			if (httpExchange.getRequestMethod().equals("POST")) {
				requestHandler.handle(httpExchange);
			} else {
				String response = "Method not allowed";
				httpExchange.sendResponseHeaders(405, response.length());
				OutputStream out = httpExchange.getResponseBody();
				out.write(response.getBytes());
				out.close();
			}
		});
	}

	void start() {
		httpServer.start();
		System.out.println("Listening on port: " + httpServer.getAddress().getPort());
	}

}
