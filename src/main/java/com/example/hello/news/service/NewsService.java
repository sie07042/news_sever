package com.example.hello.news.service;

import com.example.hello.news.dto.*;
import com.example.hello.news.entity.Article;
import com.example.hello.news.entity.Category;
import com.example.hello.news.entity.Source;
import com.example.hello.news.repository.ArticleRepository;
import com.example.hello.news.repository.CategoryRepository;
import com.example.hello.news.repository.SourceRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NewsService {
    @Value("${newsapi.source_url}")
    private String sourceURL;

    @Value("${newsapi.article_url}")
    private String articleURL;

    @Value("${newsapi.apikey}")
    private String apiKey;

    private final CategoryRepository categoryRepository;
    private final SourceRepository sourceRepository;
    private final ArticleRepository articleRepository;

    // @Autowired   //위에랑 같은 코드임 (@RequiredArgsConstructor 없이 쓰는 법)
    //private CategoryRepository categoryRepository;

    public NewsResponse getGeneral() throws URISyntaxException, IOException, InterruptedException {
        String url = "https://newsapi.org/v2/top-headlines?country=us&apiKey=65671f9acbca4086bb80c8063c043563";

        //client instance를 생성한다.
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

        return newsResponse;
    }

    public List<CategoryDTO> getCategories(){
        // categoryRepository.findAll() == select * from category; ==> fetch
        List<Category> categories = categoryRepository.findAll();

        // 비어있는 category dto 리스트 인스턴스를 생성핝다.
        List<CategoryDTO> categoryDTOList= new ArrayList<>();
        for(Category category : categories){
            CategoryDTO dto = new CategoryDTO();
            dto.setName(category.getName());
            dto.setMemo(category.getMemo());
            categoryDTOList.add(dto);
        }
        return categoryDTOList;
    }

    public String inputCategory(Category category){
        if(category !=null){
            try {
                   Category saved = categoryRepository.save(category);

                } catch (Exception e){
                    return  String.format("ERROR: %s,", e.getMessage());
                }

            return "SUCCESS";
        }
        return "ERROR:카테고리 정보가 없습니다";
    }

    @Transactional // 일괄처리 작업
    public void inputSources() throws URISyntaxException, IOException, InterruptedException {
        String url = sourceURL + apiKey;
        System.out.println(url); //sourceUrl

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
        SourceResponse sourceResponse = gson.fromJson(resBody, SourceResponse.class);

        System.out.println(sourceResponse.getStatus());
        System.out.println(sourceResponse.getSources().length);

        //sourceResponse에 있는 모든 SourceDTO 인스턴스의 데이터를 이용하여
        // Source Entity 인스턴스를 생성하고 데이터베이스에 저장핝다.
        // SourceDTO ====> Source
        try {
        for (SourceDTO dto : sourceResponse.getSources()){
                Source source = new Source(); //빈 Source Entity 인스턴스를 생성
                source.setSid(dto.getId());
                source.setName(dto.getName());
                source.setDescription(dto.getDescription());
                source.setUrl(dto.getUrl());
                source.setCountry(dto.getCountry());
                source.setLanguage(dto.getLanguage());
                source.setCategory(dto.getCategory());
                sourceRepository.save(source);
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public List<SourceDTO> getSources(){
        // 데이터베이스로부터 Source Entity 리스트를 가져와서
        // 모든 source Entity 인스턴스를 SourceDTO 인스턴스로 변환하여 반환한다.
        List<Source> sources = sourceRepository.findAll();

        //for (Source source : sources){}
        // = 위 아래 같은 코드
        // stream().foreach( Funtional Interface -> 익명 클래스 -> 람다식 )
        // stream().map ( Funtional Interface -> 익명 클래스 -> 람다식 )

        // map ( source -> {
        // Source.toDTO(Source Entity source)
        // })
        // sources.stream().forEach(source -> System.out.println(source.getName()));

        return sources.stream().map(Source::toDTO).toList();
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

                // 이미 기존에 입력되어 있는 source가 있으면 DB에서 찾아서 인스턴스를 만들고
                Optional<Source> srcOpt = sourceRepository.findByName(article.getSource().getName());
    //            Source src = srcOpt.get();
                // 안전한 처리를 위해
                // 없으면 새로 생성(srcOpt안에 인스턴스의 값이 null임)
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
}


