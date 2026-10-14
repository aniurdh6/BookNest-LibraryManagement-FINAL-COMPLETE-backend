package com.booknest.library.repo;
import com.booknest.library.model.Loan;import org.springframework.data.jpa.repository.JpaRepository;
public interface LoanRepository extends JpaRepository<Loan,Long>{long countByBookIdAndReturnedAtIsNull(Long bookId);long countByMemberIdAndReturnedAtIsNull(Long memberId);}