package com.example.hello.news.entity;

import com.example.hello.news.dto.SourceDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name="Source")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Source {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Auto Increment 자동입력

    @Column(length = 45)
    private String sid;

    @Column(nullable = false, unique = true, length = 45)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String url;

    @Column(length = 50)
    private String category;

    @Column(length = 10)
    private String language;

    @Column(length = 10)
    private String country;

    @Column(name="created_at",updatable = false,insertable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at",insertable = false)
    private LocalDateTime updatedAt;

    public static SourceDTO toDTO(Source source){
        SourceDTO dto = new SourceDTO();
        dto.setId(source.getSid());
        dto.setName(source.getName());
        dto.setDescription(source.getDescription());
        dto.setUrl(source.getUrl());
        dto.setLanguage(source.getLanguage());
        dto.setCategory(source.getCategory());
        dto.setCountry(source.getCountry());

        return dto;
    }
}