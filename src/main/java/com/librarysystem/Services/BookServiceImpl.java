package com.librarysystem.Services;

import com.librarysystem.Dto.BookDto;
import com.librarysystem.Entities.Book;
import com.librarysystem.Exception.InsufficientCopiesException;
import com.librarysystem.Exception.ResourceNotFoundException;
import com.librarysystem.Repositories.BookRepository;
import com.librarysystem.Utils.FileUpload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository repository;
    private final FileUpload fileUpload;

    //@Transaction(rollbackFor = IOException.class) // if business requirement requires adding book to be failed if the
    // uploadFile fails, then we should add rollbackFor option
    @Transactional
    @Override
    public Book addBook(BookDto request) throws IOException {
        // Assume that upload image failure does not affect add book flow
        // It carries on and user can upload the image later
        String uri = fileUpload.uploadFile(request.imageUrl());
        Book savedBook = Book.builder()
                .title(request.title())
                .author(request.author())
                .imageUrl(uri)
                .category(request.category())
                .totalCopies(request.totalCopies())
                .availableCopies(request.totalCopies())
                .build();
        return repository.save(savedBook);
    }

    
    @Transactional
    @Override
    public Book updateBook(String id, BookDto request) throws IOException {
        Book existingBook = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));

        int oldTotalCopies = existingBook.getTotalCopies();
        int newTotalCopies = oldTotalCopies + request.totalCopies();
        int difference = newTotalCopies - oldTotalCopies;

        if (newTotalCopies < (oldTotalCopies - existingBook.getAvailableCopies())) {
            throw new IllegalArgumentException("Cannot reduce total copies below currently borrowed count.");
        }

        String uri = fileUpload.uploadFile(request.imageUrl());
        existingBook.setTitle(request.title());
        existingBook.setAuthor(request.author());
        existingBook.setImageUrl(uri);
        existingBook.setCategory(request.category());
        existingBook.setTotalCopies(newTotalCopies);
        existingBook.setAvailableCopies(existingBook.getAvailableCopies() + difference); // Adjust available copies

        return repository.save(existingBook);
    }

    @Override
    public void deleteBook(String id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found with ID: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    public List<Book> getAllBooks() {
        return repository.findAll();
    }

    @Override
    public Book getBookById(String id) {
        return  repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
    }

    @Override
    public List<Book> getAvailableBooks() {
        return repository.findByAvailableCopiesGreaterThan(0);
    }

    @Override
    public void decreaseAvailableCopies(Book book) {
        if (book.getAvailableCopies() <= 0) {
            throw new InsufficientCopiesException("No copies available for book: " + book.getTitle());
        }
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        repository.save(book);
    }

    @Override
    public void increaseAvailableCopies(Book book) {
        if (book.getAvailableCopies() >= book.getTotalCopies()) {
            throw new InsufficientCopiesException("Cannot return more copies than total for book: " + book.getTitle());
        }
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        repository.save(book);
    }
}
