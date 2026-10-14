package com.booknest.library.repo;
import com.booknest.library.model.BookRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BookRequestRepository extends JpaRepository<BookRequest,Long>{
 List<BookRequest> findByStudentIdOrderByRequestedAtDesc(Long studentId);
 boolean existsByBookIdAndStudentIdAndStatus(Long bookId, Long studentId, String status);
}
