/*
 * Created by Asad Javed on 28/08/2017
 * Aalto University project
 *
 * Last modified 06/09/2017
 */

package api.wrapper.WrapperOmi;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final String PROCESS_SEGMENTS       = "-processSegments";
    private static final String PROCESS_SEGMENTS_SHORT = "-ps";
    private static final int    MAX_RETRY              = 5;
    private static final Logger LOG                    = LoggerFactory.getLogger(Main.class);

    private static int     retriedTimes    = 0;
    private static Boolean processSegments = Boolean.FALSE;

    public static void main(String[] args) throws InterruptedException {
        processArgs(args);
        // Load and get properties from the file
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("resources/config.properties"));
        } catch (IOException e1) {
            LOG.error("Error loading properties.", e1);
        }

        WazeData wazeObject = new WazeData(prop);

        if (processSegments) {
            LOG.debug("-ps flag detected");
            processSegments(wazeObject);
        }

        String wazeAlertsUrl = prop.getProperty("waze_alerts");
        String wazeJamsUrl = prop.getProperty("waze_jams");

        while (retriedTimes < MAX_RETRY) {
            try {
                // For getting waze alerts and parse it
                LOG.debug("Data access has been granted...!");
                String jsonDataA = wazeObject.getJsonData(wazeAlertsUrl, wazeObject.getAccessToken());
                JSONObject jObjectA = new JSONObject(jsonDataA);
                JSONObject wazeAlertsA = (JSONObject) jObjectA.get("waze_alerts");
                JSONArray wazeArrayA = (JSONArray) wazeAlertsA.get("waze_alert");

                // For getting waze jams and parse it
                String jsonDataJ = wazeObject.getJsonData(wazeJamsUrl, wazeObject.getAccessToken());
                JSONObject jObjectJ = new JSONObject(jsonDataJ);
                JSONObject wazeAlertsJ = (JSONObject) jObjectJ.get("waze_jams");
                JSONArray wazeArrayJ = (JSONArray) wazeAlertsJ.get("waze_jam");

                LOG.debug("Total alerts that need to be processed: {}", wazeArrayA.length());
                wazeObject.parseArray(wazeArrayA, 1);
                LOG.info("*****Done*****");

                LOG.debug("Total jams that need to be processed: {}", wazeArrayJ.length());
                wazeObject.parseArray(wazeArrayJ, 1);
                LOG.debug("*****Done*****");

                LOG.debug("Wait for 4 minutes...");
                Thread.sleep(240000);
                retriedTimes = 0;
            } catch (Exception e) {
                LOG.error("Error during process.", e);
                retriedTimes++;
                try {
                    Thread.sleep(240000);
                } catch (InterruptedException ie) {
                    LOG.error("Interrupted.", ie);
                    throw ie;
                }
            }
        }
    }

    private static void processSegments(WazeData wazeObject) {
        int offset = 0;
        while (wazeObject.count < 22780) {
            String segmentsURL =
                    "https://api.irisnetlab.be:443/api/biotope-datasources/0.0.1/biotope_street_axis2/axis?limit=1000&offset="
                            + Integer.toString(offset);
            try {
                String segmentsData = wazeObject.getJsonData(segmentsURL, wazeObject.getAccessToken());
                JSONObject segmentsObject = new JSONObject(segmentsData);
                JSONObject jObjectS = (JSONObject) segmentsObject.get("data_list");
                JSONArray segmentsArray = (JSONArray) jObjectS.get("data");
                wazeObject.parseArray(segmentsArray, 0);
                wazeObject.sendPartialSegments();
                offset = offset + 1000;
                LOG.debug("{} segments processed.", wazeObject.count);
            } catch (Exception e) {
                LOG.error("Error while processing segments.", e);
            }
        }
        LOG.info("The ODF structure with all segments has been processed.");
    }

    private static void processArgs(Object... args) {
        for (Object arg : args) {
            switch (arg.toString()) {
                case PROCESS_SEGMENTS:
                case PROCESS_SEGMENTS_SHORT:
                    processSegments = Boolean.TRUE;
                    break;
                default:
            }
        }
    }
}
