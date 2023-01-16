package searchengine.model;

import lombok.Data;
import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;

@Data
@Entity
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
        foreignKey = @ForeignKey(name = "fk_page_site", foreignKeyDefinition = "FOREIGN KEY (site_id) REFERENCES SITE(ID) ON DELETE CASCADE"),
        columnDefinition = "INT NOT NULL COMMENT 'ID веб-сайта из таблицы site'"
    )
    private Site site;

    @Column(columnDefinition = "TEXT NOT NULL COMMENT 'Адрес страницы от корня сайта', UNIQUE KEY uk_site_path(site_id,path(1000))")
    private String path;

    @Column(columnDefinition = "INT NOT NULL COMMENT 'HTTP-код ответа'")
    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'HTML-код страницы'")
    private String content;
}
