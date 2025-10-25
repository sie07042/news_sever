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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
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
            dto.setId(category.getId().toString());
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
                    return  String.format("ERROR: %s", e.getMessage());
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
                // dto의 getName()을 호출하여 발행처 이름을구하고
                // 발행처이름으로 db에서 검색을 한뒤 있으면 다음 데이터를 가져오도록 수정
                Optional<Source> srcOpt = sourceRepository.findByName(dto.getName());
                if (srcOpt.isPresent())
                    continue;

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

    public Page<SourceDTO> getSources(Pageable pageable){
        // 데이터베이스로부터 Source Entity 리스트를 가져와서
        // 모든 source Entity 인스턴스를 SourceDTO 인스턴스로 변환하여 반환한다.
        Page<Source> sources = sourceRepository.findAll(pageable);

        //for (Source source : sources){}
        // = 위 아래 같은 코드
        // stream().foreach( Funtional Interface -> 익명 클래스 -> 람다식 )
        // stream().map ( Funtional Interface -> 익명 클래스 -> 람다식 )

        // map ( source -> {
        // Source.toDTO(Source Entity source)
        // })
        // sources.stream().forEach(source -> System.out.println(source.getName()));

        return sources.map(Source::toDTO);
    }


    @Transactional
    public void updateCategory(String categoryId ,String categoryName, String categoryMemo) {
        Category category = categoryRepository.findById(Long.parseLong(categoryId))
                .orElseThrow(()-> new RuntimeException("카테고리를 찾을 수 없습니다."));
        category.setName(categoryName);
        category.setMemo(categoryMemo);

        categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(String categoryId) {
        Category category = categoryRepository.findById(Long.parseLong(categoryId))
                .orElseThrow(()-> new RuntimeException("카테고리를 찾을 수 없습니다."));

        try {
        categoryRepository.delete(category);
        }catch (Exception e) {
            throw new RuntimeException("카테고리 데이터 삭제중에 오류가발생했습니다.");
        }
    }

    public HashMap<String,Long> getRecordCount(){
        HashMap<String,Long> counts = new HashMap<>();
        counts.put("article",articleRepository.count());
        counts.put("sources",sourceRepository.count());
        counts.put("categories",categoryRepository.count());

        return counts;
    }
}


