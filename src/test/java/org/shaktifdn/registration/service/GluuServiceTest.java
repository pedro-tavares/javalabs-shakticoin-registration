package org.shaktifdn.registration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gluu.scim2.client.rest.ClientSideService;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.core.HoverflyConfig;
import io.specto.hoverfly.junit.core.model.RequestFieldMatcher;
import org.gluu.oxtrust.model.scim2.CustomAttributes;
import org.gluu.oxtrust.model.scim2.ListResponse;
import org.gluu.oxtrust.model.scim2.user.UserResource;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.shaktifdn.registration.config.GluuProperties;
import org.shaktifdn.registration.exception.BadRequestException;
import org.shaktifdn.registration.exception.ConflictRecordsException;
import org.shaktifdn.registration.exception.ExternalServiceDependencyFailure;
import org.shaktifdn.registration.exception.ShaktiWebClientException;
import org.shaktifdn.registration.request.OnboardShaktiUserRequest;
import org.shaktifdn.registration.request.TokenRequest;
import org.shaktifdn.registration.response.TokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;

import static io.specto.hoverfly.junit.core.HoverflyMode.SIMULATE;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.shaktifdn.registration.service.GluuService.USER_ATTRIBUTE_KEY;
import static org.shaktifdn.registration.service.GluuService.USER_NAME_EQ;

class GluuServiceTest {

    private static final String IP_ADDRESS = "0.0.0.0";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GluuProperties gluuProperties = mock(GluuProperties.class);
    private final ClientSideService clientSideService = mock(ClientSideService.class);
    private Hoverfly hoverfly;
    private GluuServiceApi gluuService;

    @BeforeEach
    public void setup() {
        gluuService = new GluuService(
                gluuProperties,
                Schedulers.boundedElastic(),
                clientSideService,
                WebClient.builder()
        );
        var localConfig = HoverflyConfig
                .localConfigs()
                .disableTlsVerification()
                .asWebServer()
                .proxyPort(18999);
        hoverfly = new Hoverfly(localConfig, SIMULATE);
        hoverfly.start();
        when(gluuProperties.getGluuClientId()).thenReturn("clientId");
        when(gluuProperties.getGluuClientSecret()).thenReturn("sec");
        when(gluuProperties.getGluuUri()).thenReturn("http://localhost:18999");
    }

    @AfterEach
    void tearDown() {
        hoverfly.close();
    }

