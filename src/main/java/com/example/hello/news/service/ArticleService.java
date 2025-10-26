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

/**
 * Article 관련 비즈니스 로직을 처리하는 Service 클래스
 * - 뉴스 API로부터 기사 데이터를 가져와 DB에 저장
 * - 카테고리별, 소스별 기사 통계 제공
 */
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

    /**
     * 전체 기사 수 조회
     * @return 전체 기사 개수
     */
    @Transactional
    public Long getTotalArticleCount(){
        return articleRepository.count();
    }

    /**
     * 카테고리별 기사 수 집계
     * @return 카테고리별 기사 개수 리스트
     */
    public List<CountArticleByCategory> countArticleByCategories() {
        return articleRepository.countArticleByCategory();
    }

    /**
     * 특정 카테고리의 기사를 뉴스 API로부터 가져와 DB에 저장
     * @param category 저장할 기사 카테고리명
     * @throws URISyntaxException URI 변환 예외
     * @throws IOException HTTP 요청/응답 관련 예외
     * @throws InterruptedException HTTP 요청 중 인터럽트 발생
     */
    @Transactional
    public void inputArticles(String category) throws URISyntaxException, IOException, InterruptedException, RuntimeException {
        String url = String.format("%scategory=%s&%s", articleURL, category, apiKey);
        System.out.println(url);

        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String resBody = response.body();

        Gson gson = new Gson();
        NewsResponse newsResponse = gson.fromJson(resBody, NewsResponse.class);

        saveArticles(newsResponse, category);
    }

    /**
     * NewsResponse 객체로부터 기사 데이터를 DB에 저장
     * 이미 존재하는 기사 URL은 저장하지 않음
     * @param newsResponse 뉴스 API 응답 객체
     * @param category 기사 카테고리명
     */
    public void saveArticles(NewsResponse newsResponse, String category){
        try {
            for(ArticleDTO article : newsResponse.getArticles()){
                if (article.getUrl() != null){
                    // 이미 입력된 URL이 존재하면 skip
                    boolean exists = articleRepository.findByUrl(article.getUrl()).isPresent();
                    if (exists) continue;
                }

                // 기존에 존재하는 Source가 있으면 가져오고, 없으면 새로 생성
                Optional<Source> srcOpt = sourceRepository.findByName(article.getSource().getName());
                Source src = srcOpt.orElseGet(() -> {
                    Source s1 = new Source();
                    s1.setName(article.getSource().getName());
                    return sourceRepository.save(s1);
                });

                // 기존에 존재하는 Category가 있으면 가져오고, 없으면 새로 생성
                Optional<Category> catOpt = categoryRepository.findByName(category);
                Category cat = catOpt.orElseGet(() -> {
                    Category c = new Category();
                    c.setName(category);
                    return categoryRepository.save(c);
                });

                Article articleEntity = Article.fromDTO(article, src, cat);
                articleRepository.save(articleEntity);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 소스별 기사 수 상위 10개 조회
     * @return 상위 10개 소스별 기사 개수 리스트
     */
    public List<SourceByArticleDTO> getArticleCountBySource() {
        return articleRepository.countArticleBySource(PageRequest.of(0, 10));
    }
}
