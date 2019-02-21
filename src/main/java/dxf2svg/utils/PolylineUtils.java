package dxf2svg.utils;

/**
 * read multiple dxf files from a dir and get polygon from each file (assume each file contains a polygon).
 * shift them and put into one SVG file
 */
import dxf2svg.model.Bound;
import dxf2svg.model.Config;
import dxf2svg.model.Orphan;
import dxf2svg.model.PartConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.kabeja.dxf.DXFConstants;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.dxf.DXFPolyline;
import org.kabeja.dxf.helpers.DXFUtils;
import org.kabeja.dxf.helpers.Point;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;
import org.kabeja.svg.generators.SVGPolylineGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PolylineUtils {
    private  static SVGPolylineGenerator svgPolylineGenerator= new SVGPolylineGenerator();

    public static void normalizePlines(List<DXFPolyline> aInPolyLines, Bound aInBound) {
        double min_x= 50000.0;
        double min_y = 50000.0;
        for(DXFPolyline lPline : aInPolyLines) {
            for (int i = 0; i < lPline.getVertexCount(); i++) {
                Point lPoint = lPline.getVertex(i).getPoint();
                min_x = Math.min(lPoint.getX(), min_x);
                min_y = Math.min(lPoint.getY(), min_y);
            }
        }

        for(DXFPolyline lPline : aInPolyLines) {
            for (int i = 0; i < lPline.getVertexCount(); i++) {
                Point lPoint = lPline.getVertex(i).getPoint();
                lPoint.setX(lPoint.getX() - min_x + 10);
                lPoint.setY(lPoint.getY() - min_y + aInBound.getMaxY() + 10);
            }
        }


    }

    public static Bound getBound(List<DXFPolyline> aInPolyLines) {
        double min_x= 0.0;
        double min_y = 0.0;
        double max_x= 0.0;
        double max_y = 0.0;
        for(DXFPolyline lPline : aInPolyLines) {
            for (int i = 0; i < lPline.getVertexCount(); i++) {
                Point lPoint = lPline.getVertex(i).getPoint();
                min_x = Math.min(lPoint.getX(), min_x);
                min_y = Math.min(lPoint.getY(), min_y);
                max_x = Math.max(lPoint.getX(), max_x);
                max_y = Math.max(lPoint.getY(), max_y);
            }
        }
        Bound ret = new Bound();
        ret.setMaxX((int)max_x + 1);
        ret.setMaxY((int) max_y + 1);
        ret.setMinX((int) min_x + 1);
        ret.setMinY((int) min_y + 1);

        return ret;
    }

    public static List reorder(List<DXFPolyline> plines) {
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


    public static void cleanDups(List<DXFPolyline> plines) {
        //clean for exmaple:
        /** The first line is not needed
         M0 0 L10 10
         M10 10 L0 0
         or: todo (not implemented yet
         M0 0 L10 10
         M0 0 L10 20
         */
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

    public static void cleanOrphan(List<DXFPolyline> plines) {
        //clean for exmaple:
        /** The first line is not needed
         M0 0 L10 55
         but no other polyline contains point (10 55)
         */

        List<DXFPolyline> lCopy = new ArrayList<DXFPolyline>();
        lCopy.addAll(plines);
        Iterator<DXFPolyline> lIt = plines.iterator();

        List<Orphan> lOrphanList = new ArrayList<>();
        while(lIt.hasNext()) {
            DXFPolyline lNextPl = lIt.next();

            if(iSelfClosed(lNextPl)) {
                continue;
            }
            boolean startAttached = false;
            boolean endAttached = false;
            Point startP = lNextPl.getVertex(0).getPoint();
            Point endP = lNextPl.getVertex(lNextPl.getVertexCount() - 1).getPoint();

            for (int i = 0; i < lCopy.size(); i++) {
                DXFPolyline lPl = lCopy.get(i);
                //dup should be removed already
                //so it must the same one in hte copy
                if(isDup(lPl, lNextPl)) {
                    continue;
                } else {
                    //note : Can not have both point equal, it consided to be dup
                    if(DXFUtils.equals(startP, lPl.getVertex(0).getPoint(), DXFConstants.POINT_CONNECTION_RADIUS) ||
                            DXFUtils.equals(startP, lPl.getVertex(lPl.getVertexCount() - 1).getPoint(), DXFConstants.POINT_CONNECTION_RADIUS)) {
                        startAttached = true;
                    } else if(DXFUtils.equals(endP, lPl.getVertex(0).getPoint(), DXFConstants.POINT_CONNECTION_RADIUS) ||
                            DXFUtils.equals(endP, lPl.getVertex(lPl.getVertexCount() - 1).getPoint(), DXFConstants.POINT_CONNECTION_RADIUS) ) {
                        endAttached = true;
                    }
                    if(startAttached && endAttached) {
                        break;
                    }
                }
            }
            if(!(startAttached && endAttached)) {
                System.out.println("Found Orphan " + svgPolylineGenerator.getSVGPath(lNextPl));
                lOrphanList.add(new Orphan(lNextPl, startAttached, endAttached));
                lIt.remove();
            }
        }
        //some of the orphan might be fake, they might just missing a bit
        //todo this is for later
        List<Orphan> lOrphanCopy = new ArrayList<>(lOrphanList);
        Iterator<Orphan> orphanIt = lOrphanList.iterator();
        while(orphanIt.hasNext()) {
            Orphan lCurrOrphon = orphanIt.next();
            for(Orphan lNextOrphan : lOrphanCopy) {

                if(!isDup(lNextOrphan.getPl(), lCurrOrphon.getPl())) {

                }
            }
        }
        return;
    }

    //use is closed instead
    private static boolean iSelfClosed(DXFPolyline aInPl) {

        Point startP = aInPl.getVertex(0).getPoint();
        Point endP = aInPl.getVertex(aInPl.getVertexCount() - 1).getPoint();
        return aInPl.isClosed() || (DXFUtils.equals(startP, endP, DXFConstants.POINT_CONNECTION_RADIUS));
    }

    public static boolean isDup(DXFPolyline pl, DXFPolyline lNext) {
        Point plstart = pl.getVertex(0).getPoint();
        Point plend = pl.getVertex(pl.getVertexCount() - 1).getPoint();
        Point lNextstart = lNext.getVertex(0).getPoint();
        Point lNextEnd = lNext.getVertex(lNext.getVertexCount() - 1).getPoint();

        return (DXFUtils.equals(plstart, lNextstart, DXFConstants.POINT_CONNECTION_RADIUS) && DXFUtils.equals(plend, lNextEnd, DXFConstants.POINT_CONNECTION_RADIUS)) ||
                (DXFUtils.equals(plstart, lNextEnd, DXFConstants.POINT_CONNECTION_RADIUS) && DXFUtils.equals(plend, lNextstart, DXFConstants.POINT_CONNECTION_RADIUS)) ;
    }


    public static ArrayList<String> convertFileToSvgStr(String aInFileName, Bound aInBound) {
        ArrayList<String> lines = new ArrayList<String>();
        PartConfig lPartConfig = new PartConfig();
        lPartConfig.setFilename(aInFileName);
        lPartConfig.setColorcode("red");
        lPartConfig.setDups(1);
        lPartConfig.setLayer(CommonUtils.fileNameToLayer(aInFileName));
        String lDxfFileName = lPartConfig.getFilename();
        Parser parser = ParserBuilder.createDefaultParser();
        FileInputStream lInputStream = null;
        try {
            lInputStream = new FileInputStream(lDxfFileName);
            parser.parse(lInputStream, DXFParser.DEFAULT_ENCODING);
        } catch (FileNotFoundException e) {
            System.out.println("Failed to open file. " + e);
            return lines;
        } catch (ParseException e) {
            System.out.println("Failed to parse file. " + e);
            return lines;
        }

        //get the document and the layer
        DXFDocument doc = parser.getDocument();
        DXFLayer layer = doc.getDXFLayer(lPartConfig.getLayer());

        //get all polylines from the layer
        List plines = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_POLYLINE);
        PolylineUtils.cleanDups(plines);
        PolylineUtils.cleanOrphan(plines);
        PolylineUtils.normalizePlines(plines, aInBound);
        List lOrederedPl = PolylineUtils.reorder(plines);
        addPlToArray(lOrederedPl, lines, lPartConfig);
        aInBound.setMaxX(getBound(lOrederedPl).getMaxX());
        aInBound.setMaxY(getBound(lOrederedPl).getMaxY());
        return lines;
    }



    private static void addPlToArray(List<DXFPolyline> aInPlList, ArrayList<String> aInLines, PartConfig aInPartConfig) {
        boolean isFirst = true;
        aInLines.add("<path id=\"" + aInPartConfig.getLayer() + "\" d = \"");
        for(DXFPolyline pl : aInPlList) {
            String lSVGPath = svgPolylineGenerator.getSVGPath(pl);
            if(isFirst) {
                isFirst = false;
            } else {
                lSVGPath = lSVGPath.replaceFirst("M", "L");
            }
            aInLines.add(lSVGPath + "\n");
        }
        aInLines.add("z \" stroke=\"" + aInPartConfig.getColorcode() +  " \" fill = \"none\" />");
        return;
    }
}
