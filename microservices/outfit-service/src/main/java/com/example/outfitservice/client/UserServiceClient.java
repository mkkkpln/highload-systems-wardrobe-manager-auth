package com.example.outfitservice.client;

import com.example.outfitservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/users/{id}")
    UserDto getUserById(@RequestHeader("Authorization") String authorization,
                        @PathVariable Long id);
}
