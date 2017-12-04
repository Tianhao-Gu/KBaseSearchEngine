package kbasesearchengine.test.controllers.elasticsearch;

import static us.kbase.common.test.controllers.ControllerCommon.checkExe;
import static us.kbase.common.test.controllers.ControllerCommon.findFreePort;
import static us.kbase.common.test.controllers.ControllerCommon.makeTempDirs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;

import us.kbase.common.service.UObject;


/** Q&D Utility to run a ElasticSearch server for the purposes of testing from
 * Java.
 * @author gaprice@lbl.gov
 *
 */
public class ElasticSearchController {
    
    private final static String DATA_DIR = "data";
    private final static String SCRIPTS_DIR = "scripts";
    
    private final static List<String> tempDirectories = new LinkedList<String>();
    static {
        tempDirectories.add(DATA_DIR);
    }
    
    private final Path tempDir;
    
    private final Process es;
    private final int port;

    //TODO TEST set location of indexes to temp dir so they can be cleaned up

    public ElasticSearchController(
            final String elasticSearchExe,
            final Path rootTempDir)
            throws Exception {
        checkExe(elasticSearchExe, "ElasticSearch server");
        tempDir = makeTempDirs(rootTempDir, "ElasticSearchController-", tempDirectories);
        port = findFreePort();

        List<String> command = new LinkedList<String>();
        // -E syntax for 5.0+
        command.addAll(Arrays.asList(elasticSearchExe,
                "-E", "http.port=" + port,
                "-E", "path.data=" + tempDir.resolve(DATA_DIR).toString(),
                "-E", "path.scripts=" + tempDir.resolve(SCRIPTS_DIR).toString(),
                "-E", "path.logs=" + tempDir.toString()));
        ProcessBuilder servpb = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(tempDir.resolve("es.log").toFile());
        
        es = servpb.start();
        Exception startupError = null;
        for (int i = 0; i < 40; i++) {
            Thread.sleep(1000); //wait for server to start up
            try (InputStream is = new URL("http://localhost:" + port).openStream()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = UObject.getMapper().readValue(is, Map.class);
                @SuppressWarnings("unchecked")
                Map<String, Object> verMap = (Map<String, Object>)data.get("version");
                String version = verMap == null ? null : (String)verMap.get("number");
                if (version == null) {
                    System.out.println("WARNING! Couldn't extract version of Elastic: " + data);
                } else {
                    if (!version.startsWith("5.")) {
                        System.out.println("WARNING! Your Elastic version is not default: " + version);
                    }
                }
                startupError = null;
                break;
            } catch (Exception e) {
                startupError = e;
            }
        }
        if (startupError != null) {
            throw startupError;
        }
    }

    public int getServerPort() {
        return port;
    }
    
    public Path getTempDir() {
        return tempDir;
    }
    
    public void destroy(boolean deleteTempFiles) throws IOException {
        if (es != null) {
            es.destroy();
        }
        if (tempDir != null && deleteTempFiles) {
            FileUtils.deleteDirectory(tempDir.toFile());
        }
    }

    public static void main(String[] args) throws Exception {
        ElasticSearchController ac = new ElasticSearchController(
                "/Users/rsutormin/Programs/elastic/bin/elasticsearch",
                Paths.get("test_local/manual"));
        System.out.println(ac.getServerPort());
        System.out.println(ac.getTempDir());
        Scanner reader = new Scanner(System.in);
        System.out.println("any char to shut down");
        //get user input for a
        reader.next();
        ac.destroy(false);
        reader.close();
    }
    
}

