package dxf2svg;

/**
 * read multiple dxf files from a dir and get polygon from each file (assume each file contains a polygon).
 * shift them and put into one SVG file
 */
import dxf2svg.model.Bound;
import dxf2svg.model.Config;
import dxf2svg.model.PartConfig;
import dxf2svg.utils.CommonUtils;
import dxf2svg.utils.PolylineUtils;
import org.kabeja.dxf.DXFConstants;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.dxf.DXFPolyline;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;
import org.kabeja.svg.generators.SVGPolylineGenerator;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DXFsToSvg {

    private  static SVGPolylineGenerator svgPolylineGenerator= new SVGPolylineGenerator();
    private static PrintWriter writer = null;
    private static Config lConfig = null;
    private static Bound bound = new Bound();

    public static void main(String[] args) {
        //String inputFile = "/opt/LI/nesting/dxf2svg/config/input1.json";
        String inputFile = "/opt/tmp/test";
        if(args.length != 0) {
            inputFile = args[0];
        }
        lConfig = CommonUtils.getConfig(inputFile);
        System.out.println("Config : " + lConfig);

        //open file for output
        try {
            writer = new PrintWriter("/opt/tmp/out1.svg", "UTF-8");
        } catch (FileNotFoundException e) {
            System.out.println("Failed to open file to write : " + e);
            return;
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to open file to write : " + e);
            return;
        }
        processHeader();
        //add hte mother board for fitting
        //writer.println("<path id=\"mainboard\" d = \"\n" +
        //        "M 0 0 L 600 10 L 600 800 L 10 800 z \" stroke=\"green \" fill = \"none\" />\n");
        //bound.setMaxX(810);
        //bound.setMaxY(810);
        for(PartConfig lPartConfig : lConfig.getParts()) {
            processParts(lPartConfig);
        }
        processEnd();
        return;
    }
    private static void processParts(PartConfig aInPartConfig) {
        for(int i = 0; i < aInPartConfig.getDups(); i ++) {
            processPart(aInPartConfig);
        }
    }

    private static void processPart(PartConfig aInPartConfig) {
        String lDxfFileName = aInPartConfig.getFilename();
        Parser parser = ParserBuilder.createDefaultParser();
        FileInputStream lInputStream = null;
        try {
            lInputStream = new FileInputStream(lDxfFileName);
            parser.parse(lInputStream, DXFParser.DEFAULT_ENCODING);
        } catch (FileNotFoundException e) {
            System.out.println("Failed to open file. " + e);
            return;
        } catch (ParseException e) {
            System.out.println("Failed to parse file. " + e);
            return;
        }

        //get the document and the layer
        DXFDocument doc = parser.getDocument();
        DXFLayer layer = doc.getDXFLayer(aInPartConfig.getLayer());

        //get all polylines from the layer
        List plines = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_POLYLINE);
        PolylineUtils.cleanDups(plines);
        PolylineUtils.cleanOrphan(plines);
        PolylineUtils.normalizePlines(plines, bound);
        List lOrederedPl = PolylineUtils.reorder(plines);
        addPartToFile(lOrederedPl, aInPartConfig);
        bound  = PolylineUtils.getBound(lOrederedPl);
    }

    private static void addPartToFile(List<DXFPolyline> aInPlList, PartConfig aInPartConfig) {
        boolean isFirst = true;
        String lines = "<path id=\"" + aInPartConfig.getLayer() + "\" d = \"";
        for(DXFPolyline pl : aInPlList) {
            String lSVGPath = svgPolylineGenerator.getSVGPath(pl);
            if(isFirst) {
                isFirst = false;
            } else {
                lSVGPath = lSVGPath.replaceFirst("M", "L");
            }
            lines = lines + lSVGPath + "\n";
        }
        lines = lines + "z \" stroke=\"" + aInPartConfig.getColorcode() +  " \" fill = \"none\" />";
        writer.println(lines);
    }

    private static void processHeader() {
        // <svg viewBox="0 0 150 150" xmlns="http://www.w3.org/2000/svg">
        writer.println("<svg viewBox=\"0 0 " + lConfig.getWidth() + " " + lConfig.getWidth()+  "  \" xmlns=\"http://www.w3.org/2000/svg\">");

    }

    private static void processEnd() {
        //add a big one for all
        //String all = "<path d=\"M10 " + (bound.getMaxY() + 10) + "  h 200 v 200 h -200 z\"/>";
        //writer.println(all);
        writer.println("</svg>");
        writer.close();


    }


}
