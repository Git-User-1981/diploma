package searchengine.model;

import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;
import javax.persistence.Index;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(name = "uk_site_lemma", columnNames = {"site_id", "lemma"}),
    indexes = {
        @Index(name = "fk_lemma_site_idx", columnList = "site_id"),
        @Index(name = "lemma_frequency_idx", columnList = "frequency"),
        @Index(name = "lemma_lemma_idx", columnList = "lemma")
    }
)
@org.hibernate.annotations.Table(appliesTo = "lemma", comment = "Леммы, встречающиеся в текстах")
@SQLInsert(sql = "insert into lemma (frequency, lemma, site_id) values (?, ?, ?) on duplicate key update frequency = frequency + 1")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "site_id",
        referencedColumnName="id",
        foreignKey = @ForeignKey(name = "fk_lemma_site", foreignKeyDefinition = "FOREIGN KEY (site_id) REFERENCES SITE(id) ON DELETE CASCADE"),
        nullable = false
    )
    @Comment("ID веб-сайта из таблицы site")
    @ToString.Exclude
    private Site site;

    @Column(nullable = false)
    @Comment("Нормальная форма слова (лемма)")
    private String lemma;

    @Column(nullable = false)
    @Comment("Количество страниц, на которых слово встречается хотя бы один раз")
    private Integer frequency;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lemma that = (Lemma) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(frequency, that.frequency) &&
                site.equals(that.site) &&
                lemma.equals(that.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, site, lemma, frequency);
    }
}
