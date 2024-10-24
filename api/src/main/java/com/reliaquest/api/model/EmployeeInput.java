package com.reliaquest.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeInput {
    private String name;
    private int salary;
    private int age;
    private String title;
    private String email;
}
