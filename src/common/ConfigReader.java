package common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ConfigReader {

    private Map<String, Object> config;
    public ConfigReader(String filename) {
        config = new HashMap<>();
        loadConfig(filename);
    }

    private void loadConfig(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Server's port:")) {
                    String port = line.split(":")[1].trim();
                    config.put("Server's port", Integer.parseInt(port));
                } else if (line.startsWith("Server's name:")) {
                    String serverName = line.split(":")[1].trim();
                    config.put("Server's name", serverName);
                } else if (line.startsWith("List of banned phrases:")) {
                    String[] phrases = line.split(":")[1].split(",");
                    config.put("Banned words", new HashSet<>(Arrays.asList(phrases)));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getPort(){
        return (Integer) config.get("Server's port");
    }

    public String getServerName(){
        return (String)config.get("Server's name");
    }

    public Set<String> getBannedPhrases(){
        return (Set<String>)config.get("Banned words");
    }

}
