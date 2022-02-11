package wind57.spring.retry.interceptor;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import wind57.spring.retry.properties.RetryProperties;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Configuration
public class InterceptorConfig {

    @ConditionalOnProperty(name = "retry.enabled", havingValue = "false")
    @Bean(name = "retryInterceptor")
    public RetryOperationsInterceptor neverRetry() {
        return RetryInterceptorBuilder.stateless().retryPolicy(new NeverRetryPolicy()).build();
    }

    @ConditionalOnProperty(name = "retry.enabled", havingValue = "true")
    @Bean(name = "retryInterceptor")
    public RetryOperationsInterceptor retryWithDefaults(RetryProperties props) {

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(props.getBackOff().getDelay());
        backOff.setMultiplier(props.getBackOff().getMultiplier());
        backOff.setMaxInterval(props.getBackOff().getMaxDelay());

        RetryTemplate template = RetryTemplate.builder()
                .retryOn(Exception.class)
                .customBackoff(backOff)
                .build();

        return RetryInterceptorBuilder.stateless()
                .maxAttempts(props.getMaxAttempts())
                .retryOperations(template)
                .build();
    }

    @ConditionalOnProperty(name="retry.change.params.enabled", havingValue = "true")
    @Bean(name = "retryInterceptor")
    public MethodInterceptor retryWithChangedParams(RetryProperties props) {

        // how much to narrow the gap on the interval between dates. For example this value = 4
        // and start=12:00 and end=20:00. First different between start/end is 8 hours.
        // we will narrow this difference in subsequent request to 2 hours (8 / splitBy)
        // the next one will be 30 minutes (2 hours / splitBy) and so on.
        int splitBy = props.getSplitBy();

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(props.getBackOff().getDelay());
        backOff.setMultiplier(props.getBackOff().getMultiplier());
        backOff.setMaxInterval(props.getBackOff().getMaxDelay());

        RetryTemplate template = RetryTemplate.builder()
                .retryOn(Exception.class)
                .customBackoff(backOff)
                .withListener(new CustomListener())
                .maxAttempts(props.getMaxAttempts())
                .build();

        /*
         * A custom implementation of a MethodInterceptor that allows to override
         * some inputs.
         */
        return invocation -> {

            return template.execute(context -> {

                if(context.getRetryCount() > 0) {
                    Object[] arguments = invocation.getArguments();
                    LocalDateTime left = (LocalDateTime) arguments[0];
                    LocalDateTime right = (LocalDateTime) arguments[1];

                    // if we got here, we already know that the difference it too big, thus we need to split.
                    // that is: we need to narrow the gap between start and end.
                    long seconds = ChronoUnit.SECONDS.between(left, right);
                    long narrow = seconds / splitBy;
                    right = left.plusSeconds(narrow);
                    arguments[1] = right;

                }
                return invocation.proceed();
            });
        };
    }

    private static class CustomListener implements RetryListener {

        // method that is called before the first attempt. might be needed to "register" some things
        @Override
        public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
            return true;
        }

        // when retry closes; i.e.: retry failed overall.
        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            if (throwable != null) {
                System.out.println("Failed overall retry with last error : " + throwable.getMessage());
            } else {
                System.out.println("Big success!");
            }
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            System.out.println("Failed on attempt : " + context.getRetryCount() + " with error : " + throwable.getMessage());
        }
    }

}
