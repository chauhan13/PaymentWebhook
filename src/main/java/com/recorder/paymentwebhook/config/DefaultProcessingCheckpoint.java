package com.recorder.paymentwebhook.config;

import org.springframework.stereotype.Component;

import com.recorder.paymentwebhook.service.ProcessingCheckpoint;

@Component
public class DefaultProcessingCheckpoint implements ProcessingCheckpoint {
    @Override
    public void afterDuplicateCheck() {
    }
}
