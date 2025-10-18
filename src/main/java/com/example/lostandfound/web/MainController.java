package com.example.lostandfound.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.lostandfound.model.Item;
import com.example.lostandfound.model.User;
import com.example.lostandfound.repository.ItemRepository;
import com.example.lostandfound.service.StorageService;
import com.example.lostandfound.service.UserService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;

@Controller
public class MainController {
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private StorageService storageService;
    
    @GetMapping("/")
    public String home(Model model, @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Item> items = itemRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        model.addAttribute("items", items);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", items.getTotalPages());
        
        return "index";
    }
    
    @GetMapping("/post-item")
    public String postItemForm(Model model) {
        model.addAttribute("item", new Item());
        return "post-item";
    }
    
    @PostMapping("/post-item")
    public String postItem(@Valid @ModelAttribute("item") Item item,
                          BindingResult bindingResult,
                          @RequestParam("imageFile") MultipartFile imageFile,
                          RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "post-item";
        }
        
        try {
            // Get current user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username).orElse(null);
            
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/post-item";
            }
            
            item.setUser(user);
            
            // Handle image upload
            if (!imageFile.isEmpty()) {
                String filename = storageService.storeFile(imageFile);
                item.setImagePath(filename);
            }
            
            itemRepository.save(item);
            redirectAttributes.addFlashAttribute("success", "Item posted successfully!");
            
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload image");
            return "redirect:/post-item";
        }
        
        return "redirect:/";
    }
    
    @GetMapping("/search")
    public String searchItems(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String category,
                             Model model) {
        
        List<Item> items;
        
        if (keyword == null || keyword.trim().isEmpty()) {
            // No search keyword, show all items
            items = itemRepository.findAllByOrderByCreatedAtDesc(Pageable.unpaged()).getContent();
        } else {
            // Search with filters
            Item.ItemStatus itemStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    itemStatus = Item.ItemStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore
                }
            }
            
            // Format keyword with wildcards for LIKE query
            String searchKeyword = "%" + keyword.trim() + "%";
            items = itemRepository.searchItemsWithFilters(searchKeyword, itemStatus, category);
        }
        
        model.addAttribute("items", items);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("category", category);
        
        
        return "search-results";
    }
    
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }
    
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "register";
        }
        
        try {
            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @GetMapping("/my-items")
    public String myItems(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.findByUsername(username).orElse(null);
        
        if (user != null) {
            List<Item> items = itemRepository.findByUserOrderByCreatedAtDesc(user);
            model.addAttribute("items", items);
        }
        
        return "my-items";
    }
    
    @GetMapping("/profile")
    public String profile(Model model, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {
            System.out.println("\n=== Profile Endpoint Accessed ===");
            System.out.println("Request URL: " + request.getRequestURL());
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("Authentication: " + (auth != null ? auth.toString() : "NULL"));
            
            if (auth == null) {
                System.out.println("Authentication object is null");
                redirectAttributes.addFlashAttribute("error", "Authentication required");
                return "redirect:/login";
            }
            
            String username = auth.getName();
            System.out.println("Current username: " + username);
            
            if (username == null || "anonymousUser".equals(username)) {
                System.out.println("User not authenticated, redirecting to login");
                redirectAttributes.addFlashAttribute("error", "Please log in to view your profile");
                return "redirect:/login";
            }
            
            System.out.println("Looking up user in database...");
            Optional<User> userOptional = userService.findByUsername(username);
            System.out.println("User found in database: " + userOptional.isPresent());
            
            if (userOptional.isEmpty()) {
                System.out.println("User not found in database");
                redirectAttributes.addFlashAttribute("error", "User account not found. Please contact support.");
                return "redirect:/";
            }
            
            User user = userOptional.get();
            System.out.println("Found user: " + user.getUsername() + " (ID: " + user.getId() + ")");
            
            try {
                System.out.println("Fetching items for user...");
                List<Item> items = itemRepository.findByUserOrderByCreatedAtDesc(user);
                System.out.println("Found " + items.size() + " items for user");
                
                // Log first few items for debugging
                int maxItemsToLog = Math.min(items.size(), 3);
                for (int i = 0; i < maxItemsToLog; i++) {
                    Item item = items.get(i);
                    System.out.println("Item " + (i+1) + ": " + item.getTitle() + " (ID: " + item.getId() + ")");
                }
                
                model.addAttribute("user", user);
                model.addAttribute("items", items);
                
                System.out.println("Rendering profile page...\n");
                return "profile";
                
            } catch (Exception e) {
                System.err.println("Error fetching items: " + e.getMessage());
                e.printStackTrace();
                // Continue with empty items list
                model.addAttribute("user", user);
                model.addAttribute("items", new ArrayList<Item>());
                return "profile";
            }
        } catch (Exception e) {
            System.err.println("\n!!! CRITICAL ERROR in profile endpoint !!!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("\n");
            
            // For debugging, you might want to see the full stack trace in the response
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            redirectAttributes.addFlashAttribute("error", "A server error occurred: " + e.getMessage());
            redirectAttributes.addFlashAttribute("stackTrace", sw.toString());
            
            return "redirect:/";
        }
    }
    
    @PostMapping("/delete-item/{id}")
    public String deleteItem(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.findByUsername(username).orElse(null);
        
        if (user != null) {
            try {
                // Get the item first to check if it exists and get the image path
                Item item = itemRepository.findById(id).orElse(null);
                if (item != null && item.getUser().getId().equals(user.getId())) {
                    // Delete the associated image file if it exists
                    if (item.getImagePath() != null) {
                        try {
                            storageService.deleteFile(item.getImagePath());
                        } catch (IOException e) {
                            // Log the error but continue with item deletion
                            System.err.println("Failed to delete image file: " + e.getMessage());
                        }
                    }
                    
                    // Delete the item from database
                    itemRepository.delete(item);
                    redirectAttributes.addFlashAttribute("success", "Item deleted successfully!");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Item not found or you don't have permission to delete it.");
                }
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Failed to delete item: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "User not found.");
        }
        
        return "redirect:/profile";
    }
}
