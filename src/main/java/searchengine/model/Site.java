package searchengine.model;

import lombok.*;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@org.hibernate.annotations.Table(appliesTo = "site", comment = "Информация о сайтах и статусах их индексации")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL" +
            " COMMENT 'Текущий статус полной индексации сайта, отражающий готовность поискового движка осуществлять поиск по сайту'")
    private StatusType status;

    @Column(columnDefinition = "DATETIME(6) NOT NULL COMMENT 'Дата и время статуса'")
    private Date statusTime;

    @Column(columnDefinition = "TEXT COMMENT 'Текст ошибки индексации или NULL, если её не было'")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL COMMENT 'Адрес главной страницы сайта', UNIQUE KEY uk_url(url)")
    private String url;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL COMMENT 'Имя сайта'")
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site that = (Site) o;
        return Objects.equals(id, that.id) &&
                status == that.status &&
                statusTime.equals(that.statusTime) &&
                Objects.equals(lastError, that.lastError) &&
                url.equals(that.url) &&
                name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, statusTime, lastError, url, name);
    }
}
