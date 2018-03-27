package com.adyen.mirakl.service;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.adyen.Util.Util;
import com.adyen.mirakl.domain.AdyenPayoutError;
import com.adyen.mirakl.repository.AdyenPayoutErrorRepository;
import com.adyen.model.Amount;
import com.adyen.model.marketpay.BankAccountDetail;
import com.adyen.model.marketpay.GetAccountHolderRequest;
import com.adyen.model.marketpay.GetAccountHolderResponse;
import com.adyen.model.marketpay.PayoutAccountHolderRequest;
import com.adyen.model.marketpay.PayoutAccountHolderResponse;
import com.adyen.model.marketpay.TransferFundsRequest;
import com.adyen.model.marketpay.TransferFundsResponse;
import com.adyen.service.Account;
import com.adyen.service.Fund;
import com.adyen.service.exception.ApiException;
import com.google.gson.Gson;

@Service
@Transactional
public class PayoutService {

    private final Logger log = LoggerFactory.getLogger(PayoutService.class);

    @Resource
    private Account adyenAccountService;

    @Resource
    private Fund adyenFundService;

    @Resource
    private AdyenPayoutErrorRepository adyenPayoutErrorRepository;

    @Value("${payoutService.subscriptionTransferCode}")
    private String subscriptionTransferCode;

    @Value("${payoutService.liableAccountCode}")
    private String liableAccountCode;

    protected final static Gson GSON = new Gson();


    public void parseMiraklCsv(String csvData) throws IOException {
        Iterable<CSVRecord> records = null;
        records = CSVParser.parse(csvData, CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(';'));
        for (CSVRecord record : records) {
            String accountHolderCode = record.get("shop-id");
            String amount = record.get("transfer-amount");
            String currency = record.get("currency-iso-code");
            String iban = record.get("payment-info-ibantype-iban");
            String invoiceNumber = record.get("invoice-number");
            String shopName = record.get("shop-name");
            String description = "Payout shop " + shopName + " (" + accountHolderCode + "), " + "Invoice number: " + invoiceNumber;

            PayoutAccountHolderRequest payoutAccountHolderRequest = null;
            PayoutAccountHolderResponse payoutAccountHolderResponse = null;

            TransferFundsRequest transferFundsRequest = null;
            String subscriptionAmount = record.get("subscription-amount");
            try {
                //Call Adyen to retrieve the accountCode from the accountHolderCode
                GetAccountHolderResponse accountHolderResponse = getAccountHolderResponse(accountHolderCode);

                payoutAccountHolderRequest = createPayoutAccountHolderRequest(accountHolderResponse, amount, currency, iban, description);
                if (! subscriptionAmount.isEmpty() && ! subscriptionAmount.equals("0")) {
                    transferFundsRequest = createTransferFundsSubscription(accountHolderResponse, subscriptionAmount, currency);
                    TransferFundsResponse transferFundsResponse = adyenFundService.transferFunds(transferFundsRequest);
                    log.info("Subscription submitted for accountHolder: [{}] + Response: [{}]", accountHolderCode, transferFundsResponse);
                    transferFundsRequest = null;
                }
                payoutAccountHolderResponse = adyenFundService.payoutAccountHolder(payoutAccountHolderRequest);
                log.info("Payout submitted for accountHolder: [{}] + Psp ref: [{}]", accountHolderCode, payoutAccountHolderResponse.getPspReference());
            } catch (ApiException e) {
                log.error("MP exception: " + e.getError(), e);
                storeAdyenPayoutError(payoutAccountHolderRequest, payoutAccountHolderResponse, transferFundsRequest);
            } catch (Exception e) {
                log.error("MP exception: " + e.getMessage(), e);
                storeAdyenPayoutError(payoutAccountHolderRequest, payoutAccountHolderResponse, transferFundsRequest);
            }
        }

    }

