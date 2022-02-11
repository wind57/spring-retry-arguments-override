package wind57.spring.retry;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import wind57.spring.retry.service.ServiceWithParams;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ServiceWithParamsTest {

    // we start with a diff of 4 hours
    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @SpringBootTest(classes = App.class, properties = {"retry.change.params.enabled=true", "wind57.retry.splitBy=4"})
    class RetryWithoutHttpCalls {

        @Autowired
        private ServiceWithParams service;

        @Test
        void test(CapturedOutput output) {
            LocalDateTime now = LocalDateTime.now();
            String nice = service.statusNoHttpCalls(now, now.plusHours(4));
            Assertions.assertEquals(nice, "nice");
            Assertions.assertTrue(output.getOut().contains("Failed on attempt : 1 with error : diff is in hours, too big. Got : 4 hours"));
            Assertions.assertTrue(output.getOut().contains("Failed on attempt : 2 with error : diff is in minutes, too big. Got : 60 minutes"));
            Assertions.assertTrue(output.getOut().contains("Failed on attempt : 3 with error : diff is in minutes, too big. Got : 15 minutes"));
            Assertions.assertTrue(output.getOut().contains("Failed on attempt : 4 with error : diff is in minutes, too big. Got : 3 minutes"));
            Assertions.assertTrue(output.getOut().contains("under one minute, good!"));
            Assertions.assertTrue(output.getOut().contains("Big success!"));
        }

    }

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @SpringBootTest(classes = App.class, properties = {"retry.change.params.enabled=true", "wind57.retry.splitBy=4"})
    class RetryWithHttpCalls {

        @Autowired
        private ServiceWithParams service;

        private static WireMockServer wireMockServer;

        private static final LocalDateTime START = LocalDateTime.now();
        private static final LocalDateTime END = START.plusHours(4);

        @BeforeAll
        static void setup() {
            wireMockServer = new WireMockServer(80);
            wireMockServer.start();
            WireMock.configureFor("localhost", wireMockServer.port());
        }

        @AfterAll
        static void teardown() {
            wireMockServer.stop();
        }

        @Test
        void test(CapturedOutput output) {

            LocalDateTime end = END;

            // stub for 4 hours diff
            WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/hour"))
                            .inScenario("calls")
                            .willSetStateTo("first-minute-call")
                            .withQueryParam("start", WireMock.equalTo(ServiceWithParams.FORMATTER.format(START)))
                            .withQueryParam("end", WireMock.equalTo(ServiceWithParams.FORMATTER.format(end)))
                    .willReturn(WireMock.aResponse().withStatus(500).withBody("interval too big (4 hours)")));

            LocalDateTime end2 = newEnd(end);

            // stub for 60 minutes diff
            WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/minute"))
                            .inScenario("calls")
                            .whenScenarioStateIs("first-minute-call")
                            .willSetStateTo("second-minute-call")
                    .withQueryParam("start", WireMock.equalTo(ServiceWithParams.FORMATTER.format(START)))
                    .withQueryParam("end", WireMock.equalTo(ServiceWithParams.FORMATTER.format(end2)))
                    .willReturn(WireMock.aResponse().withStatus(500).withBody("interval too big (60 minutes)")));

            LocalDateTime end3 = newEnd(end2);

            // stub for 15 minutes diff
            WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/minute"))
                            .inScenario("calls")
                            .whenScenarioStateIs("second-minute-call")
                            .willSetStateTo("third-minute-call")
                    .withQueryParam("start", WireMock.equalTo(ServiceWithParams.FORMATTER.format(START)))
                    .withQueryParam("end", WireMock.equalTo(ServiceWithParams.FORMATTER.format(end3)))
                    .willReturn(WireMock.aResponse()
                            .withStatus(500).withBody("interval too big (15 minutes)")));

            LocalDateTime end4 = newEnd(end3);

            // stub for 3 minutes diff
            WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/minute"))
                        .inScenario("calls")
                        .whenScenarioStateIs("third-minute-call")
                    .withQueryParam("start", WireMock.equalTo(ServiceWithParams.FORMATTER.format(START)))
                    .withQueryParam("end", WireMock.equalTo(ServiceWithParams.FORMATTER.format(end4)))
                    .willReturn(WireMock.aResponse()
                            .withStatus(500).withBody("interval too big (3 minutes)")));

            LocalDateTime end5 = newEnd(end4);

            WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/under-minute"))
                    .withQueryParam("start", WireMock.equalTo(ServiceWithParams.FORMATTER.format(START)))
                    .withQueryParam("end", WireMock.equalTo(ServiceWithParams.FORMATTER.format(end5)))
                    .willReturn(WireMock.aResponse()
                            .withStatus(200).withBody("nice")));

            String nice = service.status(START, END);

            Assertions.assertEquals(nice, "nice");
            Assertions.assertTrue(output.getOut().contains("Failed on attempt : 1 with error : interval too big (4 hours)"));
            Assertions.assertTrue(output.getOut().contains("Failed on attempt : 2 with error : interval too big (60 minutes)"));
            Assertions.assertTrue(output.getOut().contains("Failed on attempt : 3 with error : interval too big (15 minutes)"));
            Assertions.assertTrue(output.getOut().contains("Failed on attempt : 4 with error : interval too big (3 minutes)"));
            Assertions.assertTrue(output.getOut().contains("Big success!"));
        }

        private static LocalDateTime newEnd(LocalDateTime end) {
            long seconds = ChronoUnit.SECONDS.between(START, end);
            long narrow = seconds / 4; // we know that the test splits by 4
            return START.plusSeconds(narrow);
        }

    }

}
