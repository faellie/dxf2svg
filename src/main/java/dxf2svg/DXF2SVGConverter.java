package dxf2svg;

import dxf2svg.model.Bound;
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

public class DXF2SVGConverter {


    public static void main(String[] args) {

        if(args.length < 1) {
            System.out.println("Please specify the dir contains the DXF files");
            return;
        }

        File folder = new File(args[0]);
        if(folder == null) {
            System.out.println("failed to open the dir " + args[0]);
            return;
        }
        File[] files = folder.listFiles();
        if(null == files || files.length < 1 ) {
            System.out.println("No files found in the dir " + args[0]);
        }
        for (File lFile : files)
        {
            String lDxfFile = lFile.getAbsolutePath();
            if(!lDxfFile.endsWith(".dxf")) {
                continue;
            }
            //bad files
            if(lDxfFile.endsWith("058723-1.dxf")) {
                continue;
            }
            System.out.println("Processing " + lDxfFile);
            String outFile = lDxfFile.replace(".dxf", ".svg");
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(outFile, "UTF-8");
            } catch (FileNotFoundException e) {
                System.out.println("failed to open the file " + outFile + " : " + e);
                return;
            } catch (UnsupportedEncodingException e) {
                System.out.println("failed to open the file " + outFile + " : " + e);
                return;
            }
            if(null == writer) {
                System.out.println("Filed to open the file " + outFile);
                return;
            }

            Bound lBound = new Bound();
            ArrayList<String> lLines = PolylineUtils.convertFileToSvgStr(lDxfFile, lBound);

            //write header
            writer.println("<svg viewBox=\"0 0 " + lBound.getMaxX() + " " + lBound.getMaxY() + "  \" xmlns=\"http://www.w3.org/2000/svg\">");

            for(String line : lLines) {
                writer.println(line);
            }


            writer.println("</svg>");
            writer.close();
        }
    }


}
