Feature: Custom field validation

    As an Operator I would like to specify if the Seller is an Individual or a
    Business so I know what information I need to capture for KYC checks

    @ADY-12
    Scenario Outline: Operator can set the legal entity of shop as an Individual or Business
        Given the operator has specified that the <seller> is an <legalEntity>
        When the operator views the shop information using S20 Mirakl API call
        Then the sellers legal entity will be displayed as <legalEntity>
        Examples:
            | legalEntity | seller             |
            | Individual  | Cottons Shop       |
            | Business    | Samras Supermarket |
