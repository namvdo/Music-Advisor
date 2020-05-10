import advisor.Main;
import org.hyperskill.hstest.dynamic.output.SystemOutHandler;
import org.hyperskill.hstest.mocks.web.WebServerMock;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testcase.TestCase;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

class RedirectUriFinder {
    
    private Thread thread;
    
    volatile CheckResult checkResult = CheckResult.correct();
    
    private String fictiveAuthCode;
    
    RedirectUriFinder(String fictiveAuthCode) {
        this.fictiveAuthCode = fictiveAuthCode;
    }
    
    void start() {
        // this message will be ignored, if user program hangs
        checkResult = CheckResult.wrong("Not found a link with redirect_uri.");
        thread = new Thread(() -> {
            String redirectUri = "";
            long searchTime = System.currentTimeMillis();
            
            while (!Thread.interrupted()) {
                if (System.currentTimeMillis() - searchTime > 1000 * 9) {
                    System.out.println("Tester: Not found a link with redirect_uri after 9 seconds. Stopping.");
                    return;
                }
                
                String out = SystemOutHandler.getDynamicOutput();
                if (out.contains("redirect_uri=")) {
                    redirectUri = out.split("redirect_uri=")[1];
                    if (redirectUri.contains("&")) {
                        redirectUri = redirectUri.split("&")[0];
                    }
                    if (redirectUri.contains("\n")) {
                        // \r \n or \r\n
                        redirectUri = redirectUri.split("\\R")[0];
                    }
                    break;
                }
                
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    return;
                }
            }
            try {
                HttpClient client = HttpClient.newBuilder().build();
                HttpRequest emptyRequest = HttpRequest.newBuilder()
                        .uri(URI.create(redirectUri))
                        .timeout(Duration.ofMillis(500))
                        .GET()
                        .build();
                HttpRequest errorRequest = HttpRequest.newBuilder()
                        .uri(URI.create(redirectUri + "?error=access_denied"))
                        .timeout(Duration.ofMillis(500))
                        .GET()
                        .build();
                HttpRequest codeRequest = HttpRequest.newBuilder()
                        .uri(URI.create(redirectUri + "?code=" + fictiveAuthCode))
                        .timeout(Duration.ofMillis(500))
                        .GET()
                        .build();
                
                checkResult = CheckResult.wrong("Making request to " + redirectUri + " was not finished.");
                System.out.println("Tester: making requests to redirect uri: " + redirectUri);
                HttpResponse<String> badResponse = client.send(emptyRequest, HttpResponse.BodyHandlers.ofString());
                System.out.println("Tester: done request 1: " + badResponse.body());
                HttpResponse<String> badResponse2 = client.send(errorRequest, HttpResponse.BodyHandlers.ofString());
                System.out.println("Tester: done request 2: " + badResponse2.body());
                HttpResponse<String> goodResponse = client.send(codeRequest, HttpResponse.BodyHandlers.ofString());
                System.out.println("Tester: done request 3: " + goodResponse.body());
                
                if (!badResponse.body().contains("Not found authorization code. Try again.")
                        || !badResponse2.body().contains("Not found authorization code. Try again.")) {
                    checkResult = CheckResult.wrong("You should send to the browser: `Not found authorization code. Try again.` " +
                            "if there is no code.");
                    return;
                }
                
                if (!goodResponse.body().contains("Got the code. Return back to your program.")) {
                    checkResult = CheckResult.wrong("You should send `Got the code. Return back to your program.` " +
                            "if the query contains the code.");
                    return;
                }
                checkResult = CheckResult.correct();
            } catch (HttpTimeoutException e) {
                System.out.println("Tester: Timeout");
                // this checkResult will be ignored in most cases (if user program hangs)
                checkResult = CheckResult.wrong("Not received any response from the server, found in redirect_uri: "
                        + redirectUri);
            } catch (InterruptedException e) {
                // when the user printed token, but not answered the last request with code.
                checkResult = CheckResult.wrong("Request to " + redirectUri + " was interrupted. " +
                        "Make sure, that you give the right feedback in your browser.");
            } catch (Exception e) {
                System.out.println("Tester: Error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        thread.start();
    }
    
    void stop() {
        if (thread != null) {
            thread.interrupt();
            try {
                // wait the thread to set a proper checkResult in case of interruption.
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}


public class MusicAdvisorTest extends StageTest<Void> {
    public MusicAdvisorTest() throws Exception {
        super(Main.class);
    }
    
    private int countAppearances(String str, String findStr) {
        int lastIndex = 0;
        int count = 0;
        
        while (lastIndex != -1) {
            lastIndex = str.indexOf(findStr, lastIndex);
            if (lastIndex != -1) {
                count++;
                lastIndex += findStr.length();
            }
        }
        return count;
    }
    
    private static final String fictiveAuthCode = "123123";
    private static final String fictiveAccessToken = "456456";
    private static final String fictiveRefreshToken = "567567";
    
    private String tokenResponse = "{" +
            "\"access_token\":\"" + fictiveAccessToken + "\"," +
            "\"token_type\":\"Bearer\"," +
            "\"expires_in\":3600," +
            "\"refresh_token\":" + "\"" + fictiveRefreshToken + "\"," +
            "\"scope\":\"\"" +
            "}";
    
    private RedirectUriFinder redirectUriCatcher = new RedirectUriFinder(fictiveAuthCode);
    
    private int accessServerPort = 45678;
    private int resourceServerPort = 56789;
    
    private String accessServerUrl = "http://127.0.0.1:" + accessServerPort;
    private String resourceServerUrl = "http://127.0.0.1:" + resourceServerPort;
    private String spotifyServerUrl = "https://api\\.spotify\\.com";
    
    private String apiCategoriesResponse = "{\n" +
            "    \"categories\": {\n" +
            "        \"href\": \"https://api.spotify.com/v1/browse/categories?offset=0&limit=20\",\n" +
            "        \"items\": [\n" +
            "            {\n" +
            "                \"href\": \"https://api.spotify.com/v1/browse/categories/toplists\",\n" +
            "                \"icons\": [\n" +
            "                    {\n" +
            "                        \"height\": 275,\n" +
            "                        \"url\": \"https://datsnxq1rwndn.cloudfront.net/media/derived/toplists_11160599e6a04ac5d6f2757f5511778f_0_0_275_275.jpg\",\n" +
            "                        \"width\": 275\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"id\": \"toplists\",\n" +
            "                \"name\": \"Top Lists\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"href\": \"https://api.spotify.com/v1/browse/categories/mood\",\n" +
            "                \"icons\": [\n" +
            "                    {\n" +
            "                        \"height\": 274,\n" +
            "                        \"url\": \"https://datsnxq1rwndn.cloudfront.net/media/original/mood-274x274_976986a31ac8c49794cbdc7246fd5ad7_274x274.jpg\",\n" +
            "                        \"width\": 274\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"id\": \"mood\",\n" +
            "                \"name\": \"Super Mood\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"href\": \"https://api.spotify.com/v1/browse/categories/party\",\n" +
            "                \"icons\": [\n" +
            "                    {\n" +
            "                        \"height\": 274,\n" +
            "                        \"url\": \"https://datsnxq1rwndn.cloudfront.net/media/derived/party-274x274_73d1907a7371c3bb96a288390a96ee27_0_0_274_274.jpg\",\n" +
            "                        \"width\": 274\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"id\": \"party\",\n" +
            "                \"name\": \"Party Time\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"limit\": 20,\n" +
            "        \"next\": null,\n" +
            "        \"offset\": 0,\n" +
            "        \"previous\": null,\n" +
            "        \"total\": 3\n" +
            "    }\n" +
            "}".replaceAll(spotifyServerUrl, resourceServerUrl);
    
    
    private String apiPlaylistsPartyResponse = "{\n" +
            "    \"playlists\": {\n" +
            "        \"href\": \"https://api.spotify.com/v1/browse/categories/party/playlists?offset=0&limit=20\",\n" +
            "        \"items\": [\n" +
            "            {\n" +
            "                \"collaborative\": false,\n" +
            "                \"external_urls\": {\n" +
            "                    \"spotify\": \"http://open.spotify.com/user/spotifybrazilian/playlist/4k7EZPI3uKMz4aRRrLVfen\"\n" +
            "                },\n" +
            "                \"href\": \"https://api.spotify.com/v1/users/spotifybrazilian/playlists/4k7EZPI3uKMz4aRRrLVfen\",\n" +
            "                \"id\": \"4k7EZPI3uKMz4aRRrLVfen\",\n" +
            "                \"images\": [\n" +
            "                    {\n" +
            "                        \"height\": 300,\n" +
            "                        \"url\": \"https://i.scdn.co/image/bf6544c213532e9650088dfef76c8521093d970e\",\n" +
            "                        \"width\": 300\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"name\": \"Noite Eletronica\",\n" +
            "                \"owner\": {\n" +
            "                    \"external_urls\": {\n" +
            "                        \"spotify\": \"http://open.spotify.com/user/spotifybrazilian\"\n" +
            "                    },\n" +
            "                    \"href\": \"https://api.spotify.com/v1/users/spotifybrazilian\",\n" +
            "                    \"id\": \"spotifybrazilian\",\n" +
            "                    \"type\": \"user\",\n" +
            "                    \"uri\": \"spotify:user:spotifybrazilian\"\n" +
            "                },\n" +
            "                \"public\": null,\n" +
            "                \"snapshot_id\": \"PULvu1V2Ps8lzCxNXfNZTw4QbhBpaV0ZORc03Mw6oj6kQw9Ks2REwhL5Xcw/74wL\",\n" +
            "                \"tracks\": {\n" +
            "                    \"href\": \"https://api.spotify.com/v1/users/spotifybrazilian/playlists/4k7EZPI3uKMz4aRRrLVfen/tracks\",\n" +
            "                    \"total\": 100\n" +
            "                },\n" +
            "                \"type\": \"playlist\",\n" +
            "                \"uri\": \"spotify:user:spotifybrazilian:playlist:4k7EZPI3uKMz4aRRrLVfen\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"collaborative\": false,\n" +
            "                \"external_urls\": {\n" +
            "                    \"spotify\": \"http://open.spotify.com/user/spotifybrazilian/playlist/4HZh0C9y80GzHDbHZyX770\"\n" +
            "                },\n" +
            "                \"href\": \"https://api.spotify.com/v1/users/spotifybrazilian/playlists/4HZh0C9y80GzHDbHZyX770\",\n" +
            "                \"id\": \"4HZh0C9y80GzHDbHZyX770\",\n" +
            "                \"images\": [\n" +
            "                    {\n" +
            "                        \"height\": 300,\n" +
            "                        \"url\": \"https://i.scdn.co/image/be6c333146674440123073cb32c1c8b851e69023\",\n" +
            "                        \"width\": 300\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"name\": \"Festa Indie\",\n" +
            "                \"owner\": {\n" +
            "                    \"external_urls\": {\n" +
            "                        \"spotify\": \"http://open.spotify.com/user/spotifybrazilian\"\n" +
            "                    },\n" +
            "                    \"href\": \"https://api.spotify.com/v1/users/spotifybrazilian\",\n" +
            "                    \"id\": \"spotifybrazilian\",\n" +
            "                    \"type\": \"user\",\n" +
            "                    \"uri\": \"spotify:user:spotifybrazilian\"\n" +
            "                },\n" +
            "                \"public\": null,\n" +
            "                \"snapshot_id\": \"V66hh9k2HnLCdzHvtoXPv+tm0jp3ODM63SZ0oISfGnlHQxwG/scupDbKgIo99Zfz\",\n" +
            "                \"tracks\": {\n" +
            "                    \"href\": \"https://api.spotify.com/v1/users/spotifybrazilian/playlists/4HZh0C9y80GzHDbHZyX770/tracks\",\n" +
            "                    \"total\": 74\n" +
            "                },\n" +
            "                \"type\": \"playlist\",\n" +
            "                \"uri\": \"spotify:user:spotifybrazilian:playlist:4HZh0C9y80GzHDbHZyX770\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"limit\": 20,\n" +
            "        \"next\": null,\n" +
            "        \"offset\": 0,\n" +
            "        \"previous\": null,\n" +
            "        \"total\": 2\n" +
            "    }\n" +
            "}".replaceAll(spotifyServerUrl, resourceServerUrl);
    
    
    private String apiNewReleasesResponse = "{\n" +
            "    \"albums\": {\n" +
            "        \"href\": \"https://api.spotify.com/v1/browse/new-releases?offset=0&limit=20\",\n" +
            "        \"items\": [\n" +
            "            {\n" +
            "                \"album_type\": \"single\",\n" +
            "                \"artists\": [\n" +
            "                    {\n" +
            "                        \"external_urls\": {\n" +
            "                            \"spotify\": \"https://open.spotify.com/artist/2RdwBSPQiwcmiDo9kixcl8\"\n" +
            "                        },\n" +
            "                        \"href\": \"https://api.spotify.com/v1/artists/2RdwBSPQiwcmiDo9kixcl8\",\n" +
            "                        \"id\": \"2RdwBSPQiwcmiDo9kixcl8\",\n" +
            "                        \"name\": \"Pharrell Williams\",\n" +
            "                        \"type\": \"artist\",\n" +
            "                        \"uri\": \"spotify:artist:2RdwBSPQiwcmiDo9kixcl8\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"available_markets\": [\n" +
            "                    \"AD\"\n" +
            "                ],\n" +
            "                \"external_urls\": {\n" +
            "                    \"spotify\": \"https://open.spotify.com/album/5ZX4m5aVSmWQ5iHAPQpT71\"\n" +
            "                },\n" +
            "                \"href\": \"https://api.spotify.com/v1/albums/5ZX4m5aVSmWQ5iHAPQpT71\",\n" +
            "                \"id\": \"5ZX4m5aVSmWQ5iHAPQpT71\",\n" +
            "                \"images\": [\n" +
            "                    {\n" +
            "                        \"height\": 640,\n" +
            "                        \"url\": \"https://i.scdn.co/image/e6b635ebe3ef4ba22492f5698a7b5d417f78b88a\",\n" +
            "                        \"width\": 640\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"height\": 300,\n" +
            "                        \"url\": \"https://i.scdn.co/image/92ae5b0fe64870c09004dd2e745a4fb1bf7de39d\",\n" +
            "                        \"width\": 300\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"height\": 64,\n" +
            "                        \"url\": \"https://i.scdn.co/image/8a7ab6fc2c9f678308ba0f694ecd5718dc6bc930\",\n" +
            "                        \"width\": 64\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"name\": \"Runnin'\",\n" +
            "                \"type\": \"album\",\n" +
            "                \"uri\": \"spotify:album:5ZX4m5aVSmWQ5iHAPQpT71\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"album_type\": \"single\",\n" +
            "                \"artists\": [\n" +
            "                    {\n" +
            "                        \"external_urls\": {\n" +
            "                            \"spotify\": \"https://open.spotify.com/artist/3TVXtAsR1Inumwj472S9r4\"\n" +
            "                        },\n" +
            "                        \"href\": \"https://api.spotify.com/v1/artists/3TVXtAsR1Inumwj472S9r4\",\n" +
            "                        \"id\": \"3TVXtAsR1Inumwj472S9r4\",\n" +
            "                        \"name\": \"Drake2\",\n" +
            "                        \"type\": \"artist\",\n" +
            "                        \"uri\": \"spotify:artist:3TVXtAsR1Inumwj472S9r4\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"available_markets\": [\n" +
            "                    \"AD\"\n" +
            "                ],\n" +
            "                \"external_urls\": {\n" +
            "                    \"spotify\": \"https://open.spotify.com/album/0geTzdk2InlqIoB16fW9Nd\"\n" +
            "                },\n" +
            "                \"href\": \"https://api.spotify.com/v1/albums/0geTzdk2InlqIoB16fW9Nd\",\n" +
            "                \"id\": \"0geTzdk2InlqIoB16fW9Nd\",\n" +
            "                \"images\": [\n" +
            "                    {\n" +
            "                        \"height\": 640,\n" +
            "                        \"url\": \"https://i.scdn.co/image/d40e9c3d22bde2fbdb2ecc03cccd7a0e77f42e4c\",\n" +
            "                        \"width\": 640\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"height\": 300,\n" +
            "                        \"url\": \"https://i.scdn.co/image/dff06a3375f6d9b32ecb081eb9a60bbafecb5731\",\n" +
            "                        \"width\": 300\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"height\": 64,\n" +
            "                        \"url\": \"https://i.scdn.co/image/808a02bd7fc59b0652c9df9f68675edbffe07a79\",\n" +
            "                        \"width\": 64\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"name\": \"Sneakin'\",\n" +
            "                \"type\": \"album\",\n" +
            "                \"uri\": \"spotify:album:0geTzdk2InlqIoB16fW9Nd\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"limit\": 20,\n" +
            "        \"next\": null,\n" +
            "        \"offset\": 0,\n" +
            "        \"previous\": null,\n" +
            "        \"total\": 2\n" +
            "    }\n" +
            "}".replaceAll(spotifyServerUrl, resourceServerUrl);
    
    
    private String apiFeaturedPlaylistsResponse = "{\n" +
            "    \"message\": \"Monday morning music, coming right up!\",\n" +
            "    \"playlists\": {\n" +
            "        \"href\": \"https://api.spotify.com/v1/browse/featured-playlists?offset=0&limit=20\",\n" +
            "        \"items\": [\n" +
            "            {\n" +
            "                \"collaborative\": false,\n" +
            "                \"external_urls\": {\n" +
            "                    \"spotify\": \"http://open.spotify.com/user/spotify/playlist/6ftJBzU2LLQcaKefMi7ee7\"\n" +
            "                },\n" +
            "                \"href\": \"https://api.spotify.com/v1/users/spotify/playlists/6ftJBzU2LLQcaKefMi7ee7\",\n" +
            "                \"id\": \"6ftJBzU2LLQcaKefMi7ee7\",\n" +
            "                \"images\": [\n" +
            "                    {\n" +
            "                        \"height\": 300,\n" +
            "                        \"url\": \"https://i.scdn.co/image/7bd33c65ebd1e45975bbcbbf513bafe272f033c7\",\n" +
            "                        \"width\": 300\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"name\": \"Monday Morning Mood\",\n" +
            "                \"owner\": {\n" +
            "                    \"external_urls\": {\n" +
            "                        \"spotify\": \"http://open.spotify.com/user/spotify\"\n" +
            "                    },\n" +
            "                    \"href\": \"https://api.spotify.com/v1/users/spotify\",\n" +
            "                    \"id\": \"spotify\",\n" +
            "                    \"type\": \"user\",\n" +
            "                    \"uri\": \"spotify:user:spotify\"\n" +
            "                },\n" +
            "                \"public\": null,\n" +
            "                \"snapshot_id\": \"WwGvSIVUkUvGvqjgj/bQHlRycYmJ2TkoIxYfoalWlmIZT6TvsgvGMgtQ2dGbkrAW\",\n" +
            "                \"tracks\": {\n" +
            "                    \"href\": \"https://api.spotify.com/v1/users/spotify/playlists/6ftJBzU2LLQcaKefMi7ee7/tracks\",\n" +
            "                    \"total\": 245\n" +
            "                },\n" +
            "                \"type\": \"playlist\",\n" +
            "                \"uri\": \"spotify:user:spotify:playlist:6ftJBzU2LLQcaKefMi7ee7\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"collaborative\": false,\n" +
            "                \"external_urls\": {\n" +
            "                    \"spotify\": \"http://open.spotify.com/user/spotify__sverige/playlist/4uOEx4OUrkoGNZoIlWMUbO\"\n" +
            "                },\n" +
            "                \"href\": \"https://api.spotify.com/v1/users/spotify__sverige/playlists/4uOEx4OUrkoGNZoIlWMUbO\",\n" +
            "                \"id\": \"4uOEx4OUrkoGNZoIlWMUbO\",\n" +
            "                \"images\": [\n" +
            "                    {\n" +
            "                        \"height\": 300,\n" +
            "                        \"url\": \"https://i.scdn.co/image/24aa1d1b491dd529b9c03392f350740ed73438d8\",\n" +
            "                        \"width\": 300\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"name\": \"Upp och hoppa!\",\n" +
            "                \"owner\": {\n" +
            "                    \"external_urls\": {\n" +
            "                        \"spotify\": \"http://open.spotify.com/user/spotify__sverige\"\n" +
            "                    },\n" +
            "                    \"href\": \"https://api.spotify.com/v1/users/spotify__sverige\",\n" +
            "                    \"id\": \"spotify__sverige\",\n" +
            "                    \"type\": \"user\",\n" +
            "                    \"uri\": \"spotify:user:spotify__sverige\"\n" +
            "                },\n" +
            "                \"public\": null,\n" +
            "                \"snapshot_id\": \"0j9Rcbt2KtCXEXKtKy/tnSL5r4byjDBOIVY1dn4S6GV73EEUgNuK2hU+QyDuNnXz\",\n" +
            "                \"tracks\": {\n" +
            "                    \"href\": \"https://api.spotify.com/v1/users/spotify__sverige/playlists/4uOEx4OUrkoGNZoIlWMUbO/tracks\",\n" +
            "                    \"total\": 38\n" +
            "                },\n" +
            "                \"type\": \"playlist\",\n" +
            "                \"uri\": \"spotify:user:spotify__sverige:playlist:4uOEx4OUrkoGNZoIlWMUbO\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"limit\": 20,\n" +
            "        \"next\": null,\n" +
            "        \"offset\": 0,\n" +
            "        \"previous\": null,\n" +
            "        \"total\": 2\n" +
            "    }\n" +
            "}".replaceAll(spotifyServerUrl, resourceServerUrl);
    
    
    private String[] arguments = new String[]{
            "-access",
            accessServerUrl,
            "-resource",
            resourceServerUrl,
            "-page",
            "1"
    };
    
    private WebServerMock accessServer = new WebServerMock(accessServerPort)
            .setPage("/api/token", tokenResponse);
    
    private WebServerMock resourceServer = new WebServerMock(resourceServerPort)
            .setPage("/v1/browse/categories", apiCategoriesResponse)
            .setPage("/v1/browse/categories/party/playlists", apiPlaylistsPartyResponse)
            .setPage("/v1/browse/new-releases", apiNewReleasesResponse)
            .setPage("/v1/browse/featured-playlists", apiFeaturedPlaylistsResponse);
    
    private TestCase<Void> authTestCase(String whatNext) {
        return new TestCase<Void>()
                .addArguments(arguments)
                .runWith(accessServer)
                .runWith(resourceServer)
                .addInput(out -> {
                    redirectUriCatcher.start();
                    return "auth\n";
                })
                .addInput(out -> {
                    redirectUriCatcher.stop();
                    if (redirectUriCatcher.checkResult != CheckResult.correct()) {
                        return redirectUriCatcher.checkResult;
                    }
                    return whatNext;
                });
    }
    
    private CheckResult checkAlbum1(String reply) {
        String album1 =
                "Runnin'\n" +
                        "[Pharrell Williams]\n" +
                        "https://open.spotify.com/album/5ZX4m5aVSmWQ5iHAPQpT71"
                                .replaceAll(spotifyServerUrl, resourceServerUrl);
        String album2 =
                "Sneakin'\n" +
                        "[Drake2]\n" +
                        "https://open.spotify.com/album/0geTzdk2InlqIoB16fW9Nd"
                                .replaceAll(spotifyServerUrl, resourceServerUrl);
    
        if (!reply.contains(album1)) {
            return new CheckResult(false,
                    "Album from page 1 not appeared on \"new\" action");
        }
        if (reply.contains(album2)) {
            return new CheckResult(false,
                    "Album from page 2 appeared on page 1 on \"new\" action");
        }
        if (!reply.contains("---PAGE 1 OF 2---")) {
            return new CheckResult(false,
                    "Something wrong with pagination format. Not found ---PAGE 1 OF 2---");
        }
        return CheckResult.correct();
    }
    
    private CheckResult checkAlbum2(String reply) {
        String album1 =
                "Runnin'\n" +
                        "[Pharrell Williams]\n" +
                        "https://open.spotify.com/album/5ZX4m5aVSmWQ5iHAPQpT71"
                                .replaceAll(spotifyServerUrl, resourceServerUrl);
        String album2 =
                "Sneakin'\n" +
                        "[Drake2]\n" +
                        "https://open.spotify.com/album/0geTzdk2InlqIoB16fW9Nd"
                                .replaceAll(spotifyServerUrl, resourceServerUrl);
    
        if (!reply.contains(album2)) {
            return new CheckResult(false,
                    "Album from page 2 not appeared on \"new\" action");
        }
        if (reply.contains(album1)) {
            return new CheckResult(false,
                    "Album from page 1 appeared on page 2 on \"new\" action");
        }
        
        if (!reply.contains("---PAGE 2 OF 2---")) {
            return new CheckResult(false,
                    "Something wrong with pagination format. Not found ---PAGE 2 OF 2---");
        }
        
        return CheckResult.correct();
    }
    
    @Override
    public List<TestCase<Void>> generate() {
        return List.of(
                authTestCase("exit"),
                
                authTestCase("new")
                        .addInput(reply -> {
                            CheckResult res = checkAlbum1(reply);
                            if  (res != CheckResult.correct()) {
                                return res;
                            }
                            return "prev";
                        })
                        .addInput(reply -> {
                            if (!reply.contains("No more pages")) {
                                return CheckResult.wrong("Your output should be `No more pages` on -1 page.");
                            }
                            return "next";
                        })
                        .addInput(reply -> {
                            CheckResult res = checkAlbum2(reply);
                            if  (res != CheckResult.correct()) {
                                return res;
                            }
                            return "next";
                        })
                        .addInput(reply -> {
                            if (!reply.contains("No more pages")) {
                                return CheckResult.wrong("Your output should be `No more pages` after the last page.");
                            }
                            return "prev";
                        })
                        .addInput(reply -> {
                            CheckResult res = checkAlbum1(reply);
                            if  (res != CheckResult.correct()) {
                                return res;
                            }
                            return "exit";
                        }),
                
                
                authTestCase("categories\nexit")
                        .setCheckFunc((reply, v) -> {
                            
                            String category1 = "Top Lists";
                            String category2 = "Super Mood";
                            String category3 = "Party Time";
                            
                            if (countAppearances(reply, category1) != 1
                                    || countAppearances(reply, category2) != 0
                                    || countAppearances(reply, category3) != 0) {
                                
                                return new CheckResult(false,
                                        "Something wrong with showing categories and pages");
                            }
                            
                            return CheckResult.correct();
                        }),
        
                authTestCase("categories\nnext\nexit")
                        .setCheckFunc((reply, v) -> {
                            
                            String category1 = "Top Lists";
                            String category2 = "Super Mood";
                            String category3 = "Party Time";
                            
                            if (countAppearances(reply, category1) != 1
                                    || countAppearances(reply, category2) != 1
                                    || countAppearances(reply, category3) != 0) {
                                
                                return new CheckResult(false,
                                        "Something wrong with showing categories and pages");
                            }
                            
                            return CheckResult.correct();
                        }),
        
                authTestCase("categories\nnext\nnext\nexit")
                        .setCheckFunc((reply, v) -> {
                            
                            String category1 = "Top Lists";
                            String category2 = "Super Mood";
                            String category3 = "Party Time";
                            
                            if (countAppearances(reply, category1) != 1
                                    || countAppearances(reply, category2) != 1
                                    || countAppearances(reply, category3) != 1) {
                                
                                return new CheckResult(false,
                                        "Something wrong with showing categories and pages");
                            }
                            
                            return CheckResult.correct();
                        }),
        
                authTestCase("categories\nnext\nnext\nprev\nexit")
                        .setCheckFunc((reply, v) -> {
                            
                            String category1 = "Top Lists";
                            String category2 = "Super Mood";
                            String category3 = "Party Time";
                            
                            if (countAppearances(reply, category1) != 1
                                    || countAppearances(reply, category2) != 2
                                    || countAppearances(reply, category3) != 1) {
                                
                                return new CheckResult(false,
                                        "Something wrong with showing categories and pages");
                            }
                            
                            return CheckResult.correct();
                        }),
        
                authTestCase("categories\nnext\nnext\nprev\nprev\nexit")
                        .setCheckFunc((reply, v) -> {
                            
                            String category1 = "Top Lists";
                            String category2 = "Super Mood";
                            String category3 = "Party Time";
                            
                            if (countAppearances(reply, category1) != 2
                                    || countAppearances(reply, category2) != 2
                                    || countAppearances(reply, category3) != 1) {
                                
                                return new CheckResult(false,
                                        "Something wrong with showing categories and pages");
                            }
                            
                            return CheckResult.correct();
                        }),
        
                authTestCase("featured\nexit")
                        .setCheckFunc((reply, v) -> {
                            
                            String featured1 =
                                    "Monday Morning Mood\n" +
                                            "http://open.spotify.com/user/spotify/playlist/6ftJBzU2LLQcaKefMi7ee7"
                                                    .replaceAll(spotifyServerUrl, resourceServerUrl);
                            
                            String featured2 =
                                    "Upp och hoppa!\n" +
                                            "http://open.spotify.com/user/spotify__sverige/playlist/4uOEx4OUrkoGNZoIlWMUbO"
                                                    .replaceAll(spotifyServerUrl, resourceServerUrl);
                            
                            
                            if (countAppearances(reply, featured1) != 1
                                    || countAppearances(reply, featured2) != 0) {
                                
                                return new CheckResult(false,
                                        "Something wrong with showing featured playlists and pages");
                            }
                            
                            return CheckResult.correct();
                        }),
        
                authTestCase("featured\nnext\nexit")
                        .setCheckFunc((reply, v) -> {
                            
                            String featured1 =
                                    "Monday Morning Mood\n" +
                                            "http://open.spotify.com/user/spotify/playlist/6ftJBzU2LLQcaKefMi7ee7"
                                                    .replaceAll(spotifyServerUrl, resourceServerUrl);
                            
                            String featured2 =
                                    "Upp och hoppa!\n" +
                                            "http://open.spotify.com/user/spotify__sverige/playlist/4uOEx4OUrkoGNZoIlWMUbO"
                                                    .replaceAll(spotifyServerUrl, resourceServerUrl);
                            
                            
                            if (countAppearances(reply, featured1) != 1
                                    || countAppearances(reply, featured2) != 1) {
                                
                                return new CheckResult(false,
                                        "Something wrong with showing featured playlists and pages");
                            }
                            
                            return CheckResult.correct();
                        }),
        
                authTestCase("featured\nnext\nprev\nexit")
                        .setCheckFunc((reply, v) -> {
                            
                            String featured1 =
                                    "Monday Morning Mood\n" +
                                            "http://open.spotify.com/user/spotify/playlist/6ftJBzU2LLQcaKefMi7ee7"
                                                    .replaceAll(spotifyServerUrl, resourceServerUrl);
                            
                            String featured2 =
                                    "Upp och hoppa!\n" +
                                            "http://open.spotify.com/user/spotify__sverige/playlist/4uOEx4OUrkoGNZoIlWMUbO"
                                                    .replaceAll(spotifyServerUrl, resourceServerUrl);
                            
                            
                            if (countAppearances(reply, featured1) != 2
                                    || countAppearances(reply, featured2) != 1) {
                                
                                return new CheckResult(false,
                                        "Something wrong with showing featured playlists and pages");
                            }
                            
                            return CheckResult.correct();
                        }),
        
                authTestCase("playlists Party Time\nexit")
                        .setCheckFunc((reply, v) -> {
                            
                            String playlist1 =
                                    "Noite Eletronica\n" +
                                            "http://open.spotify.com/user/spotifybrazilian/playlist/4k7EZPI3uKMz4aRRrLVfen"
                                                    .replaceAll(spotifyServerUrl, resourceServerUrl);
                            
                            String playlist2 =
                                    "Festa Indie\n" +
                                            "http://open.spotify.com/user/spotifybrazilian/playlist/4HZh0C9y80GzHDbHZyX770"
                                                    .replaceAll(spotifyServerUrl, resourceServerUrl);
                            
                            if (countAppearances(reply, playlist1) != 1
                                    || countAppearances(reply, playlist2) != 0) {
                                
                                return new CheckResult(false,
                                        "Something wrong with showing playlists and pages");
                            }
                            
                            return CheckResult.correct();
                        }),
        
                authTestCase("playlists Party Time\nnext\nexit")
                        .setCheckFunc((reply, v) -> {
                            
                            String playlist1 =
                                    "Noite Eletronica\n" +
                                            "http://open.spotify.com/user/spotifybrazilian/playlist/4k7EZPI3uKMz4aRRrLVfen"
                                                    .replaceAll(spotifyServerUrl, resourceServerUrl);
                            
                            String playlist2 =
                                    "Festa Indie\n" +
                                            "http://open.spotify.com/user/spotifybrazilian/playlist/4HZh0C9y80GzHDbHZyX770"
                                                    .replaceAll(spotifyServerUrl, resourceServerUrl);
                            
                            if (countAppearances(reply, playlist1) != 1
                                    || countAppearances(reply, playlist2) != 1) {
                                
                                return new CheckResult(false,
                                        "Something wrong with showing playlists and pages");
                            }
                            
                            return CheckResult.correct();
                        }),
        
                authTestCase("playlists Party Time\nnext\nprev\nexit")
                        .setCheckFunc((reply, v) -> {
                            
                            String playlist1 =
                                    "Noite Eletronica\n" +
                                            "http://open.spotify.com/user/spotifybrazilian/playlist/4k7EZPI3uKMz4aRRrLVfen"
                                                    .replaceAll(spotifyServerUrl, resourceServerUrl);
                            
                            String playlist2 =
                                    "Festa Indie\n" +
                                            "http://open.spotify.com/user/spotifybrazilian/playlist/4HZh0C9y80GzHDbHZyX770"
                                                    .replaceAll(spotifyServerUrl, resourceServerUrl);
                            
                            if (countAppearances(reply, playlist1) != 2
                                    || countAppearances(reply, playlist2) != 1) {
                                
                                return new CheckResult(false,
                                        "Something wrong with showing playlists and pages");
                            }
                            
                            return CheckResult.correct();
                        })
        );
    }
    
    @Override
    public CheckResult check(String reply, Void clue) {
        return CheckResult.correct();
    }
}

