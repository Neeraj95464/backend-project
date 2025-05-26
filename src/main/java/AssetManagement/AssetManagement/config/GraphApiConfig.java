package AssetManagement.AssetManagement.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class GraphApiConfig {

    @Value("${azure.client-id}")
    private String clientId;

    @Value("${azure.client-secret}")
    private String clientSecret;

    @Value("${azure.tenant-id}")
    private String tenantId;

    // Getters
}

