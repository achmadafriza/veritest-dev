package com.veriopt.veritest.controller;

import com.veriopt.veritest.dto.IsabelleResult;
import com.veriopt.veritest.dto.TheoryRequest;
import com.veriopt.veritest.service.IsabelleService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VeritestController {
    private @NonNull IsabelleService isabelleService;

    @PostMapping("/submit")
    public IsabelleResult submitTheory(@RequestBody TheoryRequest request) {
        return isabelleService.validateTheory(request);
    }
}
