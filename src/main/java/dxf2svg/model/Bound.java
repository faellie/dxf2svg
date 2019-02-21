package dxf2svg.model;

/**
 * read multiple dxf files from a dir and get polygon from each file (assume each file contains a polygon).
 * shift them and put into one SVG file
 */
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.ArrayList;

public class Bound {

    @JsonProperty(value = "minX")
    private int minX;

    @JsonProperty(value = "minY")
    private int minY;

    @JsonProperty(value = "maxX")
    private int maxX;

    @JsonProperty(value = "maxY")
    private int maxY;

    public Bound() {
        minX = 0;
        minY = 0;
        maxX = 0;
        maxY = 0;
    }

    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonString = objectMapper.writeValueAsString(this);
            return jsonString;
        } catch (Exception e) {
            return "Unable to convert object to JSON String: " + e.getMessage();
        }
    }


    public int getMinX() {
        return minX;
    }

    public void setMinX(int minX) {
        this.minX = minX;
    }

    public int getMinY() {
        return minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }
}
