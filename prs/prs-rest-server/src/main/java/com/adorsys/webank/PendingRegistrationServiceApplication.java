package com.adorsys.webank;

import de.adorsys.ledgers.postings.impl.EnablePostingService;
import de.adorsys.webank.bank.api.service.BankAccountInitService;
import de.adorsys.webank.bank.api.service.BankAccountServiceConfiguration;
import de.adorsys.webank.bank.api.service.EnableBankAccountService;
import de.adorsys.webank.bank.server.utils.client.ExchangeRateClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;


@EnablePostingService
@EnableRepository
@EnablePrsServiceimpl
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableBankAccountService
@EnableFeignClients(basePackageClasses = ExchangeRateClient.class)
@ComponentScan(basePackages = {
        "com.adorsys.webank","de.adorsys.webank","de.adorsys.ledgers"
})
@Import(BankAccountServiceConfiguration.class)
public class PendingRegistrationServiceApplication {

    private final BankAccountInitService bankInitService;

    @Autowired
    public PendingRegistrationServiceApplication(BankAccountInitService bankInitService) {
        this.bankInitService = bankInitService;
    }

    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        bankInitService.initConfigData();
    }

    public static void main(String[] args) {
        SpringApplication.run(PendingRegistrationServiceApplication.class, args);
    }
}
