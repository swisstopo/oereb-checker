package ch.swisstopo.oerebchecker.utils;

import ch.swisstopo.oerebchecker.models.ResponseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.utils.StringUtils;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;

@SuppressWarnings("CommentedOutCode")
public class RequestHelper {
    protected static final Logger logger = LoggerFactory.getLogger(RequestHelper.class);

    private static String proxyHostname;
    private static int proxyPort = 8080;
    private static String proxyUser;
    private static String proxyPassword;

    private static HttpClient sharedClient;

    public static void setProxySettings(String proxyHostname, Integer proxyPort, String proxyUser, String proxyPassword) {
        RequestHelper.proxyHostname = proxyHostname;
        if (proxyPort != null) {
            RequestHelper.proxyPort = proxyPort;
        }
        RequestHelper.proxyUser = proxyUser;
        RequestHelper.proxyPassword = proxyPassword;

        logger.info("Proxy configured: {}:{} (User: {})", RequestHelper.proxyHostname, RequestHelper.proxyPort, (StringUtils.isNotBlank(RequestHelper.proxyUser) ? RequestHelper.proxyUser : "none"));

        synchronized (RequestHelper.class) {
            sharedClient = null;
        }
    }

    public static HttpClient getSharedHttpClient() {
        if (sharedClient == null) {
            synchronized (RequestHelper.class) {
                if (sharedClient == null) {
                    sharedClient = getNewHttpClient();
                }
            }
        }
        return sharedClient;
    }

    private static HttpClient getNewHttpClient() {

        HttpClient.Builder builder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(java.time.Duration.ofSeconds(10));

        if (StringUtils.isNotBlank(proxyHostname)) {
            ProxySelector defaultSelector = ProxySelector.of(new InetSocketAddress(proxyHostname, proxyPort));
            builder = HttpClient.newBuilder().proxy(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    List<Proxy> proxies = defaultSelector.select(uri);
                    logger.trace("Outgoing connection for {} will use proxy: {}", uri, proxies);
                    return proxies;
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    defaultSelector.connectFailed(uri, sa, ioe);
                }
            });

            if (StringUtils.isNotBlank(proxyUser) && StringUtils.isNotBlank(proxyPassword)) {
                builder.authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                    }
                });
            }
        }
        return builder.build();
    }

    public static URI buildUri(String urlTemplate, URI basicUri, ResponseFormat responseFormat, Map<String, Object> requestParams) {

        if (StringUtils.isBlank(urlTemplate)) {
            logger.error("createUri missing mandatory parameters: no urlTemplate");
            return null;
        }
        if (basicUri == null) {
            logger.error("createUri missing mandatory parameters: no basicUri");
            return null;
        }
        if (responseFormat == null) {
            logger.error("createUri missing mandatory parameters: no responseFormat");
            return null;
        }

        String urlString = "";
        try {
            urlString = String.format(urlTemplate, basicUri, responseFormat);
            URI uri = new URI(urlString);

            if (requestParams.isEmpty()) {
                logger.trace("Building URI without params: {}", uri);
                return uri;
            } else {
                StringBuilder params = new StringBuilder(StringUtils.isBlank(uri.getQuery()) ? "" : uri.getQuery() + "&");
                for (var entry : requestParams.entrySet()) {
                    params.append(entry.getKey())
                            .append("=")
                            .append(entry.getValue())
                            .append("&");
                }
                params.deleteCharAt(params.length() - 1);
                return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), params.toString(), null);
            }
        } catch (URISyntaxException e) {
            logger.error("createUrl error: Malformed URI '{}' or params: '{}'", urlString, requestParams);
        }
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    private static String getUserAgent() {
        return "Swisstopo-Oereb-Checker/1.0"; // "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36";
    }

    public static HttpRequest createRequest(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri must not be null");
        }
        return HttpRequest.newBuilder()
                .uri(uri)
                // add more header information like this to not get blocked by the server
                .header("User-Agent", getUserAgent())
                .build();
    }

    /*
    public static java.net.http.HttpResponse<String> sendWithRetry(java.net.http.HttpRequest request, int maxRetries) {
        int attempt = 0;
        Exception lastException = null;
        while (attempt < maxRetries) {
            logger.trace("Attempting request to {}. Attempt {} of {}", request.uri(), attempt + 1, maxRetries);
            try {
                return getSharedHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            } catch (java.io.IOException | InterruptedException e) {
                lastException = e;
                attempt++;
                logger.warn("Request failed (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
            }
        }
        logger.error("Request failed after {} attempts", maxRetries, lastException);
        return null;
    }
    */
}
