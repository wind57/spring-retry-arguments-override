package wind57.spring.retry.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wind57.retry")
public class RetryProperties {

    private int maxAttempts;

    private int splitBy;

    private BackOff backOff;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getSplitBy() {
        return splitBy;
    }

    public void setSplitBy(int splitBy) {
        this.splitBy = splitBy;
    }

    public BackOff getBackOff() {
        return backOff;
    }

    public void setBackOff(BackOff backOff) {
        this.backOff = backOff;
    }

    public static class BackOff {

        private int delay;

        private int maxDelay;

        private double multiplier;

        public int getDelay() {
            return delay;
        }

        public void setDelay(int delay) {
            this.delay = delay;
        }

        public int getMaxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(int maxDelay) {
            this.maxDelay = maxDelay;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
    }
}
