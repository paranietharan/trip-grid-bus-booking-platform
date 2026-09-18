package com.paranietharan.tripgrid.provider.dto;

import com.paranietharan.tripgrid.provider.entity.ProviderStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateProviderRequest {

    @NotBlank(message = "Provider name is required")
    @Size(max = 150, message = "Provider name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 30, message = "Phone number must not exceed 30 characters")
    private String phoneNumber;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    public CreateProviderRequest() {
    }

    public CreateProviderRequest(String name, String email, String phoneNumber, String address) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