    @Test
    void shouldGetToken() throws JsonProcessingException {
        TokenResponse response = TokenResponse
                .builder()
                .access_token("acc")
                .refresh_token("ref")
                .expires_in("60")
                .scope("x,yc")
                .token_type("type")
                .build();
        TokenRequest request = TokenRequest.builder().username("u").password("p").grant_type("password").build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(GluuService.GLUU_URL_AUTH_TKEN)
                        .body("grant_type=password\u0026scope\u0026username=u\u0026password=p")
                        .willReturn(
                                success()
                                        .body(objectMapper.writeValueAsString(response))
                                        .header("Content-Type", "application/json; charset=utf-8")
                        )
        ));

        StepVerifier
                .create(gluuService.getToken(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void shouldThrowBadRequestGetToken() {
        TokenRequest request = TokenRequest.builder().username("u").password("p").grant_type("password").build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(GluuService.GLUU_URL_AUTH_TKEN)
                        .body("grant_type=password\u0026scope\u0026username=u\u0026password=p")
                        .willReturn(unauthorised().body("" + HttpStatus.UNAUTHORIZED.value()))
        ));

        StepVerifier
                .create(gluuService.getToken(request))
                .expectError(BadRequestException.class)
                .verify();
    }

    @Test
    void shouldThrowShaktiWebClientGetToken() {
        TokenRequest request = TokenRequest.builder().username("u").password("p").grant_type("password").build();
        hoverfly.simulate(dsl(
                service(new RequestFieldMatcher<>(RequestFieldMatcher.MatcherType.REGEX, "*"))
                        .post(GluuService.GLUU_URL_AUTH_TKEN)
                        .body("grant_type=password\u0026scope\u0026username=u\u0026password=p")
                        .willReturn(serverError().body("test"))
        ));

        StepVerifier
                .create(gluuService.getToken(request))
                .expectError(ExternalServiceDependencyFailure.class)
                .verify();
    }

    @Test
    void shouldCreateUser() {
        OnboardShaktiUserRequest request = OnboardShaktiUserRequest
                .builder()
                .email("test@s.com")
                .shaktiID("test@s.com")
                .password("password")
                .mobileNo("+11231231234")
                .pin("1234")
                .build();
        ServerResponse response = new ServerResponse();
        response.setStatus(HttpStatus.CREATED.value());
        ArgumentCaptor<UserResource> userCaptor = ArgumentCaptor.forClass(UserResource.class);
        when(clientSideService.createUser(userCaptor.capture(), any(), any()))
                .thenReturn(response);
        StepVerifier
                .create(gluuService.createUser(request, IP_ADDRESS))
                .expectNext(response)
                .verifyComplete();
        verify(clientSideService).createUser(any(UserResource.class), any(), any());

        assertThat(userCaptor.getValue().getEmails().get(0).getValue()).isEqualTo(request.getEmail());
        assertThat(userCaptor.getValue().getUserName()).isEqualTo(request.getEmail());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo(request.getPassword());
    }

    @Test
    void shouldFailOnConflictCreateUser() {
        OnboardShaktiUserRequest request = OnboardShaktiUserRequest
                .builder()
                .email("test@s.com")
                .shaktiID("test@s.com")
                .password("password")
                .mobileNo("+11231231234")
                .pin("1234")
                .build();
        ServerResponse response = new ServerResponse();
        response.setStatus(HttpStatus.CONFLICT.value());
        ArgumentCaptor<UserResource> userCaptor = ArgumentCaptor.forClass(UserResource.class);
        when(clientSideService.createUser(userCaptor.capture(), any(), any()))
                .thenReturn(response);
        StepVerifier
                .create(gluuService.createUser(request, IP_ADDRESS))
                .expectError(ConflictRecordsException.class)
                .verify();
        verify(clientSideService).createUser(any(UserResource.class), any(), any());

        assertThat(userCaptor.getValue().getEmails().get(0).getValue()).isEqualTo(request.getEmail());
        assertThat(userCaptor.getValue().getUserName()).isEqualTo(request.getEmail());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo(request.getPassword());
    }

    @Test
    void shouldFailCreateUser() {
        OnboardShaktiUserRequest request = OnboardShaktiUserRequest
                .builder()
                .email("test@s.com")
                .shaktiID("test@s.com")
                .password("password")
                .mobileNo("+11231231234")
                .pin("1234")
                .build();
        ServerResponse response = new ServerResponse();
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        ArgumentCaptor<UserResource> userCaptor = ArgumentCaptor.forClass(UserResource.class);
        when(clientSideService.createUser(userCaptor.capture(), any(), any()))
                .thenReturn(response);
        StepVerifier
                .create(gluuService.createUser(request, IP_ADDRESS))
                .expectError(ShaktiWebClientException.class)
                .verify();
        verify(clientSideService).createUser(any(UserResource.class), any(), any());

        assertThat(userCaptor.getValue().getEmails().get(0).getValue()).isEqualTo(request.getEmail());
        assertThat(userCaptor.getValue().getUserName()).isEqualTo(request.getEmail());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo(request.getPassword());
    }

    @Test
    void shouldCheckExistingUser() {
        Response response = mock(Response.class);
        ListResponse listResponse = new ListResponse();
        UserResource userResource = new UserResource();
        listResponse.setResources(List.of(
                userResource
        ));
        LinkedHashMap<String, Object> customAttributes = new LinkedHashMap<>();
        userResource.getCustomAttributes()
                .put("urn:ietf:params:scim:schemas:extension:gluu:2.0:User", customAttributes);
        when(response.readEntity(ListResponse.class)).thenReturn(listResponse);
        when(response.getStatus()).thenReturn(200);
        when(clientSideService.searchUsers(
                USER_NAME_EQ + "test\"",
                1,
                1,
                null,
                null,
                null,
                null
        )).thenReturn(response);
        StepVerifier
                .create(gluuService.isEmailRegistered("test"))
                .expectNext(true)
                .verifyComplete();
        verify(clientSideService).searchUsers(
                USER_NAME_EQ + "test\"",
                1,
                1,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void shouldCheckExistingUserFalse() {
        Response response = mock(Response.class);
        ListResponse listResponse = new ListResponse();
        when(response.readEntity(ListResponse.class)).thenReturn(listResponse);
        when(clientSideService.searchUsers(
                USER_NAME_EQ + "test\"",
                1,
                1,
                null,
                null,
                null,
                null
        )).thenReturn(response);
        StepVerifier
                .create(gluuService.isEmailRegistered("test"))
                .expectNext(false)
                .verifyComplete();
        verify(clientSideService).searchUsers(
                USER_NAME_EQ + "test\"",
                1,
                1,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void shouldThrowErrorOnCheckExistingUser() {
        Response response = mock(Response.class);
        when(response.readEntity(ListResponse.class)).thenThrow(new RuntimeException("unit-test"));
        when(response.getStatus()).thenReturn(200);
        when(clientSideService.searchUsers(
                USER_NAME_EQ + "test\"",
                1,
                1,
                null,
                null,
                null,
                null
        )).thenReturn(response);
        StepVerifier
                .create(gluuService.isEmailRegistered("test"))
                .expectError(ShaktiWebClientException.class)
                .verify();
        verify(clientSideService).searchUsers(
                USER_NAME_EQ + "test\"",
                1,
                1,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void shouldHaveRegisteredEmail() {
        Response response = mock(Response.class);
        ListResponse listResponse = new ListResponse();
        UserResource resource = new UserResource();
        resource.addCustomAttributes(new CustomAttributes(USER_ATTRIBUTE_KEY));
        listResponse.setResources(List.of(resource));
        when(response.readEntity(ListResponse.class)).thenReturn(listResponse);
        when(response.getStatus()).thenReturn(200);
        when(clientSideService.searchUsers(
                USER_NAME_EQ + "a@a.com\"",
                1,
                1,
                null,
                null,
                null,
                null
        )).thenReturn(response);
        StepVerifier
                .create(gluuService.isEmailRegistered("a@a.com"))
                .expectNext(true)
                .verifyComplete();
        verify(clientSideService).searchUsers(
                USER_NAME_EQ + "a@a.com\"",
                1,
                1,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void shouldNotHaveRegisteredEmailMissingAttribute() {
        Response response = mock(Response.class);
        ListResponse listResponse = new ListResponse();
        UserResource resource = new UserResource();
        listResponse.setResources(List.of(resource));
        when(response.readEntity(ListResponse.class)).thenReturn(listResponse);
        when(clientSideService.searchUsers(
                USER_NAME_EQ + "a@a.com\"",
                1,
                1,
                null,
                null,
                null,
                null
        )).thenReturn(response);
        StepVerifier
                .create(gluuService.isEmailRegistered("a@a.com"))
                .expectNext(false)
                .verifyComplete();
        verify(clientSideService).searchUsers(
                USER_NAME_EQ + "a@a.com\"",
                1,
                1,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void shouldNotHaveRegisteredEmail() {
        Response response = mock(Response.class);
        ListResponse listResponse = new ListResponse();
        listResponse.setResources(List.of());
        when(response.readEntity(ListResponse.class)).thenReturn(listResponse);
        when(clientSideService.searchUsers(
                USER_NAME_EQ + "a@a.com\"",
                1,
                1,
                null,
                null,
                null,
                null
        )).thenReturn(response);
        StepVerifier
                .create(gluuService.isEmailRegistered("a@a.com"))
                .expectNext(false)
                .verifyComplete();
        verify(clientSideService).searchUsers(
                USER_NAME_EQ + "a@a.com\"",
                1,
                1,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void shouldErrorOnRegisteredEmail() {
        Response response = mock(Response.class);
        when(response.readEntity(ListResponse.class)).thenThrow(new RuntimeException("unit-test"));
        when(response.getStatus()).thenReturn(200);
        when(clientSideService.searchUsers(
                USER_NAME_EQ + "a@a.com\"",
                1,
                1,
                null,
                null,
                null,
                null
        )).thenReturn(response);
        StepVerifier
                .create(gluuService.isEmailRegistered("a@a.com"))
                .expectError(RuntimeException.class)
                .verify();
        verify(clientSideService).searchUsers(
                USER_NAME_EQ + "a@a.com\"",
                1,
                1,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void shouldDeleteUser() {

        ServerResponse response = mock(ServerResponse.class);
        ListResponse listResponse = new ListResponse();
        UserResource resource = new UserResource();
        resource.setId("test-1");
        resource.addCustomAttributes(new CustomAttributes(USER_ATTRIBUTE_KEY));
        listResponse.setResources(List.of(resource));
        when(response.readEntity(ListResponse.class)).thenReturn(listResponse);
        when(response.getStatus()).thenReturn(200);
        when(clientSideService.searchUsers(
                USER_NAME_EQ + "aa@aa.com\"",
                1,
                1,
                null,
                null,
                null,
                null
        )).thenReturn(response);

        response = new ServerResponse();
        response.setStatus(HttpStatus.NO_CONTENT.value());
        when(clientSideService.deleteUser(resource.getId()))
                .thenReturn(response);
        StepVerifier
                .create(gluuService.deleteUser("aa@aa.com"))
                .expectNext(response)
                .verifyComplete();
        verify(clientSideService).deleteUser(resource.getId());
    }

    @Test
    void shouldHandleRecordNotFoundOnDeleteUser() {
        ServerResponse response = mock(ServerResponse.class);
        ListResponse listResponse = new ListResponse();
        UserResource resource = new UserResource();
        resource.setId("test-1");
        resource.addCustomAttributes(new CustomAttributes(USER_ATTRIBUTE_KEY));
        listResponse.setResources(List.of(resource));
        when(response.readEntity(ListResponse.class)).thenReturn(listResponse);
        when(response.getStatus()).thenReturn(200);
        when(clientSideService.searchUsers(
                USER_NAME_EQ + "aa@aa.com\"",
                1,
                1,
                null,
                null,
                null,
                null
        )).thenReturn(response);

        response = new ServerResponse();
        response.setStatus(HttpStatus.NOT_FOUND.value());
        when(clientSideService.deleteUser(resource.getId()))
                .thenReturn(response);
        StepVerifier
                .create(gluuService.deleteUser("aa@aa.com"))
                .expectNext(response)
                .verifyComplete();
        verify(clientSideService).deleteUser(resource.getId());
    }

    @Test
    void shouldErrorOnDeleteUser() {

        ServerResponse response = mock(ServerResponse.class);
        ListResponse listResponse = new ListResponse();
        UserResource resource = new UserResource();
        resource.setId("test-1");
        resource.addCustomAttributes(new CustomAttributes(USER_ATTRIBUTE_KEY));
        listResponse.setResources(List.of(resource));
        when(response.readEntity(ListResponse.class)).thenReturn(listResponse);
        when(response.getStatus()).thenReturn(200);
        when(clientSideService.searchUsers(
                USER_NAME_EQ + "aa@aa.com\"",
                1,
                1,
                null,
                null,
                null,
                null
        )).thenReturn(response);

        response = new ServerResponse();
        response.setStatus(HttpStatus.NOT_FOUND.value());
        when(clientSideService.deleteUser(resource.getId()))
                .thenThrow(new RuntimeException("unit-test"));
        StepVerifier
                .create(gluuService.deleteUser("aa@aa.com"))
                .expectErrorMessage("Error on Shakti delete user service call unit-test")
                .verify();
        verify(clientSideService).deleteUser(resource.getId());
    }
}