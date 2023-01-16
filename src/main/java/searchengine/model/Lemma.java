package searchengine.model;

import lombok.Data;
import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;

@Data
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
    private Site site;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL COMMENT 'Нормальная форма слова (лемма)'")
    private String lemma;

    @Column(columnDefinition = "INT NOT NULL COMMENT 'Количество страниц, на которых слово встречается хотя бы один раз'")
    private Integer frequency;
}
