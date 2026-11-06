
package com.hardwarestore.notification.sms;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.hardwarestore.security.OtpDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;


@Service
@Slf4j
@ConditionalOnProperty(
        name = "app.otp.delivery-mode",
        havingValue = "kusha"
)
public class KushaSmsOtpDeliveryService implements OtpDeliveryService {

    private static final String API_URL =
            "https://kushasms.com/sms/v4/send-user";

    private final RestClient restClient = RestClient.create();

    @Value("${app.otp.kusha.api-token:}")
    private String apiToken;

    @Override
    public void sendOtp(String mobileNumber, String otp) {

        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalStateException(
                    "Kusha SMS API token is missing."
            );
        }

        String message = "Your Hardware Store verification code is "
                + otp
                + ". It expires in 5 minutes. Do not share this code.";

        // Build the JSON body explicitly.
        String jsonBody = """
                {
                    "to": ["%s"],
                    "text": ["%s"]
                }
                """.formatted(mobileNumber, message);

        try {
            JsonNode response = restClient.post()
                    .uri(API_URL)
                    .header("auth-token", apiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new IllegalStateException(
                        "Kusha SMS returned an empty response."
                );
            }

            if (response.path("error").asBoolean(false)) {
                String error = response.path("message")
                        .asText("Kusha SMS rejected the request.");

                throw new IllegalStateException(
                        "Kusha SMS error: " + error
                );
            }

            JsonNode responses = response.path("responses");

            if (!responses.isArray() || responses.isEmpty()) {
                log.error("Unexpected Kusha SMS response: {}", response);

                throw new IllegalStateException(
                        "Unexpected response format from Kusha SMS."
                );
            }

            JsonNode firstResponse = responses.get(0);

            if (firstResponse.path("error").asBoolean(true)) {
                throw new IllegalStateException(
                        "Kusha SMS error: "
                                + firstResponse.path("message")
                                .asText("SMS request rejected.")
                );
            }

            JsonNode validNumbers = firstResponse
                    .path("data")
                    .path("valid");

            if (!validNumbers.isArray() || validNumbers.isEmpty()) {
                log.error("Kusha SMS recipient validation failed: {}",
                        firstResponse);

                throw new IllegalStateException(
                        "Kusha SMS did not accept the recipient number."
                );
            }

            log.info(
                    "Kusha SMS accepted OTP request for number ending in {}",
                    mobileNumber.substring(
                            Math.max(0, mobileNumber.length() - 4)
                    )
            );

        } catch (RestClientException exception) {
            log.error("Kusha SMS communication failed.", exception);

            throw new IllegalStateException(
                    "Unable to communicate with Kusha SMS.",
                    exception
            );
        }
    }
}
