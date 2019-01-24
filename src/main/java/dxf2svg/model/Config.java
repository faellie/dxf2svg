package dxf2svg.model;

/**
 * read multiple dxf files from a dir and get polygon from each file (assume each file contains a polygon).
 * shift them and put into one SVG file
 */
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedList;

public class Config {

    @JsonProperty(value = "width")
    private int width;

    @JsonProperty(value = "hight")
    private int hight;


    @JsonProperty(value = "parts")
    private ArrayList<PartConfig> parts;


    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonString = objectMapper.writeValueAsString(this);
            return jsonString;
        } catch (Exception e) {
            return "Unable to convert object to JSON String: " + e.getMessage();
        }
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHight() {
        return hight;
    }

    public void setHight(int hight) {
        this.hight = hight;
    }

    public ArrayList<PartConfig> getParts() {
        return parts;
    }

    public void setParts(ArrayList<PartConfig> parts) {
        this.parts = parts;
    }
}
