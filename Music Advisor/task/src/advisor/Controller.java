package advisor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class Controller {
    private static String authCode = "";
    private static final String categoriesURL = SpotifyData.API_PATH + "v1/browse/categories";
    private static final String releasesURL = SpotifyData.API_PATH + "v1/browse/new-releases";
    private static final String featuredURL = SpotifyData.API_PATH + "v1/browse/featured-playlists";
    static String accessToken = "";

    /**
     * gets the authorization code after a user accepts the process, this function will mutates the @authCode variable which is defined inside this class.
     * @throws IOException
     * @throws InterruptedException
     */
    public static void getAuthCode() throws IOException, InterruptedException {
        HttpServer server = HttpServer.create();
        server.bind(new InetSocketAddress(8080), 0);
        server.start();
        View.printAuthView();
        server.createContext("/",
                exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    String result;
                    if (query != null && query.contains("code")) {
                        authCode = query.substring(5);
                        result = "Got the code. Return back to your program.";
                    } else {
                        result = "Not found authorization code. Try again.";
                    }
                    exchange.sendResponseHeaders(200, result.length());
                    exchange.getResponseBody().write(result.getBytes());
                    exchange.getResponseBody().close();
                    View.printText(result);
                }
        );
        while (authCode.equals("")) {
            Thread.sleep(10);
        }
        server.stop(10);
    }

    /**
     * After the authorization process has succeeded, this function get the access token and mutates the @accessToken variable.
     * @throws IOException
     * @throws InterruptedException
     */
    public static void getAccessToken() throws IOException, InterruptedException {
        View.printAccessTokenView();
        HttpRequest requestAccessToken = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(
                        "client_id=" + SpotifyData.CLIENT_ID
                                + "&client_secret=" + SpotifyData.CLIENT_SECRET
                                + "&grant_type=" + "authorization_code"
                                + "&code=" + authCode
                                + "&redirect_uri=" + SpotifyData.REDIRECT_URL_IN_QUERY_STRING))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create(SpotifyData.GET_ACCESS_TOKEN_FROM_URL))
                .build();
        HttpClient client = HttpClient.newBuilder().build();
        HttpResponse<String> responseWithAccessToken = client.send(requestAccessToken, HttpResponse.BodyHandlers.ofString());
        accessToken = JsonParser.parseString(responseWithAccessToken.body()).getAsJsonObject().get("access_token").getAsString();
    }
    /**
     * gets requested data based on URL provided.
     * @param requestedFeatureURL
     * @return a string represents the data fetched from the API.
     * @throws IOException
     * @throws InterruptedException
     */
    public static String getRequestedData(String requestedFeatureURL) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .uri(URI.create(requestedFeatureURL))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * gets new releases from Spotify
     * @return a JsonObject contains new releases data.
     * @throws IOException
     * @throws InterruptedException
     */
    public static JsonObject getNewReleases() throws IOException, InterruptedException {
        String verboseJson = getRequestedData(releasesURL);
        return JsonParser.parseString(verboseJson).getAsJsonObject().get("albums").getAsJsonObject();
    }

    /**
     * gets available category names on Spotify
     * @return a JsonObject contains category names data.
     * @throws IOException
     * @throws InterruptedException
     */
    public static JsonObject getCategoryNames() throws IOException, InterruptedException {
        var categories = getRequestedData(categoriesURL);
        return JsonParser.parseString(categories).getAsJsonObject().getAsJsonObject().get("categories").getAsJsonObject();
    }

    /**
     * this is a utility function to get a category ID based on category's name.
     * @param categoryName name of the category wanted to find the ID.
     * @return a string with the category's name.
     * @throws IOException
     * @throws InterruptedException
     */
    public static String getCategoryIdByCategoryName(String categoryName) throws IOException, InterruptedException {
        var categories = getRequestedData(categoriesURL);
        var items = JsonParser.parseString(categories).getAsJsonObject().get("categories").getAsJsonObject().get("items").getAsJsonArray();
        for (JsonElement item : items) {
            var name = item.getAsJsonObject().get("name").getAsString();
            if (categoryName.equals(name)) {
                return item.getAsJsonObject().get("id").getAsString();
            }
        }
        return null;
    }

    /**
     * gets the features from Spotify's API.
     * @return a JsonObject which represents features available on Spotify.
     * @throws IOException
     * @throws InterruptedException
     */
    public static JsonObject getFeatures() throws IOException, InterruptedException {
        var features = getRequestedData(featuredURL);
        return JsonParser.parseString(features).getAsJsonObject().get("playlists").getAsJsonObject();
    }

    /**
     * gets playlists by providing a category name
     * @param categoryName the name of the category wanted to find a playlist
     * @return a playlist of the provided category's name.
     * @throws IOException
     * @throws InterruptedException
     */
    public static JsonObject getPlaylists(String categoryName) throws IOException, InterruptedException {
        String categoryID = getCategoryIdByCategoryName(categoryName);
        if (categoryID == null) {
            View.printText("Unknown category name.");
            return null;
        }
        String playlistsURL = SpotifyData.API_PATH + "v1/browse/categories/" + categoryID + "/playlists";
        return JsonParser.parseString(getRequestedData(playlistsURL)).getAsJsonObject();
    }
}
