package com.booknest.library.repo;
import com.booknest.library.model.Member;import org.springframework.data.jpa.repository.JpaRepository;
public interface MemberRepository extends JpaRepository<Member,Long>{}