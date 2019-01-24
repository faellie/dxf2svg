package dxf2svg;

import dxf2svg.utils.PolylineUtils;
import org.kabeja.dxf.*;
import org.kabeja.dxf.helpers.DXFUtils;
import org.kabeja.dxf.helpers.Point;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;
import org.kabeja.processing.helper.PolylineQueue;
import org.kabeja.svg.generators.SVGPolylineGenerator;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DXFExtractor {

    private  static SVGPolylineGenerator svgPolylineGenerator= new SVGPolylineGenerator();
    private static PolylineQueue plQ = null;
    public static void  read(InputStream in, String layerid) {

        Parser parser = ParserBuilder.createDefaultParser();
        try {

            //parse
            parser.parse(in, DXFParser.DEFAULT_ENCODING);

            //get the documnet and the layer
            DXFDocument doc = parser.getDocument();
            DXFLayer layer = doc.getDXFLayer(layerid);

            //get all polylines from the layer
            List plines = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_POLYLINE);

            // sort the polylines
            //plines.sort(new MyComparator());
            //plines = reorder(plines);
            //normalize polylines
            normalizePlines(plines);

            //work with the polylines
            doStuff(plines);
            cleanDups(plines);
            PolylineUtils.cleanOrphan(plines);
            System.out.println("After cleandup ==============================");
            doStuff(plines);
            List orderedLines = reorder(plines);
            System.out.println("After reorder ==============================");
            doStuff(orderedLines);
            System.out.println("into one shape ==============================");
            doFinalStuff(orderedLines);

            toSVGFile(orderedLines);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static void toSVGFile(List<DXFPolyline> orderedLines) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("/opt/tmp/out.svg", "UTF-8");
        } catch (FileNotFoundException e) {
            System.out.println("Failed to open file to write : " + e);
        } catch (UnsupportedEncodingException e) {
            System.out.println("Failed to open file to write : " + e);
        }
        writer.println("<svg viewBox=\"0 0 550 550\" xmlns=\"http://www.w3.org/2000/svg\">");
        boolean isFirst = true;
        String lines = "<path d = \"";
        for(DXFPolyline pl : orderedLines) {
            String lSVGPath = svgPolylineGenerator.getSVGPath(pl);
            if(isFirst) {
                isFirst = false;
            } else {
                lSVGPath = lSVGPath.replaceFirst("M", "L");
            }
            lines = lines + lSVGPath + "\n";
        }
        lines = lines + "z \" stroke=\"red\" fill = \"none\" />";
        writer.println(lines);
        writer.println("</svg>");
        writer.close();

    }


    private static void doFinalStuff(List<DXFPolyline> orderedLines) {
        //"<path d=\"" + lSVGPath + "\" stroke=\"red\" />";
        String out = "<path d = \"";
        boolean isFirst = true;
        for(DXFPolyline pl : orderedLines) {
            String lSVGPath = svgPolylineGenerator.getSVGPath(pl);
            if(isFirst) {
                isFirst = false;
            } else {
                lSVGPath = lSVGPath.replaceFirst("M", "L");
            }
            out = out + lSVGPath + "\n";
        }
        out = out + "z \" stroke=\"red\" fill = \"none\" />";
        System.out.println(out);
        /*DXFPolyline lFinal = new DXFPolyline();
        for(DXFPolyline pl : orderedLines) {
            for(int i = 0; i < pl.getVertexCount() - 1; i ++) {
                lFinal.addVertex(pl.getVertex(i));
            }
        }
        String lSVGPath = lFinal.getSVGPath();
        System.out.println("<path d=\"" + lSVGPath + "\" stroke=\"red\" />");*/
    }

    //move to PolylineUtils
    private static void cleanDups(List<DXFPolyline> plines) {
        //clean for exmaple:
        /** The first line is not needed
         M0 0 L10 10
         M0 0 L10 20
         Or
         M0 0 L10 10
         M10 10 L0 0

         */
        //todo
        List<DXFPolyline> lCopy = new ArrayList<DXFPolyline>();
        Iterator<DXFPolyline> lIt = plines.iterator();

        while(lIt.hasNext()) {
            DXFPolyline lNext = (DXFPolyline) lIt.next();
            boolean lExist  = false;
            for(DXFPolyline pl : lCopy) {
                if(isDup(pl, lNext)) {
                    lExist = true;
                }
            }
            if(!lExist) {
                lCopy.add(lNext);
            }
        }
        plines.clear();
        plines.addAll(lCopy);
        return;
    }

    //move to PolylineUtils
    private static boolean isDup(DXFPolyline pl, DXFPolyline lNext) {
        Point plstart = pl.getVertex(0).getPoint();
        Point plend = pl.getVertex(pl.getVertexCount() - 1).getPoint();
        Point lNextstart = lNext.getVertex(0).getPoint();
        Point lNextEnd = lNext.getVertex(lNext.getVertexCount() - 1).getPoint();

        return (DXFUtils.equals(plstart, lNextstart, DXFConstants.POINT_CONNECTION_RADIUS) && DXFUtils.equals(plend, lNextEnd, DXFConstants.POINT_CONNECTION_RADIUS)) ||
                (DXFUtils.equals(plstart, lNextEnd, DXFConstants.POINT_CONNECTION_RADIUS) && DXFUtils.equals(plend, lNextstart, DXFConstants.POINT_CONNECTION_RADIUS)) ;
    }
    //move to PolylineUtils
    private static List reorder(List<DXFPolyline> plines) {
        List<DXFPolyline> lOrderedList = new ArrayList<DXFPolyline>();
        //take the first Vertax out
        DXFPolyline lCurrPl = plines.get(0);
        lOrderedList.add(lCurrPl);
        plines.remove(0);
        Point lCurrStartP = lCurrPl.getVertex(0).getPoint();
        Point lCurrEndP = lCurrPl.getVertex(lCurrPl.getVertexCount() - 1).getPoint();

        while(plines.size() > 0 ) {
            DXFPolyline lNextPl = null;
            Iterator<DXFPolyline> lIt = plines.iterator();
            while(lIt.hasNext()) {
                DXFPolyline lPl = lIt.next();
                Point startP = lPl.getVertex(0).getPoint();
                Point endP = lPl.getVertex(lPl.getVertexCount() - 1).getPoint();
                boolean isMatch = false;
                if (DXFUtils.equals(lCurrEndP, startP, DXFConstants.POINT_CONNECTION_RADIUS)) {
                    //this is the next PL, add
                    lIt.remove();
                    isMatch = true;
                } else if (DXFUtils.equals(lCurrEndP, endP, DXFConstants.POINT_CONNECTION_RADIUS)) {
                    //need to flip the endpoint first and add
                    lIt.remove();
                    DXFUtils.reverseDXFPolyline(lPl);
                    isMatch = true;
                }
                if (isMatch) {
                    if (null == lNextPl) {
                        lNextPl = lPl;
                    } else {
                        //this is a dup seems
                        //so we have two possibility lNextPl and lPl
                        System.out.println("Found Dup " +
                                svgPolylineGenerator.getSVGPath(lNextPl) + "\n\t\t\t\t" + svgPolylineGenerator.getSVGPath(lPl));
                        //todo use the big one
                        if (lNextPl.getVertexCount() == lPl.getVertexCount()) {
                            //compare the distance
                            double lPlD = DXFUtils.distance(lPl.getVertex(0).getPoint(), lPl.getVertex(lPl.getVertexCount() - 1).getPoint());
                            double lNextPlD = DXFUtils.distance(lNextPl.getVertex(0).getPoint(), lNextPl.getVertex(lPl.getVertexCount() - 1).getPoint());
                            if (lPlD > lNextPlD) {
                                //use the longer one
                                lNextPl = lPl;
                            }
                        }
                    }

                }
            }
            if(lNextPl == null) {
                // no more close
                System.out.println("No more matching for " + svgPolylineGenerator.getSVGPath(lCurrPl));
                break;
            } else {
                lOrderedList.add(lNextPl);
                lCurrPl = lNextPl;
                lCurrEndP = lCurrPl.getVertex(lCurrPl.getVertexCount() - 1).getPoint();
            }
        }
        if(plines.size() > 0) {
            for(DXFPolyline pl : plines) {
                System.out.println("Left over : " + svgPolylineGenerator.getSVGPath(pl));
            }
        }


        return lOrderedList;

    }

    //move to PolylineUtils
    private static void normalizePlines(List<DXFPolyline> aInPolyLines) {
        double min_x= 0.0;
        double min_y = 0.0;
        for(DXFPolyline lPline : aInPolyLines) {
            for (int i = 0; i < lPline.getVertexCount(); i++) {
                Point lPoint = lPline.getVertex(i).getPoint();
                min_x = Math.min(lPoint.getX(), min_x);
                min_y = Math.min(lPoint.getY(), min_y);
            }
        }

        if(min_x < 0 || min_y < 0) {
            for(DXFPolyline lPline : aInPolyLines) {
                for (int i = 0; i < lPline.getVertexCount(); i++) {
                    Point lPoint = lPline.getVertex(i).getPoint();
                    lPoint.setX(lPoint.getX() - min_x);
                    lPoint.setY(lPoint.getY() - min_y);
                }
            }
        }

    }

    private static void doStuff(List<DXFPolyline> aInPolyLines) {
        for(DXFPolyline lPline : aInPolyLines) {
            String lSVGPath = svgPolylineGenerator.getSVGPath(lPline);
            lSVGPath = "<path d=\"" + lSVGPath + "\" stroke=\"red\" />";
            System.out.println(lSVGPath);
        }
    }

    public static void doSomething(DXFPolyline pline) {

        //iterate over all vertex of the polyline
        for (int i = 0; i < pline.getVertexCount(); i++) {

            DXFVertex vertex = pline.getVertex(i);

            //do something like collect the data and
            //build a mesh for a FEM system
        }
    }

    public static void main(String[] args) {
        /*String lFileName = "/opt/LI/nesting/image/shapes/2/dxf/107834-4.dxf";
        String lLayId = "107834-4ROU_GBR";*/

        //String lFileName = "/opt/LI/nesting/image/shapes/2/dxf/128873-1.dxf";
        //String lLayId = "128873-1ROU_GBR";

        //String lFileName = "/opt/LI/nesting/image/shapes/2/dxf/108835-1.dxf";
        //String lLayId = "108835-1ROU_GBR";

        //String lFileName = "/opt/LI/nesting/image/shapes/2/dxf/108835-1.dxf";
        //String lLayId = "108835-1ROU_GBR";

        //String lFileName = "/opt/LI/nesting/image/shapes/2/dxf/ro1.dxf";
        //String lLayId = "22RO1_GBR";

        //String lFileName = "/opt/LI/nesting/image/shapes/1/dxf/107824-1.dxf";  //this is good little little curved shap
        //String lLayId = "107824-1ROU_GBR";


        //String lFileName = "/opt/LI/nesting/image/shapes/1/dxf/087774-1.dxf";     // this is good, one square
        //String lLayId = "087774-1ROU_GBR";

        //String lFileName = "/opt/LI/nesting/image/shapes/1/dxf/087782-1.dxf";     // this is good, one square but need a bit fix (two point was off by 0.01)
        //String lLayId = "087782-1ROU_GBR";

        //String lFileName = "/opt/LI/nesting/image/shapes/1/dxf/038683-1.dxf";     // this is good, one square
        //String lLayId = "038683-1ROU_GBR";

        //String lFileName = "/opt/LI/nesting/image/shapes/1/dxf/108831-1.dxf";
        //String lLayId = "108831-1ROU_GBR";

        String lFileName = "/opt/LI/nesting/image/shapes/1/dxf/108831-2.dxf";
        String lLayId = "108831-2ROU_GBR";

        InputStream lInputStream = null;
        try {
            lInputStream = new FileInputStream(lFileName);
        } catch (FileNotFoundException e) {
            System.out.println("Failed to open file. " + e);
            return;
        }
        read(lInputStream,  lLayId);
    }


}
