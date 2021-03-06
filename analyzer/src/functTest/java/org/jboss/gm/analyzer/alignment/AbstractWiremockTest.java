package org.jboss.gm.analyzer.alignment;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;
import org.junit.BeforeClass;
import org.junit.Rule;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

public abstract class AbstractWiremockTest {

    static final int PORT = 8089;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);

    @BeforeClass
    public static void beforeClass() {
        StdErrLog el = new StdErrLog();
        el.setLevel(10);
        Log.setLog(el);
    }

    String readSampleDAResponse(String responseFileName) throws URISyntaxException, IOException {
        return FileUtils.readFileToString(
                Paths.get(AbstractWiremockTest.class.getClassLoader().getResource(responseFileName)
                        .toURI()).toFile(),
                StandardCharsets.UTF_8.name());
    }
}
