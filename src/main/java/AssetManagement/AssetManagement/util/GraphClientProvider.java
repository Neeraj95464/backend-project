package AssetManagement.AssetManagement.util;

import AssetManagement.AssetManagement.config.GraphApiConfig;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;

import java.util.List;

public class GraphClientProvider {

    private final GraphServiceClient<Request> graphClient;

    public GraphClientProvider(GraphApiConfig config) {
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret())
                .tenantId(config.getTenantId())
                .build();

        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(
                List.of("https://graph.microsoft.com/.default"), clientSecretCredential);

        this.graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();
    }

    public GraphServiceClient<Request> getGraphClient() {
        return graphClient;
    }
}

