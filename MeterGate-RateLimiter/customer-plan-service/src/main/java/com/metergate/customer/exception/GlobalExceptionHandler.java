package com.metergate.customer.exception;

import com.metergate.customer.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Global Exception Handler — Poore application ka central error catcher.
 *
 * Ye class @RestControllerAdvice use karti hai, matlab:
 * - Har controller mein jo bhi exception throw hoga, ye class usse catch karegi
 * - Ugly stack traces client ko nahi jayengi
 * - Har error ka clean, consistent JSON response milega
 *
 * Soch isse ek WICKET-KEEPER ki tarah — koi bhi ball (exception)
 * chhoot jaaye controller se, ye pakad lega! 🧤
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * HANDLER 1: IllegalArgumentException
     *
     * Kab aata hai?
     * - Jab invalid input diya — jaise duplicate email, wrong plan ID
     * - Jab business logic mein koi validation fail ho
     * - Humare CustomerPlanService mein hum ye throw karte hain invalid cases mein
     *
     * HTTP Status: 400 BAD_REQUEST
     * Matlab: "Bhai teri request hi galat hai, theek karke bhej"
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {

        // ErrorResponse.of() factory method use kar rahe hain — clean aur short
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),         // 400
                HttpStatus.BAD_REQUEST.name(),           // "BAD_REQUEST"
                ex.getMessage()                          // actual error message jo humne throw kiya tha
        );

        // ResponseEntity mein wrap karke bhej rahe hain with proper HTTP status
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * HANDLER 2: NoSuchElementException
     *
     * Kab aata hai?
     * - Jab koi resource nahi milta — jaise User not found, ApiKey not found
     * - .orElseThrow() se ye automatically aata hai jab Optional empty ho
     *
     * HTTP Status: 404 NOT_FOUND
     * Matlab: "Jo tu dhundh raha hai, wo exist hi nahi karta"
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),            // 404
                HttpStatus.NOT_FOUND.name(),              // "NOT_FOUND"
                ex.getMessage()                           // "User not found with ID: 99"
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * HANDLER 3: MethodArgumentNotValidException
     *
     * Kab aata hai?
     * - Jab @Valid annotation wala request body fail ho
     * - Jaise @NotNull field null aaye, @NotBlank field blank aaye
     * - Spring automatically ye exception throw karta hai validation fail hone pe
     *
     * HTTP Status: 400 BAD_REQUEST
     *
     * Special: Ye exception mein MULTIPLE errors ho sakte hain (har field ka alag error),
     * toh hum sab errors ko ek comma-separated string mein combine karte hain.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        // Sab field errors ko ek string mein jod rahe hain
        // Example output: "email: must not be blank, name: must not be null"
        String errorMessage = ex.getBindingResult()       // validation result nikalo
                .getFieldErrors()                          // sab field errors ki list
                .stream()                                  // stream banao
                .map(fieldError ->                         // har error ko format karo
                        fieldError.getField() + ": " + fieldError.getDefaultMessage()
                )
                .collect(Collectors.joining(", "));        // comma se jod do

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),             // 400
                "VALIDATION_FAILED",                        // custom error name
                errorMessage                                // "email: must not be blank, name: must not be null"
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * HANDLER 4: Exception (CATCH-ALL)
     *
     * Ye LAST RESORT hai — agar koi unexpected exception aaye jo upar ke
     * handlers mein nahi pakda gaya, toh ye catch karega.
     *
     * HTTP Status: 500 INTERNAL_SERVER_ERROR
     * Matlab: "Server pe kuch toot gaya, client ki galti nahi hai"
     *
     * IMPORTANT: Ye hamesha LAST mein rakhna — kyunki ye sab exceptions catch karta hai.
     * Agar isse pehle rakh diya toh specific handlers kabhi call hi nahi honge!
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {

        // Console pe error print kar rahe hain debugging ke liye
        System.err.println(">>> [UNEXPECTED ERROR]: " + ex.getMessage());
        ex.printStackTrace();

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),   // 500
                HttpStatus.INTERNAL_SERVER_ERROR.name(),     // "INTERNAL_SERVER_ERROR"
                "An unexpected error occurred"               // generic message — actual error client ko nahi dikhate
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}