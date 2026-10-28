package com.librarysystem.Services;

import com.librarysystem.Entities.Book;
import com.librarysystem.Entities.BorrowRecord;
import com.librarysystem.Entities.User;
import com.librarysystem.Exception.AlreadyBorrowedException;
import com.librarysystem.Exception.BookNotBorrowedException;
import com.librarysystem.Repositories.BorrowRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BorrowRecordServiceImplTest {

    @Mock BorrowRecordRepository repository;
    @Mock UserServiceImpl userService;
    @Mock BookServiceImpl bookService;

    @InjectMocks BorrowRecordServiceImpl borrowRecordService;

    private User createUser() {
        return User.builder()
                .email("email@gmail.com")
                .build();
    }

    private Book createBook() {
        return Book.builder()
                .id("book-id")
                .title("BOOK OF THE YEAR")
                .build();
    }

    @Test
    public void borrowBook_createsRecordAndDecreasesCopies() {
        User u = createUser();
        Book b = createBook();
        when(userService.getUserByUsername("email@gmail.com")).thenReturn(u);
        when(bookService.getBookById("book-id")).thenReturn(b);
        when(repository.save(any(BorrowRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowRecord record = borrowRecordService.borrowBook("book-id", "email@gmail.com");

        assertSame(u, record.getUser());
        assertSame(b, record.getBook());
        assertEquals("email@gmail.com", record.getUser().getEmail());
        assertEquals("book-id", record.getBook().getId());
        assertEquals("BOOK OF THE YEAR", record.getBook().getTitle());
        assertEquals(LocalDate.now(), record.getBorrowDate());
        assertNull(record.getReturnDate());
        verify(bookService).decreaseAvailableCopies(b);
    }

    @Test
    public void borrowBook_ThrowExceptionOfBookAlreadyBorrowed() {
        User u = createUser();
        Book b = createBook();
        when(userService.getUserByUsername("email@gmail.com")).thenReturn(u);
        when(bookService.getBookById("book-id")).thenReturn(b);
        when(repository.findByUserAndBookAndReturnDateIsNull(u, b))
                .thenReturn(Optional.of(new BorrowRecord()));

        AlreadyBorrowedException exception =
                assertThrows(AlreadyBorrowedException.class,
                        () -> borrowRecordService.borrowBook("book-id", "email@gmail.com"));

        assertEquals("User email@gmail.com has already borrowed 'BOOK OF THE YEAR'.", exception.getMessage());
        verify(bookService, never()).decreaseAvailableCopies(b);
        verify(repository, never()).save(any());
    }

    @Test
    public void returnBook_returnsBookAndIncreaseAvailableCopies() {
        User u = createUser();
        Book b = createBook();
        BorrowRecord record = new BorrowRecord(
                "book_id",
                u, b, LocalDate.now(), null
        );
        when(userService.getUserByUsername("email@gmail.com")).thenReturn(u);
        when(bookService.getBookById("book-id")).thenReturn(b);
        when(repository.findByUserAndBookAndReturnDateIsNull(u, b)).thenReturn(Optional.of(record));
        when(repository.save(any(BorrowRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowRecord returnedRecord =
                borrowRecordService.returnBook("book-id", "email@gmail.com");

        assertEquals(LocalDate.now(), returnedRecord.getReturnDate());
        assertSame(u, returnedRecord.getUser());
        assertSame(b, returnedRecord.getBook());
        verify(bookService).increaseAvailableCopies(b);
    }

    @Test
    public void returnBook_throwBookNotBorrowed() {
        User u = createUser();
        Book b = createBook();
        when(userService.getUserByUsername("email@gmail.com")).thenReturn(u);
        when(bookService.getBookById("book-id")).thenReturn(b);
        when(repository.findByUserAndBookAndReturnDateIsNull(u, b))
                .thenReturn(Optional.empty());

        BookNotBorrowedException exception =
                assertThrows(BookNotBorrowedException.class,
                        () -> borrowRecordService.returnBook("book-id", "email@gmail.com"));

        assertEquals("Book 'BOOK OF THE YEAR' was not borrowed by user email@gmail.com.", exception.getMessage());
        verify(bookService, never()).increaseAvailableCopies(b);
        verify(repository, never()).save(any());
    }
}

