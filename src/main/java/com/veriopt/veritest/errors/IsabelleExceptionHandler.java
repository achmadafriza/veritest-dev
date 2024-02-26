package com.veriopt.veritest.errors;

import com.veriopt.veritest.dto.IsabelleResult;
import com.veriopt.veritest.dto.Status;
import com.veriopt.veritest.dto.TheoryRequest;
import com.veriopt.veritest.isabelle.response.ErrorTask;
import com.veriopt.veritest.isabelle.response.IsabelleGenericError;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Log4j2
@RestControllerAdvice
public class IsabelleExceptionHandler {
    @ExceptionHandler(IsabelleException.class)
    public IsabelleResult handleIsabelleException(IsabelleException e, WebRequest webRequest) {
        TheoryRequest request = (TheoryRequest) webRequest.getAttribute(
                "request", RequestAttributes.SCOPE_REQUEST
        );

        if (request == null) {
            request = TheoryRequest.builder()
                    .requestId("")
                    .build();
        }

        List<String> messages = null;
        if (Objects.nonNull(e.getError())) {
            messages = new ArrayList<>();

            switch (e.getError()) {
                case ErrorTask error -> messages.add(error.getError());
                case IsabelleGenericError error -> messages.add(error.getMessage());
                default -> throw new IllegalStateException("Unexpected value: " + e.getError());
            }
        }

        return IsabelleResult.builder()
                .status(Status.ERROR)
                .requestID(request.getRequestId())
                .message(e.getMessage())
                .isabelleMessages(messages)
                .build();
    }
}
