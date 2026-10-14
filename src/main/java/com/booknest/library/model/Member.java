package com.booknest.library.model;
import jakarta.persistence.*;import jakarta.validation.constraints.*;import java.time.*;
@Entity public class Member{@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;@NotBlank public String name;@Email @NotBlank @Column(unique=true) public String email;public String phone;public LocalDateTime createdAt=LocalDateTime.now();}