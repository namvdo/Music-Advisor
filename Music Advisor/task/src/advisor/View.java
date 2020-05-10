package advisor;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;

public class View {
    public static void printAuthView() {
        System.out.println("use this link to request the access code:");
        System.out.printf("%s/authorize?client_id=%s&redirect_uri=%s&response_type=code", SpotifyData.AUTH_SERVER_PATH, SpotifyData.CLIENT_ID, SpotifyData.REDIRECT_URL);
        System.out.println();
        System.out.println("waiting for code...");
    }

    public static void printText(Object str) {
        System.out.println(str);
    }

    public static void printAccessTokenView() {
        System.out.println("making http request for access_token...");
    }

    public static void printNewLine() {
        final String NEWLINE = System.getProperty("line.separator");
        System.out.println(NEWLINE);
    }
    public static void printNewReleases() throws IOException, InterruptedException {
        JsonObject albums = Controller.getNewReleases();
        albums.get("items").getAsJsonArray().forEach(item -> {
            var album = item.getAsJsonObject();
            var name = album.get("name").getAsString();
            var url = album.get("external_urls").getAsJsonObject().get("spotify").getAsString();
            var artists = new ArrayList<>();
            album.get("artists").getAsJsonArray().forEach(artist -> {
                artists.add(artist.getAsJsonObject().get("name").getAsString());
            });
            System.out.println(name);
            System.out.println(artists);
            System.out.println(url);
            System.out.println();
        });
    }

    public static String previousPage(String request, int limit) {
        return "";
    }

    public static void printCategoryNames() throws IOException, InterruptedException {
        var categories = Controller.getCategoryNames();
        categories.get("items").getAsJsonArray().forEach(item -> {
            var category = item.getAsJsonObject();
            var categoryName = category.get("name").getAsString();
            View.printText(categoryName);
        });
    }


    public static void printFeatures() throws IOException, InterruptedException {
        var features = Controller.getFeatures();
        var items = features.get("items").getAsJsonArray();
        items.forEach(item -> {
            var bar = item.getAsJsonObject();
            View.printText(bar.get("name").getAsString());
            View.printText(bar.get("owner").getAsJsonObject().get("external_urls").getAsJsonObject().get("spotify").getAsString());
            View.printNewLine();
        });
    }

    public static void printPlaylists(String categoryName) throws IOException, InterruptedException {
        var playlists = Controller.getPlaylists(categoryName);
        assert playlists != null;
        var items = playlists.get("playlists").getAsJsonObject().get("items").getAsJsonArray();
        items.forEach(item -> {
            var foo = item.getAsJsonObject();
            View.printText(foo.get("name").getAsString());
            View.printText(foo.get("external_urls").getAsJsonObject().get("spotify").getAsString());
            View.printNewLine();
        });
    }
}
