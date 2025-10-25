package com.example.hello.news.service;

import com.example.hello.news.dto.ArticleDTO;
import com.example.hello.news.dto.CountArticleByCategory;
import com.example.hello.news.dto.NewsResponse;
import com.example.hello.news.dto.SourceByArticleDTO;
import com.example.hello.news.entity.Article;
import com.example.hello.news.entity.Category;
import com.example.hello.news.entity.Source;
import com.example.hello.news.repository.ArticleRepository;
import com.example.hello.news.repository.CategoryRepository;
import com.example.hello.news.repository.SourceRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    @Value("${newsapi.source_url}")
    private String sourceURL;

    @Value("${newsapi.article_url}")
    private String articleURL;

    @Value("${newsapi.apikey}")
    private String apiKey;

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final SourceRepository sourceRepository;

    @Transactional
    public Long getTotalArticleCount(){
        return articleRepository.count();
    }

    public List<CountArticleByCategory> countArticleByCategories() {
        return articleRepository.countArticleByCategory();
    }

    @Transactional
    public void inputArticles(String category) throws URISyntaxException, IOException, InterruptedException, RuntimeException {
        String url = String.format("%scategory=%s&%s",articleURL,category,apiKey);
        System.out.println(url);
        //https://newsapi.org/v2/top-headlines?country=us&category=business&apiKey=65671f9acbca4086bb80c8063c043563


        HttpClient client = HttpClient.newBuilder().build();

        //request 인스턴스를 생성한다. (필수 : url, method(요청방법))
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .build();

        //client에서 request를 보내고 response를 문자열 형태로 받아온다.
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String resBody = response.body();

        Gson gson = new Gson();
        NewsResponse newsResponse = gson.fromJson(resBody,NewsResponse.class);
        System.out.println(newsResponse.getStatus());
        System.out.println(newsResponse.getTotalResults());
        System.out.println(newsResponse.getArticles()[0].getAuthor());

        saveArticles(newsResponse,category);
    }

    public void saveArticles(NewsResponse newsResponse,String category){
        try {
            for(ArticleDTO article : newsResponse.getArticles()){
                if (article.getUrl() != null){
                    // 이미 입력된 URL이 존재한다면 skip
                    boolean exists = articleRepository.findByUrl(article.getUrl()).isPresent();
                    if (exists) continue;
                }

                // 이미 기존에 입력되어 있는 source가 있으면 DB에서 찾아서 인스턴스를 만들고
                Optional<Source> srcOpt = sourceRepository.findByName(article.getSource().getName());
                //            Source src = srcOpt.get();
                // 안전한 처리를 위해
                // 없으면 새로 생성(srcOpt안에 인스턴스의 값이 null임)a
                Source src = srcOpt.orElseGet( () ->{
                    Source s1 = new Source();
                    s1.setName(article.getSource().getName());
                    return sourceRepository.save(s1);
                });

                Optional<Category> catOPt = categoryRepository.findByName(category);
                Category cat = catOPt.orElseGet(() ->{
                    Category c = new Category();
                    c.setName(category);
                    return  categoryRepository.save(c);
                });

                Article article1= Article.fromDTO(article,src,cat);
                articleRepository.save(article1);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<SourceByArticleDTO> getArticleCountBySource() {
        // JPA : Jakarta Persistanc
        // JPQL : JPA 전용 Query Language
        // 기사가 많은 순서대로 상위 10개만 가져온다.
        return articleRepository.countArticleBySource(PageRequest.of(0,10));
    }
}
