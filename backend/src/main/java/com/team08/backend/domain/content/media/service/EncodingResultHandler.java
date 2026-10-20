package com.team08.backend.domain.content.media.service;

import com.team08.backend.domain.content.media.dto.EncodingContext;

public interface EncodingResultHandler {
    void handleSuccess(EncodingContext context);
}