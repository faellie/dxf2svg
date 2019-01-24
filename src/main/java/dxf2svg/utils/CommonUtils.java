package dxf2svg.utils;

/**
 * read multiple dxf files from a dir and get polygon from each file (assume each file contains a polygon).
 * shift them and put into one SVG file
 */
import dxf2svg.model.Config;
import dxf2svg.model.PartConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommonUtils {

    public static Config getConfig(String aInConfigPath) {
        ObjectMapper objectMapper = new ObjectMapper();
        Config myConfig = new Config();
        File lFile = new File(aInConfigPath);
        try {
            myConfig = objectMapper.readValue(lFile, Config.class);
        } catch (IOException e) {
            System.out.println("Failed to read from file " + aInConfigPath);
        }
        return myConfig;
    }

}
