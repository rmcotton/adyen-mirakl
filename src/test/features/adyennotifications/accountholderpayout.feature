@cucumber
Feature: Payout notifications for seller payout

    @ADY-9 @ADY-86
    Scenario: Successful payout notification is received upon successful seller payout
        Given a shop has been created in Mirakl for an Individual with mandatory KYC data
            | city   | bank name | iban                   | bankOwnerName | lastName |
            | PASSED | testBank  | GB26TEST40051512347366 | TestData      | TestData |
        And we process the data and push to Adyen
        And a passport has been uploaded to Adyen
        And the accountHolders balance is increased
            | transfer amount |
            | 9900            |
        And the PayoutState allowPayout changes from false to true
        When a payment voucher is sent to the Connector
            | paymentVoucher                  |
            | PaymentVoucher_PayoutShop01.csv |
        Then adyen will send the ACCOUNT_HOLDER_PAYOUT notification
            | currency | amount | statusCode | iban                   |
            | EUR      | 2914.0 | Initiated  | GB26TEST40051512347366 |

    @ADY-9 @ADY-86
    Scenario: Failure status is received for payout notification
        Given a shop has been created in Mirakl for an Individual with mandatory KYC data
            | city   | bank name | iban                   | bankOwnerName | lastName |
            | PASSED | testBank  | GB26TEST40051512347366 | TestData      | TestData |
        And we process the data and push to Adyen
        When a payment voucher is sent to the Connector
            | paymentVoucher                  |
            | PaymentVoucher_PayoutShop02.csv |
        Then adyen will send the ACCOUNT_HOLDER_PAYOUT notification with status
            | statusCode | message                                           |
            | Failed     | There is not enough balance available for account |

    @ADY-34 @bug @ADY-111
    Scenario: The connector forces payout-retry upon accountHolder payable state change
        Given a shop has been created in Mirakl for an Individual with mandatory KYC data
            | city   | bank name | iban                   | bankOwnerName | lastName |
            | PASSED | testBank  | GB26TEST40051512347366 | TestData      | TestData |
        And we process the data and push to Adyen
        And a passport has been uploaded to Adyen
        When a payment voucher is sent to the Connector
            | paymentVoucher                  |
            | PaymentVoucher_PayoutShop04.csv |
        Then adyen will send the ACCOUNT_HOLDER_PAYOUT notification with status
            | statusCode | message                                           |
            | Failed     | There is not enough balance available for account |
        When the accountHolders balance is increased
            | transfer amount |
            | 9900            |
        And the PayoutState allowPayout changes from false to true
        Then the Connector will trigger payout retry
        And adyen will send the ACCOUNT_HOLDER_PAYOUT notification
            | currency | amount | statusCode | iban                   |
            | EUR      | 9900.0 | Initiated  | GB26TEST40051512347366 |
        And the failed payout record is removed from the Connector database

    @ADY-29
    Scenario Outline: Subscription fee payout to the liable account
        Given a shop has been created in Mirakl for an Individual with mandatory KYC data
            | city   | bank name | iban                   | bankOwnerName | lastName |
            | PASSED | testBank  | GB26TEST40051512347366 | TestData      | TestData |
        And we process the data and push to Adyen
        And the accountHolder receives balance
            | transfer amount   |
            | <transfer amount> |
        And the PayoutState allowPayout changes from false to true
        When a payment voucher is sent to the Connector
            | paymentVoucher                  |
            | PaymentVoucher_Subscription.csv |
        Then TRANSFER_FUNDS notification will be sent by Adyen
        """
        Success
        """
        And adyen will send the ACCOUNT_HOLDER_PAYOUT notification with status
            | statusCode   | message   |
            | <statusCode> | <message> |
        Examples:
            | transfer amount | statusCode | message                                           |
            | 100             | Failed     | There is not enough balance available for account |
            | 200             | Initiated  |                                                   |
