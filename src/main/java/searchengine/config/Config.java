package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "settings")
public class Config {
    private List<ConfigSite> sites;
    private String userAgent;
    private String referrer;
    private Map<String, String> messages;
}
