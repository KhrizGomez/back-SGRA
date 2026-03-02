package com.CLMTZ.Backend.dto.general;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Integer userId;
    private String firstName;
    private String lastName;
    private String identification;
    private String phoneNumber;
    private String email;
    private String address;
    private Integer institutionId;
    private Integer genderId;
}
