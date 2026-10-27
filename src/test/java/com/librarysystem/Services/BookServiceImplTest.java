package com.librarysystem.Services;


import com.librarysystem.Dto.BookDto;
import com.librarysystem.Entities.Book;
import com.librarysystem.Repositories.BookRepository;
import com.librarysystem.Utils.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
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
    public void addBook_fileUploadFailed() throws IOException {;
        BookDto req = new BookDto("title", "author", null, "category", 10);
        when(fileUpload.uploadFile(any())).thenThrow(IOException.class);

        assertThrows(IOException.class, () -> bookService.addBook(req) );
    }

    @Test
    public void updateBook_returnsUpdatedBook() {
        BookDto originalBook = new BookDto("title", "author", null, "category", 10);

    }

}
