package advisor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        int limit = 5;
        String requestedFeature = "";
        String categoryName = "";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-access")) {
                SpotifyData.AUTH_SERVER_PATH = args[i + 1];
            }
            if (args[i].equals("-resource")) {
                SpotifyData.API_PATH = args[i + 1];
            }
            if(args[i].equals("-page")){
                limit = Integer.parseInt(args[i + 1]);
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
                        View.printNewReleases(limit, true, true);
                        requestedFeature = "new";
                    } else {
                        View.printText("Please, provide access for application.\n");
                    }
                    break;
                case "featured":
                    if (auth) {
                        View.printFeatures(limit, true, true);
                        requestedFeature = "featured";
                    } else {
                        View.printText("Please, provide access for application.\n");
                    }
                    break;
                case "categories":
                    if (auth) {
                        View.printCategoryNames(limit, true, true);
                        requestedFeature = "categories";
                    } else {
                        View.printText("Please, provide access for application.\n");
                    }
                    break;
                case "playlists":
                     categoryName = option.substring(option.indexOf(" ") + 1);
                    if (auth) {
                        View.printPlaylists(categoryName, limit, true, true);
                        requestedFeature = "playlists";
                    } else {
                        View.printText("Please, provide access for application.\n");
                    }
                    break;
                case "auth":
                    Controller.getAuthCode();
                    Controller.getAccessToken();
                    View.printText("---SUCCESS---");
                    auth = true;
                    break;
                case "next":
                    if(auth && !requestedFeature.equals("")){
                        if(requestedFeature.equals("new")){
                            View.printNewReleases(limit, true, false);
                        }
                        if(requestedFeature.equals("categories")){
                            View.printCategoryNames(limit, true, false);
                        }
                        if(requestedFeature.equals("featured")){
                            View.printFeatures(limit, true, false);
                        }
                        if(requestedFeature.equals("playlists")){
                            View.printPlaylists(categoryName, limit, true, false);
                        }
                    }else{
                        System.out.println("Please provide information for the application and/or provide one feature.");
                    }
                    break;
                case "prev":
                    if(auth && !requestedFeature.equals("")){
                        if(requestedFeature.equals("new")){
                            View.printNewReleases(limit, false, false);
                        }
                        if(requestedFeature.equals("categories")){
                            View.printCategoryNames(limit, false, false);
                        }
                        if(requestedFeature.equals("featured")){
                            View.printFeatures(limit, false, false);
                        }
                        if(requestedFeature.equals("playlists")){
                            View.printPlaylists(categoryName, limit, false, false);
                        }
                    }else{
                        System.out.println("Please provide information for the application and/or provide one feature.");
                    }
                    break;
                case "exit":
                    View.printText("---GOODBYE!---");
                    bool = false;
                    break;
            }
        }
    }
}
