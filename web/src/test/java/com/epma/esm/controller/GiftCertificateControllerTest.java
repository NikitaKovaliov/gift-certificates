package com.epma.esm.controller;


import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.is;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.epam.esm.config.RepositoryConfig;
import com.epam.esm.config.SecurityConfig;
import com.epam.esm.config.ServiceConfig;
import com.epam.esm.config.WebConfig;
import com.epam.esm.dto.GiftCertificateWithTagsDto;
import com.epam.esm.dto.PriceDto;
import com.epam.esm.model.GiftCertificate;
import com.epam.esm.model.Role;
import com.epam.esm.model.Tag;
import com.epam.esm.model.User;
import com.epam.esm.security.AuthenticationFilter;
import com.epam.esm.security.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.SchemaVersion;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = {SecurityConfig.class, WebConfig.class, ServiceConfig.class,
    RepositoryConfig.class})
public class GiftCertificateControllerTest {

  private static final String ALL_CERTIFICATES_ENDPOINT = "/certificates";
  private static final String CERTIFICATE_LIST_SCHEMA_NAME =
      "validation/certificate/certificate-list-validation-schema.json";
  private static final String CERTIFICATE_OBJECT_SCHEMA_NAME =
      "validation/certificate/certificate-object-validation-schema.json";
  private static final String EXCEPTION_OBJECT_SCHEMA_NAME =
      "validation/exception/exception-object-validation-schema.json";
  private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

  @Autowired
  private WebApplicationContext webApplicationContext;
  @Autowired
  private AuthenticationFilter authenticationFilter;
  @Autowired
  private TokenService tokenService;
  private JsonSchemaFactory jsonSchemaFactory;
  private GiftCertificateWithTagsDto giftCertificateWithTagsDto;
  private String adminToken;
  private String userToken;


