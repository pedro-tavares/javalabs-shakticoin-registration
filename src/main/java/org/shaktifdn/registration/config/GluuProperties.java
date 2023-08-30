package org.shaktifdn.registration.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Data
@RefreshScope
@Configuration
@Slf4j
public class GluuProperties {

    @Value("${gluu.uri}")
    private String gluuUri;

    @Value("${gluu.client.id}")
    private String gluuClientId;

    @Value("${gluu.client.secret}")
    private String gluuClientSecret;

    @Value("${gluu.webclient.create-user-clientID}")
    private String umaAatClientId;

    @Value("${gluu.webclient.create-user-jskpath}")
    private File umaAatClientJksPath;

    @Value("${gluu.webclient.create-user-jkspassword}")
    private String umaAatClientJksPassword;

    @Value("${gluu.webclient.create-user-client-keyID}")
    private String umaAatClientKeyId;



}
