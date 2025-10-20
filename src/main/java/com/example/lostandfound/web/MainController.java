package com.example.lostandfound.web;

import com.example.lostandfound.model.Item;
import com.example.lostandfound.model.User;
import com.example.lostandfound.repository.ItemRepository;
import com.example.lostandfound.service.ImageKitService;
import com.example.lostandfound.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class MainController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ImageKitService imageKitService;

    // ... other methods like home(), search(), etc. remain the same ...
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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            User user = userService.findByUsername(username).orElse(null);

            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/post-item";
            }

            item.setUser(user);

            if (!imageFile.isEmpty()) {
                // Upload returns a Map with "url" and "fileId"
                Map<String, String> uploadData = imageKitService.uploadFile(imageFile);
                
                // Save both the URL and the File ID to the item
                item.setImagePath(uploadData.get("url"));
                item.setImageFileId(uploadData.get("fileId"));
            }

            itemRepository.save(item);
            redirectAttributes.addFlashAttribute("success", "Item posted successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload image: " + e.getMessage());
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
            items = itemRepository.findAllByOrderByCreatedAtDesc(Pageable.unpaged()).getContent();
        } else {
            Item.ItemStatus itemStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    itemStatus = Item.ItemStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore
                }
            }
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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || "anonymousUser".equals(auth.getName())) {
                redirectAttributes.addFlashAttribute("error", "Please log in to view your profile");
                return "redirect:/login";
            }

            String username = auth.getName();
            Optional<User> userOptional = userService.findByUsername(username);

            if (userOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User account not found.");
                return "redirect:/";
            }

            User user = userOptional.get();
            List<Item> items = itemRepository.findByUserOrderByCreatedAtDesc(user);
            model.addAttribute("user", user);
            model.addAttribute("items", items);
            return "profile";

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            redirectAttributes.addFlashAttribute("error", "A server error occurred: " + e.getMessage());
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
                Item item = itemRepository.findById(id).orElse(null);
                if (item != null && item.getUser().getId().equals(user.getId())) {
                    
                    // If the item has an image file ID, delete it from ImageKit
                    if (item.getImageFileId() != null && !item.getImageFileId().isEmpty()) {
                        imageKitService.deleteFile(item.getImageFileId());
                    }
                    
                    // Delete the item from the database
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

