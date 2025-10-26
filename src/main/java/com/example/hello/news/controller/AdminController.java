package com.example.hello.news.controller;

import com.example.hello.news.dto.CategoryDTO;
import com.example.hello.news.dto.CountArticleByCategory;
import com.example.hello.news.dto.SourceByArticleDTO;
import com.example.hello.news.dto.SourceDTO;
import com.example.hello.news.entity.Category;
import com.example.hello.news.service.ArticleService;
import com.example.hello.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

/**
 * 관리자(Admin) 페이지 관련 요청을 처리하는 컨트롤러 클래스
 * 카테고리, 기사, 소스, 대시보드 등 관리자 기능의 라우팅을 담당
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final NewsService newsService;
    private final ArticleService articleService;

    /**
     * 관리자 카테고리 페이지 요청 처리
     * 데이터베이스에서 모든 카테고리 목록을 가져와 템플릿에 전달
     *
     * @param model 뷰에 전달할 모델 객체
     * @return category.html 뷰 이름
     */
    @GetMapping("/category")
    public String categorise(Model model){
        List<CategoryDTO> categories = newsService.getCategories();
        model.addAttribute("category", categories);
        return "category";
    }

    /**
     * 새로운 카테고리 입력 요청 처리
     * 카테고리명을 받아 데이터베이스에 저장 후 페이지 리다이렉트
     *
     * @param categoryName 입력된 카테고리명
     * @param model 뷰에 전달할 모델 객체
     * @return category 페이지 또는 리다이렉트 URL
     */
    @PostMapping("/inputCategory")
    public String inputCategory(@RequestParam("category_name") String categoryName, Model model) {
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            Category category = new Category();
            category.setName(categoryName);
            String msg = newsService.inputCategory(category);
            if (msg != null && msg.startsWith("ERROR")) {
                model.addAttribute("ERROR", msg);
                List<CategoryDTO> categories = newsService.getCategories();
                model.addAttribute("category", categories);
                return "category";
            }
        }
        return "redirect:/admin/category";
    }

    /**
     * 카테고리 정보 업데이트 요청 처리
     *
     * @param categoryId 업데이트할 카테고리 ID
     * @param categoryName 새로운 카테고리 이름
     * @param categoryMemo 새로운 카테고리 메모
     * @param model 뷰에 전달할 모델 객체
     * @return 카테고리 페이지 리다이렉트
     */
    @PostMapping("/updateCategory/{id}")
    public String updateCategory(@PathVariable("id")String categoryId,
                                 @RequestParam("name")String categoryName,
                                 @RequestParam("memo")String categoryMemo,
                                 Model model){
        newsService.updateCategory(categoryId,categoryName,categoryMemo);
        return "redirect:/admin/category";
    }

    /**
     * 카테고리 삭제 요청 처리
     *
     * @param id 삭제할 카테고리 ID
     * @param model 뷰에 전달할 모델 객체
     * @return 삭제 성공 시 카테고리 페이지 리다이렉트, 실패 시 category.html
     */
    @PostMapping("/deleteCategory/{id}")
    public String deleteCategory(@PathVariable String id, Model model){
        try {
            newsService.deleteCategory(id);
        } catch (RuntimeException e){
            model.addAttribute("error", e.getMessage());
            return "category";
        }
        return "redirect:/admin/category";
    }

    /**
     * 소스 목록 페이지 요청 처리
     *
     * @param model 뷰에 전달할 모델 객체
     * @param pageable 페이징 정보
     * @return source.html 뷰 이름
     */
    @GetMapping("/source")
    public String getSources(Model model, Pageable pageable){
        Page<SourceDTO> sources = newsService.getSources(pageable);
        model.addAttribute("sources", sources);
        return "source";
    }

    /**
     * 외부 API로부터 소스를 입력하는 요청 처리
     *
     * @param model 뷰에 전달할 모델 객체
     * @return source 페이지 또는 리다이렉트 URL
     */
    @GetMapping("/inputSources")
    public String inputSources(Model model){
        try {
            newsService.inputSources();
        } catch (URISyntaxException|IOException|InterruptedException|RuntimeException e){
            e.getStackTrace();
            model.addAttribute("error", e.getMessage());
            return "source";
        }
        return "redirect:/admin/source";
    }

    /**
     * 기사 관리 페이지 요청 처리
     * 카테고리별 기사 수, 소스별 기사 수 등 통계 데이터를 모델에 전달
     *
     * @param model 뷰에 전달할 모델 객체
     * @return article.html 뷰 이름
     */
    @GetMapping("/article")
    public String article(Model model){
        List<CategoryDTO> categorise = newsService.getCategories();
        Long articleCount = articleService.getTotalArticleCount();
        List<CountArticleByCategory> countByCategories = articleService.countArticleByCategories();
        List<SourceByArticleDTO> sourceByArticles = articleService.getArticleCountBySource();
        Long top10sum = sourceByArticles.stream().mapToLong(SourceByArticleDTO::getCount).sum();
        Long etcCount = articleCount - top10sum;

        model.addAttribute("articleCount", articleCount);
        model.addAttribute("countsByCategory", countByCategories);
        model.addAttribute("categories", categorise);
        model.addAttribute("sourceByArticles", sourceByArticles);
        model.addAttribute("etcCount", etcCount);

        return "article";
    }

    /**
     * 특정 카테고리의 기사 데이터를 외부 API에서 입력 요청 처리
     *
     * @param category 입력할 카테고리명
     * @param model 뷰에 전달할 모델 객체
     * @return article 페이지 또는 리다이렉트 URL
     */
    @PostMapping("/inputArticles")
    public String inputArticles(@RequestParam("categoryName") String category, Model model){
        try {
            articleService.inputArticles(category);
        } catch (URISyntaxException|IOException|InterruptedException e){
            e.getStackTrace();
            model.addAttribute("error", e.getMessage());
            return "article";
        }
        return "redirect:/admin/article";
    }

    /**
     * 관리자 대시보드 페이지 요청 처리
     * 카테고리, 소스, 기사 총 개수 등 통계 정보를 모델에 전달
     *
     * @param model 뷰에 전달할 모델 객체
     * @return dashboard.html 뷰 이름
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model){
        HashMap<String, Long> counts = newsService.getRecordCount();
        model.addAttribute("counts", counts);
        return "dashboard";
    }

    /**
     * 관리자 루트 페이지 요청 처리
     * /admin 접근 시 대시보드 페이지로 리다이렉트
     *
     * @param model 뷰에 전달할 모델 객체
     * @return 대시보드 페이지 리다이렉트 URL
     */
    @GetMapping("/")
    public String index(Model model){
        return "redirect:/admin/dashboard";
    }
}
