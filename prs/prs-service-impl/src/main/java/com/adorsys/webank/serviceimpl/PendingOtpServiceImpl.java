package com.adorsys.webank.serviceimpl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.adorsys.webank.domain.OtpStatus;
import com.adorsys.webank.dto.PendingOtpDto;
import com.adorsys.webank.repository.OtpRequestRepository;
import com.adorsys.webank.service.PendingOtpServiceApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PendingOtpServiceImpl implements PendingOtpServiceApi {

    private final OtpRequestRepository otpRequestRepository;

    @Override
    public List<PendingOtpDto> fetchPendingOtpEntries() {
        log.info("Fetching all pending OTP entries");
        
        List<PendingOtpDto> pendingOtps = otpRequestRepository.findByStatus(OtpStatus.PENDING)
                .stream()
                .map(otp -> {
                    String maskedPhone = maskPhoneNumber(otp.getPhoneNumber());
                    log.debug("Found pending OTP for phone: {}", maskedPhone);
                    return new PendingOtpDto(otp.getPhoneNumber(), otp.getOtpCode(), otp.getStatus().name());
                })
                .collect(Collectors.toList());
        
        log.info("Retrieved {} pending OTP entries", pendingOtps.size());
        return pendingOtps;
    }
    
    /**
     * Masks a phone number for logging purposes
     * Shows only last 4 digits, rest are masked
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "********";
        }
        return "******" + phoneNumber.substring(Math.max(0, phoneNumber.length() - 4));
    }
}