package com.booknest.library.model;
import jakarta.persistence.*;import java.time.*;
@Entity public class Loan{@Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;@ManyToOne(optional=false) public Book book;@ManyToOne(optional=false) public Member member;public LocalDateTime issuedAt=LocalDateTime.now();public LocalDate dueDate;public LocalDateTime returnedAt;}