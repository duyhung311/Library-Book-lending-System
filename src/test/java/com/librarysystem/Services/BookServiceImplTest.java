package com.librarysystem.Services;


import com.librarysystem.Dto.BookDto;
import com.librarysystem.Entities.Book;
import com.librarysystem.Exception.InsufficientCopiesException;
import com.librarysystem.Exception.ResourceNotFoundException;
import com.librarysystem.Repositories.BookRepository;
import com.librarysystem.Utils.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BookServiceImplTest {

    @InjectMocks
    private BookServiceImpl bookService;
    @Mock
    private BookRepository repository;
    @Mock
    private FileUpload fileUpload;

    // A book already in the "database": 10 copies in total, 5 of them borrowed.
    private Book createExistingBook() {
        return Book.builder()
                .id("1")
                .title("old title")
                .author("old author")
                .imageUrl("http://old-url")
                .category("old category")
                .totalCopies(10)
                .availableCopies(5)
                .build();
    }

    // ---------- addBook ----------

    @Test
    public void addBook_fileUploadSuccess_returnAddedBook() throws IOException {
        BookDto req = new BookDto("title", "author", null, "category", 10);
        when(fileUpload.uploadFile(any())).thenReturn("http://fake-url");
        when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book addedBook = bookService.addBook(req);

        assertEquals("title", addedBook.getTitle());
        assertEquals("author", addedBook.getAuthor());
        assertEquals("http://fake-url", addedBook.getImageUrl());
        assertEquals("category", addedBook.getCategory());
        assertEquals(10, addedBook.getTotalCopies());
        assertEquals(10, addedBook.getAvailableCopies());
    }

    @Test
    public void addBook_throwsWhenUploadFails() throws IOException {
        BookDto req = new BookDto("title", "author", null, "category", 10);
        when(fileUpload.uploadFile(any())).thenThrow(new IOException("upload failed"));

        IOException e = assertThrows(IOException.class, () -> bookService.addBook(req));

        assertEquals("upload failed", e.getMessage());
        verify(repository, never()).save(any());
    }

    // ---------- updateBook ----------

    @Test
    public void updateBook_updatesFieldsAndAddsCopies() throws IOException {
        Book existingBook = createExistingBook();
        BookDto req = new BookDto("new title", "new author", null, "new category", 3);
        when(repository.findById("1")).thenReturn(Optional.of(existingBook));
        when(fileUpload.uploadFile(any())).thenReturn("http://new-url");
        when(repository.save(existingBook)).thenReturn(existingBook);

        Book updatedBook = bookService.updateBook("1", req);

        assertEquals("new title", updatedBook.getTitle());
        assertEquals("new author", updatedBook.getAuthor());
        assertEquals("http://new-url", updatedBook.getImageUrl());
        assertEquals("new category", updatedBook.getCategory());
        // Current behavior: totalCopies in the request is ADDED to the old total
        // (10 + 3 = 13), and available copies go up by the same amount (5 + 3 = 8).
        assertEquals(13, updatedBook.getTotalCopies());
        assertEquals(8, updatedBook.getAvailableCopies());
    }

    @Test
    public void updateBook_throwsWhenBookNotFound() {
        BookDto req = new BookDto("new title", "new author", null, "new category", 3);
        when(repository.findById("1")).thenReturn(Optional.empty());

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> bookService.updateBook("1", req));

        assertEquals("Book not found with ID: 1", e.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    public void updateBook_throwsWhenTotalWouldDropBelowBorrowedCount() throws IOException {
        // 5 copies are borrowed. Adding -6 would leave 4 total, which is fewer than 5.
        Book existingBook = createExistingBook();
        BookDto req = new BookDto("new title", "new author", null, "new category", -6);
        when(repository.findById("1")).thenReturn(Optional.of(existingBook));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> bookService.updateBook("1", req));

        assertEquals("Cannot reduce total copies below currently borrowed count.", e.getMessage());
        verify(fileUpload, never()).uploadFile(any());
        verify(repository, never()).save(any());
    }

    // ---------- deleteBook ----------

    @Test
    public void deleteBook_deletesWhenBookExists() {
        when(repository.existsById("1")).thenReturn(true);

        bookService.deleteBook("1");

        verify(repository).deleteById("1");
    }

    @Test
    public void deleteBook_throwsWhenBookNotFound() {
        when(repository.existsById("1")).thenReturn(false);

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> bookService.deleteBook("1"));

        assertEquals("Book not found with ID: 1", e.getMessage());
        verify(repository, never()).deleteById(any());
    }

    // ---------- getAllBooks / getBookById / getAvailableBooks ----------

    @Test
    public void getAllBooks_returnsAllBooks() {
        List<Book> books = List.of(createExistingBook(), createExistingBook());
        when(repository.findAll()).thenReturn(books);

        List<Book> result = bookService.getAllBooks();

        assertSame(books, result);
    }

    @Test
    public void getBookById_returnsFoundBook() {
        Book book = createExistingBook();
        when(repository.findById("1")).thenReturn(Optional.of(book));

        Book result = bookService.getBookById("1");

        assertSame(book, result);
    }

    @Test
    public void getBookById_throwsWhenNotFound() {
        when(repository.findById("1")).thenReturn(Optional.empty());

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> bookService.getBookById("1"));

        assertEquals("Book not found with ID: 1", e.getMessage());
    }

    @Test
    public void getAvailableBooks_asksRepositoryForBooksWithMoreThanZeroCopies() {
        List<Book> books = List.of(createExistingBook());
        when(repository.findByAvailableCopiesGreaterThan(0)).thenReturn(books);

        List<Book> result = bookService.getAvailableBooks();

        assertSame(books, result);
    }

    // ---------- decreaseAvailableCopies ----------

    @Test
    public void decreaseAvailableCopies_decreasesByOneAndSaves() {
        Book book = createExistingBook(); // 5 available

        bookService.decreaseAvailableCopies(book);

        assertEquals(4, book.getAvailableCopies());
        verify(repository).save(book);
    }

    @Test
    public void decreaseAvailableCopies_throwsWhenNoCopiesAvailable() {
        Book book = createExistingBook();
        book.setAvailableCopies(0);

        InsufficientCopiesException e = assertThrows(InsufficientCopiesException.class,
                () -> bookService.decreaseAvailableCopies(book));

        assertEquals("No copies available for book: old title", e.getMessage());
        assertEquals(0, book.getAvailableCopies());
        verify(repository, never()).save(any());
    }

    // ---------- increaseAvailableCopies ----------

    @Test
    public void increaseAvailableCopies_increasesByOneAndSaves() {
        Book book = createExistingBook(); // 5 of 10 available

        bookService.increaseAvailableCopies(book);

        assertEquals(6, book.getAvailableCopies());
        verify(repository).save(book);
    }

    @Test
    public void increaseAvailableCopies_throwsWhenAllCopiesAlreadyAvailable() {
        Book book = createExistingBook();
        book.setAvailableCopies(10); // equal to totalCopies

        InsufficientCopiesException e = assertThrows(InsufficientCopiesException.class,
                () -> bookService.increaseAvailableCopies(book));

        assertEquals("Cannot return more copies than total for book: old title", e.getMessage());
        assertEquals(10, book.getAvailableCopies());
        verify(repository, never()).save(any());
    }
}
