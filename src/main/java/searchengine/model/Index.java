package searchengine.model;

import lombok.*;
import org.hibernate.annotations.Comment;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(
    name = "`index`",
    uniqueConstraints = {@UniqueConstraint(name = "uk_page_lemma", columnNames = {"page_id", "lemma_id"})},
    indexes = {
        @javax.persistence.Index(name = "fk_index_page_idx", columnList = "page_id"),
        @javax.persistence.Index(name = "fk_index_lemma_idx", columnList = "lemma_id")
    }
)
@org.hibernate.annotations.Table(appliesTo = "`index`", comment = "Поисковый индекс")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "lemma_id",
        referencedColumnName="id",
        foreignKey = @ForeignKey(name = "fk_index_lemma", foreignKeyDefinition = "FOREIGN KEY (lemma_id) REFERENCES LEMMA(ID) ON DELETE CASCADE"),
        nullable = false
    )
    @Comment("Идентификатор леммы")
    @ToString.Exclude
    private Lemma lemma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "page_id",
        referencedColumnName="id",
        foreignKey = @ForeignKey(name = "fk_index_page", foreignKeyDefinition = "FOREIGN KEY (page_id) REFERENCES PAGE(ID) ON DELETE CASCADE"),
        nullable = false
    )
    @Comment("Идентификатор страницы")
    @ToString.Exclude
    private Page page;

    @Column(name = "page_id", insertable = false, updatable = false)
    private Integer pageId;

    @Column(name = "`rank`", nullable = false)
    @Comment("Количество данной леммы для данной страницы")
    private float rank;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Index that = (Index) o;
        return Objects.equals(id, that.id) &&
                lemma.equals(that.lemma) &&
                page.equals(that.page) &&
                Objects.equals(pageId, that.pageId) &&
                Float.compare(that.rank, rank) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lemma, page, pageId, rank);
    }
}
