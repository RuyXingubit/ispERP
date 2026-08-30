package br.dev.xb.isperp.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setDefaultEncoding("UTF-8");
        exceptionHandler = new GlobalExceptionHandler(messageSource);
    }

    @Test
    @DisplayName("Deve formatar ResourceNotFoundException no padrão RFC 7807")
    void shouldFormatResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Cliente com ID 123 não foi encontrado");
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        ResponseEntity<Object> response = exceptionHandler.handleResourceNotFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(Problem.class);

        Problem problem = (Problem) response.getBody();
        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getTitle()).isEqualTo("Recurso não encontrado");
        assertThat(problem.getDetail()).isEqualTo("Cliente com ID 123 não foi encontrado");
        assertThat(problem.getType()).isEqualTo("https://isperp.dev.br/erros/recurso-nao-encontrado");
        assertThat(problem.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Deve formatar BusinessException no padrão RFC 7807")
    void shouldFormatBusinessException() {
        BusinessException ex = new BusinessException("Não é permitido cancelar uma fatura já quitada");
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        ResponseEntity<Object> response = exceptionHandler.handleBusinessException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Problem.class);

        Problem problem = (Problem) response.getBody();
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Violação de regra de negócio");
        assertThat(problem.getDetail()).isEqualTo("Não é permitido cancelar uma fatura já quitada");
        assertThat(problem.getType()).isEqualTo("https://isperp.dev.br/erros/erro-negocio");
    }

    @Test
    @DisplayName("Deve formatar erro não tratado (500) com mensagem amigável no RFC 7807")
    void shouldFormatGenericException() {
        Exception ex = new RuntimeException("Erro inesperado de ponteiro nulo");
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        ResponseEntity<Object> response = exceptionHandler.handleUncaught(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isInstanceOf(Problem.class);

        Problem problem = (Problem) response.getBody();
        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getTitle()).isEqualTo("Erro interno do sistema");
        assertThat(problem.getType()).isEqualTo("https://isperp.dev.br/erros/erro-de-sistema");
    }
}
