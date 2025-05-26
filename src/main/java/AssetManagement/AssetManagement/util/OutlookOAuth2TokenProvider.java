package AssetManagement.AssetManagement.util;

import com.microsoft.aad.msal4j.*;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class OutlookOAuth2TokenProvider {

    public static String getAccessToken(String clientId, String clientSecret, String tenantId, String scope) throws Exception {
        IClientCredential credential = ClientCredentialFactory.createFromSecret(clientSecret);

        ConfidentialClientApplication app = ConfidentialClientApplication.builder(clientId, credential)
                .authority("https://login.microsoftonline.com/" + tenantId)
                .build();

        ClientCredentialParameters parameters = ClientCredentialParameters.builder(Collections.singleton(scope)).build();

        CompletableFuture<IAuthenticationResult> future = app.acquireToken(parameters);
        return future.get().accessToken();
    }
}
