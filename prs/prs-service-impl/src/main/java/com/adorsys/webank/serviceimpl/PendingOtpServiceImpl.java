package com.adorsys.webank.serviceimpl;

import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.webank.service.PendingOtpServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PendingOtpServiceImpl implements PendingOtpServiceApi {

    private static final Logger log = LoggerFactory.getLogger(PendingOtpServiceImpl.class);

    private final OtpRequestRepository otpRequestRepository;

    public PendingOtpServiceImpl(OtpRequestRepository otpRequestRepository) {
        this.otpRequestRepository = otpRequestRepository;
    }

    @Override
    public List<PendingOtpDto> getPendingOtps() {
        return otpRequestRepository.findByStatus(OtpStatus.PENDING)
                .stream()
                .map(otp -> new PendingOtpDto(otp.getPhoneNumber(), otp.getOtpCode(), otp.getStatus().name()))
                .toList();
    }
}
