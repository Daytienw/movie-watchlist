package com.daytien.movie_watchlist.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import io.jsonwebtoken.security.SignatureException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * Regression: the handler used to test against java.security.SignatureException,
     * which JJWT never throws, so a forged token produced a 500 instead of a 403.
     */
    @Test
    void signatureException_mapsToForbidden() {
        ProblemDetail problem = handler.handleInvalidSignature(new SignatureException("bad signature"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void badCredentials_mapsToUnauthorized() {
        ProblemDetail problem = handler.handleBadCredentials(new BadCredentialsException("Bad credentials"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void accessDenied_mapsToForbidden() {
        ProblemDetail problem = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void resourceNotFound_mapsToNotFound() {
        ProblemDetail problem = handler.handleNotFound(new ResourceNotFoundException("missing"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void duplicateResource_mapsToConflict() {
        ProblemDetail problem = handler.handleDuplicate(new DuplicateResourceException("already there"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void dataIntegrityViolation_mapsToConflict() {
        ProblemDetail problem = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("uk_watchlist_user_imdb"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void omdbFailure_mapsToBadGateway() {
        ProblemDetail problem = handler.handleOmdb(new OmdbException("Invalid API key!"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
    }

    /**
     * Internal failures must not echo their message back to the caller; the
     * detail is logged server-side instead.
     */
    @Test
    void unexpectedException_mapsToServerErrorWithoutLeakingTheMessage() {
        ProblemDetail problem = handler.handleUnexpected(
                new IllegalStateException("jdbc:postgresql://localhost:5432 connection refused"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(problem.getDetail()).doesNotContain("jdbc");
    }
}
