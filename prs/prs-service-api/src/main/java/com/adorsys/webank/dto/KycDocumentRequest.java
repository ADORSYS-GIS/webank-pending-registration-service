package com.adorsys.webank.dto;

public class KycDocumentRequest {
    private String frontId;
    private String backId;
    private String taxId;
    private String selfPic;

    public KycDocumentRequest(String frontId, String backId, String taxId, String selfPic) {
        this.frontId = frontId;
        this.backId = backId;
        this.taxId = taxId;
        this.selfPic = selfPic;
    }

    public String getFrontId() {
        return frontId;
    }

    public void setFrontId(String frontId) {
        this.frontId = frontId;
    }

    public String getBackId() {
        return backId;
    }

    public void setBackId(String backId) {
        this.backId = backId;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getSelfPic() {
        return selfPic;
    }

    public void setSelfPic(String selfPic) {
        this.selfPic = selfPic;
    }
}
