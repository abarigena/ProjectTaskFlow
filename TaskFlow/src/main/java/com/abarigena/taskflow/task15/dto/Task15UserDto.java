package com.abarigena.taskflow.task15.dto;

import com.abarigena.taskflow.task15.types.UserRole;
import com.abarigena.taskflow.task15.types.UserAccountStatus;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class Task15UserDto {
    
    private Long id;
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;
    
    @NotNull(message = "Role is required")
    private UserRole role;
    
    private UserAccountStatus accountStatus;
    
    private LocalDateTime dateCreated;
    
    private LocalDateTime dateUpdated;
} 