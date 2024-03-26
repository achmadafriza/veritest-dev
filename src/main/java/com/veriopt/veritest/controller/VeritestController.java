package com.veriopt.veritest.controller;

import com.veriopt.veritest.dto.IsabelleResult;
import com.veriopt.veritest.dto.TheoryRequest;
import com.veriopt.veritest.service.IsabelleService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VeritestController {
    private @NonNull IsabelleService isabelleService;

    @PostMapping("/submit")
    public ResponseEntity<IsabelleResult> submitTheory(@RequestBody TheoryRequest request,
                                                       @RequestHeader(value = "X-REQUEST-ID", required = false) String header) {
        String requestId = request.getRequestId();
        if (requestId == null || requestId.isEmpty()) {
            requestId = header;
        }

        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }

        request.setRequestId(requestId);

        IsabelleResult result = isabelleService.validateTheory(request);

        return ResponseEntity.ok()
                .header("X-REQUEST-ID", requestId)
                .body(result);
    }
}
