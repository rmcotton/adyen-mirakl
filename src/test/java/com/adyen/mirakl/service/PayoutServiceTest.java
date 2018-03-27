package com.adyen.mirakl.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import com.adyen.model.marketpay.AccountHolderDetails;
import com.adyen.model.marketpay.BankAccountDetail;
import com.adyen.model.marketpay.GetAccountHolderResponse;
import com.adyen.model.marketpay.PayoutAccountHolderRequest;
import com.adyen.model.marketpay.TransferFundsRequest;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class PayoutServiceTest {

    @InjectMocks
    private PayoutService payoutService;

    @Test
    public void testGetBankAccountUUID() {
        GetAccountHolderResponse getAccountHolderResponse = getResponseWithBankDetails();
        assertEquals("2a421c72-ead7-4ad3-8741-80a0aebb8758", payoutService.getBankAccountUUID(getAccountHolderResponse, "GB29NWBK60161331926819"));
    }

    @Test(expected = RuntimeException.class)
    public void testGetBankAccountUUIDException() {
        GetAccountHolderResponse getAccountHolderResponse = getResponseWithBankDetails();
        payoutService.getBankAccountUUID(getAccountHolderResponse, "invalidiban");
    }

    @Test
    public void testPayout() throws Exception {
        GetAccountHolderResponse getAccountHolderResponse = getResponseWithBankDetails();
        PayoutAccountHolderRequest request = payoutService.createPayoutAccountHolderRequest(getAccountHolderResponse, "10.25", "EUR", "GB29NWBK60161331926819", "Description");
        assertEquals("2a421c72-ead7-4ad3-8741-80a0aebb8758", request.getBankAccountUUID());
        assertEquals("2000", request.getAccountHolderCode());
        assertEquals("123456", request.getAccountCode());
        assertEquals(1025L, (long) request.getAmount().getValue());
        assertEquals("EUR", request.getAmount().getCurrency());
        assertEquals("Description", request.getDescription());
    }

    public GetAccountHolderResponse getResponseWithBankDetails() {
        BankAccountDetail bankAccountDetail = new BankAccountDetail();
        bankAccountDetail.setBankAccountUUID("7ea30ecb-8ec9-4012-9c6c-c1abf7c4f90a");
        bankAccountDetail.setIban("GB26MIDL40051512345674");
        BankAccountDetail bankAccountDetail1 = new BankAccountDetail();
        bankAccountDetail1.setBankAccountUUID("2a421c72-ead7-4ad3-8741-80a0aebb8758");
        bankAccountDetail1.setIban("GB29NWBK60161331926819");
        AccountHolderDetails accountHolderDetails = new AccountHolderDetails();
        accountHolderDetails.addBankAccountDetail(bankAccountDetail);
        accountHolderDetails.addBankAccountDetail(bankAccountDetail1);
        GetAccountHolderResponse getAccountHolderResponse = new GetAccountHolderResponse();
        getAccountHolderResponse.setAccountHolderDetails(accountHolderDetails);
        getAccountHolderResponse.setAccountHolderCode("2000");

        com.adyen.model.marketpay.Account marketpayAccount = new com.adyen.model.marketpay.Account();
        marketpayAccount.setAccountCode("123456");
        getAccountHolderResponse.addAccount(marketpayAccount);

        return getAccountHolderResponse;
    }

    @Test
    public void testCreateTransferFundsSubscription() throws Exception {
        GetAccountHolderResponse getAccountHolderResponse = getResponseWithBankDetails();
        TransferFundsRequest transferFundsRequest = payoutService.createTransferFundsSubscription(getAccountHolderResponse, "12.34", "EUR");
        assertEquals(1234L, (long) transferFundsRequest.getAmount().getValue());
        assertEquals("EUR", transferFundsRequest.getAmount().getCurrency());
    }


}
