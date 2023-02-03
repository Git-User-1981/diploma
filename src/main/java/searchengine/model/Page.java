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
    name = "page",
    indexes = {
        @Index(name = "fk_page_site_idx", columnList = "site_id"),
        @Index(name = "page_code_idx", columnList = "code")
    }
)
@org.hibernate.annotations.Table(appliesTo = "page", comment = "Проиндексированные страницы сайта")
@SQLInsert(sql = "insert into page (code, content, path, site_id) values (?, ?, ?, ?) on duplicate key update code = code")
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "site_id",
        referencedColumnName="id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_page_site", foreignKeyDefinition = "FOREIGN KEY (site_id) REFERENCES SITE(id) ON DELETE CASCADE")
    )
    @Comment("ID веб-сайта из таблицы site")
    @ToString.Exclude
    private Site site;

    @Column(columnDefinition = "TEXT NOT NULL COMMENT 'Адрес страницы от корня сайта', UNIQUE KEY uk_site_path(site_id,path(500))")
    private String path;

    @Column(nullable = false)
    @Comment("HTTP-код ответа")
    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'HTML-код страницы'")
    private String content;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page that = (Page) o;
        return Objects.equals(id, that.id) &&
                site.equals(that.site) &&
                path.equals(that.path) &&
                Objects.equals(code, that.code) &&
                content.equals(that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, site, path, code, content);
    }
}
