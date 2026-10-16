package pt.trevo.trevo_api.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tryon_images")
@Getter
@Setter
@NoArgsConstructor
public class TryOnImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usage_id", nullable = false)
    private TryOnUsage usage;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(name = "model_identifier", nullable = false, length = 255)
    private String modelIdentifier;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}
