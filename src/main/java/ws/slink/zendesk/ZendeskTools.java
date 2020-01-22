package ws.slink.zendesk;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zendesk.client.v2.model.hc.Article;
import org.zendesk.client.v2.model.hc.Category;
import org.zendesk.client.v2.model.hc.Section;
import ws.slink.model.Document;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ZendeskTools {

    public final @NonNull ZendeskFacade zendeskFacade;

    @Value("${properties.template.category.title}")
    private String categoryTitleTemplate;

    @Value("${properties.template.category.description}")
    private String categoryDescriptionTemplate;

    @Value("${properties.template.category.position}")
    private String categoryPositionTemplate;

    @Value("${properties.template.section.title}")
    private String sectionTitleTemplate;

    @Value("${properties.template.section.description}")
    private String sectionDescriptionTemplate;

    @Value("${properties.template.section.position}")
    private String sectionPositionTemplate;

    @Value("${zendesk.forced-update:false}")
    private boolean shouldUpdate;

    @Value("${zendesk.permission-group-title}")
    private String permissionGroupTitle;

    @Value("${zendesk.locale:en-us}")
    private String locale;

    @Value("${zendesk.comments-disabled:false}")
    private boolean commentsDisabled;

    public boolean updateHierarchy(ZendeskHierarchy hierarchy, Properties properties) {

        String catName = properties.getProperty(categoryTitleTemplate, null);
        String catDesc = properties.getProperty(categoryDescriptionTemplate, "");
        int     catPos = Integer.valueOf(properties.getProperty(categoryPositionTemplate, "0"));

        String secName = properties.getProperty(sectionTitleTemplate, null);
        String secDesc = properties.getProperty(sectionDescriptionTemplate, "");
        int     secPos = Integer.valueOf(properties.getProperty(sectionPositionTemplate, "0"));

        if (StringUtils.isBlank(catName) && (StringUtils.isBlank(secName))) {
            log.warn("neither category nor section name set in properties");
            return false;
        }

        // load category if needed
        if (!StringUtils.isBlank(catName) && (null == hierarchy.category() || !hierarchy.category().getName().equalsIgnoreCase(catName))) {
            Optional<Category> categoryOpt = zendeskFacade.getCategory(catName, catDesc, catPos, shouldUpdate);
            if (categoryOpt.isPresent()) {
                hierarchy.category(categoryOpt.get());
            } else {
                log.warn("could not load category '{}'", catName);
                return false;
            }
        }

        // load section if needed
        if (!StringUtils.isBlank(secName) && (null == hierarchy.section() || !hierarchy.section().getName().equalsIgnoreCase(secName))) {
            if (null == hierarchy.category()) {
                log.warn("category not set in hierarchy structure");
                return false;
            }
            Optional<Section> sectionOpt = zendeskFacade.getSection(hierarchy.category(), secName, secDesc, secPos, shouldUpdate);
            if (sectionOpt.isPresent()) {
                hierarchy.section(sectionOpt.get());
            } else {
                log.warn("could not load section '{}'", secName);
                return false;
            }
        }

        return true;
    }

    public Optional<Article> createArticle(Document document, Section section, String contents) {
        if (null == document || null == section || StringUtils.isBlank(contents))
            return Optional.empty();
        try {
            Article article = new Article();
            article.setSectionId(section.getId());
            article.setTitle(document.title());
            article.setPosition(document.position());
            article.setDraft(document.draft());
            article.setPromoted(document.promoted());
            article.setBody(contents);
            article.setLabelNames(document.tags());
            article.setUserSegmentId(null);
            article.setPermissionGroupId(zendeskFacade.getPermissionGroupId(permissionGroupTitle));
            article.setCommentsDisabled(commentsDisabled);
            return Optional.of(article);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Article> updateArticle(Article article, Document document, String contents) {
        if (null == article || null == document || StringUtils.isBlank(contents))
            return Optional.empty();
        try {
            article.setTitle(document.title());
            article.setBody(contents);
            article.setDraft(document.draft());
            article.setLabelNames(document.tags());
            article.setPromoted(document.promoted());
            article.setPosition(document.position());
            return Optional.of(article);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
