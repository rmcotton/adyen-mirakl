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
public class RetryPayoutServiceTest {

    @Autowired
    private PayoutService payoutService;

    @Autowired
    private AdyenPayoutErrorRepository adyenPayoutErrorRepository;

    @Autowired
    private RetryPayoutService retryPayoutService;

    @Before
    public void removeExistingTestAdyenPayoutErrors() {
        final List<AdyenPayoutError> all = adyenPayoutErrorRepository.findAll();
        adyenPayoutErrorRepository.delete(all);
        adyenPayoutErrorRepository.flush();
    }

    @Test
    public void testRetryFailedPayouts() {

        // add 2 failed payouts
        PayoutAccountHolderRequest payoutAccountHolderRequestFirst = createFailedPayout("1");
        payoutService.storeAdyenPayoutError(payoutAccountHolderRequestFirst, null, null);

        PayoutAccountHolderRequest payoutAccountHolderRequestSecond = createFailedPayout("2");
        payoutService.storeAdyenPayoutError(payoutAccountHolderRequestSecond, null, null);

        // retry failed payouts
        retryPayoutService.retryFailedPayouts();

        // check if it stores it properly
        List<AdyenPayoutError> all = adyenPayoutErrorRepository.findAll();
        Assertions.assertThat(all.get(0).getRawRequest()).isEqualTo(PayoutService.GSON.toJson(payoutAccountHolderRequestFirst));
        Assertions.assertThat(all.get(0).getRetry()).isEqualTo(1);

        Assertions.assertThat(all.get(1).getRawRequest()).isEqualTo(PayoutService.GSON.toJson(payoutAccountHolderRequestSecond));
        Assertions.assertThat(all.get(1).getRetry()).isEqualTo(1);

        // retry failed payouts
        retryPayoutService.retryFailedPayouts();
        all = adyenPayoutErrorRepository.findAll();
        Assertions.assertThat(all.get(0).getRawRequest()).isEqualTo(PayoutService.GSON.toJson(payoutAccountHolderRequestFirst));
        Assertions.assertThat(all.get(0).getRetry()).isEqualTo(2);

        Assertions.assertThat(all.get(1).getRawRequest()).isEqualTo(PayoutService.GSON.toJson(payoutAccountHolderRequestSecond));
        Assertions.assertThat(all.get(1).getRetry()).isEqualTo(2);

        // if retry is set to 2 then there should be no failed payouts left
        List<AdyenPayoutError> failedPayouts = adyenPayoutErrorRepository.findByRetry(2);
        Assertions.assertThat(failedPayouts.size()).isEqualTo(0);
    }

    public void retry() {
        String accountHolderCode = "1000";
        // add 2 failed payouts
        PayoutAccountHolderRequest payoutAccountHolderRequestFirst = createFailedPayout("", accountHolderCode);
        payoutService.storeAdyenPayoutError(payoutAccountHolderRequestFirst, null, null);
        retryPayoutService.retryFailedPayoutsForAccountHolder(accountHolderCode);

        List<AdyenPayoutError> all = adyenPayoutErrorRepository.findByAccountHolderCode(accountHolderCode);
        Assertions.assertThat(all.get(0).getRawRequest()).isEqualTo(PayoutService.GSON.toJson(payoutAccountHolderRequestFirst));
        Assertions.assertThat(all.get(0).getRetry()).isEqualTo(2);
    }

    public PayoutAccountHolderRequest createFailedPayout(String prefix) {
        return createFailedPayout(prefix, "_accountCode");
    }

    public PayoutAccountHolderRequest createFailedPayout(String prefix, String accountHolderCode) {
        PayoutAccountHolderRequest payoutAccountHolderRequest = new PayoutAccountHolderRequest();
        payoutAccountHolderRequest.setAccountCode(prefix + accountHolderCode);
        payoutAccountHolderRequest.setBankAccountUUID(prefix + "_bankAccountUUID");
        payoutAccountHolderRequest.setAccountHolderCode(prefix + "_accountHolderCode");
        payoutAccountHolderRequest.setDescription(prefix + "_description");
        Amount adyenAmount = Util.createAmount("100", "EUR");
        payoutAccountHolderRequest.setAmount(adyenAmount);
        return payoutAccountHolderRequest;
    }
}
