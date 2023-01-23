package searchengine.model;

import lombok.*;
import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@org.hibernate.annotations.Table(appliesTo = "lemma", comment = "Леммы, встречающиеся в текстах")
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_site_lemma", columnNames = {"site_id", "lemma"}))
@SQLInsert(sql = "insert into lemma (frequency, lemma, site_id) values (?, ?, ?) on duplicate key update frequency = frequency + 1")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "site_id",
        referencedColumnName="id",
        foreignKey = @ForeignKey(name = "fk_lemma_site", foreignKeyDefinition = "FOREIGN KEY (site_id) REFERENCES SITE(ID) ON DELETE CASCADE"),
        columnDefinition = "INT NOT NULL COMMENT 'ID веб-сайта из таблицы site'"
    )
    @ToString.Exclude
    private Site site;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL COMMENT 'Нормальная форма слова (лемма)'")
    private String lemma;

    @Column(columnDefinition = "INT NOT NULL COMMENT 'Количество страниц, на которых слово встречается хотя бы один раз'")
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
