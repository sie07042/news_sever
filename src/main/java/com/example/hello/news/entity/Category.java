package com.example.hello.news.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Length;

import java.time.LocalTime;

@Entity
@Table(name="category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Auto Increment 자동입력

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = true, unique = false, length = 500)
    private String memo;

    @Column(name="created_at",updatable = false,insertable = false)
    private LocalTime createdAt;

    @Column(name="updated_at",insertable = false)
    private LocalTime updatedAt;
}
