package com.example.ckyc.service;

import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.dto.CkycUpdateResponseDto;

public interface UpdateService {
    CkycUpdateResponseDto update(CkycUpdateRequestDto request);
}
