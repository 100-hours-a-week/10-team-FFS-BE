package com.example.kloset_lab.clothes.entity;

import com.example.kloset_lab.global.entity.BaseEntity;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
public class Clothes extends BaseEntity {

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "color_mapping", joinColumns = @JoinColumn(name = "clothes_id"))
    @Column(name = "color", length = 20)
    private List<String> colors = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "clothes_material", joinColumns = @JoinColumn(name = "clothes_id"))
    @Column(name = "material", length = 30)
    private List<String> materials = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "clothes_style_tag", joinColumns = @JoinColumn(name = "clothes_id"))
    @Column(name = "style_tag", length = 20)
    private List<String> styleTags = new ArrayList<>();

    @Builder
    public Clothes(
            User user,
            MediaFile file,
            String name,
            String brandName,
            Integer price,
            String size,
            LocalDate boughtDate,
            Category category,
            List<String> colors,
            List<String> materials,
            List<String> styleTags) {
        this.user = user;
        this.file = file;
        this.clothesName = name;
        this.brandName = brandName;
        this.price = price;
        this.size = size;
        this.boughtDate = boughtDate;
        this.category = category;
        this.colors = colors != null ? new ArrayList<>(colors) : new ArrayList<>();
        this.materials = materials != null ? new ArrayList<>(materials) : new ArrayList<>();
        this.styleTags = styleTags != null ? new ArrayList<>(styleTags) : new ArrayList<>();
    }

    public void update(
            String name,
            String brandName,
            Integer price,
            String size,
            LocalDate boughtDate,
            Category category,
            List<String> colors,
            List<String> materials) {
        if (name != null) this.clothesName = name;
        if (brandName != null) this.brandName = brandName;
        if (price != null) this.price = price;
        if (size != null) this.size = size;
        if (boughtDate != null) this.boughtDate = boughtDate;
        if (category != null) this.category = category;
        if (colors != null) {
            this.colors.clear();
            this.colors.addAll(colors);
        }
        if (materials != null) {
            this.materials.clear();
            this.materials.addAll(materials);
        }
    }

    public boolean isOwner(Long currentUserId) {
        return this.user.getId().equals(currentUserId);
    }
}
