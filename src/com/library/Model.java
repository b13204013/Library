package com.library;
import java.util.List;

class User {
    String student_no, name, password, role_level, status, created_at;
}

class Book {
    String 題名, 版本, 出版者;
    List<String> 作者;
    int 出版年;
}

class BorrowRecord {
    int user_id, book_id, borrow_days;
    String borrow_date, due_date, return_date, created_at;
}