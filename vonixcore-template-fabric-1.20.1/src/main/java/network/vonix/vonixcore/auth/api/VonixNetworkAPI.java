package network.vonix.vonixcore.auth.api;

import network.vonix.vonixcore.VonixCore;
import network.vonix.vonixcore.config.AuthConfig;
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * API client for Vonix Network authentication.
 */
public class VonixNetworkAPI {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
    
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static void shutdown() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

    public static CompletableFuture<RegistrationResponse> generateRegistrationCode(String username, String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = AuthConfig.getInstance().getVerificationApiUrl() + "/generate-code";
                String json = String.format("{\"username\":\"%s\",\"uuid\":\"%s\"}", username, uuid);
                
                Request request = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(json, MediaType.parse("application/json")))
                        .addHeader("X-API-Key", AuthConfig.getInstance().getVerificationApiKey())
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        return mapper.readValue(response.body().string(), RegistrationResponse.class);
                    }
                }
            } catch (Exception e) {
                VonixCore.LOGGER.error("[Auth API] Error generating code: {}", e.getMessage());
            }
            RegistrationResponse failed = new RegistrationResponse();
            failed.error = "API request failed";
            return failed;
        });
    }

    public static CompletableFuture<CheckRegistrationResponse> checkPlayerRegistration(String username, String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = AuthConfig.getInstance().getVerificationApiUrl() + "/check?username=" + username + "&uuid=" + uuid;
                
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("X-API-Key", AuthConfig.getInstance().getVerificationApiKey())
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        return mapper.readValue(response.body().string(), CheckRegistrationResponse.class);
                    }
                }
            } catch (Exception e) {
                VonixCore.LOGGER.error("[Auth API] Error checking registration: {}", e.getMessage());
            }
            CheckRegistrationResponse failed = new CheckRegistrationResponse();
            failed.registered = false;
            return failed;
        });
    }

    public static CompletableFuture<LoginResponse> registerPlayerWithPassword(String username, String uuid, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = AuthConfig.getInstance().getVerificationApiUrl() + "/register";
                String json = String.format("{\"username\":\"%s\",\"uuid\":\"%s\",\"password\":\"%s\"}", 
                        username, uuid, password);
                
                Request request = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(json, MediaType.parse("application/json")))
                        .addHeader("X-API-Key", AuthConfig.getInstance().getVerificationApiKey())
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        return mapper.readValue(response.body().string(), LoginResponse.class);
                    }
                }
            } catch (Exception e) {
                VonixCore.LOGGER.error("[Auth API] Error registering: {}", e.getMessage());
            }
            LoginResponse failed = new LoginResponse();
            failed.success = false;
            failed.error = "API request failed";
            return failed;
        });
    }

    public static CompletableFuture<LoginResponse> loginPlayer(String username, String uuid, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = AuthConfig.getInstance().getVerificationApiUrl() + "/login";
                String json = String.format("{\"username\":\"%s\",\"uuid\":\"%s\",\"password\":\"%s\"}", 
                        username, uuid, password);
                
                Request request = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(json, MediaType.parse("application/json")))
                        .addHeader("X-API-Key", AuthConfig.getInstance().getVerificationApiKey())
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        return mapper.readValue(response.body().string(), LoginResponse.class);
                    }
                }
            } catch (Exception e) {
                VonixCore.LOGGER.error("[Auth API] Error logging in: {}", e.getMessage());
            }
            LoginResponse failed = new LoginResponse();
            failed.success = false;
            failed.error = "API request failed";
            return failed;
        });
    }

    // Response classes
    public static class RegistrationResponse {
        public String code;
        public boolean already_registered;
        public String error;
    }

    public static class CheckRegistrationResponse {
        public boolean registered;
        public String error;
    }

    public static class LoginResponse {
        public boolean success;
        public String token;
        public String error;
        public UserInfo user;
    }

    public static class UserInfo {
        public String username;
        public String uuid;
        public DonationRank donation_rank;
    }

    public static class DonationRank {
        public String name;
        public int priority;
    }
}
