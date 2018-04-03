package com.adyen.mirakl.service;

import java.time.ZonedDateTime;
import java.util.*;
import javax.annotation.Resource;

import com.adyen.mirakl.repository.ShareholderMappingRepository;
import com.mirakl.client.mmp.request.shop.document.MiraklDeleteShopDocumentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.adyen.mirakl.config.Constants;
import com.adyen.mirakl.service.util.GetShopDocumentsRequest;
import com.adyen.model.marketpay.DocumentDetail;
import com.adyen.model.marketpay.GetAccountHolderRequest;
import com.adyen.model.marketpay.GetAccountHolderResponse;
import com.adyen.model.marketpay.UploadDocumentRequest;
import com.adyen.model.marketpay.UploadDocumentResponse;
import com.adyen.service.Account;
import com.adyen.service.exception.ApiException;
import com.mirakl.client.mmp.domain.common.FileWrapper;
import com.mirakl.client.mmp.domain.shop.document.MiraklShopDocument;
import com.mirakl.client.mmp.operator.core.MiraklMarketplacePlatformOperatorApiClient;
import com.mirakl.client.mmp.request.shop.document.MiraklDownloadShopsDocumentsRequest;
import static com.google.common.io.Files.toByteArray;

@Service
@Transactional
public class DocService {

    private final Logger log = LoggerFactory.getLogger(DocService.class);

    @Resource
    private MiraklMarketplacePlatformOperatorApiClient miraklMarketplacePlatformOperatorApiClient;

    @Resource
    private Account adyenAccountService;

    @Resource
    private DeltaService deltaService;

    @Resource
    private UboService uboService;

    @Resource
    private ShareholderMappingRepository shareholderMappingRepository;

    /**
     * Calling S30, S31, GetAccountHolder and UploadDocument to upload bankproof documents to Adyen
     */
    public void processUpdatedDocuments() {
        final ZonedDateTime beforeProcessing = ZonedDateTime.now();

        List<MiraklShopDocument> miraklShopDocumentList = retrieveUpdatedDocs();
        for (MiraklShopDocument document : miraklShopDocumentList) {
            if (document.getTypeCode().equals(Constants.BANKPROOF)) {
                updateDocument(document, DocumentDetail.DocumentTypeEnum.BANK_STATEMENT);
            }
        }
        uboService.extractUboDocuments(miraklShopDocumentList).forEach(uboDocumentDTO -> {
            updateDocument(uboDocumentDTO.getMiraklShopDocument(), uboDocumentDTO.getDocumentTypeEnum(), uboDocumentDTO.getShareholderCode());
        });
        deltaService.updateDocumentDelta(beforeProcessing);
    }

    private void updateDocument(final MiraklShopDocument document, DocumentDetail.DocumentTypeEnum type, String shareholderCode) {
        FileWrapper fileWrapper = downloadSelectedDocument(document);
        try {
            uploadDocumentToAdyen(type, fileWrapper, document.getShopId(), shareholderCode);
        } catch (ApiException e) {
            log.error("MarketPay Api Exception: {}", e.getError(), e);
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage(), e);
        }
    }

    private void updateDocument(final MiraklShopDocument document, DocumentDetail.DocumentTypeEnum type) {
        updateDocument(document, type, null);
    }

    /**
     * Retrieve documents from Mirakl(S30)
     */
    private List<MiraklShopDocument> retrieveUpdatedDocs() {
        //To replace with MiraklGetShopDocumentsRequest when fixed
        GetShopDocumentsRequest request = new GetShopDocumentsRequest();
        request.setUpdatedSince(deltaService.getDocumentDelta());
        log.debug("getShopDocuments request since: " + request.getUpdatedSince());
        return miraklMarketplacePlatformOperatorApiClient.getShopDocuments(request);
    }

    /**
     * Download one document from Mirakl(S31), it will always be a single document, this prevents mirakl from returning a zip file, which is not supported on Adyen
     */
    private FileWrapper downloadSelectedDocument(MiraklShopDocument document) {
        MiraklDownloadShopsDocumentsRequest request = new MiraklDownloadShopsDocumentsRequest();
        List<String> documentIds = new ArrayList<>();
        documentIds.add(document.getId());
        request.setDocumentIds(documentIds);
        return miraklMarketplacePlatformOperatorApiClient.downloadShopsDocuments(request);
    }

    /**
     * Encode document retrieved from Mirakl in Base64 and push it to Adyen, if the document type is BANK_STATEMENT/adyen-bankproof, a bank account is needed
     */
    private void uploadDocumentToAdyen(DocumentDetail.DocumentTypeEnum documentType, FileWrapper fileWrapper, String shopId, String shareholderCode) throws Exception {
        UploadDocumentRequest request = new UploadDocumentRequest();
        request.setAccountHolderCode(shopId);
        request.setShareholderCode(shareholderCode);

        //Encode file Base64
        byte[] bytes = toByteArray(fileWrapper.getFile());
        Base64.Encoder encoder = Base64.getEncoder();
        String encoded = encoder.encodeToString(bytes);
        request.setDocumentContent(encoded);
        //If document is a bank statement, the bankaccountUUID is required
        if (documentType.equals(DocumentDetail.DocumentTypeEnum.BANK_STATEMENT)) {
            String UUID = retrieveBankAccountUUID(shopId);
            if (UUID != null && ! UUID.isEmpty()) {
                request.setBankAccountUUID(UUID);
            } else {
                throw new IllegalStateException("No bank accounts are associated with this shop, a bank account is needed to upload a bank statement");
            }
        }
        DocumentDetail documentDetail = new DocumentDetail();
        documentDetail.setFilename(fileWrapper.getFilename());
        documentDetail.setDocumentType(documentType);
        documentDetail.setShareholderCode(shareholderCode);
        documentDetail.setAccountHolderCode(shopId);
        request.setDocumentDetail(documentDetail);
        UploadDocumentResponse response = adyenAccountService.uploadDocument(request);
        log.debug("Account holder code: " + shareholderCode);
        log.debug("Shop ID: " + shopId);
        log.debug("DocumentType: " + documentType);
        log.debug("UploadDocumentResponse: ", response.toString());
    }

    /**
     * Call to Adyen to retrieve the (first)bankaccountUUID
     */
    private String retrieveBankAccountUUID(String shopID) throws Exception {
        GetAccountHolderRequest getAccountHolderRequest = new GetAccountHolderRequest();
        getAccountHolderRequest.setAccountHolderCode(shopID);
        GetAccountHolderResponse getAccountHolderResponse = adyenAccountService.getAccountHolder(getAccountHolderRequest);
        if (! getAccountHolderResponse.getAccountHolderDetails().getBankAccountDetails().isEmpty()) {
            return getAccountHolderResponse.getAccountHolderDetails().getBankAccountDetails().get(0).getBankAccountUUID();
        }
        return null;
    }

    public void removeMiraklMediaForShareHolder(final String shareHolderCode) {

//        shareholderMappingRepository.findOneByMiraklShopIdAndMiraklUboNumber();

        final MiraklDeleteShopDocumentRequest request = new MiraklDeleteShopDocumentRequest("");
        miraklMarketplacePlatformOperatorApiClient.deleteShopDocument(request);

    }
}
