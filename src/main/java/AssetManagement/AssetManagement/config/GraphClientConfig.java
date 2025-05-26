package AssetManagement.AssetManagement.config;


import AssetManagement.AssetManagement.util.GraphClientProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphClientConfig {

    @Bean
    public GraphClientProvider graphClientProvider(GraphApiConfig config) {
        return new GraphClientProvider(config);
    }
}