    /**
     * Store Payout request into database so we can do retries
     */
    protected void storeAdyenPayoutError(PayoutAccountHolderRequest payoutAccountHolderRequest, PayoutAccountHolderResponse payoutAccountHolderResponse, TransferFundsRequest transferFundsRequest) {
        if (payoutAccountHolderRequest != null) {
            String rawRequest = GSON.toJson(payoutAccountHolderRequest);
            AdyenPayoutError adyenPayoutError = new AdyenPayoutError();

            adyenPayoutError.setAccountHolderCode(payoutAccountHolderRequest.getAccountHolderCode());
            adyenPayoutError.setRawRequest(rawRequest);


            if (payoutAccountHolderResponse != null) {
                String rawResponse = GSON.toJson(payoutAccountHolderResponse);
                adyenPayoutError.setRawResponse(rawResponse);
            }

            if (transferFundsRequest != null) {
                String subscriptionRawRequest = GSON.toJson(transferFundsRequest);
                adyenPayoutError.setRawSubscriptionRequest(subscriptionRawRequest);
            }


            adyenPayoutError.setProcessing(false);
            adyenPayoutError.setRetry(0);
            adyenPayoutErrorRepository.save(adyenPayoutError);
        }
    }

    protected PayoutAccountHolderRequest createPayoutAccountHolderRequest(GetAccountHolderResponse accountHolderResponse,
                                                                          String amount,
                                                                          String currency,
                                                                          String iban,
                                                                          String description) throws Exception {

        //Retrieve the bankAccountUUID from Adyen matching to the iban provided from Mirakl
        String bankAccountUUID = getBankAccountUUID(accountHolderResponse, iban);
        PayoutAccountHolderRequest payoutAccountHolderRequest = new PayoutAccountHolderRequest();
        payoutAccountHolderRequest.setAccountCode(getAccountCode(accountHolderResponse));
        payoutAccountHolderRequest.setBankAccountUUID(bankAccountUUID);
        payoutAccountHolderRequest.setAccountHolderCode(accountHolderResponse.getAccountHolderCode());
        payoutAccountHolderRequest.setDescription(description);
        Amount adyenAmount = Util.createAmount(amount, currency);
        payoutAccountHolderRequest.setAmount(adyenAmount);

        return payoutAccountHolderRequest;
    }

    protected GetAccountHolderResponse getAccountHolderResponse(String accountHolderCode) throws Exception {
        GetAccountHolderRequest getAccountHolderRequest = new GetAccountHolderRequest();
        getAccountHolderRequest.setAccountHolderCode(accountHolderCode);
        return adyenAccountService.getAccountHolder(getAccountHolderRequest);
    }

    private String getAccountCode(GetAccountHolderResponse accountHolderResponse) {
        return accountHolderResponse.getAccounts().get(0).getAccountCode();
    }

    protected String getBankAccountUUID(GetAccountHolderResponse accountHolderResponse, String iban) {
        //Iban Check
        List<BankAccountDetail> bankAccountDetailList = accountHolderResponse.getAccountHolderDetails().getBankAccountDetails();
        if (! bankAccountDetailList.isEmpty()) {
            for (BankAccountDetail bankAccountDetail : bankAccountDetailList) {
                if (bankAccountDetail.getIban().equals(iban)) {
                    return bankAccountDetail.getBankAccountUUID();
                }
            }
        }
        throw new IllegalStateException("No matching Iban between Mirakl and Adyen platforms.");
    }

    protected TransferFundsRequest createTransferFundsSubscription(GetAccountHolderResponse accountHolderResponse, String commissionFee, String currency) throws Exception {

        TransferFundsRequest transferFundsRequest = new TransferFundsRequest();
        Amount adyenAmount = Util.createAmount(commissionFee, currency);

        transferFundsRequest.setAmount(adyenAmount);

        transferFundsRequest.setSourceAccountCode(getAccountCode(accountHolderResponse));
        transferFundsRequest.setDestinationAccountCode(liableAccountCode);

        transferFundsRequest.setTransferCode(subscriptionTransferCode);
        return transferFundsRequest;
    }


}
