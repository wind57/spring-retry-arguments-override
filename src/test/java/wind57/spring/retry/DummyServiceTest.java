package wind57.spring.retry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import wind57.spring.retry.service.DummyService;

class DummyServiceTest {

    @Nested
    @SpringBootTest(properties = "retry.enabled=false", classes = App.class)
    class NeverRetry {

        @Autowired
        private DummyService service;

        @Test
        void test() {
            RuntimeException re = Assertions.assertThrows(RuntimeException.class, () -> service.call());
            Assertions.assertEquals(re.getMessage(), "just because");
        }

    }

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @SpringBootTest(properties = "retry.enabled=true", classes = App.class)
    class WithRetry {

        @Autowired
        private DummyService service;

        @Test
        void test(CapturedOutput out) {
            int x = Assertions.assertDoesNotThrow(() -> service.call());
            Assertions.assertEquals(x, 1);
            Assertions.assertTrue(out.getOut().contains("hehe, you like retrying?"));
        }

    }

}
