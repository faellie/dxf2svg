package dxf2svg.model;

/**
 * a DXFPolyline that have one point not attached
 */
import javafx.scene.shape.Polyline;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.kabeja.dxf.DXFPolyline;

public class Orphan {

    private DXFPolyline pl;

    private boolean startAttached;

    private boolean endAttached;

    public Orphan(DXFPolyline pl, boolean startAttached, boolean endAttached) {
        this.pl = pl;
        this.startAttached = startAttached;
        this.endAttached = endAttached;
    }

    public DXFPolyline getPl() {
        return pl;
    }

    public void setPl(DXFPolyline pl) {
        this.pl = pl;
    }

    public boolean isStartAttached() {
        return startAttached;
    }

    public void setStartAttached(boolean startAttached) {
        this.startAttached = startAttached;
    }

    public boolean isEndAttached() {
        return endAttached;
    }

    public void setEndAttached(boolean endAttached) {
        this.endAttached = endAttached;
    }
}
