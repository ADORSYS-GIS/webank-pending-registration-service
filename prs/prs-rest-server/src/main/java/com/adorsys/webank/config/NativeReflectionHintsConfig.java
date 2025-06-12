package com.adorsys.webank.config;

import com.adorsys.webank.domain.OtpEntity;
import com.adorsys.webank.domain.PersonalInfoEntity;
import com.adorsys.webank.domain.UserDocumentsEntity;
import com.adorsys.webank.model.ProofOfWorkData;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
@RegisterReflectionForBinding({
        UserDocumentsEntity.class,
        PersonalInfoEntity.class,
        OtpEntity.class,
        ProofOfWorkData.class
})
public class NativeReflectionHintsConfig {
}
