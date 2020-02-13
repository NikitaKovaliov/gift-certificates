package com.epam.esm.response;

import com.epam.esm.dto.GiftCertificateWithTags;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "giftCertificates")
@XmlAccessorType(XmlAccessType.FIELD)
public class GiftCertificatesList {

  @XmlElement(name = "giftCertificate")
  private List<GiftCertificateWithTags> giftCertificatesWithTags;

  private GiftCertificatesList() {
  }

  public GiftCertificatesList(
      List<GiftCertificateWithTags> giftCertificatesWithTags) {
    this.giftCertificatesWithTags = giftCertificatesWithTags;
  }

  public List<GiftCertificateWithTags> getGiftCertificatesWithTags() {
    return giftCertificatesWithTags;
  }
}