package com.adyen.mirakl.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A AdyenPayoutError.
 */
@Entity
@Table(name = "adyen_payout_error")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class AdyenPayoutError implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "raw_request")
    private String rawRequest;

    @Lob
    @Column(name = "raw_response")
    private String rawResponse;

    @Lob
    @Column(name = "raw_subscription_request")
    private String rawSubscriptionRequest;

    @Column(name = "account_holder_code")
    private String accountHolderCode;

    @Column(name = "retry")
    private Integer retry;

    @Column(name = "processing")
    private Boolean processing;

    @Column(name = "created_at")
    @CreationTimestamp
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private ZonedDateTime updatedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRawRequest() {
        return rawRequest;
    }

    public AdyenPayoutError rawRequest(String rawRequest) {
        this.rawRequest = rawRequest;
        return this;
    }

    public void setRawRequest(String rawRequest) {
        this.rawRequest = rawRequest;
    }

    public String getRawSubscriptionRequest() {
        return rawSubscriptionRequest;
    }

    public AdyenPayoutError rawSubscriptionRequest(String rawSubscriptionRequest) {
        this.rawSubscriptionRequest = rawSubscriptionRequest;
        return this;
    }

    public void setRawSubscriptionRequest(String rawSubscriptionRequest) {
        this.rawSubscriptionRequest = rawSubscriptionRequest;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public AdyenPayoutError rawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
        return this;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public String getAccountHolderCode() {
        return accountHolderCode;
    }

    public AdyenPayoutError accountHolderCode(String accountHolderCode) {
        this.accountHolderCode = accountHolderCode;
        return this;
    }

    public void setAccountHolderCode(String accountHolderCode) {
        this.accountHolderCode = accountHolderCode;
    }

    public Integer getRetry() {
        return retry;
    }

    public AdyenPayoutError retry(Integer retry) {
        this.retry = retry;
        return this;
    }

    public void setRetry(Integer retry) {
        this.retry = retry;
    }

    public Boolean isProcessing() {
        return processing;
    }

    public AdyenPayoutError processing(Boolean processing) {
        this.processing = processing;
        return this;
    }

    public void setProcessing(Boolean processing) {
        this.processing = processing;
    }


    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here, do not remove

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AdyenPayoutError adyenPayoutError = (AdyenPayoutError) o;
        if (adyenPayoutError.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), adyenPayoutError.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "AdyenPayoutError{" +
            "id=" + getId() +
            ", rawRequest='" + getRawRequest() + "'" +
            ", rawSubscriptionRequest='" + getRawSubscriptionRequest() + "'" +
            ", rawResponse='" + getRawResponse() + "'" +
            ", accountHolderCode='" + getAccountHolderCode() + "'" +
            ", retry=" + getRetry() +
            ", processing='" + isProcessing() + "'" +
            ", createdAt='" + createdAt + "'" +
            ", updatedAt='" + updatedAt + "'" +
            "}";
    }
}
