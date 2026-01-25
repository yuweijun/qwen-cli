package com.example.askquery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dashscope")
public class DashscopeProperties {
    private Api api = new Api();
    private String model = "qwen-plus";

    public static class Api {
        private String key;

        public String getKey() {
            // Return the configured key if available, otherwise try environment variable
            if (key == null || key.trim().isEmpty()) {
                return System.getenv("DASHSCOPE_API_KEY");
            }
            return key;
        }

        public void setKey(String key) { this.key = key; }
    }

    public Api getApi() { return api; }
    public void setApi(Api api) { this.api = api; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}