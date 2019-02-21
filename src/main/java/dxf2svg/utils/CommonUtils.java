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
        File lConfigFile = new File(aInConfigPath);
        Config myConfig = new Config();

        if(lConfigFile.isDirectory()) {
            myConfig.setWidth(1500);
            myConfig.setHight(1500);
            myConfig.setParts(new ArrayList<PartConfig>());

            if(lConfigFile == null) {
                System.out.println("failed to open the dir " + aInConfigPath);
                return null;
            }
            File[] files = lConfigFile.listFiles();
            if(null == files || files.length < 1 ) {
                System.out.println("No files found in the dir " + aInConfigPath);
            }
            for (File lFile : files) {
                String lDxfFile = lFile.getAbsolutePath();
                if (!lDxfFile.endsWith(".dxf")) {
                    continue;
                } else {
                    PartConfig lPartConfig = new PartConfig();
                    lPartConfig.setFilename(lDxfFile);
                    lPartConfig.setColorcode("red");
                    lPartConfig.setDups(1);
                    lPartConfig.setLayer(fileNameToLayer(lDxfFile));
                    myConfig.getParts().add(lPartConfig);
                }
            }
        } else if(aInConfigPath.endsWith(".json")) {

            ObjectMapper objectMapper = new ObjectMapper();


            try {
                myConfig = objectMapper.readValue(lConfigFile, Config.class);
            } catch (IOException e) {
                System.out.println("Failed to read from file " + aInConfigPath);
            }
        } else {
            System.out.println("Config file must be json format");
        }
        return myConfig;
    }


    public  static String fileNameToLayer(String aInFileFullPath) {
        //038689-1.dxf  ==> 038689-1ROU1_GBR
        File lFile = new File(aInFileFullPath);
        String lFileName = lFile.getName();
        if(lFileName.equalsIgnoreCase("ro1.dxf")) {
            return "RO1_GBR";
        }
        return lFileName.replace(".dxf", "ROU1_GBR");
    }
}
