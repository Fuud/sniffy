package io.sniffy.boot;

import io.sniffy.servlet.SnifferFilter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

@Configuration
public class SniffyConfiguration implements ImportAware, BeanFactoryAware {

    private BeanFactory beanFactory;

    private AnnotationAttributes enableSniffy;

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> map = importMetadata.getAnnotationAttributes(EnableSniffy.class.getName());
        this.enableSniffy = AnnotationAttributes.fromMap(map);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Bean
    public SnifferFilter sniffyFilter() {

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(new BeanFactoryResolver(beanFactory));

        ExpressionParser parser = new SpelExpressionParser();
        Expression enabledExpression = parser.parseExpression(enableSniffy.getString("enabled"));
        Expression injectHtmlExpression = parser.parseExpression(enableSniffy.getString("injectHtml"));

        SnifferFilter snifferFilter = new SnifferFilter();
        snifferFilter.setEnabled(enabledExpression.getValue(context, Boolean.class));
        snifferFilter.setInjectHtml(injectHtmlExpression.getValue(context, Boolean.class));
        return snifferFilter;
    }

}