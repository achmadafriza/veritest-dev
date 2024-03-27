package com.veriopt.veritest.errors;

import com.veriopt.veritest.isabelle.response.Task;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class IsabelleException extends ResponseStatusException {
    private static final HttpStatus ISABELLE_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;
    private static final HttpStatus USER_STATUS = HttpStatus.BAD_REQUEST;

    private final Task error;

    public IsabelleException() {
        super(ISABELLE_STATUS);

        this.error = null;
    }

    public IsabelleException(String reason) {
        super(ISABELLE_STATUS, reason);

        this.error = null;
    }

    public IsabelleException(String reason, Throwable cause) {
        super(ISABELLE_STATUS, reason, cause);

        this.error = null;
    }

    public IsabelleException(Task error) {
        super(USER_STATUS);

        this.error = error;
    }

    public IsabelleException(Task error, String reason) {
        super(USER_STATUS, reason);

        this.error = error;
    }

    public IsabelleException(Task error, String reason, Throwable cause) {
        super(USER_STATUS, reason, cause);

        this.error = error;
    }
}
