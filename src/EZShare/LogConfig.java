package EZShare;

/**
 * Created by Yahang Wu on 2017/4/11.
 * COMP90015 Distributed System Project1 EZServer
 * It is the configuration class, mainly provide the method to load log config.
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;

class LogConfig {
    static void logConfig(){
        //Load log config file
        try {
            FileInputStream config = new FileInputStream("logger.properties");
            LogManager.getLogManager().readConfiguration(config);

        } catch (IOException e) {
            System.out.println("WARNING: Can not load log configuration file");
            System.out.println("WARNING: Logging not configured");
            e.printStackTrace();
        }
    }
}
