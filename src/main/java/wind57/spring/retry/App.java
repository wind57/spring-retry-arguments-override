package wind57.spring.retry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import wind57.spring.retry.interceptor.InterceptorConfig;
import wind57.spring.retry.properties.RetryProperties;

@SpringBootApplication
@EnableConfigurationProperties(RetryProperties.class)
@EnableRetry
@Import(InterceptorConfig.class)
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

}
