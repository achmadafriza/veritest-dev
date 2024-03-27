package com.veriopt.veritest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veriopt.veritest.dto.IsabelleResult;
import com.veriopt.veritest.dto.TheoryRequest;
import com.veriopt.veritest.errors.IsabelleException;
import com.veriopt.veritest.service.IsabelleService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class VeritestController {
    private @NonNull IsabelleService isabelleService;
    private @NonNull ObjectMapper mapper;

    @PostMapping(value = "/submit", consumes = {MediaType.APPLICATION_JSON_VALUE})
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

    @PostMapping(value = "/submit", consumes = {
            MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE
    })
    public ResponseEntity<IsabelleResult> submitTheory(@RequestParam Map<String, String> request,
                                                       @RequestHeader(value = "X-REQUEST-ID", required = false) String header) {
        request = request.entrySet().stream()
                .parallel()
                .map(entry -> Map.entry(entry.getKey(), StringEscapeUtils.unescapeJson(entry.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        TheoryRequest theoryRequest;
        try {
            theoryRequest = mapper.convertValue(request, TheoryRequest.class);
        } catch (IllegalArgumentException e) {
            throw new IsabelleException(e.getMessage(), e);
        }

        return submitTheory(theoryRequest, header);
    }
}
