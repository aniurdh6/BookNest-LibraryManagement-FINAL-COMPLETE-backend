package com.booknest.library.repo;
import com.booknest.library.model.Book;import org.springframework.data.jpa.repository.JpaRepository;
public interface BookRepository extends JpaRepository<Book,Long>{}