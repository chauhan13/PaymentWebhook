package com.mercor.paymentwebhook.config;

import com.mercor.paymentwebhook.service.ProcessingCheckpoint;
import org.springframework.stereotype.Component;

@Component
public class DefaultProcessingCheckpoint implements ProcessingCheckpoint {
    @Override
    public void afterDuplicateCheck() {
    }
}
