package advisor;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;

public class View {
    private static int currentNewReleasesPageIdx = 0;
    private static int currentCategoryNamesPageIdx = 0;
    private static int currentPlaylistsPageIdx = 0;
    private static int currentFeaturedPageIdx = 0;

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

    /**
     * this function prints new releases fetched on Spotify with pagination.
     *
     * @param limit  number of items displayed per page
     * @param isNext whether is is a "next" or a "prev" command, if the current command is "next" this value will be true.
     * @param based  if the value is true, then this will indicate a "new" command, otherwise we will pass "false" as a value of this parameter.
     * @throws IOException
     * @throws InterruptedException
     */
    public static void printNewReleases(int limit, boolean isNext, boolean based) throws IOException, InterruptedException {
        JsonObject albums = Controller.getNewReleases();
        var size = albums.get("items").getAsJsonArray().size(); // gets the size of all albums stored in an array.
        if (!isNext) { // if this variable has the value "false", meaning the current command is "prev"
            if (currentNewReleasesPageIdx - (2 * limit) < 0) { // minus limit will give us the first index of the current page, 2*limit gives us the previous page, if it less than 0, then there is no page left.
                System.out.println("No more pages.");
                return;
            }
            currentNewReleasesPageIdx -= limit; // because the command is "prev", then we minus the index before printing the list out.
        }
        if (based) {
            currentNewReleasesPageIdx = 0; // if the "based" is true, meaning this is the "new" command, then every time the "new" command is pressed we reset the index to 0.
        }
        if (currentNewReleasesPageIdx >= size) { // if it exceeds the size, then return.
            System.out.println("No more pages.");
            return;
        }
        if (isNext) { // check if this is the "next" command, if it is, increase the index.
            currentNewReleasesPageIdx += limit;
        }
        for (int i = currentNewReleasesPageIdx - limit; i < (Math.min(currentNewReleasesPageIdx, size)); i++) {
            var album = albums.get("items").getAsJsonArray().get(i).getAsJsonObject();
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
        }
        System.out.println("---PAGE " + (based ? 1 : (currentNewReleasesPageIdx) / limit) + " OF " + (int) Math.ceil((double) size / limit) + "---");
    }


    public static void printCategoryNames(int limit, boolean isNext, boolean based) throws IOException, InterruptedException {
        var categories = Controller.getCategoryNames();
        var size = categories.get("items").getAsJsonArray().size();
        if (!isNext) {
            if (currentCategoryNamesPageIdx - (2 * limit) < 0) {
                System.out.println("No more pages.");
                return;
            }
            currentCategoryNamesPageIdx -= limit;
        }
        if (based) {
            currentCategoryNamesPageIdx = 0;
        }
        if (currentCategoryNamesPageIdx >= size) {
            System.out.println("No more pages.");
            return;
        }
        if (isNext) {
            currentCategoryNamesPageIdx += limit;
        }
        for (int i = currentCategoryNamesPageIdx - limit; i < (Math.min(currentCategoryNamesPageIdx, size)); i++) {
            var category = categories.get("items").getAsJsonArray().get(i).getAsJsonObject();
            var categoryName = category.get("name").getAsString();
            System.out.println(categoryName);
        }
        System.out.println("---PAGE " + (based ? 1 : currentCategoryNamesPageIdx / limit) + " OF " + (int) Math.ceil((double) size / limit) + "---");
    }


    public static void printFeatures(int limit, boolean isNext, boolean based) throws IOException, InterruptedException {
        var features = Controller.getFeatures();
        var size = features.get("items").getAsJsonArray().size();
        if (!isNext) {
            if (currentFeaturedPageIdx - (2 * limit) < 0) {
                System.out.println("No more pages.");
                return;
            }
            currentFeaturedPageIdx -= limit;
        }
        if (based) {
            currentFeaturedPageIdx = 0;
        }
        if (currentFeaturedPageIdx >= size) {
            System.out.println("No more pages.");
            return;
        }
        if (isNext) {
            currentFeaturedPageIdx += limit;
        }

        for (int i = currentFeaturedPageIdx - limit; i < (Math.min(currentFeaturedPageIdx, size)); i++) {
            var item = features.get("items").getAsJsonArray().get(i).getAsJsonObject();
            System.out.println(item.get("name").getAsString());
            System.out.println(item.get("owner").getAsJsonObject().get("external_urls").getAsJsonObject().get("spotify").getAsString());
            System.out.println();
        }
        System.out.println("---PAGE " + (based ? 1 : currentFeaturedPageIdx / limit) + " OF " + (int) Math.ceil((double) size / limit) + "---");
    }

    public static void printPlaylists(String categoryName, int limit, boolean isNext, boolean based) throws IOException, InterruptedException {
        var playlists = Controller.getPlaylists(categoryName);
        if (playlists == null) {
            return;
        }
        var size = playlists.get("playlists").getAsJsonObject().get("items").getAsJsonArray().size();
        if (!isNext) {
            if (currentPlaylistsPageIdx - (2 * limit) < 0) {
                System.out.println("No more pages.");
                return;
            }
            currentPlaylistsPageIdx -= limit;
        }
        if (based) {
            currentPlaylistsPageIdx = 0;
        }
        if (currentPlaylistsPageIdx >= size) {
            System.out.println("No more pages.");
            return;
        }
        if (isNext) {
            currentPlaylistsPageIdx += limit;
        }
        for (int i = currentPlaylistsPageIdx - limit; i < (Math.min(currentPlaylistsPageIdx, size)); i++) {
            var item = playlists.get("playlists").getAsJsonObject().get("items").getAsJsonArray().get(i).getAsJsonObject();
            System.out.println(item.get("name").getAsString());
            System.out.println(item.get("external_urls").getAsJsonObject().get("spotify").getAsString());
            System.out.println();
        }
        System.out.println("---PAGE " + (based ? 1 : currentPlaylistsPageIdx / limit) + " OF " + (int) Math.ceil((double) size / limit) + "---");
    }
}
