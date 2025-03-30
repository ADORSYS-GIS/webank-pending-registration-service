/*
 * Copyright (c) 2018-2024 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package com.adorsys.webank.mockbank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.adorsys.webank.bank.api.service.domain.ASPSPConfigData;
import de.adorsys.webank.bank.api.service.domain.ASPSPConfigSource;
import de.adorsys.webank.bank.api.service.domain.LedgerAccountModel;


//@Component
public class MockBankConfigSource implements ASPSPConfigSource {
    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Override
    public ASPSPConfigData aspspConfigData() {
        java.io.InputStream inputStream = MockBankConfigSource.class.getResourceAsStream("aspsps-config.yml");
        try {
            return mapper.readValue(inputStream,ASPSPConfigData.class);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public java.util.List<LedgerAccountModel> chartOfAccount(String coaFile) {
        java.io.InputStream inputStream = MockBankConfigSource.class.getResourceAsStream(coaFile);
        LedgerAccountModel[] ledgerAccounts;
        try {
            ledgerAccounts = mapper.readValue(inputStream, LedgerAccountModel[].class);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return java.util.Arrays.asList(ledgerAccounts);
    }
}
