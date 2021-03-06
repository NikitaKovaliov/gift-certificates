package com.epam.esm.controller;

import com.epam.esm.dto.EntityListDto;
import com.epam.esm.dto.GiftCertificatePriceDto;
import com.epam.esm.dto.GiftCertificateUpdateDto;
import com.epam.esm.dto.GiftCertificateWithTagsDto;
import com.epam.esm.exception.ExceptionType;
import com.epam.esm.exception.ServerException;
import com.epam.esm.security.SecurityUserDetails;
import com.epam.esm.service.GiftCertificateService;
import com.epam.esm.validation.GiftCertificateValidator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Gift certificate controller. The class used for processing GiftCertificate-related requests.
 */
@RestController
@RequestMapping(value = "/api/certificates", produces = MediaType.APPLICATION_JSON_VALUE)
public class GiftCertificateController {

  private final GiftCertificateService certificateService;

  /**
   * Instantiates a new Gift certificate controller.
   *
   * @param certificateService the certificate service
   */
  @Autowired
  public GiftCertificateController(GiftCertificateService certificateService) {
    this.certificateService = certificateService;
  }

  /**
   * Create gift certificate with tags returns service response.
   *
   * @param giftCertificateWithTagsDto the certificate with tags
   * @return the service response
   */
  @Secured("ROLE_ADMIN")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<GiftCertificateWithTagsDto> createCertificate(
      @RequestBody GiftCertificateWithTagsDto giftCertificateWithTagsDto) {
    if (!GiftCertificateValidator.isValidGiftCertificateValuesForCreate(giftCertificateWithTagsDto)) {
      throw new ServerException(ExceptionType.INCORRECT_INPUT_DATA);
    }
    return ResponseEntity
        .status(HttpStatus.CREATED.value())
        .body(certificateService.create(giftCertificateWithTagsDto));
  }

  /**
   * Find by id service returns response.
   *
   * @param id the id of requested resource
   * @return the service response
   */
  @GetMapping(value = "/{id}")
  public ResponseEntity<GiftCertificateWithTagsDto> findCertificateById(@PathVariable Long id) {
    return ResponseEntity
        .status(HttpStatus.OK.value())
        .body(certificateService.findById(id));
  }

  /**
   * Find all service returns response.
   *
   * @param parameters the parameters
   * @return the service response
   */
  @GetMapping()
  public ResponseEntity<EntityListDto<GiftCertificateWithTagsDto>> findAllCertificates(
      @RequestParam Map<String, String> parameters, @RequestParam(value = "tag", required = false) List<String> tags) {
    return ResponseEntity.status(HttpStatus.OK.value())
        .body(certificateService.findByCriteria(parameters, tags));
  }

  /**
   * Find user certificates response entity.
   *
   * @param userDetails the user details
   * @param parameters  the parameters
   * @return the response entity
   */
  @Secured({"ROLE_ADMIN", "ROLE_USER"})
  @GetMapping(params = "userCertificates")
  public ResponseEntity<EntityListDto<GiftCertificateWithTagsDto>> findUserCertificates(
      @AuthenticationPrincipal SecurityUserDetails userDetails, @RequestParam Map<String, String> parameters) {
    return ResponseEntity
        .status(HttpStatus.OK.value())
        .body(certificateService.findByUserId(userDetails.getId(), parameters));
  }

  /**
   * Find any user certificates response entity.
   *
   * @param userId     the user id
   * @param parameters the parameters
   * @return the response entity
   */
  @Secured("ROLE_ADMIN")
  @GetMapping(params = "userId")
  public ResponseEntity<EntityListDto<GiftCertificateWithTagsDto>> findAnyUserCertificates(@RequestParam Long userId,
      @RequestParam Map<String, String> parameters) {
    return ResponseEntity
        .status(HttpStatus.OK.value())
        .body(certificateService.findByUserId(userId, parameters));
  }

  /**
   * Update certificate with tags returns service response.
   *
   * @param id                       the id of updating resource
   * @param giftCertificateUpdateDto the gift certificate update dto
   * @return the service response
   */
  @Secured("ROLE_ADMIN")
  @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<GiftCertificateWithTagsDto> updateCertificate(@PathVariable Long id,
      @RequestBody GiftCertificateUpdateDto giftCertificateUpdateDto) {
    if (!GiftCertificateValidator.isValidGiftCertificateValuesForUpdate(giftCertificateUpdateDto)) {
      throw new ServerException(ExceptionType.INCORRECT_INPUT_DATA);
    }
    return ResponseEntity.status(HttpStatus.OK).body(certificateService.update(id, giftCertificateUpdateDto));
  }

  /**
   * Update price response entity.
   *
   * @param id    the id
   * @param price the price
   * @return the response entity
   */
  @Secured("ROLE_ADMIN")
  @PutMapping(value = "/{id}", params = "price", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<GiftCertificateWithTagsDto> updatePrice(@PathVariable Long id,
      @RequestBody GiftCertificatePriceDto price) {
    if (price.getPrice() == null || !GiftCertificateValidator.isValidCertificatePrice(price.getPrice())) {
      throw new ServerException(ExceptionType.INCORRECT_INPUT_DATA);
    }
    return ResponseEntity.status(HttpStatus.OK.value()).body(certificateService.updatePrice(id, price.getPrice()));
  }

  /**
   * Delete certificate by id returns service response.
   *
   * @param id the id of deleting resource
   * @return the service response
   */
  @Secured("ROLE_ADMIN")
  @DeleteMapping(value = "/{id}")
  public ResponseEntity<Void> deleteCertificate(@PathVariable Long id) {
    int numberOfDeletedRows = certificateService.delete(id);
    if (numberOfDeletedRows < 1) {
      throw new ServerException(ExceptionType.RESOURCE_NOT_FOUND);
    }
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}