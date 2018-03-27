package com.adyen.mirakl.service;


import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import com.adyen.Util.Util;
import com.adyen.mirakl.AdyenMiraklConnectorApp;
import com.adyen.mirakl.domain.AdyenPayoutError;
import com.adyen.mirakl.repository.AdyenPayoutErrorRepository;
import com.adyen.model.Amount;
import com.adyen.model.marketpay.PayoutAccountHolderRequest;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = AdyenMiraklConnectorApp.class)
@Transactional
public class PayoutServiceIntegrationTest {

    @Autowired
    private PayoutService payoutService;

    @Autowired
    private AdyenPayoutErrorRepository adyenPayoutErrorRepository;

    @Before
    public void removeExistingTestAdyenPayoutErrors() {
        final List<AdyenPayoutError> all = adyenPayoutErrorRepository.findAll();
        adyenPayoutErrorRepository.delete(all);
        adyenPayoutErrorRepository.flush();
    }

    @Test
    public void testStoreAccountHolderRequest() {
        PayoutAccountHolderRequest payoutAccountHolderRequest = new PayoutAccountHolderRequest();
        payoutAccountHolderRequest.setAccountCode("accountCode");
        payoutAccountHolderRequest.setBankAccountUUID("bankAccountUUID");
        payoutAccountHolderRequest.setAccountHolderCode("accountHolderCode");
        payoutAccountHolderRequest.setDescription("description");
        Amount adyenAmount = Util.createAmount("100", "EUR");
        payoutAccountHolderRequest.setAmount(adyenAmount);

        payoutService.storeAdyenPayoutError(payoutAccountHolderRequest, null, null);

        // check if it stores it properly
        final List<AdyenPayoutError> all = adyenPayoutErrorRepository.findAll();
        Assertions.assertThat(all.get(0).getRawRequest()).isEqualTo(PayoutService.GSON.toJson(payoutAccountHolderRequest));
    }
}