  @Before
  public void initializeRestAssuredMockMvcWebApplicationContext() {
    MockMvc mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext).addFilters(authenticationFilter).build();
    RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
    RestAssuredMockMvc.mockMvc(mockMvc);
    MockMvcBuilders.webAppContextSetup(webApplicationContext).addFilter(authenticationFilter);
    GiftCertificate giftCertificate = new GiftCertificate(10L, "name", "description", BigDecimal.valueOf(3.5),
        null, null, 5);
    Tag tag = new Tag(1L, "for_rent");
    adminToken = tokenService.createToken(new User(47L, "username2", "password", Role.ADMIN));
    userToken = tokenService.createToken(new User(1L, "username", "password", Role.USER));
    giftCertificateWithTagsDto = new GiftCertificateWithTagsDto(giftCertificate, Collections.singletonList(tag));
    jsonSchemaFactory = JsonSchemaFactory
        .newBuilder().setValidationConfiguration(ValidationConfiguration
            .newBuilder().setDefaultVersion(SchemaVersion.DRAFTV4)
            .freeze())
        .freeze();
  }

  @Test
  public void findAllCertificatesTest() {
    given().when().get(ALL_CERTIFICATES_ENDPOINT)
        .then()
        .statusCode(HttpStatus.OK.value())
        .assertThat().body(matchesJsonSchemaInClasspath(CERTIFICATE_LIST_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void findAllCertificatesTestWithPagination() {
    String pageParameters = "?page=1&perPage=2";
    given().when().get(ALL_CERTIFICATES_ENDPOINT + pageParameters)
        .then()
        .statusCode(HttpStatus.OK.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(CERTIFICATE_LIST_SCHEMA_NAME).using(jsonSchemaFactory))
        .body("size()", is(2));
  }

  @Test
  public void findCertificateByIdTestReturnsCertificateWithTags() {
    given()
        .when().get(ALL_CERTIFICATES_ENDPOINT + "/47")
        .then()
        .statusCode(HttpStatus.OK.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(CERTIFICATE_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void findCertificateByIdTestReturnsExceptionObject() {
    given()
        .when().get(ALL_CERTIFICATES_ENDPOINT + "/40")
        .then()
        .statusCode(HttpStatus.NOT_FOUND.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void createCertificateCorrectDataReturnsCreatedObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    given()
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(giftCertificateWithTagsDto))
        .header(AUTHORIZATION_HEADER_NAME, adminToken)
        .when()
        .post(ALL_CERTIFICATES_ENDPOINT)
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(CERTIFICATE_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void createCertificateIncorrectDataReturnsExceptionObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    giftCertificateWithTagsDto.setDuration(-4);
    given()
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(giftCertificateWithTagsDto))
        .header(AUTHORIZATION_HEADER_NAME, adminToken)
        .when()
        .post(ALL_CERTIFICATES_ENDPOINT)
        .then()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void createCertificateUnauthorizedReturnsExceptionObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    given()
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(giftCertificateWithTagsDto))
        .when()
        .post(ALL_CERTIFICATES_ENDPOINT)
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void createCertificateForbiddenReturnsExceptionObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    given()
        .header(AUTHORIZATION_HEADER_NAME, userToken)
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(giftCertificateWithTagsDto))
        .when()
        .post(ALL_CERTIFICATES_ENDPOINT)
        .then()
        .statusCode(HttpStatus.FORBIDDEN.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void findUserCertificatesCorrectRequestReturnsCertificates() {
    given()
        .header(AUTHORIZATION_HEADER_NAME, adminToken)
        .when()
        .get(ALL_CERTIFICATES_ENDPOINT + "?userCertificates")
        .then()
        .statusCode(HttpStatus.OK.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(CERTIFICATE_LIST_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void findUserCertificatesUnauthorizedReturnsExceptionObject() {
    given()
        .when()
        .get(ALL_CERTIFICATES_ENDPOINT + "?userCertificates")
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }


  @Test
  public void findAnyUserCertificatesCorrectRequestExceptionObject() {
    given()
        .header(AUTHORIZATION_HEADER_NAME, adminToken)
        .when()
        .get(ALL_CERTIFICATES_ENDPOINT + "?userId=8")
        .then()
        .statusCode(HttpStatus.OK.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(CERTIFICATE_LIST_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void findAnyUserCertificatesUnauthorizedReturnsExceptionObject() {
    given()
        .when()
        .get(ALL_CERTIFICATES_ENDPOINT + "?userId=8")
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void updateCertificateCorrectDataReturnsUpdatedObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    given()
        .header(AUTHORIZATION_HEADER_NAME, adminToken)
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(giftCertificateWithTagsDto))
        .when()
        .patch(ALL_CERTIFICATES_ENDPOINT + "/47?tagAction=")
        .then()
        .statusCode(HttpStatus.OK.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(CERTIFICATE_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void updateCertificateIncorrectDataReturnsUpdatedObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    giftCertificateWithTagsDto.setPrice(BigDecimal.valueOf(-3));
    given()
        .header(AUTHORIZATION_HEADER_NAME, adminToken)
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(giftCertificateWithTagsDto))
        .when()
        .patch(ALL_CERTIFICATES_ENDPOINT + "/47?tagAction=")
        .then()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void updateCertificateUnauthorizedReturnsExceptionObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    given()
        .header(AUTHORIZATION_HEADER_NAME, "someToken")
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(giftCertificateWithTagsDto))
        .when()
        .patch(ALL_CERTIFICATES_ENDPOINT + "/47?tagAction=")
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void updateCertificateForbiddenReturnsExceptionObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    given()
        .header(AUTHORIZATION_HEADER_NAME, userToken)
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(giftCertificateWithTagsDto))
        .when()
        .patch(ALL_CERTIFICATES_ENDPOINT + "/47?tagAction=")
        .then()
        .statusCode(HttpStatus.FORBIDDEN.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void updatePriceUnauthorizedReturnsExceptionObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    PriceDto priceDto = new PriceDto(BigDecimal.valueOf(5));
    given()
        .header(AUTHORIZATION_HEADER_NAME, "someToken")
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(priceDto))
        .when()
        .put(ALL_CERTIFICATES_ENDPOINT + "/47?price")
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void updatePriceForbiddenReturnsExceptionObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    PriceDto priceDto = new PriceDto(BigDecimal.valueOf(5));
    given()
        .header(AUTHORIZATION_HEADER_NAME, userToken)
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(priceDto))
        .when()
        .put(ALL_CERTIFICATES_ENDPOINT + "/47?price")
        .then()
        .statusCode(HttpStatus.FORBIDDEN.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void updatePriceNegativePriceReturnsExceptionObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    PriceDto priceDto = new PriceDto(BigDecimal.valueOf(-5));
    given()
        .header(AUTHORIZATION_HEADER_NAME, adminToken)
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(priceDto))
        .when()
        .put(ALL_CERTIFICATES_ENDPOINT + "/47?price")
        .then()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void updatePriceNullPriceReturnsExceptionObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    PriceDto priceDto = new PriceDto(BigDecimal.valueOf(-5));
    given()
        .header(AUTHORIZATION_HEADER_NAME, adminToken)
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(priceDto))
        .when()
        .put(ALL_CERTIFICATES_ENDPOINT + "/47?price")
        .then()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void updatePriceCorrectDataReturnsUpdatedObject() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    PriceDto priceDto = new PriceDto(BigDecimal.valueOf(5));
    given()
        .header(AUTHORIZATION_HEADER_NAME, adminToken)
        .contentType(ContentType.JSON)
        .body(objectMapper.writeValueAsString(priceDto))
        .when()
        .put(ALL_CERTIFICATES_ENDPOINT + "/47?price")
        .then()
        .statusCode(HttpStatus.OK.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(CERTIFICATE_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void deleteCertificateUnauthorizedReturnsExceptionObject() {
    given()
        .header(AUTHORIZATION_HEADER_NAME, "someToken")
        .contentType(ContentType.JSON)
        .when()
        .delete(ALL_CERTIFICATES_ENDPOINT + "/9")
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void deleteCertificateForbiddenReturnsExceptionObject() {
    given()
        .header(AUTHORIZATION_HEADER_NAME, userToken)
        .contentType(ContentType.JSON)
        .when()
        .delete(ALL_CERTIFICATES_ENDPOINT + "/9")
        .then()
        .statusCode(HttpStatus.FORBIDDEN.value())
        .assertThat()
        .body(matchesJsonSchemaInClasspath(EXCEPTION_OBJECT_SCHEMA_NAME).using(jsonSchemaFactory));
  }

  @Test
  public void deleteCertificateSuccessfulDeletion() {
    given()
        .header(AUTHORIZATION_HEADER_NAME, adminToken)
        .contentType(ContentType.JSON)
        .when()
        .delete(ALL_CERTIFICATES_ENDPOINT + "/9")
        .then()
        .statusCode(HttpStatus.OK.value());
  }

  @Test
  public void deleteCertificateNotFound() {
    given()
        .header(AUTHORIZATION_HEADER_NAME, adminToken)
        .contentType(ContentType.JSON)
        .when()
        .delete(ALL_CERTIFICATES_ENDPOINT + "/20")
        .then()
        .statusCode(HttpStatus.NOT_FOUND.value());
  }
}