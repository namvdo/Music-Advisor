package advisor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-access")) {
                SpotifyData.AUTH_SERVER_PATH = args[i + 1];
            }
            if (args[i].equals("-resource")) {
                SpotifyData.API_PATH = args[i + 1];
            }
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        boolean auth = false;
        boolean bool = true;
        while (bool) {
            String option = reader.readLine().trim();
            String command = option.contains(" ") ? option.substring(0, option.indexOf(" ")) : option;
            switch (command) {
                case "new":
                    if (auth) {
                        View.printNewReleases();
                    } else {
                        View.printText("Please, provide access for application.\n");
                    }
                    break;
                case "featured":
                    if (auth) {
                        View.printFeatures();
                    } else {
                        View.printText("Please, provide access for application.\n");
                    }
                    break;
                case "categories":
                    if (auth) {
                        View.printCategoryNames();
                    } else {
                        View.printText("Please, provide access for application.\n");
                    }
                    break;
                case "playlists":
                    String category = option.substring(option.indexOf(" ") + 1);
                    if (auth) {
                        View.printPlaylists(category);
                    } else {
                        View.printText("Please, provide access for application.\n");
                    }
                    break;
                case "auth":
                    Controller.printAuthCode();
                    Controller.getAccessToken();
                    View.printText(Controller.accessToken);
                    View.printText("---SUCCESS---");
                    auth = true;
                    break;
                case "exit":
                    View.printText("---GOODBYE!---");
                    bool = false;
                    break;
            }
        }
    }
}
