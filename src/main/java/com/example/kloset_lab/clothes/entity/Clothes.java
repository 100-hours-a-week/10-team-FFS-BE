package com.example.kloset_lab.clothes.entity;

import com.example.kloset_lab.global.entity.BaseTimeEntity;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "clothes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Clothes extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private MediaFile file;

    @Column(name = "clothes_name", length = 30)
    private String clothesName;

    @Column(name = "brand_name", length = 50)
    private String brandName;

    @Column(name = "price")
    private Integer price;

    @Column(name = "size", length = 10)
    private String size;

    @Column(name = "bought_date")
    private LocalDate boughtDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, columnDefinition = "varchar(15)")
    private Category category;

    @Builder
    public Clothes(
            User user,
            MediaFile file,
            String clothesName,
            String brandName,
            Integer price,
            String size,
            LocalDate boughtDate,
            Category category) {
        this.user = user;
        this.file = file;
        this.clothesName = clothesName;
        this.brandName = brandName;
        this.price = price;
        this.size = size;
        this.boughtDate = boughtDate;
        this.category = category;
    }
}
