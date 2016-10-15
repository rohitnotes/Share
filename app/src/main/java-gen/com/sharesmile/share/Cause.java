package com.sharesmile.share;

// THIS CODE IS GENERATED BY greenDAO, EDIT ONLY INSIDE THE "KEEP"-SECTIONS

// KEEP INCLUDES - put your custom includes here
// KEEP INCLUDES END
/**
 * Entity mapped to table "CAUSE".
 */
public class Cause {

    private Long id;
    private String causeTitle;
    private String causeDescription;
    private Float conversionRate;
    private Integer minDistance;
    private String causeCategory;
    private String causeBrief;
    private String causeImage;
    private String causeThankyouImage;
    private String share_template;
    private Boolean isActive;
    private Integer sponsorId;
    private String sponsorCompany;
    private String sponsorNgo;
    private String sponsorLogo;
    private Integer partnerId;
    private String partnerCompany;
    private String partnerNgo;
    private String partnerType;
    private Integer order_priority;

    // KEEP FIELDS - put your custom fields here
    // KEEP FIELDS END

    public Cause() {
    }

    public Cause(Long id) {
        this.id = id;
    }

    public Cause(Long id, String causeTitle, String causeDescription, Float conversionRate, Integer minDistance, String causeCategory, String causeBrief, String causeImage, String causeThankyouImage, String share_template, Boolean isActive, Integer sponsorId, String sponsorCompany, String sponsorNgo, String sponsorLogo, Integer partnerId, String partnerCompany, String partnerNgo, String partnerType, Integer order_priority) {
        this.id = id;
        this.causeTitle = causeTitle;
        this.causeDescription = causeDescription;
        this.conversionRate = conversionRate;
        this.minDistance = minDistance;
        this.causeCategory = causeCategory;
        this.causeBrief = causeBrief;
        this.causeImage = causeImage;
        this.causeThankyouImage = causeThankyouImage;
        this.share_template = share_template;
        this.isActive = isActive;
        this.sponsorId = sponsorId;
        this.sponsorCompany = sponsorCompany;
        this.sponsorNgo = sponsorNgo;
        this.sponsorLogo = sponsorLogo;
        this.partnerId = partnerId;
        this.partnerCompany = partnerCompany;
        this.partnerNgo = partnerNgo;
        this.partnerType = partnerType;
        this.order_priority = order_priority;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCauseTitle() {
        return causeTitle;
    }

    public void setCauseTitle(String causeTitle) {
        this.causeTitle = causeTitle;
    }

    public String getCauseDescription() {
        return causeDescription;
    }

    public void setCauseDescription(String causeDescription) {
        this.causeDescription = causeDescription;
    }

    public Float getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Float conversionRate) {
        this.conversionRate = conversionRate;
    }

    public Integer getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(Integer minDistance) {
        this.minDistance = minDistance;
    }

    public String getCauseCategory() {
        return causeCategory;
    }

    public void setCauseCategory(String causeCategory) {
        this.causeCategory = causeCategory;
    }

    public String getCauseBrief() {
        return causeBrief;
    }

    public void setCauseBrief(String causeBrief) {
        this.causeBrief = causeBrief;
    }

    public String getCauseImage() {
        return causeImage;
    }

    public void setCauseImage(String causeImage) {
        this.causeImage = causeImage;
    }

    public String getCauseThankyouImage() {
        return causeThankyouImage;
    }

    public void setCauseThankyouImage(String causeThankyouImage) {
        this.causeThankyouImage = causeThankyouImage;
    }

    public String getShare_template() {
        return share_template;
    }

    public void setShare_template(String share_template) {
        this.share_template = share_template;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getSponsorId() {
        return sponsorId;
    }

    public void setSponsorId(Integer sponsorId) {
        this.sponsorId = sponsorId;
    }

    public String getSponsorCompany() {
        return sponsorCompany;
    }

    public void setSponsorCompany(String sponsorCompany) {
        this.sponsorCompany = sponsorCompany;
    }

    public String getSponsorNgo() {
        return sponsorNgo;
    }

    public void setSponsorNgo(String sponsorNgo) {
        this.sponsorNgo = sponsorNgo;
    }

    public String getSponsorLogo() {
        return sponsorLogo;
    }

    public void setSponsorLogo(String sponsorLogo) {
        this.sponsorLogo = sponsorLogo;
    }

    public Integer getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Integer partnerId) {
        this.partnerId = partnerId;
    }

    public String getPartnerCompany() {
        return partnerCompany;
    }

    public void setPartnerCompany(String partnerCompany) {
        this.partnerCompany = partnerCompany;
    }

    public String getPartnerNgo() {
        return partnerNgo;
    }

    public void setPartnerNgo(String partnerNgo) {
        this.partnerNgo = partnerNgo;
    }

    public String getPartnerType() {
        return partnerType;
    }

    public void setPartnerType(String partnerType) {
        this.partnerType = partnerType;
    }

    public Integer getOrder_priority() {
        return order_priority;
    }

    public void setOrder_priority(Integer order_priority) {
        this.order_priority = order_priority;
    }

    // KEEP METHODS - put your custom methods here
    // KEEP METHODS END

}
