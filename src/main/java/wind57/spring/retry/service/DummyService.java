package wind57.spring.retry.service;

import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class DummyService {

    private int x;

    @Retryable(interceptor = "retryInterceptor")
    public int call() {
        if (x == 0) {
            ++x;
            throw new RuntimeException("just because");
        }

        System.out.println("hehe, you like retrying?");
        return x;
    }

}
