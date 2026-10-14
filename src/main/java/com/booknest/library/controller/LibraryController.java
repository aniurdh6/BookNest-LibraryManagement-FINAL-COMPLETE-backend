package com.booknest.library.controller;

import com.booknest.library.model.AppUser;
import com.booknest.library.model.Book;
import com.booknest.library.model.BookRequest;
import com.booknest.library.repo.BookRepository;
import com.booknest.library.repo.BookRequestRepository;
import com.booknest.library.repo.LoanRepository;
import com.booknest.library.repo.MemberRepository;
import com.booknest.library.repo.UserRepository;
import com.booknest.library.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LibraryController {

    private final BookRepository books;
    private final MemberRepository members;
    private final LoanRepository loans;
    private final UserRepository users;
    private final BookRequestRepository requests;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public LibraryController(
            BookRepository books,
            MemberRepository members,
            LoanRepository loans,
            UserRepository users,
            BookRequestRepository requests,
            PasswordEncoder encoder,
            JwtService jwtService) {
        this.books = books;
        this.members = members;
        this.loans = loans;
        this.users = users;
        this.requests = requests;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    public record Login(String email, String password) {}
    public record Register(String name, String email, String password) {}

    @PostMapping("/auth/login")
    public Map<String, String> login(@RequestBody Login body) {
        String email = body.email() == null ? "" : body.email().trim();
        String password = body.password() == null ? "" : body.password();

        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!encoder.matches(password, user.password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return Map.of(
                "token", jwtService.generateToken(user.email),
                "role", user.role,
                "email", user.email
        );
    }

    @PostMapping("/auth/register")
    public Map<String, String> register(@RequestBody Register body) {
        if (body.name() == null || body.name().isBlank()
                || body.email() == null || body.email().isBlank()
                || body.password() == null || body.password().length() < 6) {
            throw bad("Name, email and a password of at least 6 characters are required");
        }

        String email = body.email().trim().toLowerCase();

        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        AppUser user = new AppUser();
        user.name = body.name().trim();
        user.email = email;
        user.password = encoder.encode(body.password());
        user.role = "STUDENT";
        users.save(user);

        return Map.of("message", "Registration successful. Please sign in.");
    }

    @GetMapping("/books")
    public List<Book> books() {
        return books.findAll();
    }

    @GetMapping("/student/me")
    public Map<String, String> me(Authentication authentication) {
        AppUser user = current(authentication);
        return Map.of("email", user.email, "role", user.role);
    }

    @GetMapping("/student/books")
    public List<Book> studentBooks() {
        return books.findAll();
    }

    @GetMapping("/student/requests")
    public List<BookRequest> myRequests(Authentication authentication) {
        return requests.findByStudentIdOrderByRequestedAtDesc(current(authentication).id);
    }

    @PostMapping("/student/requests/{bookId}")
    public BookRequest requestBook(
            @PathVariable Long bookId,
            Authentication authentication) {
        AppUser student = currentStudent(authentication);
        Book book = book(bookId);

        if (book.availableCopies < 1) {
            throw bad("This book is currently unavailable");
        }

        if (requests.existsByBookIdAndStudentIdAndStatus(bookId, student.id, "PENDING")) {
            throw bad("You already have a pending request for this book");
        }

        BookRequest request = new BookRequest();
        request.book = book;
        request.student = student;
        return requests.save(request);
    }

    @GetMapping("/admin/books")
    public List<Book> adminBooks() {
        return books.findAll();
    }

    @PostMapping("/admin/books")
    public Book addBook(@Valid @RequestBody Book book) {
        book.id = null;
        book.availableCopies = book.totalCopies;
        return books.save(book);
    }

    @PutMapping("/admin/books/{id}")
    @Transactional
    public Book editBook(@PathVariable Long id, @Valid @RequestBody Book incoming) {
        Book existing = book(id);
        int issued = existing.totalCopies - existing.availableCopies;

        if (incoming.totalCopies < issued) {
            throw bad("Copies cannot be fewer than already approved copies");
        }

        existing.title = incoming.title;
        existing.author = incoming.author;
        existing.isbn = incoming.isbn;
        existing.category = incoming.category;
        existing.totalCopies = incoming.totalCopies;
        existing.availableCopies = incoming.totalCopies - issued;

        return books.save(existing);
    }

    @DeleteMapping("/admin/books/{id}")
    public void deleteBook(@PathVariable Long id) {
        if (loans.countByBookIdAndReturnedAtIsNull(id) > 0) {
            throw bad("Return borrowed copies first");
        }
        books.delete(book(id));
    }

    @GetMapping("/admin/requests")
    public List<BookRequest> allRequests() {
        return requests.findAll().stream()
                .sorted(Comparator.comparing(
                        (BookRequest request) -> request.requestedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @PostMapping("/admin/requests/{id}/approve")
    @Transactional
    public BookRequest approve(@PathVariable Long id) {
        BookRequest request = request(id);

        if (!"PENDING".equals(request.status)) {
            throw bad("Request is already processed");
        }

        if (request.book.availableCopies < 1) {
            throw bad("No copy is currently available");
        }

        request.book.availableCopies--;
        books.save(request.book);
        request.status = "APPROVED";
        request.processedAt = LocalDateTime.now();

        return requests.save(request);
    }

    @PostMapping("/admin/requests/{id}/reject")
    public BookRequest reject(@PathVariable Long id) {
        BookRequest request = request(id);

        if (!"PENDING".equals(request.status)) {
            throw bad("Request is already processed");
        }

        request.status = "REJECTED";
        request.processedAt = LocalDateTime.now();
        return requests.save(request);
    }

    @GetMapping("/admin/students")
    public List<AppUser> students() {
        return users.findAll().stream()
                .filter(user -> "STUDENT".equals(user.role))
                .toList();
    }

    private AppUser current(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private AppUser currentStudent(Authentication authentication) {
        AppUser user = current(authentication);
        if (!"STUDENT".equals(user.role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Students only");
        }
        return user;
    }

    private Book book(Long id) {
        return books.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Book not found"));
    }

    private BookRequest request(Long id) {
        return requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Request not found"));
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
