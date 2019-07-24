<img src="https://www.sipgatedesign.com/wp-content/uploads/wort-bildmarke_positiv_2x.jpg" alt="sipgate logo" title="sipgate" align="right" height="112" width="200"/>

# sipgate.io Java incoming call example
This example demonstrates how to receive and process webhooks from [sipgate.io](https://developer.sipgate.io/).

For further information regarding the push functionalities of sipgate.io please visit https://developer.sipgate.io/push-api/api-reference/

- [Prerequisites](#Prerequisites)
- [Enabling sipgate.io for your sipgate account](#Enabling-sipgateio-for-your-sipgate-account)
- [How sipgate.io webhooks work](#How-sipgateio-webhooks-work)
- [Configure webhooks for sipgate.io](#Configure-webhooks-for-sipgateio)
- [Making your computer accessible from the internet](#Making-your-computer-accessible-from-the-internet)
- [Install dependencies:](#Install-dependencies)
- [Execution](#Execution)
- [How It Works](#How-It-Works)
- [Common Issues](#Common-Issues)
- [Contact Us](#Contact-Us)
- [License](#License)

## Prerequisites
- JDK 8
- Gradle

## Enabling sipgate.io for your sipgate account
In order to use sipgate.io, you need to book the corresponding package in your sipgate account. The most basic package is the free **sipgate.io S** package.

If you use [sipgate basic](https://app.sipgatebasic.de/feature-store) or [simquadrat](https://app.simquadrat.de/feature-store) you can book packages in your product's feature store.
If you are a _sipgate team_ user logged in with an admin account you can find the option under **Account Administration**&nbsp;>&nbsp;**Plans & Packages**.


## How sipgate.io webhooks work

### What is a webhook?
A webhook is a POST request that sipgate.io makes to a predefined URL when a certain event occurs.
These requests contain information about the event that occurred in `application/x-www-form-urlencoded` format. You can find more information on this format in the pertinent documentation.

This is an example payload converted from `application/x-www-form-urlencoded` to json:
```json
{
  "event": "newCall",
  "direction": "in",
  "from": "492111234567",
  "to": "4915791234567",
  "callId":"12345678",
  "origCallId":"12345678",
  "user": [ "Alice" ],
  "xcid": "123abc456def789",
  "diversion": "1a2b3d4e5f"
}
```


### sipgate.io webhook events
sipgate.io offers webhooks for the following events:

- **newCall:** is triggered when a new incoming or outgoing call occurs 
- **onAnswer:** is triggered when a call is answered – either by a person or an automatic voicemail
- **onHangup:** is triggered when a call is hung up
- **dtmf:** is triggered when a user makes an entry of digits during a call

**Note:** Per default sipgate.io only sends webhooks for **newCall** events.
To subscribe to other event types you can reply to the **newCall** event with an XML response.
This response includes the event types you would like to receive webhooks for as well as the respective URL they should be directed to.
You can find more information about the XML response here:
https://developer.sipgate.io/push-api/api-reference/#the-xml-response


## Configure webhooks for sipgate.io 
You can configure webhooks for sipgate.io as follows:

1. Navigate to [console.sipgate.com](https://console.sipgate.com/) and login with your sipgate account credentials.
2. Select the **Webhooks**&nbsp;>&nbsp;**URLs** tab in the left side menu
3. Click the gear icon of the **Incoming** or **Outgoing** entry
4. Fill in your webhook URL and click save. **Note:** your webhook URL has to be accessible from the internet. (See the section [Making your computer accessible from the internet](#making-your-computer-accessible-from-the-internet)) 
5. In the **sources** section you can select what phonelines and groups should trigger webhooks.


## Making your computer accessible from the internet
There are many possibilities to obtain an externally accessible address for your computer.
In this example we use the service [serveo.net](serveo.net) which sets up a reverse ssh tunnel that forwards traffic from a public URL to your localhost.
The following command creates the specified subdomain at serveo.net and sets up a tunnel between the public port 80 on their server and your localhost:8080:

```bash
$ ssh -R [subdomain].serveo.net:80:localhost:8080 serveo.net
```

If you run this example on a server which can already be reached from the internet, you do not need the forwarding.
In that case, the webhook URL needs to be adjusted accordingly.


## Execution
Navigate to the project's root directory.

Run the application:

```bash
./gradlew run
```

## How It Works
On the top level, the code is very simple: 
```java
SimpleHttpServer simpleHttpServer = new SimpleHttpServer(8080);
simpleHttpServer.addPostHandler("/", App::handlePost);
simpleHttpServer.start();
```
A `SimpleHttpServer` is instantiated with a port it will be listening on. The `addPostHandler()` method specifies a function that should be called when the server receives a POST request on the route `"/"`. With that configuration done, all that's left to do is start the server.

For the sake of simplicity, we will not cover the inner implementation of the `SimpleHttpServer` class. It is simply a wrapper around the `HttpServer` class from `com.sun.net.httpserver` that abstracts the filtering of request methods.

The two arguments of the `addPostHandler()` method are a context string specifying the intended endpoint, and a callback function to be called in the event of a POST request received there. The callback function needs to be of the type `HttpHandler` as specified by an interface from the `httpserver` package. The interface requires a `void` method that takes a single input of type `HttpExchange`.

The `handlePost` function (referenced as `App::handlePost`) implements that interface: it takes the `HttpExchange`, extracts the `application/x-www-form-urlencoded` request body (the webhook content), decodes it, and prints it to the console. Finally, a response is written to the `HttpExchange` to inform the sipgate.io server that the webhook has been received successfully.

```java
InputStream requestBody = httpExchange.getRequestBody();
OutputStream responseBody = httpExchange.getResponseBody();
Headers responseHeaders = httpExchange.getResponseHeaders();
```

The `requestBody` and `responseBody` from the `httpExchange` are an input and an output stream, respectively (i.e. you can _read_ from the former and _write_ to the latter); the `responseHeaders` are a special kind of `Map` that is initially empty. Later on the `"ContentType"` header will be added here.

```java
BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
		String urlEncodedContent = reader.readLine();
```

In order to output the content of the `requestBody`, it first has to be transferred into a `BufferedReader`. From there, the first line (there should be only one line in the webhook body) is read.

```java
Map<String, String> keyValuePairs = parseUrlEncodedLine(urlEncodedContent);
```

Since the content of the webhook is `application/x-www-form-urlencoded` it has to be processed in order to be more readable. This task is handled in the `parseUrlEncodedLine` function:

```java
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
```

The function takes a `application/x-www-form-urlencoded` line and splits it at the `&` character yielding an array of strings, each of which represents a key-value pair. A pair, in turn, contains an `=` character as delimiter and is thus split accordingly. Lastly, both key and value are decoded using the UTF-8 charset and returned in a map.

The resulting map is passed to the `printCallEvent` function which simply prints each pair to the console.

This is also where an XML response would be sent to further interact with sipgate.io, e.g. subscribe to more webhook events. This topic, however, will be covered in a later code example.

```java
responseHeaders.set("ContentType", "Application/XML");
httpExchange.sendResponseHeaders(200, res.length());
```

Since sipgate.io expects an XML response, the `"ContentType"` header needs to be set as `"Application/XML"`. To signal everything is OK, the HTTP status is set as `200`.

Finally, the dummy response is written to the `responseBody` and both streams are closed.

## Common Issues

### web app displays "Feature sipgate.io not booked."
Possible reasons are:
- the sipgate.io feature is not booked for your account

See the section [Enabling sipgate.io for your sipgate account](#enabling-sipgateio-for-your-sipgate-account) for instruction on how to book sipgate.io


### "java.net.BindException: Address already in use"

Possible reasons are:
- another instance of the application is running
- the port configured is used by another application.


### "java.net.SocketException: Permission denied"

Possible reasons are:
- you do not have the permission to bind to the specified port. This can happen if you use port 80, 443 or another well-known port which you can only bind to if you run the application with superuser privileges.


### Call happened but no webhook was received 
Possible reasons are:
- the configured webhook URL is incorrect
- the SSH tunnel connection broke
- webhooks are not enabled for the phoneline that received the call


## Contact Us
Please let us know how we can improve this example.
If you have a specific feature request or found a bug, please use **Issues** or fork this repository and send a **pull request** with your improvements.


## License
This project is licensed under **The Unlicense** (see [LICENSE file](./LICENSE)).

---

[sipgate.io](https://www.sipgate.io) | [@sipgateio](https://twitter.com/sipgateio) | [API-doc](https://api.sipgate.com/v2/doc)
