package com.example.lostandfound.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Date;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Get error status and details
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        // Set default error values
        Integer statusCode = null;
        String error = "An error occurred";
        String message = "We're sorry, but something went wrong.";
        
        // Set status code if available
        if (status != null) {
            statusCode = Integer.valueOf(status.toString());
            error = HttpStatus.valueOf(statusCode).getReasonPhrase();
        }
        
        // Set custom error message based on status code
        if (statusCode != null) {
            if (statusCode == 404) {
                message = "The page you're looking for doesn't exist.";
            } else if (statusCode == 403) {
                message = "You don't have permission to access this resource.";
            } else if (statusCode == 500) {
                message = "An internal server error occurred.";
            }
        }
        
        // Add attributes to the model
        model.addAttribute("timestamp", new Date());
        model.addAttribute("status", statusCode);
        model.addAttribute("error", error);
        model.addAttribute("message", message);
        
        // Add additional debug information
        if (requestUri != null) {
            model.addAttribute("path", requestUri.toString());
        }
        
        if (exception != null) {
            model.addAttribute("exception", exception.toString());
        }
        
        // Log the error
        System.err.println("\n=== ERROR DETAILS ===");
        System.err.println("Status: " + statusCode);
        System.err.println("Error: " + error);
        System.err.println("Message: " + message);
        if (requestUri != null) {
            System.err.println("Path: " + requestUri);
        }
        if (exception != null) {
            System.err.println("Exception: " + exception);
        }
        System.err.println("==================\n");
        
        return "error";
    }
}
