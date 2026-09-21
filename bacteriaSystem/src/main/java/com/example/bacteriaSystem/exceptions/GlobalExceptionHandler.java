package com.example.bacteriaSystem.exceptions;

import com.example.bacteriaSystem.exceptions.customExceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;


@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErroResponse> tratarRequisicaoInvalida(RequisicaoInvalidaException ex, HttpServletRequest request) {
        var status = HttpStatus.BAD_REQUEST;

        var erro = new ErroResponse(LocalDateTime.now(),
                status.getReasonPhrase(), ex.getMessage(), status.value(),request.getRequestURI());
        return ResponseEntity.status(status).body(erro);
    }


    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> tratarAcessoNegado(AcessoNegadoException ex, HttpServletRequest request){
        var status = HttpStatus.FORBIDDEN;

        var erro = new ErroResponse(LocalDateTime.now(),
                status.getReasonPhrase(), ex.getMessage(), status.value(),request.getRequestURI());

        return ResponseEntity.status(status).body(erro);
    }

    @ExceptionHandler(EmailJaExisteException.class)
    public ResponseEntity<ErroResponse> tratarEmailJaExiste(EmailJaExisteException ex, HttpServletRequest request){
        var status = HttpStatus.BAD_REQUEST;

        var erro = new ErroResponse(LocalDateTime.now(), status.getReasonPhrase(), ex.getMessage(), status.value(),request.getRequestURI());

        return ResponseEntity.status(status).body(erro);
    }


    @ExceptionHandler(TestesInsuficientesExceptions.class)
    public ResponseEntity<ErroResponse> tratarTestesInsuficientes(TestesInsuficientesExceptions ex, HttpServletRequest request){
        var status = HttpStatus.BAD_REQUEST;
        var erro = new com.example.bacteriaSystem.exceptions.ErroResponse(LocalDateTime.now(), status.getReasonPhrase(), ex.getMessage(), status.value(),request.getRequestURI());

        return ResponseEntity.status(status).body(erro);

    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> UsuarioNaoEncontradoException(UsuarioNaoEncontradoException ex, HttpServletRequest request){
        var status = HttpStatus.NOT_FOUND;
        var erro = new com.example.bacteriaSystem.exceptions.ErroResponse(LocalDateTime.now(), status.getReasonPhrase(), ex.getMessage(), status.value(),request.getRequestURI());

        return ResponseEntity.status(status).body(erro);

    }

    @ExceptionHandler(DadosInsuficientesException.class)
    public ResponseEntity<ErroResponse> tratarDadosInsuficientesException(DadosInsuficientesException ex, HttpServletRequest request){
        var status = HttpStatus.NOT_FOUND;
        var erro = new com.example.bacteriaSystem.exceptions.ErroResponse(LocalDateTime.now(), status.getReasonPhrase(), ex.getMessage(), status.value(),request.getRequestURI());

        return ResponseEntity.status(status).body(erro);

    }

    @ExceptionHandler(TesteNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarTesteNaoEncontradoException(TesteNaoEncontradoException ex, HttpServletRequest request){
        var status = HttpStatus.NOT_FOUND;
        var erro = new com.example.bacteriaSystem.exceptions.ErroResponse(LocalDateTime.now(), status.getReasonPhrase(), ex.getMessage(), status.value(),request.getRequestURI());

        return ResponseEntity.status(status).body(erro);

    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResponse> tratarBadCredentialsException(BadCredentialsException ex, HttpServletRequest request){
        var status = HttpStatus.BAD_REQUEST;
        var erro = new com.example.bacteriaSystem.exceptions.ErroResponse(LocalDateTime.now(), status.getReasonPhrase(), ex.getMessage(), status.value(),request.getRequestURI());

        return ResponseEntity.status(status).body(erro);

    }
}
