package com.booknest.library.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="book_requests")
public class BookRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne(optional=false) public Book book;
 @ManyToOne(optional=false) public AppUser student;
 @Column(nullable=false) public String status="PENDING";
 @Column(nullable=false) public LocalDateTime requestedAt=LocalDateTime.now();
 public LocalDateTime processedAt;
}
